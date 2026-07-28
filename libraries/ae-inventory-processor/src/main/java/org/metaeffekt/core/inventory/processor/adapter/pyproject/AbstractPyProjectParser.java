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
package org.metaeffekt.core.inventory.processor.adapter.pyproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Abstract class defining shared methods for poetry and pdm file parsing.
 */
@Getter
@AllArgsConstructor
public abstract class AbstractPyProjectParser implements PyProjectParser {
    /**
     * Mapper for reading toml files into objects.
     */
    protected final ObjectMapper mapper = new TomlMapper();

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject();
    }

    @Override
    public ResolvedModule parseProject(JsonNode root) {
        JsonNode project = getProjectNode(root);
        ResolvedModule module = new ResolvedModule(project.get("name").asText(), null);
        module.setVersion(project.path("version").asText(null));

        return module;
    }

    @Override
    public List<ResolvedModule> parseLockFile(JsonNode lockNode) {
        final List<ResolvedModule> resolvedModules = new ArrayList<>();

        lockNode.path("package").valueStream().forEach(packageNode -> {
            final ResolvedModule resolvedModule = new ResolvedModule(packageNode.get("name").textValue(), null);
            final JsonNode packageDependenciesNode = packageNode.path("dependencies");
            final Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();

            extractAndFillUnresolvedModules(packageDependenciesNode, unresolvedModuleMap);

            resolvedModule.setVersion(packageNode.get("version").textValue());
            resolvedModule.setPyProjectPackageFiles(collectPackageFileData(packageNode));
            resolvedModule.setPyProjectPackageSource(parseSource(packageNode));
            resolvedModule.setRuntimeDependencies(unresolvedModuleMap);
            resolvedModules.add(resolvedModule);
        });
        return resolvedModules;
    }

    @Override
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        PyProjectData data = new PyProjectData();
        data.setProjectModule(parseProject(root));
        data.setDirectRuntimeDependencies(extractRuntimeDependencies(root));
        data.setDirectDevelopmentDependencies(extractDevelopmentDependencies(root));

        JsonNode lockNode = readLockFile(pyProjectToml);
        data.setResolvedModulesFromLockFile(parseLockFile(lockNode));
        return data;
    }

    /**
     * Abstract method for getting the project JSON node.
     *
     * @param root the root file node
     * @return the project node
     */
    protected abstract JsonNode getProjectNode(JsonNode root);

    /**
     * Abstract method for extracting runtime dependencies from toml file as unresolved modules.
     *
     * @param root the root file node
     * @return list of unresolved runtime dependencies
     */
    protected abstract List<UnresolvedModule> extractRuntimeDependencies(JsonNode root);

    /**
     * Abstract method for extracting development dependencies from toml file as unresolved modules.
     *
     * @param root the root file node
     * @return list of unresolved development dependencies
     */
    protected abstract List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root);

    /**
     * Return the lock file root as a JSON node.
     *
     * @param pyProjectToml the pyproject toml file
     * @return the JSON node of the lock file
     * @throws IOException if an I/O error occurs
     */
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        return mapper.readTree(getLockFile(pyProjectToml));
    }

    /**
     * Collects the file elements from the files attribute from the package in the lock file.
     *
     * @param packageNode the package node
     * @return JSON array containing the files and the hashes
     */
    protected JSONArray collectPackageFileData(JsonNode packageNode) {
        final JsonNode files = packageNode.path("files");
        if (files.isMissingNode() || !files.isArray()) {
            return null;
        }

        final JSONArray fileData = new JSONArray();
        for (JsonNode file : files) {
            if (file.isObject()) {
                final JSONObject fileDataObject = new JSONObject();
                fileDataObject.put("file", file.path("file").asText(null));
                fileDataObject.put("hash", file.path("hash").asText(null));
                fileData.put(fileDataObject);
            }
        }
        return fileData.isEmpty() ? null : fileData;
    }

    /**
     * Adds an element to a collection if it is not null.
     *
     * @param collection the collection or any subclass of the collection
     * @param value      the value
     * @param <T>        the type of the value
     */
    protected <T> void addIfNotNull(Collection<T> collection, T value) {
        if (value != null) {
            collection.add(value);
        }
    }
}
