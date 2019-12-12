/**
 * Copyright 2009-2019 the original author or authors.
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
package org.metaeffekt.core.inventory;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.DependenciesDitaReport;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for dealing with Inventories.
 */
public abstract class InventoryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DependenciesDitaReport.class);

    /**
     * Read the inventories located in inventoryBaseDir matching the inventoryIncludes pattern.
     * This methods (to the extend that a specific inventory is qualified by inventoryIncludes)
     * supports reading the inventory from the classpath. This is especially useful for cases
     * where the license and component files are not required in the filesystem and an inventory
     * jar is not required to be unpacked.
     *
     * @param inventoryBaseDir The inventory base dir.
     * @param inventoryIncludes The comma-separated include patterns.
     * @return The read aggregated inventory.
     * @throws IOException
     */
    public static Inventory readInventory(File inventoryBaseDir, String inventoryIncludes) throws IOException {
        if (inventoryBaseDir != null) {
            if (inventoryBaseDir.exists()) {
                return readInventoryFromFilesystem(inventoryBaseDir, inventoryIncludes);
            } else {
                // not in the file system; maybe a classpath resource
                return readInventoryFromClasspath(inventoryBaseDir, inventoryIncludes);
            }
        }
        throw new IOException("Cannot read inventory. No base dir specified.");
    }

    private static Inventory readInventoryFromFilesystem(File inventoryBaseDir, String inventoryIncludes) throws IOException {
        // read the inventories in the file structure
        String[] inventories = FileUtils.scanForFiles(inventoryBaseDir, inventoryIncludes, "-nothing-");

        // sort for deterministic inheritance
        List<String> orderedInventories = Arrays.stream(inventories).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        Inventory aggregateInventory = new Inventory();
        InventoryReader reader = new InventoryReader();
        for (String inventoryFile : orderedInventories) {
            Inventory inventory = reader.readInventory(new File(inventoryBaseDir, inventoryFile));
            aggregateInventory.inheritArtifacts(inventory, true);
            aggregateInventory.inheritLicenseMetaData(inventory, true);
            aggregateInventory.inheritComponentPatterns(inventory, true);
            aggregateInventory.inheritVulnerabilityMetaData(inventory, true);
        }
        return aggregateInventory;
    }

    private static Inventory readInventoryFromClasspath(File inventoryBaseDir, String inventoryIncludes) throws IOException {
        File file = new File(inventoryBaseDir, inventoryIncludes);
        Resource inventoryResource = new ClassPathResource(file.getPath());
        try (InputStream in = inventoryResource.getInputStream()) {
            return new InventoryReader().readInventory(in);
        } catch (IOException e) {
            throw new IOException(String.format("Unable to read inventory from classpath: {}/{}", inventoryBaseDir, inventoryIncludes), e);
        }
    }

}
