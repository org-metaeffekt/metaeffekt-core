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

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class VersionedFolderComponentPatternContributor extends ComponentPatternContributor {

    public static final Pattern FOLDER_VERSION_PATTERN =
            Pattern.compile(".*/([a-zA-Z0-9-_]+)-([0-9]+\\.[0-9]+(\\.[0-9]+)*(-x86_64-linux-gnu){0,1})/.*");

    @Override
    public boolean applies(String pathInContext) {
        if (FOLDER_VERSION_PATTERN.matcher(pathInContext).matches()) {
            return true;
        }
        return false;
    }

    @Override
    public int getExecutionPhase() {
        return 11;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum, EvaluationContext context) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);
            final String contextPath = FileUtils.asRelativePath(baseDir, anchorFile);

            List<ComponentPatternData> componentPatternDataList = null;
            Matcher matcher = FOLDER_VERSION_PATTERN.matcher("/" + contextPath);
            if (matcher.find()) {
                if (componentPatternDataList == null) {
                    componentPatternDataList = new ArrayList<>();
                }

                String name = matcher.group(1);
                String version = matcher.group(2);

                String subjectFolderName = name + "-" + version;
                int index = relativeAnchorPath.indexOf("/" + subjectFolderName + "/");

                if (index == -1) {
                    throw new IllegalStateException("Matched folder not found: " + relativeAnchorPath);
                }

                String contextBaseDir = relativeAnchorPath.substring(0, index + subjectFolderName.length() + 1);
                String relativeVersionAnchorPath = subjectFolderName + relativeAnchorPath.substring(contextBaseDir.length());

                String semaphore = relativeAnchorPath.substring(0, index) + ":" + subjectFolderName;

                if (!context.isProcessed(semaphore)) {
                    final ComponentPatternData componentPatternData = new ComponentPatternData();
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeVersionAnchorPath);
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, subjectFolderName);

                    componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, subjectFolderName + "/**/*");
                    componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");

                    componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
                    componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "versioned-folder");

                    componentPatternDataList.add(componentPatternData);

                    context.registerProcessed(semaphore);
                }
            }

            return componentPatternDataList;
        } catch (Exception e) {
            log.warn("Failed to process versioned folder contributor for file: {}", relativeAnchorPath, e);
            return Collections.emptyList();
        }
    }

    // this method is not applicable
    public List<String> getSuffixes() {
        throw new  UnsupportedOperationException("Contributor is not suffix-based.");
    }

}
