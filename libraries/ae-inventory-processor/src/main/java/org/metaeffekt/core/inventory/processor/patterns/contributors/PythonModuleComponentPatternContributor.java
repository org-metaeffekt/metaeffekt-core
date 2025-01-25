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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.ParsingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PythonModuleComponentPatternContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(PythonModuleComponentPatternContributor.class);

    // TODO: unify suffixes and other checks like in "applies"
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add(".dist-info/metadata");
        add(".dist-info/record");
        add(".dist-info/wheel");
    }});

    @Override
    public boolean applies(String pathInContext) {
        //FIXME: this triggers THRICE, creating three identical component patterns for one unpacked wheel. is that OK?
        return pathInContext.endsWith(".dist-info/METADATA")
                || pathInContext.endsWith(".dist-info/RECORD")
                || pathInContext.endsWith(".dist-info/WHEEL");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile();

        // this is the root of the component
        final File contextBaseDir = anchorParentDir.getParentFile();
        final String anchorPathInContext = FileUtils.asRelativePath(contextBaseDir, anchorFile);

        String distInfoFolderName = anchorFile.getParentFile().getName();

        // unclean id first, better than nothing
        String componentPart = relativeAnchorPath.replace(".dist-info/" + anchorFile.getName(), "");
        String componentName = null;
        String componentVersion = null;

        // not yet propagated details
        String homepage = null;
        String summary = null;
        String licenseExpression = null;

        if (anchorFile.getName().equals("METADATA") && anchorFile.exists()) {
            try {
                List<String> fileContentLines = FileUtils.readLines(anchorFile, FileUtils.ENCODING_UTF_8);

                componentVersion = ParsingUtils.getValue(fileContentLines, "Version:");
                componentName = ParsingUtils.getValue(fileContentLines, "Name:");
                summary = ParsingUtils.getValue(fileContentLines, "Summary:");
                homepage = ParsingUtils.getValue(fileContentLines, "Home-page:");
                licenseExpression = ParsingUtils.getValue(fileContentLines, "License:");

                // update id with better data
                componentPart = componentName + "-" + componentVersion;
            } catch (IOException e) {
                LOG.debug("IOException while trying to parse a METADATA file at [{}].", anchorFile);
            }
        } else {
            // in case it is not a METADATA file we extract name and version from the path
            String pDistInfoFolderName = distInfoFolderName.replace(".dist-info", "");

            int lastDash = pDistInfoFolderName.lastIndexOf("-");
            if (lastDash != -1) {
                componentName = pDistInfoFolderName.substring(0, lastDash);
                componentVersion = pDistInfoFolderName.substring(lastDash + 1);
                componentPart = componentName + "-" + componentVersion;
            }
        }

        // this one doesn't seem to be doing anything
        String includePattern = distInfoFolderName + "/**/*";

        final File topLevelInfo = new File(anchorFile.getParentFile(), "top_level.txt");
        if (topLevelInfo.exists()) {
            try {
                final List<String> topLevelNames = FileUtils.readLines(topLevelInfo, FileUtils.ENCODING_UTF_8);
                for (String line : topLevelNames) {
                    line = line.trim();
                    if (StringUtils.isBlank(line)) continue;
                    includePattern += "," + line + "/**/*";
                }
            } catch (IOException e) {
                LOG.debug("IOException while trying to parse top_level.txt at [{}]." , topLevelInfo);
            }
        } else {
            // FIXME: this creates VERY inclusive paths like "numpy/**/*" which didn't even do anything in testing
            // in case no top_level.txt exists we use the current component name
            includePattern += ", " + componentName + "/**/*";
        }

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorPathInContext);
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, componentName);
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, componentVersion);
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, componentPart);

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/node_modules/**/*" + ",**/bower_components/**/*" + ",**/__pycache__/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePattern);
        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/*.py, **/WHEEL, **/RECORD, **/METADATA, **/top_level.txt, **/*.exe");

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "python-library");

        String purl = buildPurl(componentName, componentVersion);
        componentPatternData.set(Artifact.Attribute.PURL.getKey(), purl);

        return Collections.singletonList(componentPatternData);
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
        // first we have to handle that the name should not be case sensitive and underscore should be replaced by dash
        // see: https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst#pypi
        name = name.toLowerCase(Locale.ENGLISH).replace("_", "-");
        return "pkg:pypi/" + name + "@" + version;
    }

}
