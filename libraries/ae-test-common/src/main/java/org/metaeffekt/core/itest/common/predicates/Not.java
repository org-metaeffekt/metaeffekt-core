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

import java.util.function.Predicate;

public class Not<T> implements NamedBasePredicate<T> {

    private final NamedBasePredicate<T> input;

    private Not(NamedBasePredicate<T> input) {
        this.input = input;
    }

    /**
     * Return the inverted meaning for a {@link NamedBasePredicate}.
     *
     * @param input The predicate to negate.
     *
     * @param <T> Type the predicate applied to.
     *
     * @return The {@link BooleanPredicate} instance.
     */
    public static <T> NamedBasePredicate<T> not(NamedBasePredicate<T> input) {
        return new Not<>(input);
    }

    @Override
    public Predicate<T> getPredicate() {
        return input.getPredicate().negate();
    }

    @Override
    public String getDescription() {
        return " NOT( " + input.getDescription() + " ) ";
    }
}
