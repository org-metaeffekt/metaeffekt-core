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
package org.metaeffekt.core.inventory.processor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.ArtifactLicenseData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
    public void testCreateTestReport001() throws Exception {

        File inventoryDir = INVENTORY_DIR;
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());

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

        File  targetReportPath = new File(target, "report");
        targetReportPath.mkdirs();

        File licenseReport = new File(targetReportPath, "tpc_inventory-licenses.dita");
        File componentReport = new File(targetReportPath, "tpc_inventory-component-report.dita");
        File noticeReport = new File(targetReportPath, "tpc_inventory-component-license-details.dita");
        File artifactReport = new File(targetReportPath, "tpc_inventory-artifact-report.dita");

        final File targetLicensesDir = new File(target, "licenses");
        final File targetComponentDir = new File(target, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);
        report.setTargetReportDir(targetReportPath);

        report.setAssessmentReportEnabled(true);
        report.setAssetBomReportEnabled(true);
        report.setInventoryBomReportEnabled(true);
        report.setInventoryPomEnabled(true);
        report.setInventoryDiffReportEnabled(false);
        report.setInventoryVulnerabilityReportEnabled(true);
        report.setInventoryVulnerabilityReportSummaryEnabled(true);
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

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

        // copy bookmap
        FileUtils.copyFileToDirectory(new File("src/test/resources/test-inventory-01/bm_test.ditamap"), target);

        // generate PDF using 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISBALED -Ddita.source.dir=target/test-inventory-01' from terminal

    }

    @Test
    public void testStringEscaping() {

        InventoryReport inventoryReport = new InventoryReport(ReportConfigurationParameters.builder().build());

        Assert.assertEquals("this&amp;that.&#8203;those-&#8203;these_&#8203;which",
                inventoryReport.xmlEscapeArtifactId("this&that.those-these_which"));

        Assert.assertEquals("this#&#8203;that",
                inventoryReport.xmlEscapeArtifactId("this#that"));

        Assert.assertEquals("&nbsp;", inventoryReport.xmlEscapeArtifactId(null));
    }

    @Test
    public void testComponentPatterns() throws IOException {
        File inventoryFile = new File(INVENTORY_DIR, "artifact-inventory-01.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        Assert.assertNotNull(inventory.getComponentPatternData());
        Assert.assertEquals(6, inventory.getComponentPatternData().size());

        Assert.assertEquals("org/metaeffekt/core/**/*-org/metaeffekt.core/Inventory.class-ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).deriveQualifier());
        Assert.assertEquals("org/metaeffekt/core/**/*::metaeffekt Core:org/metaeffekt/core Classes:0.21.0:org/metaeffekt.core/Inventory.class:ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).createCompareStringRepresentation());

        File targetFile = new File("target/test-inventory.xls");
        new InventoryWriter().writeInventory(inventory, targetFile);
    }

    @Test
    public void testAntPatternMatcher() {
        String path = "/spring-boot-example/documentation/spring-boot-war/target/bomscan/spring-boot-sample-war-1.5.4.RELEASE-war/org/springframework/boot/loader/LaunchedURLClassLoader.class";
        AntPathMatcher matcher = new AntPathMatcher();
        Assert.assertTrue(matcher.match("/**/org/springframework/boot/loader/**/*", path));
    }

    @Test
    public void testCreateTestReport002() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-02");
        final File reportDir = new File("target/test-inventory-02");

        configureAndCreateReport(inventoryDir, "*.xls",
                inventoryDir, "*.xls",
                reportDir, new InventoryReport(ReportConfigurationParameters.builder().build()));

        // read package report (effective)
        File packageReportEffectiveFile = new File(reportDir, "report/tpc_inventory-package-report-effective.dita");
        String packageReportEffective = FileUtils.readFileToString(packageReportEffectiveFile, FileUtils.ENCODING_UTF_8);

        // check links from package report
        Assert.assertTrue(
                "Expecting references to license chapter.",
                packageReportEffective.contains
                        ("<xref href=\"tpc_inventory-license-usage.dita#tpc_effective_license_gnu-general-public-license-3.0\""));

        // read license overview
        File licenseOverviewFile = new File(reportDir, "report/tpc_inventory-licenses-effective.dita");
        String licenseOverview = FileUtils.readFileToString(licenseOverviewFile, FileUtils.ENCODING_UTF_8);
        Assert.assertFalse("All artifacts counts must be greater than 0.", licenseOverview.contains("<codeph>0</codeph>"));

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
    public void testCreateTestReport002_ILD_DE() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-02");
        final File reportDir = new File("target/test-inventory-02_ILD_DE");

        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());

        prepareReport(inventoryDir, "*.xls", inventoryDir, "*.xls", reportDir, report);

        report.setAssessmentReportEnabled(false);
        report.setInventoryBomReportEnabled(false);
        report.setInventoryDiffReportEnabled(false);
        report.setInventoryVulnerabilityReportEnabled(false);
        report.setInventoryVulnerabilityReportSummaryEnabled(false);
        report.setInventoryVulnerabilityStatisticsReportEnabled(false);

        report.setTemplateLanguageSelector("de");

        report.createReport();

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

        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());
        report.setFailOnMissingLicense(false);
        report.setFailOnMissingLicenseFile(false);

        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

        report.addGenerateOverviewTablesForAdvisories(AeaaAdvisoryTypeStore.ANY_ADVISORY_FILTER_WILDCARD);
        report.getSecurityPolicy()
                // .setIncludeVulnerabilitiesWithAdvisoryProviders(Collections.singletonList("GHSA"))
                .setVulnerabilityStatusDisplayMapper(CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED);

        report.setInventoryVulnerabilityReportSummaryEnabled(true);

        configureAndCreateReport(inventoryDir, "*.xls",
                inventoryDir, "*.xls",
                reportDir, report);

        // put asserts here

    }

    @Test
    public void testCreateTestReport004() throws Exception {
        File target = new File("target/test-inventory-04");
        target.mkdirs();

        // copy bookmap to enable PDF generation testing
        FileUtils.copyFileToDirectory(new File("src/test/resources/test-inventory-01/bm_test.ditamap"), target);

        File inventoryDir = new File("src/test/resources/test-inventory-04");
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());

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

        File targetReportPath = new File(target, "report");
        targetReportPath.mkdirs();

        final File targetLicensesDir = new File(target, "licenses");
        final File targetComponentDir = new File(target, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);
        report.setTargetReportDir(targetReportPath);

        report.setAssetBomReportEnabled(true);
        report.setInventoryBomReportEnabled(true);
        report.setInventoryPomEnabled(true);
        report.setInventoryDiffReportEnabled(false);
        report.setInventoryVulnerabilityReportEnabled(true);
        report.setInventoryVulnerabilityReportSummaryEnabled(true);

        report.setFailOnMissingLicenseFile(false);

        final boolean valid = report.createReport();
        assertTrue(valid);

        // generate PDF using the following command from terminal:
        // 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISABLED -Ddita.source.dir=target/test-inventory-04'
    }

    @Test
    public void testCreateTestReport05() throws IOException {
        File inventoryDir = new File("src/test/resources/test-inventory-05/");
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .hidePriorityInformation(false)
                .build());

        report.setTemplateLanguageSelector("en");

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

        File target = new File("target/test-inventory-05");
        target.mkdirs();

        File  targetReportPath = new File(target, "report");
        targetReportPath.mkdirs();

        File licenseReport = new File(targetReportPath, "tpc_inventory-licenses.dita");
        File componentReport = new File(targetReportPath, "tpc_inventory-component-report.dita");
        File noticeReport = new File(targetReportPath, "tpc_inventory-component-license-details.dita");
        File artifactReport = new File(targetReportPath, "tpc_inventory-artifact-report.dita");

        report.setTargetLicenseDir(new File("licenses"));
        report.setTargetComponentDir(new File("components"));
        report.setTargetReportDir(targetReportPath);

        report.setAssessmentReportEnabled(true);
        report.setAssetBomReportEnabled(true);
        report.setInventoryBomReportEnabled(true);
        report.setInventoryPomEnabled(true);
        report.setInventoryDiffReportEnabled(true);
        report.setInventoryVulnerabilityReportEnabled(true);
        report.setInventoryVulnerabilityReportSummaryEnabled(true);
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

        final boolean valid = report.createReport();

        // copy bookmap
        FileUtils.copyFileToDirectory(new File("src/test/resources/test-inventory-05/bm_test.ditamap"), target);

        // generate PDF using the following command from terminal:
        // 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISABLED -Ddita.source.dir=target/test-inventory-05'
    }

    @Test
    public void testCreateTestReportCertMetaData() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-cert");
        final File reportDir = new File("target/test-inventory-cert");

        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);

        configureAndCreateReport(inventoryDir, "*.xls", inventoryDir, "*.xls", reportDir,
                new InventoryReport(ReportConfigurationParameters.builder().build()));

        // put asserts here


    }

    private boolean configureAndCreateReport(File inventoryDir, String inventoryIncludes,
                                             File referenceInventoryDir, String referenceInventoryIncludes,
                                             File reportTarget, InventoryReport report) throws Exception {

        prepareReport(inventoryDir, inventoryIncludes,
                referenceInventoryDir, referenceInventoryIncludes,
                reportTarget, report);

        return report.createReport();
    }

    private void prepareReport(File inventoryDir, String inventoryIncludes,
                               File referenceInventoryDir, String referenceInventoryIncludes,
                               File reportTarget, InventoryReport report) throws IOException {
        report.setReportContext(new ReportContext("test", "Test", "Test Context"));

        report.setInventoryBomReportEnabled(false);
        report.setInventoryVulnerabilityReportEnabled(false);
        report.setInventoryVulnerabilityStatisticsReportEnabled(true);
        report.setAssetBomReportEnabled(false);
        report.setAssessmentReportEnabled(true);

        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);

        report.setReferenceInventoryDir(referenceInventoryDir);
        report.setReferenceInventoryIncludes(referenceInventoryIncludes);

        report.setReferenceLicensePath(new File(inventoryDir, "licenses").getAbsolutePath());
        report.setReferenceComponentPath(new File(inventoryDir, "components").getAbsolutePath());

        report.setInventory(InventoryUtils.readInventory(inventoryDir, inventoryIncludes));

        report.setTargetReportDir(new File(reportTarget, "report"));

        report.addGenerateOverviewTablesForAdvisories(AeaaAdvisoryTypeStore.CERT_FR, AeaaAdvisoryTypeStore.GHSA, AeaaAdvisoryTypeStore.CERT_SEI, AeaaAdvisoryTypeStore.MSRC);
        report.getSecurityPolicy()
                .setInsignificantThreshold(7)
                .setIncludeScoreThreshold(0)
                .setIncludeAdvisoryTypes(Arrays.asList("alert", "notice"))
                .setVulnerabilityStatusDisplayMapper(CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED);

        reportTarget.mkdirs();

        final File targetLicensesDir = new File(reportTarget, "licenses");
        final File targetComponentDir = new File(reportTarget, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);

        report.setTargetInventoryDir(reportTarget);
        report.setTargetInventoryPath("result.xls");
    }

    @Ignore
    @Test
    public void testCreateTestReport_External() throws Exception {
        final File inventoryDir = new File("<path>");
        final File reportDir = new File("<path>");

        configureAndCreateReport(inventoryDir, "<file>",
                null, null,
                reportDir, new InventoryReport(ReportConfigurationParameters.builder().build()));
    }

    @Test
    public void xmlEscapeDateStringTest() {
        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().build());
        Assert.assertEquals("2020-20-20", report.xmlEscapeDate("2020-20-20"));
    }

}
