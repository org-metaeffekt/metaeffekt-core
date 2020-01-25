package org.metaeffekt.core.test.container;

import static org.junit.Assert.*;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ArchContainerInventoryExtractionTest extends AbstractContainerValidationTest {

    @Test
    public void testInventory() throws IOException {
        assertInventory(
                new File("target/analysis"),
                new File("target/inventory/ae-arch-container-test-container-inventory.xls"));
    }

}