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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ExeComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ExeComponentPatternContributor.class);

    private static final String EXE_SOURCE_TYPE = "exe";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".exe]/.rdata");
        add(".exe]/.idata");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".exe]/.rdata") || pathInContext.endsWith(".exe]/.idata");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        final File detectedFile = new File(baseDir, relativeAnchorPath);
        File virtualRoot = new File(baseDir, virtualRootPath);
        File versionFile = FileUtils.findSingleFile(virtualRoot, ".rsrc/**/version.txt", ".rsrc/version.txt");
        final List<ComponentPatternData> components = new ArrayList<>();

        File unpackedExeDir = detectedFile.getParentFile();
        while (!unpackedExeDir.getName().toLowerCase(Locale.ROOT).endsWith(".exe]")) {
            unpackedExeDir = unpackedExeDir.getParentFile();
        }

        checkForVersionFile(versionFile, unpackedExeDir, components, relativeAnchorPath, anchorChecksum);
        return components;
    }

    private void addComponent(File parentDir, List<ComponentPatternData> components, String productName, String productVersion, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, productVersion);
        final String name = parentDir.getName();
        String exeName = name.substring(1, name.length() - 1);
        String exeNameWithoutExtension = exeName.replace(".exe", "");
        if (productVersion != null && exeNameWithoutExtension.contains(productVersion)) {
            exeNameWithoutExtension = exeNameWithoutExtension.replace(productVersion, "");
        }
        if (productName != null) {
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, productName);
        } else {
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, exeNameWithoutExtension);
        }
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, exeName);


        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);

        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        final File fullRelativeAnchorFile = new File(relativeAnchorPath).getParentFile();

        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "/" + fullRelativeAnchorFile.getPath() + "/**/*");
        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.exe");


        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, EXE_SOURCE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(productName, productVersion, exeNameWithoutExtension));

        components.add(cpd);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String productName, String productVersion, String exeName) {
        if (productName == null && productVersion == null) {
            return "pkg:generic/" + exeName;
        } else if (productVersion == null) {
            return "pkg:generic/" + productName +  "/" + exeName;
        } else if (productName == null) {
            return "pkg:generic/" + exeName + "@" + productVersion;
        }
        return "pkg:generic/" + productName +  "/" + exeName + "@" + productVersion;
    }

    private void checkForVersionFile(File versionFile, File unpackedExeDir, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        if (versionFile != null && versionFile.exists()) {
            try (Stream<String> lines = Files.lines(versionFile.toPath(), StandardCharsets.UTF_16LE)) {
                String productName = null;
                String productVersion = null;
                String originalFilename = null;

                Pattern productNamePattern = Pattern.compile("VALUE\\s+\"ProductName\"\\s*,\\s*\"([^\"]*)\"");
                Pattern originalFilenamePattern = Pattern.compile("VALUE\\s+\"OriginalFilename\"\\s*,\\s*\"([^\"]*)\"");
                Pattern productVersionPattern = Pattern.compile("VALUE\\s+\"ProductVersion\"\\s*,\\s*\"([^\"]*)\"");

                for (String line : lines.collect(Collectors.toList())) {
                    Matcher nameMatcher = productNamePattern.matcher(line);
                    if (nameMatcher.find()) {
                        productName = nameMatcher.group(1).replace("\0", "").trim();
                    }

                    Matcher originalFilenameMatcher = originalFilenamePattern.matcher(line);
                    if (originalFilenameMatcher.find()) {
                        originalFilename = originalFilenameMatcher.group(1).replace("\0", "").trim();
                    }

                    Matcher versionMatcher = productVersionPattern.matcher(line);
                    if (versionMatcher.find()) {
                        productVersion = versionMatcher.group(1).replace("\0", "").trim();
                    }

                    // if both values are found, we can stop processing further lines
                    if (originalFilename != null && productVersion != null) {
                        break;
                    }
                }

                if (originalFilename != null && productVersion != null) {
                    addComponent(unpackedExeDir, components, productName, productVersion, relativeAnchorPath, anchorChecksum);
                } else {
                    LOG.warn("Could not find ProductName or ProductVersion in MSI version file: {}", versionFile.getAbsolutePath());
                }
            } catch (Exception e) {
                LOG.warn("Error reading MSI version file: {}", versionFile.getAbsolutePath(), e);
            }
        } else {
            addComponent(unpackedExeDir, components, null, null, relativeAnchorPath, anchorChecksum);
        }
    }
}

