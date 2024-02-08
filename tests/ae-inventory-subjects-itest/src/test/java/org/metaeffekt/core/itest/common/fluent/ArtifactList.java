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
package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.List;

public class ArtifactList implements
        ArtifactListFilter,
        ArtifactListSize,
        ArtifactListLogger,
        ArtifactListAsserts {

    private final List<Artifact> artifactlist;

    private String description;

    public ArtifactList(List<Artifact> artifacts, String description) {
        this.artifactlist = artifacts;
        this.description = description;
    }

    public ArtifactList(){
        this.artifactlist = new ArrayList<>();
        this.description = "unnamed list";
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<Artifact> getArtifactList() {
        return artifactlist;
    }

    @Override
    public ArtifactList setDescription(String description) {
        this.description = description;
        return this;
    }
}
