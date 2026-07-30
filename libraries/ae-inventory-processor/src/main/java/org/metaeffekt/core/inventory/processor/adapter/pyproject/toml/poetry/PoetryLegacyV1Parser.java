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
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.AbstractTomlParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.extractVersion;

/**
 * Class for parsing legacy (V1) poetry toml files.
 */
public class PoetryLegacyV1Parser extends AbstractTomlParser implements PoetryToml {
    private static final String RUNTIME_DEPENDENCIES_PATH = "/tool/poetry/dependencies";
    private static final String DEV_DEPENDENCIES_PATH = "/tool/poetry/group/dev/dependencies";
    private final String POETRY_PATH = getPoetryPath();

    @Override
    public boolean supports(JsonNode root) {
        return root.at(POETRY_PATH).isObject() && !root.at("/project").isObject();
    }

    @Override
    public String getIncludePattern() {
        return getPoetryIncludePattern();
    }

    @Override
    protected JsonNode getProjectNode(JsonNode root) {
        return root.at(POETRY_PATH);
    }

    @Override
    protected ResolvedModule parseProject(JsonNode projectNode) {
        return parsePoetryProject(projectNode);
    }

    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        return extractObjectDirectDependencies(root.at(RUNTIME_DEPENDENCIES_PATH));
    }

    @Override
    protected List<UnresolvedModule> extractDevelopmentDependencies(JsonNode root) {
        return extractObjectDirectDependencies(root.at(DEV_DEPENDENCIES_PATH));
    }

    @Override
    protected JsonNode readLockFile(File pyProjectToml) throws IOException {
        return mapper.readTree(getLockFile(pyProjectToml));
    }

    protected List<UnresolvedModule> extractObjectDirectDependencies(JsonNode dependencyNode) {
        final List<UnresolvedModule> unresolvedModules = new ArrayList<>();
        if (dependencyNode.isMissingNode()) {
            return unresolvedModules;
        }
        dependencyNode.propertyStream().forEach(entry ->
                unresolvedModules.add(new UnresolvedModule(entry.getKey(), null, extractVersion(entry.getValue())))
        );
        return unresolvedModules;
    }
}
