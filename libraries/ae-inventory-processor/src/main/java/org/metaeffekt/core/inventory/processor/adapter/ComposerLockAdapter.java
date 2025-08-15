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
package org.metaeffekt.core.inventory.processor.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts an inventory for PHP Composer modules based on a composer.lock file.
 */
public class ComposerLockAdapter {

    public static final String TYPE_VALUE_PHP_COMPOSER = "php-composer";

    /**
     * Extracts an inventory from a composer.lock file.
     *
     * @param composerLock The composer.lock file to parse
     * @param relativePath The relative path to the file from the relevant basedir
     * @param dependencies The list of dependencies from composer.json
     *
     * @return An inventory populated with the modules defined in the composer.lock file
     *
     * @throws IOException May be thrown on file access.
     */
    public Inventory extractInventory(File composerLock, String relativePath, List<WebModuleDependency> dependencies) throws IOException {
        final Map<String, WebModuleDependency> dependencyMap = buildDependencyLookupMap(dependencies);

        final JSONObject object = new JSONObject(FileUtils.readFileToString(composerLock, StandardCharsets.UTF_8));
        final JSONArray packages = object.getJSONArray("packages");

        Inventory inventory = new Inventory();

        for (int j = 0; j < packages.length(); j++) {
            final JSONObject jsonObject = packages.getJSONObject(j);
            final String name = jsonObject.optString("name");
            String version = jsonObject.optString("version");

            if (version != null && version.startsWith("v")) {
                version = version.substring(1);
            }

            String url = null;
            String sourceArchiveUrl = null;

            // the url is the address of the source repository
            final JSONObject source = jsonObject.optJSONObject("source");
            if (source != null) {
                url = source.optString("url");
            }

            // the dist url is mapped to the source archive url
            final JSONObject dist = jsonObject.optJSONObject("dist");
            if (dist != null) {
                sourceArchiveUrl = dist.optString("url");
            }

            Artifact artifact = new Artifact();
            artifact.setId(name + "-" + version);
            artifact.setComponent(name);
            artifact.setVersion(version);
            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_PHP_COMPOSER);
            artifact.set("Source Archive - URL", sourceArchiveUrl);
            artifact.set("URL", url);
            artifact.set(Constants.KEY_PATH_IN_ASSET, relativePath + "[" + name + "]");

            String assetId = "AID-" + artifact.getId();

            // Check if we have dependency information from composer.json
            WebModuleDependency dependency = dependencyMap.get(name);
            if (dependency != null) {
                if (dependency.isDevDependency()) {
                    artifact.set(assetId, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
                }
            }

            String purl = buildPurl(name, version);
            artifact.set(Artifact.Attribute.PURL, purl);

            inventory.getArtifacts().add(artifact);
        }

        return inventory;
    }

    private static Map<String, WebModuleDependency> buildDependencyLookupMap(List<WebModuleDependency> dependencies) {
        final Map<String, WebModuleDependency> dependencyMap = new HashMap<>();
        if (dependencies != null) {
            for (WebModuleDependency dependency : dependencies) {
                dependencyMap.put(dependency.getName(), dependency);
            }
        }
        return dependencyMap;
    }

    /**
     * Extracts an inventory from a composer.lock file without dependency information.
     *
     * @param composerLock The composer.lock file to parse.
     * @param relativePath The relative path to the file from the relevant basedir.
     *
     * @return An inventory populated with the modules defined in the composer.lock file
     *
     * @throws IOException May be thrown on file access.
     */
    public Inventory extractInventory(File composerLock, String relativePath) throws IOException {
        return extractInventory(composerLock, relativePath, Collections.emptyList());
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