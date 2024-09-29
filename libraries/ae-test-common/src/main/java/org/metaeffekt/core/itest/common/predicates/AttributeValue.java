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
package org.metaeffekt.core.itest.common.predicates;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * {@link NamedBasePredicate} implementation checking for a specific attribute value in an object.
 *
 * @param <T> Type of the object.
 * @param <E> Key type.
 */
public class AttributeValue<T, E extends Enum<E>> implements NamedBasePredicate<T> {

    private final E attributeKey;
    private final String value;

    public AttributeValue(E attributeKey, String value) {
        this.attributeKey = attributeKey;
        this.value = value;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     *
     * @param attributeKey The attribute key to use.
     * @param value The attribute value to match.
     *
     * @param <T> Type of the object.
     * @param <E> Key type.
     *
     * @return The created {@link AttributeValue} predicate.
     */
    public static <T, E extends Enum<E>> NamedBasePredicate<T> attributeValue(E attributeKey, String value) {
        return new AttributeValue<>(attributeKey, value);
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> {
            try {
                Method getMethod = instance.getClass().getMethod("get", attributeKey.getDeclaringClass());
                String attributeValue = (String) getMethod.invoke(instance, attributeKey);
                return Objects.equals(attributeValue, value);
            } catch (Exception e) {
                return false;
            }
        };
    }

    @Override
    public String getDescription() {
        return "'" + attributeKey + "' is '" + value + "'";
    }
}
