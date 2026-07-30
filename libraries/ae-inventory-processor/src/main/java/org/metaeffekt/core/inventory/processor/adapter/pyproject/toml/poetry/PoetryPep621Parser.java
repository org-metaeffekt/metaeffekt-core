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
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.AbstractPep621Parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class for parsing V2 poetry toml files that support PEP621.
 */
public class PoetryPep621Parser extends AbstractPep621Parser implements PoetryToml {
    private static final String DEV_DEPENDENCIES_PATH = "/project/dependency-groups/dev";

    @Override
    public boolean supports(JsonNode root) {
        return getProjectNode(root).isObject() && root.at(getPoetryPath()).isObject();
    }

    @Override
    public String getIncludePattern() {
        return getPoetryIncludePattern();
    }

    @Override
    protected ResolvedModule parseProject(JsonNode projectNode) {
        return parsePoetryProject(projectNode);
    }

    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        return extractPep508DirectDependencies(root.at(DEV_DEPENDENCIES_PATH));
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        return mapper.readTree(getLockFile(pyProjectToml));
    }
}
