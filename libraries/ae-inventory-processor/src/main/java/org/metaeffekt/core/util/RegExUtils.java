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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Common utils for dealing with regular expressions.
 */
public class RegExUtils {

    /**
     * Replaces the given regular expression with in the text by the specified replacement.
     *
     * This implementation applies the regular expression / replacement as often as a change is observed.
     *
     * @param text The text to apply the regular expression to.
     * @param regex The regular expression.
     * @param replacement The replacement string.
     *
     * @return The replaced text. If the regular expression does not match
     */
    public static String replaceAll(String text, String regex, String replacement) {
        return replaceAll(text, Pattern.compile(regex), replacement);
    }

    /**
     * Replaces the given regular expression with in the text by the specified replacement.
     *
     * This implementation applies the regular expression / replacement as often as a change is observed.
     *
     * @param text The text to apply the regular expression to.
     * @param pattern The regular expression pattern.
     * @param replacement The replacement string.
     *
     * @return The replaced text. If the regular expression does not match
     */
    public static String replaceAll(String text, Pattern pattern, String replacement) {
        Set<String> previousVersions = new HashSet<>();
        do {
            previousVersions.add(text);
            text = pattern.matcher(text).replaceAll(replacement);
        } while (!previousVersions.contains(text));
        return text;
    }

}
