/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for parsing poetry and pdm lock files.
 */
@Slf4j
public abstract class AbstractLockFileParser implements LockFileParser {

    /**
     * Extracts data for packages in a .lock file as resolved modules.
     *
     * @param lockRoot the lock file root node
     * @return list of resolved packages
     */
    protected List<ResolvedModule> extractPackages(JsonNode lockRoot) {
        final List<ResolvedModule> resolvedModules = new ArrayList<>();
        final JsonNode packages = lockRoot.path("package");
        if (!packages.isArray()) {
            return resolvedModules;
        }
        packages.forEach(packageNode -> {
                    final ResolvedModule module = extractPackage(packageNode, lockRoot);
                    resolvedModules.add(module);
                }
        );
        return resolvedModules;
    }

    /**
     * Extracts the dependencies from a package.
     *
     * @param dependenciesNode the dependencies node
     * @return map consisting of the dependency name and itself as a unresolved module
     */
    protected abstract Map<String, UnresolvedModule> extractDependencies(JsonNode dependenciesNode);


    /**
     * Parses information of the package source attribute.
     *
     * @param packageNode the package node
     * @return the extracted source data
     */
    protected abstract PyProjectPackageSource parseSource(JsonNode packageNode);

    /**
     * Extracts package information for one package as a resolved module.
     *
     * @param packageNode the package node
     * @return the resolved package
     */
    private ResolvedModule extractPackage(JsonNode packageNode, JsonNode lockRoot) {
        final ResolvedModule module = new ResolvedModule(packageNode.path("name").asText(), null);
        module.setVersion(packageNode.path("version").asText(null));
        final Map<String, UnresolvedModule> unresolvedModuleMap = extractDependencies(packageNode.path("dependencies"));
        module.setRuntimeDependencies(unresolvedModuleMap);

        module.setPyProjectPackageSource(parseSource(packageNode));
        module.setPyProjectPackageFiles(collectFiles(packageNode, lockRoot));

        return module;
    }

    /**
     * Collects the file elements from the files array in [[package]], or if the lock version is 1.x from metadata.files.
     *
     * @param packageNode the package node
     * @param lockRoot    the lock file root node used for extracting the centralized files
     * @return JSON array containing the files and the hashes for a package
     */
    private JSONArray collectFiles(JsonNode packageNode, JsonNode lockRoot) {
        final boolean filesAreCentralized = lockRoot.path("metadata").path("lock-version").asText().startsWith("1.");
        if (filesAreCentralized) {
            final Map<String, JSONArray> packageNamesToFilesMap = parseMetadataFiles(lockRoot.at("/metadata/files"));
            return packageNamesToFilesMap.get(packageNode.path("name").asText());
        }
        return extractFilesAndAddToArray(packageNode.path("files"));
    }

    /**
     * In lock versions 1.x the files are listed under metadata.files, as opposed to version 2.0 and above where they are listed for each package under [[package]].files.
     *
     * @param metadataFiles the files node
     * @return a map containing the package name as the key and the files as its values (as JSONArray)
     */
    private static Map<String, JSONArray> parseMetadataFiles(JsonNode metadataFiles) {
        if (!metadataFiles.isObject()) {
            return Map.of();
        }

        final Map<String, JSONArray> packageNamesToFilesMap = new LinkedHashMap<>();
        metadataFiles.propertyStream().forEach(entry -> {
            final JSONArray filesArray = extractFilesAndAddToArray(entry.getValue());
            packageNamesToFilesMap.put(entry.getKey(), filesArray);
        });
        return packageNamesToFilesMap;
    }

    private static JSONArray extractFilesAndAddToArray(JsonNode files) {
        final JSONArray filesArray = new JSONArray();
        if (!files.isArray()) {
            return filesArray;
        }

        for (final JsonNode file : files) {
            filesArray.put(new JSONObject()
                    .put("file", file.path("file").asText(null))
                    .put("hash", file.path("hash").asText(null)));
        }
        return filesArray;
    }
}
