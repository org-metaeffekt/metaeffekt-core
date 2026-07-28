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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.pdm;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.AbstractPyProjectParser;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class for parsing pdm toml and pdm.lock files.
 */
public abstract class AbstractPdmParser extends AbstractPyProjectParser {
    private static final Pattern REQUIREMENT_PATTERN = Pattern.compile(
            "^([A-Za-z0-9][A-Za-z0-9._-]*)" + // package name
                    "(?:\\[[^]]+\\])?" +               // ignore extras
                    "(.*)$"                            // version range
    );

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode rootNode, String fullQualifiedPath) {
        final List<UnresolvedModule> unresolvedModules = new ArrayList<>();
        final JsonNode dependencyNode = rootNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode() && dependencyNode.isArray()) {
            dependencyNode.valueStream().forEach(dependency -> {
                final UnresolvedModule unresolvedModule = parseRequirement(dependency.asText());
                unresolvedModules.add(unresolvedModule);
            });
        }
        return unresolvedModules;
    }

    @Override
    public void extractAndFillUnresolvedModules(JsonNode packageDependenciesNode, Map<String, UnresolvedModule> unresolvedModuleMap) {
        if (!packageDependenciesNode.isMissingNode() && packageDependenciesNode.isArray()) {
            packageDependenciesNode.valueStream().forEach(dependency -> {
                final UnresolvedModule unresolvedModule = parseRequirement(dependency.asText());
                unresolvedModuleMap.put(unresolvedModule.getName(), unresolvedModule);
            });
        }
    }

    @Override
    public PyProjectPackageSource parseSource(JsonNode packageNode) {
        final JsonNode files = packageNode.path("files");
        if (files.isMissingNode()) {
            return null;
        }

        final List<String> urls = new ArrayList<>();
        for (final JsonNode file : files) {
            if (!file.isMissingNode()) {
                final String url = file.path("url").asText(null);
                addIfNotNull(urls, url);
            }
        }

        return urls.isEmpty() ? null : new PyProjectPackageSource(null, urls, null);
    }

    @Override
    public File getLockFile(File pyProjectToml) {
        return new File(pyProjectToml.getParentFile(), "pdm.lock");
    }

    /**
     * Parses a requirement (dependency with version and additional data) and only stored the name and version of it.
     *
     * @param requirement the requirement string to parse
     * @return an unresolved module instance build from the requirement
     */
    private UnresolvedModule parseRequirement(String requirement) {
        final String cleanedRequirement = removeMarker(requirement);

        final Matcher matcher = REQUIREMENT_PATTERN.matcher(cleanedRequirement);
        if (!matcher.matches()) {
            return new UnresolvedModule(requirement, null, null);
        }

        final String name = matcher.group(1);
        String versionRange = matcher.group(2);
        if (versionRange != null) {
            versionRange = versionRange.trim();
        }

        return new UnresolvedModule(name, null, versionRange);
    }

    private String removeMarker(String requirement) {
        final int markerIndex = requirement.indexOf(';');
        if (markerIndex >= 0) {
            return requirement.substring(0, markerIndex).trim();
        }
        return requirement.trim();
    }
}
