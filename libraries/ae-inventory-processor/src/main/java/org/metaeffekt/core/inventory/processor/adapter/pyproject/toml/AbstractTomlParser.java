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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PyProjectData;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParserFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.toml.TomlMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract class defining and providing shared methods for poetry and pdm file parsing.
 */
@Getter
@Slf4j
@AllArgsConstructor
public abstract class AbstractTomlParser implements PyProjectTomlParser {
    protected static final String LEGACY_DEV_DEPENDENCIES_PATH_SUFFIX = "/dev-dependencies/dev";
    private static final String BUILD_BACKEND = "/build-system/build-backend";

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
        final PyProjectData data = new PyProjectData();
        data.setProjectModule(parseProject(root));
        data.setDirectRuntimeDependencies(extractRuntimeDependencies(root));
        data.setDirectDevelopmentDependencies(extractDevelopmentDependencies(root));

        final JsonNode lockNode = readLockFile(pyProjectToml);
        final LockFileParser lockParser = createLockFileParser(lockNode);
        data.setResolvedModulesFromLockFile(lockParser.parse(lockNode));
        return data;
    }

    @Override
    public LockFileParser createLockFileParser(JsonNode lockRoot) {
        return lockParserFactory.getParser(lockRoot);
    }

    @Override
    public String detectBuildBackend(JsonNode root) {
        return root.at(BUILD_BACKEND).asText(null);
    }

    /**
     * Merges all dependencies from a source into a target map while checking for version conflicts and deduplicating same dependencies.
     *
     * @param target the target map
     * @param source the source list of unresolved dependencies
     */
    protected static void mergeInto(Map<String, UnresolvedModule> target, List<UnresolvedModule> source) {
        if (source.isEmpty()) {
            return;
        }
        for (final UnresolvedModule module : source) {
            target.merge(module.getName(), module, AbstractTomlParser::checkNoConflict);
        }
    }

    /**
     * BiFunction for Map.merge: identical entries will be deduplicated,
     * conflicting entries: the existing dependency entry (unresolved module) will be kept.
     *
     * @param existing the existing unresolved module in the map
     * @param incoming the incoming unresolved module in the map
     * @return the unresolved module to keep
     */
    protected static UnresolvedModule checkNoConflict(UnresolvedModule existing, UnresolvedModule incoming) {
        if (Objects.equals(existing.getVersionRange(), incoming.getVersionRange())) {
            return existing;
        }

        log.warn("Development dependency '{}' is defined multiple times with different version ranges ('{}' and '{}'). Keeping the first definition.",
                incoming.getName(),
                existing.getVersionRange(),
                incoming.getVersionRange());
        return existing;
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
