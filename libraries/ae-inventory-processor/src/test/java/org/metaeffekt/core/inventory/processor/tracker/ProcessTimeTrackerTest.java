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
package org.metaeffekt.core.inventory.processor.tracker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ProcessTimeTrackerTest {

    private final Inventory inventory = new Inventory();

    @BeforeEach
    public void setUp() {
        final ProcessorTimeTracker tracker = ProcessorTimeTracker.fromInventory(inventory);

        tracker.addTimestamp(new ProcessTimeEntry(ProcessType.SBOM_CREATION, 1));
        tracker.addTimestamp(new ProcessTimeEntry(ProcessType.SPDX_IMPORTER, 10));

        final ProcessTimeEntry processTimeEntry1 = new ProcessTimeEntry(ProcessType.INVENTORY_ENRICHMENT, 11);
        processTimeEntry1.addIndexTimestamp("index1", 1);
        processTimeEntry1.addIndexTimestamp("index2", 3);
        processTimeEntry1.addIndexTimestamp("index1", 5);
        processTimeEntry1.addIndexTimestamp("index1", 2);

        tracker.addTimestamp(processTimeEntry1);

        final ProcessTimeEntry processTimeEntry2 = new ProcessTimeEntry(ProcessType.INVENTORY_ENRICHMENT, 12);
        processTimeEntry2.addIndexTimestamp("index2", 6);

        tracker.addTimestamp(processTimeEntry2);
    }

    @Test
    public void test001() {
        final ProcessorTimeTracker tracker = ProcessorTimeTracker.fromInventory(inventory);
        log.info(String.valueOf(tracker.toJson()));

        assertEquals(3, tracker.getEntries().size());

        ProcessTimeEntry sbomCreation = tracker.getTimestamp(ProcessType.SBOM_CREATION);
        assertEquals(0, sbomCreation.getTimestamp().getFirst());
        assertEquals(1, sbomCreation.getTimestamp().getLast());
        assertEquals(0, sbomCreation.getIndexTimestamps().size());

        ProcessTimeEntry spdxImporter = tracker.getTimestamp(ProcessType.SPDX_IMPORTER);
        assertEquals(0, spdxImporter.getTimestamp().getFirst());
        assertEquals(10, spdxImporter.getTimestamp().getLast());
        assertEquals(0, spdxImporter.getIndexTimestamps().size());

        ProcessTimeEntry enrichment = tracker.getTimestamp(ProcessType.INVENTORY_ENRICHMENT);
        assertEquals(11, enrichment.getTimestamp().getFirst());
        assertEquals(12, enrichment.getTimestamp().getLast());
        assertEquals(2, enrichment.getIndexTimestamps().size());

        Map<String, ProcessTimestamp> indexStamps = enrichment.getIndexTimestamps();
        ProcessTimestamp index1 = indexStamps.get("index1");
        assertEquals(1, index1.getFirst());
        assertEquals(5, index1.getLast());

        ProcessTimestamp index2 = indexStamps.get("index2");
        assertEquals(3, index2.getFirst());
        assertEquals(6, index2.getLast());
    }

    @Test
    public void test() throws IOException {
        final File testOutputDir = new File("target/process-time-tracker");
        FileUtils.forceMkdir(testOutputDir);
        File f = new File(testOutputDir, "multipleIds.xls");
        Inventory inv = new Inventory();

        ProcessorTimeTracker tracker = ProcessorTimeTracker.fromInventory(inv);
        tracker.getOrCreateTimestamp(ProcessType.INVENTORY_ENRICHMENT, "correlate", 1);

        new InventoryWriter().writeInventory(inv, f);
        inv = new InventoryReader().readInventory(f);

        tracker = ProcessorTimeTracker.fromInventory(inv);
        ProcessTimeEntry advise = tracker.getOrCreateTimestamp(ProcessType.INVENTORY_ENRICHMENT, "advise", 1);
        advise.addIndexTimestamp("index", 10);

        log.info(String.valueOf(tracker.toJson()));
    }

    @Test
    public void mergeTrackersTest() {
        final Inventory inv1 = new Inventory();
        final ProcessorTimeTracker tracker1 = ProcessorTimeTracker.fromInventory(inv1);
        final ProcessTimeEntry entry1 = new ProcessTimeEntry(ProcessType.INVENTORY_ENRICHMENT, "advise", 100L);
        entry1.addIndexTimestamp("nvd", 50L);
        tracker1.addTimestamp(entry1);

        final Inventory inv2 = new Inventory();
        final ProcessorTimeTracker tracker2 = ProcessorTimeTracker.fromInventory(inv2);
        final ProcessTimeEntry entry2 = new ProcessTimeEntry(ProcessType.INVENTORY_ENRICHMENT, "advise", 200L);
        entry2.addIndexTimestamp("nvd", 150L);
        tracker2.addTimestamp(entry2);

        final Inventory inv3 = new Inventory();
        final ProcessorTimeTracker tracker3 = ProcessorTimeTracker.fromInventory(inv3);
        tracker3.addTimestamp(new ProcessTimeEntry(ProcessType.SBOM_CREATION, 300L));

        final Inventory targetInventory = new Inventory();
        final ProcessorTimeTracker merged = ProcessorTimeTracker.merge(targetInventory, Arrays.asList(tracker1, tracker2, tracker3));

        // verify merged tracker contains combined ranges
        final ProcessTimeEntry mergedAdvise = merged.getTimestamp(ProcessType.INVENTORY_ENRICHMENT, "advise");
        assertNotNull(mergedAdvise);
        assertEquals(100L, mergedAdvise.getTimestamp().getFirst());
        assertEquals(200L, mergedAdvise.getTimestamp().getLast());
        assertEquals(50L, mergedAdvise.getIndexTimestamps().get("nvd").getFirst());
        assertEquals(150L, mergedAdvise.getIndexTimestamps().get("nvd").getLast());

        final ProcessTimeEntry mergedSbom = merged.getTimestamp(ProcessType.SBOM_CREATION);
        assertNotNull(mergedSbom);
        assertEquals(0L, mergedSbom.getTimestamp().getFirst());
        assertEquals(300L, mergedSbom.getTimestamp().getLast());
    }
}
