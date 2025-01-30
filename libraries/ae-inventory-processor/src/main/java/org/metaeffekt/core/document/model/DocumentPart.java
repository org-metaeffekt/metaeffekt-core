package org.metaeffekt.core.document.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class representing a part of a document.
 */
@Slf4j
@Getter
@Setter
public class DocumentPart {

    /**
     * List containing the inventoryContexts for each inventory we want to add to a report. Each inventoryContext contains
     * an inventory and other data needed for the report.
     */
    private List<InventoryContext> inventoryContexts;

    /**
     * The type of DocumentPart (e.g. vulnerability-statistics-report, vulnerability-summary-report, etc.)
     */
    private DocumentPartType documentPartType;

    /**
     * We define params on document-level as well as document-part-level. If a documentPart defines a parameter that the
     * already defines, then we expect the Document parameter to be overwritten on document-part-level.
     */
    private Map<String, String> params;

    public void validate() {

        // validate each inventoryContext
        Set<String> identifiers = new HashSet<>();
        for (InventoryContext context : inventoryContexts) {
            // check if each inventoryContext references an inventory
            if (context.getInventory() == null) {
                throw new IllegalStateException("The inventory must be specified.");
            }
            // check if each inventoryContext has an identifier
            if (context.getIdentifier() == null){
                throw new IllegalStateException("The identifier must be specified.");
            }
            // check if each inventoryContext has an identifier
            if (context.getIdentifier().isEmpty()){
                throw new IllegalStateException("The identifier must not be empty.");
            }
        }
    }
}
