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

import java.util.List;


/**
 * This class serves as an edge of the RelationshipGraph class and tracks the relationship
 * of two nodes.
 */
@Getter
@Setter
public class RelationshipGraphEdge {
    private RelationshipGraphNode fromNode;
    private List<RelationshipGraphNode> toNodes;
    private RelationshipGraph.RelationshipType relationshipType;

    public RelationshipGraphEdge(RelationshipGraphNode fromNode,
                                 List<RelationshipGraphNode> toNode, RelationshipGraph.RelationshipType relationshipType) {

        this.fromNode = fromNode;
        this.toNodes = toNode;
        this.relationshipType = relationshipType;
    }


    public void addToNode(RelationshipGraphNode toNode) {
        this.toNodes.add(toNode);
    }

    @Override
    public String toString() {
        return "Relationship: " + fromNode.getId() + " - " + relationshipType.toString() + " - " + toNodes.toString();
    }
}
