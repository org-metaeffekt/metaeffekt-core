/**
 * Copyright 2009-2017 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;

/**
 * Created by kklein on 16.06.17.
 */
public class InheritInventoryProcessorTest {

    @Test
    public void testAddArtifact() {
        final Inventory inputInventory = new Inventory();
        final Inventory inventory = new Inventory();

        Artifact artifact10 = createTestArtifact("1.0");
        Artifact artifact11 = createTestArtifact("1.1");

        inputInventory.getArtifacts().add(artifact10);
        inventory.getArtifacts().add(artifact11);

        // validate precondition
        Assert.assertNotNull(inputInventory.findArtifact("test-1.0.jar"));
        Assert.assertNull(inputInventory.findArtifact("test-1.1.jar"));
        Assert.assertNull(inventory.findArtifact("test-1.0.jar"));
        Assert.assertNotNull(inventory.findArtifact("test-1.1.jar"));

        final InheritInventoryProcessor processor = createInheritInventoryProcessor(inputInventory);
        processor.process(inventory);

        // validate post-condition
        Assert.assertNotNull(inventory.findArtifact("test-1.0.jar"));
        Assert.assertNotNull(inventory.findArtifact("test-1.1.jar"));
    }

    @Test
    public void testOverwriteArtifact() {
        final Inventory inputInventory = new Inventory();
        final Inventory inventory = new Inventory();

        Artifact artifact10 = createTestArtifact("1.0");
        Artifact artifact10Overwrite = createTestArtifact("1.0");
        artifact10Overwrite.setLicense("MIT License");

        inputInventory.getArtifacts().add(artifact10);
        inventory.getArtifacts().add(artifact10Overwrite);

        // validate precondition
        Assert.assertNotNull(inputInventory.findArtifact("test-1.0.jar"));
        Assert.assertEquals("Apache License 2.0", inputInventory.findArtifact("test-1.0.jar").getLicense());
        Assert.assertNotNull(inventory.findArtifact("test-1.0.jar"));
        Assert.assertEquals("MIT License", inventory.findArtifact("test-1.0.jar").getLicense());

        final InheritInventoryProcessor processor = createInheritInventoryProcessor(inputInventory);
        processor.process(inventory);

        // validate post-condition
        Assert.assertNotNull(inventory.findArtifact("test-1.0.jar"));
        Assert.assertEquals("MIT License", inventory.findArtifact("test-1.0.jar").getLicense());
    }

    @Test
    public void testAddLicenseMetaData() {
        final Inventory inputInventory = new Inventory();
        final Inventory inventory = new Inventory();

        LicenseMetaData licenseMetaData10 = createTestLicenseMetaData("1.0");
        LicenseMetaData licenseMetaData11 = createTestLicenseMetaData("1.1");

        inputInventory.getLicenseMetaData().add(licenseMetaData10);
        inventory.getLicenseMetaData().add(licenseMetaData11);

        // validate precondition
        Assert.assertNotNull(inputInventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertNull(inputInventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.1"));
        Assert.assertNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertNotNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.1"));

        final InheritInventoryProcessor processor = createInheritInventoryProcessor(inputInventory);
        processor.process(inventory);

        // validate post-condition
        Assert.assertNotNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertNotNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.1"));
    }

    @Test
    public void testOverwriteLicenseMetaData() {
        final Inventory inputInventory = new Inventory();
        final Inventory inventory = new Inventory();

        LicenseMetaData licenseMetaData10 = createTestLicenseMetaData("1.0");
        LicenseMetaData licenseMetaData10Overwrite = createTestLicenseMetaData("1.0");
        licenseMetaData10Overwrite.setNotice("Overwritten");

        inputInventory.getLicenseMetaData().add(licenseMetaData10);
        inventory.getLicenseMetaData().add(licenseMetaData10Overwrite);

        // validate precondition
        Assert.assertNotNull(inputInventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertNotNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertEquals("Test support license notices.", inputInventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0").getNotice());
        Assert.assertEquals("Overwritten", inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0").getNotice());

        final InheritInventoryProcessor processor = createInheritInventoryProcessor(inputInventory);
        processor.process(inventory);

        // validate post-condition
        Assert.assertNotNull(inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0"));
        Assert.assertEquals("Overwritten", inventory.findMatchingLicenseMetaData("Test Support", "MIT License", "1.0").getNotice());
    }

    private InheritInventoryProcessor createInheritInventoryProcessor(final Inventory inputInventory) {
        return new InheritInventoryProcessor() {
            @Override
            protected Inventory loadInputInventory() {
                return inputInventory;
            }
        };
    }

    private Artifact createTestArtifact(String version) {
        DefaultArtifact artifact = new DefaultArtifact();
        artifact.setId("test-" + version + ".jar");
        artifact.setGroupId("org.test");
        artifact.setVersion(version);
        artifact.setClassification("current");
        artifact.setComment("Test Comment");
        artifact.setLicense("Apache License 2.0");
        artifact.setComponent("Test Component");
        return artifact;
    }

    private LicenseMetaData createTestLicenseMetaData(String version) {
        LicenseMetaData licenseMetaData = new LicenseMetaData();
        licenseMetaData.setName("MIT License");
        licenseMetaData.setComponent("Test Support");
        licenseMetaData.setComment("Test support comment");
        licenseMetaData.setNotice("Test support license notices.");
        licenseMetaData.setVersion(version);
        return licenseMetaData;
    }

}
