package org.metaeffekt.core.maven.inventory.depres;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.metaeffekt.core.maven.inventory.linkres.LinuxSymlinkResolver;
import org.metaeffekt.core.maven.inventory.linkres.ResolverPathHolder;
import org.metaeffekt.core.maven.inventory.linkres.ResolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class ResolutionRun {
    private static final Logger LOG = LoggerFactory.getLogger(ResolutionRun.class);

    /**
     * A list of hardcoded packaage name prefixes that can be safely ignored for our purposes.<br>
     * Maps from prefix to a rationale.
     */
    private final LinkedHashMap<String, String> ignoreIfStart = new LinkedHashMap<String, String>(){{
        // rpmlib seems to be a prerequisite that is only relevant for packaging purposes.
        // https://stackoverflow.com/questions/25775285/rpmlibfiledigests-dependency-error-on-suse
        put("rpmlib(", "implicitly added during build; rpm feature, only relevant for packaging / installation");
    }};

    // basic variables required to run
    protected final File extractionDir;
    protected final Set<String> mustHaves;

    protected final AtomicBoolean wasCalled = new AtomicBoolean(false);

    // constructed variables to make life simpler
    File packageDepsDir;
    LinuxSymlinkResolver linkResolver;
    protected Map<String, Set<String>> providesStringToProvider;
    protected Map<String, Set<String>> filesystemPathToProvider;

    // variables where we'll dump all our data at runtime
    Set<String> installedPackages = new HashSet<>();
    /**
     * Contains packages that have unresolved requirements, mapping from a package name to such requirement strings.
     */
    Map<String, Set<String>> packageToUnresolvedRequirements = new HashMap<>();
    /**
     * Contains conditionally required packages, possibly obtained inexactly (like by chopping boolean dependencies).
     */
    Set<String> conditionallyRequired = new HashSet<>();

    public ResolutionRun(File extractionDir, Collection<String> mustHaves) {
        this.extractionDir = Objects.requireNonNull(extractionDir);
        this.mustHaves = new LinkedHashSet<>(Objects.requireNonNull(mustHaves));
    }


    protected Map<String, Set<String>> getProvidesMap(final File[] packageDirsArray) {
        Map<String, Set<String>> providesStringToProvider = new HashMap<>();

        for (File packageDir : packageDirsArray) {
            if (shouldIgnorePackageDir(packageDir)) {
                LOG.debug("Ignoring package dir '[{}]'", packageDir);
                continue;
            }

            if (!packageDir.isDirectory()) {
                LOG.error("package dir '[{}]' is not a directory", packageDir);
            }

            String packageName = packageDir.getName();

            // each package provides itself
            providesStringToProvider.computeIfAbsent(packageName, (k) -> new HashSet<>()).add(packageName);

            // read and add lines from the providers file
            File providesFile = new File(packageDir, "provides.txt");
            if (!Files.isRegularFile(providesFile.toPath())) {
                LOG.warn("provides file '[{}]' doesn't exist. skipping '[{}]' in provides indexing.",
                        providesFile, packageName);
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(providesFile.toPath(), StandardOpenOption.READ);
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String myString = IOUtils.toString(reader);
                for (String line : myString.split("\n")) {
                    String providesLine = line.trim();
                    if (StringUtils.isBlank(providesLine)) {
                        continue;
                    }

                    providesStringToProvider
                            .computeIfAbsent(providesLine, (k) -> new HashSet<>())
                            .add(packageName);

                    // to ease lookups, remove the version numbers from provides.
                    // TODO: if we implement proper version matching / comparison, remove this bodge.
                    //  this bodge adds a provides line WITHOUT respecting version numbers.
                    //  this is likely to work since it is unlikely that a package is provided with two different
                    //  versions. since i'm not sure about the possibility of this happening, it may be considered
                    //  a bodge.
                    providesStringToProvider
                            .computeIfAbsent(stripVersionRequirement(providesLine), (k) -> new HashSet<>())
                            .add(packageName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return providesStringToProvider;
    }

    protected Map<String, Set<String>> getFileProviderMap(final File[] packageFilesArray) {
        Map<String, Set<String>> filePathToProviders = new HashMap<>();

        for (File packageFilesFile : packageFilesArray) {
            if (shouldIgnorePackageDir(packageFilesFile)) {
                LOG.debug("Ignoring package files in '[{}]'", packageFilesFile);
                continue;
            }

            if (!Files.isRegularFile(packageFilesFile.toPath())) {
                LOG.error("package file '[{}]' is not a file", packageFilesFile);
            }

            if (!packageFilesFile.getName().endsWith("_files.txt")) {
                LOG.warn("expected files file at [{}] to end in \"_files.txt\".", packageFilesFile);
            }

            String packageName = packageFilesFile.getName().replaceAll("_files\\.txt$", "");

            try (InputStream inputStream = Files.newInputStream(packageFilesFile.toPath(), StandardOpenOption.READ);
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String fileContent = IOUtils.toString(reader);

                for (String line : fileContent.split("\n")) {
                    filePathToProviders.computeIfAbsent(line, (k) -> new HashSet<>()).add(packageName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return filePathToProviders;
    }

    protected LinuxSymlinkResolver getSymlinkResolverFromSymlinks(final File symlinksFile) {
        Map<String, String> symlinks = new HashMap<>();

        try (InputStream inputStream = Files.newInputStream(symlinksFile.toPath());
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            String symlinksFileContent = IOUtils.toString(reader);

            long i = 0;
            for (String line : symlinksFileContent.split("\n")) {
                i++;
                String symlinkSeparator = " --> ";
                int cutAt = line.indexOf(symlinkSeparator);
                if (cutAt == -1) {
                    LOG.error("Invalid line [{}] in symlinks.txt file: no \" --> \" separator in [{}]", i, line);
                    continue;
                }

                // the symlink paths in these files have their leading slash removed. re-add it for resolving.
                String symlinkPath = line.substring(0, cutAt);
                String symlinkTarget = line.substring(cutAt + symlinkSeparator.length());

                if (!symlinkPath.startsWith("/")) {
                    LOG.error(
                            "Invalid line [{}] in symlinks.txt file: no identifiable absolute path in [{}]",
                            i,
                            line
                    );
                    continue;
                }

                String previousValue = symlinks.put(symlinkPath, symlinkTarget);
                if (previousValue != null) {
                    LOG.error("Dupe symlink path, Overrode present symlink at [{}] to [{}] with target [{}].",
                            symlinkPath,
                            previousValue,
                            symlinkTarget
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LinuxSymlinkResolver(symlinks);
    }

    protected String stripVersionRequirement(String requirement) {
        return requirement.replaceFirst("[<>=][ =]+.*$", "").trim();
    }

    protected Set<String> tryFileRequirementResolve(String requirement) {
        // check the filesystem, minding symlinks.
        Set<String> installedProviders = filesystemPathToProvider.get(requirement);

        // do another lookup, trying different paths and following symlinks each time
        ResolverPathHolder holder = linkResolver.resolve(requirement);

        if (holder.getStatus() == ResolverStatus.DONE) {
            Set<String> installedProvidersForReal = filesystemPathToProvider.get(holder.getCurrentPath());

            if (installedProviders != null && !Objects.equals(installedProviders, installedProvidersForReal)) {
                LOG.debug("Oddity: Different providers [{}] and [{}] for symlink paths [{}] and [{}].",
                        installedProviders,
                        installedProvidersForReal,
                        requirement,
                        holder.getCurrentPath());
            }
        } else {
            LOG.warn("Resolving requirement [{}] as path returned status [{}]", requirement, holder.getStatus());
            return Collections.emptySet();
        }

        return installedProviders;
    }

    /**
     * Chops strings into even smaller parts, discarding empty ones.
     * @param toChop the strings to be chopped
     * @param chopSequence the sequence of characters around which will be chopped (string split).
     */
    protected void chop(Set<String> toChop, String chopSequence) {
        Set<String> chopped = new HashSet<>();

        for (String bit : toChop) {
            for (String piece : bit.split(Pattern.quote(chopSequence))) {
                if (!piece.trim().isEmpty()) {
                    chopped.add(piece.trim());
                }
            }
        }

        toChop.clear();
        toChop.addAll(chopped);
    }

    protected ProviderLookupResult getProviders(String requirementLine) {
        String requirement = requirementLine.trim();

        // check if this requirement is even interesting to resolve
        for (Map.Entry<String, String> entry : ignoreIfStart.entrySet()) {
            if (requirement.startsWith(entry.getKey())) {
                // print ignored requirement and rationale in debug log
                LOG.debug("Ignoring known requirement [{}]: {}",
                        requirement,
                        entry.getValue()
                );

                return ProviderLookupResult.successWithNone();
            }
        }

        // try first lookup: provides strings
        Set<String> installedProviders = providesStringToProvider.get(requirement);

        if (installedProviders == null || installedProviders.size() == 0) {
            // unable to resolve. try another lookup:
            // split version requirements from the requirement if present.
            // we do this because we don't want to reimplement rpm's (somewhat complicated) verion compare function.
            // this may lead to lookups with more than one provider DESPITE the real system knowing one.

            String requirementWithoutVersion = stripVersionRequirement(requirement).trim();

            installedProviders = providesStringToProvider.get(requirementWithoutVersion);
        }

        // try to roughly handle boolean dependencies by marking them as occurring in some sort of "conditional"
        if (requirement.startsWith("(")) {
            // this is a boolean dependency
            if (!requirement.endsWith(")")) {
                // AFAIK these have to end with a bracket if they start with one
                LOG.warn("boolean dependency [{}] was expected to end in a bracket", requirement);
            }

            // instead of properly resolving these, we'll collect them and output them separately.
            Set<String> pieces = new HashSet<>();
            pieces.add(requirement);

            chop(pieces, "(");
            chop(pieces, ")");
            chop(pieces, " and ");
            chop(pieces, " or ");
            chop(pieces, " if ");
            chop(pieces, " else ");
            chop(pieces, " with ");
            chop(pieces, " without ");
            chop(pieces, " unless ");

            // now we should be left with only requirement specs or conditional parts. do stuff with this data.
            for (String choppedRequirement : pieces) {
                // try to skip version statements
                boolean isVersionSpecifier = false;
                isVersionSpecifier |= choppedRequirement.startsWith("<=");
                isVersionSpecifier |= choppedRequirement.startsWith(">=");
                isVersionSpecifier |= choppedRequirement.startsWith("=");

                if (isVersionSpecifier) {
                    // skip version specifiers
                    continue;
                }

                choppedRequirement = stripVersionRequirement(choppedRequirement);

                // warn if we still find odd package names
                if (choppedRequirement.contains(" ")) {
                    LOG.warn("skipping odd package name (after chopping) [{}] from [{}]", choppedRequirement, requirement);
                    continue;
                }

                // cutting corners, only add these to their own collection instead of processing them
                conditionallyRequired.add(choppedRequirement);
            }

            // terminate to avoid odd errors about symlink resolves (which will most certainly fail)
            return ProviderLookupResult.successWithNone();
        }

        if (installedProviders == null || installedProviders.size() == 0) {
            // still haven't found it. could it be a file requirement?

            try {
                installedProviders = tryFileRequirementResolve(requirement);
            } catch (Exception e) {
                LOG.warn("couldn't resolve requirement [{}] as a symlink. might not be one after all.", requirement);
            }
        }

        if (installedProviders == null || installedProviders.size() == 0) {
            // out of things to try, return lookup failure
            return new ProviderLookupResult(Collections.emptySet(), false);
        } else {
            // return whatever is in installedproviders
            return new ProviderLookupResult(installedProviders, true);
        }
    }

    /**
     * Determines whether a "package dir" is invalid and should be skipped in processing package dirs.
     * @param packageDir a file present in the dir of package dirs.
     * @return true if the file is known as bogus and should be skipped in processing, false otherwise.
     */
    protected boolean shouldIgnorePackageDir(File packageDir) {
        return packageDir.getName().equals(".DS_Store");
    }

    /**
     * Gets the requirements for a set of packages to resolve.
     * @param toResolve the set of package names to resolve requirements for
     * @return a map from package name to the set of packages that it (definitely) requires
     */
    protected HashMap<String, Set<String>> getRequirements(final Set<String> toResolve) {
        HashMap<String, Set<String>> packageToRequiredPackages = new HashMap<>();
        for (String packageName : toResolve) {
            if (!installedPackages.contains(packageName)) {
                LOG.debug("Looked up package name [{}] is not contained in installed packages list.", packageName);
            }

            File packageDir = new File(packageDepsDir, packageName);

            if (shouldIgnorePackageDir(packageDir)) {
                LOG.debug("Ignoring package dir '[{}]'", packageDir.getPath());
                continue;
            }

            if (!packageDir.isDirectory()) {
                LOG.error("Package directory [{}] is not a directory", packageDir.toPath());
                LOG.error("This will likely lead to a crash and is likely caused by missing extraction data.");
            }

            File requiresFile = new File(packageDir, "requires.txt");

            if (!Files.isRegularFile(requiresFile.toPath())) {
                LOG.error("File '[{}]' with requires data doesn't exist. skipping '[{}]'.", requiresFile, packageName);
                throw new RuntimeException("Could not resolve a package due to missing data or invalid dependency.");
            }

            // read requirements and attempt to determine the providing package
            try (InputStream inputStream = Files.newInputStream(requiresFile.toPath(), StandardOpenOption.READ);
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String requiresString = IOUtils.toString(reader);

                // default to no providers present
                packageToRequiredPackages.computeIfAbsent(packageName, (k) -> new HashSet<>());

                for (String line : requiresString.split("\n")) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    ProviderLookupResult providerLookup = getProviders(line);

                    // use ProviderLookupResult's functionality to determine success and log otherwise
                    Set<String> resolvedRequired = providerLookup.requiredPackages;
                    if (!providerLookup.resolverSuccess) {
                        // no direct providers for this package. add to the issue list of resolve was unsuccessful
                        packageToUnresolvedRequirements
                                .computeIfAbsent(packageName, (k) -> new HashSet<>())
                                .add(line.trim());
                    } else {
                        if (resolvedRequired.size() > 1) {
                            LOG.warn("multiple providers were resolved for '[{}]' in package '[{}]', " +
                                            "marking all as a requirement.",
                                    line.trim(), packageName);
                        }

                        // add ALL found providers for this requirement as required since we can't make this choice
                        packageToRequiredPackages
                                .computeIfAbsent(packageName, (k) -> new HashSet<>())
                                .addAll(resolvedRequired);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return packageToRequiredPackages;
    }

    protected File getPackageDepsDir() {
        File packageDepsDir = new File(extractionDir, "package-deps");
        if (!packageDepsDir.isDirectory()) {
            throw new IllegalArgumentException("Directory doesn't contain a package-deps directory.");
        }
        return packageDepsDir;
    }

    protected File[] getPackageFilesPackageFileArray() {
        File packageFilesDir = new File(extractionDir, "package-files");
        if (!packageFilesDir.isDirectory()) {
            throw new IllegalArgumentException("Directory doesn't contain package-files directory.");
        }

        File[] packageFilesPackageFileArray = packageFilesDir.listFiles();
        return Objects.requireNonNull(packageFilesPackageFileArray, "Failes to list files in package-files.");
    }

    protected void readInstalled() {
        File packagesNameOnlyFile = new File(extractionDir, "packages_rpm-name-only.txt");

        try (InputStream inputStream = new FileInputStream(packagesNameOnlyFile);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            String readFile = IOUtils.toString(reader);

            if (StringUtils.isBlank(readFile)) {
                LOG.warn("Read [{}] is empty. This is likely an error.", packagesNameOnlyFile.getName());
            }

            for (String packageName : readFile.split("\n")) {
                if (!packageName.trim().isEmpty()) {
                    installedPackages.add(packageName);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("file " + packagesNameOnlyFile.getName() + " not in analysis dir.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Does some prepwork, grabbing useful data from the filesystem.
     */
    protected void doPrep() {
        // TODO: update this and associated methods to use NUL-delimited data if present, otherwise fall back
        packageDepsDir = getPackageDepsDir();
        File[] packageDepsPackageDirArray = packageDepsDir.listFiles();
        Objects.requireNonNull(packageDepsPackageDirArray, "Failed to list files in package-deps.");

        // read symlinks and create a resolver
        File filesystemDir = new File(extractionDir, "filesystem");
        // TODO: update this and associated methods to use NUL-delimited data if present, otherwise fall back
        File symlinksFile = new File(filesystemDir, "symlinks.txt");

        if (!Files.isRegularFile(symlinksFile.toPath())) {
            LOG.warn("Directory doesn't contain [filesystem/symlinks.txt]. File dependencies might not resolve.");
            linkResolver = new LinuxSymlinkResolver(Collections.emptyMap());
        } else {
            linkResolver = getSymlinkResolverFromSymlinks(symlinksFile);
        }

        LOG.debug("Building maps");

        // build maps for provides
        providesStringToProvider = getProvidesMap(packageDepsPackageDirArray);
        filesystemPathToProvider = getFileProviderMap(getPackageFilesPackageFileArray());

        readInstalled();
    }

    public RequirementsResult runResolution() {
        if (wasCalled.getAndSet(true)) {
            throw new IllegalStateException("this object has already been called and can no longer be used.");
        }

        doPrep();

        LOG.debug("Performing lookups");
        // try to resolve all dependency strings, leaving only simple package names
        HashMap<String, Set<String>> packageToRequiredPackages = new HashMap<>();
        Set<String> newlyRequired = new HashSet<>(mustHaves);

        do {
            HashMap<String, Set<String>> toPut = getRequirements(newlyRequired);

            packageToRequiredPackages.putAll(toPut);

            newlyRequired.removeAll(toPut.keySet());

            // determine newly required packages for the next round
            for (Set<String> requirements : toPut.values()) {
                for (String derivedRequirement : requirements) {
                    if (!packageToRequiredPackages.containsKey(derivedRequirement)) {
                        newlyRequired.add(derivedRequirement);
                    }
                }
            }

        } while (!newlyRequired.isEmpty());

        return new RequirementsResult(
                packageToRequiredPackages,
                packageToUnresolvedRequirements,
                conditionallyRequired,
                installedPackages
        );
    }
}
