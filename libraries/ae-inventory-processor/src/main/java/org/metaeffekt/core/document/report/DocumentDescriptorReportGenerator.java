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
import org.json.JSONObject;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentPart;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final Logger log = LoggerFactory.getLogger(DocumentDescriptorReportGenerator.class);

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

        documentDescriptorReport.createPartBookMap(documentDescriptor);

        documentDescriptorReport.createDocumentBookMap(documentDescriptor);
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
        List<InventoryReport> inventoryReports = new ArrayList<>();

        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {

            documentPart.validate();

            InventoryContext inventoryContext = documentPart.getInventoryContext();
            inventoryContext.validate();

            Map<String, String> mergedParams;

            if (documentPart.getParams() != null && documentDescriptor.getParams() != null) {
                mergedParams = mergeParams(documentDescriptor.getParams(), documentPart.getParams());
            } else if (documentPart.getParams() != null) {
                mergedParams = documentPart.getParams();
            } else if (documentDescriptor.getParams() != null) {
                mergedParams = documentDescriptor.getParams();
            } else {
                mergedParams = new HashMap<>();
            }

            ReportConfigurationParameters.ReportConfigurationParametersBuilder builder = ReportConfigurationParameters.builder();

            switch (documentPart.getDocumentPartType()) {
                case ANNEX:
                    builder.filterVulnerabilitiesNotCoveredByArtifacts(Boolean.parseBoolean(mergedParams.getOrDefault("vulnerabilitiesNotCoveredByArtifacts", "false")));
                    builder.inventoryBomReportEnabled(true);
                    break;
                case VULNERABILITY_STATISTICS_REPORT:
                    builder.inventoryVulnerabilityStatisticsReportEnabled(true);
                    break;
                case VULNERABILITY_SUMMARY_REPORT:
                    builder.inventoryVulnerabilityReportSummaryEnabled(true);
                    break;
                case VULNERABILITY_REPORT:
                    builder.filterVulnerabilitiesNotCoveredByArtifacts(Boolean.parseBoolean(mergedParams.getOrDefault("vulnerabilitiesNotCoveredByArtifacts", "false")));
                    builder.inventoryVulnerabilityReportEnabled(true);
                    break;
                case INITIAL_LICENSE_DOCUMENTATION:
                    builder.assetBomReportEnabled(true);
                    break;
                case LICENSE_DOCUMENTATION:
                    builder.inventoryBomReportEnabled(true);
            }
            builder.reportLanguage(documentDescriptor.getLanguage());
            boolean includeInofficialOsiStatus = Boolean.parseBoolean(mergedParams.get("includeInofficialOsiStatus"));
            builder.includeInofficialOsiStatus(includeInofficialOsiStatus);
            ReportConfigurationParameters configParams = builder.build();

            // FIXME-KKL: revise if we want different pre-requisites for the different document types, currently we handle all the same way
            configParams.setAllFailConditions(false);

            InventoryReport report = new InventoryReport(configParams);
            report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getReportContextTitle(), inventoryContext.getReportContext()));

                switch (documentPart.getDocumentPartType()) {
                    case ANNEX:
                        setPolicy(mergedParams, report);
                        break;
                    case VULNERABILITY_REPORT:
                        setPolicy(mergedParams, report);
                        String generateOverviewTablesForAdvisories = mergedParams.get("generateOverviewTablesForAdvisories");
                        if (generateOverviewTablesForAdvisories != null) {
                            try {
                                // FIXME-RTU: discuss with Karsten how we want to pass the list of providers & how to list them in the yaml
                                report.addGenerateOverviewTablesForAdvisoriesByMap(convertToJSONArray(generateOverviewTablesForAdvisories));
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to parse generateOverviewTablesForAdvisories, must be a valid content identifier JSONArray: " + generateOverviewTablesForAdvisories, e);
                            }
                            break;
                        }
                }

            report.setReferenceInventory(inventoryContext.getReferenceInventory());
            report.setInventory(inventoryContext.getInventory());

            // these fields were originally part of DocumentDescriptorReportContext, however we decided that these seem
            // to be default values that we do not need to change for different DocumentDescriptors, thus we set them here
            report.setReferenceComponentPath("components");
            report.setReferenceLicensePath("licenses");

            if (mergedParams.get("LicensesDir") == null) {
                report.setTargetLicenseDir(new File("license"));
                log.info("used default targetLicensesDir as 'license'");
            } else {
                report.setTargetLicenseDir(new File(mergedParams.get("targetLicensesDir")));
            }
            if (mergedParams.get("targetComponentDir") == null) {
                report.setTargetLicenseDir(new File("component"));
                log.info("used default targetComponentDir as 'component'");
            } else {
                report.setTargetComponentDir(new File(mergedParams.get("targetComponentDir")));
            }

            boolean omitAssetPrefix = Boolean.parseBoolean(mergedParams.get("omitAssetPrefix"));
            String title = null;

            if (mergedParams.get("omitAssetPrefix") != null) {
                if (!omitAssetPrefix) {
                    title = inventoryContext.getReportContextTitle();
                }
            } else {
                title = inventoryContext.getReportContextTitle();
            }

            report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), title, inventoryContext.getReportContext()));

            report.getReportContext().setReportInventoryName(inventoryContext.getReportContextTitle());

            report.setTargetReportDir(new File(documentDescriptor.getTargetReportDir(), inventoryContext.getIdentifier()));
            report.getReportContext().setReportInventoryVersion(inventoryContext.getInventoryVersion());

            if (report.createReport()) {
                inventoryReports.add(report);
            } else {
                throw new RuntimeException("Report creation failed for " + report);
            }
        }
    }

    public static JSONArray convertToJSONArray(String input) {
        JSONArray jsonArray = new JSONArray();

        // Split the input string by commas and trim whitespace
        String[] names = input.split(",");

        for (String name : names) {
            // Create a JSONObject for each name and add it to the JSONArray
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name.trim()); // Trim to remove extra spaces
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    private static void setPolicy(Map<String, String> params, InventoryReport report) throws IOException {
        if (params != null && (params.containsKey("securityPolicyFile") || params.containsKey("securityPolicyOverwriteJson"))) {
            String securityPolicyFilePath = params.get("securityPolicyFile");
            File securityPolicyFile = securityPolicyFilePath != null ? new File(securityPolicyFilePath) : null;

            String securityPolicyOverwriteJson = params.getOrDefault("securityPolicyOverwriteJson", "");

            boolean filterVulnerabilitiesNotCoveredByArtifacts = Boolean.parseBoolean(params.getOrDefault("vulnerabilitiesNotCoveredByArtifacts", "false"));

            CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();
            securityPolicy = CentralSecurityPolicyConfiguration.fromConfiguration(securityPolicy, securityPolicyFile, securityPolicyOverwriteJson);

            report.setSecurityPolicy(securityPolicy);
        }
        log.info("no securityPolicyFile provided");
    }

    private static Map<String, String> mergeParams(Map<String, String> globalParams, Map<String, String> partParams) {
        Map<String, String> mergedParams = new HashMap<>(globalParams);
        mergedParams.putAll(partParams);

        return mergedParams;
    }
}
