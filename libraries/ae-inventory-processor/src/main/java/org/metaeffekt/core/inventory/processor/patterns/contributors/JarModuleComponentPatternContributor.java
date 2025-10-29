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

import org.apache.commons.lang3.StringUtils;
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
import java.util.Properties;

import static org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer.LocaleConstants.PATH_LOCALE;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

public class JarModuleComponentPatternContributor extends ComponentPatternContributor {

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/pom.xml");
        add("/meta-inf/manifest.mf");
        add("/meta-inf/build-info.properties");
    }});

    private static final List<String> ROOT_SUFFIXES = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/meta-inf/");
        add("/META-INF/");
    }});

    @Override
    public boolean applies(String pathInContext) {
        pathInContext = pathInContext.toLowerCase(PATH_LOCALE);

        // check for manifest
        if (pathInContext.endsWith("/manifest.mf")) return true;
        if (pathInContext.endsWith("/build-info.properties")) return true;

        return (pathInContext.contains("/meta-inf/maven/") && pathInContext.endsWith("/pom.xml"));
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        return contribute(baseDir, relativeAnchorPath, anchorChecksum, new EvaluationContext());
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum, EvaluationContext context) {

        final JarInspector jarInspector = new JarInspector();

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile();

        final String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, ROOT_SUFFIXES);
        final File contextBaseDir = new File(baseDir, virtualRootPath);
        final String folderName = contextBaseDir.getName();

        String contextSemaphore = getClass().getCanonicalName() + "-" + virtualRootPath;

        if (context.isProcessed(contextSemaphore)) {
            return Collections.emptyList();
        }

        File pomXmlFile = FileUtils.findSingleFile(anchorParentDir, "pom.xml");
        if (pomXmlFile == null) {
            pomXmlFile = FileUtils.findSingleFile(contextBaseDir, "**/pom.xml");
        }
        File pomPropertiesFile = FileUtils.findSingleFile(anchorParentDir, "pom.properties");
        if (pomPropertiesFile == null) {
            pomPropertiesFile = FileUtils.findSingleFile(contextBaseDir, "**/pom.properties");
        }
        final File manifestFile = FileUtils.findSingleFile(anchorParentDir, "MANIFEST.MF");
        final File buildPropertiesInfoFile = FileUtils.findSingleFile(anchorParentDir, "build-info.properties");

        final Artifact artifact = new Artifact();

        Artifact fromXml = null;
        if (pomXmlFile != null) {
            try (InputStream in = Files.newInputStream(pomXmlFile.toPath())) {
                fromXml = jarInspector.getArtifactFromPomXml(artifact, in, relativeAnchorPath);
            } catch (IOException ignored) {
            }
        }

        Artifact fromProperties = null;
        if (pomPropertiesFile != null) {
            try (InputStream in = Files.newInputStream(pomPropertiesFile.toPath())) {
                fromProperties = jarInspector.getArtifactFromPomProperties(artifact, in, relativeAnchorPath);
            } catch (IOException ignored) {
            }
        }

        Artifact fromManifest = null;
        if (manifestFile != null) {
            try (InputStream in = Files.newInputStream(manifestFile.toPath())) {
                fromManifest = jarInspector.getArtifactFromManifest(artifact, in, relativeAnchorPath);
            } catch (IOException ignored) {
            }
        }

        Artifact fromBuildInfoProperties = null;
        if (buildPropertiesInfoFile != null) {
            try (InputStream in = Files.newInputStream(buildPropertiesInfoFile.toPath())) {
                fromBuildInfoProperties = getArtifactFromBuildInfoProperties(artifact, in);
            } catch (IOException ignored) {
            }
        }

        // cherry-pick attributes from pom / properties
        mergeArtifact(artifact, fromXml);
        mergeArtifact(artifact, fromProperties);
        mergeArtifact(artifact, fromManifest);

        mergeArtifact(artifact, fromBuildInfoProperties);

        final boolean isManifestAnchor = (folderName + "/META-INF/MANIFEST.MF").equalsIgnoreCase(relativeAnchorPath);

        final String artifactId = artifact.get(JarInspector.ATTRIBUTE_KEY_ARTIFACT_ID);

        // FIXME: revise
        if (StringUtils.isEmpty(artifact.getId())) {
            String id = null;
            if (artifactId == null && isManifestAnchor) {
                id = folderName;
                if (id.startsWith("[")) {
                    id = id.substring(1, id.length() - 1);
                }
            } else {
                if (artifactId != null) {
                    final String packaging = artifact.get("Packaging");
                    if (packaging == null) {
                        id = artifactId + "-" + artifact.getVersion();
                    } else {
                        id = artifactId + "-" + artifact.getVersion() + "." + JarInspector.deriveSuffix(packaging);
                    }
                }
            }
            artifact.setId(id);
        }

        if (artifact.getId() == null) {
            return Collections.emptyList();
        }

        // NOTE:  in case of a single pom the whole content is used; in case of multiple poms the include covers only
        // the groupid-covered content
        String includePattern = "**/*";

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile));
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.deriveArtifactId());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/package-lock.json" + ", " +
                "**/package.json" + ", " +
                "**/node_modules/**/*" + ", " +
                "**/bower_components/**/*" + ", " +
                "**/*.jar" + ", " +
                "**/*.xar" + ", " +
                "**/*.xed" + ", " +
                "**/*.so*" + ", " +
                "**/*.exe" + ", " +
                "**/*.dll");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePattern);

        // contribute groupid (consider also other attributes)
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_GROUP_ID, artifact.getGroupId());

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "jar-module");

        context.registerProcessed(contextSemaphore);
        return Collections.singletonList(componentPatternData);
    }


    private Artifact getArtifactFromBuildInfoProperties(Artifact artifact, InputStream in) throws IOException {
        final Properties p = new Properties();
        p.load(in);

        final String artifactId = p.getProperty("build.artifact");
        final String groupId = p.getProperty("build.group");
        final String version = p.getProperty("build.version");

        if (StringUtils.isEmpty(artifact.getId())) {
            if (StringUtils.isNotEmpty(artifactId) && StringUtils.isNotEmpty(version)) {
                artifact.setId(artifactId + "-" + version);
            }
        }
        if (StringUtils.isEmpty(artifact.getComponent())) {
            artifact.setComponent(artifactId);
        }
        if (StringUtils.isEmpty(artifact.getVersion())) {
            artifact.setVersion(version);
        }
        if (StringUtils.isEmpty(artifact.getGroupId())) {
            artifact.setGroupId(groupId);
        }

        return artifact;
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
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
