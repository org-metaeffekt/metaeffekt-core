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

import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class ValidReferencesTest {

    final static File reportDir = new File("target/report/valid-references/");

    @Before
    public void setUp() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-01/");

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .inventoryBomReportEnabled(true)
                .inventoryDiffReportEnabled(true)
                .inventoryPomEnabled(true)
                .inventoryVulnerabilityReportEnabled(true)
                .inventoryVulnerabilityReportSummaryEnabled(true)
                .inventoryVulnerabilityStatisticsReportEnabled(true)
                .assetBomReportEnabled(true)
                .assessmentReportEnabled(true)
                .build());

        report.setReportContext(new ReportContext("test", "Test", "Test Context"));
        report.setReferenceLicensePath("licenses");
        report.setReferenceComponentPath("components");
        report.setInventory(InventoryUtils.readInventory(inventoryDir, "*.xls"));
        report.setTargetReportDir(new File(reportDir, "report"));

        reportDir.mkdirs();

        final File targetLicensesDir = new File(reportDir, "licenses");
        final File targetComponentDir = new File(reportDir, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);

        report.setTargetInventoryDir(reportDir);
        report.setTargetInventoryPath("result.xls");

        report.createReport();
        FileUtils.copyFileToDirectory(new File(inventoryDir, "bm_test.ditamap"), reportDir);

    }

    @Test
    public void testValidLinksForEffectiveLicenses() throws IOException {
        File originTemplateFile = new File(reportDir, "report/tpc_inventory-licenses-effective.dita");
        File targetTemplateFile = new File(reportDir, "report/tpc_inventory-license-usage.dita");

        assertThat(originTemplateFile).exists();
        assertThat(targetTemplateFile).exists();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"tpc_inventory-license-usage.dita#tpc_effective_license_a-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_effective_license_a-license\">"))).isTrue();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"tpc_inventory-license-usage.dita#tpc_effective_license_b-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_effective_license_b-license\">"))).isTrue();

    }

    @Test
    public void testValidLinksForAssetLicenses() throws IOException {
        File originTemplateFile = new File(reportDir, "report/tpc_asset-licenses.dita");
        File targetTemplateFile = new File(reportDir, "report/tpc_asset-licenses.dita");

        assertThat(originTemplateFile).exists();
        assertThat(targetTemplateFile).exists();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"#tpc_associated_license_details_a-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_associated_license_details_a-license\">"))).isTrue();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"#tpc_associated_license_details_b-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_associated_license_details_b-license\">"))).isTrue();

    }

    @Test
    public void testDitaTemplateLinksValid() {
        DitaTemplateLinkValidator ditaTemplateLinkValidator = new DitaTemplateLinkValidator();
        List<String> errors = ditaTemplateLinkValidator.validateDir(new File(reportDir, "report"));
        assertThat(errors).isEmpty();
    }
}
