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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConanComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ConanComponentPatternContributor.class);
    private static final String CONAN_PACKAGE_TYPE = "conan";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("conaninfo.txt");
        add("conanfile.py");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("conaninfo.txt") || pathInContext.endsWith("conanfile.py");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File conanFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!conanFile.exists()) {
            LOG.warn("Conan package file does not exist: {}", conanFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(conanFile.toPath(), StandardCharsets.UTF_8)) {
            if (relativeAnchorPath.endsWith("conaninfo.txt")) {
                processConanInfo(lines, components, relativeAnchorPath, anchorChecksum);
            } else if (relativeAnchorPath.endsWith("conanfile.py")) {
                processConanFilePy(lines, components, relativeAnchorPath, anchorChecksum);
            }
            return components;
        } catch (IOException e) {
            LOG.warn("Error processing Conan package file", e);
            return Collections.emptyList();
        }
    }

    private void processConanInfo(Stream<String> lines, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        String packageName = null;
        String version = null;
        String namespace = null;
        String channel = null;
        for (String line : lines.collect(Collectors.toList())) {
            if (line.startsWith("[name]")) {
                packageName = line.trim();
            } else if (line.startsWith("[version]")) {
                version = line.trim();
            } else if (line.startsWith("[user]")) {
                namespace = line.trim();
            } else if (line.startsWith("[channel]")) {
                channel = line.trim();
            }
            if (packageName != null && version != null && namespace != null && channel != null) {
                addComponent(components, packageName, version, namespace, channel, relativeAnchorPath, anchorChecksum);
                packageName = null;
                version = null;
                namespace = null;
                channel = null;
            }
        }
    }

    private void processConanFilePy(Stream<String> lines, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        String packageName = null;
        String version = null;
        String namespace = null;
        String channel = null;
        for (String line : lines.collect(Collectors.toList())) {
            if (line.trim().startsWith("name =")) {
                packageName = line.split("=")[1].trim().replace("\"", "").replace("'", "");
            } else if (line.trim().startsWith("version =")) {
                version = line.split("=")[1].trim().replace("\"", "").replace("'", "");
            } else if (line.trim().startsWith("user =")) {
                namespace = line.split("=")[1].trim().replace("\"", "").replace("'", "");
            } else if (line.trim().startsWith("channel =")) {
                channel = line.split("=")[1].trim().replace("\"", "").replace("'", "");
            }
            if (packageName != null && version != null) {
                addComponent(components, packageName, version, namespace, channel, relativeAnchorPath, anchorChecksum);
                packageName = null;
                version = null;
                namespace = null;
                channel = null;
            }
        }
    }

    private void addComponent(List<ComponentPatternData> components, String packageName, String version, String namespace, String channel, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, CONAN_PACKAGE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version, namespace, channel));

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

    private String buildPurl(String name, String version, String namespace, String channel) {
        StringBuilder purl = new StringBuilder("pkg:conan/");
        if (namespace != null && !namespace.isEmpty()) {
            purl.append(namespace).append("/");
        }
        purl.append(name).append("@").append(version);
        if (channel != null && !channel.isEmpty()) {
            purl.append("?channel=").append(channel);
        }
        return purl.toString();
    }
}

