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
package org.metaeffekt.core.document;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.document.report.DocumentDescriptorReportGenerator;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Slf4j
public class DocumentDescriptorReportGeneratorTest {
        DocumentDescriptor documentDescriptor = new DocumentDescriptor();
        DocumentDescriptorReportGenerator documentDescriptorReportGenerator = new DocumentDescriptorReportGenerator();

        File targetTestDir = new File("target/test-document-descriptor-report-generator");
    @Before
    public void setup() throws IOException {
        cleanUpTargetTestDir();

        Inventory inventory = new Inventory();
        InventoryContext inventoryContext = new InventoryContext(inventory, inventory, "test", "testReportContextTitle", "testReportContext", "testVersion");
        List<InventoryContext> inventoryContexts = new ArrayList<>();

        Map<String, String> params = new HashMap<>();
        params.put("targetLicensesDir", "");
        params.put("targetComponentDir", "");
        params.put("generateOverviewTablesForAdvisories", "[ {\"name\":\"CERT_EU\"} ]");
        String templatelanguageSelector = "en";

        inventoryContexts.add(inventoryContext);
        documentDescriptor.setInventoryContexts(inventoryContexts);
        documentDescriptor.setParams(params);
        documentDescriptor.setLanguage(templatelanguageSelector);

    }

    @Test
    public void testAnnexTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/annex");

        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.ANNEX);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile = new File(targetReportDir, "map_annex-bookmap.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());
    }

    @Test
    public void testVulnerabilityReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-report");

        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile = new File(targetReportDir, "map_vulnerability-report-bookmap.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());
    }

    @Test
    public void testVulnerabilityStatisticsReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-statistics-report");

        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_STATISTICS_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile = new File(targetReportDir, "map_vulnerability-statistics-report-bookmap.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());
    }

    @Test
    public void testVulnerabilitySummaryReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-summary-report");

        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_SUMMARY_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile = new File(targetReportDir, "map_vulnerability-summary-report-bookmap.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile.getAbsolutePath(), expectedFile.exists());
    }

    public void cleanUpTargetTestDir() throws IOException {
        if (targetTestDir.exists()) {
            FileUtils.deleteDirectory(targetTestDir);
        }
        if (targetTestDir.mkdirs()) {
            log.info("Created target report dir: {}", targetTestDir);
        }
    }
}
