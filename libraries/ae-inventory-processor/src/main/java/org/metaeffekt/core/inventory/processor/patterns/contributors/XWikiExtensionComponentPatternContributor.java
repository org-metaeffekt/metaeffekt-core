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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class XWikiExtensionComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".xed");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile();

        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.parse(anchorFile);
            final Element documentElement = doc.getDocumentElement();

            final String id = optStringValue(documentElement, "id");
            final String version = optStringValue(documentElement, "version");
            final String type = optStringValue(documentElement, "type");
            final String name = optStringValue(documentElement, "name");
            final String url = optStringValue(documentElement, "website");

            final String subjectPattern = anchorFile.getName().replace(".xed", ".*");
            final File subjectFile = new File(contextBaseDir, anchorFile.getName().replace(".xed", "." + type));

            String artifactId = id;
            String groupId = null;

            final int colonIndex = id.indexOf(":");
            if (colonIndex != -1) {
                groupId = id.substring(0, colonIndex);
                artifactId = id.substring(colonIndex + 1);
            }

            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, contextRelPath);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifactId + "-" + version + "." + type);
            componentPatternData.set(Artifact.Attribute.GROUPID.getKey(), groupId);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, subjectPattern);

            if (subjectFile.exists()) {
                componentPatternData.set("Component Checksum", FileUtils.computeChecksum(subjectFile));
            }

            // NOTE: potentially a mapping is required
            // componentPatternData.set(Constants.KEY_TYPE, TYPE_VALUE_XWIKI_EXTENSION);

            componentPatternData.set(Artifact.Attribute.URL.getKey(), url);

            return Collections.singletonList(componentPatternData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String optStringValue(Element documentElement, String key) {
        final NodeList optNodeList = documentElement.getElementsByTagName(key);
        if (optNodeList.getLength() > 0) {
            return optNodeList.item(0).getTextContent();
        }
        return null;
    }

}
