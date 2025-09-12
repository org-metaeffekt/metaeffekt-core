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
package org.metaeffekt.core.inventory.relationship;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements a graph structure which holds an indefinite amount of nodes and edges. The graph maps inventory
 * artifacts and their relationships to one another and is meant to be used in conjunction with the SPDX standard.
 */
@Slf4j
public class RelationshipGraph {

    public enum RelationshipType {
        CONTAINS,
        DESCRIBES,
        CONTAINS_DEV_DEPENDENCY
    }

    /**
     * Used as an identifier for the document itself during relationship hierarchy tracking.
     */
    public static final String DOCUMENT = "DOCUMENT";

    @Getter
    private final Map<String, RelationshipGraphNode> nodes;
    private final List<RelationshipGraphEdge> relationships;

    public RelationshipGraph(Inventory inventory) {
        this.nodes = new HashMap<>();
        this.relationships = new ArrayList<>();
        mapRelationships(inventory, createAssetToArtifactMap(inventory));
    }

    public void addRelationship(String fromId, String toId, RelationshipType relationshipType) {
        RelationshipGraphNode fromNode = nodes.get(fromId);
        RelationshipGraphNode toNode = nodes.get(toId);
        boolean edgeExists = false;

        for (RelationshipGraphEdge edge : relationships) {
            if (edge.getFromNode().getId().equals(fromId) && edge.getRelationshipType().equals(relationshipType)) {
                edge.addToNode(toNode);
                edgeExists = true;
            }
        }

        if (!edgeExists) {
            List<RelationshipGraphNode> toNodes = new ArrayList<>();
            toNodes.add(toNode);
            relationships.add(new RelationshipGraphEdge(fromNode, toNodes, relationshipType));
        }
    }

    public List<RelationshipGraphEdge> getAllRelationships() {
        return relationships;
    }

    private void mapRelationships(Inventory inventory, Map<AssetMetaData, Artifact> assetToArtifactMap) {
        List<AssetMetaData> assetMetaDataList = inventory.getAssetMetaData();
        List<Artifact> artifactList = inventory.getArtifacts();

        addNodes(artifactList, assetMetaDataList, assetToArtifactMap);
        mapEdgeCases(artifactList, assetMetaDataList, assetToArtifactMap);
        mapMainRelationships(artifactList ,assetMetaDataList, assetToArtifactMap);
    }


    /**
     * Processes all artifacts to map out DESCRIBES, CONTAINS, and DEVELOPMENT relationships.
     *
     * <p>This method iterates through each artifact and checks whether it contains a relationship marker
     * corresponding to each asset from the provided asset metadata list. Depending on the marker found,
     * it adds one of the following relationships:
     * <ul>
     *   <li><b>DESCRIBES:</b> Always added from the document.</li>
     *   <li><b>CONTAINS:</b> Added from the asset if an associated artifact exists in the asset-to-artifact map;
     *       otherwise, uses the asset ID directly.</li>
     *   <li><b>DEVELOPMENT:</b> Handled similarly to CONTAINS but with a DEVELOPMENT relationship type.</li>
     * </ul>
     * </p>
     *
     * @param artifactList       The list of artifacts to process.
     * @param assetMetaDataList  The list of asset metadata containing asset identifiers and related attributes.
     * @param assetToArtifactMap A map linking asset metadata to their corresponding artifacts used to determine the source for CONTAINS and DEVELOPMENT relationships.
     */

