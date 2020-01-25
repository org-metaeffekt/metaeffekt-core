package org.metaeffekt.core.test.container;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertNull;

public class UbuntuContainerInventoryExtractionTest extends AbstractContainerValidationTest {

    @Test
    public void testInventory() throws IOException {
        assertInventory(
                new File("target/analysis"),
                new File("target/inventory/ae-ubuntu-container-test-container-inventory.xls"));
    }

    @Override
    protected void assertCommonPackageAttributes(Artifact artifact) {
        notNullOrEmpty("Component not set for " + artifact.getId(), artifact.getComponent());
        notNullOrEmpty("Version not set for " + artifact.getId(), artifact.getVersion());

        // NOTE:
        // - whether url is set depends on package meta data. No general assertion possible.
    }

    /**
     * Override. Debian does not support package level license annotations. Everything is in the copyright files.
     *
     * @param artifact
     */
    @Override
    protected void assertArtifactAttributes(Artifact artifact) {
        if (Objects.equals(artifact.get(InventoryExtractor.KEY_ATTRIBUTE_TYPE), InventoryExtractor.TYPE_PACKAGE)) {
            assertNull(artifact.get(InventoryExtractor.KEY_DERIVED_LICENSE_PACKAGE));
        }
    }

}