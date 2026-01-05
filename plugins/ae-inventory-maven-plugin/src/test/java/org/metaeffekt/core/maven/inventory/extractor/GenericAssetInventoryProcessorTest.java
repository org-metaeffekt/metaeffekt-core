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
package org.metaeffekt.core.maven.inventory.extractor;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GenericAssetInventoryProcessorTest {

    private final static File SOURCE_DIRECTORY = new File("src/test/resources");
    private final static File TARGET_DIRECTORY = new File("target/test-output");

    @Test
    public void findExistingAssetTest() throws IOException {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("Asset Path", "namespace/corporation/level");
        attributes.put("Assessment_Id", "ASSESSMENT-01");

        final File targetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/findExistingAssetTest.xls");
        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .supply("ASSET-01")
                .withAttributes(attributes)
                .augmenting(new File(SOURCE_DIRECTORY, "GenericAssetInventoryProcessorTest/findExistingAssetTest.xls"))
                .writing(targetFile);

        processor.process();

        final Inventory targetInventory = new InventoryReader().readInventory(targetFile);
        Assert.assertEquals(1, targetInventory.getAssetMetaData().size()); // should not create a new asset
        Assert.assertEquals("ASSET-01", targetInventory.getAssetMetaData().get(0).get("Asset Id"));
        Assert.assertEquals("namespace/corporation/level", targetInventory.getAssetMetaData().get(0).get("Asset Path"));
        Assert.assertEquals("ASSESSMENT-01", targetInventory.getAssetMetaData().get(0).get("Assessment Id"));
    }

    @Test
    public void createNewFromNoExistingAssetTest() throws IOException {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("Asset Path", "namespace/corporation/level");
        attributes.put("Assessment_Id", "ASSESSMENT-01");

        final File targetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls");
        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .supply("ASSET-01")
                .withAttributes(attributes)
                .augmenting(new File(SOURCE_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls"))
                .writing(targetFile);

        processor.process();

        final Inventory targetInventory = new InventoryReader().readInventory(targetFile);
        Assert.assertEquals(1, targetInventory.getAssetMetaData().size()); // should create a new asset
        Assert.assertEquals("ASSET-01", targetInventory.getAssetMetaData().get(0).get("Asset Id"));
        Assert.assertEquals("namespace/corporation/level", targetInventory.getAssetMetaData().get(0).get("Asset Path"));
        Assert.assertEquals("ASSESSMENT-01", targetInventory.getAssetMetaData().get(0).get("Assessment Id"));
    }

    @Test
    public void pickSingleAssetOnEmptyInventoryTest() {
        final File targetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls");
        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .pickSingleAsset(true)
                .withAttributes(new HashMap<>())
                .augmenting(new File(SOURCE_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls"))
                .writing(targetFile);

        Assert.assertThrows(IllegalStateException.class, processor::process);
    }

    @Test
    public void pickSingleAssetAndAssetIdOnEmptyInventoryTest() {
        final File targetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls");
        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .supply("ASSET-01")
                .pickSingleAsset(true)
                .withAttributes(new HashMap<>())
                .augmenting(new File(SOURCE_DIRECTORY, "GenericAssetInventoryProcessorTest/createNewFromNoExistingAssetTest.xls"))
                .writing(targetFile);

        Assert.assertThrows(IllegalArgumentException.class, processor::process);
    }

    @Test
    public void findSingleAssetInventoryTest() throws IOException {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("Asset Path", "namespace/corporation/level");
        attributes.put("Assessment_Id", "ASSESSMENT-01");

        final File targetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/findExistingAssetTest.xls");
        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .pickSingleAsset(true)
                .withAttributes(attributes)
                .augmenting(new File(SOURCE_DIRECTORY, "GenericAssetInventoryProcessorTest/findExistingAssetTest.xls"))
                .writing(targetFile);

        processor.process();

        final Inventory targetInventory = new InventoryReader().readInventory(targetFile);
        Assert.assertEquals(1, targetInventory.getAssetMetaData().size()); // should not create a new asset
        Assert.assertEquals("ASSET-01", targetInventory.getAssetMetaData().get(0).get("Asset Id"));
        Assert.assertEquals("namespace/corporation/level", targetInventory.getAssetMetaData().get(0).get("Asset Path"));
        Assert.assertEquals("ASSESSMENT-01", targetInventory.getAssetMetaData().get(0).get("Assessment Id"));
    }

    @Test
    public void inPlaceModificationTest() throws IOException {
        final File sourceAndTargetFile = new File(TARGET_DIRECTORY, "GenericAssetInventoryProcessorTest/inPlaceModificationTest.xls");

        final Inventory sourceInventory = new Inventory();
        final AssetMetaData sourceAsset = new AssetMetaData();
        sourceAsset.set("Asset Id", "ASSET-01");
        sourceInventory.getAssetMetaData().add(sourceAsset);
        new InventoryWriter().writeInventory(sourceInventory, sourceAndTargetFile);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("Asset Path", "namespace/corporation/level");
        attributes.put("Assessment_Id", "ASSESSMENT-01");

        final GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .supply("ASSET-01")
                .withAttributes(attributes)
                .augmenting(sourceAndTargetFile);

        processor.process();

        final Inventory targetInventory = new InventoryReader().readInventory(sourceAndTargetFile);
        Assert.assertEquals(1, targetInventory.getAssetMetaData().size()); // should not create a new asset
        Assert.assertEquals("ASSET-01", targetInventory.getAssetMetaData().get(0).get("Asset Id"));
        Assert.assertEquals("namespace/corporation/level", targetInventory.getAssetMetaData().get(0).get("Asset Path"));
        Assert.assertEquals("ASSESSMENT-01", targetInventory.getAssetMetaData().get(0).get("Assessment Id"));
    }
}