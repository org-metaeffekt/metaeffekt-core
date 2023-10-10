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
package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Processor to attach key value pairs to assets
 */
public class GenericAssetInventoryProcessor extends BaseInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GenericAssetInventoryProcessor.class);

    private Map<String, String> attributes;

    private String assetId;

    public GenericAssetInventoryProcessor augmenting(File inventoryFile) {
        super.augmenting(inventoryFile);
        return this;
    }

    public void process() throws IOException {
        final Inventory inventory = new InventoryReader().readInventory(getInventoryFile());

        // FIXME: currently we do not expect, that the asset metadata may already exists; create new instance and add
        final AssetMetaData assetMetaData = new AssetMetaData();

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {

            // handle escaping (_ are spaces, __ are _)
            String key = attribute.getKey().replaceAll("([^_]+)_([^_]+)", "$1 $2");
            key = key.replaceAll("([^_]+)__([^_]+)", "$1_$2");

            assetMetaData.set(key, attribute.getValue());
        }

        // overwrite asset id
        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, assetId);

        // add asset metadata
        inventory.getAssetMetaData().add(assetMetaData);

        // add marker for artifacts
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.set(assetId, Constants.MARKER_CROSS);
        }

        File targetInventoryFile = getTargetInventoryFile();

        // support in-place modification, when targetInventory file nott set
        if (targetInventoryFile == null) {
            targetInventoryFile = getInventoryFile();
        }

        new InventoryWriter().writeInventory(inventory, targetInventoryFile);
    }

    public GenericAssetInventoryProcessor withAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    public GenericAssetInventoryProcessor writing(File targetInventoryFile) {
        super.writing(targetInventoryFile);
        return this;
    }

    public GenericAssetInventoryProcessor supply(String assetId) {
        this.assetId = assetId;
        return this;
    }
}
