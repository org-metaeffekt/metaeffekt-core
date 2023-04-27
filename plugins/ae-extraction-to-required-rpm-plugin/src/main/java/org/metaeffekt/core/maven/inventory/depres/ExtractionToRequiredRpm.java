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

public class ExtractionToRequiredRpm {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractionToRequiredRpm.class);

    protected final File extractionDir;

    public ExtractionToRequiredRpm(final File extractionDir) {
        this.extractionDir = extractionDir;
    }

    protected Map<String, Set<String>> getProvidesMap(final File[] packageDirsArray) {
        Map<String, Set<String>> providesStringToProvider = new HashMap<>();

        for (File packageDir : packageDirsArray) {
            if (shouldIgnore(packageDir)) {
                LOG.info("Ignoring '[{}]'", packageDir);
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
            if (shouldIgnore(packageFilesFile)) {
                LOG.info("Ignoring '[{}]'", packageFilesFile);
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
        return requirement.replaceFirst("[<>=].*$", "").trim();
    }

    protected Set<String> getProviders(String requirementLine,
                                       Map<String, Set<String>> providesStringToProvider,
                                       Map<String, Set<String>> filesystemPathToProvider,
                                       LinuxSymlinkResolver linkResolver) {
        String requirement = requirementLine.trim();

        Set<String> installedProviders = providesStringToProvider.get(requirement);

        // TODO: consider making a list of "known" strings that require special handling.
        if (requirement.startsWith("rpmlib(")) {
            // rpmlib seems to be a prerequisite that is only relevant for packaging purposes.
            // https://stackoverflow.com/questions/25775285/rpmlibfiledigests-dependency-error-on-suse
            LOG.debug("Ignoring known requirement (rpm feature, only relevant for packaging / installation) [{}]",
                    requirement
            );
            return Collections.emptySet();
        }

        if (installedProviders == null || installedProviders.size() == 0) {
            // unable to resolve. try another lookup:
            // split version requirements from the requirement if present.
            // we do this because we don't want to reimplement rpm's (somewhat complicated) verion compare function.
            // this may lead to lookups with more than one provider DESPITE the real system knowing one.

            String requirementWithoutVersion = stripVersionRequirement(requirement).trim();

            installedProviders = providesStringToProvider.get(requirementWithoutVersion);
        }

        if (installedProviders == null || installedProviders.size() == 0) {
            // still haven't found it. could it be a file requirement? check the filesystem, minding symlinks.
            installedProviders = filesystemPathToProvider.get(requirement);


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
        }

        return installedProviders == null ? Collections.emptySet() : installedProviders;
    }

    protected boolean shouldIgnore(File packageDir) {
        return packageDir.getName().equals(".DS_Store");
    }

    protected HashMap<String, Set<String>> getRequirements(final Set<String> toResolve,
                                                           final File depsDir,
                                                           final Map<String, Set<String>> providesStringToProvider,
                                                           final Map<String, Set<String>> filesystemPathToProvider,
                                                           LinuxSymlinkResolver linkResolver,
                                                           Map<String, Set<String>> packageToUnresolvedRequirements) {
        HashMap<String, Set<String>> packageToRequiredPackages = new HashMap<>();
        for (String packageName : toResolve) {
            File packageDir = new File(depsDir, packageName);

            if (shouldIgnore(packageDir)) {
                LOG.info("Ignoring '[{}]'", packageDir.getPath());
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

            try (InputStream inputStream = Files.newInputStream(requiresFile.toPath(), StandardOpenOption.READ);
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                String requiresString = IOUtils.toString(reader);

                // default to no providers present
                packageToRequiredPackages.computeIfAbsent(packageName, (k) -> new HashSet<>());

                for (String line : requiresString.split("\n")) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    Set<String> resolvedRequired = getProviders(
                            line,
                            providesStringToProvider,
                            filesystemPathToProvider,
                            linkResolver
                    );
                    if (resolvedRequired.size() == 0) {
                        // no direct providers for this package. add it to the list for later logging or handling
                        packageToUnresolvedRequirements
                                .computeIfAbsent(packageName, (k) -> new HashSet<>())
                                .add(line.trim());
                    } else if (resolvedRequired.size() > 1) {
                        LOG.warn("multiple providers were resolved for '[{}]' in package '[{}]', " +
                                        "marking all as a requirement.",
                                line.trim(), packageName);
                    }

                    // add ALL found providers for this requirement as required since we can't make this choice
                    packageToRequiredPackages
                            .computeIfAbsent(packageName, (k) -> new HashSet<>())
                            .addAll(resolvedRequired);
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

    public RequirementsResult runResolution(final Collection<String> mustHaves) {
        // TODO: update this and associated methods to attempt to use NUL-delimited data if present, otherwise fall back

        File packageDepsDir = getPackageDepsDir();
        File[] packageDepsPackageDirArray = packageDepsDir.listFiles();
        Objects.requireNonNull(packageDepsPackageDirArray, "Failed to list files in package-deps.");

        // read symlinks and create a resolver
        File filesystemDir = new File(extractionDir, "filesystem");
        // TODO update this and associated methods to use NUL-delimited data whenever possible, otherwise fall back
        File symlinksFile = new File(filesystemDir, "symlinks.txt");

        LinuxSymlinkResolver linkResolver;
        if (!Files.isRegularFile(symlinksFile.toPath())) {
            LOG.warn("Directory doesn't contain [filesystem/symlinks.txt]. File dependencies might not resolve.");
            linkResolver = new LinuxSymlinkResolver(Collections.emptyMap());
        } else {
            linkResolver = getSymlinkResolverFromSymlinks(symlinksFile);
        }

        LOG.info("Building maps");

        // build maps for provides
        Map<String, Set<String>> providesStringToProvider = getProvidesMap(packageDepsPackageDirArray);
        Map<String, Set<String>> filesystemPathToProvider = getFileProviderMap(getPackageFilesPackageFileArray());

        LOG.info("Performing lookups");

        // try to resolve all dependency strings, leaving only simple package names
        HashMap<String, Set<String>> packageToRequiredPackages = new HashMap<>();
        Set<String> newlyRequired = new HashSet<>(mustHaves);

        HashMap<String, Set<String>> packageToUnresolvedRequirements = new HashMap<>();

        do {
            HashMap<String, Set<String>> toPut = getRequirements(
                    newlyRequired,
                    packageDepsDir,
                    providesStringToProvider,
                    filesystemPathToProvider,
                    linkResolver,
                    packageToUnresolvedRequirements
            );

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
                packageToUnresolvedRequirements
        );
    }
}
