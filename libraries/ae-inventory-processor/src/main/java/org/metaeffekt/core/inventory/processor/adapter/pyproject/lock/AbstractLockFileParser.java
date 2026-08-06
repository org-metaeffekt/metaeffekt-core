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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

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
     * Extracts all packages from poetry.lock as resolved modules.
     *
     * @param lockRoot the lock file root node
     * @return list of resolved packages
     */
    protected List<ResolvedModule> extractPackages(JsonNode lockRoot) {
        final boolean filesAreCentralized = lockRoot.path("metadata").path("lock-version").asText().startsWith("1.");
        final List<ResolvedModule> resolvedModules = new ArrayList<>();
        final JsonNode packages = lockRoot.path("package");
        if (!packages.isArray()) {
            return resolvedModules;
        }
        packages.forEach(packageNode -> {
                    final ResolvedModule module = extractPackage(packageNode, filesAreCentralized, lockRoot);
                    resolvedModules.add(module);
                }
        );
        return resolvedModules;
    }

    /**
     * Extracts the dependencies from the package.
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
    private ResolvedModule extractPackage(JsonNode packageNode, boolean filesAreCentralized, JsonNode lockRoot) {
        final ResolvedModule module = new ResolvedModule(packageNode.path("name").asText(), null);
        module.setVersion(packageNode.path("version").asText(null));
        final Map<String, UnresolvedModule> unresolvedModuleMap = extractDependencies(packageNode.path("dependencies"));
        module.setRuntimeDependencies(unresolvedModuleMap);

        module.setPyProjectPackageSource(parseSource(packageNode));
        module.setPyProjectPackageFiles(collectFiles(packageNode, filesAreCentralized, lockRoot));

        return module;
    }

    /**
     * Collects the file elements from the files array in the [[package]] array, or if the lock version is 1.x from metadata.files.
     *
     * @param packageNode         the package node
     * @param filesAreCentralized whether the files are centrally listed under metadata.files or not
     * @param lockRoot            the lock file root node used for extracting the centralized files
     * @return JSON array containing the files and the hashes for a package
     */
    private JSONArray collectFiles(JsonNode packageNode, boolean filesAreCentralized, JsonNode lockRoot) {
        if (filesAreCentralized) {
            final Map<String, JSONArray> packageNamesToFilesMap = parseMetadataFiles(lockRoot);
            return packageNamesToFilesMap.get(packageNode.path("name").asText());
        }
        return extractFilesAndAddToArray(packageNode.path("files"));
    }

    /**
     * In lock versions 1.x the files are listed under metadata.files, as opposed to version 2.0 and above where they are listed for each package under [[package]].files.
     *
     * @param lockRoot the lock file root node
     * @return a map containing the package name as the key and the files as its values (as JSONArray)
     */
    private Map<String, JSONArray> parseMetadataFiles(JsonNode lockRoot) {
        JsonNode metadataFiles = lockRoot.at("/metadata/files");
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
        if (!files.isArray()) {
            return null;
        }
        final JSONArray filesArray = new JSONArray();
        files.forEach(file -> {
                    final JSONObject object = new JSONObject();
                    object.put("file", file.path("file").asText(null));
                    object.put("hash", file.path("hash").asText(null));
                    filesArray.put(object);
                }
        );
        return filesArray;
    }
}
