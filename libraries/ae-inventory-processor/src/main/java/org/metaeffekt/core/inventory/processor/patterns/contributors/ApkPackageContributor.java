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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApkPackageContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ApkPackageContributor.class);
    private static final String APK_PACKAGE_TYPE = "apk";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("lib/apk/db/installed");
    }});

    @Override
    public boolean applies(String pathInContext) {
        // this contributor applies if it's the APK package database
        return pathInContext.endsWith("lib/apk/db/installed");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File apkDbFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();
        // get path of virtual root
        Path virtualRoot = new File(virtualRootPath).toPath();
        Path relativeAnchorFile = new File(relativeAnchorPath).toPath();

        if (!apkDbFile.exists()) {
            LOG.warn("APK database file does not exist: {}", apkDbFile.getAbsolutePath());
            return components;
        }

        try (Stream<String> lineStream = Files.lines(apkDbFile.toPath(), StandardCharsets.UTF_8)) {
            String packageName = null;
            String version = null;
            String architecture = null;
            StringJoiner includePatterns = new StringJoiner(",");
            String currentFolder = null;
            for (String line : lineStream.collect(Collectors.toList())) {
                if (line.startsWith("P:")) {
                    packageName = line.substring(2).trim();
                } else if (line.startsWith("V:")) {
                    version = line.substring(2).trim();
                } else if (line.startsWith("A:")) {
                    architecture = line.substring(2).trim();
                } else if (line.startsWith("F:")) {
                    currentFolder = line.substring(2).trim();
                    includePatterns.add(currentFolder);
                } else if (line.startsWith("R:")) {
                    includePatterns.add(currentFolder + "/" + line.substring(2).trim());
                } else if (line.isEmpty()) {
                    // end of package block, process collected data
                    if (packageName != null && version != null && architecture != null) {
                        if (includePatterns.length() > 0) {
                            processCollectedData(components, packageName, version, architecture, includePatterns.toString(), virtualRoot.relativize(relativeAnchorFile).toString(), anchorChecksum);
                            includePatterns = new StringJoiner(",");
                        } else {
                            LOG.warn("No include patterns found for package: {}-{}-{}", packageName, version, architecture);
                            processCollectedData(components, packageName, version, architecture, relativeAnchorPath, virtualRoot.relativize(relativeAnchorFile).toString(), anchorChecksum);
                        }
                    }
                    packageName = null;
                    version = null;
                    architecture = null;
                    currentFolder = null;
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing APK database file", e);
        }

        return components;
    }

    private void processCollectedData(List<ComponentPatternData> components, String packageName, String version, String architecture, String includePatterns, String path, String checksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, path);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, APK_PACKAGE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version, architecture));
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

    private String buildPurl(String name, String version, String architecture) {
        return "pkg:apk/alpine/" + name + "@" + version + "?arch=" + architecture;
    }
}
