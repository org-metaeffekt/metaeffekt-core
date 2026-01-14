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
package org.metaeffekt.core.inventory.relationship;

import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Helper class to extract new identifiers from inventory objects or create new identifiers.
 * @param <T> object type generic
 */
class IdentifierStrategy<T> {
    private final List<String> fieldNames;
    private final List<FieldExtractor<T>> extractors;

    IdentifierStrategy(List<String> fieldNames, List<FieldExtractor<T>> extractors) {
        this.fieldNames = fieldNames;
        this.extractors = extractors;
    }

    /**
     * Creates a new identifier from an inventory object if a fitting identifier creation strategy is found.
     * @param object the inventory object determining the strategy used
     * @return the identifier as a string
     */
    public String createIdentifier(T object) {
        StringJoiner joiner = new StringJoiner("-");
        for (FieldExtractor<T> extractor : extractors) {
            String value = extractor.extract(object);
            if (StringUtils.isNotBlank(value)) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    /**
     * Maps the original field names from which the identifier was created to their respective values.
     * @param identifier the string identifer of an inventory object.
     * @return a map containing field name to value pairs.
     */
    Map<String, String> parseIdentifier(String identifier) {
        String[] parts = identifier.split("-");
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < parts.length && i < fieldNames.size(); i++) {
            result.put(fieldNames.get(i), parts[i]);
        }
        return result;
    }
}
