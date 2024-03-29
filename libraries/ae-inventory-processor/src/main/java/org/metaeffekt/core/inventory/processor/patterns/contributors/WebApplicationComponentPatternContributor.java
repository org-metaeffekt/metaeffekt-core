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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.PropertiesUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer.localeConstants.PATH_LOCALE;

public class WebApplicationComponentPatternContributor extends ComponentPatternContributor {
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/web-inf/web.xml");
    }});

        @Override
    public boolean applies(String pathInContext) {
        return pathInContext.toLowerCase(PATH_LOCALE).endsWith("/web-inf/web.xml");
    }

    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);
            final File contextBaseDir = anchorFile.getParentFile().getParentFile();

            WebXmlData webXmlData = parseNameFromWebXml(anchorFile);
            String version = parseNameFromVersionPropertyFile(new File(anchorFile.getParentFile(), "version.properties"));

            if (StringUtils.isNotEmpty(webXmlData.displayName)) {

                // construct component pattern
                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                        FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                String derivedPart = webXmlData.displayName.toLowerCase(PATH_LOCALE);
                if (StringUtils.isNotEmpty(version)) {
                    derivedPart = derivedPart + "-" + version;
                }

                String derivedComponent = webXmlData.description;
                if (StringUtils.isBlank(derivedComponent)) {
                    derivedComponent = webXmlData.displayName;
                }

                String derivedVersion = version;
                if (StringUtils.isBlank(derivedVersion)) {
                    derivedVersion = "$" + webXmlData.displayName.toUpperCase().replaceAll(" ", "_") + "_VERSION";
                }

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, derivedComponent);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, derivedVersion);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, derivedPart);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");

                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/node_modules/**/*,**/*.jar");

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

                return Collections.singletonList(componentPatternData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    private String parseNameFromVersionPropertyFile(File file) {
        Properties p = PropertiesUtils.loadPropertiesFile(file);

        final String version = p.getProperty("version");

        return version;

    }

    class WebXmlData {
        String displayName;
        String description;
    }

    private static final Pattern displayNamePattern = Pattern.compile("(?s)^(.*?)<display-name>(.*?)<\\/display-name>(.*)$");

    private static final Pattern descriptionPattern = Pattern.compile("(?s)^(.*?)<description>(.*?)<\\/description>(.*)$");

    private WebXmlData parseNameFromWebXml(File anchorFile) throws IOException {
        final String content = FileUtils.readFileToString(anchorFile, FileUtils.ENCODING_UTF_8);

        final WebXmlData webXmlData = new WebXmlData();

        final Matcher displayNameMatcher = displayNamePattern.matcher(content);
        if (displayNameMatcher.matches()) {
            webXmlData.displayName = displayNameMatcher.replaceAll("$2");
        }

        final Matcher descriptionMatcher = descriptionPattern.matcher(content);
        if (descriptionMatcher.matches()) {
            webXmlData.description = descriptionMatcher.replaceAll("$2");
        }

        return webXmlData;
    }

}
