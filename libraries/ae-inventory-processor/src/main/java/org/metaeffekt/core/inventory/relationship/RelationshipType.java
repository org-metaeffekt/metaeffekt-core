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
import org.metaeffekt.core.inventory.processor.model.Constants;

import java.util.HashMap;
import java.util.Map;

public enum RelationshipType {

    DESCRIBES("describes", Constants.MARKER_CROSS),
    CONTAINS("contains", Constants.MARKER_CONTAINS),
    CONTAINS_TRANSITIVE("containsTransitive", Constants.MARKER_CONTAINS_TRANSITIVE),
    HAS_RUNTIME_DEPENDENCY("hasRuntimeDependencies", Constants.MARKER_RUNTIME_DEPENDENCY),
    HAS_RUNTIME_DEPENDENCY_TRANSITIVE("hasRuntimeDependencyTransitive", Constants.MARKER_RUNTIME_DEPENDENCY_TRANSITIVE),
    HAS_DEV_DEPENDENCY("hasDevDependencies", Constants.MARKER_DEVELOPMENT_DEPENDENCY),
    HAS_DEV_DEPENDENCY_TRANSITIVE("hasDevDependencyTransitive", Constants.MARKER_DEVELOPMENT_DEPENDENCY_TRANSITIVE),
    HAS_OPTIONAL_DEPENDENCY("hasOptionalDependencies", Constants.MARKER_OPTIONAL_DEPENDENCY),
    HAS_OPTIONAL_DEPENDENCY_TRANSITIVE("hasOptionalDependencyTransitive", Constants.MARKER_OPTIONAL_DEPENDENCY_TRANSITIVE),
    HAS_PEER_DEPENDENCY("hasPeerDependency", Constants.MARKER_PEER_DEPENDENCY),
    HAS_PEER_DEPENDENCY_TRANSITIVE("hasPeerDependencyTransitive", Constants.MARKER_PEER_DEPENDENCY_TRANSITIVE),;

    private final String name;

    @Getter
    private final String inventoryConstant;

    private static final Map<String, RelationshipType> INVENTORY_CONSTANT_MAP = new HashMap<>();

    static {
        for (RelationshipType relationshipType : RelationshipType.values()) {
            INVENTORY_CONSTANT_MAP.put(relationshipType.inventoryConstant, relationshipType);
        }
    }

    public static RelationshipType fromInventoryConstant(String inventoryConstant) {
        RelationshipType relationshipType = INVENTORY_CONSTANT_MAP.get(inventoryConstant);

        if (relationshipType == null) {
            return null;
        }

        return INVENTORY_CONSTANT_MAP.get(inventoryConstant);
    }

    RelationshipType(String name, String inventoryConstant) {
        this.name = name;
        this.inventoryConstant = inventoryConstant;
    }



}
