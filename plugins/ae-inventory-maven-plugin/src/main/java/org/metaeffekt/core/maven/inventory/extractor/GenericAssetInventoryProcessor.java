/*
 * Copyright 2009-2026 the original author or authors.
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

import org.apache.commons.lang3.ObjectUtils;
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

    private boolean pickSingleAsset = false;

    public GenericAssetInventoryProcessor augmenting(File inventoryFile) {
        super.augmenting(inventoryFile);
        return this;
    }

    private AssetMetaData findOrCreateAsset(Inventory inventory) {
        for (AssetMetaData asset : inventory.getAssetMetaData()) {
            if (assetId.equals(asset.get(AssetMetaData.Attribute.ASSET_ID))) {
                return asset;
            }
        }

        // create and add new asset metadata
        final AssetMetaData asset = new AssetMetaData();
        asset.set(AssetMetaData.Attribute.ASSET_ID, assetId);
        inventory.getAssetMetaData().add(asset);
        return asset;
    }

    public void process() throws IOException {
        if (assetId != null && pickSingleAsset) {
            throw new IllegalArgumentException("Either supply an asset id (assetId) or choose to pick a single asset (pickSingleAsset), but not both are allowed at the same time.");
        } else if (assetId == null && !pickSingleAsset) {
            throw new IllegalArgumentException("Either supply an asset id (assetId) or choose to pick a single asset (pickSingleAsset).");
        }

        final Inventory inventory = new InventoryReader().readInventory(getInventoryFile());

        // find asset or create new with id
        final AssetMetaData assetMetaData;
        if (assetId != null) {
            assetMetaData = findOrCreateAsset(inventory);
        } else if (pickSingleAsset) {
            if (inventory.getAssetMetaData().size() != 1) {
                throw new IllegalStateException("pickSingleAsset: Expected exactly one asset in the inventory, but found " + inventory.getAssetMetaData().size() + " assets.");
            }
            assetMetaData = inventory.getAssetMetaData().get(0);
        } else {
            throw new IllegalArgumentException("Either supply an asset id (assetId) or choose to pick a single asset (pickSingleAsset).");
        }

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            // handle escaping (_ are spaces, __ are _)
            final String key = unescapeKey(attribute.getKey());
            assetMetaData.set(key, attribute.getValue());
        }

        // add marker for artifacts
        final String effectiveAssetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.set(effectiveAssetId, Constants.MARKER_CONTAINS);
        }

        // support in-place modification, when targetInventory file not set, use the input inventory file
        final File targetInventoryFile = ObjectUtils.firstNonNull(getTargetInventoryFile(), getInventoryFile());
        if (targetInventoryFile == null) {
            throw new IllegalArgumentException("Either the source or target inventory must be set");
        }

        if (!targetInventoryFile.getParentFile().exists()) {
            targetInventoryFile.getParentFile().mkdirs();
        }
        new InventoryWriter().writeInventory(inventory, targetInventoryFile);
    }

    public static String unescapeKey(String key) {
        return key
                .replaceAll("([^_]+)_(?=[^_])", "$1 ")
                .replaceAll("([^_]+)__(?=[^_])", "$1_");
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

    public GenericAssetInventoryProcessor pickSingleAsset(boolean pickSingleAsset) {
        this.pickSingleAsset = pickSingleAsset;
        return this;
    }
}
