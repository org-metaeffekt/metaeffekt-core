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
package org.metaeffekt.core.inventory.processor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class RepositoryReportTest {

    private static final File INVENTORY_DIR = new File("src/test/resources/test-inventory-01");
    private static final String INVENTORY_INCLUDES = "*.xls";
    private static final String LICENSES_PATH = "licenses";
    private static final String COMPONENTS_PATH = "components";

    public static final String UTF_8 = "UTF-8";

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
        File inventoryFile = new File(INVENTORY_DIR, "artifact-inventory-01.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        Assert.assertNotNull(inventory.getComponentPatternData());
        Assert.assertEquals(6, inventory.getComponentPatternData().size());

        Assert.assertEquals("org/metaeffekt/core/**/*-org/metaeffekt/core Classes-metaeffekt Core-0.21.0-org/metaeffekt.core/Inventory.class-ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).deriveQualifier());
        Assert.assertEquals("org/metaeffekt/core/**/*::metaeffekt Core:org/metaeffekt/core Classes::0.21.0::org/metaeffekt.core/Inventory.class:ABBBCBBASBANSB", inventory.getComponentPatternData().get(0).createCompareStringRepresentation());

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

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .inventoryBomReportEnabled(true)
                .build());

        configureAndCreateReport(inventoryDir, "*.xls",
                inventoryDir, "*.xls",
                reportDir, report);

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

        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .assessmentReportEnabled(false)
                .inventoryBomReportEnabled(false)
                .inventoryDiffReportEnabled(false)
                .inventoryVulnerabilityReportEnabled(false)
                .inventoryVulnerabilityStatisticsReportEnabled(false)
                .inventoryVulnerabilityReportSummaryEnabled(false)
                .reportLanguage("de")
                .failOnUnknown(false)
                .failOnUnknownVersion(false)
                .build());

        prepareReport(inventoryDir, "*.xls", inventoryDir, "*.xls", reportDir, report);

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
    public void testCreateTestReport004() throws Exception {
        File target = new File("target/test-inventory-04");
        target.mkdirs();

        // copy bookmap to enable PDF generation testing
        FileUtils.copyFileToDirectory(new File("src/test/resources/test-inventory-01/bm_test.ditamap"), target);

        File inventoryDir = new File("src/test/resources/test-inventory-04");
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .failOnUnknown(false)
                .failOnUnknownVersion(false)
                .failOnMissingLicenseFile(false)
                .build());

        report.setReportContext(new ReportContext("test", "Test", "Test Context"));

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

        final boolean valid = report.createReport();
        assertTrue(valid);

        // generate PDF using the following command from terminal:
        // 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISABLED -Ddita.source.dir=target/test-inventory-04'
    }

    @Test
    public void testCreateTestReport005() throws IOException {
        File inventoryDir = new File("src/test/resources/test-inventory-05/");
        String inventoryIncludes = INVENTORY_INCLUDES;

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .reportLanguage("en")
                .failOnMissingLicense(false)
                .failOnUnknown(false)
                .failOnUnknownVersion(false)
                .build());

        report.setReportContext(new ReportContext("test", "Test", "Test Context"));

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

        File targetReportPath = new File(target, "report");
        targetReportPath.mkdirs();

        File licenseReport = new File(targetReportPath, "tpc_inventory-licenses.dita");
        File componentReport = new File(targetReportPath, "tpc_inventory-component-report.dita");
        File noticeReport = new File(targetReportPath, "tpc_inventory-component-license-details.dita");
        File artifactReport = new File(targetReportPath, "tpc_inventory-artifact-report.dita");

        report.setTargetLicenseDir(new File("licenses"));
        report.setTargetComponentDir(new File("components"));
        report.setTargetReportDir(targetReportPath);

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

        final InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .assessmentReportEnabled(false)
                .inventoryBomReportEnabled(false)
                .inventoryDiffReportEnabled(false)
                .inventoryVulnerabilityReportEnabled(false)
                .inventoryVulnerabilityReportSummaryEnabled(false)
                .assetBomReportEnabled(false)
                .build());

        configureAndCreateReport(inventoryDir, "*.xls", inventoryDir, "*.xls", reportDir, new InventoryReport());

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

        report.setReferenceInventoryDir(referenceInventoryDir);
        report.setReferenceInventoryIncludes(referenceInventoryIncludes);

        report.setReferenceLicensePath(new File(inventoryDir, "licenses").getAbsolutePath());
        report.setReferenceComponentPath(new File(inventoryDir, "components").getAbsolutePath());

        report.setInventory(InventoryUtils.readInventory(inventoryDir, inventoryIncludes));

        report.setTargetReportDir(new File(reportTarget, "report"));

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
        final File inventoryDir = new File("<source>");
        final File reportDir = new File("<target>");

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .reportLanguage("en")
                .inventoryVulnerabilityStatisticsReportEnabled(true)
                .inventoryVulnerabilityReportSummaryEnabled(true)
                .inventoryVulnerabilityReportEnabled(true)
                .hidePriorityInformation(false)
                .build());

        configureAndCreateReport(inventoryDir, "*.xls",
                null, null,
                reportDir, report);
    }

    @Test
    public void xmlEscapeDateStringTest() {
        final InventoryReport report = new InventoryReport();
        Assert.assertEquals("2020-20-20", report.xmlEscapeDate("2020-20-20"));
    }

}
