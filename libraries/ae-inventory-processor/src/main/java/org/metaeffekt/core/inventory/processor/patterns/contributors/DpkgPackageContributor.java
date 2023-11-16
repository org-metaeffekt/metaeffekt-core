/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    @SuppressWarnings("unused")
    public static class DpkgStatusFileEntry {
        private static final Logger LOG = LoggerFactory.getLogger(DpkgStatusFileEntry.class);

        /**
         * Static mapping from dpkg keys to our field names. Used for setting fields via reflection.
         */
        public static final Map<String, String> dpkgToKey = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("Architecture","architecture");
                put("Breaks","breaks");
                put("Build-Ids","buildIds");
                put("Built-Using","builtUsing");
                put("Cnf-Extra-Commands","cnfExtraCommands");
                put("Cnf-Priority-Bonus","cnfPriorityBonus");
                put("Cnf-Visible-Pkgname","cnfVisiblePkgname");
                put("Conffiles","conffiles");
                put("Config-Version","configVersion");
                put("Conflicts","conflicts");
                put("Depends","depends");
                put("Description","description");
                put("Efi-Vendor","efiVendor");
                put("Enhances","enhances");
                put("Essential","essential");
                put("Gstreamer-Decoders","gstreamerDecoders");
                put("Gstreamer-Elements","gstreamerElements");
                put("Gstreamer-Encoders","gstreamerEncoders");
                put("Gstreamer-Uri-Sinks","gstreamerUriSinks");
                put("Gstreamer-Uri-Sources","gstreamerUriSources");
                put("Gstreamer-Version","gstreamerVersion");
                put("Homepage","homepage");
                put("Important","important");
                put("Installed-Size","installedSize");
                put("Lua-Versions","luaVersions");
                put("Maintainer","maintainer");
                put("Multi-Arch","multiArch");
                put("Package","packageName");
                put("Pre-Depends","preDepends");
                put("Priority","priority");
                put("Protected","protectedAttr");
                put("Provides","provides");
                put("Recommends","recommends");
                put("Replaces","replaces");
                put("Section","section");
                put("Source","source");
                put("Status","status");
                put("Suggests","suggests");
                put("Version","version");
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

    @Override
    public boolean applies(String pathInContext) {
        // The default location under root is var/lib/dpkg.
        // if this part matches, make the bet that this is a debian file system and run package file inclusions.

        // could also match files from ./info/ as anchors?
        return pathInContext.equals("var/lib/dpkg/status");
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

    public boolean hasFileChanged(File baseDir, String relativeFilePath, String md5Checksum) {
        File toCheck = new File(baseDir, relativeFilePath);

        return hasFileChangedMd5(toCheck, md5Checksum);
    }

    public boolean hasFileChangedMd5(File toCheck, String knownChecksum) {
        String calculatedDigest;
        try (InputStream inputStream = Files.newInputStream(toCheck.toPath())) {
            calculatedDigest = DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            LOG.warn("Could not check digest for supposed package file at [{}]. Assuming changed.", toCheck.getPath());
            return true;
        }

        return knownChecksum.equals(calculatedDigest);
    }

    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorFilePath, String checksum) {
        File anchorFile = new File(baseDir, relativeAnchorFilePath);

        List<DpkgStatusFileEntry> entries;
        try {
            entries = readCompleteStatusFile(anchorFile);
        } catch (IOException e) {
            // crash if we can't read this file for some reason
            throw new RuntimeException(e);
        }

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
                    LOG.error("Can't create ComponentPattern: No m5sums file found for package [{}].",
                            entry.packageName);
                }
            }

            // prepare file list
            StringJoiner fileJoiner = new StringJoiner(",");
            try(Stream<String> lineStream = Files.lines(correspondingMd5sumsFile.toPath(), StandardCharsets.UTF_8)) {
                // stream lines so we don't need to preload to memory
                lineStream.forEachOrdered(line -> {
                    // definitely cut off the hash before adding...
                    int spaceSeparatorPosision = line.indexOf("  ");
                    if (spaceSeparatorPosision == -1) {
                        // skip invalid lines immediately.
                        return;
                    }
                    String hash = line.substring(0, spaceSeparatorPosision);
                    String toAdd = line.substring(spaceSeparatorPosision + 2);

                    // skip empty lines or invalid formatting
                    if (StringUtils.isBlank(toAdd)) {
                        return;
                    }

                    // err out if the md5sum isn't an md5sum
                    if (!hexStringPattern.matcher(hash).matches()) {
                        // this was not a real hash. should never happen. means splitting failed miserably.
                        LOG.error("Splitting failed miserably while reading line of dpkg md5sums at [{}].",
                                correspondingMd5sumsFile.getAbsolutePath());
                        return;
                    }

                    // use the checksum to detect and handle changed files
                    if (hasFileChanged(baseDir, toAdd, checksum)) {
                        // changed files might contain modifications and not truly be part of this package any more.

                        // handle this by skipping this line
                        LOG.debug("Component pattern from [{}] won't contain diverged entry [{}].",
                                correspondingMd5sumsFile, toAdd);
                        return;
                    }

                    fileJoiner.add(toAdd);
                });
            } catch (IOException e) {
                LOG.info("Could not read file list for entry with name [{}].", entry.packageName);
                continue;
            }

            // FIXME: review with JKR; these files have not been covered
            fileJoiner.add("var/lib/dpkg/info/" + entry.packageName + ":*");
            fileJoiner.add("var/lib/dpkg/info/" + entry.packageName + ".*");
            fileJoiner.add("var/lib/dpkg/info/" + entry.packageName);

            // create, fill and add the pattern
            ComponentPatternData componentPatternData = new ComponentPatternData();
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, entry.packageName);
            // add list of comma-separated paths
            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, fileJoiner.toString());
            // get version from the entry
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, entry.version);

            // try to fill "component part" in a plausible way
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART,
                    entry.packageName + "-" + entry.version);

            // set whatever this is in whatever way i think might be correct from looking at other contributors
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorFilePath);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);

            componentPatterns.add(componentPatternData);
        }

        return componentPatterns;
    }
}
