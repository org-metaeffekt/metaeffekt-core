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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.util.ArrayList;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.parseRequirement;

/**
 * Abstract class for parsing pdm toml files.
 */
public abstract class AbstractPep621Parser extends AbstractTomlParser {
    private static final String PROJECT_PATH = "/project";
    private static final String RUNTIME_DEPENDENCIES_PATH = "/project/dependencies";

    @Override
    protected JsonNode getProjectNode(JsonNode root) {
        return root.at(PROJECT_PATH);
    }

    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        return extractPep508DirectDependencies(root.at(RUNTIME_DEPENDENCIES_PATH));
    }

    /**
     * Extracts PEP508 dependency strings.
     *
     * @param dependenciesNode the dependencies node (array)
     * @return list of unresolved dependencies
     */
    protected static List<UnresolvedModule> extractPep508DirectDependencies(JsonNode dependenciesNode) {
        List<UnresolvedModule> unresolvedModules = new ArrayList<>();
        if (!dependenciesNode.isArray()) {
            return unresolvedModules;
        }
        dependenciesNode.valueStream().forEach(dependency -> unresolvedModules.add(parseRequirement(dependency.asText())));
        return unresolvedModules;
    }
}
