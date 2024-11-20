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
package org.metaeffekt.core.maven.inventory.mojo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

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
        List<Inventory> sourceInventories = Arrays.asList(source);
        new InventoryMergeUtils().mergeInventories(sourceInventories, target);

        Assertions.assertThat(target.getArtifacts()).isEmpty();
    }

    @Test
    public void singleArtifactInventoryMergedIntoTarget() {
        Inventory source = new Inventory();
        Inventory target = new Inventory();
        Artifact singleArtifact = buildArtifact("singleArtifact");
        source.getArtifacts().add(singleArtifact);

        Assert.assertTrue(targetInventory.getLicenseData().size() > 0);
    }


}