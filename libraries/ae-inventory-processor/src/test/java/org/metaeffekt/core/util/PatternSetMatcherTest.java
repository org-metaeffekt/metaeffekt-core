/*
 * Copyright 2009-2024 the original author or authors.
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

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class PatternSetMatcherTest {

    @Test
    public void testComputeLongestLiteral() {

        final ArrayList<String> patterns = new ArrayList();
        patterns.add("this/is/a/longer/path/**/*");
        patterns.add("**/somepath/*");
        patterns.add("**/somepath/**/*");
        patterns.add("**/*");
        patterns.add("");
        patterns.add(null);

        final PatternSetMatcher patternSetMatcher = new PatternSetMatcher(patterns);

        final Map<String, Set<String>> longestLiteralMatchPatternMap = patternSetMatcher.getLongestLiteralMatchPatternMap();

        final Set<String> pathKeyPatterns = longestLiteralMatchPatternMap.get("this/is/a/longer/path/");
        Assertions.assertThat(pathKeyPatterns).isNotNull();
        Assertions.assertThat(pathKeyPatterns).contains("this/is/a/longer/path/**/*");

        // **/* modulated to ** --> ""; any string
        Assertions.assertThat(longestLiteralMatchPatternMap.containsKey("/")).isFalse();

        // **/* and "" produce an "" as key.
        final Set<String> emptyKeyPatterns = longestLiteralMatchPatternMap.get("");
        Assertions.assertThat(emptyKeyPatterns).isNotNull();
        Assertions.assertThat(emptyKeyPatterns).contains("**/*");
        Assertions.assertThat(emptyKeyPatterns).contains("");

        final Set<String> somePathKeyPatterns = longestLiteralMatchPatternMap.get("somepath/");
        Assertions.assertThat(somePathKeyPatterns).isNotNull();
        Assertions.assertThat(somePathKeyPatterns).contains("**/somepath/**/*");
        Assertions.assertThat(somePathKeyPatterns).contains("**/somepath/*");

    }

}