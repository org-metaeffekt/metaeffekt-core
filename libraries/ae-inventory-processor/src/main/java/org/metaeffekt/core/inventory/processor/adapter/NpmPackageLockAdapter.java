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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.WebModuleComponentPatternContributor;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
public class NpmPackageLockAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NpmPackageLockAdapter.class);

    /**
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param relPath The relative path to the file from the relevant basedir.
     * @param projectName The name of the project for which to extract data.
     * @param dependencies The list of dependencies from package.json.
     *
     * @return An inventory populated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, String relPath, String projectName, List<WebModuleComponentPatternContributor.WebModuleDependency> dependencies) throws IOException {
        final Inventory inventory = new Inventory();

        populateInventory(packageLockJsonFile, inventory, relPath, projectName, dependencies);

        return inventory;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    public static final class NpmModule {
        final String name;
        final String path;

        @EqualsAndHashCode.Include
        final String id;

        @Setter
        String version;

        @Setter
        String url;

        @Setter
        String hash;

        @Setter
        boolean resolved;

        @Setter
        boolean devDependency;

        @Setter
        boolean peerDependency;

        @Setter
        boolean optionalDependency;

        public NpmModule(String name, String path) {
            this.name = name;
            this.path = path;

            this.id = path + name;
        }
    }

    private void populateInventory(File packageLockJsonFile, Inventory inventory, String path, String name, List<WebModuleComponentPatternContributor.WebModuleDependency> dependencies) throws IOException {
        final String json = FileUtils.readFileToString(packageLockJsonFile, FileUtils.ENCODING_UTF_8);
        final JSONObject obj = new JSONObject(json);

        // support old versions, where the dependencies were listed top-level.
        addDependencies(packageLockJsonFile, obj, inventory, path, "dependencies", dependencies);

        // in case a name is not provided, take all packages into account
        if (name == null) {
            addDependencies(packageLockJsonFile, obj, inventory, path, "packages", dependencies);
        } else {
            // name provided
            final JSONObject packages = obj.optJSONObject("packages");
            if (packages != null) {
                final JSONObject project = packages.optJSONObject(name);

                // if project with the given name was not found; fallback to all packages
                if (project == null) {
                    addDependencies(packageLockJsonFile, obj, inventory, path, "packages", dependencies);
                } else {
                    // otherwise parse project dependencies, only
                    parseProjectDependencies(inventory, path, project, packages);
                }
            }
        }
    }

    private static void parseProjectDependencies(Inventory inventory, String path, JSONObject project, JSONObject packages) {
        final Set<NpmModule> modules = collectModules(project.optJSONObject("dependencies"), "node_modules/", null);
        // dev dependencies
        modules.addAll(collectModules(project.optJSONObject("devDependencies"), "node_modules/", "devDependencies"));
        // optional dependencies
        modules.addAll(collectModules(project.optJSONObject("optionalDependencies"), "node_modules/", "optionalDependencies"));
        // peer dependencies
        modules.addAll(collectModules(project.optJSONObject("peerDependencies"), "node_modules/", "peerDependencies"));

        boolean changed;
        do {
            changed = false;
            for (NpmModule module : new HashSet<>(modules)) {
                if (module.isResolved()) continue;

                final String effectiveModuleId = resolveModuleId(module, packages, module.getId());
                final JSONObject moduleObject = packages.getJSONObject(effectiveModuleId);

                module.setUrl(moduleObject.optString("resolved"));
                module.setHash(moduleObject.optString("integrity"));
                module.setVersion(moduleObject.optString("version"));

                module.setDevDependency(moduleObject.optBoolean("dev"));
                module.setPeerDependency(moduleObject.optBoolean("peer"));
                module.setOptionalDependency(moduleObject.optBoolean("optional"));

                changed |= modules.addAll(collectModules(
                        moduleObject.optJSONObject("dependencies"), effectiveModuleId + "/node_modules/", null));

                module.setResolved(true);
            }
        } while (changed);

        populateInventory(inventory, path, modules);

    }

    public static void populateInventory(Inventory inventory, String path, Collection<NpmModule> modules) {
        for (NpmModule module : modules) {
            Artifact artifact = new Artifact();
            artifact.setId(module.getName() + "-" + module.getVersion());
            String componentName = module.getName();
            int slashIndex = componentName.indexOf("/");
            if (slashIndex > 0) {
                componentName = componentName.substring(0, slashIndex);
            }
            artifact.setComponent(componentName);
            artifact.setVersion(module.getVersion());
            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
            artifact.set("Source Archive - URL", module.getUrl());
            artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + module.getId() + "]");

         String assetId = "AID-" + artifact.getId();
            if (module.isDevDependency()) {
                artifact.set(assetId, Constants.MARKER_DEVELOPMENT);
            } else if (module.isPeerDependency()) {
                artifact.set(assetId, Constants.MARKER_PEER_DEPENDENCY);
            } else if (module.isOptionalDependency()) {
                artifact.set(assetId, Constants.MARKER_OPTIONAL_DEPENDENCY);
            }

            // NOTE: do not populate ARTIFACT_ROOT_PATHS; a rrot path is not known in this case

            String purl = buildPurl(module.getName(), module.getVersion());
            artifact.set(Artifact.Attribute.PURL, purl);

            inventory.getArtifacts().add(artifact);
        }
    }

    private static String resolveModuleId(NpmModule module, JSONObject packages, String effectiveModuleId) {
        JSONObject moduleObject = packages.optJSONObject(module.getId());
        while (moduleObject == null) {
            String alternativeId = effectiveModuleId;
            alternativeId = alternativeId.substring(0, alternativeId.lastIndexOf("/node_modules"));
            alternativeId = alternativeId.substring(0, alternativeId.lastIndexOf("/node_modules") + 14);
            alternativeId = alternativeId + module.getName();
            effectiveModuleId = alternativeId;
            moduleObject = packages.optJSONObject(effectiveModuleId);
        }
        return effectiveModuleId;
    }

    private static Set<NpmModule> collectModules(JSONObject dependencies, String path, String dependencyTag) {
        Set<NpmModule> modules = new HashSet<>();
        if (dependencies != null) {
            Map<String, Object> map = dependencies.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                NpmModule module = new NpmModule(entry.getKey(), path);
                if ("devDependencies".equals(dependencyTag)) {
                    module.setDevDependency(true);
                } else if ("peerDependencies".equals(dependencyTag)) {
                    module.setPeerDependency(true);
                } else if ("optionalDependencies".equals(dependencyTag)) {
                    module.setOptionalDependency(true);
                }
                modules.add(module);
            }
        }
        return modules;
    }

    private void addDependencies(File file, JSONObject obj, Inventory inventory, String path, String dependencyTag, List<WebModuleComponentPatternContributor.WebModuleDependency> dependencies) {
        final String prefix = "node_modules/";

        if (obj.has(dependencyTag)) {
            final JSONObject dependenciesObject = obj.getJSONObject(dependencyTag);
            for (String key : dependenciesObject.keySet()) {
                if (StringUtils.isBlank(key)) continue;
                final JSONObject dep = dependenciesObject.getJSONObject(key);

                String module = key;
                int index = module.lastIndexOf(prefix);
                if (index != -1) {
                    module = module.substring(index + prefix.length());
                }

                String version = dep.optString("version");
                String url = dep.optString("resolved");
                if (!dependencies.isEmpty()) {
                    for (WebModuleComponentPatternContributor.WebModuleDependency dependency : dependencies) {
                        if (module.equals(dependency.getName())) {
                            Artifact artifact = new Artifact();
                            if (version == null) {
                                version = dependency.getVersion();
                            }
                            artifact.setId(dependency.getName() + "-" + version);
                            artifact.setComponent(dependency.getName());
                            artifact.setVersion(dependency.getVersion());
                            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                            artifact.setUrl(url);
                            artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + key + "]");

                            // NOTE: do not populate ARTIFACT_ROOT_PATHS; a root path is not known in this case

                            String assetId = "AID-" + artifact.getId();

                            // Set dependency type marker based on the dependency type
                            if (dependency.isDevDependency()) {
                                // mark as development dependency
                                artifact.set(assetId, Constants.MARKER_DEVELOPMENT);
                            } else if (dependency.isPeerDependency()) {
                                // mark as peer dependency
                                artifact.set(assetId, Constants.MARKER_PEER_DEPENDENCY);
                            } else if (dependency.isOptionalDependency()) {
                                // mark as optional dependency
                                artifact.set(assetId, Constants.MARKER_OPTIONAL_DEPENDENCY);
                            }
                            // production dependencies don't need a special marker

                            String purl = buildPurl(dependency.getName(), dependency.getVersion());
                            artifact.set(Artifact.Attribute.PURL, purl);

                            inventory.getArtifacts().add(artifact);
                            addDependencies(file, dep, inventory, path, dependencyTag, dependencies);
                        }
                    }
                } else {
                    if (version != null) {
                        Artifact artifact = new Artifact();
                        artifact.setId(module + "-" + version);
                        artifact.setComponent(module);
                        artifact.setVersion(version);
                        artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                        artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                        artifact.setUrl(url);
                        artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + key + "]");

                        String purl = buildPurl(module, version);
                        artifact.set(Artifact.Attribute.PURL, purl);

                        inventory.getArtifacts().add(artifact);

                    }
                }

                if (version == null) {
                    LOG.warn("Missing version information in [{}] parse package-lock.json file as expected: {}", key, file.getAbsolutePath());
                }
            }
        }
    }

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }

}
