/*
 * Copyright 2009-2021 the original author or authors.
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
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryReportTest {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryReportTest.class);

    private static final File INVENTORY_DIR = new File("src/test/resources/test-inventory-01");
    private static final String INVENTORY_INCLUDES = "*.xls";
    private static final String LICENSES_PATH = "licenses";
    private static final String COMPONENTS_PATH = "components";

    public static final String UTF_8 = "UTF-8";

    @Test
    public void testInventoryReport() throws Exception {

        File inventoryDir = INVENTORY_DIR;
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport();

        report.setReportContext(new ReportContext("test", "Test", "Test Context"));

        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setReferenceInventoryDir(inventoryDir);
        report.setReferenceInventoryIncludes(inventoryIncludes);
        report.setReferenceLicensePath(LICENSES_PATH);
        report.setReferenceComponentPath(COMPONENTS_PATH);

        report.setInventory(InventoryUtils.readInventory(inventoryDir, inventoryIncludes));

        PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
        artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
        report.setArtifactFilter(artifactFilter);

        File target = new File("target/test-inventory-01");
        target.mkdirs();

        File targetReportPath = new File(target, "report");
        targetReportPath.mkdirs();

        File licenseReport = new File(targetReportPath, "tpc_inventory-licenses.dita");
        File componentReport = new File(targetReportPath, "tpc_inventory-component-report.dita");
        File noticeReport = new File(targetReportPath, "tpc_inventory-notices.dita");
        File artifactReport = new File(targetReportPath, "tpc_inventory-artifact-report.dita");

        final File targetLicensesDir = new File(target, "licenses");
        final File targetComponentDir = new File(target, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);
        report.setTargetReportDir(targetReportPath);

        report.setInventoryBomReportEnabled(true);
        report.setInventoryPomEnabled(true);
        report.setInventoryDiffReportEnabled(false);
        report.setInventoryVulnerabilityReportEnabled(true);

        final boolean valid = report.createReport();

        assertTrue(valid);
        assertTrue(targetLicensesDir.exists());

        // check first-level license folders are created as expected
        assertTrue(new File(targetLicensesDir, "A-License").exists());
        assertTrue(new File(targetLicensesDir, "B-License").exists());
        assertTrue(new File(targetLicensesDir, "D-License").exists());
        assertFalse(new File(targetLicensesDir, "T-License").exists());

        // check multiple licensed artifacts are multiplied to the different license folders
        assertTrue(new File(targetLicensesDir, "A-License/AlphaBeta-Component-1.0.0").exists());
        assertTrue(new File(targetComponentDir, "AlphaBeta-Component-1.0.0").exists());

        // check license information is multiplied for sub-components
        assertTrue(new File(targetLicensesDir, "A-License/Gamma-Component-1.0.0").exists());
        assertTrue(new File(targetLicensesDir, "B-License/Gamma-Component-1.0.0").exists());
        assertTrue(new File(targetComponentDir, "Gamma-Component-1.0.0").exists());

        // check license information is multiplied for sub-components
        assertTrue(new File(targetLicensesDir, "A-License/Omega-Component").exists());
        assertTrue(new File(targetLicensesDir, "B-License/Omega-Component").exists());
        assertTrue(new File(targetComponentDir, "Omega-Component").exists());

        assertTrue(new File(targetComponentDir, "Sigma-Component-1.0.0").exists());

        // check generated DITA files contain the appropriate details
        String notices = FileUtils.readFileToString(noticeReport, UTF_8);
        assertTrue(notices.contains("Notice for Alpha component licensed under A License."));
        assertTrue(notices.contains("Notice for Beta component licensed under B License."));
        assertTrue(notices.contains("Notice for AlphaBeta component licensed under either A License or B License. A License is selected for this distribution."));
        assertTrue(notices.contains("Notice for Gamma component, which contains sub-components licensed under A License and B License."));

        List<ArtifactLicenseData> artifactLicenseData = report.getLastProjectInventory().evaluateNotices("A License");
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Alpha Component")));
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Gamma Component")));
        assertFalse(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Beta Component")));

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("B License");
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Beta Component")));
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Gamma Component")));
        assertFalse(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Alpha Component")));

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("D License");
        assertTrue(artifactLicenseData.isEmpty());

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("G License (with sub-components)");
        assertTrue(artifactLicenseData.isEmpty());
    }

    @Test
    public void testStringEscaping() {

        InventoryReport inventoryReport = new InventoryReport();

        Assert.assertEquals("this&amp;that.&#8203;those-&#8203;these_&#8203;which",
                inventoryReport.xmlEscapeArtifactId("this&that.those-these_which"));

        Assert.assertEquals("this#&#8203;that",
                inventoryReport.xmlEscapeArtifactId("this#that"));

        Assert.assertEquals("&nbsp;", inventoryReport.xmlEscapeArtifactId(null));
    }

    @Test
    public void testComponentPatterns() throws IOException {
        File inventoryFile = new File(INVENTORY_DIR, "artifact-inventory.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        Assert.assertNotNull(inventory.getComponentPatternData());
        Assert.assertEquals(6, inventory.getComponentPatternData().size());

        Assert.assertEquals("org/metaeffekt/core/**/*-org/metaeffekt.core/Inventory.class-ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).deriveQualifier());
        Assert.assertEquals("org/metaeffekt/core/**/*::metaeffekt Core:org/metaeffekt/core Classes:0.21.0:org/metaeffekt.core/Inventory.class:ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).createCompareStringRepresentation());

        File targetFile = new File("target/test-inventory.xls");
        new InventoryWriter().writeInventory(inventory, targetFile);
    }

    @Test
    public void testVulnerabilityMetaData() throws IOException {
        File inventoryFile = new File(INVENTORY_DIR, "artifact-inventory.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        Assert.assertNotNull(inventory.getVulnerabilityMetaData());
        Assert.assertEquals(16, inventory.getVulnerabilityMetaData().size());

        List<VulnerabilityMetaData> applicableVulnerabilities = VulnerabilityMetaData.filterApplicableVulnerabilities(inventory.getVulnerabilityMetaData(), 7.0f);
        Assert.assertEquals(3, applicableVulnerabilities.size());

        LOG.info("Applicable:");
        applicableVulnerabilities.stream()
                .map(vmd -> vmd.get(VulnerabilityMetaData.Attribute.NAME) + " " + vmd.get(VulnerabilityMetaData.Attribute.MAX_SCORE))
                .forEach(LOG::info);

        List<VulnerabilityMetaData> notApplicableVulnerabilities = VulnerabilityMetaData.filterNotApplicableVulnerabilities(inventory.getVulnerabilityMetaData(), 7.0f);
        Assert.assertEquals(10, notApplicableVulnerabilities.size());

        List<VulnerabilityMetaData> insignificantVulnerabilities = VulnerabilityMetaData.filterInsignificantVulnerabilities(inventory.getVulnerabilityMetaData(), 7.0f);
        Assert.assertEquals(3, insignificantVulnerabilities.size());

        LOG.info("Not Applicable:");
        notApplicableVulnerabilities.stream()
                .map(vmd -> vmd.get(VulnerabilityMetaData.Attribute.NAME) + " " + vmd.get(VulnerabilityMetaData.Attribute.MAX_SCORE))
                .forEach(LOG::info);

        File targetFile = new File("target/test-inventory.xls");
        new InventoryWriter().writeInventory(inventory, targetFile);
    }

    @Test
    public void testAntPatternMatcher() {
        String path = "/Users/kklein/workspace/spring-boot-example/documentation/spring-boot-war/target/bomscan/spring-boot-sample-war-1.5.4.RELEASE-war/org/springframework/boot/loader/LaunchedURLClassLoader.class";
        AntPathMatcher matcher = new AntPathMatcher();
        Assert.assertTrue(matcher.match("/**/org/springframework/boot/loader/**/*", path));
    }

    @Test
    public void testCreateTestReport() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-02");
        final File reportDir = new File("target/test-inventory-02");
        createReport(inventoryDir, "*.xls", reportDir);

        // read package report (effective)
        File packageReportEffectiveFile = new File(reportDir, "report/tpc_inventory-package-report-effective.dita");
        String packageReportEffective = FileUtils.readFileToString(packageReportEffectiveFile, FileUtils.ENCODING_UTF_8);

        // check links from package report
        Assert.assertTrue(
                "Expecting references to license chapter.",
                packageReportEffective.contains("<xref href=\"tpc_inventory-licenses.dita#tpc_effective_license_gnu-general-public-license-3.0\""));

        // read/write inventory
        Inventory inventory = InventoryUtils.readInventory(inventoryDir, "*.xls");
        new InventoryWriter().writeInventory(inventory, new File(reportDir, "output_artifact-inventory.xls"));

        // read rewritten
        Inventory rereadInventory = new InventoryReader().readInventory(new File(reportDir, "output_artifact-inventory.xls"));

        // check selected data in reread inventory
        Assert.assertEquals("GPL-2.0", rereadInventory.
                findMatchingLicenseData("GNU General Public License 2.0").get(LicenseData.Attribute.ID));
    }

    @Test
    public void testCreateTestReport003() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-03");
        final File reportDir = new File("target/test-inventory-03");

        final InventoryReport report = new InventoryReport();
        report.setFailOnMissingLicense(false);
        report.setFailOnMissingLicenseFile(false);
        //report.addVulnerabilityAdvisoryFilter("CERT-FR"); // this also filters out the 'void' vulnerability
        report.addGenerateOverviewTablesForAdvisories("CERT-FR", "CERT-SEI", "MSRC");
        report.setOverviewTablesVulnerabilityStatusMappingFunction("abstracted");
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

        report.setInventoryVulnerabilityReportSummaryEnabled(true);

        createReport(inventoryDir, "*.xls", reportDir, report);

        // put asserts here


    }

    @Ignore // needs external resources
    @Test
    public void testCreateTestReport004() throws Exception {
        final File inventoryDir = new File("XXX");
        final File reportDir = new File("target/test-inventory-04");

        InventoryReport report = new InventoryReport();
        prepareReport(inventoryDir, "*.xls", reportDir, report);

        report.setInventoryBomReportEnabled(false);

        report.setAssetBomReportEnabled(true);

        report.setInventoryVulnerabilityReportEnabled(true);
        report.setInventoryVulnerabilityReportSummaryEnabled(true);

        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

        report.setOverviewTablesVulnerabilityStatusMappingFunction("abstracted");

        report.setVulnerabilityScoreThreshold(0.6f);

        report.createReport();
    }

    @Test
    public void testCreateTestReportCertMetaData() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-cert");
        final File reportDir = new File("target/test-inventory-cert");

        final InventoryReport report = new InventoryReport();
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);
        createReport(inventoryDir, "*.xls", reportDir);

        // put asserts here


    }


    private boolean createReport(File inventoryDir, String inventoryIncludes, File reportTarget) throws Exception {
        return createReport(inventoryDir, inventoryIncludes, reportTarget, new InventoryReport());
    }

    private boolean createReport(File inventoryDir, String inventoryIncludes, File reportTarget, InventoryReport report) throws Exception {

        prepareReport(inventoryDir, inventoryIncludes, reportTarget, report);

        return report.createReport();
    }

    private void prepareReport(File inventoryDir, String inventoryIncludes, File reportTarget, InventoryReport report) throws IOException {
        report.setReportContext(new ReportContext("test", "Test", "Test Context"));

        report.setInventoryBomReportEnabled(true);
        report.setInventoryVulnerabilityReportEnabled(true);
        report.setAssetBomReportEnabled(true);

        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setReferenceInventoryDir(inventoryDir);
        report.setReferenceInventoryIncludes(inventoryIncludes);
        report.setReferenceLicensePath(new File(inventoryDir, "licenses").getAbsolutePath());
        report.setReferenceComponentPath(new File(inventoryDir, "components").getAbsolutePath());

        report.setInventory(InventoryUtils.readInventory(inventoryDir, inventoryIncludes));

        report.setTargetReportDir(new File(reportTarget, "report"));

        reportTarget.mkdirs();

        final File targetLicensesDir = new File(reportTarget, "licenses");
        final File targetComponentDir = new File(reportTarget, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);
    }

    @Ignore
    @Test
    public void testCreateTestReport_External() throws Exception {
        final File inventoryDir = new File("<path-to-inventory>");
        final File reportDir = new File("target/test-inventory-external");
        createReport(inventoryDir, "*.xls", reportDir);

        // read package report (effective)
        File packageReportEffectiveFile = new File(reportDir, "report/tpc_inventory-package-report-effective.dita");
        String packageReportEffective = FileUtils.readFileToString(packageReportEffectiveFile, FileUtils.ENCODING_UTF_8);

        // check links from package report
        Assert.assertTrue(
                "Expecting references to license chapter.",
                packageReportEffective.contains("<xref href=\"tpc_inventory-licenses.dita#tpc_effective_license_gnu-general-public-license-3.0\""));

        // read/write inventory
        Inventory inventory = InventoryUtils.readInventory(inventoryDir, "*.xls");
        new InventoryWriter().writeInventory(inventory, new File(reportDir, "output_artifact-inventory.xls"));

        // read rewritten
        Inventory rereadInventory = new InventoryReader().readInventory(new File(reportDir, "output_artifact-inventory.xls"));

        // check selected data in reread inventory
        Assert.assertEquals("GPL-2.0", rereadInventory.
                findMatchingLicenseData("GNU General Public License 2.0").get(LicenseData.Attribute.ID));
    }

}
