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
package org.metaeffekt.core.itest.inventory.dsl;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.inventory.ArtifactList;

import java.util.List;

public interface ArtifactListDescriptor {

    List<Artifact> getArtifactList();

    String getDescription();

    ArtifactList setDescription(String description);

    default ArtifactList as(String description) {
        return this.setDescription(description);
    }

}
