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

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A single many-to-many relationship containing a set of from-entities, to-entities as well as a single {@link RelationshipType}.
 * All from-entities and all to-entities have to be of the same type.
 * @param <A> class type of all from-entities      .
 * @param <B> class type of all to-entites.
 */
public class Relationship<A, B> {

    @Getter
    private final String id;

    @Getter
    @Setter
    private RelationshipType type;

    private final Set<RelationshipEntity<A>> rootEntities;
    private final Set<RelationshipEntity<B>> relatedEntities;

    public Relationship(RelationshipType type, RelationshipEntity<A> from, RelationshipEntity<B> to) {
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "RelationshipType cannot be null");
        this.rootEntities = new HashSet<>();
        rootEntities.add(from);
        this.relatedEntities = new HashSet<>();
        relatedEntities.add(to);
    }

    public Relationship(RelationshipType type) {
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "RelationshipType cannot be null");
        this.rootEntities = new HashSet<>();
        this.relatedEntities = new HashSet<>();
    }

    public Relationship() {
        this.id = UUID.randomUUID().toString();
        this.type = null;
        this.rootEntities = new HashSet<>();
        this.relatedEntities = new HashSet<>();
    }

    public boolean addRootEntity(A entity, String representative) {
        return rootEntities.add(new RelationshipEntity<>(entity, representative));
    }

    public boolean addRootEntity(RelationshipEntity<A> relationshipEntity) {
        return rootEntities.add(relationshipEntity);
    }

    public boolean addRelatedEntity(B entity, String representative) {
        return relatedEntities.add(new RelationshipEntity<>(entity, representative));
    }

    public boolean addRelatedEntity(RelationshipEntity<B> relationshipEntity) {
        return relatedEntities.add(relationshipEntity);
    }

    public boolean removeRootEntity(A entity) {
        return rootEntities.removeIf(re -> re.getEntity().equals(entity));
    }

    public boolean removeRelatedEntity(B entity) {
        return relatedEntities.removeIf(re -> re.getEntity().equals(entity));
    }

    @NonNull
    public Set<RelationshipEntity<A>> getRootEntities() {
        return Collections.unmodifiableSet(rootEntities);
    }

    @NonNull
    public Set<RelationshipEntity<B>> getRelatedEntities() {
        return Collections.unmodifiableSet(relatedEntities);
    }

    public boolean isEmpty() {
        return rootEntities.isEmpty() && relatedEntities.isEmpty();
    }

    public boolean isRootEntitiesEmpty() {
        return rootEntities.isEmpty();
    }

    public boolean isRelatedEntitiesEmpty() {
        return relatedEntities.isEmpty();
    }

    public boolean containsRootEntity(A entity) {
        return rootEntities.stream().anyMatch(re -> re.getEntity().equals(entity));
    }

    public boolean containsRelatedEntity(B entity) {
        return relatedEntities.stream().anyMatch(re -> re.getEntity().equals(entity));
    }

    public List<String> getRootEntityIdentifiers() {
        return rootEntities.stream().map(RelationshipEntity::getIdentifier).collect(Collectors.toList());
    }

    public List<String> getRelatedEntityIdentifiers() {
        return relatedEntities.stream().map(RelationshipEntity::getIdentifier).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relationship<?, ?> that = (Relationship<?, ?>) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(rootEntities, that.rootEntities)
                && Objects.equals(relatedEntities, that.relatedEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rootEntities, relatedEntities);
    }
}
