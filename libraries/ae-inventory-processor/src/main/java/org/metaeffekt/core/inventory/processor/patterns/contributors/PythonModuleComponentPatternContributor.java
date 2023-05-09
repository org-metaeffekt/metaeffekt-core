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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.ParsingUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PythonModuleComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(File contextBaseDir, String file, Artifact artifact) {
        return file.endsWith(".dist-info/METADATA")
                || file.endsWith(".dist-info/RECORD")
                || file.endsWith(".dist-info/WHEEL");
    }

    @Override
    public void contribute(File contextBaseDir, String anchorFilePath, Artifact artifact, ComponentPatternData componentPatternData) {
        final File anchorFile = new File(contextBaseDir, anchorFilePath);
        final File anchorParentDir = anchorFile.getParentFile();

        String includePattern = anchorFilePath.replace("/" + anchorFile.getName(), "") + "/**/*";

        artifact.setId(anchorFilePath.replace(".dist-info/"+ anchorFile.getName(), ""));

        if (anchorFile.getName().equals("METADATA") && anchorFile.exists()) {
            try {
                List<String> fileContentLines = FileUtils.readLines(anchorFile, FileUtils.ENCODING_UTF_8);

                String version = ParsingUtils.getValue(fileContentLines, "Version:");
                String name = ParsingUtils.getValue(fileContentLines, "Name:");
                String summary = ParsingUtils.getValue(fileContentLines, "Summary:");
                String homepage = ParsingUtils.getValue(fileContentLines, "Home-page:");
                String licenseExpression = ParsingUtils.getValue(fileContentLines, "License:");

                artifact.setVersion(version);
                artifact.setComponent(name);
                artifact.set(Constants.KEY_SUMMARY, summary);
                artifact.setUrl(homepage);
                artifact.set("Package Specified Licenses", licenseExpression);

            } catch (IOException e) {
            }
        } else {
            // in case it is not a METADATA file we extract name and version from the path

            String distInfoFolderName = anchorFile.getParentFile().getName();
            distInfoFolderName = distInfoFolderName.replace(".dist-info", "");

            int lastDash = distInfoFolderName.lastIndexOf("-");
            if (lastDash != -1) {
                String name = distInfoFolderName.substring(0, lastDash);
                String version = distInfoFolderName.substring(lastDash + 1);

                artifact.setVersion(version);
                artifact.setComponent(name);
            }
        }

        final File topLevelInfo = new File(anchorFile.getParentFile(), "top_level.txt");
        if (topLevelInfo.exists()) {
            try {
                final List<String> topLevelNames = FileUtils.readLines(topLevelInfo, FileUtils.ENCODING_UTF_8);
                includePattern += "," + topLevelNames.stream()
                        .map(String::trim).filter(s -> !StringUtils.isEmpty(s))
                        .map(s -> FileUtils.asRelativePath(contextBaseDir, new File(anchorFile.getParentFile().getParentFile(), s)) + "/**/*")
                        .collect(Collectors.joining(","));
            } catch (IOException e) {
            }
        } else {
            // in case no top_level.txt exists we use the current component name
            includePattern += "," + artifact.getComponent() + "/**/*";
        }

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, anchorParentDir.getName() + "/**/node_modules/**/*" + "," + anchorParentDir.getName() + "/**/bower_components/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePattern);
    }
}
