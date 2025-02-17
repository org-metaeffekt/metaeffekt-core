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
import java.util.*;

@Slf4j
public class DocumentPartTest {

        File targetReportDir = new File("target/test-document-parts");

    @Test
    public void generateDocument() throws IOException {
        cleanUpTargetReportDir();

        DocumentDescriptor documentDescriptor = new DocumentDescriptor();

        Inventory inventory = new Inventory();
        Inventory referencedInventory = new Inventory();
        InventoryContext inventoryContext = new InventoryContext(inventory, referencedInventory, "test", "test", "test", "1.0");
        List<InventoryContext> contexts = new ArrayList<>();
        contexts.add(inventoryContext);
        Map<String, String> partParams = new HashMap<>();
        partParams.put("generateOverviewTablesForAdvisories", "CERT_EU");

        List<DocumentPart> parts = new ArrayList<>();

        DocumentPart vulnerabilityReportPart = new DocumentPart("test", contexts, DocumentPartType.VULNERABILITY_REPORT, partParams);
        DocumentPart annexPart = new DocumentPart("test", contexts, DocumentPartType.ANNEX, partParams);
        parts.add(vulnerabilityReportPart);
        parts.add(annexPart);

        Map<String, String> documentParams = new HashMap<>();
        documentParams.put("targetLicensesDir", "test");
        documentParams.put("targetComponentDir", "test");
        documentDescriptor.setDocumentParts(parts);
        documentDescriptor.setLanguage(Locale.ENGLISH);
        documentDescriptor.setDocumentType(DocumentType.ANNEX);
        documentDescriptor.setTargetReportDir(targetReportDir);
        documentDescriptor.setParams(documentParams);
        documentDescriptor.setIdentifier("annex");

        DocumentDescriptorReportGenerator documentDescriptorReportGenerator = new DocumentDescriptorReportGenerator();
        documentDescriptorReportGenerator.generate(documentDescriptor);
    }

    public void cleanUpTargetReportDir() throws IOException {
        if (targetReportDir.exists()) {
            FileUtils.deleteDirectory(targetReportDir);
        }
        if (targetReportDir.mkdirs()) {
            log.info("Created target report dir: {}", targetReportDir);
        }
    }
}
