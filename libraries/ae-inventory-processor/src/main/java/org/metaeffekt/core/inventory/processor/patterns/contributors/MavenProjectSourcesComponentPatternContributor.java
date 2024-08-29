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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MavenProjectSourcesComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(MavenProjectSourcesComponentPatternContributor.class);
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("pom.xml");
    }});

    public static final String MAVEN_PROJECT_SOURCE_TYPE = "maven-project-source";

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("pom.xml") && !pathInContext.contains("META-INF");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final List<ComponentPatternData> components = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(anchorFile)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(fis);

            Properties properties = model.getProperties();

            String groupId = resolveProperty(model.getGroupId(), properties, model);
            String artifactId = model.getArtifactId();
            String version = resolveProperty(model.getVersion(), properties, model);

            String purl = buildPurl(groupId, artifactId, version);

            ComponentPatternData cpd = new ComponentPatternData();
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifactId);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, artifactId + "-" + version);
            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
            cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
            cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
            cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MAVEN_PROJECT_SOURCE_TYPE);
            cpd.set(Artifact.Attribute.PURL, purl);
            components.add(cpd);

            // Add dependencies
            List<Dependency> dependencies = model.getDependencies();
            for (Dependency dependency : dependencies) {
                String depGroupId = resolveProperty(dependency.getGroupId(), properties, model);
                String depArtifactId = dependency.getArtifactId();
                String depVersion = resolveProperty(dependency.getVersion(), properties, model);
                String depPurl = buildPurl(depGroupId, depArtifactId, depVersion);
                String depScope = dependency.getScope();

                ComponentPatternData depCpd = new ComponentPatternData();
                depCpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, depArtifactId);
                depCpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, depVersion);
                depCpd.set(ComponentPatternData.Attribute.COMPONENT_PART, depArtifactId + "-" + depVersion);
                depCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
                depCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                depCpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
                depCpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                depCpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MAVEN_PROJECT_SOURCE_TYPE);
                depCpd.set(Constants.KEY_SCOPE, depScope);
                depCpd.set(Artifact.Attribute.PURL, depPurl);
                components.add(depCpd);
            }

        } catch (Exception e) {
            LOG.warn("Unable to parse maven project source [{}]: [{}]", anchorFile.getAbsolutePath(), e.getMessage());
        }

        return components;
    }

    private String resolveProperty(String value, Properties properties, Model model) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            String key = value.substring(2, value.length() - 1);
            return properties.getOrDefault(key, model.getProperties().getProperty(key)).toString();
        }
        return value;
    }

    private String buildPurl(String groupId, String artifactId, String version) {
        return String.format("pkg:maven/%s/%s@%s", groupId, artifactId, version);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }
}
