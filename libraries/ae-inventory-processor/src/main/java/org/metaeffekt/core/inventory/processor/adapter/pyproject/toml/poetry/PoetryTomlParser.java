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
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParserFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for parsing poetry toml files.
 */
public class PoetryTomlParser extends AbstractPoetryTomlParser {
    private final LockFileParserFactory lockParserFactory;
    private static final String POETRY_1_RUNTIME_DEPENDENCIES_PATH = "/tool/poetry/dependencies";
    private static final String POETRY_2_RUNTIME_DEPENDENCIES_PATH = "/project/dependencies";
    private static final String DEV_DEPENDENCIES_PATH = "/tool/poetry/group/dev/dependencies";

    public PoetryTomlParser(LockFileParserFactory lockParserFactory) {
        this.lockParserFactory = lockParserFactory;
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.at("/tool/poetry").isObject();
    }

    @Override
    protected ResolvedModule parseProject(JsonNode root) {
        final JsonNode projectNode;

        // Poetry 2
        if (hasPep621Project(root)) {
            projectNode = root.path("project");
        }
        // Poetry 1
        else {
            projectNode = getPoetryNode(root);
        }

        ResolvedModule module = new ResolvedModule(projectNode.path("name").asText(), null);
        module.setVersion(projectNode.path("version").asText(null));

        return module;
    }

    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        // Poetry 2
        if (hasPep621Project(root)) {
            return extractPep621Dependencies(root.at(POETRY_2_RUNTIME_DEPENDENCIES_PATH));
        }
        // Poetry 1
        return extractObjectDirectDependencies(root.at(POETRY_1_RUNTIME_DEPENDENCIES_PATH));
    }

    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        // Poetry 1/2 groups
        return extractObjectDirectDependencies(root.at(DEV_DEPENDENCIES_PATH));
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        File lock = new File(pyProjectToml.getParentFile(), "poetry.lock");
        return mapper.readTree(lock);
    }

    @Override
    protected LockFileParser createLockFileParser(JsonNode lockRoot) {
        return lockParserFactory.getParser(lockRoot);
    }

    private List<UnresolvedModule> extractPep621Dependencies(JsonNode dependencies) {
        return new ArrayList<>(extractPep508DirectDependencies(dependencies));
    }
}
