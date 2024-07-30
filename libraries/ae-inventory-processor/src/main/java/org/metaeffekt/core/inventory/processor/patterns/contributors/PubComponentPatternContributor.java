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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PubComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(PubComponentPatternContributor.class);
    private static final String DART_PACKAGE_TYPE = "pub";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("pubspec.yaml");
        add("pubspec.lock");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("pubspec.yaml") || pathInContext.endsWith("pubspec.lock");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File pubspecFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!pubspecFile.exists()) {
            LOG.warn("Dart package file does not exist: {}", pubspecFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(pubspecFile.toPath(), StandardCharsets.UTF_8)) {
            if (relativeAnchorPath.endsWith("pubspec.yaml")) {
                processPubspecYaml(lines, components, relativeAnchorPath, anchorChecksum);
            } else if (relativeAnchorPath.endsWith("pubspec.lock")) {
                processPubspecLock(lines, components, relativeAnchorPath, anchorChecksum);
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error processing Dart package file", e);
            return Collections.emptyList();
        }
    }

    private void processPubspecYaml(Stream<String> lines, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        String packageName = null;
        String version = null;
        for (String line : lines.collect(Collectors.toList())) {
            if (line.trim().startsWith("name:")) {
                packageName = line.split(":")[1].trim();
            } else if (line.trim().startsWith("version:")) {
                version = line.split(":")[1].trim();
            }
            if (packageName != null && version != null) {
                addComponent(components, packageName, version, relativeAnchorPath, anchorChecksum);
                packageName = null;
                version = null;
            }
        }
    }

    private void processPubspecLock(Stream<String> lines, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        String packageName = null;
        String version = null;
        for (String line : lines.collect(Collectors.toList())) {
            if (line.trim().startsWith("name:")) {
                packageName = line.split(":")[1].trim();
            } else if (line.trim().startsWith("version:")) {
                version = line.split(":")[1].trim();
            }
            if (packageName != null && version != null) {
                addComponent(components, packageName, version, relativeAnchorPath, anchorChecksum);
                packageName = null;
                version = null;
            }
        }
    }

    private void addComponent(List<ComponentPatternData> components, String packageName, String version, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, DART_PACKAGE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version));

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

    private String buildPurl(String name, String version) {
        return "pkg:pub/" + name + "@" + version;
    }
}

