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
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GemMetadataContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GemMetadataContributor.class);

    public static final String TYPE_VALUE_RUBY_GEM = "ruby-gem";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("metadata");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("[metadata.gz]/metadata");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            // parse gemspec
            String content = FileUtils.readFileToString(anchorFile, StandardCharsets.ISO_8859_1);

            final String anchorFileName = anchorFile.getName();
            final String id = anchorFileName.replace(".gemspec", "");

            System.out.println("Hallo Ben!");

            String folderName = anchorFile.getParentFile().getParentFile().getName();

            if (folderName.startsWith("[") && folderName.endsWith("]")) {
                folderName = folderName.substring(1, folderName.length()-1);
            }
            if (folderName.endsWith(".gem")) {
                folderName = folderName.substring(0, folderName.length()-4);
            }
            int dashIndex = folderName.lastIndexOf('-');
            String name = folderName.substring(0, dashIndex);
            String version = folderName.substring(dashIndex+1, folderName.length());

            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile());

            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, name + "-" + version);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
            componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
            componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_RUBY_GEM);

            String purl = buildPurl(name, version, "ruby");
            componentPatternData.set(Artifact.Attribute.PURL.getKey(), purl);

            return Collections.singletonList(componentPatternData);
        } catch (Exception e) {
            LOG.warn("Failure while processing anchor [{}]: [{}]", anchorFile.getAbsolutePath(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private static String optStringValue(Element documentElement, String key) {
        final NodeList optNodeList = documentElement.getElementsByTagName(key);
        if (optNodeList.getLength() > 0) {
            return optNodeList.item(0).getTextContent();
        }
        return null;
    }

    private String buildPurl(String name, String version, String platform) {
        if (platform.equals("ruby")) {
            return String.format("pkg:gem/%s@%s", name, version);
        }
        return String.format("pkg:gem/%s@%s?platform=%s", name, version, platform);
    }
}
