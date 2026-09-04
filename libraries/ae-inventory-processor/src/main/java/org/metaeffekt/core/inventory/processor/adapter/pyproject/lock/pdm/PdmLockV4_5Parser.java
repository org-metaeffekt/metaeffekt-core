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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.pdm;

import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Parser for pdm lock file format 4.x.
 */
public class PdmLockV4_5Parser extends AbstractPdmLockParser {

    @Override
    public boolean supports(JsonNode root) {
        return root.path("metadata").path("lock_version").asText().startsWith("4.");
    }

    @Override
    public List<ResolvedModule> parse(JsonNode root) {
        return extractPackages(root);
    }
}
