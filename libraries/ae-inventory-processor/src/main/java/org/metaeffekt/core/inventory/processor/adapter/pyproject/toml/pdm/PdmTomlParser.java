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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.pdm;

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
 * Class for parsing pdm toml files (<a href="https://pdm-project.org/en/latest/">https://pdm-project.org/en/latest//</a>).
 */
public class PdmTomlParser extends AbstractPep621Parser {
    private static final String PDM_PATH = "/tool/pdm";
    private static final String LEGACY_DEV_DEPENDENCIES_PATH = PDM_PATH + LEGACY_DEV_DEPENDENCIES_PATH_SUFFIX;

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject() && !root.at(PDM_PATH).isMissingNode();
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }

    @Override
    protected ResolvedModule parseProject(JsonNode projectNode) {
        final ResolvedModule module = new ResolvedModule(projectNode.path("name").asText(), null);
        // FIXME-SFA: version can be dynamic, then it can be determined by reading tool.pdm.version
        module.setVersion(projectNode.path("version").asText(null));

        return module;
    }

    /**
     * Toml format for pdm support dev dependencies listed under the PEP 735 defined standard path /dependency-groups but also the
     * legacy path /tool/pdm/dev-dependencies/dev.
     * So the file may have dev dependencies listed under both paths if they coexist in the toml file.
     * Therefore, both these paths have to be evalated and the dev dependencies have to be extracted and merged from these.
     *
     * @param root the file root node
     * @return list of unresolved dev dependencies
     */
    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        final List<UnresolvedModule> pep735DevDependencies = extractPEP735DependencyGroup(root.at(PEP_735_DEPENDENCY_GROUPS_PATH), "dev", new LinkedHashSet<>());
        final List<UnresolvedModule> unresolvedLegacyDevPep508Modules = extractPep508Dependencies(root.at(LEGACY_DEV_DEPENDENCIES_PATH));

        final Map<String, UnresolvedModule> dependencies = new LinkedHashMap<>();
        mergeInto(dependencies, pep735DevDependencies);
        mergeInto(dependencies, unresolvedLegacyDevPep508Modules);

        return List.copyOf(dependencies.values());
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        File lock = new File(pyProjectToml.getParentFile(), "pdm.lock");
        return mapper.readTree(lock);
    }
}
