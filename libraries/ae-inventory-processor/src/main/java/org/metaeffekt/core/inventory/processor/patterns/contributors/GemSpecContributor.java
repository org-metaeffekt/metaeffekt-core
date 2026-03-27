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

import org.metaeffekt.core.inventory.processor.filepatterns.FileMetaData;
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

public class GemSpecContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GemSpecContributor.class);

    public static final String TYPE_VALUE_RUBY_GEM = "ruby-gem-spec";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".gemspec");
    }});

    public static final Pattern VERSION_LINE_PATTERN = Pattern.compile("^[a-zA-Z0-9 ]{1,128}\\.version *=.*", Pattern.MULTILINE);
    public static final Pattern FIRST_QUOTE_PATTERN = Pattern.compile("[\"']");
    public static final Pattern VERSION_FROM_FILENAME_PATTERN = Pattern.compile("-[0-9]+\\.[0-9]+");

    public static final Pattern FOLDER_VERSION_PATTERN =
            Pattern.compile("([a-zA-Z0-9-_]+)-([0-9]+\\.[0-9]+(\\.[0-9]+)*(-x86_64-linux-gnu){0,1})");

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".gemspec");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        File contextBaseDir = anchorFile.getParentFile();

        // kklein/workspace/metaeffekt-core/.examples/case-001-scan/[root]/user/local/bundle/cache/[webrick-1.9.1.gem]/
        if (contextBaseDir.getName().equals("[data.tar]")) {
            contextBaseDir = contextBaseDir.getParentFile();
        }
        if (contextBaseDir.getName().equals("[data.tar.gz]")) {
            contextBaseDir = contextBaseDir.getParentFile();
        }

        try {
            // parse gemspec
            final String content = FileUtils.readFileToString(anchorFile, StandardCharsets.UTF_8);

            final String anchorFileName = anchorFile.getName();
            String parentDirName = contextBaseDir.getName();

            if (parentDirName.startsWith("[")) {
                parentDirName = parentDirName.substring(1, parentDirName.length() - 1);
            }

            if (parentDirName.endsWith(".gem")) {
                parentDirName = parentDirName.substring(0, parentDirName.length() - 4);
            }

            // anchor must match at least a qualified versioning string <major>.<minor> to be able to extract a version
            final Matcher matcher = FOLDER_VERSION_PATTERN.matcher(parentDirName);

            final String nameDerivedFromFile;
            final String versionDerivedFromFileName;

            if (matcher.find()) {
                nameDerivedFromFile = matcher.group(1);
                versionDerivedFromFileName = matcher.group(2);
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
                final Pattern versionLinePattern = VERSION_LINE_PATTERN;
                final Matcher versionLineMatcher = versionLinePattern.matcher(content);
                if (!versionLineMatcher.find()) {
                    throw new IllegalStateException("Regex could not find a valid version line in file.");
                }
                final String matchingVersionLine = versionLineMatcher.group();
                final String cutAfterFirstEquals = matchingVersionLine.split("=", 2)[1].trim();
                final Pattern firstQuoteFinder = FIRST_QUOTE_PATTERN;
                Matcher firstQuoteMatcher = firstQuoteFinder.matcher(cutAfterFirstEquals);
                if (!firstQuoteMatcher.find()) {
                    throw new IllegalStateException("Regex could not find a valid quote in the line.");
                }
                String firstQuote = firstQuoteMatcher.group();
                // find corresponding next quote. does not respect escapes.
                int lastQuoteIndex = cutAfterFirstEquals.indexOf(firstQuote, firstQuoteMatcher.end());
                versionDerivedFromGemspecContent = cutAfterFirstEquals.substring(firstQuoteMatcher.end(), lastQuoteIndex);
            } catch (Exception e) {
                // ignore, try to get version from other sources, otherwise crash later.
                // gemspecs are actually CODE, so we may find all sorts of non-literal shenanigans.
            }

            // FIXME: parse version, url, license, potentially secondary name
            String version = versionDerivedFromFileName;
            if (version == null) version = versionDerivedFromFolderName;
            if (version == null) version = versionDerivedFromGemspecContent;

            if (version == null) {
                FileMetaData fileMetaData = getFileComponentPatternProcessor().deriveFileMetaData(relativeAnchorPath);
                if (fileMetaData != null) {
                    version = fileMetaData.getVersion();
                }
            }

            String url = null;

            String concludedName = nameDerivedFromFile;
            int versionIndex = concludedName.indexOf("-" + version);
            if (versionIndex != -1) {
                concludedName = concludedName.substring(0, versionIndex);
            }

            String concludedId = concludedName + "-" + version;

            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir.getParentFile(), anchorFile.getParentFile());

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
            if (version == null || "VERSION".equals(version)) {
                LOG.warn("No version extracted from Gemspec [{}. Skipped.", relativeAnchorPath);
                return Collections.emptyList();
            }

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, contextBaseDir.getName() + "/**/*");
            componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
            componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_RUBY_GEM);
            componentPatternData.set(Artifact.Attribute.URL.getKey(), url);

            // TODO: find out how to extract platform-attribute .gemspec file
            String purl = buildPurl(concludedName, version, "ruby");
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
