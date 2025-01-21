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
package org.metaeffekt.core.inventory.processor.patterns.contributors.cargo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class to deal with nested maps, list and strings.
 *
 * @param <K> Key type of the base map.
 * @param <V> Value type of the base map.
 */
public abstract class AbstractMapAccess<K, V> {

    private final Map<K, V> map;

    public AbstractMapAccess(Map<K, V> map) {
        this.map = map;
    }

    public String stringOf(K key) {
        return valueOfOrNull(map.get(key));
    }

    @SuppressWarnings("unchecked")
    public <R, S> Map<R, S> mapOf(K key) {
        final V v = map.get(key);
        if (v instanceof Map) {
            return (Map<R, S>) v;
        } else {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public <R> List<R> listOf(K key) {
        final V v = map.get(key);
        if (v instanceof List) {
            return (List<R>) v;
        } else {
            return Collections.emptyList();
        }
    }

    private String valueOfOrNull(V object) {
        if (object == null) return null;
        return String.valueOf(object);
    }

}
