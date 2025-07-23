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

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.metaeffekt.core.inventory.processor.model.Constants.STRING_TRUE;

@Ignore
public class ExternalRepositoryReportTest {

    private static final File INVENTORY_DIR = new File("<path-to-inventory-folder>");

    private static final String INVENTORY_INCLUDES = "inventory/*.xls";
    private static final String LICENSE_FOLDER = "licenses";
    private static final String COMPONENT_FOLDER = "components";

    @Test
    public void testValidateInventoryProcessor() throws IOException {
        final Inventory inventory = InventoryUtils.readInventory(INVENTORY_DIR, INVENTORY_INCLUDES);

        Properties properties = new Properties();
        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, new File(INVENTORY_DIR, LICENSE_FOLDER).getAbsolutePath());
        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_DIR, new File(INVENTORY_DIR, COMPONENT_FOLDER).getAbsolutePath());

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        validateInventoryProcessor.process(inventory);
    }

    @Ignore
    @Test
    public void testValidateInventoryProcessor_WorkbenchInput() throws IOException {
        boolean enableDeleteObsolete = false;

        final File inventoryDir = new File("<path-to-workbench-inventory-dir>");
        final Inventory inventory = InventoryUtils.readInventory(inventoryDir, INVENTORY_INCLUDES);

        File licensesDir = new File(inventoryDir, LICENSE_FOLDER);
        File componentsDir = new File(inventoryDir, COMPONENT_FOLDER);

        Properties properties = new Properties();

        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, licensesDir.getPath());
//        properties.setProperty(ValidateInventoryProcessor.LICENSES_TARGET_DIR, licensesTargetDir.getPath());

        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_DIR, componentsDir.getPath());
//        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_TARGET_DIR, componentsTargetDir.getPath());

        properties.setProperty(ValidateInventoryProcessor.FAIL_ON_ERROR, STRING_TRUE);
        properties.setProperty(ValidateInventoryProcessor.CREATE_LICENSE_FOLDERS, STRING_TRUE);
        properties.setProperty(ValidateInventoryProcessor.CREATE_COMPONENT_FOLDERS, STRING_TRUE);

        if (enableDeleteObsolete) {
            properties.setProperty(ValidateInventoryProcessor.DELETE_LICENSE_FOLDERS, STRING_TRUE);
            properties.setProperty(ValidateInventoryProcessor.DELETE_COMPONENT_FOLDERS, STRING_TRUE);
        }

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        validateInventoryProcessor.process(inventory);
    }

    @Ignore
    @Test
    public void testFullReport() throws IOException {
        final File inventoryDir = new File("src/test/resources/external-report-test");
        final File reportDir = new File("target/external-full-report");

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

        // copy bookmap
        FileUtils.copyFileToDirectory(new File("src/test/resources/external-report-test/bm_test.ditamap"), reportDir);

        // generate PDF using the following command from terminal:
        // 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISABLED -Ddita.source.dir=target/external-full-report'
    }
}
