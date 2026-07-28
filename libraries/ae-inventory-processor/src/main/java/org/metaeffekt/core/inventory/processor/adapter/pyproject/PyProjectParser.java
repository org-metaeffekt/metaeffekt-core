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
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface defining required methods for parsing py project toml and lock files.
 */
public interface PyProjectParser {

    /**
     * Checks whether this specific parser supports toml files.
     */
    boolean supports(JsonNode root);

    /**
     * Defines the include pattern for a py project implementation subclass.
     *
     * @return the include pattern for this py project subclass
     */
    String getIncludePattern();

    /**
     * Determines the lock file from the py project toml file.
     *
     * @param pyProjectToml the py project toml file
     * @return the corresponding lock file
     */
    File getLockFile(File pyProjectToml);

    /**
     * Parses a project as a module.
     *
     * @param root the root
     * @return the resolved project module
     */
    ResolvedModule parseProject(JsonNode root);

    /**
     * Method for resolving modules from lock files.
     *
     * @param lockNode the lock file node
     * @return list of resolved modules
     */
    List<ResolvedModule> parseLockFile(JsonNode lockNode);

    /**
     * Parses the toml and lock files extracting the dependencies.
     *
     * @param pyProjectToml the toml file
     * @param root          the root JSON node from the toml file
     * @return a {@link PyProjectData} object containing the parsed data
     * @throws IOException if an I/O error occurs
     */
    PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException;

    /**
     * Extracts the direct dependencies in a toml file.
     *
     * @param projectNode       the root project node
     * @param fullQualifiedPath the path to a node in the root project node
     * @return list of unresolved dependencies (modules)
     */
    List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath);

    /**
     * Parses the dependencies from packages in the lock file and creates {@link UnresolvedModule} objects from them.
     *
     * @param packageDependenciesNode the dependencies node in the package
     * @param unresolvedModuleMap     a map consisting of the name of the dependency and its unresolved module representation
     */
    void extractAndFillUnresolvedModules(JsonNode packageDependenciesNode, Map<String, UnresolvedModule> unresolvedModuleMap);

    /**
     * Parses information of the package source attribute of the lock file.
     *
     * @param packageNode the package node
     * @return the extracted source data
     */
    PyProjectPackageSource parseSource(JsonNode packageNode);
}
