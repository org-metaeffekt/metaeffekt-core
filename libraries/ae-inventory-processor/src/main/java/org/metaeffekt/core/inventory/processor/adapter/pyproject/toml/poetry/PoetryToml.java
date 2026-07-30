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

import java.io.File;

/**
 * Interface for collecting poetry specific attributes and methods.
 */
public interface PoetryToml {
    /**
     * Returns the standard include pattern for poetry files.
     *
     * @return the standard include pattern for poetry files
     */
    default String getPoetryIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    /**
     * Returns the standard poetry path in a poetry toml file.
     *
     * @return the standard poetry path in a poetry toml file
     */
    default String getPoetryPath() {
        return "/tool/poetry";
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
}
