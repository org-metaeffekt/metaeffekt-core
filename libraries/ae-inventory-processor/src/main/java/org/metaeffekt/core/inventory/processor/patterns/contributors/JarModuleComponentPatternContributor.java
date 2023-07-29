/*
 * Copyright 2009-2022 the original author or authors.
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
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class JarModuleComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(File contextBaseDir, String file) {
        file = file.toLowerCase();
        return (file.startsWith("meta-inf/maven/") && file.endsWith("/pom.xml"));
    }

    @Override
    public List<ComponentPatternData> contribute(File contextBaseDir,
                 String anchorRelPath, String anchorAbsPath, String anchorChecksum) {
        JarInspector jarInspector = new JarInspector();

        File referenceFile = new File(contextBaseDir, anchorRelPath);
        File mavenDir = referenceFile.getParentFile();

        final File pomXmlFile = FileUtils.findSingleFile(mavenDir, "pom.xml");
        final File pomPropertiesFile = FileUtils.findSingleFile(mavenDir, "pom.properties");

        final Artifact artifact = new Artifact();

        Artifact fromXml = null;
        if (pomXmlFile != null) {
            try (InputStream in = Files.newInputStream(pomXmlFile.toPath())) {
                fromXml = jarInspector.getArtifactFromPomXml(artifact, in, anchorRelPath);
            } catch (IOException e) {
            }
        }

        Artifact fromProperties = null;
        if (pomPropertiesFile != null) {
            try (InputStream in = Files.newInputStream(pomPropertiesFile.toPath())) {
                fromProperties = jarInspector.getArtifactFromPomProperties(artifact, in, anchorRelPath);
            } catch (IOException e) {
            }
        }

        // cherry-pick attributes from pom / properties
        mergeArtifact(artifact, fromXml);
        mergeArtifact(artifact, fromProperties);

        artifact.deriveArtifactId();

        String id = artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
        artifact.setId(id);

        String includePattern = "**/*";

        final File anchorFile = new File(contextBaseDir, anchorRelPath);
        final File anchorParentFile = anchorFile.getParentFile();

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/node_modules/**/*" + "," +
                "**/bower_components/**/*" + "," +
                "**/*.jar" + "," +
                "**/*.so*" + "," +
                "**/*.dll");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePattern);

        // contribute groupid (consider also other attributes)
        componentPatternData.set("Group Id", artifact.getGroupId());

        return Collections.singletonList(componentPatternData);
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
