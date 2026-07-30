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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.pdm;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.LockFileParserFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class for parsing pdm toml files.
 */
public class PdmTomlParser extends AbstractPdmTomlParser {
    private final LockFileParserFactory lockParserFactory;
    private static final String RUNTIME_DEPENDENCIES_PATH = "/project/dependencies";
    private static final String DEV_DEPENDENCIES_PATH = "/tool/pdm/dev-dependencies/dev";

    public PdmTomlParser(LockFileParserFactory lockParserFactory) {
        this.lockParserFactory = lockParserFactory;
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.path("project").isObject() && root.at("/tool/pdm").isObject();
    }

    @Override
    protected ResolvedModule parseProject(JsonNode root) {
        JsonNode projectNode = root.path("project");
        ResolvedModule module = new ResolvedModule(projectNode.path("name").asText(), null);
        // version can be dynamic
        module.setVersion(projectNode.path("version").asText(null));

        return module;
    }

    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        return extractPep508DirectDependencies(root.at(RUNTIME_DEPENDENCIES_PATH));
    }

    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        return extractPep508DirectDependencies(root.at(DEV_DEPENDENCIES_PATH));
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        File lock = new File(pyProjectToml.getParentFile(), "pdm.lock");
        return mapper.readTree(lock);
    }

    @Override
    protected LockFileParser createLockFileParser(JsonNode lockRoot) {
        return lockParserFactory.getParser(lockRoot);
    }
}
