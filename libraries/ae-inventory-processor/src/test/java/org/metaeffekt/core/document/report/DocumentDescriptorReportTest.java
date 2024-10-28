package org.metaeffekt.core.document.report;

import org.codehaus.plexus.util.PropertyUtils;
import org.junit.Test;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class  DocumentDescriptorReportTest {

    @Test
    public void testAnnex001() throws Exception {
        final File resourceRootDir = new File("src/test/resources/document-descriptor/annex-001");

        // step 1 - create documentDescriptor and assign inventoryContext to it
        final DocumentDescriptor documentDescriptor = new DocumentDescriptor();
        documentDescriptor.setDocumentType(DocumentType.ANNEX);

        final File inventoryFile = new File(resourceRootDir, "scan-inventory.xlsx");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        InventoryContext inventoryContext001 = new InventoryContext();
        inventoryContext001.setInventory(inventory);
        inventoryContext001.setIdentifier("Keycloak");
        inventoryContext001.setReferenceInventory(inventory);
        inventoryContext001.setReportContextId("123");
        inventoryContext001.setReportContextTitle("Test");
        inventoryContext001.setReportContext("Test");

        InventoryContext inventoryContext002 = new InventoryContext();
        inventoryContext002.setInventory(inventory);
        inventoryContext002.setIdentifier("Scan");
        inventoryContext002.setReferenceInventory(inventory);
        inventoryContext002.setReportContextId("456");
        inventoryContext002.setReportContextTitle("Test");
        inventoryContext002.setReportContext("Test");

        DocumentDescriptorReportContext reportContext = new DocumentDescriptorReportContext();
        reportContext.setTargetReportPath("target/document-descriptor-report-001/report");

        List<InventoryContext> inventoryContexts = new ArrayList<>();
        inventoryContexts.add(inventoryContext001);
        inventoryContexts.add(inventoryContext002);

        documentDescriptor.setInventoryContexts(inventoryContexts);

        // step 2 - set parameters for documentDescriptor

        File target = new File("target/document-descriptor-report-001");
        target.mkdirs();

        File targetLicenseDir = new File(target, "license");
        File targetComponentDir = new File(target, "component");

        final Map<String, String> params = new HashMap<>();

        params.put("targetLicensesDir", targetLicenseDir.getPath());
        params.put("targetComponentDir", targetComponentDir.getPath());

        documentDescriptor.setParams(params);

        // step 3 - create ReportGenerator and generate a report
        DocumentDescriptorReportGenerator reportGenerator = new DocumentDescriptorReportGenerator();

        reportGenerator.generate(documentDescriptor, reportContext);



    }

}