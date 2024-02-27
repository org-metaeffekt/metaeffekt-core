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

package org.metaeffekt.core.itest.common.predicates;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenStartsWith<T, E extends Enum<E>> implements NamedBasePredicate<T> {

    @FunctionalInterface
    public interface AttributeGetter<T, E extends Enum<E>> {
        String getAttribute(T instance, E attributeKey);
    }

    private final AttributeGetter<T, E> attributeGetter;
    private final E attributeKey;
    private final String prefix;
    private final String[] separators;

    private static final String[] defaultSeparators = new String[] {".", "-", " ", "_"};

    private final int position;

    public TokenStartsWith(AttributeGetter<T, E> attributeGetter, E attributeKey, String prefix, String[] separators) {
        this(attributeGetter, attributeKey, prefix, separators, 0);
    }

    public TokenStartsWith(AttributeGetter<T, E> attributeGetter, E attributeKey, String prefix, String[] separators, int position) {
        this.attributeGetter = attributeGetter;
        this.attributeKey = attributeKey;
        this.prefix = prefix;
        this.separators = separators;
        this.position = position;
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> {
            List<String> tokens = tokenize(attributeGetter.getAttribute(instance, attributeKey), separators);
            return position < tokens.size() && tokens.get(position).startsWith(prefix);
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

    public static <T, E extends Enum<E>> NamedBasePredicate<T> tokenStartsWith(AttributeGetter<T, E> attributeGetter, E attributeKey, String prefix, String... separators) {
        String[] combinedSeparatorsArray = getCombinedSeparators(separators);
        return new TokenStartsWith<>(attributeGetter, attributeKey, prefix, combinedSeparatorsArray, 0);
    }

    public static <T, E extends Enum<E>> NamedBasePredicate<T> tokenStartsWith(AttributeGetter<T, E> attributeGetter, E attributeKey, String prefix) {
        return new TokenStartsWith<>(attributeGetter, attributeKey, prefix, defaultSeparators);
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
