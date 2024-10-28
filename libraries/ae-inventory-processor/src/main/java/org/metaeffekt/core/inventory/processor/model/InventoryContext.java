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

    Inventory inventory;
    String identifier;
    Inventory referenceInventory;
    String reportContextId;
    String reportContextTitle;
    String reportContext;
}
