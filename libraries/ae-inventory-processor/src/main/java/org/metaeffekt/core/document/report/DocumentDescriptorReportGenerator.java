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

import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for controlling the process of generating reports from documentDescriptors and their respective ReportContexts.
 */
public class DocumentDescriptorReportGenerator {

    public void generate(DocumentDescriptor documentDescriptor, DocumentDescriptorReportContext reportContext) throws Exception {

        generateInventoryReports(documentDescriptor, reportContext);

        // generate bookmaps to integrate InventoryReport-generated results
        DocumentDescriptorReport documentDescriptorReport = new DocumentDescriptorReport();
        documentDescriptorReport.setTargetReportDir(new File(reportContext.getTargetReportPath()));
        documentDescriptorReport.setTemplateLanguageSelector(documentDescriptor.getTemplateLanguageSelector());

        documentDescriptorReport.createReport(documentDescriptor);
    }

    /**
     * Method for creating inventory Reports for the inventories of the given DocumentDescriptor. We currently use the
     * functionality of InventoryReports.java for creating the dita.vt files needed for document generation, the outlook
     * for this is that we extract documentDescriptor specific functionality out of InventoryReport.java and create an
     * abstract class for encapsulating the functionality used by both types of reports.
     *
     * @param documentDescriptor the given DocumentDescriptor for which the report is generated
     * @param reportContext the context containing fields necessary for starting report generation in InventoryReport
     * @throws Exception
     */
    private static void generateInventoryReports(DocumentDescriptor documentDescriptor, DocumentDescriptorReportContext reportContext) throws Exception {
        List<InventoryReport> inventoryReports = new ArrayList<InventoryReport>();

        // for each inventory trigger according InventoryReport instances to produce
        for(InventoryContext inventoryContext: documentDescriptor.getInventoryContexts()) {

            Map<String, String> params = documentDescriptor.getParams();
            InventoryReport report = new InventoryReport();
            report.setReportContext(new ReportContext(inventoryContext.getReportContextId(), inventoryContext.getReportContextTitle(), inventoryContext.getReportContext()));

            // check pre-requisites dependent on DocumentType
            if (documentDescriptor.getDocumentType() == DocumentType.ANNEX) {
                report.setInventoryBomReportEnabled(true);
                // insert more pre-requisites for annex
            }

            report.setReferenceInventory(inventoryContext.getReferenceInventory());
            report.setInventory(inventoryContext.getInventory());

            // set fields from reportContext
            report.setFailOnUnknown(false);
            report.setFailOnUnknownVersion(false);
            report.setFailOnMissingLicense(false);

            report.setReferenceComponentPath(reportContext.getReferenceComponentPath());
            report.setReferenceLicensePath(reportContext.getReferenceLicensePath());

            report.setTargetLicenseDir(new File(params.get("targetLicensesDir")));
            report.setTargetComponentDir(new File(params.get("targetComponentDir")));
            report.setTargetReportDir(new File(reportContext.getTargetReportPath(), inventoryContext.getIdentifier()));

            report.getReportContext().setReportInventoryName(inventoryContext.getReportContextTitle());
            report.getReportContext().setReportInventoryVersion(inventoryContext.getInventoryVersion());

            report.setTemplateLanguageSelector(documentDescriptor.getTemplateLanguageSelector());

            if (report.createReport()) {
                inventoryReports.add(report);
            } else {
                throw new Exception(report.createReport() + " failed");
            }
        }
    }
}
