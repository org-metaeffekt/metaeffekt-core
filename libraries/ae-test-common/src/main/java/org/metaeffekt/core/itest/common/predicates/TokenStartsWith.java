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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenStartsWith implements NamedBasePredicate<Artifact> {

    private final Artifact.Attribute attribute;
    private final String prefix;
    private final String[] separators;

    private static final String[] defaultSeparators = new String[] {Constants.DOT, Constants.HYPHEN, Constants.SPACE, Constants.UNDERSCORE};

    private final int position;

    public TokenStartsWith(Artifact.Attribute attribute, String prefix, String[] separators) {
        this(attribute, prefix, separators, 0);
    }

    public TokenStartsWith(Artifact.Attribute attribute, String prefix, String[] separators, int position) {
        this.attribute = attribute;
        this.prefix = prefix;
        this.separators = separators;
        this.position = position;
    }

    @Override
    public Predicate<Artifact> getPredicate() {
        return artifact -> {
            List<String> tokens = tokenize(artifact.get(attribute), separators);
            return position < tokens.size() && tokens.get(position).startsWith(prefix);
        };
    }

    @Override
    public String getDescription() {
        return "Artifact " + attribute.getKey() + " token starts with " + prefix;
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

    public static NamedBasePredicate<Artifact> tokenStartsWith(Artifact.Attribute attribute, String prefix, String... separators) {
        String[] combinedSeparatorsArray = getCombinedSeparators(separators);
        return new TokenStartsWith(attribute, prefix, combinedSeparatorsArray);
    }

    public static NamedBasePredicate<Artifact> tokenStartsWith(Artifact.Attribute attribute, String prefix) {
        return new TokenStartsWith(attribute, prefix, defaultSeparators);
    }

    private static String[] getCombinedSeparators(String... separators) {
        Set<String> combinedSeparators = Stream.concat(Arrays.stream(defaultSeparators), Arrays.stream(separators))
                .collect(Collectors.toCollection(() -> Stream.of(defaultSeparators).collect(Collectors.toSet())));
        return combinedSeparators.toArray(new String[0]);
    }

    public static String getTokenAtPosition(String value, int position, String... separators) {
        List<String> tokenList = tokenize(value, separators);
        if (tokenList.size() > position && tokenList.size() > 0) {
            return tokenize(value, separators).get(position);
        }
        return null;
    }
}
