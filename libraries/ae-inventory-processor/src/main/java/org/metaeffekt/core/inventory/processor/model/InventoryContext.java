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
