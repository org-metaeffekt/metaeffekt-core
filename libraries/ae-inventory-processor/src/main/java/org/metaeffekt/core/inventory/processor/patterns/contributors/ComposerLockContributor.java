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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComposerLockContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ComposerLockContributor.class);

    public static final String TYPE_VALUE_PHP_COMPOSER = "php-composer";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
            add("composer.lock");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("composer.lock");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile();

        final String anchorRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile);

        try {
            final JSONObject object = new JSONObject(FileUtils.readFileToString(anchorFile, StandardCharsets.UTF_8));
            final JSONArray packages = object.getJSONArray("packages");

            final List<ComponentPatternData> list = new ArrayList<>();

            for (int j = 0; j < packages.length(); j++) {
                final JSONObject jsonObject = packages.getJSONObject(j);
                final String name = jsonObject.optString("name");
                String version = jsonObject.optString("version");

                if (version != null && version.startsWith("v")) {
                    version = version.substring(1);
                }
                final String purl = buildPurl(name, version);
                String url = null;

                // FIXME: define different URLs for source and dist
                final JSONObject source = jsonObject.optJSONObject("source");
                if (source != null) {
                    url = source.optString("url");
                }

                // dist overwrites source at the moment
                final JSONObject dist = jsonObject.optJSONObject("dist");
                if (dist != null) {
                    url = dist.optString("url");
                }

                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorRelPath);
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, name + "/**/*");
                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/node_modules/**/*");

                // exclude embedded web modules; these require to be identified by themselves
                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                        name + "/**/node_modules/**/*," +
                        name + "/**/bower_components/**/*");

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, name + "-" + version);

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_PHP_COMPOSER);
                componentPatternData.set(Artifact.Attribute.URL.getKey(), url);
                componentPatternData.set(Artifact.Attribute.PURL.getKey(), purl);

                list.add(componentPatternData);
            }
            return list;
        } catch (IOException e) {
            LOG.warn("Failure processing composer.lock file: [{}]", e.getMessage());
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

    public static String buildPurl(String name, String version) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (version == null || version.isEmpty()) {
            return String.format("pkg:composer/%s", name);
        }
        return String.format("pkg:composer/%s@%s", name, version);
    }
}
