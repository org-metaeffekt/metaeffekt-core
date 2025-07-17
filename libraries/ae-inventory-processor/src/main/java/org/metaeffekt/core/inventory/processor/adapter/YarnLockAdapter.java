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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts an inventory for production npm modules based on a yarn.lock file.
 *
 * Compare:
 * <ul>
 *     <li>https://www.arahansen.com/the-ultimate-guide-to-yarn-lock-lockfiles/</li>
 *     <li>https://ayc0.github.io/posts/yarn/yarn-lock-how-to-read-it/</li>
 * </ul>
 */
@Slf4j
public class YarnLockAdapter {

    /**
     * Extracts an inventory for production npm modules based on a yarn.lock file.
     *
     * @param yarnLock The yarn.lock file to parse.
     * @param relativePath The relative path to the file from the relevant basedir.
     * @param webModule The webModule of dependencies from package.json.
     *
     * @return An inventory populated with the modules defined in the yarn.lock file.
     */
    public Inventory extractInventory(File yarnLock, String relativePath, WebModule webModule) {
        try {
            log.debug("Parsing yarn.lock file: {}", yarnLock.getAbsolutePath());

            // list of npm modules parsed from the yarn.lock
            final List<NpmModule> webModuleList = new ArrayList<>();

            // map to track dependency types directly from yarn.lock; name to dependency type (d, p, o)
            final Map<String, String> dependencyTypeMap = new HashMap<>();

            final List<YarnInfoBlock> yarnInfoBlocks = parseYarnLock(yarnLock);

            for (YarnInfoBlock yarnInfoBlock : yarnInfoBlocks) {
                String moduleName = yarnInfoBlock.getName();

                // __metadata is not a module
                if (moduleName != null && moduleName.startsWith("__metadata")) continue;

                final NpmModule module = new NpmModule(moduleName, yarnLock.getName());
                module.setUrl(yarnInfoBlock.resolved);
                module.setHash(yarnInfoBlock.integrity);
                if (module.getHash() != null) {
                    module.setHash(yarnInfoBlock.checksum);
                }
                module.setResolved(yarnInfoBlock.resolved != null);
                module.setVersion(yarnInfoBlock.version);

                webModuleList.add(module);
            }

            log.debug("Found {} modules in yarn.lock file", webModuleList.size());
            log.debug("Found {} dependency type markers in yarn.lock file", dependencyTypeMap.size());

            List<WebModuleDependency> dependencies = webModule.getDirectDependencies();

            if (dependencies != null) {
                log.debug("Consolidating with {} dependencies from package.json", dependencies.size());
            }

            return createInventory(webModuleList, relativePath, dependencies, dependencyTypeMap);

        } catch (Exception e) {
            log.warn("Cannot read / parse [{}]: {}", yarnLock.getAbsoluteFile(), e.getMessage(), e);
        }
        return null;
    }

