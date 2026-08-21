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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.*;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.PyProjectUtils.parseRequirement;

/**
 * Abstract class for parsing pdm toml files.
 */
@Slf4j
public abstract class AbstractPep621Parser extends AbstractTomlParser {
    protected static final String PEP_735_DEPENDENCY_GROUPS_PATH = "/dependency-groups";
    private static final String PROJECT_PATH = "/project";
    private static final String RUNTIME_DEPENDENCIES_PATH = PROJECT_PATH + "/dependencies";

    @Override
    protected JsonNode getProjectNode(JsonNode root) {
        return root.at(PROJECT_PATH);
    }

    @Override
    protected List<UnresolvedModule> extractRuntimeDependencies(JsonNode root) {
        return extractPep508Dependencies(root.at(RUNTIME_DEPENDENCIES_PATH));
    }

    /**
     * Extracts PEP735 dependency groups for development dependencies and also its included dependency groups (recursively).
     *
     * @param groupsNode the group node
     * @return Map of unresolved development dependencies by group name
     */
    protected static Map<String, List<UnresolvedModule>> extractAllPEP735DependencyGroups(JsonNode groupsNode) {
        if (!groupsNode.isObject()) {
            return Map.of();
        }

        final Map<String, List<UnresolvedModule>> devDependenciesByGroupName = new LinkedHashMap<>();
        groupsNode.propertyNames().iterator().forEachRemaining(groupName ->
                devDependenciesByGroupName.put(groupName, extractPEP735DependencyGroup(groupsNode, groupName, new LinkedHashSet<>()))
        );
        return devDependenciesByGroupName;
    }

    /**
     * Resolves a single named group from a PEP 735 {@code [dependency-groups]} table, recursively expanding any {@code {include-group = "..."}} references it contains.
     *
     * <p>Each entry in the group's array is either a PEP 508 dependency string
     * (added directly) or an include reference, whose target group is resolved
     * recursively and merged into the result. Dependencies are deduplicated by
     * name across the group and all of its (transitive) includes.
     *
     * <p>{@code pathInProgress} tracks the chain of group names currently being
     * resolved on the current recursion path (not globally across sibling calls),
     * so that diamond-shaped includes (e.g. both {@code dev} and {@code ci}
     * including {@code test}) are resolved twice without being mistaken for a
     * cycle. If a group is encountered a second time on the same path, an
     * {@link IllegalStateException} is thrown describing the full cycle.
     *
     * @param groupsNode     the JSON node representation of the {@code [dependency-groups]} table
     * @param groupName      the name of the group to resolve
     * @param pathInProgress the set of group names on the current recursion path
     * @return the fully resolved, deduplicated list of dependencies for {@code groupName},
     * or an empty list if the group does not exist
     * @throws IllegalStateException if a cyclic {@code include-group} reference is detected,
     *                               or if a group entry is neither a string nor a valid
     *                               {@code include-group} table
     */
    private static List<UnresolvedModule> extractPEP735DependencyGroup(JsonNode groupsNode, String groupName, LinkedHashSet<String> pathInProgress) {
        if (groupsNode.isMissingNode() || !groupsNode.has(groupName)) {
            return List.of();
        }

        if (!pathInProgress.add(groupName)) {
            final String cycle = String.join(" -> ", pathInProgress) + " -> " + groupName;
            throw new IllegalStateException("Cycle detected in include-group: " + cycle);
        }

        final JsonNode groupArray = groupsNode.get(groupName);
        final ArrayNode stringEntries = JsonNodeFactory.instance.arrayNode();
        final List<String> includedGroups = new ArrayList<>();

        for (final JsonNode entry : groupArray) {
            if (entry.isTextual()) {
                stringEntries.add(entry);
            } else if (entry.isObject() && entry.has("include-group")) {
                includedGroups.add(entry.get("include-group").asText());
            } else {
                throw new IllegalStateException("Invalid entry in dependency-groups." + groupName + ": " + entry);
            }
        }

        final Map<String, UnresolvedModule> unresolvedModuleMap = new LinkedHashMap<>();
        // own, direct PEP 508 dependency strings (without included groups)
        final List<UnresolvedModule> directPep508dependencies = extractPep508Dependencies(stringEntries);
        for (final UnresolvedModule module : directPep508dependencies) {
            unresolvedModuleMap.put(module.getName(), module);
        }
        // recursively include other included groups
        for (final String included : includedGroups) {
            final List<UnresolvedModule> pep735DependencyGroupDependencies = extractPEP735DependencyGroup(groupsNode, included, pathInProgress);
            mergeInto(unresolvedModuleMap, pep735DependencyGroupDependencies);
        }

        pathInProgress.remove(groupName);
        return List.copyOf(unresolvedModuleMap.values());
    }

    /**
     * Extracts PEP508 dependency strings.
     *
     * @param dependenciesNode the dependencies node (array)
     * @return list of unresolved dependencies
     */
    protected static List<UnresolvedModule> extractPep508Dependencies(JsonNode dependenciesNode) {
        if (!dependenciesNode.isArray()) {
            return List.of();
        }

        return dependenciesNode.valueStream().map(dependency -> {
            if (!dependency.isTextual()) {
                log.info("Expected PEP-508-String but found: {}", dependency);
            }
            return parseRequirement(dependency.asText());
        }).toList();
    }
}
