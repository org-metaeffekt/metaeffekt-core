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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Interface defining required methods for parsing py project lock files.
 */
public interface LockFileParser {
    /**
     * Checks whether this parser supports the lock file format.
     *
     * @param lockRoot the lock file root node
     * @return true if the parser supports the version, else false
     */
    boolean supports(JsonNode lockRoot);

    /**
     * Parses the lock file packages.
     *
     * @param lockRoot the lock file root node
     * @return list of resolved package modules
     */
    List<ResolvedModule> parse(JsonNode lockRoot);
}
