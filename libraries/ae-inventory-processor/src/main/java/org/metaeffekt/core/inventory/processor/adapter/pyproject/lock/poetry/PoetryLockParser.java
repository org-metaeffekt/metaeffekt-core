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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.poetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;

import java.util.List;

/**
 * Parser for poetry lock file formats 1.x and 2.x (same format convention).
 */
public class PoetryLockParser extends AbstractPoetryLockParser {
    @Override
    public boolean supports(JsonNode root) {
        final String version = root.path("metadata").path("lock-version").asText();
        return version.startsWith("1.") || version.startsWith("2.");
    }

    @Override
    public List<ResolvedModule> parse(JsonNode root) {
        return extractPackages(root);
    }
}
