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

import lombok.Data;
import lombok.Getter;
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
        final Map<String, NpmModule> pathModuleMap = parseModules(packageLockJsonFile, webModule);

        // merge web module direct dependency information
        for (WebModuleDependency wmd : webModule.getDirectDependencies()) {
            NpmModule npmModule = pathModuleMap.get(wmd.getName());
            if (npmModule == null) {
                final String queryName = "node_modules/" + wmd.getName();
                npmModule = pathModuleMap.get(queryName);
            }
            if (npmModule != null) {
                // module dependency attributes in the context
                npmModule.setRuntimeDependency(wmd.isRuntimeDependency());
                npmModule.setDevDependency(wmd.isDevDependency());
                npmModule.setPeerDependency(wmd.isPeerDependency());
                npmModule.setOptionalDependency(wmd.isOptionalDependency());
            } else {
                log.warn("Module [{}] not found.", wmd.getName());
            }
        }

        // populate data into inventory
        final Inventory inventory = new Inventory();
        populateInventory(inventory, relPath, webModule, pathModuleMap);
        return inventory;
    }

    private Map<String, NpmModule> parseModules(File packageLockJsonFile, WebModule webModule) throws IOException {
        final String json = FileUtils.readFileToString(packageLockJsonFile, FileUtils.ENCODING_UTF_8);
        JSONObject allPackages = new JSONObject(json);

        // detect format variant; the one with packages has an additional upper level.
        JSONObject packages = allPackages.optJSONObject("packages");
        JSONObject specificPackage = null;

        final Map<String, NpmModule> pathModuleMap = new HashMap<>();
        final Map<String, NpmModule> qualifierModuleMap = new HashMap<>();

        if (packages == null) {
            log.info("Single package found in [{}].", packageLockJsonFile.getAbsolutePath());
        } else {
            // change level
            allPackages = packages;

            // in this case we may want to filter / select a package
            log.info("Multiple packages found in [{}].", packageLockJsonFile.getAbsolutePath());

            if (StringUtils.isNotBlank(webModule.getName())) {
                specificPackage = packages.optJSONObject(webModule.getName());
                if (specificPackage == null) {
                    log.warn("Matching package in [{}] with name [{}] not found.", packageLockJsonFile.getAbsolutePath(), webModule.getName());
                } else {
                    final NpmModule npmModule = new NpmModule(webModule.getName(), webModule.getName());
                    parseModuleContent(npmModule, specificPackage);

                    final String qualifier = npmModule.deriveQualifier();
                    pathModuleMap.put(webModule.getName(), npmModule);
                    qualifierModuleMap.put(qualifier, npmModule);
                }
            }
        }

        // build map with all modules covered by the lock file
        final String prefix = "node_modules/";

        for (String path : allPackages.keySet()) {
            String name = path;

            int index = name.lastIndexOf(prefix);
            if (index != -1) {
                name = name.substring(index + prefix.length());
            }

            final JSONObject jsonObject = allPackages.getJSONObject(path);

            final NpmModule npmModule = new NpmModule(name, path);
            parseModuleContent(npmModule, jsonObject);

            if (StringUtils.isNotBlank(npmModule.getName())) {
                final String qualifier = npmModule.deriveQualifier();
                final NpmModule qualifiedModule = qualifierModuleMap.get(qualifier);
                if (qualifiedModule == null) {
                    pathModuleMap.put(path, npmModule);
                    qualifierModuleMap.put(qualifier, npmModule);
                } else {
                    pathModuleMap.put(path, qualifiedModule);
                }
            }
        }
        return pathModuleMap;
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

    private void populateInventory(Inventory inventory, String path, WebModule webModule, Map<String, NpmModule> pathModuleMap) {

        final Map<NpmModule, Artifact> npmModuleArtifactMap = new HashMap<>();

        Stack<NpmModule> stack = new Stack<>();

        // Set<NpmModule> topLevelModules = new HashSet<>();

        // push self onto stack first
        final NpmModule npmModule = pathModuleMap.get(webModule.getName());
        if (npmModule != null) {
            stack.push(npmModule);
        } else {
            throw new IllegalStateException("Could not find artifact for [" + webModule.getName() + "] in dependency tree.");
        }

        final Map<String, Artifact> qualifierArtifactMap = new HashMap<>();

        while (!stack.isEmpty()) {
            final NpmModule module = stack.pop();
            if (module == null) continue;

            String qualifier = module.deriveQualifier();
            final Artifact artifact = qualifierArtifactMap.computeIfAbsent(qualifier, a -> new Artifact());

            artifact.setId(qualifier);

            // determine component name
            String componentName = module.getName();
            int slashIndex = componentName.lastIndexOf("/");
            if (slashIndex > 0) {
                componentName = componentName.substring(0, slashIndex);
            }
            artifact.setComponent(componentName);

            // contribute other attributes
            artifact.setVersion(module.getVersion());
            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
            artifact.set("Source Archive - URL", module.getUrl());

            artifact.append(Constants.KEY_PATH_IN_ASSET, "[" + module.getPath() + "]", Artifact.PATH_DELIMITER);

            final String purl = buildPurl(module.getName(), module.getVersion());
            artifact.set(Artifact.Attribute.PURL, purl);
            // NOTE: do not populate ARTIFACT_ROOT_PATHS; a root path is not known in this case

            inventory.getArtifacts().add(artifact);
            npmModuleArtifactMap.put(module, artifact);

            pushDependencies(module, module.getRuntimeDependencies(), pathModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getDevDependencies(), pathModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getPeerDependencies(), pathModuleMap, stack, npmModuleArtifactMap);
            pushDependencies(module, module.getOptionalDependencies(), pathModuleMap, stack, npmModuleArtifactMap);
        }

        // fill in transitive dependency information and collect DependencyTuples
        // step 1:
        final Stack<DependencyTuple> dependencyTuples = new Stack<>();
        for (NpmModule module : npmModuleArtifactMap.keySet()) {
            apply(module, module.getRuntimeDependencies(), npmModuleArtifactMap, pathModuleMap, Constants.MARKER_RUNTIME_DEPENDENCY, dependencyTuples);
            apply(module, module.getDevDependencies(), npmModuleArtifactMap, pathModuleMap, Constants.MARKER_DEVELOPMENT_DEPENDENCY, dependencyTuples);
            apply(module, module.getPeerDependencies(), npmModuleArtifactMap, pathModuleMap, Constants.MARKER_PEER_DEPENDENCY, dependencyTuples);
            apply(module, module.getOptionalDependencies(), npmModuleArtifactMap, pathModuleMap, Constants.MARKER_OPTIONAL_DEPENDENCY, dependencyTuples);
        }

        // step 2: extend the DependencyTuples to all possible DependencyTriples and conclude dependency types
        final Set<String> processedTuples = new HashSet<>();
        while (!dependencyTuples.isEmpty()) {
            final DependencyTuple dependencyTuple = dependencyTuples.pop();

            final NpmModule middleModule = dependencyTuple.getDependentModule();
            final NpmModule bottomModule = dependencyTuple.getDependencyModule();
            final String middleToBottomDependencyType = dependencyTuple.getDependencyType();

            final String tupleQualifier = deriveQualifier(middleModule, middleToBottomDependencyType, bottomModule);
            if (processedTuples.contains(tupleQualifier)) continue;

            processedTuples.add(tupleQualifier);

            log.warn("Dependency Tuple: {} depends ({}) on {}.",
                    middleModule.getName() + ":" + middleModule.getVersion(),
                    middleToBottomDependencyType,
                    bottomModule.getName() + ":" + middleModule.getVersion());

            for (NpmModule topModule : middleModule.getDependentModules()) {
                final Artifact middleArtifact = npmModuleArtifactMap.get(middleModule);
                final Artifact bottomArtifact = npmModuleArtifactMap.get(bottomModule);

                final String topAssetId = "AID-" + topModule.getName() + "-" + topModule.getVersion();
                final String topToMiddleDependencyType = middleArtifact.get(topAssetId);
                final String topToBottomDependencyType = bottomArtifact.get(topAssetId);

                final DependencyTriple dependencyTriple = new DependencyTriple(topModule, topToMiddleDependencyType, middleModule, middleToBottomDependencyType, bottomModule);

                if (topToBottomDependencyType == null) {
                    bottomArtifact.set(topAssetId, dependencyTriple.getTopToBottomDependencyType());
                }

                dependencyTuples.push(new DependencyTuple(dependencyTriple.getTopModule(), dependencyTriple.getTopToMiddleDependencyType(), dependencyTriple.getMiddleModule()));
            }
        }
    }

    private static String deriveQualifier(NpmModule dependentModule, String dependencyType, NpmModule dependencyModule) {
        String qualifier = dependentModule.getName() + dependentModule.getVersion();
        qualifier += "=" + dependencyType;
        qualifier += "=" + dependencyModule.getName() + dependencyModule.getVersion();
        return qualifier;
    }

    @Getter
    private static class DependencyTuple {
        private final NpmModule dependentModule;
        private final NpmModule dependencyModule;
        private final String dependencyType;

        public DependencyTuple(NpmModule dependentModule, String dependencyType, NpmModule dependencyModule) {
            this.dependentModule = dependentModule;
            this.dependencyModule = dependencyModule;
            this.dependencyType = dependencyType;
        }

    }

    @Getter
    private static class DependencyTriple {
        private final NpmModule topModule;
        private final NpmModule middleModule;
        private final NpmModule bottomModule;

        private String topToMiddleDependencyType;
        private final String middleToBottomDependencyType;

        public DependencyTriple(NpmModule topModule, String topToMiddleDependencyType, NpmModule middleModule, String middleToBottomDependencyType, NpmModule bottomModule) {
            this.topModule = topModule;
            this.middleModule = middleModule;
            this.bottomModule = bottomModule;
            this.topToMiddleDependencyType = topToMiddleDependencyType;
            this.middleToBottomDependencyType = middleToBottomDependencyType;
        }

        public void completeDependencyTypes() {
            if (middleToBottomDependencyType == null) throw new IllegalStateException("The dependency type towards the leaf module must be set.");

            if (topToMiddleDependencyType == null) {
                if (middleToBottomDependencyType.startsWith("(")) {
                    topToMiddleDependencyType = middleToBottomDependencyType;
                } else {
                    topToMiddleDependencyType = "(" +  middleToBottomDependencyType + ")";
                }
                System.out.println(topToMiddleDependencyType);
            }

        }

        public String getTopToBottomDependencyType() {
            switch (topToMiddleDependencyType + middleToBottomDependencyType) {
                case "dr" :
                case "dd" :
                case "dp" : return "(d)";
                case "do" : return "(o)";

                case "rr" : return "(r)";
                case "rd" : return "(d)";
                case "rp" : return "(r)";
                case "ro" : return "(o)";

                case "pr" : return "(r)";
                case "pd" : return "(d)";
                case "pp" : return "(p)";
                case "po" : return "(o)";

                case "or" : return "(o)";
                case "od" : return "(o)";
                case "op" : return "(o)";
                case "oo" : return "(o)";

                case "ur" : return "(u)";
                case "ud" : return "(u)";
                case "up" : return "(u)";
                case "uo" : return "(u)";
                case "ru" : return "(u)";
                case "du" : return "(u)";
                case "pu" : return "(u)";
                case "ou" : return "(u)";

                default:
                    return "u";
            }
        }
    }

    private void pushDependencies(NpmModule dependentNpmModule, Map<String, String> dependencyMap,
        Map<String, NpmModule> pathModuleMap, Stack<NpmModule> stack, Map<NpmModule, Artifact> npmModuleArtifactMap) {

        for  (Map.Entry<String, String> entry : dependencyMap.entrySet()) {
            final String path = "node_modules/" + entry.getKey();
            final NpmModule dependencyModule = pathModuleMap.get(path);
            if (dependencyModule == null) {
                log.warn("Module [{}] not resolved using path [{}]. Potentially an optional dependency of [{}].",
                        entry.getKey() + "@" + entry.getValue(), path, dependentNpmModule.getName());
            } else {
                // manage dependent modules
                final List<NpmModule> dependentModules = dependencyModule.getDependentModules();
                if (!dependentModules.contains(dependentNpmModule)) {
                    dependentModules.add(dependentNpmModule);
                }
                // manage stack; add in case not already processed
                if (!npmModuleArtifactMap.containsKey(dependencyModule)) {
                    stack.push(dependencyModule);
                }
            }
        }
    }

    private static void apply(NpmModule dependentModule, Map<String, String> dependencyMap,
              Map<NpmModule, Artifact> npmModuleArtifactMap, Map<String, NpmModule> nameModuleMap,
              String dependencyType, Stack<DependencyTuple> dependencyTuples) {

        final Artifact dependentArtifact = npmModuleArtifactMap.get(dependentModule);
        if (dependentArtifact == null) throw new IllegalStateException("No artifact found for module: " + dependentModule);

        for  (Map.Entry<String, String> entry : dependencyMap.entrySet()) {
            final String path = "node_modules/" + entry.getKey();
            final NpmModule dependencyModule = nameModuleMap.get(path);
            if (dependencyModule == null) {
                log.warn("Module [{}] not resolved using path [{}]. Potentially an optional dependency of [{}].", entry.getKey() + "@" + entry.getValue(), path, dependentModule.getName());
            } else {
                final Artifact dependencyArtifact = npmModuleArtifactMap.get(dependencyModule);
                if (dependencyArtifact != null) {
                    if (dependencyArtifact != dependentArtifact) {
                        dependencyArtifact.set("AID-" + dependentArtifact.getId(), dependencyType);

                        dependencyTuples.add(new DependencyTuple(dependentModule, dependencyType, dependencyModule));
                    }
                } else {
                    log.warn("Unable to find dependency for module '" + dependentModule.getName() + "'.");
                }
            }
        }
    }

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }

}
