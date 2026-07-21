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
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Getter
@AllArgsConstructor
public abstract class PyProjectParser {
    protected String projectNode;
    protected String dependencyPath;
    protected String devDependencyPath;
    protected String lockFileName;

    /**
     * Checks whether this specific parser supports toml files.
     */
    public abstract boolean supports(JsonNode root);

    /**
     * Defines the include pattern for a py project implementation subclass.
     *
     * @return the include pattern for this py project subclass
     */
    public abstract String getIncludePattern();

    /**
     * Parses the toml and lock files extracting the dependencies.
     *
     * @param pyProjectToml the toml file
     * @param root          the root JSON node from the toml file
     * @return a {@link PyProjectData} object containing the parsed data
     * @throws IOException if an I/O error occurs
     */
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        PyProjectData pyProjectData = new PyProjectData();

        // parse toml file
        JsonNode projectNode = root.at(getProjectNode());
        ResolvedModule projectModule = new ResolvedModule(projectNode.get("name").asText(), null);
        projectModule.setVersion(projectNode.get("version").asText());

        pyProjectData.setProjectModule(projectModule);
        pyProjectData.setDirectRuntimeDependencies(extractDirectDependencies(projectNode, getDependencyPath()));
        pyProjectData.setDirectDevelopmentDependencies(extractDirectDependencies(projectNode, getDevDependencyPath()));

        // parse lock file
        final File lockFile = new File(pyProjectToml.getParentFile(), getLockFileName());
        final ObjectMapper lockObjectMapper = new TomlMapper();
        final JsonNode lockNode = lockObjectMapper.readTree(lockFile);

        pyProjectData.setResolvedModulesFromLockFile(getResolvedModulesFromLockFile(lockNode));
        return pyProjectData;
    }

    /**
     * Method for resolving modules from lock files.
     *
     * @param lockNode the lock file node
     * @return list of resolved modules
     */
    protected abstract List<ResolvedModule> getResolvedModulesFromLockFile(JsonNode lockNode);

    /**
     * Extracts the direct dependencies in a toml file
     *
     * @param projectNode       the root project node
     * @param fullQualifiedPath the path to a node in the root project node
     * @return list of unresolved dependencies (modules)
     */
    protected abstract List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath);

}