    private Inventory createInventory(List<NpmModule> webModules, String path,
                                     List<WebModuleDependency> dependencies,
                                     Map<String, String> dependencyTypeMap) {

        final Inventory inventory = new Inventory();

        // First build a map of dependencies from package.json for faster lookup
        final Map<String, WebModuleDependency> dependencyMap = new HashMap<>();
        for (WebModuleDependency dependency : dependencies) {
            dependencyMap.put(dependency.getName(), dependency);
        }

        // Populate inventory with modules
        for (NpmModule module : webModules) {

            if (module.getVersion() == null) continue;
            if (module.getName() == null) continue;

            final Artifact artifact = new Artifact();
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
            artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + module.getPath() + "]");

            String assetId = "AID-" + artifact.getId();

            // Check if we have dependency information from package.json (takes precedence)
            WebModuleDependency dependency = dependencyMap.get(componentName);
            if (dependency != null) {
                if (dependency.isDevDependency()) {
                    // mark as development dependency
                    artifact.set(assetId, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
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
        for (WebModuleDependency dependency : dependencies) {
            boolean found = false;
            for (Artifact existingArtifact : inventory.getArtifacts()) {
                if (dependency.getName().equals(existingArtifact.getComponent())) {
                    found = true;
                    break;
                }
            }

            if (!found && dependency.getResolvedVersion() != null) {
                Artifact artifact = new Artifact();
                artifact.setId(dependency.getName() + "-" + dependency.getResolvedVersion());
                artifact.setComponent(dependency.getName());
                artifact.setVersion(dependency.getResolvedVersion());
                artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + dependency.getName() + "]");

                String assetId = "AID-" + artifact.getId();

                if (dependency.isDevDependency()) {
                    artifact.set(assetId, Constants.MARKER_DEVELOPMENT_DEPENDENCY);
                } else if (dependency.isPeerDependency()) {
                    artifact.set(assetId, Constants.MARKER_PEER_DEPENDENCY);
                } else if (dependency.isOptionalDependency()) {
                    artifact.set(assetId, Constants.MARKER_OPTIONAL_DEPENDENCY);
                }

                String purl = NpmPackageLockAdapter.buildPurl(dependency.getName(), dependency.getResolvedVersion());
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
            log.error("Cannot parse " + line);
            return null;
        }

        String key = line.substring(0, separatorIndex);
        if (key.endsWith(":")) key = key.substring(0, key.length() - 1);
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
            log.error("Cannot parse " + line);
            return null;
        }

        final String key = line.substring(0, separatorIndex);

        if (StringUtils.isEmpty(key)) {
            log.error("Cannot parse " + line);
        }

        final String value = line.substring(separatorIndex + 1);

        return Pair.of(trimQuotes(key), trimQuotes(value));
    }

    private String trimQuotes(String string) {
        if (string.endsWith(":")) {
            string = string.substring(0, string.length() - 1);
        }
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length() - 1);
        }
        return string;
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

    protected List<YarnInfoBlock> parseYarnLock(File file) throws IOException {
        final List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

        // yarn.lock files are organized in blocks.
        // a block starts with a line without indentation
        // # are used to comment

        YarnInfoBlock currentBlock = null;
        List<String> currentBlockLines = null;

        List<YarnInfoBlock> parsedBlocks = new ArrayList<>();

        for (String line : lines) {
            if (StringUtils.isBlank(line)) continue;
            if (line.startsWith("#") || line.startsWith("\t") || line.startsWith(" ")) {
                if (currentBlockLines != null) {
                    currentBlockLines.add(line);
                }
                continue;
            }

            // here we have identified a first or new block

            if (currentBlock != null) {
                currentBlock.parse(currentBlockLines);
                parsedBlocks.add(currentBlock);
            }

            // a block may have a single name or many names seperated by ", "
            String[] blockNames = YarnInfoBlock.trimKey(line).split(", *");

            // start a new block
            currentBlock = new YarnInfoBlock(blockNames);
            currentBlockLines = new ArrayList<>();
        }

        if (currentBlock != null) {
            currentBlock.parse(currentBlockLines);
            parsedBlocks.add(currentBlock);
        }

        return parsedBlocks;
    }

    @Data
    public static final class YarnInfoBlock {

       final private List<String> blockNames;

       private String name;

       private String version;
       private String resolved;
       private String resolution;
       private String languageName;
       private String linkType;
       private String checksum;
       private String integrity;

        /**
         * Dependencies are stored as pair of name and version
         */
        private List<Pair<String, String>> dependencies;
        private List<Pair<String, String>> dependenciesMeta;
        private List<Pair<String, String>> peerDependencies;
        private List<Pair<String, String>> peerDependenciesMeta;
        private List<Pair<String, String>> bin;
        private List<Pair<String, String>> conditions;
        private List<Pair<String, String>> optionalDependencies;

        public YarnInfoBlock(String[] names) {
            this.blockNames = Arrays.stream(names).map(YarnInfoBlock::trimKey).collect(Collectors.toList());

            String name = this.blockNames.get(0);
            int atIndex = name.lastIndexOf("@");
            if (atIndex == -1) {
                this.name = name;
            } else {
                this.name = name.substring(0, atIndex);
            }
        }

        public void parse(List<String> currentBlockLines) {

            boolean parsingDependencies = false;
            boolean parsingDependenciesMeta = false;
            boolean parsingOptionalDependencies = false;
            boolean parsingPeerDependencies = false;
            boolean parsingPeerDependenciesMeta = false;
            boolean parsingBin = false;
            boolean parsingConditions = false;

            for (String line : currentBlockLines) {

                // a line is either a list-key or a key-value pair
                // as delimiter either " " or ": " are used

                // a line starts either with "  " or "    " (list element)

                if (line.startsWith("    ")) {
                    final Pair<String, String> keyValuePair = parseKeyValuePair(line);
                    if (parsingDependencies) {
                        dependencies.add(keyValuePair);
                    } else if (parsingDependenciesMeta) {
                        dependenciesMeta.add(keyValuePair);
                    } else if (parsingPeerDependencies) {
                        peerDependencies.add(keyValuePair);
                    } else if (parsingPeerDependenciesMeta) {
                        peerDependenciesMeta.add(keyValuePair);
                    } else if (parsingBin) {
                        bin.add(keyValuePair);
                    } else if (parsingConditions) {
                        conditions.add(keyValuePair);
                    } else if (parsingOptionalDependencies) {
                        optionalDependencies.add(keyValuePair);
                    } else {
                        log.warn("Cannot extract from: " + keyValuePair);
                    }
                } else if (line.startsWith("  ")) {
                    parsingDependencies = false;
                    parsingDependenciesMeta = false;
                    parsingPeerDependencies = false;
                    parsingPeerDependenciesMeta = false;
                    parsingBin = false;
                    parsingConditions = false;
                    parsingOptionalDependencies = false;

                    final Pair<String, String> keyValuePair = parseKeyValuePair(line);
                    if (keyValuePair.getRight() == null) {
                        // list key

                        if (keyValuePair.getLeft().equals("dependencies")) {
                            this.dependencies = new ArrayList<>();
                            parsingDependencies = true;
                        } else if (keyValuePair.getLeft().equals("dependenciesMeta")) {
                            this.dependenciesMeta = new ArrayList<>();
                            parsingDependenciesMeta = true;
                        } else if (keyValuePair.getLeft().equals("peerDependencies")) {
                            this.peerDependencies = new ArrayList<>();
                            parsingPeerDependencies = true;
                        } else if (keyValuePair.getLeft().equals("peerDependenciesMeta")) {
                            this.peerDependenciesMeta = new ArrayList<>();
                            parsingPeerDependenciesMeta = true;
                        } else if (keyValuePair.getLeft().equals("bin")) {
                            this.bin = new ArrayList<>();
                            parsingBin = true;
                        } else if (keyValuePair.getLeft().equals("conditions")) {
                            this.conditions = new ArrayList<>();
                            parsingConditions = true;
                        } else if (keyValuePair.getLeft().equals("optionalDependencies")) {
                            this.optionalDependencies = new ArrayList<>();
                            parsingOptionalDependencies = true;
                        } else {
                            log.warn("Cannot extract from list-key: " + keyValuePair);
                        }
                    } else {
                        // key value pair

                        if (keyValuePair.getLeft().equals("version")) {
                            this.version = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("resolved")) {
                            this.resolved = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("resolution")) {
                            this.resolution = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("checksum")) {
                            this.checksum = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("integrity")) {
                            this.integrity = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("languageName")) {
                            this.languageName = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("linkType")) {
                            this.linkType = keyValuePair.getRight();
                        } else {
                            log.warn("Cannot extract from key-value pair: " + keyValuePair);
                        }
                    }
                }
            }

            log.info("{}", this);

        }

        private Pair<String, String> parseKeyValuePair(String line) {
            String internalLine = line.trim();

            int separatorIndex = internalLine.indexOf(" ");

            String key;
            String value;

            if (separatorIndex == -1) {
                key = internalLine;
                value = null;
            } else {
                key = internalLine.substring(0, separatorIndex);
                value = internalLine.substring(separatorIndex + 1);
            }

            if (StringUtils.isBlank(value)) value = null;

            key = trimKey(key);
            value = trimQuotes(value);

            return Pair.of(key, value);
        }

        private static String trimKey(String key) {
            // remove trailing colon
            if (key.endsWith(":")) {
                key = key.substring(0, key.length() - 1);
            }
            return trimQuotes(key);
        }

        private static String trimQuotes(String string) {
            if (string == null) return null;
            if (string.startsWith("\"") && string.endsWith("\"")) {
                string = string.substring(1, string.length() - 1);
            }
            return string;
        }
    }

}
