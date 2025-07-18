/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.writer;

import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryWriterTest {

    @Test
    public void testWrite_Empty() throws IOException {
        final File testOutputDir = new File("target/inventory-writer-test");
        FileUtils.forceMkdir(testOutputDir);
        new InventoryWriter().writeInventory(new Inventory(), new File(testOutputDir, "emtpy.xls"));
        new InventoryWriter().writeInventory(new Inventory(), new File(testOutputDir, "emtpy.xlsx"));
        new InventoryWriter().writeInventory(new Inventory(), new File(testOutputDir, "emtpy.ser"));
    }

    @Ignore
    @Test
    public void testReadWrite() throws IOException {
        final File inputInventoryFile = new File("<path-to-input-inventory>");
        final File outputInventoryFile = new File("<path-to-output-inventory>");

        final Inventory inventory = new InventoryReader().readInventory(inputInventoryFile);
        new InventoryWriter().writeInventory(inventory, outputInventoryFile);
    }

    @Test
    public void testCustomTempFileCreationStrategy() throws IOException {
        final File inputInventoryFile = new File("src/test/resources/test-inventory-keycloak/keycloak-25.0.4-scanned.xlsx");
        final File defaultStrategyOutputFile = new File("target/inventory-writer-test/defaultFileCreationStrategy.xlsx");
        final File customStrategyOutputFile = new File("target/inventory-writer-test/customTempFileCreationStrategy.xlsx");

        FileUtils.forceMkdirParent(defaultStrategyOutputFile);
        FileUtils.forceMkdirParent(customStrategyOutputFile);

        final Inventory inventory = new InventoryReader().readInventory(inputInventoryFile);

        new InventoryWriter().writeInventory(inventory, customStrategyOutputFile);

        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy());
        new InventoryWriter().writeInventory(inventory, defaultStrategyOutputFile);

        Inventory defaultStrategyInventory = new InventoryReader().readInventory(defaultStrategyOutputFile);
        Inventory customStrategyInventory = new InventoryReader().readInventory(customStrategyOutputFile);

        assertThat(defaultStrategyInventory.getArtifacts().containsAll(customStrategyInventory.getArtifacts()))
                .isEqualTo(customStrategyInventory.getArtifacts().containsAll(defaultStrategyInventory.getArtifacts()));

        assertThat(defaultStrategyInventory.getAssetMetaData().containsAll(customStrategyInventory.getAssetMetaData()))
                .isEqualTo(customStrategyInventory.getAssetMetaData().containsAll(defaultStrategyInventory.getAssetMetaData()));
    }
}