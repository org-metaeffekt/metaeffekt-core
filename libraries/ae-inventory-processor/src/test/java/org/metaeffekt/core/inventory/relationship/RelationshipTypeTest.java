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

import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Constants;

import static org.assertj.core.api.Assertions.assertThat;

public class RelationshipTypeTest {

    @Test
    public void testValidTypeMappings() {
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_CROSS)).isEqualTo(RelationshipType.DESCRIBES);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_CONTAINS)).isEqualTo(RelationshipType.CONTAINS);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_CONTAINS_TRANSITIVE)).isEqualTo(RelationshipType.CONTAINS_TRANSITIVE);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_RUNTIME_DEPENDENCY)).isEqualTo(RelationshipType.HAS_RUNTIME_DEPENDENCY);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_RUNTIME_DEPENDENCY_TRANSITIVE)).isEqualTo(RelationshipType.HAS_RUNTIME_DEPENDENCY_TRANSITIVE);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_DEVELOPMENT_DEPENDENCY)).isEqualTo(RelationshipType.HAS_DEV_DEPENDENCY);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_DEVELOPMENT_DEPENDENCY_TRANSITIVE)).isEqualTo(RelationshipType.HAS_DEV_DEPENDENCY_TRANSITIVE);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_OPTIONAL_DEPENDENCY)).isEqualTo(RelationshipType.HAS_OPTIONAL_DEPENDENCY);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_OPTIONAL_DEPENDENCY_TRANSITIVE)).isEqualTo(RelationshipType.HAS_OPTIONAL_DEPENDENCY_TRANSITIVE);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_PEER_DEPENDENCY)).isEqualTo(RelationshipType.HAS_PEER_DEPENDENCY);
        assertThat(RelationshipType.fromInventoryConstant(Constants.MARKER_PEER_DEPENDENCY_TRANSITIVE)).isEqualTo(RelationshipType.HAS_PEER_DEPENDENCY_TRANSITIVE);
    }

    @Test
    public void testInvalidTypeMappings() {
        assertThat(RelationshipType.fromInventoryConstant("invalid")).isNull();
        assertThat(RelationshipType.fromInventoryConstant("")).isNull();
        assertThat(RelationshipType.fromInventoryConstant(null)).isNull();
    }

}
