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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
@Slf4j
public class NpmPackageLockAdapter {

    /**
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param relPath The relative path to the file from the relevant basedir.
     * @param webModule The webModule for which to extract data.
     *
     * @return An inventory populated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, String relPath, WebModule webModule) throws IOException {

        // parse dependency tree from lock; may use the webModule name for filtering (project in workspace)
        final Map<String, NpmModule> modules = parseModules(packageLockJsonFile, webModule);

        // merge web module direct dependency information
        for (WebModuleDependency wmd : webModule.getDirectDependencies()) {
            final String queryName = "node_modules/" + wmd.getName();
            final NpmModule npmModule = modules.get(queryName);
            if (npmModule != null) {
                // module dependency attributes in the context
                npmModule.setRuntimeDependency(wmd.isRuntimeDependency());
                npmModule.setDevDependency(wmd.isDevDependency());
                npmModule.setPeerDependency(wmd.isPeerDependency());
                npmModule.setOptionalDependency(wmd.isOptionalDependency());
            } else {
                log.warn("Module [{}] not found.", queryName);
            }
        }

        // populate data into inventory
        final Inventory inventory = new Inventory();
        populateInventory(inventory, relPath, webModule, modules);
        return inventory;
    }

    private Map<String, NpmModule> parseModules(File packageLockJsonFile, WebModule webModule) throws IOException {
        final String json = FileUtils.readFileToString(packageLockJsonFile, FileUtils.ENCODING_UTF_8);
        JSONObject allPackages = new JSONObject(json);

        // detect format variant; the one with packages has an additional upper level.
        JSONObject packages = allPackages.optJSONObject("packages");
        JSONObject specificPackage = null;

        if (packages == null) {
            log.info("Single package found in [{}].", packageLockJsonFile.getAbsolutePath());
        } else {
            // change level
            allPackages = packages;

            // in this case we may want to filter / select a package
            log.info("Multiple packages found in [{}].", packageLockJsonFile.getAbsolutePath());

            if (StringUtils.isNotBlank(webModule.getName())) {
                specificPackage = packages.optJSONObject(webModule.getName());

                if (specificPackage != null) {
                    log.info("Found matching package in [{}] with name [{}].", packageLockJsonFile.getAbsolutePath(), webModule.getName());
                }
            }
        }

        // here allPackages indicates the level where all packages are provided
        // here specificPackage is either null or points to an object/module

        // build map with all modules covered by the lock file
        final String prefix = "node_modules/";
        final Map<String, NpmModule> npmModuleMap = new HashMap<>();

        for (String key : allPackages.keySet()) {
            String module = key;

            int index = module.lastIndexOf(prefix);
            if (index != -1) {
                module = module.substring(index + prefix.length());
            }

            final JSONObject jsonObject = allPackages.getJSONObject(key);

            final NpmModule npmModule = new NpmModule(module, key);
            parseModuleContent(npmModule, jsonObject);

            if (StringUtils.isNotBlank(npmModule.getVersion())) {
                npmModuleMap.put(key, npmModule);
            }
        }
        return npmModuleMap;
    }

    private void parseModuleContent(NpmModule npmModule, JSONObject specificPackage) {
        npmModule.setUrl(specificPackage.optString("resolved"));
        npmModule.setHash(specificPackage.optString("integrity"));
        npmModule.setVersion(specificPackage.optString("version"));

        npmModule.setDevDependency(specificPackage.optBoolean("dev"));
        npmModule.setPeerDependency(specificPackage.optBoolean("peer"));
        npmModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        npmModule.setRuntimeDependencies(collectModuleMap(specificPackage, "dependencies"));
        npmModule.setDevDependencies(collectModuleMap(specificPackage, "devDependencies"));
        npmModule.setPeerDependencies(collectModuleMap(specificPackage, "peerDependencies"));
        npmModule.setOptionalDependencies(collectModuleMap(specificPackage, "optionalDependencies"));
    }

    private Map<String, String> collectModuleMap(JSONObject specificPackage, String dependencies) {
        Map<String, String> nameVersionMap = new HashMap<>();
        if (dependencies != null) {
            JSONObject jsonObject = specificPackage.optJSONObject(dependencies);
            if (jsonObject != null) {
                Map<String, Object> map = jsonObject.toMap();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    nameVersionMap.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return nameVersionMap;
    }

    private void populateInventory(Inventory inventory, String path, WebModule webModule, Map<String, NpmModule> nameModuleMap) {

        final Map<String, Artifact> moduleNameArtifactMap = new HashMap<>();
        final Map<NpmModule, Artifact> npmModuleArtifactMap = new HashMap<>();

        Stack<NpmModule> stack = new Stack<>();

        Set<NpmModule> topLevelModules = new HashSet<>();
        Set<NpmModule> addedModules = new HashSet<>();

        for (WebModuleDependency webModuleDependency : webModule.getDirectDependencies()) {
            String name = webModuleDependency.getName();
            NpmModule npmModule = nameModuleMap.get("node_modules/" + name);
            log.info("{}", npmModule);
            stack.push(npmModule);
            topLevelModules.add(npmModule);
        }

        while (!stack.isEmpty()) {
            final NpmModule module = stack.pop();
            if (module == null) continue;

            final Artifact artifact = new Artifact();
            System.out.println(module.getName());


            artifact.setId(module.getName() + "-" + module.getVersion());

            // determine component name
            String componentName = module.getName();
            int slashIndex = componentName.indexOf("/");
            if (slashIndex > 0) {
                componentName = componentName.substring(0, slashIndex);
            }
            artifact.setComponent(componentName);

            // contribute other attributes
            artifact.setVersion(module.getVersion());
            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
            artifact.set("Source Archive - URL", module.getUrl());
            artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + module.getPath() + "]");

            final String purl = buildPurl(module.getName(), module.getVersion());
            artifact.set(Artifact.Attribute.PURL, purl);

            // add relationship to asset table
            final String assetId = "AID-" + webModule.getName() + "-" + webModule.getVersion();

            if (topLevelModules.contains(module)) {
                if (module.isDevDependency()) {
                    artifact.set(assetId, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
                } else if (module.isRuntimeDependency()) {
                    artifact.set(assetId, Constants.MARKER_RUNTIME_DEPENDENCY);
                } else if (module.isPeerDependency()) {
                    artifact.set(assetId, Constants.MARKER_PEER_DEPENDENCY);
                } else if (module.isOptionalDependency()) {
                    artifact.set(assetId, Constants.MARKER_OPTIONAL_DEPENDENCY);
                }
            } else {
                if (module.isDevDependency()) {
                    artifact.set(assetId, "(" + Constants.MARKER_DEVELOPMENT_DEPENDENCY + ")");
                } else if (module.isRuntimeDependency()) {
                    artifact.set(assetId, "(" + Constants.MARKER_RUNTIME_DEPENDENCY + ")");
                } else if (module.isPeerDependency()) {
                    artifact.set(assetId, "(" + Constants.MARKER_PEER_DEPENDENCY + ")");
                } else if (module.isOptionalDependency()) {
                    artifact.set(assetId, "(" + Constants.MARKER_OPTIONAL_DEPENDENCY + ")");
                }
            }
            // in the else case the module is not in a direct relationship (rather a transitive dependency)

            // NOTE: do not populate ARTIFACT_ROOT_PATHS; a root path is not known in this case

            inventory.getArtifacts().add(artifact);

            npmModuleArtifactMap.put(module, artifact);
            moduleNameArtifactMap.put(artifact.getId(), artifact);

            pushDependencies(module, module.getRuntimeDependencies(), Constants.MARKER_RUNTIME_DEPENDENCY, nameModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getDevDependencies(), Constants.MARKER_DEVELOPMENT_DEPENDENCY, nameModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getPeerDependencies(), Constants.MARKER_PEER_DEPENDENCY, nameModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getOptionalDependencies(), Constants.MARKER_OPTIONAL_DEPENDENCY, nameModuleMap, stack, npmModuleArtifactMap);
        }

        // fill in transitive dependency information
        // step 1:
        for (NpmModule module : npmModuleArtifactMap.keySet()) {
            apply(module, module.getRuntimeDependencies(), npmModuleArtifactMap, nameModuleMap, Constants.MARKER_RUNTIME_DEPENDENCY);
            apply(module, module.getDevDependencies(), npmModuleArtifactMap, nameModuleMap, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
            apply(module, module.getPeerDependencies(), npmModuleArtifactMap, nameModuleMap, Constants.MARKER_PEER_DEPENDENCY);
            apply(module, module.getOptionalDependencies(), npmModuleArtifactMap, nameModuleMap, Constants.MARKER_OPTIONAL_DEPENDENCY);
        }


        // step 2: transitive evaluation by module
        for (Map.Entry<NpmModule, Artifact> entry : npmModuleArtifactMap.entrySet()) {
            NpmModule module = entry.getKey();

            // TODO: build edge-pairs, evaluate edge-pairs (evaluatable first), fill inventory
        }

    }

    private void pushDependencies(NpmModule dependentNpmModule, Map<String, String> dependencyMap, String dependencyType, Map<String, NpmModule> nameModuleMap, Stack<NpmModule> stack, Map<NpmModule, Artifact> npmModuleArtifactMap) {
        for  (Map.Entry<String, String> entry : dependencyMap.entrySet()) {
            final String path = "node_modules/" + entry.getKey();
            final NpmModule dependencyModule = nameModuleMap.get(path);
            if (dependencyModule == null) {
                log.warn("Module [{}] not resolved using path [{}]. Potentially an optional dependency of [{}].", entry.getKey() + "@" + entry.getValue(), path, dependentNpmModule.getName());
            } else {
                // manage dependent modules
                if (!dependencyModule.getDependentModules().contains(dependentNpmModule)) {
                    dependencyModule.getDependentModules().add(dependentNpmModule);
                }
                // manage stack; add in case not already processed
                if (!npmModuleArtifactMap.containsKey(dependencyModule)) {
                    stack.push(dependencyModule);
                }
            }
        }
    }

    private static void apply(NpmModule npmModule, Map<String, String> dependencyMap, Map<NpmModule, Artifact> npmModuleArtifactMap, Map<String, NpmModule> nameModuleMap, String dependencyType) {
        final Artifact dependendArtifact = npmModuleArtifactMap.get(npmModule);
        if (dependendArtifact == null) throw new IllegalStateException("No artifact found for module: " + npmModule);

        for  (Map.Entry<String, String> entry : dependencyMap.entrySet()) {
            final String path = "node_modules/" + entry.getKey();
            final NpmModule dependencyModule = nameModuleMap.get(path);
            if (dependencyModule == null) {
                log.warn("Module [{}] not resolved using path [{}]. Potentially an optional dependency of [{}].", entry.getKey() + "@" + entry.getValue(), path, npmModule.getName());
            } else {
                final Artifact dependencyArtifact = npmModuleArtifactMap.get(dependencyModule);
                if (dependencyArtifact != null) {
                    if (dependencyArtifact != dependendArtifact) {
                        dependencyArtifact.set("AID-" + dependendArtifact.getId(), dependencyType);
                    }
                } else {
                    log.warn("Unable to find dependency for module '" + npmModule.getName() + "'.");
                }
            }
        }
    }

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }

}
