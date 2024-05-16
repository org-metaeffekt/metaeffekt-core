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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class AlpmPackageContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(AlpmPackageContributor.class);
    private static final String ALPM_PACKAGE_TYPE = "alpm";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/desc");
        add("/files");
    }});

    @Override
    public boolean applies(String pathInContext) {
        // this contributor applies if it's the ALPM package database directory
        return pathInContext.contains("var/lib/pacman/local");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File packageDir = new File(baseDir, relativeAnchorPath).getParentFile();
        List<ComponentPatternData> components = new ArrayList<>();

        if (!packageDir.exists() || !packageDir.isDirectory()) {
            LOG.warn("ALPM package directory does not exist: {}", packageDir.getAbsolutePath());
            return components;
        }

        try {
            Path virtualRoot = new File(virtualRootPath).toPath();
            Path relativeAnchorFile = new File(relativeAnchorPath).toPath();

            String packageName = null;
            String version = null;
            String architecture = null;
            StringJoiner includePatterns = new StringJoiner(",");

            // read the desc file
            File descFile = new File(packageDir, "desc");
            if (descFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(descFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("%NAME%")) {
                            packageName = reader.readLine().trim();
                        } else if (line.startsWith("%VERSION%")) {
                            version = reader.readLine().trim();
                        } else if (line.startsWith("%ARCH%")) {
                            architecture = reader.readLine().trim();
                        }
                    }
                }
            }

            // read the files file
            File filesFile = new File(packageDir, "files");
            if (filesFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(filesFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.equals("%FILES%")) {
                            includePatterns.add(line.trim());
                        }
                    }
                }
            }

            if (includePatterns.length() == 0) {
                LOG.warn("No include patterns found for package: {}-{}-{}", packageName, version, architecture);
                includePatterns.add(relativeAnchorPath);
            }

            if (packageName != null && version != null && architecture != null) {
                processCollectedData(components, packageName, version, architecture, includePatterns.toString(), virtualRoot.relativize(relativeAnchorFile).toString(), anchorChecksum);
            }
        } catch (Exception e) {
            LOG.error("Error processing ALPM package directory: {}", packageDir.getAbsolutePath(), e);
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
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, ALPM_PACKAGE_TYPE);
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
        return "pkg:alpm/arch/" + name + "@" + version + "?arch=" + architecture;
    }
}

