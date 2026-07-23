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
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.util.*;

/**
 * Class for parsing poetry toml and poetry.lock files.
 */
public class PoetryParser extends PyProjectParser {
    public PoetryParser() {
        super("/tool/poetry", "/tool/poetry/dependencies", "/tool/poetry/group/dev/dependencies", "poetry.lock");
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.at("/tool/poetry").isObject();
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode rootNode, String fullQualifiedPath) {
        final List<UnresolvedModule> modules = new ArrayList<>();
        final JsonNode dependencyNode = rootNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode()) {
            dependencyNode.propertyStream().forEach(entry -> {
                String versionRange = deriveVersionRange(entry.getValue());
                UnresolvedModule unresolvedModule = new UnresolvedModule(entry.getKey(), null, versionRange);
                modules.add(unresolvedModule);
            });
        }
        return modules;
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    @Override
    public List<ResolvedModule> getResolvedModulesFromLockFile(JsonNode lockNode) {
        final List<ResolvedModule> resolvedModules = new ArrayList<>();
        lockNode.path("package").valueStream().forEach(packageNode -> {
            final ResolvedModule resolvedModule = new ResolvedModule(packageNode.get("name").textValue(), null);
            resolvedModule.setVersion(packageNode.get("version").textValue());

            resolvedModule.setPyProjectPackageSource(parseSource(packageNode, "source"));
            resolvedModule.setPyProjectPackageFiles(collectPackageFileData(packageNode));

            final JsonNode packageDependenciesNode = packageNode.path("dependencies");
            final Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();
            if (!packageDependenciesNode.isMissingNode()) {
                packageDependenciesNode.propertyStream().forEach(dependency -> {
                    final UnresolvedModule unresolvedModule = new UnresolvedModule(dependency.getKey(), null, dependency.getValue().toString());
                    unresolvedModuleMap.put(dependency.getKey(), unresolvedModule);
                });
            }
            resolvedModule.setRuntimeDependencies(unresolvedModuleMap);
            resolvedModules.add(resolvedModule);
        });
        return resolvedModules;
    }

    private String deriveVersionRange(JsonNode value) {
        return value.isTextual() ? value.textValue() : value.get("version").textValue();
    }
}
