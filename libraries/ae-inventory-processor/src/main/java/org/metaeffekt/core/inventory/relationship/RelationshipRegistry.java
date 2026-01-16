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
package org.metaeffekt.core.inventory.relationship;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The main class of a data model responsible for mapping many-to-many relationships. When mapping inventory relationships
 * this should be the preferred way of doing so, as the registry provides a unified model for how theses relationships
 * are stored and accessed. This registry also provides some rudimentary helper methods and is currently only utilized by
 * the bom conversion module, meaning some design decisions have been made to make sense in this context.
 */
@Slf4j
public class RelationshipRegistry {

    @Setter
    private boolean failOnMissingEntity = false;

    private final List<Relationship<?, ?>> relationships;
    private final Map<String, Relationship<?, ?>> relationshipIndex;
    private final Map<RelationshipType, List<Relationship<?, ?>>> typeIndex;

    public RelationshipRegistry() {
        this.relationships = new ArrayList<>();
        this.relationshipIndex = new HashMap<>();
        this.typeIndex = new EnumMap<>(RelationshipType.class);

        for (RelationshipType type : RelationshipType.values()) {
            typeIndex.put(type, new ArrayList<>());
        }
    }

    public <A, B> boolean addRelationship(Relationship<A, B> relationship) {
        if (relationship == null || relationshipIndex.containsKey(relationship.getId())) {
            return false;
        }

        if (relationship.isRootEntitiesEmpty() ||  relationship.isRelatedEntitiesEmpty()) {
            return false;
        }

        relationships.add(relationship);
        relationshipIndex.put(relationship.getId(), relationship);
        typeIndex.get(relationship.getType()).add(relationship);

        return true;
    }

    public boolean removeRelationship(String relationshipId) {
        Relationship<?, ?> relationship = relationshipIndex.remove(relationshipId);
        if (relationship == null) {
            return false;
        }

        relationships.remove(relationship);
        typeIndex.get(relationship.getType()).remove(relationship);

        return true;
    }

    public boolean removeRelationship(Relationship<?, ?> relationship) {
        if (relationship == null) {
            return false;
        }
        return removeRelationship(relationship.getId());
    }

    public List<Relationship<?, ?>> getCopyOfRelationships() {
        return new ArrayList<>(relationships);
    }

    /**
     * Searches the relationship registry for all relationships in which a specific object is contained.
     *
     * @param object the with which to search for
     * @return a copy of the list of relationships containing the object
     */
    public List<Relationship<?, ?>> getRelationshipsByObject(Object object) {
        List<Relationship<?, ?>> copyOfRelationships = getCopyOfRelationships();
        List<Relationship<?, ?>> applicableRelationships = new ArrayList<>();

        for (Relationship<?, ?> relationship : copyOfRelationships) {
            if (relationship.getRootEntities().stream().map(RelationshipEntity::getEntity).collect(Collectors.toSet()).contains(object)) {
                applicableRelationships.add(relationship);
            }

            if (relationship.getRelatedEntities().stream().map(RelationshipEntity::getEntity).collect(Collectors.toSet()).contains(object)) {
                applicableRelationships.add(relationship);
            }
        }
        return applicableRelationships;
    }

    /**
     * Finalizes all relationships present by consolidating all relationships where possible.
     * All relationships are consolidated based on if they have the same toEntities and type.
     * This means all from-entities with the same type and to-entities will be merged into a single relationship
     * object.
     */
    public void finalizeRelationships() {
        // group all relationships by their toNodes and type
        Map<RelationshipKey, List<Relationship<?, ?>>> groupedByToNodesAndType = relationships.stream()
                .collect(Collectors.groupingBy(rel ->
                        new RelationshipKey(rel.getType(), rel.getRelatedEntities())));

        List<Relationship<?, ?>> result = new ArrayList<>();

        // iterate through all relationship groups with different toNodes and type
        for (Map.Entry<RelationshipKey, List<Relationship<?, ?>>> entry : groupedByToNodesAndType.entrySet()) {
            List<Relationship<?,?>> group = entry.getValue();

            // create a new set containing all fromNodes from different relationships in the same group
            Set<RelationshipEntity<?>> mergedFrom = new HashSet<>();
            for (Relationship<?, ?> relationship : group) {
                mergedFrom.addAll(relationship.getRootEntities());
            }

            Relationship<Object, Object> mergedRelationship = getMergedRelationship(group, mergedFrom);
            result.add(mergedRelationship);
        }

        List<String> idsToRemove = relationships.stream()
                .map(Relationship::getId)
                .collect(Collectors.toList());

        idsToRemove.forEach(this::removeRelationship);
        result.forEach(this::addRelationship);
    }

