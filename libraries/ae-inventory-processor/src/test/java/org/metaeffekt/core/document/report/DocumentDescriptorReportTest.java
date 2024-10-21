package org.metaeffekt.core.document.report;

import org.junit.Test;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocumentDescriptorReportTest {

    @Test
    public void testAnnex001() throws IOException {
        final File resourceBaseDir = new File("src/test/resources/document-descriptor/annex-001");

        final DocumentDescriptor documentDescriptor = new DocumentDescriptor();
        documentDescriptor.setDocumentType(DocumentType.ANNEX);

        final File inventoryFile = new File(resourceBaseDir, "scan-inventory.xlsx");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        documentDescriptor.setInventories(Collections.singletonList(inventory));

        final Map<String, String> params = new HashMap<>();
        documentDescriptor.setParams(params);

        DocumentDescriptorReportGenerator reportGenerator = new DocumentDescriptorReportGenerator();

        reportGenerator.generate(documentDescriptor);

    }

}