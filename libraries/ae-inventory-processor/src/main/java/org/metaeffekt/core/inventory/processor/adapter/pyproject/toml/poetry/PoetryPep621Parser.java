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
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.AbstractPep621Parser;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Class for parsing V2 poetry toml files that support PEP621 (versions >=2.1) (<a href="https://python-poetry.org/docs/">https://python-poetry.org/docs/</a>).
 */
public class PoetryPep621Parser extends AbstractPep621Parser implements PoetryToml {
    private static final String LEGACY_DEV_DEPENDENCIES_PATH = POETRY_PATH + LEGACY_DEV_DEPENDENCIES_PATH_SUFFIX;

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject() && !root.at(POETRY_PATH).isMissingNode();
    }

    @Override
    public String getIncludePattern() {
        return getPoetryIncludePattern();
    }

    @Override
    protected ResolvedModule parseProject(JsonNode projectNode) {
        return parsePoetryProject(projectNode);
    }

    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        final List<UnresolvedModule> pep735DevDependencies = extractPEP735DependencyGroup(root.at(PEP_735_DEPENDENCY_GROUPS_PATH), "dev", new LinkedHashSet<>());
        final List<UnresolvedModule> poetryGroupDevDependencies = extractPropertySpecifiedDependencies(root.at(DEV_DEPENDENCIES_PATH));
        final List<UnresolvedModule> legacyDevDependencies = extractPropertySpecifiedDependencies(root.at(LEGACY_DEV_DEPENDENCIES_PATH));

        // merge order important for keeping dependency when more than one dependency exists.
        final Map<String, UnresolvedModule> dependencies = new LinkedHashMap<>();
        mergeInto(dependencies, pep735DevDependencies);
        mergeInto(dependencies, poetryGroupDevDependencies);
        mergeInto(dependencies, legacyDevDependencies);

        return List.copyOf(dependencies.values());
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        return mapper.readTree(getLockFile(pyProjectToml));
    }
}
