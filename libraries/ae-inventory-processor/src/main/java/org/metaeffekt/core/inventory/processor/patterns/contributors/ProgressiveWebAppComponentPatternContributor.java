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

import org.json.JSONException;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProgressiveWebAppComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressiveWebAppComponentPatternContributor.class);
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/manifest.json");
    }});

    public static final String TYPE_VALUE_PWA = "pwa-module";

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("manifest.json");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        // FIXME-AOE: revise exception handling; deviations from JSON syntax should only be logged on DEBUG level
        try {
            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String manifestContent = FileUtils.readFileToString(anchorFile, FileUtils.ENCODING_UTF_8);

            Path parentPath = anchorFile.getParentFile().toPath();
            File contributeFile = new File(parentPath.toFile(), "contribute.json");

            final JSONObject jsonObject = new JSONObject(manifestContent);
            String name = null;
            String version = null;
            String license = null;
            try {
                name = jsonObject.getString("name");
            } catch (JSONException e) {
                LOG.debug("Could not find name in manifest.json. Trying to find name in contribute.json file: [{}]", contributeFile.getAbsolutePath());
                if (contributeFile.exists()) {
                    final String contributeContent = FileUtils.readFileToString(contributeFile, FileUtils.ENCODING_UTF_8);
                    final JSONObject contributeJsonObject = new JSONObject(contributeContent);
                    name = contributeJsonObject.getString("name");
                }
            }

            try {
                version = jsonObject.getString("version");
            } catch (Exception e) {
                final File versionFile = new File(parentPath.toFile(), "version");
                if (versionFile.exists() && versionFile.isFile()) {
                    LOG.debug("Could not find version in manifest.json. Trying to find version in version file: [{}]", versionFile.getAbsolutePath());
                    try (Stream<String> lines = Files.lines(versionFile.toPath())) {
                        for (String line : lines.collect(Collectors.toList())) {
                            if (!line.isEmpty()) {
                                version = line;
                                break;
                            }
                        }
                    }
                } else {
                    LOG.debug("Unable to parse progressive web application version [{}]: {}", anchorFile.getAbsolutePath(), e.getMessage());
                    return Collections.emptyList();
                }
            }

            try {
                license = jsonObject.getString("license");
            } catch (Exception e) {
                LOG.debug("Could not find license in manifest.json. Trying to find license in contribute.json file: [{}]", contributeFile.getAbsolutePath());
                if (contributeFile.exists()) {
                    final String contributeContent = FileUtils.readFileToString(contributeFile, FileUtils.ENCODING_UTF_8);
                    final JSONObject contributeJsonObject = new JSONObject(contributeContent);
                    license = contributeJsonObject.getJSONObject("repository").getString("license");
                }
            }

            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorFile.getName());
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, name + "-" + version);
            componentPatternData.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, license);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
            componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                    "**/node_modules/**/*" + ",**/node_modules/*");

            componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
            componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_PWA);

            return Collections.singletonList(componentPatternData);
        } catch (JSONException e) {
            LOG.debug("Unable to parse progressive web application [{}]: {}", anchorFile.getAbsolutePath(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.warn("Unable to parse progressive web application [{}]: {}", anchorFile.getAbsolutePath(), e.getMessage());
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
}
