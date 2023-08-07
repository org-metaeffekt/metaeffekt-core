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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;

public class AssessmentInventoryMergerTest {

    @Test
    public void sditMergedTest() throws IOException {
        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();
        merger.addInputInventoryFile(new File("/Users/ywittmann/workspace/ref/inventories/00_Other/testing/assessment_merging_S-DIT-007-Merged-Inventories-DK/S-DIT-007-Merged-Inventories-DK/merged"));

        final Inventory merged = merger.mergeInventories();
        new InventoryWriter().writeInventory(merged, new File("/Users/ywittmann/workspace/ref/inventories/00_Other/testing/assessment_merging_S-DIT-007-Merged-Inventories-DK/S-DIT-007-Merged-Inventories-DK/merged.xls"));
    }

    @Test
    public void normalizeNameTest() {
        AssessmentInventoryMerger merger = new AssessmentInventoryMerger();
        System.out.println(merger.formatNormalizedAssessmentContextName("Test-Name-longer-than-25-chars"));
    }
}