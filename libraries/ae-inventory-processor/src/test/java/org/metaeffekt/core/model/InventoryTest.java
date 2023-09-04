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
package org.metaeffekt.core.model;


import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;

public class InventoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryTest.class);

    @Test
    public void testFindCurrent() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setLicense("L");
        artifact.setComponent("Test Component");
        artifact.setClassification("current");
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.setGroupId("org.test");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setLicense("L");
        candidate.setComponent("Test Component");
        candidate.setGroupId("org.test");
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.0");
        candidate.deriveArtifactId();

        Assert.assertNotNull(inventory.findArtifact(candidate));
        Assert.assertNotNull(inventory.findArtifactClassificationAgnostic(candidate));
    }

    @Test
    public void testFindClassificationAgnostic() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setLicense("L");
        artifact.setComponent("Test Component");
        artifact.setClassification("any");
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.setGroupId("org.test");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setLicense("L");
        candidate.setComponent("Test Component");
        candidate.setClassification("any");
        candidate.setGroupId("org.test");
        candidate.setVersion("1.0.1");
        candidate.setId("test-1.0.1.jar");
        candidate.deriveArtifactId();

        Assert.assertNull(inventory.findArtifact(candidate));
        Assert.assertNotNull(inventory.findArtifactClassificationAgnostic(candidate));
    }

    @Test
    public void testUnderscoreSupport() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test_1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Assert.assertEquals("test", artifact.getArtifactId());

        // while the artifacts are equivalent with respect to GAV details they remain different by id.
        Assert.assertNull(inventory.findArtifact("test-1.0.0.jar"));
        Assert.assertNotNull(inventory.findArtifact("test_1.0.0.jar"));
    }

    @Test
    public void testWildcardMatch() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-" + ASTERISK + ".jar");
        artifact.setVersion(ASTERISK);
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), ASTERISK);
    }


    @Test
    public void testSpecificWildcardMatch() {
        Inventory inventory = new Inventory();

        final Artifact artifact1 = new Artifact();
        artifact1.setId("test-" + ASTERISK + ".jar");
        artifact1.setVersion(ASTERISK);
        artifact1.deriveArtifactId();

        final Artifact artifact2 = new Artifact();
        artifact2.setId("test-lib-" + ASTERISK + ".jar");
        artifact2.setVersion(ASTERISK);
        artifact2.deriveArtifactId();

        inventory.getArtifacts().add(artifact1);
        inventory.getArtifacts().add(artifact2);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-lib-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getId(), "test-lib-" + ASTERISK + ".jar");
    }

    @Test
    public void testMatch_withVersion() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "1.0.0");
    }

    @Test
    public void testMatch_withoutVersion() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "1.0.0");
    }

    @Test
    public void testMatch_withChecksumDifference() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setChecksum("A");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.0");
        candidate.setChecksum("B");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifference() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.1");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifferencePlaceholder() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.setVersion("${PROJECTVERSION}");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test.jar");
        candidate.setVersion("1");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifferencePlaceholderBoth() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.setVersion("${PROJECTVERSION}");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "${PROJECTVERSION}");
    }

    @Test
    public void testWildcardMatch_AsteriskAsPrefix() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId(ASTERISK + "test.jar");
        artifact.setVersion(ASTERISK);
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("1.0.0.test.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), ASTERISK);
    }

    @Test
    public void vulnerabilityMetaDataContextTest() throws IOException {
        Inventory inventory = new Inventory();
        VulnerabilityMetaData vulnerabilityMetaData = new VulnerabilityMetaData();
        vulnerabilityMetaData.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2017-1234");
        vulnerabilityMetaData.set(VulnerabilityMetaData.Attribute.URL, " ");
        inventory.getVulnerabilityMetaData().add(vulnerabilityMetaData);

        Assert.assertNotNull(inventory.getVulnerabilityMetaData());
        Assert.assertEquals(inventory.getVulnerabilityMetaData().size(), 1);
        Assert.assertEquals(inventory.getVulnerabilityMetaData().get(0), vulnerabilityMetaData);

        VulnerabilityMetaData vulnerabilityMetaDataTestContext = new VulnerabilityMetaData();
        vulnerabilityMetaDataTestContext.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2018-1234");
        vulnerabilityMetaDataTestContext.set(VulnerabilityMetaData.Attribute.URL, " ");
        inventory.getVulnerabilityMetaData("test").add(vulnerabilityMetaDataTestContext);

        Assert.assertNotNull(inventory.getVulnerabilityMetaData("test"));
        Assert.assertEquals(1, inventory.getVulnerabilityMetaData("test").size());
        Assert.assertEquals(vulnerabilityMetaDataTestContext, inventory.getVulnerabilityMetaData("test").get(0));

        Assert.assertEquals(vulnerabilityMetaData, inventory.getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT).get(0));

        Assert.assertEquals(0, inventory.getVulnerabilityMetaData("test2").size());

        final File xlsInventoryFile = new File("target/vulnerabilityMetaDataContextTest.xls");
        LOG.info("Writing " + xlsInventoryFile);
        long ts = System.currentTimeMillis();
        new InventoryWriter().writeInventory(inventory, xlsInventoryFile);
        LOG.info("Done in " + (System.currentTimeMillis() - ts) + " ms");

        final File xlsxInventoryFile = new File("target/vulnerabilityMetaDataContextTest.xlsx");
        LOG.info("Writing " + xlsxInventoryFile);
        ts = System.currentTimeMillis();
        new InventoryWriter().writeInventory(inventory, xlsxInventoryFile);
        LOG.info("Done in " + (System.currentTimeMillis() - ts) + " ms");

        // XLS
        inventory = new InventoryReader().readInventory(xlsInventoryFile);

        Assert.assertNotNull(inventory.getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT));
        Assert.assertEquals(1, inventory.getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT).size());

        // methods that used to access the VMD via the getVulnerabilityMetaData() method, without a context
        inventory.getFilteredInventory();
        new Inventory().inheritVulnerabilityMetaData(inventory, false);
        inventory.filterVulnerabilityMetaData();

        // XLXS
        inventory = new InventoryReader().readInventory(xlsxInventoryFile);

        Assert.assertNotNull(inventory.getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT));
        Assert.assertEquals(1, inventory.getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT).size());

        // methods that used to access the VMD via the getVulnerabilityMetaData() method, without a context
        inventory.getFilteredInventory();
        new Inventory().inheritVulnerabilityMetaData(inventory, false);
        inventory.filterVulnerabilityMetaData();

        cleanUpFiles(xlsInventoryFile, xlsxInventoryFile);
    }

    @Test
    public void writeAndReadInventoryWithArtifactsTest() throws IOException {
        final Inventory initialInventory = new Inventory();
        initialInventory.getArtifacts().add(dummyArtifact());

        final File xlsInventoryFile = new File("target/writeAndReadInventoryWithArtifactsTest.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile);

        final Inventory readInventoryXls = new InventoryReader().readInventory(xlsInventoryFile);
        Assert.assertEquals(1, readInventoryXls.getArtifacts().size());

        final File xlsxInventoryFile = new File("target/writeAndReadInventoryWithArtifactsTest.xlsx");
        new InventoryWriter().writeInventory(initialInventory, xlsxInventoryFile);

        final Inventory readInventoryXlsx = new InventoryReader().readInventory(xlsxInventoryFile);
        Assert.assertEquals(1, readInventoryXlsx.getArtifacts().size());


        initialInventory.getVulnerabilityMetaData().add(dummyVulnerabilityMetaData());

        final File xlsInventoryFile2 = new File("target/writeAndReadInventoryWithArtifactsTest2.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile2);

        final Inventory readInventoryXls2 = new InventoryReader().readInventory(xlsInventoryFile2);
        Assert.assertEquals(1, readInventoryXls2.getVulnerabilityMetaData().size());
        Assert.assertEquals(1, readInventoryXls2.getArtifacts().size());

        final File xlsxInventoryFile2 = new File("target/writeAndReadInventoryWithArtifactsTest2.xlsx");
        new InventoryWriter().writeInventory(initialInventory, xlsxInventoryFile2);

        final Inventory readInventoryXlsx2 = new InventoryReader().readInventory(xlsxInventoryFile2);
        Assert.assertEquals(1, readInventoryXlsx2.getVulnerabilityMetaData().size());
        Assert.assertEquals(1, readInventoryXlsx2.getArtifacts().size());

        cleanUpFiles(xlsInventoryFile, xlsxInventoryFile, xlsInventoryFile2, xlsxInventoryFile2);
    }

    @Test
    public void writeAndReadInventoryWithoutArtifactsTest() throws IOException {
        final Inventory initialInventory = new Inventory();

        final File xlsInventoryFile = new File("target/writeAndReadInventoryWithoutArtifactsTest.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile);

        final Inventory readInventoryXls = new InventoryReader().readInventory(xlsInventoryFile);
        Assert.assertEquals(0, readInventoryXls.getArtifacts().size());
        Assert.assertEquals(0, readInventoryXls.getVulnerabilityMetaData().size());

        final File xlsxInventoryFile = new File("target/writeAndReadInventoryWithoutArtifactsTest.xlsx");
        new InventoryWriter().writeInventory(initialInventory, xlsxInventoryFile);

        final Inventory readInventoryXlsx = new InventoryReader().readInventory(xlsxInventoryFile);
        Assert.assertEquals(0, readInventoryXlsx.getArtifacts().size());
        Assert.assertEquals(0, readInventoryXlsx.getVulnerabilityMetaData().size());

        initialInventory.getVulnerabilityMetaData().add(dummyVulnerabilityMetaData());

        final File xlsInventoryFile2 = new File("target/writeAndReadInventoryWithoutArtifactsTest2.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile2);

        final Inventory readInventoryXls2 = new InventoryReader().readInventory(xlsInventoryFile2);
        Assert.assertEquals(1, readInventoryXls2.getVulnerabilityMetaData().size());

        final File xlsxInventoryFile2 = new File("target/writeAndReadInventoryWithoutArtifactsTest2.xlsx");
        new InventoryWriter().writeInventory(initialInventory, xlsxInventoryFile2);

        final Inventory readInventoryXlsx2 = new InventoryReader().readInventory(xlsxInventoryFile2);
        Assert.assertEquals(1, readInventoryXlsx2.getVulnerabilityMetaData().size());

        cleanUpFiles(xlsInventoryFile, xlsxInventoryFile, xlsInventoryFile2, xlsxInventoryFile2);
    }

    private Artifact dummyArtifact() {
        Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        return artifact;
    }

    private VulnerabilityMetaData dummyVulnerabilityMetaData() {
        VulnerabilityMetaData vulnerabilityMetaData = new VulnerabilityMetaData();
        vulnerabilityMetaData.set(VulnerabilityMetaData.Attribute.NAME, "CVE-2023-1234");
        vulnerabilityMetaData.set(VulnerabilityMetaData.Attribute.URL, " ");
        return vulnerabilityMetaData;
    }

    private void cleanUpFiles(File... files) {
        for (File file : files) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
