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
package org.metaeffekt.core.inventory.processor.reader;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.ThreatMetaData;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;


@Slf4j
public class InventoryReaderTest {

    @Test
    public void inventoryReaderTest() throws IOException {
        Inventory inventory = new InventoryReader().readInventory(new File("src/test/resources/read-write-inventory/keycloak-reference-inventory_single.xls"));

        ThreatMetaData tmd = new ThreatMetaData();
        tmd.set(ThreatMetaData.Attribute.ID, "THR-STRIDE-S");
        inventory.setThreatMetaData(Collections.singletonList(tmd));

        File f = new File("target/read-write-inventory/keycloak-reference-inventory_single.xls");
        FileUtils.forceMkdirParent(f);

        new InventoryWriter().writeInventory(inventory, f);



    }
}
