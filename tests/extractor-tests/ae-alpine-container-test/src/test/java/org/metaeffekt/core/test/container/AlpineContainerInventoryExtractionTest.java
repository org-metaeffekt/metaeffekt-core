package org.metaeffekt.core.test.container;

import org.junit.Test;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;

import java.io.File;
import java.io.IOException;

public class AlpineContainerInventoryExtractionTest extends AbstractContainerValidationTest {

    @Test
    public void testInventory() throws IOException {
        assertInventory(
                new File("target/analysis"),
                new File("target/inventory/ae-alpine-container-test-container-inventory.xls"));
    }

}