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

import org.metaeffekt.core.inventory.processor.inspector.JarInspector;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer.localeConstants.PATH_LOCALE;

public class JarModuleComponentPatternContributor extends ComponentPatternContributor {

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/pom.xml");
    }});

    @Override
    public boolean applies(String pathInContext) {
        pathInContext = pathInContext.toLowerCase(PATH_LOCALE);
        return (pathInContext.contains("/meta-inf/maven/") && pathInContext.endsWith("/pom.xml"));
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        JarInspector jarInspector = new JarInspector();

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile();

        final File pomXmlFile = FileUtils.findSingleFile(anchorParentDir, "pom.xml");
        final File pomPropertiesFile = FileUtils.findSingleFile(anchorParentDir, "pom.properties");

        final Artifact artifact = new Artifact();

        Artifact fromXml = null;
        if (pomXmlFile != null) {
            try (InputStream in = Files.newInputStream(pomXmlFile.toPath())) {
                fromXml = jarInspector.getArtifactFromPomXml(artifact, in, relativeAnchorPath);
            } catch (IOException e) {
            }
        }

        Artifact fromProperties = null;
        if (pomPropertiesFile != null) {
            try (InputStream in = Files.newInputStream(pomPropertiesFile.toPath())) {
                fromProperties = jarInspector.getArtifactFromPomProperties(artifact, in, relativeAnchorPath);
            } catch (IOException e) {
            }
        }

        // cherry-pick attributes from pom / properties
        mergeArtifact(artifact, fromXml);
        mergeArtifact(artifact, fromProperties);

        final String artifactId = artifact.get(JarInspector.ATTRIBUTE_KEY_ARTIFACT_ID);
        String id = artifactId + "-" + artifact.getVersion() + "." + artifact.get("Packaging");
        artifact.setId(id);

        final File contextBaseDir = new File(baseDir, relativeAnchorPath.substring(0, relativeAnchorPath.indexOf("/META-INF/")));

        final String[] pomFiles = FileUtils.scanForFiles(contextBaseDir, "META-INF/maven/**/pom.xml,WEB-INF/maven/**/pom.xml", null);

        // NOTE:  in case of a single pom the whole content is used; in case of multiple poms the include covers only
        // the groupid-covered content
        final String includePattern = (pomFiles.length == 1) ?
                "**/*" :
                "**/" + artifact.getGroupId() + "/**/*,**/" + artifactId + "/**/*";

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile));
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/node_modules/**/*" + "," +
                "**/bower_components/**/*" + "," +
                "**/*.jar" + "," +
                "**/*.xar" + "," +
                "**/*.xed" + "," +
                "**/*.so*" + "," +
                "**/*.dll");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePattern);

        // contribute groupid (consider also other attributes)
        componentPatternData.set("Group Id", artifact.getGroupId());

        // FIXME: check what type represents (a characterisitic of the artifact or its embedding)
        componentPatternData.set(Constants.KEY_TYPE, "jar-component");

        return Collections.singletonList(componentPatternData);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    private void mergeArtifact(Artifact artifact, Artifact fromXml) {
        if (fromXml != null) {
            for (String attribute : fromXml.getAttributes()) {
                artifact.set(attribute, eliminateValuesWithPlaceholders(artifact.get(attribute)));
            }
            artifact.merge(fromXml);
        }
    }

    private String eliminateValuesWithPlaceholders(String string) {
        return string == null || string.contains("$") ? null : string;
    }
}
