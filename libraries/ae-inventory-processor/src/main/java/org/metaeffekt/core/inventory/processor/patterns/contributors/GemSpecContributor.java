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
import org.metaeffekt.core.inventory.processor.patterns.contributors.exception.ContributorFailureException;
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

public class GemSpecContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GemSpecContributor.class);

    public static final String TYPE_VALUE_RUBY_GEM = "ruby-gem";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".gemspec");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".gemspec");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            // parse gemspec
            String content = FileUtils.readFileToString(anchorFile, StandardCharsets.UTF_8);

            final String anchorFileName = anchorFile.getName();
            final String id = anchorFileName.replace(".gemspec", "");
            final String anchorFileNameNoSuffix = anchorFileName.replace(".gemspec", "");

            final String parentDirName = anchorFile.getParentFile().getName();

            // anchor must match at least a qualified versioning string <major>.<minor> to be able to extract a version
            // FIXME: why is there a minus ("-") at the beginning of the pattern? ask karsten what this is.
            final Pattern versionFromFilenamePattern = Pattern.compile("-[0-9]+\\.[0-9]+");
            final Matcher matcher = versionFromFilenamePattern.matcher(anchorFileNameNoSuffix);
            int anchorNameSeparatorIndex = matcher.find() ? matcher.start() : -1;
            final String nameDerivedFromFile;
            final String versionDerivedFromFileName;
            if (anchorNameSeparatorIndex != -1) {
                nameDerivedFromFile = anchorFileNameNoSuffix.substring(0, anchorNameSeparatorIndex);
                versionDerivedFromFileName = anchorFileNameNoSuffix.substring(anchorNameSeparatorIndex + 1);
            } else {
                nameDerivedFromFile = anchorFileName;
                versionDerivedFromFileName = null;
            }

            final int anchorFolderSeparatorIndex = parentDirName.lastIndexOf("-");
            final String nameDerivedFromFolderName;
            final String versionDerivedFromFolderName;
            if (anchorFolderSeparatorIndex != -1) {
                nameDerivedFromFolderName = parentDirName.substring(0, anchorFolderSeparatorIndex);
                versionDerivedFromFolderName = parentDirName.substring(anchorFolderSeparatorIndex + 1);
            } else {
                nameDerivedFromFolderName = parentDirName;
                versionDerivedFromFolderName = null;
            }

            String versionDerivedFromGemspecContent = null;
            try {
                final Pattern versionLinePattern = Pattern.compile(
                        "^[a-zA-Z0-9 ]{1,128}\\.version *=.*", Pattern.MULTILINE);
                final Matcher versionLineMatcher = versionLinePattern.matcher(content);
                if (!versionLineMatcher.find()) {
                    throw new IllegalStateException("Regex could not find a valid version line in file.");
                }
                final String matchingVersionLine = versionLineMatcher.group();
                final String cutAfterFirstEquals = matchingVersionLine
                        .split("=", 2)[1]
                        .trim();
                System.out.println(cutAfterFirstEquals);
                final Pattern firstQuoteFinder = Pattern.compile("[\"']");
                Matcher firstQuoteMatcher = firstQuoteFinder.matcher(cutAfterFirstEquals);
                if (!firstQuoteMatcher.find()) {
                    throw new IllegalStateException("Regex could not find a valid quote in the line.");
                }
                String firstQuote = firstQuoteMatcher.group();
                // find corresponding next quote. does not respect escapes.
                int lastQuoteIndex = cutAfterFirstEquals.indexOf(firstQuote, firstQuoteMatcher.end());
                versionDerivedFromGemspecContent = cutAfterFirstEquals
                        .substring(firstQuoteMatcher.end(), lastQuoteIndex);
            } catch (Exception e) {
                // ignore, try to get version from other sources, otherwise crash later.
                // gemspecs are actually CODE, so we may find all sorts of non-literal shenanigans.
            }

            // FIXME: parse version, url, license, potentially secondary name
            String version = versionDerivedFromFileName;
            if (version == null) version = versionDerivedFromFolderName;
            if (version == null) version = versionDerivedFromGemspecContent;

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

            if (versionDerivedFromFileName == null && versionDerivedFromFolderName == null) {
                // FIXME: why do we care? it seems it's not standard to quality version in filanems.
                LOG.trace("No version denoted in file's or folder's name.");
            }
            if (version == null) {
                LOG.error("No version extracted from Gemspec: " + relativeAnchorPath);
                throw new IllegalStateException("No version could be extracted from the logged gemspec.");
            }

            if (versionDerivedFromFileName != null) {
                // covers folders with name as folder name and anything deeper nested
                sb.append("**/" + nameDerivedFromFile + "-" + versionDerivedFromFileName + "/**/*").append(",");

                // cover cache files
                sb.append("/**/cache/**/" + nameDerivedFromFile + "-" + versionDerivedFromFileName + "*").append(",");
            } else {
                if (versionDerivedFromFolderName != null) {
                    // covers folders with name as folder name and anything deeper nested
                    sb.append("**/" + nameDerivedFromFile + "-" + versionDerivedFromFolderName + "/**/*").append(",");
                    sb.append("**/" + nameDerivedFromFolderName + "-" + versionDerivedFromFolderName + "/**/*").append(",");

                    // cover cache folders (version may vary in cache; potential to catch more than required)
                    sb.append("/**/cache/**/" + nameDerivedFromFile + "-" + "*" + "/**/*").append(",");
                    sb.append("/**/cache/**/" + nameDerivedFromFolderName + "-" + "*" + "/**/*").append(",");
                }
            }

            // cover anchor file itself and those matching the same name
            sb.append("**/" + anchorFileName);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, sb.toString());
            componentPatternData.set(Constants.KEY_TYPE, TYPE_VALUE_RUBY_GEM);
            componentPatternData.set(Artifact.Attribute.URL.getKey(), url);

            return Collections.singletonList(componentPatternData);
        } catch (Exception e) {
            LOG.error("Error [{}] while processing anchor [{}].", e.getMessage(), anchorFile.getAbsolutePath());
            throw new ContributorFailureException(e);
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    private static String optStringValue(Element documentElement, String key) {
        final NodeList optNodeList = documentElement.getElementsByTagName(key);
        if (optNodeList.getLength() > 0) {
            return optNodeList.item(0).getTextContent();
        }
        return null;
    }
}
