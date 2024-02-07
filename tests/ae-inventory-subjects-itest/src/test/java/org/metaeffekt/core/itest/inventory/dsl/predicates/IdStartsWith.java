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
package org.metaeffekt.core.itest.inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public class IdStartsWith implements NamedArtifactPredicate {

    private String prefix;

    public IdStartsWith(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return new Predicate<Artifact>() {
            @Override
            public boolean test(Artifact artifact) {
                String id = artifact.getId();
                return id.startsWith(prefix);
            }
        };
    }

    @Override
    public String getDescription() {
        return "Artifact id starts with " + prefix;
    }

    public static IdStartsWith idStartsWith(String prefix) {
        return new IdStartsWith(prefix);
    }
}
