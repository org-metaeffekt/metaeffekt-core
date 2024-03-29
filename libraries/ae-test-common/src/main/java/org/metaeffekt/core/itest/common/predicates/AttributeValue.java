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

public class AttributeValue<T, E extends Enum<E>> implements NamedBasePredicate<T> {
    private final E attributeKey;
    private final String value;

    public AttributeValue(E attributeKey, String value) {
        this.attributeKey = attributeKey;
        this.value = value;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
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
                return value.equals(attributeValue);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        };
    }

    @Override
    public String getDescription() {
        return "'" + attributeKey + "' is '" + value + "'";
    }
}
