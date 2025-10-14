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

    private final Set<RelationshipEntity<A>> fromEntities;
    private final Set<RelationshipEntity<B>> toEntities;

    public Relationship(RelationshipType type, RelationshipEntity<A> from, RelationshipEntity<B> to) {
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "RelationshipType cannot be null");
        this.fromEntities = new HashSet<>();
        fromEntities.add(from);
        this.toEntities = new HashSet<>();
        toEntities.add(to);
    }

    public Relationship(RelationshipType type) {
        this.id = UUID.randomUUID().toString();
        this.type = Objects.requireNonNull(type, "RelationshipType cannot be null");
        this.fromEntities = new HashSet<>();
        this.toEntities = new HashSet<>();
    }

    public Relationship() {
        this.id = UUID.randomUUID().toString();
        this.type = null;
        this.fromEntities = new HashSet<>();
        this.toEntities = new HashSet<>();
    }

    public boolean addFrom(A entity, String representative) {
        return fromEntities.add(new RelationshipEntity<>(entity, representative));
    }

    public boolean addFrom(RelationshipEntity<A> relationshipEntity) {
        return fromEntities.add(relationshipEntity);
    }

    public boolean addTo(B entity, String representative) {
        return toEntities.add(new RelationshipEntity<>(entity, representative));
    }

    public boolean addTo(RelationshipEntity<B> relationshipEntity) {
        return toEntities.add(relationshipEntity);
    }

    public boolean removeFrom(A entity) {
        return fromEntities.removeIf(re -> re.getEntity().equals(entity));
    }

    public boolean removeTo(B entity) {
        return toEntities.removeIf(re -> re.getEntity().equals(entity));
    }


    public Set<RelationshipEntity<A>> getFromEntities() {
        return Collections.unmodifiableSet(fromEntities);
    }

    public Set<RelationshipEntity<B>> getToEntities() {
        return Collections.unmodifiableSet(toEntities);
    }

    public boolean isEmpty() {
        return fromEntities.isEmpty() && toEntities.isEmpty();
    }

    public boolean fromEntitiesEmpty() {
        return fromEntities.isEmpty();
    }

    public boolean toEntitiesEmpty() {
        return toEntities.isEmpty();
    }

    public boolean containsFromEntity(A entity) {
        return fromEntities.stream().anyMatch(re -> re.getEntity().equals(entity));
    }

    public boolean containsToEntity(B entity) {
        return toEntities.stream().anyMatch(re -> re.getEntity().equals(entity));
    }

    public List<String> getToRepresentatives() {
        return toEntities.stream().map(RelationshipEntity::getRepresentative).collect(Collectors.toList());
    }

    public List<String> getFromRepresentatives() {
        return fromEntities.stream().map(RelationshipEntity::getRepresentative).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relationship<?, ?> that = (Relationship<?, ?>) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(fromEntities, that.fromEntities)
                && Objects.equals(toEntities, that.toEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, fromEntities, toEntities);
    }
}
