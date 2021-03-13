/**
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class InferMetaDataProcessorTest {

    @Ignore
    @Test
    public void testInfer() throws IOException {

        File inventoryFile = new File("/Users/kklein/workspace/metaeffekt-container-annex/documentation/ae-container-annex/target/inventory-container/analysis/ae-container-extractor-inventory.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        Properties properties = new Properties();
        properties.setProperty(InferMetaDataProcessor.INPUT_INVENTORY, inventoryFile.getAbsolutePath());
        InferMetaDataProcessor p = new InferMetaDataProcessor(properties);

        p.process(inventory);

        new InventoryWriter().writeInventory(inventory, new File("target/infer.xls"));
    }
}
