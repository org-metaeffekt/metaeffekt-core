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
package org.metaeffekt.core.itest.inventory;

import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.List;

public class Analysis {
    private final Inventory inventory;

    public Analysis(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public ArtifactList selectArtifacts() {
        return new ArtifactList(inventory.getArtifacts(), "All artifacts");
    }

    public ArtifactList selectArtifacts(NamedArtifactPredicate artifactPredicate) {
        return selectArtifacts()
                .filter(artifactPredicate.getArtifactPredicate())
                .as(artifactPredicate.getDescription());
    }
}
