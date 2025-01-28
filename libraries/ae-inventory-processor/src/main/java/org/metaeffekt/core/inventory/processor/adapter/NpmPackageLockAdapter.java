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
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
public class NpmPackageLockAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NpmPackageLockAdapter.class);

    /**
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param relPath The relative path to the file from the relevant basedir.
     * @param projectName The name of the project for which to extract data.
     *
     * @return An inventory populated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, String relPath, String projectName) throws IOException {
        final Inventory inventory = new Inventory();

        populateInventory(packageLockJsonFile, inventory, relPath, projectName);

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

        public NpmModule(String name, String path) {
            this.name = name;
            this.path = path;

            this.id = path + name;
        }
    }

    private void populateInventory(File packageLockJsonFile, Inventory inventory, String path, String name) throws IOException {
        final String json = FileUtils.readFileToString(packageLockJsonFile, FileUtils.ENCODING_UTF_8);
        final JSONObject obj = new JSONObject(json);

        // support old versions, where the dependencies were listed top-level.
        addDependencies(packageLockJsonFile, obj, inventory, path, "dependencies");

        // in case a name is not provided, take all packages into account
        if (name == null) {
            addDependencies(packageLockJsonFile, obj, inventory, path, "packages");
        } else {
            // name provided
            final JSONObject packages = obj.optJSONObject("packages");
            if (packages != null) {
                final JSONObject project = packages.optJSONObject(name);

                // if project with the given name was not found; fallback to all packages
                if (project == null) {
                    addDependencies(packageLockJsonFile, obj, inventory, path, "packages");
                } else {
                    // otherwise parse project dependencies, only
                    parseProjectDependencies(inventory, path, project, packages);
                }
            }
        }
    }

    private static void parseProjectDependencies(Inventory inventory, String path, JSONObject project, JSONObject packages) {
        // NOTE: we ignore the devDependencies
        final Set<NpmModule> modules = collectModules(project.optJSONObject("dependencies"), "node_modules/");

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

                final Boolean dev = moduleObject.optBoolean("dev");
                if (dev != null && dev) {
                    LOG.warn("Invariant violated. Expecting only non-dev dependency in production dependencies. " +
                            "Module [{}] is a development dependency.", module.getName());
                }

                changed |= modules.addAll(collectModules(
                        moduleObject.optJSONObject("dependencies"), effectiveModuleId + "/node_modules/"));

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

    private static Set<NpmModule> collectModules(JSONObject dependencies, String path) {
        Set<NpmModule> modules = new HashSet<>();
        if (dependencies != null) {
            Map<String, Object> map = dependencies.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                NpmModule module = new NpmModule(entry.getKey(), path);
                modules.add(module);
            }
        }
        return modules;
    }

    private void addDependencies(File file, JSONObject obj, Inventory inventory, String path, String dependencyTag) {
        final String prefix = "node_modules/";

        if (obj.has(dependencyTag)) {
            final JSONObject dependencies = obj.getJSONObject(dependencyTag);
            for (String key : dependencies.keySet()) {
                if (StringUtils.isBlank(key)) continue;

                final JSONObject dep = dependencies.getJSONObject(key);

                String version = dep.optString("version");
                String url = dep.optString("resolved");

                String module = key;
                int index = module.lastIndexOf(prefix);
                if (index != -1) {
                    module = module.substring(index + prefix.length());
                }

                if (version != null) {
                    Artifact artifact = new Artifact();
                    artifact.setId(module + "-" + version);
                    artifact.setComponent(module);
                    artifact.setVersion(version);
                    artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                    artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                    artifact.setUrl(url);
                    artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + key + "]");

                    // NOTE: do not populate ARTIFACT_ROOT_PATHS; a rrot path is not known in this case

                    String purl = buildPurl(module, version);
                    artifact.set(Artifact.Attribute.PURL, purl);

                    boolean production = !dep.has("dev") || !dep.getBoolean("dev");

                    // only consider production artifacts
                    if (production) {
                        inventory.getArtifacts().add(artifact);

                        // validate whether this is still required
                        addDependencies(file, dep, inventory, path, dependencyTag);
                    }
                } else {
                    LOG.warn("Missing version information in [{}] parse package-lock.json file as expected: {}", key, file.getAbsolutePath());
                }
            }
        }
    }

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }

}
