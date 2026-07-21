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

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PyProjectParser {
    /**
     * Checks whether this specific parser supports toml files.
     */
    boolean supports(JsonNode root);

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
     * Extracts the direct dependencies in a toml file
     *
     * @param projectNode       the root project node
     * @param fullQualifiedPath the path to a node in the root project node
     * @return list of unresolved dependencies (modules)
     */
    List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath);

    String getIncludePattern();
}
