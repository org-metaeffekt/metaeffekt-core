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
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.inventory.processor.writer.excel.AbstractXlsInventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

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

    @Test
    public void serializationInventoryReaderWriterEmptyInventoryTest() throws IOException {
        final Inventory initialInventory = new Inventory();

        final File serializedInventoryFile = new File("target/serializationInventoryReaderWriterEmptyInventoryTest.ser");
        new InventoryWriter().writeInventory(initialInventory, serializedInventoryFile);

        final Inventory readInventory = new InventoryReader().readInventory(serializedInventoryFile);
        Assert.assertEquals(0, readInventory.getArtifacts().size());

        cleanUpFiles(serializedInventoryFile);
    }

    @Test
    public void serializationInventoryReaderWriterInventoryWithArtifactsTest() throws IOException {
        final Inventory initialInventory = new Inventory();
        initialInventory.getArtifacts().add(dummyArtifact());

        final File serializedInventoryFile = new File("target/serializationInventoryReaderWriterInventoryWithArtifactsTest.ser");
        new InventoryWriter().writeInventory(initialInventory, serializedInventoryFile);

        final Inventory readInventory = new InventoryReader().readInventory(serializedInventoryFile);
        Assert.assertEquals(1, readInventory.getArtifacts().size());
        Assert.assertEquals("test.jar", readInventory.getArtifacts().get(0).getId());

        cleanUpFiles(serializedInventoryFile);
    }

    @Test
    public void serializationInventoryReaderWriterInventoryWithVulnerabilityContextsTest() throws IOException {
        final Inventory initialInventory = new Inventory();
        initialInventory.getVulnerabilityMetaData("test-context").add(dummyVulnerabilityMetaData());

        final File serializedInventoryFile = new File("target/serializationInventoryReaderWriterInventoryWithArtifactsTest.ser");
        new InventoryWriter().writeInventory(initialInventory, serializedInventoryFile);

        final Inventory readInventory = new InventoryReader().readInventory(serializedInventoryFile);
        Assert.assertEquals(1, readInventory.getVulnerabilityMetaData("test-context").size());
        Assert.assertEquals("CVE-2023-1234", readInventory.getVulnerabilityMetaData("test-context").get(0).get(VulnerabilityMetaData.Attribute.NAME));

        cleanUpFiles(serializedInventoryFile);
    }

    @Test
    public void serializationInventoryReaderWriterInventoryWithVariousDataTest() throws IOException {
        final Inventory initialInventory = new Inventory();
        initialInventory.getVulnerabilityMetaData().add(dummyVulnerabilityMetaData());
        initialInventory.getVulnerabilityMetaData().add(dummyVulnerabilityMetaData());
        initialInventory.getArtifacts().add(dummyArtifact());
        initialInventory.getArtifacts().add(dummyArtifact());
        initialInventory.getInventoryInfo().add(opAndReturn(new InventoryInfo(), i -> i.set("key", "value")));
        initialInventory.getCertMetaData().add(opAndReturn(new CertMetaData(), c -> c.set("key", "value")));

        final File serializedInventoryFile = new File("target/serializationInventoryReaderWriterInventoryWithVariousDataTest.ser");
        new InventoryWriter().writeInventory(initialInventory, serializedInventoryFile);

        final Inventory readInventory = new InventoryReader().readInventory(serializedInventoryFile);
        Assert.assertEquals(2, readInventory.getVulnerabilityMetaData().size());
        Assert.assertEquals("CVE-2023-1234", readInventory.getVulnerabilityMetaData().get(0).get(VulnerabilityMetaData.Attribute.NAME));
        Assert.assertEquals(2, readInventory.getArtifacts().size());
        Assert.assertEquals("test.jar", readInventory.getArtifacts().get(0).getId());
        Assert.assertEquals(1, readInventory.getInventoryInfo().size());
        Assert.assertEquals("value", readInventory.getInventoryInfo().get(0).get("key"));
        Assert.assertEquals(1, readInventory.getCertMetaData().size());
        Assert.assertEquals("value", readInventory.getCertMetaData().get(0).get("key"));


        // serializationContext is transient and not serialized, check if access works correctly
        final File xlsInventoryFile = new File("target/serializationInventoryReaderWriterInventoryWithVariousDataTest.xls");
        new InventoryWriter().writeInventory(readInventory, xlsInventoryFile);

        final Inventory readInventoryXls = new InventoryReader().readInventory(xlsInventoryFile);
        Assert.assertEquals(2, readInventoryXls.getVulnerabilityMetaData().size());

        cleanUpFiles(serializedInventoryFile, xlsInventoryFile);
    }

    @Test
    public void writeVeryLongStringsIntoArtifactTest() throws IOException {
        final String longString = buildLongString(AbstractXlsInventoryWriter.MAX_CELL_LENGTH * 3);

        final Inventory initialInventory = new Inventory();
        final Artifact longTextArtifact = new Artifact();
        longTextArtifact.setId("test.jar");
        longTextArtifact.set("TEST", longString);
        initialInventory.getArtifacts().add(longTextArtifact);

        final File xlsInventoryFile = new File("target/writeVeryLongStringsIntoArtifactTest.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile);

        final Inventory readInventory = new InventoryReader().readInventory(xlsInventoryFile);
        Assert.assertEquals(1, readInventory.getArtifacts().size());
        Assert.assertEquals("test.jar", readInventory.getArtifacts().get(0).getId());
        Assert.assertEquals(longString.trim(), readInventory.getArtifacts().get(0).get("TEST"));

        cleanUpFiles(xlsInventoryFile);
    }

    @Test
    public void newInventoryWriterSystem2Test() throws IOException {
        final String longString = buildLongString(AbstractXlsInventoryWriter.MAX_CELL_LENGTH);

        final Inventory initialInventory = new Inventory();

        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.set("TEST", longString);
        artifact.set("CID-ASSET-ID", "5");
        initialInventory.getArtifacts().add(artifact);

        final File xlsInventoryFile = new File("target/newInventoryWriterSystemTest.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile);
    }

    @Test
    public void newInventoryWriterSystemManualFormatCheckTest() throws IOException {
        final String longString = buildLongString(AbstractXlsInventoryWriter.MAX_CELL_LENGTH + 10);
        final String specialChars = "!@#$%^&*()_+{}:\"<>?";
        final String url = "http://example.com";

        final Inventory initialInventory = new Inventory();
        final InventorySerializationContext serializationContext = initialInventory.getSerializationContext();

        // Populate serializationContext
        serializationContext.put("licensedata.header.[License].fg", "224,153,255");
        serializationContext.put("licensedata.header.[License].width", 50);
        serializationContext.put("licensedata.column.[License].centered", true);

        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.set("TEST", longString);
        artifact.set("CID-ASSET-ID", longString);
        artifact.set("Incomplete Match", "Yes");
        artifact.set("Errors", "None");
        initialInventory.getArtifacts().add(artifact);

        final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
        vmd.set(VulnerabilityMetaData.Attribute.V2_SCORE, "9.8");
        vmd.set(VulnerabilityMetaData.Attribute.V3_SCORE, "9.0");
        vmd.set(VulnerabilityMetaData.Attribute.MAX_SCORE, "9.8");
        vmd.set(VulnerabilityMetaData.Attribute.URL, url);
        initialInventory.getVulnerabilityMetaData().add(vmd);

        final LicenseMetaData lmd = new LicenseMetaData();
        lmd.set("License", "MIT");
        initialInventory.getLicenseMetaData().add(lmd);

        final LicenseData licenseData = new LicenseData();
        licenseData.set("License", "GPL");
        licenseData.set("Version", "3.0");
        initialInventory.getLicenseData().add(licenseData);

        final ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*.jar");
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, specialChars);
        initialInventory.getComponentPatternData().add(cpd);

        final AssetMetaData amd = new AssetMetaData();
        amd.set("SRC-AssetSource", "Source1");
        amd.set("config_setting", "value");
        amd.set(AssetMetaData.Attribute.NAME, "AssetName");
        amd.set(AssetMetaData.Attribute.ASSET_ID, "12345");
        initialInventory.getAssetMetaData().add(amd);

        final ReportData rd = new ReportData();
        rd.set("AssetId", "12345");
        initialInventory.getReportData().add(rd);

        final InventoryInfo info = new InventoryInfo();
        info.set("InfoKey", "InfoValue");
        initialInventory.getInventoryInfo().add(info);

        final CertMetaData cm = new CertMetaData();
        cm.set(CertMetaData.Attribute.NAME, "CERT-SEI-343434");
        cm.set(CertMetaData.Attribute.URL, url);
        initialInventory.getCertMetaData().add(cm);

        final File xlsInventoryFile = new File("target/newInventoryWriterSystemTest.xls");
        new InventoryWriter().writeInventory(initialInventory, xlsInventoryFile);
        final File xlsxInventoryFile = new File("target/newInventoryWriterSystemTest.xlsx");
        new InventoryWriter().writeInventory(initialInventory, xlsxInventoryFile);

        // comment out the line below to preserve the test file for manual inspection
        cleanUpFiles(xlsInventoryFile);
    }

    private static String buildLongString(int minLength) {
        final StringBuilder longStringBuilder = new StringBuilder();
        for (int i = 0; longStringBuilder.length() < minLength; i++) {
            longStringBuilder.append(i).append(" ");
        }
        return longStringBuilder.toString();
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
                // LOG.info("Skipping file deletion: {}", file.getAbsolutePath());
            }
        }
    }

    private <T> T opAndReturn(T t, Consumer<T> consumer) {
        consumer.accept(t);
        return t;
    }
}
