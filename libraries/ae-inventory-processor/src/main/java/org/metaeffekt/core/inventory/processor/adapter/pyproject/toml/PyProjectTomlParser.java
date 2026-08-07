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
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PyProjectData;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParser;

import java.io.File;
import java.io.IOException;

/**
 * Interface defining required methods for parsing py project toml files.
 */
public interface PyProjectTomlParser {

    /**
     * Checks whether this specific parser supports toml files.
     *
     * @param root the root JSON node from the toml file
     * @return whether this parser supports the given file or not
     */
    boolean supports(JsonNode root);

    /**
     * Detects the build backend to decide if the .toml file is a poetry or pdm file.
     *
     * @param root the root JSON node from the toml file
     * @return the build-backend value as string
     */
    String detectBuildBackend(JsonNode root);

    /**
     * Defines the include pattern for a py project implementation subclass.
     *
     * @return the include pattern for this py project subclass
     */
    String getIncludePattern();

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
     * Creates a lock file parser corresponding to the lock file version-
     *
     * @param lockRoot the root node of the lock file
     * @return the lock file parser for the specific lock file version
     */
    LockFileParser createLockFileParser(JsonNode lockRoot);
}
