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

import org.json.JSONArray;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for orchestrating the generation of reports for a given {@link DocumentDescriptor}.
 * It controls the entire process of validating the document descriptor, generating inventory reports, and producing
 * the final document descriptor report (such as a DITA BookMap).
 * <p>
 * The report generation process involves validating the {@link DocumentDescriptor} and its associated inventory
 * contexts, generating inventory reports using the {@link InventoryReport} class, and then using the results to
 * produce a final report (including DITA BookMap) with the {@link DocumentDescriptorReport}.
 * </p>
 *
 * @see DocumentDescriptor
 * @see DocumentDescriptorReport
 * @see InventoryReport
 * @see InventoryContext
 */
public class DocumentDescriptorReportGenerator {

    /**
     * Generates the complete set of reports for the given {@link DocumentDescriptor}.
     * This method validates the descriptor, triggers the inventory report generation, and then proceeds to
     * generate the final report using {@link DocumentDescriptorReport}.
     * <p>
     * The report generation consists of:
     * 1. Validating the {@link DocumentDescriptor}.
     * 2. Generating inventory reports.
     * 3. Creating the final document descriptor report.
     * </p>
     *
     * @param documentDescriptor the document descriptor containing the metadata for generating reports
     * @throws IOException if there is an error during file handling or report generation
     */
    public void generate(DocumentDescriptor documentDescriptor) throws IOException {

        // validate documentDescriptor before report generation
        documentDescriptor.validate();

        generateInventoryReports(documentDescriptor);

        // generate bookmaps to integrate InventoryReport-generated results
        DocumentDescriptorReport documentDescriptorReport = new DocumentDescriptorReport();
        documentDescriptorReport.setTargetReportDir(documentDescriptor.getTargetReportDir());
        documentDescriptorReport.setTemplateLanguageSelector(documentDescriptor.getTemplateLanguageSelector());

        documentDescriptorReport.createReport(documentDescriptor);
    }

    /**
     * Generates inventory reports for the inventories associated with the given {@link DocumentDescriptor}.
     * This method uses the {@link InventoryReport} class to generate reports for each inventory context.
     * It currently triggers the creation of the necessary DITA templates (e.g., dita.vt files).
     * <p>
     * This method works by iterating over all inventory contexts and validating each before generating the
     * corresponding inventory report. The pre-requisites for generating these reports are checked based on the
     * {@link DocumentType}.
     * </p>
     *
     * @param documentDescriptor the document descriptor containing the inventory contexts for report generation
     * @throws IOException if there is an error accessing inventory files or generating reports
     */
    private static void generateInventoryReports(DocumentDescriptor documentDescriptor) throws IOException {
        List<InventoryReport> inventoryReports = new ArrayList<InventoryReport>();

        // for each inventory trigger according InventoryReport instances to produce
        for(InventoryContext inventoryContext: documentDescriptor.getInventoryContexts()) {

            // validate each inventoryContext before processing
            inventoryContext.validate();

            Map<String, String> params = documentDescriptor.getParams();
            InventoryReport report = new InventoryReport();
            report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getReportContextTitle(), inventoryContext.getReportContext()));

            if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                report.setInventoryVulnerabilityReportSummaryEnabled(true);
            }
            if (documentDescriptor.getDocumentType() == DocumentType.ANNEX) {
                report.setInventoryBomReportEnabled(true);
            }
            if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_STATISTICS_REPORT) {
                report.setInventoryVulnerabilityStatisticsReportEnabled(true);
            }
            if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                report.setInventoryVulnerabilityReportSummaryEnabled(true);
            }
            if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT) {
                File securityPolicyFile = new File(params.get("securityPolicyFile"));
                String securityPolicyOverwriteJson = "";

                if (params.get("securityPolicyOverwriteJson") != null) {
                    securityPolicyOverwriteJson = params.get("securityPolicyOverwriteJson");
                }

                String generateOverviewTablesForAdvisories = params.get("generateOverviewTablesForAdvisories");

                boolean filterVulnerabilitiesNotCoveredByArtifacts = Boolean.parseBoolean(params.get("vulnerabilitiesNotCoveredByArtifacts"));
                //boolean filterAdvisorySummary = Boolean.parseBoolean(params.get("filterAdvisorySummary"));
                //File diffInventoryFile = new File(params.get("diffInventoryFile"));

                CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();
                securityPolicy = CentralSecurityPolicyConfiguration.fromConfiguration(securityPolicy, securityPolicyFile, securityPolicyOverwriteJson);


                report.setSecurityPolicy(securityPolicy);
                report.setFilterVulnerabilitiesNotCoveredByArtifacts(filterVulnerabilitiesNotCoveredByArtifacts);
                //report.setFilterAdvisorySummary(filterAdvisorySummary);
                //report.setDiffInventoryFile(diffInventoryFile);

                try {
                    report.addGenerateOverviewTablesForAdvisoriesByMap(new JSONArray(generateOverviewTablesForAdvisories));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse generateOverviewTablesForAdvisories, must be a valid content identifier JSONArray: " + generateOverviewTablesForAdvisories, e);
                }

                report.setInventoryVulnerabilityReportEnabled(true);
                report.setInventoryVulnerabilityReportEnabled(true);
            }

            report.setReferenceInventory(inventoryContext.getReferenceInventory());
            report.setInventory(inventoryContext.getInventory());

            report.setFailOnDevelopment(false);
            report.setFailOnError(false);
            report.setFailOnBanned(false);
            report.setFailOnDowngrade(false);
            report.setFailOnInternal(false);
            report.setFailOnUnknown(false);
            report.setFailOnUnknownVersion(false);
            report.setFailOnUpgrade(false);
            report.setFailOnMissingLicense(false);
            report.setFailOnMissingLicenseFile(false);
            report.setFailOnMissingComponentFiles(false);
            report.setFailOnMissingNotice(false);


            // these fields were originally part of DocumentDescriptorReportContext, however we decided that these seem
            // to be default values that we do not need to change for different DocumentDescriptors, thus we set them here
            report.setReferenceComponentPath("components");
            report.setReferenceLicensePath("licenses");

            report.setTargetLicenseDir(new File(params.get("targetLicensesDir")));
            report.setTargetComponentDir(new File(params.get("targetComponentDir")));
            report.setTargetReportDir(new File(documentDescriptor.getTargetReportDir(), inventoryContext.getIdentifier()));

            report.getReportContext().setReportInventoryName(inventoryContext.getReportContextTitle());
            report.getReportContext().setReportInventoryVersion(inventoryContext.getInventoryVersion());

            report.setTemplateLanguageSelector(documentDescriptor.getTemplateLanguageSelector());

            if (report.createReport()) {
                inventoryReports.add(report);
            } else {
                throw new RuntimeException("Report creation failed for " + report);
            }
        }
    }
}
