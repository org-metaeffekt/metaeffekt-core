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
package org.metaeffekt.core.inventory.processor.tracker;


import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class ProcessTimeTrackerTest {

    Inventory inventory;

    @Before
    public void setUp() {
        inventory = new Inventory();
        ProcessorTimeTracker tracker = new ProcessorTimeTracker(inventory);


        tracker.addTimestamp(new ProcessTimeEntry(ProcessId.SBOM_CREATION, 1));
        tracker.addTimestamp(new ProcessTimeEntry(ProcessId.SPDX_IMPORTER, 10));

        ProcessTimeEntry processTimeEntry1 = new ProcessTimeEntry(ProcessId.INVENTORY_ENRICHMENT, 11);
        processTimeEntry1.addIndexTimestamp("index1", 1);
        processTimeEntry1.addIndexTimestamp("index2", 3);
        processTimeEntry1.addIndexTimestamp("index1", 5);
        processTimeEntry1.addIndexTimestamp("index1", 2);

        tracker.addTimestamp(processTimeEntry1);

        ProcessTimeEntry processTimeEntry2 = new ProcessTimeEntry(ProcessId.INVENTORY_ENRICHMENT, 12);
        processTimeEntry2.addIndexTimestamp("index2", 6);

        tracker.addTimestamp(processTimeEntry2);

    }

    @Test
    public void test001() {
        ProcessorTimeTracker tracker = new ProcessorTimeTracker(inventory);

        log.info(String.valueOf(tracker.toJSON()));

        Assert.assertEquals(3, tracker.getEntries().size());

        ProcessTimeEntry sbomCreation = tracker.getTimestamp(ProcessId.SBOM_CREATION);
        Assert.assertEquals(0, sbomCreation.getTimestamp().getFirst());
        Assert.assertEquals(1, sbomCreation.getTimestamp().getLast());
        Assert.assertEquals(0, sbomCreation.getIndexTimestamps().size());

        ProcessTimeEntry spdxImporter = tracker.getTimestamp(ProcessId.SPDX_IMPORTER);
        Assert.assertEquals(0, spdxImporter.getTimestamp().getFirst());
        Assert.assertEquals(10, spdxImporter.getTimestamp().getLast());
        Assert.assertEquals(0, spdxImporter.getIndexTimestamps().size());

        ProcessTimeEntry enrichment = tracker.getTimestamp(ProcessId.INVENTORY_ENRICHMENT);
        Assert.assertEquals(11, enrichment.getTimestamp().getFirst());
        Assert.assertEquals(12, enrichment.getTimestamp().getLast());
        Assert.assertEquals(2, enrichment.getIndexTimestamps().size());

        Map<String, ProcessTimestamp> indexStamps = enrichment.getIndexTimestamps();
        ProcessTimestamp index1 = indexStamps.get("index1");
        Assert.assertEquals(1, index1.getFirst());
        Assert.assertEquals(5, index1.getLast());

        ProcessTimestamp index2 = indexStamps.get("index2");
        Assert.assertEquals(3, index2.getFirst());
        Assert.assertEquals(6, index2.getLast());


    }


    @Test
    public void testOutdatedFormat() throws IOException {
        InventoryReader reader = new InventoryReader();
        //Inventory with old json format
        Inventory inv = reader.readInventory(new File("src/test/resources/merged-inventories/outdated-tracker-format/example-0.1.0-merged.xls"));

        //Check if attempting to read or add a timestamp fails
        ProcessorTimeTracker tracker = new ProcessorTimeTracker(inv);
        tracker.addTimestamp(new ProcessTimeEntry(ProcessId.SPDX_IMPORTER, 1));
    }

}
