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

package org.metaeffekt.core.itest.common.predicates;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenStartsWith<T, E extends Enum<E>> implements NamedBasePredicate<T> {

    private final E attributeKey;
    private final String prefix;
    private final String[] separators;

    private static final String[] defaultSeparators = new String[] {".", "-", " ", "_"};

    private final int position;

    public TokenStartsWith(E attributeKey, String prefix, String[] separators) {
        this(attributeKey, prefix, separators, 0);
    }

    public TokenStartsWith(E attributeKey, String prefix, String[] separators, int position) {
        this.attributeKey = attributeKey;
        this.prefix = prefix;
        this.separators = separators;
        this.position = position;
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> {
            try {
                Method getMethod = instance.getClass().getMethod("get", attributeKey.getDeclaringClass());
                String attributeValue = (String) getMethod.invoke(instance, attributeKey);
                List<String> tokens = tokenize(attributeValue, separators);
                return position < tokens.size() && tokens.get(position).startsWith(prefix);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Override
    public String getDescription() {
        return String.format("Object's '%s' attribute token starts with '%s'", attributeKey.name(), prefix);
    }

    private static List<String> tokenize(String value, String... separators) {
        String[] combinedSeparatorsArray = getCombinedSeparators(separators);
        if (value == null) {
            return Collections.emptyList();
        }

        String combinedRegex = Arrays.stream(combinedSeparatorsArray)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));

        return Arrays.asList(value.split(combinedRegex));
    }

    public static <T, E extends Enum<E>> NamedBasePredicate<T> tokenStartsWith(E attributeKey, String prefix, String... separators) {
        String[] combinedSeparatorsArray = getCombinedSeparators(separators);
        return new TokenStartsWith<>(attributeKey, prefix, combinedSeparatorsArray, 0);
    }

    public static <T, E extends Enum<E>> NamedBasePredicate<T> tokenStartsWith(E attributeKey, String prefix) {
        return new TokenStartsWith<>(attributeKey, prefix, defaultSeparators);
    }

    private static String[] getCombinedSeparators(String... separators) {
        Set<String> combinedSeparators = Stream.concat(Arrays.stream(defaultSeparators), Arrays.stream(separators))
                .collect(Collectors.toCollection(() -> Stream.of(defaultSeparators).collect(Collectors.toSet())));
        return combinedSeparators.toArray(new String[0]);
    }

    public static String getTokenAtPosition(String value, int position, String... separators) {
        List<String> tokenList = tokenize(value, separators);
        if (tokenList.size() > position && !tokenList.isEmpty()) {
            return tokenize(value, separators).get(position);
        }
        return null;
    }
}
