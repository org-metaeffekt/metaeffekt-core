/*
 * Copyright 2009-2026 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentPart;
import org.metaeffekt.core.document.model.DocumentPartType;
import org.metaeffekt.core.document.model.DocumentType;
import org.metaeffekt.core.inventory.processor.InventorySeparator;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CspLoader;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.util.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
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
@Slf4j
public class DocumentDescriptorReportGenerator {
    public static final String GEN_PATH = "genPath";

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
        generateLabelSvgs(documentDescriptor);

        // generate bookmaps to integrate InventoryReport-generated results
        DocumentDescriptorReport documentDescriptorReport = new DocumentDescriptorReport();
        documentDescriptorReport.setTargetReportDir(documentDescriptor.getTargetDocumentDir());
        documentDescriptorReport.createPartBookMap(documentDescriptor);
        documentDescriptorReport.createDocumentBookMap(documentDescriptor);
        documentDescriptorReport.createImprint(documentDescriptor);
    }

    static void deriveAssets(DocumentDescriptor documentDescriptor) {
        List<DocumentPart> newParts = new ArrayList<>();

        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            final List<InventoryContext> inventoryContexts = new ArrayList<>();

            boolean reportWithoutAsset = false;
            if (documentPart.getParams() != null && documentPart.getParams().containsKey("reportWithoutAsset")) {
                reportWithoutAsset = Boolean.parseBoolean(documentPart.getParams().get("reportWithoutAsset"));
            }

            for (InventoryContext inventoryContext : documentPart.getInventoryContexts()) {
                final String assetName = inventoryContext.getAssetName();
                final String assetVersion = inventoryContext.getAssetVersion();

                // 1. no assets are provided and reportWithoutAssets flag is set, we initialize with empty asset info
                if (reportWithoutAsset) {
                    inventoryContext.setAssetName("");
                    inventoryContext.setAssetVersion("");
                    inventoryContexts.add(inventoryContext);

                // 2. asset info is provided completely, initialize normally
                } else if (assetName != null && assetVersion != null) {
                    inventoryContexts.add(inventoryContext);

                // 3. only assetName is provided, set empty version
                } else if (assetName != null && assetVersion == null) {
                    inventoryContext.setAssetVersion("");
                    inventoryContexts.add(inventoryContext);

                // 4. only assetVersion is provided, invalid state
                } else if (assetName == null && assetVersion != null) {
                    throw new IllegalStateException("The field 'assetVersion' for inventoryContext [" + inventoryContext.getIdentifier() + "] is set, but no 'assetName' is specified, please set an 'assetName' as well or remove the field 'assetVersion'.");

                // 5. no assets are provided, asset information is derived from inventory
                } else {
                    if (documentPart.getDocumentPartType() == DocumentPartType.INITIAL_LICENSE_DOCUMENTATION) {
                        // separate handling for initial license documentation, since we want to report on all assets in
                        // the inventory, but do not want to generate the content for each asset separately
                        inventoryContexts.add(inventoryContext);
                    } else {
                        final List<Inventory> splitInventories = InventorySeparator.separate(inventoryContext.getInventory());
                        for (Inventory inventory : splitInventories) {
                            final Optional<AssetMetaData> primaryAsset = inventory.getAssetMetaData().stream()
                                    .filter(AssetMetaData::isPrimary)
                                    .findFirst();

                            String derivedName = primaryAsset
                                    .map(a -> a.get(AssetMetaData.Attribute.NAME))
                                    .orElseThrow(() -> new IllegalStateException("Missing asset name in primary asset for inventory [" + inventoryContext.getIdentifier() + "]. Please make sure that every primary asset has a specified name."));

                            String derivedVersion = primaryAsset
                                    .map(a -> a.get(AssetMetaData.Attribute.VERSION))
                                    .orElse("");

                            final String encodedAssetName = Base64.getEncoder().encodeToString(derivedName.getBytes());
                            InventoryContext derivedContext = new InventoryContext(inventory, encodedAssetName, inventoryContext.getReportContext(), inventoryContext.getLicensesPath(), inventoryContext.getComponentsPath());
                            derivedContext.setAssetName(derivedName);
                            derivedContext.setAssetVersion(derivedVersion);
                            inventoryContexts.add(derivedContext);
                        }
                    }
                }
            }

            if (documentPart.getDocumentPartType() == DocumentPartType.ANNEX && inventoryContexts.size() > 1) {
                for (InventoryContext ctx : inventoryContexts) {
                    String assetId = ctx.getInventory().getAssetMetaData().stream()
                            .filter(AssetMetaData::isPrimary)
                            .findFirst()
                            .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                            .orElse("unknown");

                    String normalizedAssetId = assetId.replaceAll("[^a-zA-Z0-9_-]", "_");
                    String newIdentifier = documentPart.getIdentifier() + "-" + normalizedAssetId;

                    DocumentPart newPart = new DocumentPart(
                            newIdentifier,
                            Collections.singletonList(ctx),
                            documentPart.getDocumentPartType(),
                            new HashMap<>(documentPart.getParams() != null ? documentPart.getParams() : Collections.emptyMap())
                    );
                    newParts.add(newPart);
                }
            } else {
                documentPart.setInventoryContexts(inventoryContexts);
                newParts.add(documentPart);
            }
        }
        documentDescriptor.setDocumentParts(newParts);
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

            List<InventoryContext> contexts = documentPart.getInventoryContexts();
            if (documentPart.getDocumentPartType() == DocumentPartType.CONTEXT) {
                // For parts like CONTEXT and PURPOSE that do not require an inventory
                Map<String, String> mergedParams;

                if (documentPart.getParams() != null && documentDescriptor.getParams() != null) {
                    mergedParams = mergeParams(documentDescriptor.getParams(), documentPart.getParams());
                } else if (documentPart.getParams() != null) {
                    mergedParams = new HashMap<>(documentPart.getParams());
                } else if (documentDescriptor.getParams() != null) {
                    mergedParams = new HashMap<>(documentDescriptor.getParams());
                } else {
                    mergedParams = new HashMap<>();
                }

                ReportConfigurationParameters configParams = buildReportConfiguration(documentPart, documentDescriptor, mergedParams);

                InventoryReport report = new InventoryReport(configParams);
                report.setReportContext(new ReportContext(documentPart.getIdentifier(), null, null));
                report.setTargetReportDir(new File(new File(documentDescriptor.getTargetDocumentDir(), "parts"), documentPart.getIdentifier()));

                if (!report.createReport()) {
                    throw new RuntimeException("Report creation failed for " + report);
                }
            } else {
                // for each inventory trigger according InventoryReport instances to produce
                for (InventoryContext inventoryContext : contexts) {
                    // validate each inventoryContext before processing
                    inventoryContext.validate();

                    Map<String, String> mergedParams;

                    if (documentPart.getParams() != null && documentDescriptor.getParams() != null) {
                        mergedParams = mergeParams(documentDescriptor.getParams(), documentPart.getParams());
                    } else if (documentPart.getParams() != null) {
                        mergedParams = new HashMap<>(documentPart.getParams());
                    } else if (documentDescriptor.getParams() != null) {
                        mergedParams = new HashMap<>(documentDescriptor.getParams());
                    } else {
                        mergedParams = new HashMap<>();
                    }

                    ReportConfigurationParameters configParams = buildReportConfiguration(documentPart, documentDescriptor, mergedParams);

                    InventoryReport report = new InventoryReport(configParams);
                    report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getAssetName(), inventoryContext.getAssetName()));

                    setPolicy(mergedParams, report, documentDescriptor);

                if (inventoryContext.getReferenceInventoryContext() != null) {
                    report.setReferenceInventory(inventoryContext.getReferenceInventoryContext().getInventory());
                    String referenceComponentsDir = inventoryContext.getReferenceInventoryContext().getComponentsPath();
                    if (referenceComponentsDir != null) {
                        report.setReferenceComponentsDir(referenceComponentsDir);
                    }
                    String referenceLicensesDir = inventoryContext.getReferenceInventoryContext().getLicensesPath();
                    if (referenceLicensesDir != null) {
                        report.setReferenceLicensesDir(referenceLicensesDir);
                    }
                } else {
                    report.setReferenceInventory(inventoryContext.getInventory());
                }
                report.setInventory(inventoryContext.getInventory());



                // the genPath specifies, where the SVGs are generated, it is relative to the targetDocumentDir of the document,
                // the InventoryReport however requires this path to be relative to its local targetReportDir (e.g. <targetDocumentDir>/parts/<partName>)
                if (mergedParams.get(GEN_PATH) != null) {
                    String partSvgPath = String.format("../../%s/%s", mergedParams.get(GEN_PATH), documentPart.getIdentifier());
                    report.setReportPartSvgPath(partSvgPath);
                }
                if (mergedParams.get("referenceLicensesDir") != null) {
                    report.setReferenceLicensesDir(mergedParams.get("referenceLicensesDir"));
                }
                if (mergedParams.get("referenceComponentsDir") != null) {
                    report.setReferenceComponentsDir(mergedParams.get("referenceComponentsDir"));
                }
                if (mergedParams.get("targetLicensesDir") == null) {
                    report.setTargetLicensesDir(new File(documentDescriptor.getTargetDocumentDir(), "licenses"));
                } else {
                    File targetLicensesDir = new File(mergedParams.get("targetLicensesDir"));
                    if (!targetLicensesDir.isAbsolute()) {
                        targetLicensesDir = new File(documentDescriptor.getTargetDocumentDir(), mergedParams.get("targetLicensesDir"));
                    }
                    report.setTargetLicensesDir(targetLicensesDir);
                }
                if (mergedParams.get("targetComponentsDir") == null) {
                    report.setTargetComponentsDir(new File(documentDescriptor.getTargetDocumentDir(), "components"));
                } else {
                    File targetComponentsDir = new File(mergedParams.get("targetComponentsDir"));
                    if (!targetComponentsDir.isAbsolute()) {
                        targetComponentsDir = new File(documentDescriptor.getTargetDocumentDir(), mergedParams.get("targetComponentsDir"));
                    }
                    report.setTargetComponentsDir(targetComponentsDir);
                }

                    report.setReportContext(new ReportContext(inventoryContext.getIdentifier(), inventoryContext.getAssetName(), inventoryContext.getAssetName()));

                    report.getReportContext().setReportInventoryName(inventoryContext.getAssetName());

                    report.setTargetReportDir(new File(new File(documentDescriptor.getTargetDocumentDir(), "parts"), documentPart.getIdentifier()));
                    report.getReportContext().setReportInventoryVersion(inventoryContext.getAssetVersion());

                    if (!report.createReport()) {
                        throw new RuntimeException("Report creation failed for " + report);
                    }
                }
            }
        }
    }

    private static void setPolicy(Map<String, String> params, InventoryReport report, DocumentDescriptor documentDescriptor) throws IOException {

        if (params == null) {
            log.info("no securityPolicyFile or secondarySecurityPolicyFile provided");
            return;
        }

        boolean hasPrimary = params.containsKey("securityPolicyFile");
        boolean hasSecondary = params.containsKey("secondarySecurityPolicyFile");

        if (!hasPrimary && !hasSecondary) {
            log.info("no securityPolicyFile or secondarySecurityPolicyFile provided");
            return;
        }

        List<File> files = new ArrayList<>();

        if (hasPrimary) {
            String primaryPathStr = params.get("securityPolicyFile");
            String resolvedPrimary = resolveAgainstBasePath(primaryPathStr, documentDescriptor.getBasePath());
            File primaryFile = resolvedPrimary != null ? new File(resolvedPrimary) : null;

            log.info("Using securityPolicyFile: {}", resolvedPrimary);
            files.add(primaryFile);
        }

        if (hasSecondary) {
            String optionalPathStr = params.get("secondarySecurityPolicyFile");
            String resolvedOptional = resolveAgainstBasePath(optionalPathStr, documentDescriptor.getBasePath());
            File optionalFile = resolvedOptional != null ? new File(resolvedOptional) : null;

            log.info("Using secondarySecurityPolicyFile: {}", resolvedOptional);
            files.add(optionalFile);
        }

        CspLoader securityPolicy = new CspLoader();
        securityPolicy.setFiles(files);

        if (params.containsKey("securityPolicyActiveIds")) {
            String activeIds = params.get("securityPolicyActiveIds");

            if (activeIds != null && !activeIds.trim().isEmpty()) {

                List<String> activeIdsList = Arrays.stream(activeIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                if (activeIdsList.isEmpty()) {
                    log.warn("No activeIds contained in 'securityPolicyActiveIds'. Please provide Ids as a comma-separated list.");
                }

                securityPolicy.setActiveIds(activeIdsList);

            } else {
                throw new IOException("No activeIds specified for parameter 'securityPolicyActiveIds'. Please provided Ids as a comma-separated list.");
            }
        }

        report.setSecurityPolicy(securityPolicy.loadConfiguration());
    }

    private static Map<String, String> mergeParams(Map<String, String> globalParams, Map<String, String> partParams) {
        Map<String, String> mergedParams = new HashMap<>(globalParams);
        mergedParams.putAll(partParams);

        return mergedParams;
    }

    public static String resolveAgainstBasePath(String filePath, String basePath) {
        if (filePath == null) {
            return null;
        }

        FileRef resolvedFilePath = (basePath != null)
                ? FileUtils.toAbsoluteOrReferencePath(filePath, basePath)
                : FileUtils.toAbsoluteOrReferencePath(filePath, new File(".").getAbsolutePath());

        return resolvedFilePath.getPath();
    }

    private static ReportConfigurationParameters buildReportConfiguration(DocumentPart documentPart, DocumentDescriptor documentDescriptor, Map<String, String> mergedParams
    ) {
        ReportConfigurationParameters.ReportConfigurationParametersBuilder builder = ReportConfigurationParameters.builder();

        switch (documentPart.getDocumentPartType()) {
            case ANNEX:
                builder.filterVulnerabilitiesNotCoveredByArtifacts(Boolean.parseBoolean(mergedParams.getOrDefault("vulnerabilitiesNotCoveredByArtifacts", "false")));
                builder.inventoryBomReportEnabled(true);
                break;
            case INITIAL_LICENSE_DOCUMENTATION:
                builder.assetBomReportEnabled(true);
                break;
            case LICENSE_DOCUMENTATION:
                builder.inventoryBomReportEnabled(true);
                break;
            case VULNERABILITY_REPORT:
                builder.filterVulnerabilitiesNotCoveredByArtifacts(Boolean.parseBoolean(mergedParams.getOrDefault("vulnerabilitiesNotCoveredByArtifacts", "false")));
                builder.inventoryVulnerabilityReportEnabled(true);
                break;
            case VULNERABILITY_STATISTICS_REPORT:
                builder.inventoryVulnerabilityStatisticsReportEnabled(true);
                break;
            case CONTEXT:
                builder.documentContextEnabled(true);
                if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT ||
                        documentDescriptor.getDocumentType() == DocumentType.PERIODIC_VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.context.remediation.enabled", "true");
                    mergedParams.putIfAbsent("document.context.prioritization.enabled", "true");
                    mergedParams.putIfAbsent("document.context.threshold.enabled", "true");
                    mergedParams.putIfAbsent("document.context.metrics.epss.enabled", "true");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                    mergedParams.putIfAbsent("document.context.remediation.enabled", "false");
                    mergedParams.putIfAbsent("document.context.prioritization.enabled", "false");
                    mergedParams.putIfAbsent("document.context.threshold.enabled", "false");
                    mergedParams.putIfAbsent("document.context.metrics.epss.enabled", "false");
                }

                if (documentDescriptor.getDocumentType() == DocumentType.PERIODIC_VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.context.intro.key", "document.context.intro.periodic.report");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                    mergedParams.putIfAbsent("document.context.intro.key", "document.context.intro.summary.report");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.context.intro.key", "document.context.intro.vulnerability.report");
                }
                break;
            case PURPOSE:
                builder.documentPurposeEnabled(true);
                if (documentDescriptor.getDocumentType() == DocumentType.PERIODIC_VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.purpose.intro.key", "document.purpose.intro.periodic");
                    mergedParams.putIfAbsent("document.purpose.query.period.enabled", "true");
                    mergedParams.putIfAbsent("document.purpose.subcomponent.enabled", "false");
                    mergedParams.putIfAbsent("document.purpose.external.data.enabled", "true");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                    mergedParams.putIfAbsent("document.purpose.intro.key", "document.purpose.intro.summary");
                    mergedParams.putIfAbsent("document.purpose.query.period.enabled", "false");
                    mergedParams.putIfAbsent("document.purpose.subcomponent.enabled", "false");
                    mergedParams.putIfAbsent("document.purpose.external.data.enabled", "false");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.purpose.intro.key", "document.purpose.intro.vulnerability");
                    mergedParams.putIfAbsent("document.purpose.query.period.enabled", "false");
                    mergedParams.putIfAbsent("document.purpose.subcomponent.enabled", "true");
                    mergedParams.putIfAbsent("document.purpose.external.data.enabled", "true");
                }
                break;
            case VULNERABILITY_SUMMARY_PART:
                builder.inventoryVulnerabilityReportSummaryEnabled(true);
                break;
            case NOTICE:
                builder.documentNoticeEnabled(true);
                if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT ||
                        documentDescriptor.getDocumentType() == DocumentType.PERIODIC_VULNERABILITY_REPORT) {
                    mergedParams.putIfAbsent("document.notice.prioritization.enabled", "true");
                } else if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
                    mergedParams.putIfAbsent("document.notice.prioritization.enabled", "false");
                }
                break;
            case VULNERABILITY_SUMMARY_REPORT:
                builder.assessmentReportEnabled(true);
                break;
        }

        builder.reportLanguage(documentDescriptor.getLanguage());

        // automatically propagate all parameters to the builder using reflection
        for (Map.Entry<String, String> entry : mergedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) continue;
            try {
                Method[] methods = builder.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(key) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (paramType == String.class) {
                            method.invoke(builder, value);
                        } else if (paramType == boolean.class || paramType == Boolean.class) {
                            method.invoke(builder, Boolean.parseBoolean(value));
                        } else if (paramType == int.class || paramType == Integer.class) {
                            method.invoke(builder, Integer.parseInt(value));
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not apply parameter '{}' to builder", key, e);
            }
        }

        // Ensure all parameters, including dynamically added ones with dots, are available in customParams
        builder.customParams(mergedParams);

        ReportConfigurationParameters configParams = builder.build();

        configParams.setAllFailConditions(false); // current default handling for all document types

        return configParams;
    }

    private static void generateLabelSvgs(DocumentDescriptor documentDescriptor) throws IOException {
        final DocumentType documentType = documentDescriptor.getDocumentType();
        if (documentType != DocumentType.VULNERABILITY_REPORT &&
                documentType != DocumentType.VULNERABILITY_STATISTICS_REPORT &&
                documentType != DocumentType.PERIODIC_VULNERABILITY_REPORT &&
                documentType != DocumentType.VULNERABILITY_SUMMARY_REPORT) {
            return;
        }

        final File targetDir = new File(documentDescriptor.getTargetDocumentDir(), "resources/svg/labels");
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Failed to create directory " + targetDir.getAbsolutePath());
        }

        final Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADERS, "class, file");
        properties.put("resource.loader.class.class", ClasspathResourceLoader.class.getName());
        properties.put(Velocity.INPUT_ENCODING, FileUtils.ENCODING_UTF_8);
        // disable strict runtime references to match InventoryReport behavior if needed
        properties.put(Velocity.RUNTIME_REFERENCES_STRICT, "false");
        properties.put("velocimacro.arguments.strict", "true");

        final VelocityEngine velocityEngine = new VelocityEngine(properties);
        final VelocityContext context = new VelocityContext();
        context.put("report", new InventoryReport());

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final String labelTemplatePath = InventoryReport.TEMPLATES_GENERIC_BASE_DIR + "/" + InventoryReport.TEMPLATE_GROUP_ASSESSMENT_LABELS + "/svg/";
        final Resource[] resources = resolver.getResources(labelTemplatePath + "*.svg.vt");

        for (Resource r : resources) {
            final String targetFileName = r.getFilename().replace(".vt", "");
            final File targetFile = new File(targetDir, targetFileName);

            log.info("Generating label SVG: {}", targetFile.getAbsolutePath());

            final Template template = velocityEngine.getTemplate(labelTemplatePath + r.getFilename());

            try (FileWriter writer = new FileWriter(targetFile)) {
                template.merge(context, writer);
            }
        }
    }
}
