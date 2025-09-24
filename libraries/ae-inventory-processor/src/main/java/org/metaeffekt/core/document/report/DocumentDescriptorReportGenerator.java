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
import org.metaeffekt.core.inventory.processor.InventorySeparator;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.CspLoader;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
     *
     * @param documentDescriptor the document descriptor containing the metadata for generating reports
     * @throws IOException if there is an error during file handling or report generation
     */
    public void generate(DocumentDescriptor documentDescriptor) throws IOException {

        // validate documentDescriptor before report generation
        documentDescriptor.validate();
        deriveAssets(documentDescriptor);
        generateInventoryReports(documentDescriptor);

        // generate bookmaps to integrate InventoryReport-generated results
        DocumentDescriptorReport documentDescriptorReport = new DocumentDescriptorReport();
        documentDescriptorReport.setTargetReportDir(documentDescriptor.getTargetReportDir());
        documentDescriptorReport.createPartBookMap(documentDescriptor);
        documentDescriptorReport.createDocumentBookMap(documentDescriptor);
        documentDescriptorReport.createImprint(documentDescriptor);
    }

    private static void deriveAssets(DocumentDescriptor documentDescriptor) throws IOException {
        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            List<InventoryContext> inventoryContexts = new ArrayList<>();

            for (InventoryContext inventoryContext : documentPart.getInventoryContexts()) {
                if (inventoryContext.getAssetName() != null && inventoryContext.getAssetVersion() != null) {
                    inventoryContexts.add(inventoryContext);
                } else if (inventoryContext.getAssetName() == null && inventoryContext.getAssetVersion() == null) {
                    List<Inventory> splitInventories = InventorySeparator.separate(inventoryContext.getInventory());

                    for (Inventory inventory : splitInventories) {
                        Optional<AssetMetaData> primaryAsset = inventory.getAssetMetaData().stream()
                                .filter(AssetMetaData::isPrimary)
                                .findFirst();

                        String assetName = primaryAsset
                                .map(a -> a.get(AssetMetaData.Attribute.NAME))
                                .orElseThrow(() -> new IllegalStateException("Missing asset name in primary asset for inventory [" + inventoryContext.getIdentifier() + "]. Please make sure that every primary asset has a specified name."));

                        String assetVersion = primaryAsset
                                .map(a -> a.get(AssetMetaData.Attribute.VERSION))
                                .orElseThrow(() -> new IllegalStateException("Missing asset version in primary asset for inventory [" + inventoryContext.getIdentifier() + "]. Please make sure that every primary asset has a specified version."));

                        String encodedAssetName = Base64.getEncoder().encodeToString(assetName.getBytes());
                        InventoryContext derivedContext = new InventoryContext(inventory, encodedAssetName, inventoryContext.getReportContext(), inventoryContext.getLicensesPath(), inventoryContext.getComponentsPath());
                        derivedContext.setAssetName(assetName);
                        derivedContext.setAssetVersion(assetVersion);
                        inventoryContexts.add(derivedContext);
                        writeInventoryToFile(inventory, documentDescriptor.getTargetReportDir().getAbsolutePath(), encodedAssetName);
                    }
                } else if (inventoryContext.getAssetName() == null) {
                    throw new IllegalStateException("The field 'assetVersion' for inventoryContext [" + inventoryContext.getIdentifier() + "] is set, but no 'assetName' is specified, please set an 'assetName' as well or remove the field 'assetName'.");
                } else {
                    throw new IllegalStateException("The field 'assetName' for inventoryContext [" + inventoryContext.getIdentifier() + "] is set, but no 'assetVersion' is specified, please set an 'assetVersion' as well or remove the field 'assetName'.");
                }
            }
            documentPart.setInventoryContexts(inventoryContexts);
        }
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
        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            documentPart.validate();

            // for each inventory trigger according InventoryReport instances to produce
            for (InventoryContext inventoryContext : documentPart.getInventoryContexts()) {
                // validate each inventoryContext before processing
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

                ReportConfigurationParameters configParams = buildReportConfiguration(documentPart, documentDescriptor, mergedParams);

                InventoryReport report = new InventoryReport(configParams);
                report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getAssetName(), inventoryContext.getAssetName()));

                setPolicy(mergedParams, report, documentDescriptor);

                if (inventoryContext.getReferenceInventoryContext() != null) {
                    report.setReferenceInventory(inventoryContext.getReferenceInventoryContext().getInventory());
                    report.setReferenceComponentPath(inventoryContext.getReferenceInventoryContext().getComponentsPath());
                    report.setReferenceLicensePath(inventoryContext.getReferenceInventoryContext().getLicensesPath());

                } else {
                    report.setReferenceInventory(inventoryContext.getInventory());
                }
                report.setInventory(inventoryContext.getInventory());

                // these fields were originally part of DocumentDescriptorReportContext, however we decided that these seem
                // to be default values that we do not need to change for different DocumentDescriptors, thus we set them here
                report.setReferenceComponentPath("components");
                report.setReferenceLicensePath("licenses");

                if (mergedParams.get("referenceLicensePath") != null) {
                    report.setReferenceLicensePath(mergedParams.get("referenceLicensePath"));
                }
                if (mergedParams.get("referenceComponentPath") != null) {
                    report.setReferenceComponentPath(mergedParams.get("referenceComponentPath"));
                }
                if (mergedParams.get("LicensesDir") == null) {
                    report.setTargetLicenseDir(new File("license"));
                    log.info("used default targetLicensesDir as 'license'");
                } else {
                    report.setTargetLicenseDir(new File(mergedParams.get("targetLicensesDir")));
                }
                if (mergedParams.get("targetComponentDir") == null) {
                    report.setTargetComponentDir(new File("component"));
                    log.info("used default targetComponentDir as 'component'");
                } else {
                    report.setTargetComponentDir(new File(mergedParams.get("targetComponentDir")));
                }

                report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getAssetName(), inventoryContext.getAssetName()));

                report.getReportContext().setReportInventoryName(inventoryContext.getAssetName());

                report.setTargetReportDir(new File(documentDescriptor.getTargetReportDir(), inventoryContext.getIdentifier()));
                report.getReportContext().setReportInventoryVersion(inventoryContext.getAssetVersion());

                if (!report.createReport()) {
                    throw new RuntimeException("Report creation failed for " + report);
                }

            }
        }
    }
    public static void writeInventoryToFile(Inventory inventory, String targetReportDir, String encodedAssetName) throws IOException {
        File outputDir = new File(targetReportDir, "intermediate-inventories");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + outputDir.getAbsolutePath());
        }

        File outputFile = new File(outputDir, encodedAssetName + ".xlsx");
        InventoryWriter inventoryWriter = new InventoryWriter();
        inventoryWriter.writeInventory(inventory, outputFile);
    }

    private static JSONArray convertToJSONArray(String input) {
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

    private static void setPolicy(Map<String, String> params, InventoryReport report, DocumentDescriptor documentDescriptor) throws IOException {
        if (params != null && (params.containsKey("securityPolicyFile"))) {
            String securityPolicyFilePath = resolveAgainstBasePath(params.get("securityPolicyFile"), documentDescriptor.getBasePath());
            File securityPolicyFile = securityPolicyFilePath != null ? new File(securityPolicyFilePath) : null;

            CspLoader securityPolicy = new CspLoader();
            securityPolicy.setFile(securityPolicyFile);

            if (params.containsKey("securityPolicyActiveIds")) {
                String activeIds = params.get("securityPolicyActiveIds");
                if (activeIds != null && !activeIds.trim().isEmpty()) {
                    List<String> activeIdsList = Arrays.stream(activeIds.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    if (activeIds.isEmpty()) {
                        throw new IOException("No valid active security policy IDs found in 'securityPolicyActiveIds'. Please provided IDs as a comma-separated list.");
                    }

                    securityPolicy.setActiveIds(activeIdsList);
                } else {
                    throw new IOException("No active security policy IDs specified for parameter 'securityPolicyActiveIds'. Please provided IDs as a comma-separated list.");
                }
            }
            report.setSecurityPolicy(securityPolicy.loadConfiguration());
        } else {
            log.info("no securityPolicyFile provided");
        }
    }

    private static Map<String, String> mergeParams(Map<String, String> globalParams, Map<String, String> partParams) {
        Map<String, String> mergedParams = new HashMap<>(globalParams);
        mergedParams.putAll(partParams);

        return mergedParams;
    }

    private static String resolveAgainstBasePath(String filePath, String basePath) {
        if (filePath == null) {
            return null;
        }

        Path resolvedFilePath = basePath != null
                ? Paths.get(basePath).normalize().toAbsolutePath().resolve(filePath).normalize().toAbsolutePath()
                : Paths.get(filePath).normalize().toAbsolutePath();

        return resolvedFilePath.toString();
    }

    private static ReportConfigurationParameters buildReportConfiguration(
            DocumentPart documentPart,
            DocumentDescriptor documentDescriptor,
            Map<String, String> mergedParams
    ) {
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
                break;
        }

        builder.reportLanguage(documentDescriptor.getLanguage());

        builder.includeInofficialOsiStatus(Boolean.parseBoolean(mergedParams.get("includeInofficialOsiStatus")));
        builder.filterAdvisorySummary(Boolean.parseBoolean(mergedParams.get("filterAdvisorySummary")));
        builder.hidePriorityInformation(Boolean.parseBoolean(mergedParams.get("hidePriorityInformation")));
        builder.filterVulnerabilitiesNotCoveredByArtifacts(Boolean.parseBoolean(mergedParams.get("filterVulnerabilitiesNotCoveredByArtifacts")));

        ReportConfigurationParameters configParams = builder.build();
        configParams.setAllFailConditions(false); // current default handling for all document types

        return configParams;
    }
}