    /**
     * Creates a new relationship merging all relationships containing the same toNodes and type.
     *
     * @param group a list of relationships which are in the same group (e.g. have the same toNodes and type)
     * @param mergedFrom a list of all fromNodes which are in this group
     * @return a relationship object containing all merged entities
     */
    private static Relationship<Object, Object> getMergedRelationship(List<Relationship<?, ?>> group, Set<RelationshipEntity<?>> mergedFrom) {
        Relationship<?,?> relationshipTemplate = group.get(0);

        Relationship<Object, Object> mergedRelationship = new Relationship<>();
        mergedRelationship.setType(relationshipTemplate.getType());

        for (RelationshipEntity<?> fromEntity : mergedFrom) {
            RelationshipEntity<Object> typedFromEntity = (RelationshipEntity<Object>) fromEntity;
            mergedRelationship.addRootEntity(typedFromEntity);
        }

        for (RelationshipEntity<?> toEntity : relationshipTemplate.getRelatedEntities()) {
            RelationshipEntity<Object> typedToEntity = (RelationshipEntity<Object>) toEntity;
            mergedRelationship.addRelatedEntity(typedToEntity);
        }

        return mergedRelationship;
    }

    /**
     * This method attempts to apply the registered relationships to an inventory. The prerequisites for which are as follows:
     * <ul>
     *     <li>each {@link RelationshipEntity} representative correlated to an artifact or asset id</li>
     * </ul>
     *
     * @param inventory the inventory to which the relationships will be applied
     */
    public void applyToInventory(Inventory inventory) {
        log.info("Applying relationships to inventory. Found {} relationship entries.", relationships.size());

        setMissingRelationshipEntityIdentifiers();
        Map<String, Map<RelationshipType, List<String>>> relatedEntityToRootEntitiesMap = buildReversedRelationshipsMap();

        List<String> assetIds = inventory.getAssetMetaData().stream()
                .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toList());

