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
package org.metaeffekt.core.inventory.processor.adapter.npm;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class YarnLockParser extends PackageLockParser {

    public YarnLockParser(File file) {
        super(file);
    }
    
    @Override
    public void parseModules(WebModule webModule) throws IOException {
        final Map<String, ResolvedModule> pathModuleMap = new HashMap<>();

        final List<YarnInfoBlock> yarnInfoBlocks = parseYarnLock(getFile());

        for (YarnInfoBlock yarnInfoBlock : yarnInfoBlocks) {
            String moduleName = yarnInfoBlock.getName();

            // __metadata is not a module
            if (moduleName != null && moduleName.startsWith("__metadata")) continue;

            final ResolvedModule module = new ResolvedModule(moduleName, getFile().getName());
            module.setSourceArchiveUrl(yarnInfoBlock.resolved);
            module.setHash(yarnInfoBlock.integrity);
            if (module.getHash() != null) {
                module.setHash(yarnInfoBlock.checksum);
            }
            module.setResolved(yarnInfoBlock.resolved != null);
            module.setVersion(yarnInfoBlock.version);

            // manage dependencies
            applyDependencies(yarnInfoBlock.getDependencies(), module::setRuntimeDependencies);
            applyDependencies(yarnInfoBlock.getPeerDependencies(), module::setPeerDependencies);
            applyDependencies(yarnInfoBlock.getOptionalDependencies(), module::setOptionalDependencies);

            for (String nameVersionRange : yarnInfoBlock.mappingNameVersionRangeList) {
                pathModuleMap.put(nameVersionRange, module);
            }
        }

        // required to use resolve
        setPathModuleMap(pathModuleMap);

        // merge web module direct dependency information; this is not required, but the check is good
        ResolvedModule rootModule = resolveNpmModule(null, webModule.getName(), webModule.getVersion());

        // the yarn lock may not include the root modules; therefore we add it
        if (rootModule == null) {
            if (StringUtils.isNotBlank(webModule.getName())) {
                // FIXME-KKL: unify creation from webmodule
                rootModule = new ResolvedModule(webModule.getName(), "");
                rootModule.setVersion(webModule.getVersion());
                rootModule.setUrl(webModule.getUrl());
            } else {
                // in any case we add dummy; to be revised
                rootModule = new ResolvedModule("dummy", "dummy");
            }

            pathModuleMap.put(rootModule.getName(), rootModule);
            setRootModule(rootModule);
        }

        // fill dependency information from package.json
        propagateDependencyDetails(webModule);
    }

    private static void applyDependencies(List<Pair<String, String>> depList, Consumer<Map<String, UnresolvedModule>> consumer) {
        if (depList != null) {
            Map<String, UnresolvedModule> depMap = new HashMap<>();
            for (Pair<String, String> dep : depList) {
                // for yarn the path is <name>@<version>
                String path = dep.getKey();
                String versionRange = dep.getValue();
                depMap.put(path, new UnresolvedModule(path, path, versionRange));
            }
            consumer.accept(depMap);
        }
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

        final private List<String> mappingNameVersionRangeList;

        private String name;

        private String version;
        private String resolved;
        private String resolution;
        private String languageName;
        private String linkType;
        private String checksum;
        private String integrity;
        private String conditions;
        private String cacheKey;

        /**
         * Dependencies are stored as pair of name and version
         */
        private List<Pair<String, String>> dependencies;
        private List<Pair<String, String>> dependenciesMeta;
        private List<Pair<String, String>> peerDependencies;
        private List<Pair<String, String>> peerDependenciesMeta;
        private List<Pair<String, String>> bin;

        private List<Pair<String, String>> optionalDependencies;

        public YarnInfoBlock(String[] names) {
            this.mappingNameVersionRangeList = Arrays.stream(names).map(YarnInfoBlock::trimKey).collect(Collectors.toList());

            name = names[0];
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
                        } else if (keyValuePair.getLeft().equals("conditions")) {
                            this.conditions = keyValuePair.getRight();
                        } else if (keyValuePair.getLeft().equals("cacheKey")) {
                            this.cacheKey = keyValuePair.getRight();
                        } else {
                            log.warn("Cannot extract from key-value pair: " + keyValuePair);
                        }
                    }
                }
            }
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

    public ResolvedModule resolveNpmModule(ResolvedModule dependentModule, String path, String versionRange) {
        ResolvedModule resolvedModule;

        resolvedModule = getPathModuleMap().get(path + "@npm:" + versionRange);
        if (resolvedModule != null) return resolvedModule;

        resolvedModule = getPathModuleMap().get(path + "@" + versionRange);
        if (resolvedModule != null) return resolvedModule;

        resolvedModule = getPathModuleMap().get(path);
        if (resolvedModule != null) return resolvedModule;

        return resolvedModule;
    }

}
