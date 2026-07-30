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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PyProjectData;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParserFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Abstract class defining and providing shared methods for poetry and pdm file parsing.
 */
@Getter
@AllArgsConstructor
public abstract class AbstractTomlParser implements PyProjectTomlParser {
    /**
     * Lock file parser factory used for determining lock file parser.
     */
    protected final LockFileParserFactory lockParserFactory = new LockFileParserFactory();

    /**
     * Mapper for reading toml files into objects.
     */
    protected final ObjectMapper mapper = new TomlMapper();

    @Override
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        PyProjectData data = new PyProjectData();
        JsonNode projectNode = getProjectNode(root);
        data.setProjectModule(parseProject(projectNode));
        data.setDirectRuntimeDependencies(extractRuntimeDependencies(root));
        data.setDirectDevelopmentDependencies(extractDevelopmentDependencies(root));

        JsonNode lockNode = readLockFile(pyProjectToml);
        LockFileParser lockParser = createLockFileParser(lockNode);
        data.setResolvedModulesFromLockFile(lockParser.parse(lockNode));
        return data;
    }

    @Override
    public LockFileParser createLockFileParser(JsonNode lockRoot) {
        return lockParserFactory.getParser(lockRoot);
    }

    /**
     * Returns the poetry or pdm project node.
     *
     * @param root the file root node
     * @return the project node
     */
    protected abstract JsonNode getProjectNode(JsonNode root);

    /**
     * Parses a poetry or pdm project as a module.
     *
     * @param projectNode the project root
     * @return the resolved project module
     */
    protected abstract ResolvedModule parseProject(JsonNode projectNode);

    /**
     * Extracts the runtime dependencies.
     *
     * @param root the file root node
     * @return list of unresolved runtime dependencies
     */
    protected abstract List<UnresolvedModule> extractRuntimeDependencies(JsonNode root);

    /**
     * Extracts the dev dependencies.
     *
     * @param root the file root node
     * @return list of unresolved dev dependencies
     */
    protected abstract List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root);

    /**
     * Returns the lock file root as a JSON node.
     *
     * @param pyProjectToml the pyproject toml file
     * @return the JSON node of the lock file
     * @throws IOException if an I/O error occurs
     */
    protected abstract JsonNode readLockFile(File pyProjectToml) throws IOException;
}
