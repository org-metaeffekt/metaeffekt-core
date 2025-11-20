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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
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

        if (relationship.fromEntitiesEmpty() ||  relationship.toEntitiesEmpty()) {
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
            if (relationship.getFromEntities().stream().map(RelationshipEntity::getEntity).collect(Collectors.toSet()).contains(object)) {
                applicableRelationships.add(relationship);
            }

            if (relationship.getToEntities().stream().map(RelationshipEntity::getEntity).collect(Collectors.toSet()).contains(object)) {
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
                        new RelationshipKey(rel.getType(), rel.getToEntities())));

        List<Relationship<?, ?>> result = new ArrayList<>();

        // iterate through all relationship groups with different toNodes and type
        for (Map.Entry<RelationshipKey, List<Relationship<?, ?>>> entry : groupedByToNodesAndType.entrySet()) {
            List<Relationship<?,?>> group = entry.getValue();

            // create a new set containing all fromNodes from different relationships in the same group
            Set<RelationshipEntity<?>> mergedFrom = new HashSet<>();
            for (Relationship<?, ?> relationship : group) {
                mergedFrom.addAll(relationship.getFromEntities());
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
            mergedRelationship.addFrom(typedFromEntity);
        }

        for (RelationshipEntity<?> toEntity : relationshipTemplate.getToEntities()) {
            RelationshipEntity<Object> typedToEntity = (RelationshipEntity<Object>) toEntity;
            mergedRelationship.addTo(typedToEntity);
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

        fillEntityRepresentatives();
        Map<String, Map<RelationshipType, List<String>>> toEntityMap = buildToEntityRepresentativeMap();
        log.info("Mapped {} relationships to {} Artifact -> Asset relationships.", relationships.size(), toEntityMap.size());

        List<String> assetIds = inventory.getAssetMetaData().stream()
                .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toList());

        for (Artifact artifact : inventory.getArtifacts()) {
            String artifactId = artifact.get(Artifact.Attribute.ID);

            Map<RelationshipType, List<String>> relationships = toEntityMap.get(artifactId);
            if (relationships != null) {
                for (Map.Entry<RelationshipType, List<String>> entry : relationships.entrySet()) {
                    RelationshipType type = entry.getKey();
                    String inventoryConstant = type.getInventoryConstant();

                    for (String assetRepresentative : entry.getValue()) {
                        if (assetIds.contains(assetRepresentative)) {
                            artifact.set(assetRepresentative, inventoryConstant);
                        } else {
                            log.warn("Failed to map relationship [{} - {} - {}] because the inventory does not contain " +
                                    "the asset: {}.", assetRepresentative, inventoryConstant, artifactId, assetRepresentative);
                        }
                    }
                }
            }
        }
        /*
        FIXME JFU: If assets contain other assets, we currently have to represent them in the artifact sheet
         as we dont track relationships anywhere else. Consider relocating to relationships sheet.
         */
    }

    /**
     * Fills the representative String of each {@link RelationshipEntity} with the artifact or asset id if the entity
     * is an instance of either of these object and has no set representative.
     */
    private void fillEntityRepresentatives() {
        for (Relationship<?,?> relationship : relationships) {
            updateRepresentatives(relationship.getFromEntities());
            updateRepresentatives(relationship.getToEntities());
        }
    }

    private void updateRepresentatives(Set<? extends RelationshipEntity<?>> relationshipEntities) {
        for (RelationshipEntity<?> relationshipEntity : relationshipEntities) {
            if (StringUtils.isNotBlank(relationshipEntity.getRepresentative())) {
                String representative = null;

                if (relationshipEntity.getEntity() instanceof AssetMetaData) {
                    representative = ((AssetMetaData) relationshipEntity.getEntity()).get(AssetMetaData.Attribute.ASSET_ID);
                } else if (relationshipEntity.getEntity() instanceof Artifact) {
                    representative = ((Artifact) relationshipEntity.getEntity()).get(Artifact.Attribute.ID);
                }
                if (representative != null) {
                    relationshipEntity.setRepresentative(representative);
                }
            }
        }
    }

    private Map<String, Map<RelationshipType, List<String>>> buildToEntityRepresentativeMap() {
        Map<String, Map<RelationshipType, List<String>>> map = new HashMap<>();

        for (Relationship<?, ?> relationship : relationships) {
            List<String> fromRepresentatives = relationship.getFromRepresentatives();
            RelationshipType relationshipType = relationship.getType();

            for (RelationshipEntity<?> toEntity : relationship.getToEntities()) {
                String toRepresentative = toEntity.getRepresentative();
                Map<RelationshipType, List<String>> innerMap = map.computeIfAbsent(toRepresentative, k -> new HashMap<>());
                List<String> representativesList = innerMap.computeIfAbsent(relationshipType, k -> new ArrayList<>());
                representativesList.addAll(fromRepresentatives);
            }
        }

        return map;
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
                    relationship.addFrom(new RelationshipEntity<>(assetMetaData, InventoryObjectIdentifier.createIdentifier(assetMetaData)));
                    relationship.addTo(artifact,  InventoryObjectIdentifier.createIdentifier(artifact));
                    relationship.setType(relationshipType);
                    addRelationship(relationship);

                } else if (relationshipType != null) {
                    Relationship<Artifact, AssetMetaData> relationship = new Relationship<>();
                    relationship.addTo(new RelationshipEntity<>(assetMetaData, InventoryObjectIdentifier.createIdentifier(assetMetaData)));
                    relationship.addFrom(artifact,  InventoryObjectIdentifier.createIdentifier(artifact));
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
