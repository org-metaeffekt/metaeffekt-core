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
package org.metaeffekt.core.inventory;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.tracker.ProcessTimeEntry;
import org.metaeffekt.core.inventory.processor.tracker.ProcessType;
import org.metaeffekt.core.inventory.processor.tracker.ProcessorTimeTracker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InventoryMergeUtilsTest {

    @Disabled
    @Test
    public void testMerge() throws IOException {
        Inventory targetInventory = new Inventory();
        File sourceInventoryFile = new File("<path to file>");

        List<File> sourceInventories = Collections.singletonList(sourceInventoryFile);

        new InventoryMergeUtils().merge(sourceInventories, targetInventory);

        System.out.println(targetInventory.getArtifacts().size());

        Assertions.assertThat(targetInventory.getArtifacts()).hasSizeGreaterThan(0);
    }


    @Test
    public void emptyInventories() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        List<Inventory> sourceInventories = Collections.singletonList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        Assertions.assertThat(target.getArtifacts()).isEmpty();
    }

    @Test
    public void singleArtifactInventoryMergedIntoTarget() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        Artifact singleArtifact = buildArtifact("singleArtifact");
        source.getArtifacts().add(singleArtifact);

        List<Inventory> sourceInventories = Collections.singletonList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        Assertions.assertThat(target.getArtifacts()).extracting("id").contains("singleArtifact");
    }

    @Test
    public void sourceAndTargetWithSameArtifactId() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        Artifact singleArtifact = buildArtifact("singleArtifact");
        source.getArtifacts().add(singleArtifact);
        target.getArtifacts().add(singleArtifact);

        List<Inventory> sourceInventories = Collections.singletonList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        // FIXME is this the expected result
        Assertions.assertThat(target.getArtifacts()).extracting("id").isEmpty();
//        Assertions.assertThat(target.getArtifacts()).extracting("id").containsOnlyOnce("singleArtifact");
    }

    @Test
    public void sourceAndTargetWithSameArtifactIdDifferentObjects() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        source.getArtifacts().add(buildArtifact("singleArtifact"));
        target.getArtifacts().add(buildArtifact("singleArtifact"));

        Assertions.assertThat(source.getArtifacts()).hasSize(1);
        Assertions.assertThat(target.getArtifacts()).hasSize(1);

        List<Inventory> sourceInventories = Collections.singletonList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        Assertions.assertThat(target.getArtifacts()).extracting("id").containsOnlyOnce("singleArtifact");
    }

    @Test
    public void sourceAndTargetWithDifferentArtifactIds() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        source.getArtifacts().add(buildArtifact("singleSourceArtifact"));
        target.getArtifacts().add(buildArtifact("singleTargetArtifact"));

        List<Inventory> sourceInventories = Collections.singletonList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        Assertions.assertThat(target.getArtifacts()).extracting("id")
                .containsOnlyOnceElementsOf(Arrays.asList("singleSourceArtifact", "singleTargetArtifact"));
    }

    @Test
    public void processorTimeTrackersMergedIntoTarget() {
        Inventory source1 = new Inventory();
        ProcessorTimeTracker.fromInventory(source1).addTimestamp(new ProcessTimeEntry(ProcessType.SBOM_CREATION, 100L));

        Inventory source2 = new Inventory();
        ProcessorTimeTracker.fromInventory(source2).addTimestamp(new ProcessTimeEntry(ProcessType.SBOM_CREATION, 250L));

        Inventory target = new Inventory();
        new InventoryMergeUtils().mergeInventories(Arrays.asList(source1, source2), target);

        ProcessorTimeTracker targetTracker = ProcessorTimeTracker.fromInventory(target);
        ProcessTimeEntry sbomEntry = targetTracker.getTimestamp(ProcessType.SBOM_CREATION);
        Assertions.assertThat(sbomEntry).isNotNull();
        Assertions.assertThat(sbomEntry.getTimestamp().getFirst()).isEqualTo(100L);
        Assertions.assertThat(sbomEntry.getTimestamp().getLast()).isEqualTo(250L);
    }

    private static Artifact buildArtifact(String id) {
        Artifact singleArtifact = new Artifact();
        singleArtifact.setId(id);
        return singleArtifact;
    }
}