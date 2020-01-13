package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;

/**
 * Interface common to all {@link InventoryExtractor}s.
 */
public interface InventoryExtractor {

    String KEY_DERIVED_LICENSE_PACKAGE = "Specified Package License";

    String KEY_ATTRIBUTE_TYPE = "Type";
    String KEY_ATTRIBUTE_SOURCE_PROJECT = "Source Project";

    String TYPE_PACKAGE = "package";

    /**
     * Checks whether the extractor is applicable to the content in analysisDir.
     *
     * @param analysisDir The analysisDir.
     * @return
     */
    boolean applies(File analysisDir);

    /**
     * Validates that the content in analysisDir is as anticipated.
     *
     * @param analysisDir The analysisDir.
     * @return
     *
     * @Throws IllegalStateException
     */
    void validate(File analysisDir) throws IllegalStateException;

    /**
     * Extract an inventory from the information aggregated in analysisDir.
     *
     * @param analysisDir The analysisDir.
     * @param inventoryId The identifier or discriminator for the inventory.
     * @return
     * @throws IOException
     */
    Inventory extractInventory(File analysisDir, String inventoryId) throws IOException;

}
