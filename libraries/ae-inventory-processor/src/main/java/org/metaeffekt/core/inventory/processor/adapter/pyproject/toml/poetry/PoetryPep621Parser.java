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

import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.AbstractPep621Parser;
import tools.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for parsing V2 poetry toml files that support PEP621 (versions from 2.1) (<a href="https://python-poetry.org/docs/">https://python-poetry.org/docs/</a>).
 */
public class PoetryPep621Parser extends AbstractPep621Parser implements PoetryToml {
    private static final String LEGACY_DEV_DEPENDENCIES_PATH = POETRY_PATH + LEGACY_DEV_DEPENDENCIES_PATH_SUFFIX;
    private static final String LEGACY_RUNTIME_DEPENDENCIES_PATH = POETRY_PATH + "/dependencies";

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject() && isPoetryFile(root);
    }

    @Override
    public String getIncludePattern() {
        return getPoetryIncludePattern();
    }

    @Override
    protected ResolvedModule parseProject(JsonNode root) {
        return parsePoetryProject(getProjectNode(root));
    }

    /**
     * Poetry versions from 2.1 by convention support the /project/dependencies path where the main (runtime) dependencies are listed.
     * If this path contains minimum one dependency, the legacy /tool/poetry/dependencies path will only contain enrichment information for the dependencies listed under /project/dependencies.
     * If /project/dependencies does not list any dependencies then the /tool/poetry/dependencies path will be considered as the primary source that contains the runtime dependencies.
     * More information on: <a href="https://python-poetry.org/docs/dependency-specification/">https://python-poetry.org/docs/dependency-specification/</a>.
     *
     * @param root the file root node
     * @return list of unresolved runtime dependencies from /project/dependencies if not empty, else from /tool/poetry/dependencies
     */
    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        final List<UnresolvedModule> pep621RuntimeDependencies = super.extractRuntimeDependencies(root);
        if (pep621RuntimeDependencies.isEmpty()) {
            return extractPropertySpecifiedDependencies(root.at(LEGACY_RUNTIME_DEPENDENCIES_PATH));
        }

        return pep621RuntimeDependencies;
    }

    /**
     * Toml formats for poetry versions from 2.1 support dev dependencies listed under the PEP 735 defined standard path /dependency-groups but also the legacy paths
     * /tool/poetry/group/dev/dependencies as well as under /tool/poetry/dev-dependencies/dev.
     * So the file may have dev dependencies listed under those three paths if they coexist in the toml file.
     * Therefore, all paths have to be evalated and the dev dependencies have to be extracted and merged from these.
     *
     * @param root the file root node
     * @return list of unresolved dev dependencies
     */
    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        final Map<String, List<UnresolvedModule>> pep735DevDependencies = extractAllPEP735DependencyGroups(root.at(PEP_735_DEPENDENCY_GROUPS_PATH));
        final List<UnresolvedModule> poetryGroupDevDependencies = extractPropertySpecifiedDependencies(root.at(DEV_DEPENDENCIES_PATH));
        final List<UnresolvedModule> legacyDevDependencies = extractPropertySpecifiedDependencies(root.at(LEGACY_DEV_DEPENDENCIES_PATH));

        // merge order important for keeping dependency when more than one dependency exists.
        final Map<String, UnresolvedModule> dependencies = new LinkedHashMap<>();
        pep735DevDependencies.values().forEach(groupDeps -> mergeInto(dependencies, groupDeps));
        mergeInto(dependencies, poetryGroupDevDependencies);
        mergeInto(dependencies, legacyDevDependencies);

        return List.copyOf(dependencies.values());
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        return mapper.readTree(getLockFile(pyProjectToml));
    }

    private boolean isPoetryFile(JsonNode root) {
        String backend = detectBuildBackend(root);
        if (backend != null) {
            return backend.startsWith("poetry");
        }
        // Fallback, if no build-system field is defined in toml
        return root.at(POETRY_PATH).isObject();
    }
}
