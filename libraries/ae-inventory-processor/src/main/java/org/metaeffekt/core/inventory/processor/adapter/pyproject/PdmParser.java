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

/**
 * Class for parsing pdm toml and pdm.lock files.
 */
public class PdmParser implements PyProjectParser {

    @Override
    public boolean supports(JsonNode root) {
        return root.at("/project").isObject();
    }

    @Override
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        return null;
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath) {
        return List.of();
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }
}
