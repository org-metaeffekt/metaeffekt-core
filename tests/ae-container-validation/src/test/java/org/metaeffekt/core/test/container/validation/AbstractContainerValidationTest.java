/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.test.container.validation;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertTrue(inventoryFile.exists(), "Inventory file must exist.");
        assertTrue(analysisDir.exists(), "Analysis dir must exist.");

        Inventory inventory = new InventoryReader().readInventory(inventoryFile);
        assertTrue(!inventory.getArtifacts().isEmpty(), "Inventory must contain artifacts.");

        inventory.getArtifacts().stream().forEach(this::assertAttributes);

        assertTrue(new File(analysisDir, "filesystem/files.txt").exists());
        assertTrue(new File(analysisDir, "filesystem/folders.txt").exists());
        assertTrue(new File(analysisDir, "filesystem/symlinks.txt").exists());
        assertTrue(new File(analysisDir, "package-meta").exists());
        assertTrue(new File(analysisDir, "package-files").exists());
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
            assertTrue(artifact.getId().contains(artifact.getVersion()),
                    String.format("Version not included in artifact id: id: %s, v: %s",
                    artifact.getId(), artifact.getVersion())
            );
        }

        assertArtifactAttributes(artifact);
    }

    protected void assertCommonPackageAttributes(Artifact artifact) {
        notNullOrEmpty("Component not set for " + artifact.getId(), artifact.getComponent());
        notNullOrEmpty("Version not set for " + artifact.getId(), artifact.getVersion());
        notNullOrEmpty("Url not set for " + artifact.getId(), artifact.getUrl());
    }

    protected void assertCommonFileAttributes(Artifact artifact) {
        Set<String> paths = artifact.getRootPaths();
        assertTrue(paths != null && paths.size() == 1, "No root path is set for file artifact " + artifact.getId());
        assertNotNull(artifact.getChecksum(), "Checksum is required for file artifact " + artifact.getId());
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
        assertTrue(!StringUtils.isEmpty(value), message);
    }

    protected void nullOrEmpty(String message, String value) {
        assertTrue(StringUtils.isEmpty(value), message);
    }
}