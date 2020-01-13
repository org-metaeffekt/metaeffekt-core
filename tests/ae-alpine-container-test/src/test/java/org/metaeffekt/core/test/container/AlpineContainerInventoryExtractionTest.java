package org.metaeffekt.core.test.container;

import static org.junit.Assert.*;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractor;

import java.io.File;
import java.io.IOException;

public class AlpineContainerInventoryExtractionTest {

    @Test
    public void testInventory() throws IOException {
        File file = new File("target/inventory/ae-alpine-container-test-inventory.xls");
        assertTrue("Inventory file must exist.", file.exists());
        Inventory inventory = new InventoryReader().readInventory(file);
        assertTrue("Inventory must contain artifacts.", !inventory.getArtifacts().isEmpty());
        inventory.getArtifacts().stream().forEach(this::assertArtifactConsistent);
    }

    public void assertArtifactConsistent(Artifact artifact) {
        assertNotNull(artifact.getId());
        assertNotNull(artifact.getComponent());
        assertNotNull(artifact.getVersion());

        assertNotNull(artifact.get(InventoryExtractor.KEY_DERIVED_LICENSE_PACKAGE));
        assertNotNull(artifact.get(InventoryExtractor.KEY_ATTRIBUTE_SOURCE_PROJECT));
        assertNotNull(artifact.get(InventoryExtractor.KEY_ATTRIBUTE_TYPE));
        assertNotNull(artifact.getUrl());

        assertNullOrEmpty(artifact.getLicense());
        assertNullOrEmpty(artifact.getLatestAvailableVersion());

        assertTrue(String.format("Version not included in artifact id: id: %s, v: %s",
                artifact.getId(), artifact.getVersion()),
                artifact.getId().contains(artifact.getVersion()));
    }

    private void assertNullOrEmpty(String value) {
        if (value != null) {
            assertEquals("", value);
        }
    }
}