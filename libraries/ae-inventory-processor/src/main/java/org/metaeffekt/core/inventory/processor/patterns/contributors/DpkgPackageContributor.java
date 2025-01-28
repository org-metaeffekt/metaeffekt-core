/*
 * Copyright 2009-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.metaeffekt.core.inventory.processor.linux.LinuxDistributionUtil;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DpkgPackageContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(DpkgPackageContributor.class);

    protected static final Pattern hexStringPattern = Pattern.compile("^[a-fA-F0-9]+$");

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/status");
        add(".md5sums");
    }});

    private static final List<String> PATH_FRAGMENTS = new ArrayList<String>() {{
        add("var/lib/");
        add("usr/lib/");
    }};

    @SuppressWarnings("unused")
    public static class DpkgStatusFileEntry {
        private static final Logger LOG = LoggerFactory.getLogger(DpkgStatusFileEntry.class);

        /**
         * Static mapping from dpkg keys to our field names. Used for setting fields via reflection.
         */
        public static final Map<String, String> dpkgToKey = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("Architecture", "architecture");
                put("Breaks", "breaks");
                put("Build-Ids", "buildIds");
                put("Built-Using", "builtUsing");
                put("Cnf-Extra-Commands", "cnfExtraCommands");
                put("Cnf-Priority-Bonus", "cnfPriorityBonus");
                put("Cnf-Visible-Pkgname", "cnfVisiblePkgname");
                put("Conffiles", "conffiles");
                put("Config-Version", "configVersion");
                put("Conflicts", "conflicts");
                put("Depends", "depends");
                put("Description", "description");
                put("Efi-Vendor", "efiVendor");
                put("Enhances", "enhances");
                put("Essential", "essential");
                put("Gstreamer-Decoders", "gstreamerDecoders");
                put("Gstreamer-Elements", "gstreamerElements");
                put("Gstreamer-Encoders", "gstreamerEncoders");
                put("Gstreamer-Uri-Sinks", "gstreamerUriSinks");
                put("Gstreamer-Uri-Sources", "gstreamerUriSources");
                put("Gstreamer-Version", "gstreamerVersion");
                put("Homepage", "homepage");
                put("Important", "important");
                put("Installed-Size", "installedSize");
                put("Lua-Versions", "luaVersions");
                put("Maintainer", "maintainer");
                put("Multi-Arch", "multiArch");
                put("Package", "packageName");
                put("Pre-Depends", "preDepends");
                put("Priority", "priority");
                put("Protected", "protectedAttr");
                put("Provides", "provides");
                put("Recommends", "recommends");
                put("Replaces", "replaces");
                put("Section", "section");
                put("Source", "source");
                put("Status", "status");
                put("Suggests", "suggests");
                put("Version", "version");
            }
        });

        AtomicBoolean hasChecked = new AtomicBoolean(false);
        public DpkgStatusFileEntry() {
            if (hasChecked.getAndSet(true)) {
                // check this object for inconsistencies between the map and fields
                Field[] fields = DpkgStatusFileEntry.class.getFields();

                for (String value : dpkgToKey.values()) {
                    // check that each value also be a field
                    try {
                        DpkgStatusFileEntry.class.getDeclaredField(value);
                    } catch (NoSuchFieldException e) {
                        LOG.warn("Self-check inconsistency: Field [{}] exists in map, not in class.", value);
                    }
                }
            }
        }

        public String architecture;
        public String breaks;
        public String buildIds;
        public String builtUsing;
        public String cnfExtraCommands;
        public String cnfPriorityBonus;
        public String cnfVisiblePkgname;
        public String conffiles;
        public String configVersion;
        public String conflicts;
        public String depends;
        public String description;
        public String efiVendor;
        public String enhances;
        public String essential;
        public String gstreamerDecoders;
        public String gstreamerElements;
        public String gstreamerEncoders;
        public String gstreamerUriSinks;
        public String gstreamerUriSources;
        public String gstreamerVersion;
        public String homepage;
        public String important;
        public String installedSize;
        public String luaVersions;
        public String maintainer;
        public String multiArch;
        public String packageName;
        public String preDepends;
        public String priority;
        public String protectedAttr;
        public String provides;
        public String recommends;
        public String replaces;
        public String section;
        public String source;
        public String status;
        public String suggests;
        public String version;

        /**
         * Overflow map containing dpkg keys to their values.<br>
         * Must only be non-empty in case the dpkg status files contains keys that this class can't otherwise handle.
         */
        public Map<String, String> overflowDpkgKeyToValue = new HashMap<>();

        /**
         * Sets a value in this class via mapped reflection.
         *
         * @param dpkgKey the key dpkg uses for this field
         * @param value   the value of the field
         */
        public void setByKey(String dpkgKey, String value) {
            String fieldName = dpkgToKey.get(dpkgKey);

            if (fieldName == null) {
                LOG.warn("Passed unknown or invalid dpkgKey [{}] (missing field in class).", dpkgKey);
                // set it as an overflow key and continue
                overflowDpkgKeyToValue.putIfAbsent(dpkgKey, value);
                return;
            }

            Field toSet;
            try {
                toSet = this.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                // this should not happen since the map should only contain keys that are also in this object
                throw new RuntimeException(e);
            }

            // toSet now needs to be non-null since otherwise we should have thrown already

            try {
                toSet.set(this, value);
            } catch (IllegalAccessException e) {
                // should not happen because all fields of this holder object are public
                throw new RuntimeException(e);
            }
        }
    }

    // TODO: find out how to use (pseudo-)absolute paths for include patterns. there is some magic in deciding
    //  where our current scan root is but once we find out what it is we can use it to resolve stuff for us
    //  instead of messing about with more imprecise scanning methods

    @Override
    public boolean applies(String pathInContext) {
        // The default location under root is var/lib/dpkg.
        // if this part matches, make the bet that this is a debian file system and run package file inclusions.

        // could also match files from ./info/ as anchors?
        return pathInContext.endsWith("var/lib/dpkg/status") || pathInContext.contains("var/lib/dpkg/status.d");
    }

    protected List<DpkgStatusFileEntry> readCompleteStatusFile(final File statusFile) throws IOException {
        // load complete status file cause streaming it is more annoying
        String statusFileContent = FileUtils.readFileToString(statusFile, StandardCharsets.UTF_8);

        // the package name that is being parsed. Useful for error reporting
        String lastFoundPackageName = null;

        // to add DpkgStatusFileEntry to
        List<DpkgStatusFileEntry> entries = new ArrayList<>();

        // to iterate over
        String[] packageStrings = statusFileContent.split("\n\n");

        // should not stream since this file format and error detection rely on things being in the correct order
        for (final String packageString : packageStrings) {
            // a new package's information block starts here.
            String currentKey = null;
            StringBuilder currentValue = null;

            boolean expectNewPackage = true;
            boolean unexpectedBlankLine = false;

            DpkgStatusFileEntry statusFileEntry = new DpkgStatusFileEntry();

            for (final String line : packageString.split("\n")) {
                if (StringUtils.isBlank(line)) {
                    unexpectedBlankLine = true;
                    continue;
                }

                // first check if this line is a continuation of the previous value
                if (line.startsWith(" ")) {
                    // ensure this is not the beginning of a package
                    if (expectNewPackage) {
                        LOG.error("Expected beginning of new package block, found value continuation.");
                        continue;
                    }

                    // cut off the space
                    String appendValue = line.substring(1);

                    // "." values are left as literal values as i can't find a spec for how to handle this
                    currentValue.append(appendValue);
                } else {
                    // this is not a continuation of a previous value. get and put key-value
                    int colonIndex = line.indexOf(":");

                    // didn't find a colon
                    if (colonIndex == -1) {
                        // possibly an invalid "description" line
                        LOG.error("No colon in supposed key-value pair [{}].", line);
                    }

                    // split key and value, cut off ": " to get value
                    String key = line.substring(0, colonIndex);
                    String value = line.length() >= colonIndex + 2 ? line.substring(colonIndex + 2) : "";

                    if (StringUtils.isBlank(key)) {
                        // this indicates bad splitting behaviour or broken info
                        LOG.error("Unexpected blank key while parsing lines after [{}].", lastFoundPackageName);
                        continue;
                    }

                    if (expectNewPackage && !"Package".equals(key)) {
                        // continue to try to parse other packages anyway
                        LOG.error("Expected key 'Package' but got [{}]. Data corruption imminent.", key);
                    }

                    // set package name for debugging purposes
                    if ("Package".equals(key)) {
                        lastFoundPackageName = value;
                    }

                    currentKey = key;
                    currentValue = new StringBuilder(value);
                }

                // add key and value
                statusFileEntry.setByKey(currentKey, currentValue.toString());

                expectNewPackage = false;
            }

            entries.add(statusFileEntry);

            if (unexpectedBlankLine) {
                LOG.warn("Odd blank line while parsing [{}]. Bad status file or parsing error?", lastFoundPackageName);
            }
        }

        return entries;
    }

    public ComponentPatternData createComponentPattern(String versionAnchor,
                                                       DpkgStatusFileEntry entry,
                                                       String checksum,
                                                       String includePatterns, LinuxDistributionUtil.LinuxDistro distro) {
        ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, entry.packageName);
        // add list of comma-separated paths
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);

        // FIXME: we need a post-processing step to clear the inventory by known individual components (e.g. if package 2 is fully a subset of package 1, remove package 2)
        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");

        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/*.py, **/WHEEL, **/RECORD, **/METADATA, **/top_level.txt, **/__pycache__/**/*, /var/lib/dpkg/info/*.postinst, **/var/lib/dpkg/info/*.preinst, **/var/lib/dpkg/info/*.list, **/var/lib/dpkg/info/*.md5sums, **/var/lib/dpkg/info/*.postrm");


        // get version from the entry
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, entry.version);

        // try to fill "component part" in a plausible way
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART,
                entry.packageName + "-" + entry.version);

        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, versionAnchor);
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);

        // FIXME: how do we handle symlinks? they are not included in the md5sums file but can be part of the package
        // componentPatternData.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
        try {
            componentPatternData.set(Artifact.Attribute.PURL.getKey(),
                    buildPurl(distro, entry.packageName, entry.version, entry.architecture));
        } catch (Exception e) {
            LOG.error("Could not create PURL for package [{}].", entry.packageName);
        }

        return componentPatternData;
    }

    /**
     * Reads list of files that were in this package and tries to create an include pattern.
     *
     * @param entry the entry in the corresponding status file
     * @param correspondingMd5sumsFile the corresponding md5sums file
     *
     * @return a joiner with resulting include patterns or null on failure
     */
    public StringJoiner createIncludePatternsFromHashFile(
              DpkgStatusFileEntry entry, File correspondingMd5sumsFile) {

        // prepare file list
        final StringJoiner fileJoiner = new StringJoiner(", ");

        if (correspondingMd5sumsFile.exists()) {
            try (Stream<String> lineStream = Files.lines(correspondingMd5sumsFile.toPath(), StandardCharsets.UTF_8)) {
                // streamlines so we don't need to preload to memory
                lineStream.forEachOrdered(line -> append(correspondingMd5sumsFile, line, fileJoiner));
            } catch (Exception e) {
                LOG.info("Could not read file list for entry with name [{}]: {}", entry.packageName, e.getMessage());
            }
        }

        // we have to include the files in the status.d directory as well
        fileJoiner.add("var/lib/dpkg/status.d/" + entry.packageName);
        fileJoiner.add("var/lib/dpkg/status.d/" + entry.packageName + ".*");

        // NOTE: we have to check if the share folder or the var/lib/dpkg/info folder are correct patterns or if we can
        //   assume that they are always present and belong to the package
        fileJoiner.add("var/lib/dpkg/info/" + entry.packageName + "*");

        fileJoiner.add("usr/share/doc/" + entry.packageName + "/**/*");

        fileJoiner.add("usr/share/lintian/overrides/" + entry.packageName + "/**/*");
        if ("tzdata".equals(entry.packageName)) {
            fileJoiner.add("usr/share/zoneinfo/**/*");
        }

        return fileJoiner;
    }

    private void append(File correspondingMd5sumsFile, String line, StringJoiner fileJoiner) {
        try {
            // definitely cut off the hash before adding...
            int spaceSeparatorIndex = line.indexOf("  ");
            if (spaceSeparatorIndex == -1) {
                // skip invalid lines immediately.
                return;
            }
            final String hash = line.substring(0, spaceSeparatorIndex);
            final String toAdd = line.substring(spaceSeparatorIndex + 2);

            // skip empty lines or invalid formatting
            if (StringUtils.isBlank(toAdd)) {
                return;
            }

            // err out if the md5sum isn't a md5sum
            if (!hexStringPattern.matcher(hash).matches()) {
                // this was not a real hash. should never happen. means splitting failed miserably.
                LOG.error("Splitting failed while reading line of dpkg md5sums at [{}].",
                        correspondingMd5sumsFile.getAbsolutePath());
                return;
            }

            // NOTE: we are not analysing whether the checksum has changed here.

            fileJoiner.add(toAdd);

            if (toAdd.endsWith(".gz")) {
                fileJoiner.add(ContributorUtils.extendArchivePattern(toAdd));
            }

            // FIXME: validate
            if (toAdd.endsWith(".a") || toAdd.endsWith(".o")) {
                fileJoiner.add(ContributorUtils.extendLibPattern(toAdd));
            }

            // FIXME: compensated symbolic link issue lib --> var/lib
            if (toAdd.startsWith("lib/")) {
                fileJoiner.add("var/" + toAdd);
                fileJoiner.add("usr/" + toAdd);
            }

            // FIXME: compensated symbolic link issue sbin --> usr/sbin
            if (toAdd.startsWith("sbin/")) {
                fileJoiner.add("usr/" + toAdd);
            }

            // FIXME: compensated symbolic link issue bin --> usr/bin
            if (toAdd.startsWith("bin/")) {
                fileJoiner.add("usr/" + toAdd);
            }

            // FIXME: compensated symbolic link issue var/lib --> lib
            if (toAdd.startsWith("var/lib/")) {
                fileJoiner.add(toAdd.substring(4));
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public List<ComponentPatternData> contributeStatusFileBased(File baseDir,
                                                                String relativeAnchorFilePath,
                                                                String checksum) {
        final File anchorFile = new File(baseDir, relativeAnchorFilePath);

        String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorFilePath, PATH_FRAGMENTS);

        List<DpkgStatusFileEntry> entries;
        try {
            entries = readCompleteStatusFile(anchorFile);
        } catch (IOException e) {
            LOG.error("Unable to parse status file [{}].", anchorFile);
            return Collections.emptyList();
        }

        final LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(new File(baseDir, virtualRootPath));

        // contribute component patterns for each registered package
        List<ComponentPatternData> componentPatterns = new ArrayList<>();
        for (DpkgStatusFileEntry entry : entries) {
            if (StringUtils.isBlank(entry.packageName)) {
                // skip useless incomplete entry
                LOG.info("Skipping entry with empty name.");
                continue;
            }

            // first, find and read the dpkg file that lists included files. .md5sums is better than .list cause dirs.

            // potential filenames to be tested. use .md5sums because .list also includes parent directories
            final String fileListName = entry.packageName + ".md5sums";
            final String fileListNameWithArch = entry.packageName + ":" + entry.architecture + ".md5sums";

            // construct paths required
            final File infoFileBasedir = new File(anchorFile.getParentFile(), "info");
            File fileWithoutArch = new File(infoFileBasedir, fileListName);
            File fileWithArch = new File(infoFileBasedir, fileListNameWithArch);

            // check for existence of one of them
            final File correspondingMd5sumsFile;
            if (fileWithoutArch.exists()) {
                correspondingMd5sumsFile = fileWithoutArch;
                if (fileWithArch.exists()) {
                    LOG.warn("Oddity: multiple md5sums files found for package [{}].", entry.packageName);
                }

                // check with the original way of figuring out which filename is correct:
                //if (pkgbin->multiarch == PKG_MULTIARCH_SAME &&
                //	    format == PKG_INFODB_FORMAT_MULTIARCH)
                // with this we can match behaviour of dpkg(-query). assume the newer multiarch db format
                if ("same".equals(entry.multiArch)) {
                    LOG.warn("Possible mismatch: using non-arch md5sums when dpkg would use :arch suffix");
                }
            } else {
                // use the filename with arch as the corresponding filename
                correspondingMd5sumsFile = fileWithArch;

                if (!fileWithoutArch.exists()) {
                    LOG.debug("No md5sums file found for package [{}].", entry.packageName);
                }
            }

            // create patterns
            StringJoiner includePatternsJoiner = createIncludePatternsFromHashFile(entry, correspondingMd5sumsFile);

            if (includePatternsJoiner == null) {
                // something went wrong. skip without adding.
                continue;
            }


            final String relativePath = org.metaeffekt.core.util.FileUtils.asRelativePath(virtualRootPath, relativeAnchorFilePath);
            final ComponentPatternData cpd = createComponentPattern(
                    relativePath, entry, checksum, includePatternsJoiner.toString(), distro);

            cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
            cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "dpkg");

            // add created patterns
            componentPatterns.add(cpd);
        }

        return componentPatterns;
    }

    /**
     * Tries to get metadata from dpkg status.d. Useful for "distroless" images.<br>
     * Hacked together to support a system that didn't have a status file but only a status.d.
     * @param baseDir same as {@link ComponentPatternContributor#contribute(File, String, String)}
     * @param relativeAnchorFilePath same as {@link ComponentPatternContributor#contribute(File, String, String)}
     * @param checksum same as {@link ComponentPatternContributor#contribute(File, String, String)}
     * @param virtualRootPath same as {@link ComponentPatternContributor#contribute(File, String, String)}
     *
     * @return same as {@link ComponentPatternContributor#contribute(File, String, String)}
     */
    public List<ComponentPatternData> contributeStatusDirectoryBased(File baseDir,
                 String virtualRootPath, String relativeAnchorFilePath, String checksum) {

        final File md5sumsFile = new File(baseDir, relativeAnchorFilePath);

        // maybe this check should be moved to applies but it also requires a constructed File so...
        final String parentPath = md5sumsFile.getParentFile().getAbsolutePath();
        if (!parentPath.endsWith("var/lib/dpkg/status.d")) {
            // this md5sums is not in the expected location. probably not a dpkg status.d anchor. skip it.
            return Collections.emptyList();
        }

        if (!md5sumsFile.isFile()) {
            // this is supposed to be a file. let's fail silently cause it would not be egregious otherwise, i guess.
            return Collections.emptyList();
        }

        final List<ComponentPatternData> createdComponentPatterns = new ArrayList<>();

        File fileWithoutMd5sums = new File(md5sumsFile.getParentFile(),
                md5sumsFile.getName().replaceAll("\\.md5sums$", ""));

        if (!fileWithoutMd5sums.exists()) {
            // no metadata is not that useful. i'll just expect them to have valid metadata for now.
            return Collections.emptyList();
        }

        List<DpkgStatusFileEntry> entries;
        try {
            entries = readCompleteStatusFile(fileWithoutMd5sums);
        } catch (IOException e) {
            // skip if we can't read metadata
            return Collections.emptyList();
        }

        if (entries.size() != 1) {
            LOG.warn("Skipping potential status.d anchor with multiple entries: expected 1 but got [{}] from [{}].",
                entries.size(), fileWithoutMd5sums.getAbsoluteFile());
        }

        DpkgStatusFileEntry entry = entries.get(0);

        // this may be less complicated since this type of format seems to have simpler filenames

        // compute pseudoroot
        final File virtualRoot = md5sumsFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        // create patterns
        if (!virtualRoot.exists()) {
            LOG.warn("Computed virtual root [{}] does not exist", virtualRoot.getAbsolutePath());
            return Collections.emptyList();
        }

        StringJoiner includesJoiner = createIncludePatternsFromHashFile(entry, md5sumsFile);

        ComponentPatternData cpd = createComponentPattern(
                org.metaeffekt.core.util.FileUtils.asRelativePath(virtualRoot, md5sumsFile),
                entry, checksum,
                includesJoiner.toString(), LinuxDistributionUtil.parseDistro(new File(baseDir, virtualRootPath)));

        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "dpkg-distroless");

        // add the created pattern
        createdComponentPatterns.add(cpd);

        return createdComponentPatterns;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorFilePath, String checksum) {
        String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorFilePath, PATH_FRAGMENTS);
        if (relativeAnchorFilePath.endsWith("status")) {
            return contributeStatusFileBased(baseDir, relativeAnchorFilePath, checksum);
        } else if (relativeAnchorFilePath.endsWith(".md5sums")) {
            return contributeStatusDirectoryBased(baseDir, virtualRootPath, relativeAnchorFilePath, checksum);
        } else {
            LOG.warn("Skipping unknown dpkg file [{}].", relativeAnchorFilePath);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(LinuxDistributionUtil.LinuxDistro distro, String packageName, String version, String arch) {
        if (distro != null && distro.id != null) {
            return String.format("pkg:deb/%s/%s@%s?arch=%s", distro.id, packageName, version, arch);
        }
        return null;
    }

}
