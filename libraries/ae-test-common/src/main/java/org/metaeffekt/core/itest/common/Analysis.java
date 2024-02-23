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
package org.metaeffekt.core.itest.common;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.fluent.AssetList;
import org.metaeffekt.core.itest.common.fluent.ComponentPatternList;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;

import java.util.List;

public class Analysis {

    private final Inventory inventory;

    private final String description;

    public Analysis(Inventory inventory) {
        this(inventory, "All artifacts");
    }

    public Analysis(Inventory inventory, String description) {
        this.inventory = inventory;
        this.description = description;
    }

    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public ArtifactList selectArtifacts() {
        return new ArtifactList(inventory.getArtifacts(), description);
    }

    public ArtifactList selectArtifacts(NamedBasePredicate<Artifact> artifactPredicate) {
        return selectArtifacts()
                .filter(artifactPredicate.getPredicate())
                .as(artifactPredicate.getDescription());
    }

    public AssetList selectAssets() {
        return new AssetList(inventory.getAssetMetaData(), description);
    }

    public AssetList selectAssets(NamedBasePredicate<AssetMetaData> assetPredicate) {
        return selectAssets()
                .filter(assetPredicate.getPredicate())
                .as(assetPredicate.getDescription());
    }

    public ComponentPatternList selectComponentPatterns() {
        return new ComponentPatternList(inventory.getComponentPatternData(), description);
    }

    public ComponentPatternList selectComponentPatterns(NamedBasePredicate<ComponentPatternData> componentPatternPredicate) {
        return selectComponentPatterns()
                .filter(componentPatternPredicate.getPredicate())
                .as(componentPatternPredicate.getDescription());
    }
}
