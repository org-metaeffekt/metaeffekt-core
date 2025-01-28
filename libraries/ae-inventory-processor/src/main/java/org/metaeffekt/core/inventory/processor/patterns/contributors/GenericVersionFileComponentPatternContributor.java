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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericVersionFileComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GenericVersionFileComponentPatternContributor.class);
    private static final String GENERIC_VERSION_TYPE = "generic-version";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("version.txt");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("version.txt") && !pathInContext.contains(".rsrc"); // TODO: check if libs_version.txt is a valid file and if it should be included
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        File versionFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!versionFile.exists()) {
            LOG.warn("Generic version file does not exist: {}", versionFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(versionFile.toPath())) {
            for (String line: lines.collect(Collectors.toList())) {
                Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
                Matcher versionMatcher = versionPattern.matcher(line);
                if (versionMatcher.find()) {
                    String version = versionMatcher.group();
                    String pluginName = versionFile.getParentFile().getName();
                    addComponent(components, pluginName, version, versionFile.getName(), anchorChecksum);
                }
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Could not process generic version file", e);
            return Collections.emptyList();
        }
    }

    private void addComponent(List<ComponentPatternData> components, String pluginName, String pluginVersion, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, pluginName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, pluginVersion);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, pluginName + "-" + pluginVersion);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, GENERIC_VERSION_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(pluginName, pluginVersion));

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

    private String buildPurl(String pluginName, String pluginVersion) {
        // this is self-made, not a real purl
        return "pkg:generic/" + pluginName + "@" + pluginVersion;
    }
}

