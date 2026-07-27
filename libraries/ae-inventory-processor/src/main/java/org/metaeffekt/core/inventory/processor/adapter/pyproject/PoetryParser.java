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
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

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
    public String getIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    @Override
    protected List<UnresolvedModule> extractDirectDependencies(JsonNode rootNode, String fullQualifiedPath) {
        final List<UnresolvedModule> unresolvedModules = new ArrayList<>();
        final JsonNode dependencyNode = rootNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode()) {
            dependencyNode.propertyStream().forEach(entry -> {
                final String versionRange = deriveVersionRange(entry.getValue());
                final UnresolvedModule unresolvedModule = new UnresolvedModule(entry.getKey(), null, versionRange);
                unresolvedModules.add(unresolvedModule);
            });
        }
        return unresolvedModules;
    }

    @Override
    protected void extractAndFillUnresolvedModules(JsonNode packageDependenciesNode, Map<String, UnresolvedModule> unresolvedModuleMap) {
        if (!packageDependenciesNode.isMissingNode()) {
            packageDependenciesNode.propertyStream().forEach(dependency -> {
                final String versionRange = deriveVersionRange(dependency.getValue());
                final UnresolvedModule unresolvedModule = new UnresolvedModule(dependency.getKey(), null, versionRange);
                unresolvedModuleMap.put(dependency.getKey(), unresolvedModule);
            });
        }
    }

    @Override
    protected PyProjectPackageSource parseSource(JsonNode packageNode) {
        final JsonNode source = packageNode.path("source");
        if (source.isMissingNode()) {
            return null;
        }
        final String type = source.path("type").asText(null);
        final List<String> urls = new ArrayList<>();
        String url = source.path("url").asText(null);
        addIfNotNull(urls, url);
        final String reference = source.path("reference").asText(null);

        return new PyProjectPackageSource(type, urls, reference);
    }

    /**
     * The dependency value containing the version information can either be a JSON object, a JSON array or a string.
     *
     * @param dependencyValue the value of the dependency containing the version
     * @return the version
     */
    private String deriveVersionRange(JsonNode dependencyValue) {
        if (dependencyValue.isObject()) {
            return dependencyValue.get("version").textValue();
        } else if (dependencyValue.isArray()) {
            dependencyValue.get(1).get("version").textValue();
        }
        return dependencyValue.textValue();
    }
}
