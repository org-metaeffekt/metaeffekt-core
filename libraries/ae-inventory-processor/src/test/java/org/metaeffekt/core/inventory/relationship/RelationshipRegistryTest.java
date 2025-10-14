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


import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class RelationshipRegistryTest {

    @Test
    public void testAddAndRemoveRelationship() {
        RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        Relationship<String, String> relationship = new Relationship<>(
                RelationshipType.CONTAINS,
                new RelationshipEntity<>("fromEntity", "fromEntity"),
                new RelationshipEntity<>("toEntity", "toEntity"));

        relationshipRegistry.addRelationship(relationship);

        assertThat(relationshipRegistry.getCopyOfRelationships().size()).isEqualTo(1);
        assertThat(relationshipRegistry.getCopyOfRelationships().get(0).getFromRepresentatives()).contains("fromEntity");
        assertThat(relationshipRegistry.getCopyOfRelationships().get(0).getToRepresentatives()).contains("toEntity");
        assertThat(relationshipRegistry.removeRelationship(relationship)).isTrue();
        assertThat(relationshipRegistry.getCopyOfRelationships().size()).isEqualTo(0);
    }


    @Test
    public void testMergeRelationships() {
        RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        Relationship<String, String> relationship = buildRelationship(RelationshipType.CONTAINS, "a", "b");
        Relationship<String, String> relationship2 = buildRelationship(RelationshipType.CONTAINS, "c", "b");

        relationshipRegistry.addRelationship(relationship);
        relationshipRegistry.addRelationship(relationship2);

        relationshipRegistry.finalizeRelationships();

        List<Relationship<?, ?>> relationships = relationshipRegistry.getCopyOfRelationships();

        assertThat(relationships.size()).isEqualTo(1);
        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Arrays.asList("a", "c"), Collections.singletonList("b"))).isTrue();
    }

    @Test
    public void testNoMergeRelationshipsWithDifferentType() {
        RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        Relationship<String, String> relationship = buildRelationship(RelationshipType.CONTAINS, "a", "b");
        Relationship<String, String> relationship2 = buildRelationship(RelationshipType.DESCRIBES, "c", "b");

        relationshipRegistry.addRelationship(relationship);
        relationshipRegistry.addRelationship(relationship2);

        relationshipRegistry.finalizeRelationships();

        List<Relationship<?, ?>> relationships = relationshipRegistry.getCopyOfRelationships();

        assertThat(relationships.size()).isEqualTo(2);

        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Collections.singletonList("a"), Collections.singletonList("b"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.DESCRIBES,
                Collections.singletonList("c"), Collections.singletonList("b"))).isTrue();
    }

    @Test
    public void testNoMergeRelationshipsWithDifferentToNodes() {
        RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        Relationship<String, String> relationship = buildRelationship(RelationshipType.CONTAINS, "a", "b");
        Relationship<String, String> relationship2 = buildRelationship(RelationshipType.CONTAINS, "c", "b");
        relationship2.addTo(new RelationshipEntity<>("d", "d"));

        relationshipRegistry.addRelationship(relationship);
        relationshipRegistry.addRelationship(relationship2);

        relationshipRegistry.finalizeRelationships();

        List<Relationship<?, ?>> relationships = relationshipRegistry.getCopyOfRelationships();

        assertThat(relationships.size()).isEqualTo(2);

        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Collections.singletonList("a"), Collections.singletonList("b"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Collections.singletonList("c"), Arrays.asList("b", "d"))).isTrue();
    }


    @Test
    public void testBuildFromInventory() throws IOException {
        File inventoryFile = new File("src/test/resources/relationships/testBuildFromInventory.xlsx");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        RelationshipRegistry relationshipRegistry = new RelationshipRegistry();
        relationshipRegistry.buildFromInventory(inventory);

        List<Relationship<?, ?>> relationships = relationshipRegistry.getCopyOfRelationships();

        assertThat(relationships.size()).isEqualTo(5);


        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Stream.of("AID-asset-1", "AID-asset-2").collect(Collectors.toList()),
                Collections.singletonList("artifact-1"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Collections.singletonList("AID-asset-1"), Collections.singletonList("artifact-2"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.HAS_RUNTIME_DEPENDENCY,
                Collections.singletonList("AID-asset-2"), Collections.singletonList("artifact-2"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.CONTAINS,
                Collections.singletonList("AID-asset-2"), Collections.singletonList("artifact-3"))).isTrue();

        assertThat(hasRelationship(relationships, RelationshipType.DESCRIBES,
                Collections.singletonList("artifact-4"), Collections.singletonList("AID-asset-2"))).isTrue();

    }

    private static boolean hasRelationship(List<Relationship<?, ?>> relationships, RelationshipType relationshipType,
                                           List<String> fromNodes, List<String> toNodes) {

        Optional<Relationship<?, ?>> relationshipOptional = relationships.stream()
                .filter(r -> r.getType().equals(relationshipType)
                        && (r.getToRepresentatives().containsAll(toNodes) && toNodes.containsAll(r.getToRepresentatives()))
                        && (r.getFromRepresentatives().containsAll(fromNodes) && fromNodes.containsAll(r.getFromRepresentatives())))
                .findFirst();

        return relationshipOptional.isPresent();
    }

    private static Relationship<String, String> buildRelationship(RelationshipType type, String from, String to) {
        return new Relationship<>(
                type,
                new RelationshipEntity<>(from, from),
                new RelationshipEntity<>(to, to));
    }
}
