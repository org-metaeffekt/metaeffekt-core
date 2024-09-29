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
import java.util.function.Predicate;

import static org.metaeffekt.core.itest.common.predicates.Not.not;

/**
 * {@link NamedBasePredicate} implementation checking for existence of an attribute in an object.
 *
 * @param <T> Type of the object.
 * @param <E> Key type.
 */
public class AttributeExists<T, E extends Enum<E>> implements NamedBasePredicate<T> {
    private final E attributeKey;

    public AttributeExists(E attributeKey) {
        this.attributeKey = attributeKey;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     *
     * @param attributeKey The attribute key to use.
     *
     * @param <T> Type of the object.
     * @param <E> Key type.
     *
     * @return The created {@link AttributeExists} predicate.
     */
    public static <T, E extends Enum<E>> NamedBasePredicate<T> withAttribute(E attributeKey) {
        return new AttributeExists<>(attributeKey);
    }


    /**
     * Only include Artifacts in the collection where attribute is null.
     *
     * @param attributeKey The attribute key to use.
     *
     * @param <T> Type of the object.
     * @param <E> Key type.
     *
     * @return The created AttributeExists predicate.
     */
    public static <T, E extends Enum<E>>  NamedBasePredicate<T> withoutAttribute(E attributeKey) {
        return not(new AttributeExists<>(attributeKey));
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> {
            try {
                final Method getMethod = instance.getClass().getMethod("get", attributeKey.getDeclaringClass());
                final String attributeValue = (String) getMethod.invoke(instance, attributeKey);
                return attributeValue != null;
            } catch (Exception e) {
                return false;
            }
        };
    }

    @Override
    public String getDescription() {
        return "'" + attributeKey.name() + "' exists";
    }

}
