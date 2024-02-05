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
package org.metaeffekt.core.itest.inventory.dsl.predicates.attributes;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;

import java.util.function.Predicate;


public class AttributeValue implements NamedArtifactPredicate {

    private final String attribute;

    private final String value;

    public AttributeValue(String attribute, String value) {
        this.attribute = attribute;
        this.value = value;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     */
    public static NamedArtifactPredicate attributeValue(Artifact.Attribute attribute, String value) {
        return new AttributeValue(attribute.getKey(), value);
    }

    public static NamedArtifactPredicate attributeValue(String attribute, String value) {
        return new AttributeValue(attribute, value);
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> value.equals(artifact.get(attribute));
    }

    @Override
    public String getDescription() {
        return "'" + attribute + "' is '"+ value+"'";
    }
}
