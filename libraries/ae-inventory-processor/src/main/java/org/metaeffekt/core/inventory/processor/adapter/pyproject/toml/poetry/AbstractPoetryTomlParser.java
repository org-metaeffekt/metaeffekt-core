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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.poetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.AbstractTomlParser;

import java.util.*;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.extractVersion;

/**
 * Abstract class for parsing poetry toml files.
 */
public abstract class AbstractPoetryTomlParser extends AbstractTomlParser {
    protected static final String POETRY_PATH = "/tool/poetry";

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    /**
     * Returns default oetry node.
     */
    protected JsonNode getPoetryNode(JsonNode root) {
        return root.at(POETRY_PATH);
    }

    protected List<UnresolvedModule> extractObjectDirectDependencies(JsonNode dependencyNode) {
        List<UnresolvedModule> unresolvedModules = new ArrayList<>();
        if (dependencyNode.isMissingNode()) {
            return unresolvedModules;
        }
        dependencyNode.propertyStream().forEach(entry -> unresolvedModules.add(new UnresolvedModule(entry.getKey(), null, extractVersion(entry.getValue()))));
        return unresolvedModules;
    }

    /**
     * Checks if Poetry 2 PEP621 structure exists.
     *
     * @param root the file root node
     * @return true if file is in poetry 2 PEP621 structure, else false
     */
    protected boolean hasPep621Project(JsonNode root) {
        return root.path("project").isObject();
    }
}
