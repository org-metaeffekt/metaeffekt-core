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

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts an inventory for Bower modules based on a .bower.json file.
 */
public class BowerLockAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(BowerLockAdapter.class);

    public static final String TYPE_VALUE_BOWER = "bower-module";

    /**
     * Extracts an inventory from a .bower.json file.
     *
     * @param bowerJson The .bower.json file to parse
     * @param relativePath The relative path to the file from the relevant basedir
     * @param dependencies The list of dependencies from bower.json
     * @return An inventory populated with the modules defined in the .bower.json file
     */
    public Inventory extractInventory(File bowerJson, String relativePath, List<WebModuleDependency> dependencies) {
        try {
            final JSONObject object = new JSONObject(FileUtils.readFileToString(bowerJson, StandardCharsets.UTF_8));
            
            Map<String, WebModuleDependency> dependencyMap = new HashMap<>();
            for (WebModuleDependency dependency : dependencies) {
                dependencyMap.put(dependency.getName(), dependency);
            }

            Inventory inventory = new Inventory();

            String name = object.optString("name");
            String version = object.optString("version");
            
            // .bower.json specific fields
            String resolvedVersion = object.optString("_release");
            if (resolvedVersion != null && !resolvedVersion.isEmpty()) {
                version = resolvedVersion;
            }

            if (version != null && version.startsWith("v")) {
                version = version.substring(1);
            }

            String url = null;
            if (object.has("_source")) {
                url = object.getString("_source");
            } else if (object.has("_target")) {  // .bower.json specific
                url = object.getString("_target");
            } else if (object.has("homepage")) {
                url = object.getString("homepage");
            }

            Artifact mainArtifact = createArtifact(name, version, url, relativePath);
            
            if (bowerJson.getName().equals(".bower.json")) {
                if (object.has("_originalSource")) {
                    mainArtifact.set("Original Source", object.getString("_originalSource"));
                }
                if (object.has("_resolution")) {
                    JSONObject resolution = object.getJSONObject("_resolution");
                    if (resolution.has("type")) {
                        mainArtifact.set("Resolution Type", resolution.getString("type"));
                    }
                    if (resolution.has("tag")) {
                        mainArtifact.set("Resolution Tag", resolution.getString("tag"));
                    }
                    if (resolution.has("commit")) {
                        mainArtifact.set("Resolution Commit", resolution.getString("commit"));
                    }
                }
            }
            
            inventory.getArtifacts().add(mainArtifact);

            processDependencies(object, "dependencies", dependencyMap, inventory, relativePath, false);
            processDependencies(object, "devDependencies", dependencyMap, inventory, relativePath, true);

            return inventory;
        } catch (IOException e) {
            LOG.warn("Cannot read / parse [{}]: {}", bowerJson.getAbsoluteFile(), e.getMessage());
            return null;
        }
    }

    private void processDependencies(JSONObject object, String dependencyType, 
                                   Map<String, WebModuleDependency> dependencyMap,
                                   Inventory inventory, String relativePath, boolean isDev) {
        if (object.has(dependencyType)) {
            JSONObject deps = object.getJSONObject(dependencyType);
            for (String key : deps.keySet()) {
                String depVersion = deps.getString(key);
                
                WebModuleDependency depInfo = dependencyMap.get(key);
                
                Artifact depArtifact = createArtifact(key, depVersion, null, relativePath);
                
                String assetId = "AID-" + depArtifact.getId();
                if (isDev || (depInfo != null && depInfo.isDevDependency())) {
                    depArtifact.set(assetId, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
                }
                
                inventory.getArtifacts().add(depArtifact);
            }
        }
    }

    private Artifact createArtifact(String name, String version, String url, String relativePath) {
        Artifact artifact = new Artifact();
        artifact.setId(name + "-" + version);
        artifact.setComponent(name);
        artifact.setVersion(version);
        artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
        artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, TYPE_VALUE_BOWER);
        if (url != null) {
            artifact.set("Source Archive - URL", url);
        }
        artifact.set(Constants.KEY_PATH_IN_ASSET, relativePath + "[" + name + "]");
        
        String purl = String.format("pkg:bower/%s@%s", name.toLowerCase(), version);
        artifact.set(Artifact.Attribute.PURL, purl);
        
        return artifact;
    }

    /**
     * Extracts an inventory from a .bower.json file without dependency information.
     *
     * @param bowerJson The .bower.json file to parse
     * @param relativePath The relative path to the file from the relevant basedir
     * @return An inventory populated with the modules defined in the .bower.json file
     */
    public Inventory extractInventory(File bowerJson, String relativePath) {
        return extractInventory(bowerJson, relativePath, Collections.emptyList());
    }
} 