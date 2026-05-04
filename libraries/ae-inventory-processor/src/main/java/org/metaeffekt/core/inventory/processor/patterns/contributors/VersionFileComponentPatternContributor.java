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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class VersionFileComponentPatternContributor extends ComponentPatternContributor {

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
            add("/version");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/VERSION");
    }

    @Override
    public int getExecutionPhase() {
        return 10;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum, EvaluationContext evaluationContext) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);
            final File contextBaseDir = anchorFile.getParentFile().getParentFile();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile());
            final String relativeVersionAnchorPath = contextRelPath + "/" + anchorFile.getName();

            String version = FileUtils.readFileToString(anchorFile, StandardCharsets.UTF_8).trim();

            if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
                log.info("Skipping version file content:  " + version);
                return Collections.emptyList();
            }

            final String componentName = anchorFile.getParentFile().getName();

            // a version file within a python egg; do not process any further; expecting the dedicated contributor to pick up
            if ("EGG-INFO".equals(componentName)) {
                return Collections.emptyList();
            }

            // there are some names, we do not want to get triggered on
            if (componentName.startsWith("[") || componentName.contains(".")) {
                return Collections.emptyList();
            }

            String subjectName = componentName.endsWith("-" + version) ? componentName : componentName + "-" + version;
            String semaphore = FileUtils.asRelativePath(baseDir, contextBaseDir) + ":" + subjectName;

            if (!evaluationContext.isProcessed(semaphore)) {
                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeVersionAnchorPath);
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, componentName);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, subjectName);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, contextRelPath + "/**/*");
                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
                componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "versioned-file");

                evaluationContext.registerProcessed(semaphore);

                return Collections.singletonList(componentPatternData);
            }

        } catch (Exception e) {
            log.warn("Failed to process versioned folder contributor for file: {}", relativeAnchorPath, e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

}
