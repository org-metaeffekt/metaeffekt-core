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
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing pdm toml and pdm.lock files.
 */
public class PdmParser extends PyProjectParser {
    private static final Pattern REQUIREMENT_PATTERN =
            Pattern.compile(
                    "^([A-Za-z0-9][A-Za-z0-9._-]*)" + // package name
                            "(?:\\[[^]]+\\])?" +               // ignore extras
                            "(.*)$"                            // version range
            );

    public PdmParser() {
        super("/project", "/dependencies", "/optional-dependencies/dev", "pdm.lock");
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.at("/project").isObject();
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath) {
        final List<UnresolvedModule> modules = new ArrayList<>();
        final JsonNode dependencyNode = projectNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode() && dependencyNode.isArray()) {
            dependencyNode.valueStream().forEach(dependency -> {
                UnresolvedModule unresolvedModule = parseRequirement(dependency.asText());
                modules.add(unresolvedModule);
            });
        }
        return modules;
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }

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

    @Override
    public List<ResolvedModule> getResolvedModulesFromLockFile(JsonNode lockNode) {
        final List<ResolvedModule> resolvedModules = new ArrayList<>();

        lockNode.path("package").valueStream().forEach(packageNode -> {
            final ResolvedModule resolvedModule = new ResolvedModule(packageNode.get("name").textValue(), null);
            resolvedModule.setVersion(packageNode.get("version").textValue());

            final PyProjectPackageSource source = parseSource(packageNode.path("index"));
            resolvedModule.setPyProjectPackageSource(source);

            final JsonNode packageDependenciesNode = packageNode.path("dependencies");
            final Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();
            if (!packageDependenciesNode.isMissingNode()) {
                packageDependenciesNode.valueStream().forEach(dependency -> {
                    final UnresolvedModule unresolvedModule = parseRequirement(dependency.asText());
                    unresolvedModuleMap.put(unresolvedModule.getName(), unresolvedModule);
                });
            }
            resolvedModule.setRuntimeDependencies(unresolvedModuleMap);

            resolvedModules.add(resolvedModule);
        });
        return resolvedModules;
    }

    private PyProjectPackageSource parseSource(JsonNode source) {
        if (source.isMissingNode()) {
            return new PyProjectPackageSource(null, null, null);
        }
        final JsonNode urlNode = source.path("url");
        return new PyProjectPackageSource(null, urlNode.asText(null), null);
    }
}
