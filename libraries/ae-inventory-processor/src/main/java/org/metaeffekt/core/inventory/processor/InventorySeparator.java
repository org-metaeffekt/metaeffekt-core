package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.relationship.RelationshipGraph;
import org.metaeffekt.core.inventory.relationship.RelationshipGraphEdge;
import org.metaeffekt.core.inventory.relationship.RelationshipGraphNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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

        return splitInventory(inventory);
    }

    private static void validateInventory(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory to separate cannot be null");
        }

        if (!allArtifactsContainedInPrimaries(inventory)) {
            throw new IllegalArgumentException("Not all artifacts in inventory are contained in primary assets: " + inventory);
        }
    }

    private static List<Inventory> splitInventory(Inventory inventory) {
        List<String> primaryAssetIds = getPrimaryAssetIds(inventory);
        RelationshipGraph relationshipGraph = new RelationshipGraph(inventory);

        List<RelationshipGraphEdge> relevantRelationships =
                relationshipGraph.getAllRelationships()
                        .stream()
                        .filter(relationship -> primaryAssetIds.contains(relationship.getFromNode().getId()))
                        .collect(Collectors.toList());

        return relevantRelationships.stream()
                .map(edge -> createSeparateInventory(inventory, edge))
                .collect(Collectors.toList());
    }

    private static Inventory createSeparateInventory(Inventory originalInventory, RelationshipGraphEdge relationshipGraphEdge) {
        Inventory separateInventory = new Inventory(originalInventory);
        removeIrrelevantAssets(separateInventory, relationshipGraphEdge);
        removeIrrelevantArtifacts(separateInventory, relationshipGraphEdge);
        return separateInventory;
    }

    private static void removeIrrelevantAssets(Inventory inventory, RelationshipGraphEdge relationshipGraphEdge) {
        List<String> allOriginalAssetIds = inventory.getAssetMetaData()
                .stream()
                .map(asset -> asset.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toList());

        String targetAssetId = relationshipGraphEdge.getFromNode().getId();

        inventory.getAssetMetaData().removeIf(assetMetaData ->!assetMetaData.get(AssetMetaData.Attribute.ASSET_ID).equals(targetAssetId));
        removeDanglingHierarchyEntries(inventory, allOriginalAssetIds);
    }

    private static void removeIrrelevantArtifacts(Inventory inventory, RelationshipGraphEdge relationshipGraphEdge) {
        Set<String> containedArtifactIds = relationshipGraphEdge.getToNodes()
                .stream()
                .map(RelationshipGraphNode::getId)
                .collect(Collectors.toSet());

        inventory.getArtifacts().removeIf(artifact -> !containedArtifactIds.contains(artifact.getId()));
    }

    private static void removeDanglingHierarchyEntries(Inventory inventory, List<String> allOriginalAssetIds) {
        Set<String> currentAssetIds = inventory.getAssetMetaData()
                .stream()
                .map(asset -> asset.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toSet());

        Set<String> removedAssetIds = allOriginalAssetIds.stream()
                .filter(assetId -> !currentAssetIds.contains(assetId))
                .collect(Collectors.toSet());

        for (Artifact artifact : inventory.getArtifacts()) {
            for (String assetId : removedAssetIds) {
                if (StringUtils.isNotBlank(artifact.get(assetId))) {
                    artifact.set(assetId, null);
                }
            }
        }
    }

    private static boolean allArtifactsContainedInPrimaries(Inventory inventory) {
        List<String> primaryAssetIds = getPrimaryAssetIds(inventory);

        return inventory.getArtifacts()
                .stream()
                .allMatch(artifact -> primaryAssetIds
                        .stream()
                        .anyMatch(primaryAssetId -> StringUtils.isNotBlank(artifact.get(primaryAssetId))));
    }

    private static boolean hasOnlyOnePrimary(Inventory inventory) {
        return inventory.getAssetMetaData()
                .stream()
                .filter(AssetMetaData::isPrimary)
                .count() == 1;
    }

    private static List<String> getPrimaryAssetIds(Inventory inventory) {
        return inventory.getAssetMetaData()
                .stream()
                .filter(AssetMetaData::isPrimary)
                .map(asset -> asset.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toList());
    }
}

