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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GemSpecContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GemSpecContributor.class);

    public static final String TYPE_VALUE_RUBY_GEM = "ruby-gem";

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".gemspec");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            // parse gemspec
            String content = FileUtils.readFileToString(anchorFile, FileUtils.ENCODING_UTF_8);

            final String anchorFileName = anchorFile.getName();
            final String id = anchorFileName.replace(".gemspec", "");
            final String anchorFileNameNoSuffix = anchorFileName.replace(".gemspec", "");

            final String parentDirName = anchorFile.getParentFile().getName();

            // anchor must match at least a qualified versioning string <major>.<minor> to be able to extract a version
            final Pattern versionPattern = Pattern.compile("-[0-9]+\\.[0-9]+");
            final Matcher matcher = versionPattern.matcher(anchorFileNameNoSuffix);
            int anchorNameSeparatorIndex = matcher.find() ? matcher.start() : -1;
            final String nameDerivedFromFile;
            final String versionDerivedFromFile;
            if (anchorNameSeparatorIndex != -1) {
                nameDerivedFromFile = anchorFileNameNoSuffix.substring(0, anchorNameSeparatorIndex);
                versionDerivedFromFile = anchorFileNameNoSuffix.substring(anchorNameSeparatorIndex + 1);
            } else {
                nameDerivedFromFile = anchorFileName;
                versionDerivedFromFile = null;
            }

            final int anchorFolderSeparatorIndex = parentDirName.lastIndexOf("-");
            final String nameDerivedFromFolder;
            final String versionDerivedFromFolder;
            if (anchorFolderSeparatorIndex != -1) {
                nameDerivedFromFolder = parentDirName.substring(0, anchorFolderSeparatorIndex);
                versionDerivedFromFolder = parentDirName.substring(anchorFolderSeparatorIndex + 1);
            } else {
                nameDerivedFromFolder = parentDirName;
                versionDerivedFromFolder = null;
            }

            // FIXME: parse version, url, license, potentially secondary name
            String version = versionDerivedFromFile;
            if (version == null) version = versionDerivedFromFolder;
            String url = null;

            String concludedName = nameDerivedFromFile;
            int versionIndex = concludedName.indexOf("-" + version);
            if (versionIndex != -1) {
                concludedName = concludedName.substring(0, versionIndex);
            }

            concludedName = concludedName.replace(".gemspec", "");

            String concludedId = concludedName + "-" + version;

            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile());

            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, contextRelPath + "/" + anchorFileName);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, concludedName);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, concludedId);

            final StringBuilder sb = new StringBuilder();

            if (versionDerivedFromFile == null && versionDerivedFromFolder == null) {
                LOG.warn("No version extracted from Gemspec: " + relativeAnchorPath);
                throw new IllegalStateException();
            }

            if (versionDerivedFromFile != null) {
                // covers folders with name as folder name and anything deeper nested
                sb.append("**/" + nameDerivedFromFile + "-" + versionDerivedFromFile + "/**/*").append(",");

                // cover cache files
                sb.append("/**/cache/**/" + nameDerivedFromFile + "-" + versionDerivedFromFile + "*").append(",");
            } else {
                if (versionDerivedFromFolder != null) {
                    // covers folders with name as folder name and anything deeper nested
                    sb.append("**/" + nameDerivedFromFile + "-" + versionDerivedFromFolder + "/**/*").append(",");
                    sb.append("**/" + nameDerivedFromFolder + "-" + versionDerivedFromFolder + "/**/*").append(",");

                    // cover cache folders (version may vary in cache; potential to catch more than required)
                    sb.append("/**/cache/**/" + nameDerivedFromFile + "-" + "*" + "/**/*").append(",");
                    sb.append("/**/cache/**/" + nameDerivedFromFolder + "-" + "*" + "/**/*").append(",");
                }
            }

            // cover anchor file itself and those matching the same name
            sb.append("**/" + anchorFileName);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, sb.toString());
            componentPatternData.set(Constants.KEY_TYPE, TYPE_VALUE_RUBY_GEM);
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
