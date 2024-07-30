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
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JenkinsPluginsComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsPluginsComponentPatternContributor.class);
    private static final String JENKINS_PLUGIN_TYPE = "jenkins-plugin";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".jpi");
        add(".hpi");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".jpi") || pathInContext.endsWith(".hpi");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File pluginFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!pluginFile.exists()) {
            LOG.warn("Jenkins plugin file does not exist: {}", pluginFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(pluginFile.toPath()))) {
            Manifest manifest = jarInputStream.getManifest();
            if (manifest != null) {
                String pluginName = manifest.getMainAttributes().getValue("Extension-Name");
                String pluginVersion = manifest.getMainAttributes().getValue("Plugin-Version");
                if (pluginName != null && pluginVersion != null) {
                    addComponent(components, pluginName, pluginVersion, relativeAnchorPath, anchorChecksum);
                } else {
                    LOG.warn("Missing Extension-Name or Plugin-Version in manifest for plugin file: {}", pluginFile.getAbsolutePath());
                }
            } else {
                LOG.warn("Manifest not found in plugin file: {}", pluginFile.getAbsolutePath());
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error processing Jenkins plugin file", e);
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
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, JENKINS_PLUGIN_TYPE);
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
        return "pkg:jenkins/" + pluginName + "@" + pluginVersion;
    }
}

