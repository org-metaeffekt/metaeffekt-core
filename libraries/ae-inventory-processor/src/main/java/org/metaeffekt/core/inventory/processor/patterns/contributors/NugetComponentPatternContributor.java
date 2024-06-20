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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NugetComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(NugetComponentPatternContributor.class);
    private static final String DOTNET_PACKAGE_TYPE = "nuget";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".nuspec");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".nuspec");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File nuspecFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!nuspecFile.exists()) {
            LOG.warn("DotNet package file does not exist: {}", nuspecFile.getAbsolutePath());
            return components;
        }

        try {
            processNuspecFile(components, relativeAnchorPath, anchorChecksum);
        } catch (Exception e) {
            LOG.error("Error processing DotNet package file", e);
        }

        return components;
    }

    private void processNuspecFile(List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        String packageName;
        String version;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(relativeAnchorPath));

            NodeList metadataList = doc.getElementsByTagName("metadata");
            if (metadataList.getLength() > 0) {
                for (int i = 0; i < metadataList.getLength(); i++) {
                    Element metadata = (Element) metadataList.item(i);
                    packageName = metadata.getElementsByTagName("id").item(i).getTextContent();
                    version = metadata.getElementsByTagName("version").item(i).getTextContent();
                    addComponent(components, packageName, version, relativeAnchorPath, anchorChecksum);
                }
            }
        } catch (Exception e) {
            LOG.error("Error parsing .nuspec file", e);
        }
    }

    private void addComponent(List<ComponentPatternData> components, String packageName, String version, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, DOTNET_PACKAGE_TYPE);
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
        return "pkg:nuget/" + name + "@" + version;
    }
}

