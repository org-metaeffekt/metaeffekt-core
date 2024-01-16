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

import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.Not.not;


public class Exists implements NamedArtifactPredicate {

    private final String attribute;

    public Exists(String attribute) {
        this.attribute = attribute;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     */
    public static NamedArtifactPredicate withAttribute(Artifact.Attribute attribute){
        return new Exists(attribute.getKey());
    }


    /**
     * Only include Artifacts in the collection where attribute is null.
     */
    public static NamedArtifactPredicate withoutAttribute(Artifact.Attribute attribute){
        return not(new Exists(attribute.getKey()));
    }


    public static NamedArtifactPredicate withAttribute(String attribute){
        return new Exists(attribute);
    }
    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> artifact.get(attribute) != null;
    }

    @Override
    public String getDescription() {
        return "'"+attribute+"' exists";
    }
}
