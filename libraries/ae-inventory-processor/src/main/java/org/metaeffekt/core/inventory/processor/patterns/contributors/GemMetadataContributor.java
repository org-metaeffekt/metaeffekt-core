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

import org.apache.commons.lang3.StringUtils;
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

import static org.apache.commons.lang3.StringUtils.isBlank;

public class GemMetadataContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GemMetadataContributor.class);

    public static final String TYPE_VALUE_RUBY_GEM = "ruby-gem-metadata";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("metadata");
    }});

    public static final Pattern FOLDER_VERSION_PATTERN =
            Pattern.compile("([a-zA-Z0-9-_]+)-([0-9]+\\.[0-9]+(\\.[0-9]+)*(-x86_64-linux-gnu){0,1})");

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("[metadata.gz]/metadata");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            // parse gemspec
            // FIXME: detect charset
            String content8859 = FileUtils.readFileToString(anchorFile, StandardCharsets.ISO_8859_1);
            String contentUtf8 = FileUtils.readFileToString(anchorFile, StandardCharsets.UTF_8);

            final String anchorFileName = anchorFile.getName();
            final String id = anchorFileName.replace(".gemspec", "");

            String folderName = anchorFile.getParentFile().getParentFile().getName();

            if (folderName.startsWith("[") && folderName.endsWith("]")) {
                folderName = folderName.substring(1, folderName.length()-1);
            }

            String gemName = folderName;

            if (gemName.endsWith(".gem")) {
                gemName = gemName.substring(0, folderName.length() - 4);
            }

            // anchor must match at least a qualified versioning string <major>.<minor> to be able to extract a version
            final Matcher matcher = FOLDER_VERSION_PATTERN.matcher(gemName);

            if (matcher.find()) {
                final String name = matcher.group(1);
                final String version = matcher.group(2);
                final String platform = matcher.group(4);

                final String purifiedVersion = isBlank(platform) ? version : version.substring(0, version.lastIndexOf(platform));

                // NOTE: the version may include platform specific parts

                // verify name / version against metadata file
                boolean containsName = content8859.contains("name: " + name) || contentUtf8.contains("name: " + name);
                boolean containsVersion = content8859.contains("version: " + purifiedVersion) || contentUtf8.contains("version: " + purifiedVersion);

                if (containsName && containsVersion) {

                    // construct component pattern
                    final ComponentPatternData componentPatternData = new ComponentPatternData();
                    final String contextRelPath = FileUtils.asRelativePath(contextBaseDir.getParentFile(), anchorFile.getParentFile().getParentFile());

                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, FileUtils.asRelativePath(contextBaseDir.getParentFile(), anchorFile));
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, FileUtils.asRelativePath(baseDir, contextBaseDir));

                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);

                    // NOTE: here we deliberately use the name without the .gem suffix
                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, gemName);

                    componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, contextRelPath + "/**/*");
                    componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
                    componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_RUBY_GEM);

                    String purl = buildPurl(name, version, "ruby");
                    componentPatternData.set(Artifact.Attribute.PURL.getKey(), purl);

                    return Collections.singletonList(componentPatternData);
                }
            } else {
                LOG.warn("Could not verify gem. Name and version not detected in file [{}]", anchorFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.warn("Failure while processing anchor [{}]: [{}]", anchorFile.getAbsolutePath(), e.getMessage());
        }
        return Collections.emptyList();
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
