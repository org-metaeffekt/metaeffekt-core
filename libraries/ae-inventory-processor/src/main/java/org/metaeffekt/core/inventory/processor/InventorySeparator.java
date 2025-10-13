/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.relationship.*;

import java.util.*;
import java.util.stream.Collectors;


public class InventorySeparator {

    private InventorySeparator() {
        // Utility class - prevent instantiation
    }

    /**
     * Separates an input inventory into multiple inventories for each primary asset.
     * If only one primary asset exists in the original inventory, returns a list
     * containing the original inventory if all listed artifacts are contained in that asset.
     *
     * @param inventory inventory to split
     * @return a list of inventories split by primary asset
     * @throws IllegalArgumentException if inventory is null or not all artifacts
     *                                  are contained in primary assets
     */
    public static List<Inventory> separate(Inventory inventory) {
        validateInventory(inventory);

        if (hasOnlyOnePrimary(inventory)) {
            return Collections.singletonList(inventory);
        }

        final List<Inventory> inventories = splitInventory(inventory);

        validateArtifactsCoveredByInventories(inventory, inventories);

        return inventories;
    }

    /**
     * Validates that all artifacts have been distributed. No artifact has been left out.
     *
     * @param inventory The inventory with the original artifacts.
     * @param inventories The inventories that have been split from inventory.
     */
    private static void validateArtifactsCoveredByInventories(Inventory inventory, List<Inventory> inventories) {
        final Set<String> remainingArtifacts = new HashSet<>();

        inventory.getArtifacts().forEach(a -> remainingArtifacts.add(a.deriveQualifier()));
        inventories.forEach(i -> i.getArtifacts().forEach(a -> remainingArtifacts.remove(a.deriveQualifier())));

        if (!remainingArtifacts.isEmpty()) {
            throw new IllegalStateException("Found " + remainingArtifacts.size() + " remaining artifacts after split. Please check inventory integrity: " + remainingArtifacts);
        }
    }

    private static void validateInventory(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory to separate cannot be null");
        }

        final List<String> unmatchedQualifiers = collectArtifactQualifiersWithoutPrimaryAsset(inventory);
        if (!unmatchedQualifiers.isEmpty()) {
            throw new IllegalArgumentException("Not all artifacts in inventory are contained in primary assets: " + unmatchedQualifiers);
        }
    }

    /**
     * Splits an inventory into multiple inventories by its primary assets
     *
     * @param inventory the initial input inventory
     * @return a list of inventories split by primary assets
     */
    private static List<Inventory> splitInventory(Inventory inventory) {
        List<Inventory> resultingInventories = new ArrayList<>();
        final List<AssetMetaData> primaryAssets = inventory.getAssetMetaData()
                .stream().
                filter(AssetMetaData::isPrimary)
                .collect(Collectors.toList());

        final RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        relationshipRegistry.buildFromInventory(inventory);

        for (AssetMetaData primaryAsset : primaryAssets) {
            Inventory splitInventory = new Inventory();
            splitInventory.getAssetMetaData().add(primaryAsset);

            for (Relationship<?, ?> relationship : relationshipRegistry.getRelationshipsByObject(primaryAsset)) {
                if (!relationship.getType().equals(RelationshipType.DESCRIBES)) {
                    for (RelationshipEntity<?> relationshipEntity : relationship.getToEntities()) {
                        if (relationshipEntity.getEntity() instanceof Artifact) {
                            Artifact artifactCopy = new Artifact((Artifact) relationshipEntity.getEntity());
                            splitInventory.getArtifacts().add(artifactCopy);
                        }
                    }
                }
            }

            removeDanglingAssetEntries(splitInventory, primaryAsset, inventory.getAssetMetaData());
            resultingInventories.add(splitInventory);
        }
        return resultingInventories;
    }

    private static void removeDanglingAssetEntries(Inventory inventory, AssetMetaData primaryAsset, List<AssetMetaData> allAssets) {
        String primaryAssetId = primaryAsset.get(AssetMetaData.Attribute.ASSET_ID);


        List<String> assetIds = allAssets.stream()
                .map(assetMetaData ->  assetMetaData.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toList());

        List<String> attributesToRemove = inventory.getArtifacts().stream()
                .flatMap(artifact -> artifact.getAttributes().stream())
                .distinct()
                .filter(attribute -> !attribute.equals(primaryAssetId) && assetIds.contains(attribute))
                .collect(Collectors.toList());

       for (Artifact artifact : inventory.getArtifacts()) {
           attributesToRemove.forEach(artifact.getAttributes()::remove);
       }

       inventory.getArtifacts().removeIf(artifact -> artifact.get(primaryAssetId) == null
                || Objects.equals(artifact.get(primaryAssetId), Constants.MARKER_CROSS));
    }

    private static List<String> collectArtifactQualifiersWithoutPrimaryAsset(Inventory inventory) {
        final Set<String> primaryAssetIds = getPrimaryAssetIds(inventory);
        final List<String> unmatchedArtifactQualifier = new ArrayList<>();

        // NOTE: this requires that the dependencies are fully provided; no evaluation of transitivity

        // NOTE: the current implementation checks for any marker
        for (Artifact artifact : inventory.getArtifacts()) {
            boolean matchedPrimaryAsset = primaryAssetIds.stream()
                    .anyMatch(assetId -> StringUtils.isNotBlank(artifact.get(assetId)));
            if (!matchedPrimaryAsset) {
                unmatchedArtifactQualifier.add(artifact.deriveQualifier());
            }
        }

        return unmatchedArtifactQualifier;
    }

    private static boolean hasOnlyOnePrimary(Inventory inventory) {
        return inventory.getAssetMetaData()
                .stream()
                .filter(AssetMetaData::isPrimary)
                .count() == 1;
    }

    private static Set<String> getPrimaryAssetIds(Inventory inventory) {
        return inventory.getAssetMetaData()
                .stream()
                .filter(AssetMetaData::isPrimary)
                .map(asset -> asset.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toSet());
    }
}

