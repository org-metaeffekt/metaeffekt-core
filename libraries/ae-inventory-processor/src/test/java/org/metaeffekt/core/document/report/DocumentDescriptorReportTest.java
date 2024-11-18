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
package org.metaeffekt.core.document.report;

import org.junit.Test;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  DocumentDescriptorReportTest {

    @Test
    public void testAnnex001() throws Exception {
        final File resourceRootDir = new File("src/test/resources/document-descriptor/annex-001");

        // create documentDescriptor and assign documentType
        final DocumentDescriptor documentDescriptor = new DocumentDescriptor();
        documentDescriptor.setDocumentType(DocumentType.ANNEX);

        // create and read inventory
        final File inventoryFile = new File(resourceRootDir, "scan-inventory.xlsx");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        // create inventoryContexts and define fields
        InventoryContext inventoryContext001 = new InventoryContext(inventory, inventory, "keycloak", "Keycloak", "Keycloak", "0.0.1");

        InventoryContext inventoryContext002 = new InventoryContext(inventory, inventory, "scan", "Scan", "Scan", "1.3.2");

        List<InventoryContext> inventoryContexts = new ArrayList<>();
        inventoryContexts.add(inventoryContext001);
        inventoryContexts.add(inventoryContext002);
        documentDescriptor.setInventoryContexts(inventoryContexts);

        // create reportContext for this report
        documentDescriptor.setTargetReportDir(new File("target/document-descriptor-report-001/report"));

        // set params for documentDescriptor
        File target = new File("target/document-descriptor-report-001");
        target.mkdirs();

        File targetLicenseDir = new File(target, "license");
        File targetComponentDir = new File(target, "component");

        final Map<String, String> params = new HashMap<>();
        params.put("targetLicensesDir", targetLicenseDir.getPath());
        params.put("targetComponentDir", targetComponentDir.getPath());
        documentDescriptor.setParams(params);

        // generate report
        DocumentDescriptorReportGenerator reportGenerator = new DocumentDescriptorReportGenerator();
        reportGenerator.generate(documentDescriptor);

        // HINT: produce PDF using 'mvn initialize -Pgenerate-dita -Dphase.inventory.check=DISBALED -Ddita.source.dir=target/document-descriptor-report-001/report -Ddita.map=map_annex-bookmap.ditamap'
    }

}