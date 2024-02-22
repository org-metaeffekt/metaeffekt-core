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

public class StartsWith implements NamedArtifactPredicate {

    private final String prefix;

    private final Artifact.Attribute attribute;

    public StartsWith(Artifact.Attribute attribute, String prefix) {
        this.attribute = attribute;
        this.prefix = prefix;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
      return artifact -> {
            String value = artifact.get(attribute);
            return value != null && value.startsWith(prefix);
        };
    }

    @Override
    public String getDescription() {
        return "Artifact " + attribute.getKey() + " starts with " + prefix;
    }

    public static StartsWith predicateStartsWith(Artifact.Attribute attribute, String prefix) {
        return new StartsWith(attribute, prefix);
    }
}
