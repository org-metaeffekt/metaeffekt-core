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
package org.metaeffekt.core.inventory.processor.report;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class AssessmentInventoryMergerTest {

    @Test
    public void mergeTwoInventoriesSuccessfullyTest() throws IOException {
        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();

        {
            final Inventory inputInventory = new Inventory();
            merger.addInputInventory(inputInventory);

            final AssetMetaData amd = new AssetMetaData();
            inputInventory.getAssetMetaData().add(amd);
            amd.set(AssetMetaData.Attribute.ASSET_ID, "some id");
            amd.set(AssetMetaData.Attribute.NAME, "some id");

            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            inputInventory.getVulnerabilityMetaData().add(vmd);
            vmd.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2018-1234");
        }

        {
            final Inventory inputInventory = new Inventory();
            merger.addInputInventory(inputInventory);

            final AssetMetaData amd = new AssetMetaData();
            inputInventory.getAssetMetaData().add(amd);
            amd.set(AssetMetaData.Attribute.ASSET_ID, "some other id");
            amd.set(AssetMetaData.Attribute.NAME, "some other id");

            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            inputInventory.getVulnerabilityMetaData().add(vmd);
            vmd.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2020-4321");
        }

        final Inventory merged = merger.mergeInventories();

        Assertions.assertThat(merged.getAssetMetaData()).hasSize(2);
        Assertions.assertThat(merged.getVulnerabilityMetaData("SOME_ID-001")).hasSize(1);
        Assertions.assertThat(merged.getVulnerabilityMetaData("SOME_OTHER_ID-001")).hasSize(1);
        Assertions.assertThat(merged.getVulnerabilityMetaData("SOME_ID-001").iterator().next().get(VulnerabilityMetaData.Attribute.NAME)).isEqualTo("CVE-2018-1234");
        Assertions.assertThat(merged.getVulnerabilityMetaData("SOME_OTHER_ID-001").iterator().next().get(VulnerabilityMetaData.Attribute.NAME)).isEqualTo("CVE-2020-4321");
    }

    @Test
    public void mergeTwoInventoriesFailReasonsTest() {
        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();

        final Inventory inputInventory = new Inventory();
        merger.addInputInventory(inputInventory);

        final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
        inputInventory.getVulnerabilityMetaData().add(vmd);
        vmd.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2018-1234");

        Assertions.assertThatThrownBy(merger::mergeInventories)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one asset");

        final AssetMetaData amd = new AssetMetaData();
        inputInventory.getAssetMetaData().add(amd);

        Assertions.assertThatThrownBy(merger::mergeInventories)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("name must not be empty");

        inputInventory.getAssetMetaData().add(amd);
        amd.set(AssetMetaData.Attribute.ASSET_ID, "some id");
        amd.set(AssetMetaData.Attribute.NAME, "some id");

        Assertions.assertThatThrownBy(merger::mergeInventories)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one asset");
    }

    @Test
    public void mergeInventoriesWithDuplicateAssetsTest() throws IOException {
        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();

        {
            final Inventory inputInventory = new Inventory();
            merger.addInputInventory(inputInventory);

            final AssetMetaData amd = new AssetMetaData();
            inputInventory.getAssetMetaData().add(amd);
            amd.set(AssetMetaData.Attribute.ASSET_ID, "duplicate id");
            amd.set(AssetMetaData.Attribute.NAME, "duplicate id");

            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            inputInventory.getVulnerabilityMetaData().add(vmd);
            vmd.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2018-1234");
        }

        {
            final Inventory inputInventory = new Inventory();
            merger.addInputInventory(inputInventory);

            final AssetMetaData amd = new AssetMetaData();
            inputInventory.getAssetMetaData().add(amd);
            amd.set(AssetMetaData.Attribute.ASSET_ID, "duplicate id");
            amd.set(AssetMetaData.Attribute.NAME, "duplicate id");

            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            inputInventory.getVulnerabilityMetaData().add(vmd);
            vmd.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2020-4321");
        }

        final Inventory merged = merger.mergeInventories();

        Assertions.assertThat(merged.getAssetMetaData()).hasSize(2);
        Assertions.assertThat(merged.getVulnerabilityMetaData("DUPLICATE_ID-001")).hasSize(1);
        Assertions.assertThat(merged.getVulnerabilityMetaData("DUPLICATE_ID-002")).hasSize(1);
        Assertions.assertThat(merged.getVulnerabilityMetaData("DUPLICATE_ID-001").iterator().next().get(VulnerabilityMetaData.Attribute.NAME)).isEqualTo("CVE-2018-1234");
        Assertions.assertThat(merged.getVulnerabilityMetaData("DUPLICATE_ID-002").iterator().next().get(VulnerabilityMetaData.Attribute.NAME)).isEqualTo("CVE-2020-4321");
    }

    @Test
    public void normalizeNameTest() {
        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();
        Assertions.assertThat(merger.formatNormalizedAssessmentContextName("Test-Name-longer-than-25-chars")).isEqualTo("TEST-NAME-LONG");
    }

    @Test
    @Ignore
    public void customMergeTest() throws IOException {
        final File inputDirectory = new File("{local path required}");

        final AssessmentInventoryMerger merger = new AssessmentInventoryMerger();
        for (File file : FileUtils.listFiles(inputDirectory, new String[]{"xls", "xlsx"}, true)) {
            final Inventory inventory = new InventoryReader().readInventory(file);
            merger.addInputInventory(inventory);
        }

        final Inventory merged = merger.mergeInventories();

        new InventoryWriter().writeInventory(merged, new File(inputDirectory.getParentFile(), "report/output.xlsx"));
    }
}
