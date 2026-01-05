/*
 * Copyright 2009-2026 the original author or authors.
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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ExeComponentPatternContributor extends ComponentPatternContributor {

    private static final String EXE_SOURCE_TYPE = "exe";
    private static final String[] VERSION_DELIMITERS = new String[] { "-", "_" };

    private static final List<String> SUFFIXES = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".exe]/.rdata");
        add(".exe]/.idata");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".exe]/.rdata") || pathInContext.endsWith(".exe]/.idata");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File detectedFile = new File(baseDir, relativeAnchorPath);

        // step up path until we match the exe pattern
        File unpackedExeDir = detectedFile.getParentFile();
        while (!unpackedExeDir.getName().toLowerCase(Locale.ROOT).endsWith(".exe]")) {
            unpackedExeDir = unpackedExeDir.getParentFile();
        }

        final File versionFile = FileUtils.findSingleFile(unpackedExeDir, ".rsrc/**/version.txt", ".rsrc/version.txt");

        return deriveComponents(baseDir, versionFile, unpackedExeDir, relativeAnchorPath, anchorChecksum);
    }

    private ComponentPatternData deriveComponentPatternData(File baseDir, File unpackedExeDir, VersionFileData versionFileData, String relativeAnchorPath, String anchorChecksum) {
        final ComponentPatternData cpd = new ComponentPatternData();

        final String unpackedExeDirAnchorPath = FileUtils.asRelativePath(unpackedExeDir.getParentFile(), new File(baseDir, relativeAnchorPath));

        // version is only provided via version file
        final String version = versionFileData.getProductVersion();

        final String name = unpackedExeDir.getName();
        final String exeName = name.substring(1, name.length() - 1);
        final String exeNameWithoutExtensionAndVersion = derivePurifiedName(exeName, version);
        final String productName = versionFileData.getProductName();

        if (productName != null) {
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, productName);
        } else {
            // alternatively fall back on purified name
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, exeNameWithoutExtensionAndVersion);
        }

        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, exeName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, exeName);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, unpackedExeDirAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_EXECUTABLE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, EXE_SOURCE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(versionFileData, exeNameWithoutExtensionAndVersion));

        // include all in dir except for embedded exe files; identified separately
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, name + "/**/*");
        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, name + "/**/*.exe");

        return cpd;
    }

    private static String derivePurifiedName(String exeName, String version) {
        // compute pure name (no extension, no version)
        String exeNameWithoutExtensionAndVersion = exeName.replaceAll("(.*)\\.(exe|EXE)", "$1");
        if (version != null && exeNameWithoutExtensionAndVersion.contains(version)) {
            for (String delimiter : VERSION_DELIMITERS) {
                final int index = exeNameWithoutExtensionAndVersion.lastIndexOf(delimiter + version);
                if (index != -1) {
                    exeNameWithoutExtensionAndVersion = exeNameWithoutExtensionAndVersion.substring(0, index);
                }
            }
        }
        return exeNameWithoutExtensionAndVersion;
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIXES;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(VersionFileData versionFileData, String exeNameWithoutExtensionAndVersion) {
        final String version = versionFileData.getProductVersion();
        final String componentName = versionFileData.getProductName() == null ? null :
                versionFileData.getProductName().toLowerCase(Locale.US).replace(" ", "-");
        if (componentName == null && version == null) {
            return "pkg:generic/" + exeNameWithoutExtensionAndVersion;
        } else if (version == null) {
            return "pkg:generic/" + componentName +  "/" + exeNameWithoutExtensionAndVersion;
        } else if (componentName == null) {
            return "pkg:generic/" + exeNameWithoutExtensionAndVersion + "@" + version;
        }
        return "pkg:generic/" + componentName +  "/" + exeNameWithoutExtensionAndVersion + "@" + version;
    }

    private List<ComponentPatternData> deriveComponents(File baseDir, File versionFile, File unpackedExeDir, String relativeAnchorPath, String anchorChecksum) {
        final List<ComponentPatternData> components = new ArrayList<>();

        final VersionFileData versionFileData = parseVersionFileIfExists(versionFile);
        components.add(deriveComponentPatternData(baseDir, unpackedExeDir, versionFileData, relativeAnchorPath, anchorChecksum));

        return components;
    }

    private VersionFileData parseVersionFileIfExists(File versionFile) {
        VersionFileData data = new VersionFileData();
        if (versionFile != null && versionFile.exists()) {
            try (Stream<String> lines = Files.lines(versionFile.toPath(), StandardCharsets.UTF_16LE)) {

                for (String line : lines.collect(Collectors.toList())) {
                    Matcher nameMatcher = VersionFileData.PRODUCT_NAME_PATTERN.matcher(line);
                    if (nameMatcher.find()) {
                        data.setProductName(nameMatcher.group(1).replace("\0", "").trim());
                    }

                    Matcher originalFilenameMatcher = VersionFileData.ORIGINAL_FILENAME_PATTERN.matcher(line);
                    if (originalFilenameMatcher.find()) {
                        data.setOriginalFilename(originalFilenameMatcher.group(1).replace("\0", "").trim());
                    }

                    Matcher versionMatcher = VersionFileData.PRODUCT_VERSION_PATTERN.matcher(line);
                    if (versionMatcher.find()) {
                        data.setProductVersion(versionMatcher.group(1).replace("\0", "").trim());
                    }

                    if (data.isComplete()) {
                        break;
                    }
                }

                if (!data.isComplete()) {
                    log.warn("Could not find ProductName or ProductVersion in MSI version file: {}", versionFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Error reading MSI version file: {}", versionFile.getAbsolutePath(), e);
            }
        }

        // we return the potentially partially filled data
        return data;
    }

    @Getter
    @Setter
    private static class VersionFileData {

        private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile("VALUE\\s+\"ProductName\"\\s*,\\s*\"([^\"]*)\"");
        private static final Pattern ORIGINAL_FILENAME_PATTERN = Pattern.compile("VALUE\\s+\"OriginalFilename\"\\s*,\\s*\"([^\"]*)\"");
        private static final Pattern PRODUCT_VERSION_PATTERN = Pattern.compile("VALUE\\s+\"ProductVersion\"\\s*,\\s*\"([^\"]*)\"");

        private String productName;
        private String productVersion;
        private String originalFilename;

        boolean isComplete() {
            return productName != null && productVersion != null && originalFilename != null;
        }
    }

}

