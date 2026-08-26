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
import tools.jackson.databind.JsonNode;

import java.io.File;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.PyProjectUtils.extractVersion;

/**
 * Interface for collecting poetry specific attributes and methods.
 */
public interface PoetryToml {
    /**
     * The poetry path definition.
     */
    String POETRY_PATH = "/tool/poetry";
    String DEV_DEPENDENCIES_PATH = POETRY_PATH + "/group/dev/dependencies";

    /**
     * Returns the standard include pattern for poetry files.
     *
     * @return the standard include pattern for poetry files
     */
    default String getPoetryIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    /**
     * Returns the standard poetry lock file.
     *
     * @param pyProjectToml the poetry toml file
     * @return the standard poetry lock file
     */
    default File getLockFile(File pyProjectToml) {
        return new File(pyProjectToml.getParentFile(), "poetry.lock");
    }

    /**
     * Parses the poetry metadata from the toml file.
     *
     * @param projectNode the project node
     * @return the poetry metadata from the toml file
     */
    default ResolvedModule parsePoetryProject(JsonNode projectNode) {
        final ResolvedModule module = new ResolvedModule(projectNode.path("name").asText(), null);
        module.setVersion(projectNode.path("version").asText(null));
        return module;
    }

    /**
     * Extracts dev dependencies for poetry, specified as properties (dependency="^2.0").
     *
     * @param dependencyNode the dependencies node
     * @return list of unresolved dependencies
     */
    default List<UnresolvedModule> extractPropertySpecifiedDependencies(JsonNode dependencyNode) {
        if (dependencyNode.isMissingNode() || !dependencyNode.isObject()) {
            return List.of();
        }

        return dependencyNode.propertyStream()
                .map(entry -> new UnresolvedModule(entry.getKey(), null, extractVersion(entry.getValue()))).toList();
    }
}
