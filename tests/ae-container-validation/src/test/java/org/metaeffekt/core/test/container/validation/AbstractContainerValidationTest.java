package org.metaeffekt.core.test.container.validation;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public abstract class AbstractContainerValidationTest {

    /**
     * Checks various general assertions on the analysisDir and inventoryFile.
     *
     * @param analysisDir
     * @param inventoryFile
     *
     * @throws IOException
     */
    protected void assertInventory(File analysisDir, File inventoryFile) throws IOException {
        assertTrue("Inventory file must exist.", inventoryFile.exists());
        assertTrue("Analysis dir must exist.", analysisDir.exists());

        Inventory inventory = new InventoryReader().readInventory(inventoryFile);
        assertTrue("Inventory must contain artifacts.", !inventory.getArtifacts().isEmpty());

        inventory.getArtifacts().stream().forEach(this::assertAttributes);

        assertTrue(new File(analysisDir, "files.txt").exists());
        assertTrue(new File(analysisDir, "files").exists());
    }

    protected void assertAttributes(Artifact artifact) {
        assertCommonAttributes(artifact);

        if (ARTIFACT_TYPE_PACKAGE.equalsIgnoreCase(artifact.get(KEY_TYPE))) {
            assertCommonPackageAttributes(artifact);
        }

        if (ARTIFACT_TYPE_FILE.equalsIgnoreCase(artifact.get(KEY_TYPE))) {
            assertCommonFileAttributes(artifact);
        }

        if (!StringUtils.isEmpty(artifact.getVersion())) {
            assertTrue(String.format("Version not included in artifact id: id: %s, v: %s",
                    artifact.getId(), artifact.getVersion()),
                    artifact.getId().contains(artifact.getVersion()));
        }

        assertArtifactAttributes(artifact);
    }

    protected void assertCommonPackageAttributes(Artifact artifact) {
        notNullOrEmpty("Component not set for " + artifact.getId(), artifact.getComponent());
        notNullOrEmpty("Version not set for " + artifact.getId(), artifact.getVersion());
        notNullOrEmpty("Url not set for " + artifact.getId(), artifact.getUrl());
    }

    protected void assertCommonFileAttributes(Artifact artifact) {
        Set<String> projects = artifact.getProjects();
        assertTrue("No project is set for file artifact " + artifact.getId() ,projects != null && projects.size() == 1);
        assertNotNull("Checksum is required for file artifact " + artifact.getId() ,artifact.getChecksum());
    }

    protected void assertCommonAttributes(Artifact artifact) {
        notNullOrEmpty("Id not set for artifact " + artifact.createStringRepresentation(), artifact.getId());
        notNullOrEmpty(KEY_SOURCE_PROJECT + " not set for " + artifact.getId(), artifact.get(KEY_SOURCE_PROJECT));
        notNullOrEmpty(KEY_TYPE + " not set for " + artifact.getId(), artifact.get(KEY_TYPE));

        // no license information curated yet
        nullOrEmpty("License must not be set " + artifact.getId(),
                artifact.getLicense());

        // no additional version information set
        nullOrEmpty("Latest version must not be set " + artifact.getId(),
                artifact.getLatestVersion());
    }

    /**
     * Overwrite if required.
     *
     * @param artifact
     */
    protected void assertArtifactAttributes(Artifact artifact) {
        if (Objects.equals(artifact.get(KEY_TYPE), ARTIFACT_TYPE_PACKAGE)) {
            assertNotNull(artifact.get(KEY_DERIVED_LICENSE_PACKAGE));
        }
    }

    protected void notNullOrEmpty(String message, String value) {
        assertTrue(message, !StringUtils.isEmpty(value));
    }

    protected void nullOrEmpty(String message, String value) {
        assertTrue(message, StringUtils.isEmpty(value));
    }
}