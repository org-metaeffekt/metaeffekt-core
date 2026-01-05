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
import lombok.Setter;

import java.util.Objects;

/**
 * A single relationship entity made up of a single object as well as an optional representative. The representative
 * allows this relationship entity to have a second identifer which is usefull for mapping entities during the
 * sbom conversion cycle for example.
 * @param <T> class type generic
 */
@Getter
@Setter
public class RelationshipEntity<T> {

    private T entity;
    private String representative;

    public RelationshipEntity(T entity) {
        this(entity, null);
    }

    public RelationshipEntity(T entity, String representative) {
        this.entity = entity;
        this.representative = representative;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(entity, ((RelationshipEntity<?>) o).entity) &&
                Objects.equals(representative, ((RelationshipEntity<?>) o).getRepresentative());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, representative);
    }

    @Override
    public String toString() {
        return "Entity: " + entity.toString() + " Representative: " + representative;
    }
}
