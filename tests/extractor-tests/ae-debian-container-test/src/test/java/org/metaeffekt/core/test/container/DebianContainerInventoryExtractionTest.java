package org.metaeffekt.core.test.container;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertNull;

public class DebianContainerInventoryExtractionTest extends AbstractContainerValidationTest {

    @Test
    public void testInventory() throws IOException {
        assertInventory(
                new File("target/analysis"),
                new File("target/inventory/ae-debian-container-test-container-inventory.xls"));
    }

    @Override
    protected void assertCommonPackageAttributes(Artifact artifact) {
        notNullOrEmpty("Component not set for " + artifact.getId(), artifact.getComponent());
        notNullOrEmpty("Version not set for " + artifact.getId(), artifact.getVersion());

        // NOTE:
        //  The url can be null or filled, depending on the package meta data. No general assertion
        //  is possible on artifact.getUrl()

        // NOTE: to derive the download urls and further package information the .dsc files need to be loaded first
        //  This is regarded a further processing step; in particular when sources need to be aggregated from the
        //  debian mirrors.
    }

    /**
     * Override. Debian does not support package level license annotations. Everything is in the copyright files.
     *
     * @param artifact
     */
    @Override
    protected void assertArtifactAttributes(Artifact artifact) {
        if (Objects.equals(artifact.get(KEY_TYPE), ARTIFACT_TYPE_PACKAGE)) {
            assertNull(artifact.get(KEY_DERIVED_LICENSE_PACKAGE));
        }
    }

}