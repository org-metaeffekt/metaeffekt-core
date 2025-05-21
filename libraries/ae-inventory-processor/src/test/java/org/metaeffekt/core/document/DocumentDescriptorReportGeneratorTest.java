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
import org.metaeffekt.core.document.model.DocumentPart;
import org.metaeffekt.core.document.model.DocumentPartType;
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

        List<InventoryContext> inventoryContexts = new ArrayList<>();
        List<DocumentPart> documentParts = new ArrayList<>();

        File targetTestDir = new File("target/test-document-descriptor-report-generator");
    @Before
    public void setup() throws IOException {
        cleanUpTargetTestDir();

        Inventory inventory = new Inventory();
        InventoryContext inventoryContext = new InventoryContext(inventory, inventory, "test", "testReportContextTitle", "testReportContext", "testVersion");

        Map<String, String> documentParams = new HashMap<>();
        documentParams.put("targetLicensesDir", "");
        documentParams.put("targetComponentDir", "");
        String language = "en";
        String identifier = "test";

        inventoryContexts.add(inventoryContext);
        documentDescriptor.setParams(documentParams);
        documentDescriptor.setLanguage(language);
        documentDescriptor.setIdentifier(identifier);

    }

    @Test
    public void testAnnexTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/annex");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("", "");
        DocumentPartType partType = DocumentPartType.ANNEX;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.ANNEX);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-annex.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
    }

    @Test
    public void testVulnerabilityReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-report");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("securityPolicyFile", "security-policy-report.json");
        partParams.put("generateOverviewTablesForAdvisories", "CERT_EU, CERT_FR");
        DocumentPartType partType = DocumentPartType.VULNERABILITY_REPORT;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-vulnerability-report.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
    }

    @Test
    public void testVulnerabilityStatisticsReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-statistics-report");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("securityPolicyFile", "security-policy-report.json");
        partParams.put("generateOverviewTablesForAdvisories", "CERT_EU, CERT_FR");
        DocumentPartType partType = DocumentPartType.VULNERABILITY_STATISTICS_REPORT;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_STATISTICS_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-vulnerability-statistics-report.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
    }

    @Test
    public void testVulnerabilitySummaryReportTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/vulnerability-summary-report");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("securityPolicyFile", "security-policy-report.json");
        partParams.put("generateOverviewTablesForAdvisories", "CERT_EU, CERT_FR");
        DocumentPartType partType = DocumentPartType.VULNERABILITY_SUMMARY_REPORT;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.VULNERABILITY_SUMMARY_REPORT);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-vulnerability-summary-report.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
    }

    @Test
    public void testInitialLicenseDocumentationTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/initial-license-documentation");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("", "");
        DocumentPartType partType = DocumentPartType.INITIAL_LICENSE_DOCUMENTATION;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.INITIAL_LICENSE_DOCUMENTATION);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-initial-license-documentation.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
    }

    @Test
    public void testlicenseDocumentationTemplates () throws IOException {
        File targetReportDir = new File("target/test-document-descriptor-report-generator/license-documentation");

        Map<String, String> partParams = new HashMap<>();
        partParams.put("", "");
        DocumentPartType partType = DocumentPartType.LICENSE_DOCUMENTATION;
        DocumentPart documentPart = new DocumentPart("test", inventoryContexts, partType, partParams);
        documentParts.add(documentPart);

        documentDescriptor.setDocumentParts(documentParts);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setDocumentType(DocumentType.LICENSE_DOCUMENTATION);

        documentDescriptorReportGenerator.generate(documentDescriptor);

        File expectedFile01 = new File(targetReportDir, "parts/map_test-license-documentation.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile01.getAbsolutePath(), expectedFile01.exists());
        File expectedFile02 = new File(targetReportDir, "map_test-document.ditamap");
        assertTrue("Expected file does not exist: " + expectedFile02.getAbsolutePath(), expectedFile02.exists());
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
