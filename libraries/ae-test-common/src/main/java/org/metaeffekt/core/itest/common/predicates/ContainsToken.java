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

import java.util.function.Predicate;

public class ContainsToken<T, E extends Enum<E>> implements NamedBasePredicate<T> {

    @FunctionalInterface
    public interface AttributeGetter<T, E extends Enum<E>> {
        String getAttribute(T instance, E attributeKey);
    }

    private final AttributeGetter<T, E> attributeGetter;
    private final E attributeKey;
    private final String token;

    public ContainsToken(AttributeGetter<T, E> attributeGetter, E attributeKey, String token) {
        this.attributeGetter = attributeGetter;
        this.attributeKey = attributeKey;
        this.token = token;
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> attributeGetter.getAttribute(instance, attributeKey).contains(token);
    }

    @Override
    public String getDescription() {
        return String.format("Object's '%s' attribute contains token with '%s'", attributeKey.name(), token);
    }

    public static <T, E extends Enum<E>> NamedBasePredicate<T> containsToken(AttributeGetter<T, E> attributeGetter, E attributeKey, String token) {
        return new ContainsToken<>(attributeGetter, attributeKey, token);
    }
}
