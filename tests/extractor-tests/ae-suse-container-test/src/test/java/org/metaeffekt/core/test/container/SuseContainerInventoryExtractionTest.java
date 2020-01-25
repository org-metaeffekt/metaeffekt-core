package org.metaeffekt.core.test.container;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;

import java.io.File;
import java.io.IOException;

public class SuseContainerInventoryExtractionTest extends AbstractContainerValidationTest {

    @Test
    public void testInventory() throws IOException {
        assertInventory(
                new File("target/analysis"),
                new File("target/inventory/ae-suse-container-test-container-inventory.xls"));
    }

    protected void assertCommonPackageAttributes(Artifact artifact) {
        notNullOrEmpty("Component not set for " + artifact.getId(), artifact.getComponent());
        notNullOrEmpty("Version not set for " + artifact.getId(), artifact.getVersion());

        // NOTE:
        // - whether url is set depends on package meta data. No general assertion possible.
    }
}