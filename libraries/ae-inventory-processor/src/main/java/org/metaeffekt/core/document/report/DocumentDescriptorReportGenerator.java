package org.metaeffekt.core.document.report;

import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for controlling the process of generating reports from documentDescriptors and their respective ReportContexts.
 */
public class DocumentDescriptorReportGenerator {

    public void generate(DocumentDescriptor documentDescriptor, DocumentDescriptorReportContext reportContext) throws Exception {

        List<InventoryReport> inventoryReports = new ArrayList<InventoryReport>();



        // for each inventory trigger according InventoryReport instances to produce
        for(InventoryContext inventoryContext: documentDescriptor.getInventoryContexts()) {

            Map<String, String> params = documentDescriptor.getParams();
            InventoryReport report = new InventoryReport();
            report.setReportContext(new ReportContext(params.get("reportContextId"), params.get("reportContextTitle"), params.get("reportContext")));

            // check pre-requisites dependent on DocumentType
            if (documentDescriptor.getDocumentType() == DocumentType.ANNEX) {
                report.setInventoryBomReportEnabled(true);
                // insert more pre-requisites for annex
            }

            report.setReferenceInventory(inventoryContext.getReferenceInventory());
            report.setInventory(inventoryContext.getInventory());

            // set fields from reportContext
            report.setFailOnUnknown(reportContext.getFailOnUnknown());
            report.setFailOnUnknownVersion(reportContext.getFailOnUnknownVersion());
            report.setFailOnMissingLicense(reportContext.getFailOnMissingLicense());

            report.setReferenceComponentPath(reportContext.getReferenceComponentPath());
            report.setReferenceLicensePath(reportContext.getReferenceLicensePath());

            report.setTargetLicenseDir(new File(params.get("targetLicensesDir")));
            report.setTargetComponentDir(new File(params.get("targetComponentDir")));
            report.setTargetReportDir(new File(reportContext.getTargetReportPath(), inventoryContext.getIdentifier()));

            if (report.createReport()) {
                inventoryReports.add(report);
            } else {
                throw new Exception(report.createReport() + " failed");
            }
        }
        // generate bookmaps to integrate InventoryReport-generated results

    }
}
