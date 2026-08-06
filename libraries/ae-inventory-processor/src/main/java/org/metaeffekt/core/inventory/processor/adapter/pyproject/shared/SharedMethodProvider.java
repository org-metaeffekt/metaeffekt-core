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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.shared;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SharedMethodProvider {
    private static final Pattern REQUIREMENT_PATTERN = Pattern.compile(
            "^([A-Za-z0-9][A-Za-z0-9._-]*)" + // package name
                    "(?:\\[[^]]+\\])?" +               // ignore extras
                    "(.*)$"                            // version range
    );
    private static final Pattern SEPARATOR_RUN = Pattern.compile("[-_.]+");

    /**
     * Adds an element to a collection if it is not null.
     *
     * @param collection the collection or any subclass of the collection
     * @param value      the value
     * @param <T>        the type of the value
     */
    public static <T> void addIfNotNull(Collection<T> collection, T value) {
        if (value != null) {
            collection.add(value);
        }
    }

    /**
     * The dependency value containing the version information can either be a JSON object, a JSON array or a string.
     *
     * @param dependencyNode the dependency node containing the version
     * @return the version
     */
    public static String extractVersion(JsonNode dependencyNode) {
        if (dependencyNode.isTextual()) {
            return dependencyNode.asText();
        } else if (dependencyNode.isObject()) {
            return dependencyNode.path("version").asText(null);
        } else if (dependencyNode.isArray()) {
            dependencyNode.get(1).get("version").textValue();
        }
        return null;
    }

    /**
     * Normalizes a package name as defined by PEP 503.
     * -, _, . are translated to '-'.
     * Example: 'charset_normalizer', 'Charset-Normalizer', 'charset.normalizer' are normalized to 'charset-normalizer'.
     *
     * @param name the unnormalized package name
     * @return the normalized package name
     */
    public static String normalizePackageName(String name) {
        if (name == null) return null;
        return SEPARATOR_RUN.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-");
    }

    /**
     * Parses a requirement (dependency with version and additional data) and only stored the name and version of it.
     *
     * @param requirement the requirement string to parse
     * @return an unresolved module instance build from the requirement
     */
    public static UnresolvedModule parseRequirement(String requirement) {
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

    private static String removeMarker(String requirement) {
        final int markerIndex = requirement.indexOf(';');
        if (markerIndex >= 0) {
            return requirement.substring(0, markerIndex).trim();
        }
        return requirement.trim();
    }
}
