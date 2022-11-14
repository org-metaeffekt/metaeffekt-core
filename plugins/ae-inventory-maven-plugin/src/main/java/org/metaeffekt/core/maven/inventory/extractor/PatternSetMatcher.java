/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.extractor;

import org.springframework.util.AntPathMatcher;

import java.util.*;

public class PatternSetMatcher {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private static final Comparator<String> LENGTH_COMPARATOR = (o1, o2) -> o2.length() - o1.length();

    /**
     * Maps the longest possible literal part of a pattern to
     */
    private final Map<String, Set<String>> longestLiteralMatchPatternMap = new HashMap<>();

    public PatternSetMatcher(Collection<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                final String[] splitPattern = pattern.split("[\\*\\?]+");
                final String longestLiteralPart = Arrays.stream(splitPattern).sorted(LENGTH_COMPARATOR).findFirst().get();
                longestLiteralMatchPatternMap.computeIfAbsent(longestLiteralPart, c -> new HashSet<>()).add(pattern);
            }
        }
    }

    public boolean matches(String stringToMatch) {
        for (final Map.Entry<String, Set<String>> entry : longestLiteralMatchPatternMap.entrySet()) {
            final String literal = entry.getKey();
            if (stringToMatch.contains(literal)) {
                // match on full literal key
                if (literal.equals(stringToMatch)) {
                    return true;
                }

                // match patterns
                for (String pattern : entry.getValue()) {
                    if (ANT_PATH_MATCHER.match(pattern, stringToMatch)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
