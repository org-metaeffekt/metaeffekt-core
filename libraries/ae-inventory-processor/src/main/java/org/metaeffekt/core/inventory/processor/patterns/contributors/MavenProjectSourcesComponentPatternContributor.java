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

import org.metaeffekt.core.inventory.processor.adapter.MavenPomAdapter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final List<ComponentPatternData> components = new ArrayList<>();

        try {
            MavenPomAdapter mavenPomAdapter = new MavenPomAdapter(anchorFile);

            final String artifactId = mavenPomAdapter.resolveArtifactId();
            final String groupId = mavenPomAdapter.resolveGroupId();
            final String version = mavenPomAdapter.resolveVersion();

            final String purl = MavenPomAdapter.buildPurl(groupId, artifactId, version);

            final ComponentPatternData cpd = new ComponentPatternData();
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifactId);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_GROUP_ID, groupId);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            if (artifactId != null && version != null) {
                cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, artifactId + "-" + version);
            } else {
                cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, artifactId);
            }

            cpd.set(Artifact.Attribute.PURL, purl);

            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
            cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
            cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

            cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MAVEN_PROJECT_SOURCE_TYPE);

            components.add(cpd);

            // NOTE: evaluation of dependencies is not performed during the
            //   extraction phase. Interpretation and evaluation of the Maven POM
            //   is subject to the resolver phase (where access to parent-poms is
            //   more likely.

        } catch (Exception e) {
            LOG.warn("Unable to parse maven project model [{}]: [{}]", anchorFile.getAbsolutePath(), e.getMessage());
        }

        return components;
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
