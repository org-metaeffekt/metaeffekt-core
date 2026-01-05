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

/**
 * These trivial predicates are used during test development. You can implement the test asserts and filters
 * with these first and replace them when the Code under test becomes ready.
 *
 * @param <T> Type the predicate applied to.
 */
public class BooleanPredicate<T> implements NamedBasePredicate<T> {

    /**
     * Will return the whole collection when filtered.
     *
     * @param <T> Type the predicate applied to.
     *
     * @return The {@link BooleanPredicate} instance.
     */
    public static <T> NamedBasePredicate<T> alwaysTrue() {
        return new BooleanPredicate<>(true);
    }

    /**
     * Will return an empty collection when filtered.
     *
     * @param <T> Type the predicate applied to.
     *
     * @return The {@link BooleanPredicate} instance.
     */
    public static <T> NamedBasePredicate<T> alwaysFalse() {
        return new BooleanPredicate<>(false);
    }

    private boolean returnAll;

    public BooleanPredicate(boolean returnAll) {
        this.returnAll = returnAll;
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> returnAll;
    }

    @Override
    public String getDescription() {
        return returnAll ? "All Elements" : "No Elements";
    }
}