        // Iterate through all artifacts in the inventory.
        for (Artifact artifact : inventory.getArtifacts()) {
            String artifactId = artifact.get(Artifact.Attribute.ID);

            // Get all related assets and their relationship to the artifact
            Map<RelationshipType, List<String>> artifactRelationships = relatedEntityToRootEntitiesMap.get(artifactId);

            if (artifactRelationships != null) {

                // For every related asset, create a column in the artifacts sheet with that assets id and set the correct marker
                for (Map.Entry<RelationshipType, List<String>> relationshipEntry : artifactRelationships.entrySet()) {
                    RelationshipType relationshipType = relationshipEntry.getKey();
                    String inventoryMarker = relationshipType.getInventoryMarker();

                    for (String assetIdentifier : relationshipEntry.getValue()) {
                        if (assetIds.contains(assetIdentifier)) {
                            artifact.set(assetIdentifier, inventoryMarker);
                        } else {
                            log.warn("Failed to map relationship [{} - {} - {}] because the inventory does not contain " +
                                    "the asset: {}.", assetIdentifier, inventoryMarker, artifactId, assetIdentifier);
                        }
                    }
                }
            }
        }
        /*
        FIXME-JFU: If assets contain other assets, we currently have to represent them in the artifact sheet
         as we dont track relationships anywhere else. Consider relocating to relationships sheet.
         */
    }

    /**
     * Sets the artifact/asset id as an identifier for each {@link RelationshipEntity}, only if no identifer has been set.
     */
    private void setMissingRelationshipEntityIdentifiers() {
        for (Relationship<?,?> relationship : relationships) {
            updateRelationshipEntityIdentifiers(relationship.getRootEntities());
            updateRelationshipEntityIdentifiers(relationship.getRelatedEntities());
        }
    }

    private void updateRelationshipEntityIdentifiers(Set<? extends RelationshipEntity<?>> relationshipEntities) {
        for (RelationshipEntity<?> relationshipEntity : relationshipEntities) {
            if (StringUtils.isNotBlank(relationshipEntity.getIdentifier())) {
                String identifier = null;

                if (relationshipEntity.getEntity() instanceof AssetMetaData) {
                    identifier = ((AssetMetaData) relationshipEntity.getEntity()).get(AssetMetaData.Attribute.ASSET_ID);
                } else if (relationshipEntity.getEntity() instanceof Artifact) {
                    identifier = ((Artifact) relationshipEntity.getEntity()).get(Artifact.Attribute.ID);
                }
                if (identifier != null) {
                    relationshipEntity.setIdentifier(identifier);
                }
            }
        }
    }

    /**
     * This method builds a map from all existing relationships where each key is a relatedEntity and each value a map
     * containing every rootEntity as well as the relationship type.

     * @return a map containing all relatedEntities as keys with the list of originEntities / types as values.
     */
    private Map<String, Map<RelationshipType, List<String>>> buildReversedRelationshipsMap() {
        Map<String, Map<RelationshipType, Set<String>>> intermediateMap = new HashMap<>();

        if (relationships == null) {
            return new HashMap<>();
        }

        for (Relationship<?, ?> relationship : relationships) {
            List<String> rootIdentifiers = relationship.getRootEntityIdentifiers();
            Set<? extends RelationshipEntity<?>> relatedEntities = relationship.getRelatedEntities();
            RelationshipType type = relationship.getType();

            if (rootIdentifiers == null || rootIdentifiers.isEmpty()) {
                continue;
            }

            for (RelationshipEntity<?> relatedEntity : relatedEntities) {
                String relatedIdentifier = relatedEntity.getIdentifier();

                if (relatedIdentifier == null) continue;

                intermediateMap
                        .computeIfAbsent(relatedIdentifier, k -> new HashMap<>())
                        .computeIfAbsent(type, k -> new LinkedHashSet<>())
                        .addAll(rootIdentifiers);
            }
        }

        Map<String, Map<RelationshipType, List<String>>> finalMap = new HashMap<>();

        for (Map.Entry<String, Map<RelationshipType, Set<String>>> entry : intermediateMap.entrySet()) {
            Map<RelationshipType, List<String>> typeMap = new HashMap<>();

            for (Map.Entry<RelationshipType, Set<String>> innerEntry : entry.getValue().entrySet()) {
                typeMap.put(innerEntry.getKey(), new ArrayList<>(innerEntry.getValue()));
            }

            finalMap.put(entry.getKey(), typeMap);
        }

        return finalMap;
    }

    /**
     * Automatically builds a relationship registry from an inventory. Currently this method only tracks
     * relationships between assets and artifacts.
     * @param inventory the inventory from which to build a relationship registry.
     */
    public void buildFromInventory(Inventory inventory) {

        for (AssetMetaData assetMetaData : inventory.getAssetMetaData()) {
            for (Artifact artifact : inventory.getArtifacts()) {

                String relationshipMarker = artifact.get(assetMetaData.get(AssetMetaData.Attribute.ASSET_ID));
                RelationshipType relationshipType = RelationshipType.fromInventoryConstant(relationshipMarker);

                if (relationshipMarker != null && relationshipType == null) {
                    log.warn("The inventory [{}] contains an artifact [{}] with an unknown relationship marker [{}]. " +
                            "The affected artifact will not have it's relationships tracked unless the marker is fixed.",
                            inventory, artifact.get(Artifact.Attribute.ID), relationshipMarker);
                    continue;
                }

                if (relationshipType != null && !relationshipType.equals(RelationshipType.DESCRIBES)) {
                    Relationship<AssetMetaData, Artifact> relationship = new Relationship<>();
                    relationship.addRootEntity(new RelationshipEntity<>(assetMetaData, InventoryObjectIdentifier.createIdentifier(assetMetaData)));
                    relationship.addRelatedEntity(artifact,  InventoryObjectIdentifier.createIdentifier(artifact));
                    relationship.setType(relationshipType);
                    addRelationship(relationship);

                } else if (relationshipType != null) {
                    Relationship<Artifact, AssetMetaData> relationship = new Relationship<>();
                    relationship.addRelatedEntity(new RelationshipEntity<>(assetMetaData, InventoryObjectIdentifier.createIdentifier(assetMetaData)));
                    relationship.addRootEntity(artifact,  InventoryObjectIdentifier.createIdentifier(artifact));
                    relationship.setType(relationshipType);
                    addRelationship(relationship);
                }
            }
        }

        finalizeRelationships();
    }

    /**
     * Helper class to correctly consolidate relationships by their toNodes and type.
     */
    private static class RelationshipKey {
        private final RelationshipType type;
        private final Set<?> toEntities;

        public RelationshipKey(RelationshipType type, Set<?> toEntities) {
            this.type = type;
            this.toEntities = new HashSet<>(toEntities);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelationshipKey that = (RelationshipKey) o;
            return Objects.equals(type, that.type) &&
                    Objects.equals(toEntities, that.toEntities);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, toEntities);
        }

        @Override
        public String toString() {
            return "(" + type + ", " + toEntities + ")";
        }
    }
}
