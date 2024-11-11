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
package org.metaeffekt.core.inventory.processor.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Class for defining context information for an inventory. Enables DocumentDescriptors to identify each referenced
 * inventory and access document generation relevant fields.
 */
@Getter
@Setter
public class InventoryContext {

    /**
     * The inventory which is defined in this context.
     */
    Inventory inventory;

    /**
     * The custom identifier of an inventory, which is used for structuring the report.
     */
    String identifier;

    /**
     * This inventory is used as a reference for e.g. handling of unknown fields, etc. If no reference seems fit, set
     * this to the same inventory as the inventory of this context.
     */
    Inventory referenceInventory;

    /**
     * Fields needed for the context of a report.
     */
    String reportContextId;
    String reportContextTitle;
    String reportContext;

    public InventoryContext(Inventory inventory, Inventory referenceInventory, String identifier, String reportContextId, String reportContextTitle, String reportContext) {
        this.inventory = inventory;
        this.identifier = identifier;
        this.referenceInventory = referenceInventory;
        this.reportContextId = reportContextId;
        this.reportContextTitle = reportContextTitle;
        this.reportContext = reportContext;
    }
}
