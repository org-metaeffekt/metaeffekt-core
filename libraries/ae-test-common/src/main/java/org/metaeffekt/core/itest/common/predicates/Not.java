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

import java.util.function.Predicate;

public class Not implements NamedBasePredicate<Artifact> {

    private final NamedBasePredicate<Artifact> input;

    private Not(NamedBasePredicate<Artifact> input) {
        this.input = input;
    }

    /**
     * Return the inverted meaning for a filterpredicate.
     */
    public static NamedBasePredicate<Artifact> not(NamedBasePredicate<Artifact> input) {
        return new Not(input);
    }

    @Override
    public Predicate<Artifact> getPredicate() {
        return input.getPredicate().negate();
    }

    @Override
    public String getDescription() {
        return " NOT( " + input.getDescription() + " ) ";
    }
}
