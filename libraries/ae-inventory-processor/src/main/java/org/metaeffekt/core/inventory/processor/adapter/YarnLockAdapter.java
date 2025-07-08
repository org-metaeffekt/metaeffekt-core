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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.WebModuleComponentPatternContributor;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts an inventory for production npm modules based on a yarn.lock file.
 */
public class YarnLockAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(YarnLockAdapter.class);

    /**
     * Extracts an inventory for production npm modules based on a yarn.lock file.
     *
     * @param yarnLock The yarn.lock file to parse.
     * @param relativePath The relative path to the file from the relevant basedir.
     * @param dependencies The list of dependencies from package.json.
     * @return An inventory populated with the modules defined in the yarn.lock file.
     */
    public Inventory extractInventory(File yarnLock, String relativePath, List<WebModuleComponentPatternContributor.WebModuleDependency> dependencies) {
        try {
            LOG.debug("Parsing yarn.lock file: {}", yarnLock.getAbsolutePath());

            final List<String> lines = FileUtils.readLines(yarnLock, FileUtils.ENCODING_UTF_8);

            Map<String, NpmPackageLockAdapter.NpmModule> webModuleMap = new HashMap<>();

            // map to track dependency types directly from yarn.lock
            Map<String, String> dependencyTypeMap = new HashMap<>();

            String currentModuleId = null;
            boolean inDependencySection = false;
            boolean inOptionalDependencySection = false;
            boolean inPeerDependencySection = false;
            boolean inDevDependencySection = false;

            // detect yarn.lock format version
            boolean isYarnV1Format = true;
            for (String line : lines) {
                if (line.contains("__metadata")) {
                    isYarnV1Format = false;
                    break;
                }
            }

            LOG.debug("Detected yarn.lock format: {}", isYarnV1Format ? "v1" : "v2+");

            int lineNumber = 0;
            for (String line : lines) {
                lineNumber++;
                try {
                    if (line.startsWith("#")) continue;
                    if (StringUtils.isBlank(line)) continue;

                    // Check for dependency section markers
                    if (line.startsWith("  dependencies:")) {
                        inDependencySection = true;
                        inOptionalDependencySection = false;
                        inPeerDependencySection = false;
                        inDevDependencySection = false;
                        continue;
                    }
                    if (line.startsWith("  optionalDependencies:")) {
                        inDependencySection = false;
                        inOptionalDependencySection = true;
                        inPeerDependencySection = false;
                        inDevDependencySection = false;
                        continue;
                    }
                    if (line.startsWith("  peerDependencies:")) {
                        inDependencySection = false;
                        inOptionalDependencySection = false;
                        inPeerDependencySection = true;
                        inDevDependencySection = false;
                        continue;
                    }
                    if (line.startsWith("  devDependencies:")) {
                        inDependencySection = false;
                        inOptionalDependencySection = false;
                        inPeerDependencySection = false;
                        inDevDependencySection = true;
                        continue;
                    }

                    if (line.startsWith("    ")) {
                        // dependency within a section
                        final Pair<String, String> keyValuePair = extractKeyValuePair(line);
                        if (keyValuePair == null) continue;

                        // Store dependency type information
                        String depName = keyValuePair.getLeft();
                        if (inOptionalDependencySection) {
                            dependencyTypeMap.put(depName, Constants.MARKER_OPTIONAL_DEPENDENCY);
                            LOG.debug("Found optional dependency in yarn.lock: {}", depName);
                        } else if (inPeerDependencySection) {
                            dependencyTypeMap.put(depName, Constants.MARKER_PEER_DEPENDENCY);
                            LOG.debug("Found peer dependency in yarn.lock: {}", depName);
                        } else if (inDevDependencySection) {
                            dependencyTypeMap.put(depName, Constants.MARKER_DEVELOPMENT);
                            LOG.debug("Found development dependency in yarn.lock: {}", depName);
                        }
                        continue;
                    }
                    if (line.startsWith("  ")) {
                        // attribute
                        final Pair<String, String> keyValuePair = extractKeyValuePair(line);

                        if (keyValuePair == null) continue;

                        // process attribute
                        NpmPackageLockAdapter.NpmModule npmModule = webModuleMap.get(currentModuleId);
                        if (npmModule == null) continue;

                        if ("version".equalsIgnoreCase(keyValuePair.getKey())) {
                            npmModule.setVersion(keyValuePair.getRight());
                        }
                        if ("resolved".equalsIgnoreCase(keyValuePair.getKey())) {
                            npmModule.setUrl(keyValuePair.getRight());
                        }
                        // Check for dev dependency marker
                        if ("dev".equalsIgnoreCase(keyValuePair.getKey()) && "true".equalsIgnoreCase(keyValuePair.getRight())) {
                            dependencyTypeMap.put(npmModule.getName(), Constants.MARKER_DEVELOPMENT);
                            LOG.debug("Found development dependency marker in yarn.lock: {}", npmModule.getName());
                        }
                        // Check for optional dependency marker
                        if ("optional".equalsIgnoreCase(keyValuePair.getKey()) && "true".equalsIgnoreCase(keyValuePair.getRight())) {
                            dependencyTypeMap.put(npmModule.getName(), Constants.MARKER_OPTIONAL_DEPENDENCY);
                            LOG.debug("Found optional dependency marker in yarn.lock: {}", npmModule.getName());
                        }
                        continue;
                    }
                    // module
                    line = line.trim();

                    line = trimColon(line);

                    // Handle different formats for Yarn v1 and v2+
                    if (isYarnV1Format) {
                        final Pair<String, String> nameVersionPair = extractNameVersionPair(line);
                        if (nameVersionPair == null) continue;

                        // FIXME-KKL: we used to use relativePath here; that however produced duplication within
                        //  the path; explicit tests required.
                        final NpmPackageLockAdapter.NpmModule webModule =
                                new NpmPackageLockAdapter.NpmModule(nameVersionPair.getKey(), relativePath);

                        final String webModuleId = line;
                        webModuleMap.put(webModuleId, webModule);

                        // subsequent parse events are centric to this module
                        currentModuleId = webModuleId;
                    } else {
                        // Yarn v2+ format
                        if (line.contains("@")) {
                            String moduleName = line;
                            if (moduleName.contains(",")) {
                                moduleName = moduleName.substring(0, moduleName.indexOf(",")).trim();
                            }

                            NpmPackageLockAdapter.NpmModule webModule =
                                    new NpmPackageLockAdapter.NpmModule(extractModuleName(moduleName), relativePath);

                            webModuleMap.put(moduleName, webModule);
                            currentModuleId = moduleName;
                        }
                    }

                    // Reset section flags when starting a new module
                    inDependencySection = false;
                    inOptionalDependencySection = false;
                    inPeerDependencySection = false;
                    inDevDependencySection = false;
                } catch (Exception e) {
                    LOG.warn("Error parsing line {} in yarn.lock file: {}", lineNumber, e.getMessage());
                    // Continue with next line
                }
            }

            LOG.debug("Found {} modules in yarn.lock file", webModuleMap.size());
            LOG.debug("Found {} dependency type markers in yarn.lock file", dependencyTypeMap.size());

            if (dependencies != null) {
                LOG.debug("Consolidating with {} dependencies from package.json", dependencies.size());
            }

            return createInventory(webModuleMap, relativePath, dependencies, dependencyTypeMap);

        } catch (Exception e) {
            LOG.warn("Cannot read / parse [{}]: {}", yarnLock.getAbsoluteFile(), e.getMessage(), e);
        }
        return null;
    }

    /**
     * Extracts an inventory for production npm modules based on a yarn.lock file.
     *
     * @param yarnLock The yarn.lock file to parse.
     * @param relativePath The relative path to the file from the relevant basedir.
     * @return An inventory populated with the modules defined in the yarn.lock file.
     */
    public Inventory extractInventory(File yarnLock, String relativePath) {
        return extractInventory(yarnLock, relativePath, Collections.emptyList());
    }

    private Inventory createInventory(Map<String, NpmPackageLockAdapter.NpmModule> webModuleMap, String path,
                                     List<WebModuleComponentPatternContributor.WebModuleDependency> dependencies,
                                     Map<String, String> dependencyTypeMap) {
        Inventory inventory = new Inventory();

        // First build a map of dependencies from package.json for faster lookup
        Map<String, WebModuleComponentPatternContributor.WebModuleDependency> dependencyMap = new HashMap<>();
        for (WebModuleComponentPatternContributor.WebModuleDependency dependency : dependencies) {
            dependencyMap.put(dependency.getName(), dependency);
        }

        // Populate inventory with modules
        for (NpmPackageLockAdapter.NpmModule module : webModuleMap.values()) {
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

            // Check if we have dependency information from package.json (takes precedence)
            WebModuleComponentPatternContributor.WebModuleDependency dependency = dependencyMap.get(componentName);
            if (dependency != null) {
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
            }
            // If no info from package.json, use info from yarn.lock
            else if (dependencyTypeMap.containsKey(componentName)) {
                artifact.set(assetId, dependencyTypeMap.get(componentName));
            }

            String purl = NpmPackageLockAdapter.buildPurl(module.getName(), module.getVersion());
            artifact.set(Artifact.Attribute.PURL, purl);

            inventory.getArtifacts().add(artifact);
        }

        // Add missing dependencies from package.json that weren't found in yarn.lock
        for (WebModuleComponentPatternContributor.WebModuleDependency dependency : dependencies) {
            boolean found = false;
            for (Artifact existingArtifact : inventory.getArtifacts()) {
                if (dependency.getName().equals(existingArtifact.getComponent())) {
                    found = true;
                    break;
                }
            }

            if (!found && dependency.getVersion() != null) {
                Artifact artifact = new Artifact();
                artifact.setId(dependency.getName() + "-" + dependency.getVersion());
                artifact.setComponent(dependency.getName());
                artifact.setVersion(dependency.getVersion());
                artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + dependency.getName() + "]");

                String assetId = "AID-" + artifact.getId();

                if (dependency.isDevDependency()) {
                    artifact.set(assetId, Constants.MARKER_DEVELOPMENT);
                } else if (dependency.isPeerDependency()) {
                    artifact.set(assetId, Constants.MARKER_PEER_DEPENDENCY);
                } else if (dependency.isOptionalDependency()) {
                    artifact.set(assetId, Constants.MARKER_OPTIONAL_DEPENDENCY);
                }

                String purl = NpmPackageLockAdapter.buildPurl(dependency.getName(), dependency.getVersion());
                artifact.set(Artifact.Attribute.PURL, purl);

                inventory.getArtifacts().add(artifact);
            }
        }

        return inventory;
    }

    private String trimColon(String line) {
        if (line.endsWith(":")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    private Pair<String, String> extractKeyValuePair(String line) {
        line = line.trim();
        int separatorIndex = line.indexOf(" ");

        if (separatorIndex == -1) {
            LOG.error("Cannot parse " + line);
            return null;
        }

        final String key = line.substring(0, separatorIndex);
        final String value = line.substring(separatorIndex + 1);
        return Pair.of(trimQuotes(key), trimQuotes(value));
    }

    private Pair<String, String> extractNameVersionPair(String line) {
        line = line.trim();

        int commaIndex = line.lastIndexOf(",");
        if (commaIndex != -1) {
            line = line.substring(commaIndex + 1).trim();
        }

        line = trimQuotes(line);

        int separatorIndex = line.lastIndexOf("@");
        if (separatorIndex == -1) {
            LOG.error("Cannot parse " + line);
            return null;
        }

        final String key = line.substring(0, separatorIndex);

        if (StringUtils.isEmpty(key)) {
            LOG.error("Cannot parse " + line);
        }

        final String value = line.substring(separatorIndex + 1);

        return Pair.of(trimQuotes(key), trimQuotes(value));
    }

    private String trimQuotes(String key) {
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        return key;
    }

    /**
     * Extracts the module name from a yarn.lock entry.
     * Handles different formats like "@scope/name@version" or "name@version".
     */
    private String extractModuleName(String entry) {
        // Handle scoped packages
        if (entry.startsWith("@")) {
            int versionSeparator = entry.lastIndexOf("@");
            if (versionSeparator > 0) {
                return entry.substring(0, versionSeparator);
            }
        } else {
            int versionSeparator = entry.indexOf("@");
            if (versionSeparator > 0) {
                return entry.substring(0, versionSeparator);
            }
        }
        return entry;
    }

}