    private void mapMainRelationships(List<Artifact> artifactList, List<AssetMetaData> assetMetaDataList, Map<AssetMetaData, Artifact> assetToArtifactMap) {
        for (AssetMetaData assetMetaData : assetMetaDataList) {
            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
            for (Artifact artifact : artifactList) {
                String relationshipMarker = artifact.get(assetId);
                if (StringUtils.isBlank(relationshipMarker)) {
                    continue;
                }

                if (Constants.MARKER_CROSS.equals(relationshipMarker)) {
                    addRelationship(DOCUMENT, artifact.getId(), RelationshipType.DESCRIBES);
                } else if (Constants.MARKER_CONTAINS.equals(relationshipMarker) || Constants.MARKER_DEVELOPMENT_DEPENDENCY.equals(relationshipMarker)) {
                    RelationshipType relationshipType =
                            Constants.MARKER_CONTAINS.equals(relationshipMarker)
                                    ? RelationshipType.CONTAINS
                                    : RelationshipType.CONTAINS_DEV_DEPENDENCY;

                    String fromNodeId = assetToArtifactMap.containsKey(assetMetaData)
                            ? assetToArtifactMap.get(assetMetaData).getId()
                            : assetId;

                    addRelationship(fromNodeId, artifact.getId(), relationshipType);
                }
            }
        }
    }

    /**
     * Adds all artifacts, assets and the document itself as nodes in the RelationshipHierarchyGraph.
     * @param artifactList A list of all artifacts contained in the inventory.
     * @param assetMetaDataList A list of all the assets contained in the inventory.
     * @param assetToArtifactMap A map containing asset -> artifact pairs.
     */
    private void addNodes(List<Artifact> artifactList, List<AssetMetaData> assetMetaDataList, Map<AssetMetaData, Artifact> assetToArtifactMap) {
        RelationshipGraphNode documentNode = new RelationshipGraphNode(DOCUMENT);
        nodes.put(documentNode.getId(), documentNode);

        for (Artifact artifact : artifactList) {
            RelationshipGraphNode artifactNode = new RelationshipGraphNode(artifact.getId());
            nodes.put(artifactNode.getId(), artifactNode);
        }

        for (AssetMetaData assetMetaData : assetMetaDataList) {
            if (!assetToArtifactMap.containsKey(assetMetaData)) {
                RelationshipGraphNode assetNode = new RelationshipGraphNode(assetMetaData.get(AssetMetaData.Attribute.ASSET_ID));
                nodes.put(assetNode.getId(), assetNode);
            }
        }
    }


    /**
     * Maps edge cases which when true, lead to an early return making the main relationship mapping loop redundant.
     * @param artifactList A list of all artifacts contained in the inventory.
     * @param assetMetaDataList A list of all the assets contained in the inventory.
     * @param assetToArtifactMap A map containing asset -> artifact pairs.
     */
    private void mapEdgeCases(List<Artifact> artifactList, List<AssetMetaData> assetMetaDataList, Map<AssetMetaData, Artifact> assetToArtifactMap) {
        // if no assets exist fallback to adding all artifacts
        if (assetMetaDataList.isEmpty()) {
            for (Artifact artifact : artifactList) {
                addRelationship("DOCUMENT", artifact.getId(), RelationshipType.DESCRIBES);
            }
            return;
        }

        // if there is no relationship hierarchy at all, add DOCUMENT DESCRIBES relationships for all assets.
        if (assetToArtifactMap.isEmpty()) {
            for (AssetMetaData assetMetaData : assetMetaDataList) {
                addRelationship("DOCUMENT", assetMetaData.get(Constants.KEY_ASSET_ID), RelationshipType.DESCRIBES);
            }
        }
    }

    private static Map<AssetMetaData, Artifact> createAssetToArtifactMap(Inventory inventory) {
        final Map<AssetMetaData, Artifact> assetToArtifactMap = new HashMap<>();
        final List<Artifact> artifacts = inventory.getArtifacts();
        final List<AssetMetaData> assetMetaDataList = inventory.getAssetMetaData();

        // FIXME-KKL: in case relationship between artifact and asset is not distinct, we should raise an issue
        for (AssetMetaData assetMetaData : assetMetaDataList) {
            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
            for (Artifact artifact : artifacts) {
                final String s = artifact.get(assetId);
                if (s != null && s.equals(Constants.MARKER_CROSS)) {
                    assetToArtifactMap.put(assetMetaData, artifact);
                }
            }
        }
        return assetToArtifactMap;
    }

}
