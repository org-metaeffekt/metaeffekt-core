/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.model;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;

public class InventoryInfoTest {

    private final File TARGET_DIRECTORY = new File("target");

    @Test
    public void createNonExistingAndWrite() throws IOException {
        final Inventory inventory = new Inventory();

        final InventoryInfo info = inventory.findOrCreateInventoryInfo("test-id");
        info.set("test-key", "test-value");

        Assert.assertTrue(inventory.getInventoryInfo().contains(info));
        Assert.assertEquals("test-id", info.get(InventoryInfo.Attribute.ID));

        final File file = new File(TARGET_DIRECTORY, "test-inventory/info/info-inventory.xls");
        file.getParentFile().mkdirs();
        new InventoryWriter().writeInventory(inventory, file);

        final Inventory read = new InventoryReader().readInventory(file);

        Assert.assertEquals(1, read.getInventoryInfo().size());
        Assert.assertEquals("test-id", read.getInventoryInfo().get(0).get(InventoryInfo.Attribute.ID));
    }
}
