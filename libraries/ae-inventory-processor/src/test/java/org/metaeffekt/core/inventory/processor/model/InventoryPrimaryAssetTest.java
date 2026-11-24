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
package org.metaeffekt.core.inventory.processor.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class InventoryPrimaryAssetTest {

    @Test
    public void getPrimaryAssetEmptyInventoryTest() {
        final Inventory inventory = new Inventory();

        Assertions.assertTrue(inventory.getPrimaryAsset(null).isEmpty());
        Assertions.assertTrue(inventory.getPrimaryAsset("  ").isEmpty());
        Assertions.assertTrue(inventory.getPrimaryAsset("a").isEmpty());
    }

    @Test
    public void getPrimaryAssetWithoutPrimaryMarkerTest() {
        final AssetMetaData a = createAsset("a", false);
        final AssetMetaData b = createAsset("b", false);

        assertSelected(a, createInventory(null, a).getPrimaryAsset(null));
        Assertions.assertTrue(createInventory(null, a, b).getPrimaryAsset(null).isEmpty());
    }

    @Test
    public void getPrimaryAssetWithPrimaryMarkerTest() {
        final AssetMetaData a = createAsset("a", false);
        final AssetMetaData b = createAsset("b", true);
        final AssetMetaData c = createAsset("c", true);

        final Inventory uniquePrimary = createInventory(a, b);
        assertSelected(b, uniquePrimary.getPrimaryAsset(null));
        assertSelected(b, uniquePrimary.getPrimaryAsset("  "));

        final Inventory multiplePrimaries = createInventory(a, b, c);
        Assertions.assertTrue(multiplePrimaries.getPrimaryAsset(null).isEmpty());
        Assertions.assertTrue(multiplePrimaries.getPrimaryAsset("  ").isEmpty());
    }

    @Test
    public void getPrimaryAssetByIdTest() {
        final AssetMetaData a = createAsset("a", false);
        final AssetMetaData b = createAsset("b", true);
        final AssetMetaData c = createAsset("c", true);
        final Inventory inventory = createInventory(a, b, c);

        assertSelected(a, inventory.getPrimaryAsset("a"));
        Assertions.assertTrue(inventory.getPrimaryAsset("unknown").isEmpty());

        final Inventory duplicateIds = createInventory(a, createAsset("a", false));
        Assertions.assertTrue(duplicateIds.getPrimaryAsset("a").isEmpty());
    }

    @Test
    public void getFirstPrimaryAssetWhenAmbiguousTest() {
        final AssetMetaData a = createAsset("a", false);
        final AssetMetaData b = createAsset("b", true);
        final AssetMetaData c = createAsset("c", true);

        assertSelected(a, createInventory(null, a, createAsset("d", false)).getFirstPrimaryAsset(null));
        assertSelected(b, createInventory(a, b, c).getFirstPrimaryAsset(null));
        assertSelected(a, createInventory(a, createAsset("a", false)).getFirstPrimaryAsset("a"));
        Assertions.assertTrue(createInventory(a, b, c).getFirstPrimaryAsset("unknown").isEmpty());
    }

    private static AssetMetaData createAsset(String assetId, boolean primary) {
        final AssetMetaData asset = new AssetMetaData();
        asset.set(AssetMetaData.Attribute.ASSET_ID, assetId);
        if (primary) {
            asset.set(Constants.KEY_PRIMARY, Constants.MARKER_CROSS);
        }
        return asset;
    }

    private static Inventory createInventory(AssetMetaData... assets) {
        final Inventory inventory = new Inventory();
        for (AssetMetaData asset : assets) {
            inventory.getAssetMetaData().add(asset);
        }
        return inventory;
    }

    private static void assertSelected(AssetMetaData expected, Optional<AssetMetaData> actual) {
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertSame(expected, actual.get());
    }
}
