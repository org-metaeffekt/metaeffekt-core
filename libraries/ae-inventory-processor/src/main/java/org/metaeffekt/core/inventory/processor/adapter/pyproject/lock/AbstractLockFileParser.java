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
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for parsing poetry and pdm lock files.
 */
public abstract class AbstractLockFileParser implements LockFileParser {

    /**
     * Extracts all packages from poetry.lock as resolved modules.
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
                    final ResolvedModule module = extractPackage(packageNode);
                    resolvedModules.add(module);
                }
        );
        return resolvedModules;
    }

    /**
     * Extracts package information for one package as a resolved module.
     *
     * @param packageNode the package node
     * @return the resolved package
     */
    protected ResolvedModule extractPackage(JsonNode packageNode) {
        final ResolvedModule module = new ResolvedModule(packageNode.path("name").asText(), null);
        module.setVersion(packageNode.path("version").asText(null));
        final Map<String, UnresolvedModule> unresolvedModuleMap = extractDependencies(packageNode.path("dependencies"));
        module.setRuntimeDependencies(unresolvedModuleMap);

        module.setPyProjectPackageSource(parseSource(packageNode));
        module.setPyProjectPackageFiles(collectFiles(packageNode));

        return module;
    }

    /**
     * Collects the file elements from the files array in the package.
     *
     * @param packageNode the package node
     * @return JSON array containing the files and the hashes
     */
    protected JSONArray collectFiles(JsonNode packageNode) {
        final JsonNode files = packageNode.path("files");
        if (!files.isArray()) {
            return null;
        }
        final JSONArray fileData = new JSONArray();
        files.forEach(file -> {
                    final JSONObject object = new JSONObject();
                    object.put("file", file.path("file").asText(null));
                    object.put("hash", file.path("hash").asText(null));
                    fileData.put(object);
                }
        );

        return fileData;
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
}
