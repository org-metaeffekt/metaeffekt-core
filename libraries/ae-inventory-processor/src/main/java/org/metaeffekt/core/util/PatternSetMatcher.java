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
package org.metaeffekt.core.util;

import lombok.Getter;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.regex.Pattern;

public class PatternSetMatcher {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private static final Comparator<String> LENGTH_COMPARATOR = (o1, o2) -> o2.length() - o1.length();

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\\*\\?]+");

    private static final String SEPARATOR_SLASH = "/";

    /**
     * Maps the longest possible literal part of a pattern to
     */
    @Getter
    private final Map<String, Set<String>> longestLiteralMatchPatternMap = new HashMap<>();

    public PatternSetMatcher(Collection<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (pattern != null) {
                    final String modulatedPattern = pattern.replace("**/", "*");
                    final String[] split = SEPARATOR_PATTERN.split(modulatedPattern);
                    final String longestLiteralPart = Arrays.stream(split).sorted(LENGTH_COMPARATOR).findFirst().orElse("");
                    longestLiteralMatchPatternMap.computeIfAbsent(longestLiteralPart, c -> new HashSet<>()).add(pattern);
                }
            }
        }
    }

    public boolean matches(String stringToMatch) {
        for (final Map.Entry<String, Set<String>> entry : longestLiteralMatchPatternMap.entrySet()) {
            final String literal = entry.getKey();

            // check whether longest literal is matched; prefilter candidates
            if (stringToMatch.contains(literal)) {

                // match patterns; exit once matched
                for (String pattern : entry.getValue()) {
                    if (internalMatching(stringToMatch, pattern)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The AntPathMatcher has the unexpected behavior to treat absolute paths differently. These would only match in
     * case the pattern is also absolute. This method adapts pattern and path to reach the anticipated results.
     *
     * @param normalizedPath Normalized path to match.
     * @param normalizedPattern Normalized pattern to match.
     *
     * @return <code>true</code> in case the pattern matches the path.
     */
    public static boolean internalMatching(final String normalizedPath, final String normalizedPattern) {

        // match on string equals level when no wildcard is contained
        if (!normalizedPattern.contains("*")) {
            return normalizedPath.equals(normalizedPattern);
        }

        if (normalizedPattern.startsWith(SEPARATOR_SLASH)) {
            // matching absolute path; check whether this is at all needed; i.e. by static defined component patterns

            if (!normalizedPath.contains(":")) {
                final Boolean matched = matchStandardPatternAnyFileInPath(normalizedPath, normalizedPattern);
                if (matched != null) return matched;
            }

            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern.substring(1), normalizedPath);
            }
        } else {

            if (!normalizedPath.contains(":")) {
                if (normalizedPattern.startsWith("**/")) {
                    final String subPattern = normalizedPattern.substring(2);
                    if (!subPattern.contains("*") && !subPattern.contains(":")) {
                        return normalizedPath.endsWith(subPattern);
                    }
                } else {
                    final Boolean matched = matchStandardPatternAnyFileInPath(normalizedPath, normalizedPattern);
                    if (matched != null) return matched;
                }
            }

            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath.substring(1));
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            }
        }
    }

    public static Boolean matchStandardPatternAnyFileInPath(String normalizedPath, String normalizedPattern) {
        if (normalizedPattern.endsWith("/**/*")) {
            final String subPattern = normalizedPattern.substring(0, normalizedPattern.length() - 4);
            if (!subPattern.contains("*") && !subPattern.contains(":")) {
                return normalizedPath.startsWith(subPattern);
            }
        }

        // return null to indicate that match was not evaluated
        return null;
    }

}
