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
import java.util.List;
import java.util.Map;

/**
 * Class for parsing pdm toml files (<a href="https://pdm-project.org/en/latest/">https://pdm-project.org/en/latest//</a>).
 */
public class PdmTomlParser extends AbstractPep621Parser {
    private static final String PDM_PATH = "/tool/pdm";
    private static final String LEGACY_DEV_DEPENDENCIES_PATH = PDM_PATH + LEGACY_DEV_DEPENDENCIES_PATH_SUFFIX;

    /**
     * Record for storing dynamic version data.
     *
     * @param source   the source to determine the dynamic version ("file" | "scm" | "call")
     * @param getter   the getter function to determine the dynamic version if source="call": "modul:function"
     * @param fromFile the path to the file for determining the dynamic version if source="file"
     * @param pattern  optional regex pattern for "file"/"scm"
     */
    private record DynamicVersionInfo(String source, String getter, String fromFile, String pattern) {}

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject() && !root.at(PDM_PATH).isMissingNode();
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }

    @Override
    protected ResolvedModule parseProject(JsonNode root) {
        final ResolvedModule module = new ResolvedModule(getProjectNode(root).path("name").asText(), null);
        final String version = extractPdmProjectVersion(root);
        module.setVersion(version);

        return module;
    }

    /**
     * Extracts the version of the project for pdm files.
     * If no value for version under /project/version is defined, it will be checked, whether the version is dynamic.
     * If so the source of that dynamically determined version will be stored, otherwise null.
     *
     * @param root the root file node
     * @return the extracted version or its source
     */
    private String extractPdmProjectVersion(JsonNode root) {
        final JsonNode projectNode = getProjectNode(root);
        final JsonNode versionNode = projectNode.path("/version");
        if (!versionNode.isMissingNode()) {
            return versionNode.asText();
        }

        final boolean isDynamicVersion = isFieldListedAsDynamic(projectNode, "version");
        if (!isDynamicVersion) {
            return null; // no version and also not declared dynamic
        }

        // version is dynamic -> no value extractable -> extract source
        return extractDynamicVersionSource(root);
    }

    private boolean isFieldListedAsDynamic(JsonNode projectNode, String fieldName) {
        final JsonNode dynamic = projectNode.at("/dynamic");
        if (!dynamic.isArray()) return false;
        return dynamic.valueStream().anyMatch(n -> fieldName.equals(n.asText()));
    }

    private String extractDynamicVersionSource(JsonNode root) {
        final JsonNode versionConfig = root.at(PDM_PATH + "/version");
        if (versionConfig.isMissingNode()) {
            return null;
        }

        final DynamicVersionInfo dynamicVersionInfo = new DynamicVersionInfo(
                versionConfig.path("source").asText(null),
                versionConfig.path("getter").asText(null),
                versionConfig.path("path").asText(null),
                versionConfig.path("pattern").asText(null)
        );

        return "dynamic:" + dynamicVersionInfo.source;
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
        final Map<String, List<UnresolvedModule>> pep735DevDependencies = extractAllPEP735DependencyGroups(root.at(PEP_735_DEPENDENCY_GROUPS_PATH));
        final List<UnresolvedModule> unresolvedLegacyDevPep508Modules = extractPep508Dependencies(root.at(LEGACY_DEV_DEPENDENCIES_PATH));

        final Map<String, UnresolvedModule> dependencies = new LinkedHashMap<>();
        pep735DevDependencies.values().forEach(groupDeps -> mergeInto(dependencies, groupDeps));
        mergeInto(dependencies, unresolvedLegacyDevPep508Modules);

        return List.copyOf(dependencies.values());
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        File lock = new File(pyProjectToml.getParentFile(), "pdm.lock");
        return mapper.readTree(lock);
    }
}
