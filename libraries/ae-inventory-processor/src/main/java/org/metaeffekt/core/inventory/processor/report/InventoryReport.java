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
package org.metaeffekt.core.inventory.processor.report;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.adapter.AssessmentReportAdapter;
import org.metaeffekt.core.inventory.processor.report.adapter.AssetReportAdapter;
import org.metaeffekt.core.inventory.processor.report.adapter.InventoryReportAdapter;
import org.metaeffekt.core.inventory.processor.report.adapter.VulnerabilityReportAdapter;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.inventory.processor.report.model.AssetData;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.RegExUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

@Getter
@Setter
public class InventoryReport {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReport.class);

    private static final String SEPARATOR_SLASH = "/";
    private static final String PATTERN_ANY_VT = "**/*.vt";

    private static final String TEMPLATES_BASE_DIR = "/META-INF/templates";

    private static final String TEMPLATES_TECHNICAL_BASE_DIR = TEMPLATES_BASE_DIR + SEPARATOR_SLASH + "technical";

    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_BOM = "inventory-report-bom";
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY = "inventory-report-vulnerability";
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY_SUMMARY = "inventory-report-vulnerability-summary";

    public static final String TEMPLATE_GROUP_INVENTORY_STATISTICS_VULNERABILITY = "inventory-statistics-vulnerability";

    public static final String TEMPLATE_GROUP_LABELS_VULNERABILITY_ASSESSMENT = "labels-vulnerability-assessment";

    public static final String TEMPLATE_GROUP_INVENTORY_POM = "inventory-pom";
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_DIFF = "inventory-report-diff";

    public static final String TEMPLATE_GROUP_ASSET_REPORT_BOM = "asset-report-bom";

    public static final String TEMPLATE_GROUP_ASSESSMENT_REPORT = "assessment-report";

    public static final String KEY_PREVIOUS_VERSION = "Previous Version";

    /**
     * The reference inventory is a complete file structure. referenceInventoryDir is the root folder.
     */
    private File referenceInventoryDir;

    /**
     * Include pattern for reference inventory in the file structure. Relative to referenceInventoryDir.
     */
    private String referenceInventoryIncludes;

    /**
     * Path to the component details. Relative to referenceInventoryDir.
     */
    private String referenceComponentPath;

    /**
     * Path to the license details. Relative to referenceInventoryDir.
     */
    private String referenceLicensePath;

    /**
     * The target path where to produce the reports.
     */
    private File targetReportDir;

    /**
     * The diff inventory is used for version diffs.
      */
    private File diffInventoryFile;

    // FIXME; dir / path ambiguity
    /**
     * The target directory for inventories.
     */
    private File targetInventoryDir;

    /**
     * The target directory for inventories.
     */
    private String targetInventoryPath;

    /**
     * The directory where components are aggregated.
     */
    private File targetComponentDir;

    /**
     * The directory where licenses are aggregated.
     */
    private File targetLicenseDir;

    /**
     * Project name initialized with default value.
     */
    private String projectName = "local project";

    private CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

    private ArtifactFilter artifactFilter;

    /**
     * This is the relative path as it will be used in the resulting dita templates. This needs
     * to be configured to match the relative path from the documentation to the licenses.
     */
    private String relativeLicensePath = "licenses";

    private List<Artifact> addOnArtifacts;

    // FIXME: do we want to further support this? This is for aggregating information in a reactor project.
    private transient Inventory lastProjectInventory;

    /**
     * The inventory for which to create the report.
     */
    private Inventory inventory;

    /**
     * The inventory for which to create the report.
     */
    private Inventory referenceInventory;

    /**
     * Default {@link ReportContext}.
     */
    private ReportContext reportContext = new ReportContext("default", null, null);

    private final ReportConfigurationParameters configParams;

    /**
     * Initializes ReportConfigurationParameters with default values.
     */
    public InventoryReport() {
        this.configParams = ReportConfigurationParameters.builder().build();
    }

    public InventoryReport(ReportConfigurationParameters configParams) {
        this.configParams = configParams;
    }

    public boolean createReport() throws IOException {
        logHeaderBox("Creating Inventory Report for project [" + getProjectName() + "]");
        if (LOG.isDebugEnabled()) {
            this.logConfiguration();
        }

        final Inventory globalInventory = readGlobalInventory();

        // read local repository
        final Inventory localRepositoryInventory;
        if (this.inventory != null) {
            localRepositoryInventory = this.inventory;
        } else {
            localRepositoryInventory = new Inventory();
        }

        // insert (potentially incomplete) artifacts for reporting
        // this supports adding licenses and obligations into the
        // report, which are not covered by the repository.
        if (addOnArtifacts != null) {
            localRepositoryInventory.getArtifacts().addAll(addOnArtifacts);
        }

        // FIXME: WORKAROUND
        // if the given repository already contains LMD content we take it into the global inventory
        for (LicenseMetaData lmd : localRepositoryInventory.getLicenseMetaData()) {
            LicenseMetaData globalLmd = globalInventory.findMatchingLicenseMetaData(lmd.getComponent(), lmd.getLicense(), lmd.getVersion());
            if (globalLmd == null) {
                globalInventory.getLicenseMetaData().add(lmd);
            }
        }

        return createReport(globalInventory, localRepositoryInventory);
    }

    protected Inventory readGlobalInventory() throws IOException {
        if (referenceInventory != null) {
            return referenceInventory;
        }
        if (referenceInventoryDir == null || referenceInventoryIncludes == null) {
            return new Inventory();
        }
        LOG.info("Creating global inventory for inventory report by combining inventories from {}: file://{}",
                referenceInventoryDir.isDirectory() ? "directory" : "file", referenceInventoryDir.getAbsolutePath());
        return InventoryUtils.readInventory(referenceInventoryDir, referenceInventoryIncludes);
    }

    protected void checkReportInventory(Inventory inventory) {
        final int totalSize = inventory.getArtifacts().size() + inventory.getAssetMetaData().size() + inventory.getLicenseMetaData().size() + inventory.getVulnerabilityMetaData().size();
        if (totalSize == 0) {
            LOG.warn("The provided inventory appears to be empty: {}", inventory.getInventorySizePrintString());
        }
    }

    protected boolean createReport(Inventory globalInventory, Inventory localInventory) throws IOException {
        final File targetInventoryFile = targetInventoryDir != null ? new File(targetInventoryDir, targetInventoryPath) : null;

        Inventory diffInventory = null;

        // read the reference file if specified
        if (diffInventoryFile != null) {
            diffInventory = new InventoryReader().readInventory(diffInventoryFile);
        }

        LOG.debug("Constructing project inventory from local inventory {} and global inventory {}", globalInventory.getInventorySizePrintString(), localInventory.getInventorySizePrintString());

        // FIXME-KKL: why don't we simply use the local inventory
        final Inventory projectInventory = new Inventory();
        this.lastProjectInventory = projectInventory;

        // transfer component patterns from scan inventory (these may include wildcard checksum replacements)
        projectInventory.inheritComponentPatterns(localInventory, false);

        // transfer identified assets from scan
        projectInventory.inheritAssetMetaData(localInventory, false);

        // transfer license data
        projectInventory.inheritLicenseData(localInventory, false);

        // transfer available vulnerability information
        projectInventory.inheritVulnerabilityMetaData(localInventory, false);

        // transfer available cert information
        projectInventory.inheritCertMetaData(localInventory, false);

        // transfer inventory info
        projectInventory.inheritInventoryInfo(localInventory, false);

        localInventory.sortArtifacts();

        final List<Artifact> list = localInventory.getArtifacts();

        boolean unknown = false;
        boolean unknownVersion = false;
        boolean development = false;
        boolean internal = false;
        boolean error = false;
        boolean banned = false;
        boolean upgrade = false;
        boolean downgrade = false;

        boolean missingLicense = false;

        final Map<String, Integer> classifierMessageCount = new HashMap<>();

        for (Artifact localArtifact : list) {
            localArtifact.deriveArtifactId();

            boolean localError = false;
            boolean localBanned = false;
            boolean localWarn = false;
            boolean localUnknown = false;
            boolean localUnknownVersion = false;
            boolean localDevelopment = false;
            boolean localInternal = false;
            boolean localUpgrade = false;
            boolean localDowngrade = false;

            boolean localMissingLicense = false;

            Artifact matchedReferenceArtifact = globalInventory.findArtifactByIdAndChecksum(localArtifact.getId(), localArtifact.getChecksum());
            if (matchedReferenceArtifact == null) {
                matchedReferenceArtifact = globalInventory.findArtifact(localArtifact);
            }
            if (matchedReferenceArtifact == null) {
                matchedReferenceArtifact = globalInventory.findArtifact(localArtifact, true);
            }

            LOG.debug("Query for local artifact: " + localArtifact);
            LOG.debug("Matched reference artifact: " + matchedReferenceArtifact);

            String classifier = "";
            String comment = "";

            String artifactFileId = localArtifact.getId();
            if (matchedReferenceArtifact != null) {
                comment = matchedReferenceArtifact.getComment();

                // in case the group id does not contain anything we
                // infer the group id from the localArtifact from the repository. Better than nothing.
                if (!StringUtils.isNotBlank(matchedReferenceArtifact.getGroupId())) {
                    matchedReferenceArtifact.setGroupId(localArtifact.getGroupId());
                }

                String classification = matchedReferenceArtifact.getClassification();
                localWarn = true;
                if (classification != null) {
                    if (classification.contains("upgrade")) {
                        classifier += "[upgrade]";
                        localUpgrade = true;
                        if (configParams.isFailOnUpgrade()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("downgrade")) {
                        classifier += "[downgrade]";
                        localDowngrade = true;
                        if (configParams.isFailOnDowngrade()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("development")) {
                        classifier += "[development]";
                        localDevelopment = true;
                        localWarn = false;
                        if (configParams.isFailOnDevelopment()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("internal")) {
                        classifier += "[internal]";
                        localInternal = true;
                        if (configParams.isFailOnInternal()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("banned")) {
                        classifier += "[banned]";
                        localBanned = true;
                        if (configParams.isFailOnBanned()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("unknown")) {
                        classifier += "[unknown]";
                        localUnknown = true;
                        if (configParams.isFailOnUnknown()) {
                            localError = true;
                        }
                    }
                    if (classification.contains("hint")) {
                        classifier += "[hint]";
                        localWarn = false;
                    }
                }
                if (!StringUtils.isNotBlank(matchedReferenceArtifact.getLicense())) {
                    classifier += "[no license]";
                    localMissingLicense = true;
                    if (configParams.isFailOnMissingLicense()) {
                        localError = true;
                    }
                }
            } else {
                classifier += "[unknown]";
                localUnknown = true;
                if (configParams.isFailOnUnknown()) {
                    localError = true;
                }
            }

            if (artifactFilter == null || !artifactFilter.filter(localArtifact)) {
                Artifact reportArtifact = localArtifact;
                if (matchedReferenceArtifact != null) {
                    // ids may vary; we try to preserve the id here
                    Artifact copy = new Artifact(matchedReferenceArtifact);
                    copy.setId(artifactFileId);

                    if (ASTERISK.equals(matchedReferenceArtifact.getVersion())) {
                        // in this case we also to manage the version (and replace the wildcard with the concrete version)
                        final String id = matchedReferenceArtifact.getId();
                        try {
                            int index = id.indexOf(ASTERISK);
                            String prefix = id.substring(0, index);
                            String suffix = id.substring(index + 1);
                            String version = artifactFileId.substring(prefix.length(),
                                    artifactFileId.length() - suffix.length());
                            copy.setVersion(version);
                            copy.set(Constants.KEY_WILDCARD_MATCH, STRING_TRUE);
                        } catch (Exception e) {
                            LOG.error("Cannot extract version from artifact {}. To express that no version information " +
                                    "is available use a different version keyword such as 'undefined' or 'unspecific'.", id);
                        }
                    }

                    // override flags (the original data does not include context)
                    copy.setRelevant(localArtifact.isRelevant());
                    copy.setManaged(localArtifact.isManaged());
                    copy.setRootPaths(localArtifact.getRootPaths());
                    projectInventory.getArtifacts().add(copy);

                    // handle checksum
                    if (!StringUtils.isNotBlank(copy.getChecksum())) {
                        copy.setChecksum(localArtifact.getChecksum());
                    } else {
                        if (StringUtils.isNotBlank(localArtifact.getChecksum()) && !copy.getChecksum().equalsIgnoreCase(localArtifact.getChecksum())) {
                            throw new IllegalStateException(String.format("Checksum mismatch for %s.", localArtifact.getId()));
                        }
                    }

                    // copy details not covered by reference artifact to resulting artifact
                    for (String attributeKey : localArtifact.getAttributes()) {
                        final String currentValue = copy.get(attributeKey);
                        if (!StringUtils.isNotBlank(currentValue)) {
                            copy.set(attributeKey, localArtifact.get(attributeKey));
                        }
                    }

                    reportArtifact = matchedReferenceArtifact;
                } else {
                    projectInventory.getArtifacts().add(localArtifact);
                    localArtifact.setManaged(true);
                }

                // log information
                if (!classifier.isEmpty()) {
                    final String artifactQualifier = reportArtifact.deriveQualifier();
                    if (StringUtils.isNotBlank(comment)) {
                        comment = "- " + comment;
                    } else {
                        comment = "";
                    }
                    if (localArtifact.isRelevant() || localArtifact.isManaged()) {
                        final String messagePattern = "{} for artifact [{}] {}";
                        if (localError) {
                            // all errors are logged
                            LOG.error(messagePattern, classifier, artifactQualifier, comment);
                        } else if (localWarn) {

                            // only 10 warnings of each kind (classifier) are logged to not pollute the log.
                            Integer count = classifierMessageCount.get(classifier);
                            count = count == null ? 1 : count + 1;
                            classifierMessageCount.put(classifier, count);

                            if (count <= 10) {
                                LOG.warn(messagePattern, classifier, artifactQualifier, comment);
                            }
                            if (count == 10) {
                                LOG.warn("{} - logging no further warnings for classifier.", classifier);
                            }
                        } else {
                            LOG.info(messagePattern, classifier, artifactQualifier, comment);
                        }
                    } else {
                        if (classifier.contains("[hint]")) {
                            if (!localArtifact.isRelevant()) {
                                final String messagePattern = "{} {} {} (not relevant for report)";
                                LOG.info(messagePattern, classifier, artifactQualifier, comment);
                            } else {
                                final String messagePattern = "{} {} {} (not managed)";
                                LOG.info(messagePattern, classifier, artifactQualifier, comment);
                            }
                        }
                    }
                }

                // escalate the error only in case the localArtifact is relevant for reporting
                if (localArtifact.isManaged()) {
                    error |= localError;
                    banned |= localBanned;
                    unknown |= localUnknown;
                    unknownVersion |= localUnknownVersion;
                    development |= localDevelopment;
                    internal |= localInternal;
                    upgrade |= localUpgrade;
                    downgrade |= localDowngrade;
                    missingLicense |= localMissingLicense;
                }
            }
        }

        // before writing reports sort all the artifacts
        projectInventory.sortArtifacts();

        // perform additional processing steps
        for (Artifact artifact : projectInventory.getArtifacts()) {
            artifact.setArtifactId(null);
            artifact.deriveArtifactId();
        }
        projectInventory.mergeDuplicates();

        // transfer license metadata into project inventory (required for the license related reports)
        projectInventory.inheritLicenseMetaData(globalInventory, false);
        projectInventory.filterLicenseMetaData();

        // transfer license data
        projectInventory.inheritLicenseData(globalInventory, false);

        // transfer available vulnerability information
        projectInventory.inheritVulnerabilityMetaData(globalInventory, false);

        // transfer available cert information
        projectInventory.inheritCertMetaData(globalInventory, false);

        // transfer available asset information
        projectInventory.inheritAssetMetaData(globalInventory, false);

        LOG.debug("Project inventory constructed: {}", projectInventory.getInventorySizePrintString());
        this.checkReportInventory(projectInventory);

        // filter the vulnerability metadata to only cover the items remaining in the inventory
        if (configParams.isFilterVulnerabilitiesNotCoveredByArtifacts()) {
            projectInventory.filterVulnerabilityMetaData();
            LOG.debug("Project inventory after filtering using [filterVulnerabilitiesNotCoveredByArtifacts]: {}", projectInventory.getInventorySizePrintString());
        }

        // FIXME: we need to unify this; filtering is more or less obsolete
        final Inventory filteredInventory = projectInventory.getFilteredInventory();

        // build adapters
        final InventoryReportAdapters inventoryReportAdapters = new InventoryReportAdapters(
                new AssetReportAdapter(filteredInventory),
                new VulnerabilityReportAdapter(projectInventory, securityPolicy),
                new AssessmentReportAdapter(projectInventory, securityPolicy),
                new InventoryReportAdapter(filteredInventory));

        // write reports
        if (configParams.isInventoryBomReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_INVENTORY_REPORT_BOM, reportContext);
        }

        if (configParams.isInventoryDiffReportEnabled()) {
            writeDiffReport(diffInventory, projectInventory, reportContext);
        }

        if (configParams.isInventoryVulnerabilityReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY, reportContext);
        }

        if (configParams.isInventoryVulnerabilityReportSummaryEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY_SUMMARY, reportContext);
        }

        if (configParams.isInventoryVulnerabilityStatisticsReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_INVENTORY_STATISTICS_VULNERABILITY, reportContext);
        }

        // all vulnerability-related templates require to generate labels
        if (configParams.isInventoryVulnerabilityReportEnabled() || configParams.isInventoryVulnerabilityStatisticsReportEnabled() || configParams.isAssessmentReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_LABELS_VULNERABILITY_ASSESSMENT, reportContext);
        }

        if (configParams.isInventoryPomEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_TECHNICAL_BASE_DIR, TEMPLATE_GROUP_INVENTORY_POM, reportContext);
        }

        if (configParams.isAssetBomReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_ASSET_REPORT_BOM, reportContext);
        }

        if (configParams.isAssessmentReportEnabled()) {
            writeReports(projectInventory, filteredInventory, inventoryReportAdapters,
                    TEMPLATES_BASE_DIR, TEMPLATE_GROUP_ASSESSMENT_REPORT, reportContext);
        }

        // evaluate licenses only for managed artifacts
        final List<String> licenses = projectInventory.evaluateLicenses(false, true);

        // evaluate / copy / check checks on license and notice files
        boolean missingLicenseFile = evaluateLicenseFiles(projectInventory);
        boolean missingNotice = evaluateNotices(projectInventory, licenses);

        // TODO: verify availability of source artifacts to complement distribution

        // write report xls
        if (targetInventoryFile != null) {
            // ensure directory exists
            targetInventoryFile.getParentFile().mkdirs();

            // write the report
            try {
                new InventoryWriter().writeInventory(projectInventory, targetInventoryFile);
                LOG.info("Report inventory written to [{}].", getTargetInventoryPath());
            } catch (Exception e) {
                LOG.error("Unable to write inventory to [{}]. Skipping write.", getTargetInventoryPath(), e);
            }
        }

        if (!error && !unknown && !unknownVersion && !development && !internal && !upgrade && !downgrade &&
                !missingLicenseFile && !missingNotice) {
            LOG.info("No findings!");
        }

        if (error && configParams.isFailOnError()) {
            return false;
        }
        if (banned && configParams.isFailOnBanned()) {
            return false;
        }
        if (unknown && configParams.isFailOnUnknown()) {
            return false;
        }
        if (unknownVersion && configParams.isFailOnUnknownVersion()) { // is always false
            return false;
        }
        if (downgrade && configParams.isFailOnDowngrade()) {
            return false;
        }
        if (development && configParams.isFailOnDevelopment()) {
            return false;
        }
        if (internal && configParams.isFailOnInternal()) {
            return false;
        }
        if (upgrade && configParams.isFailOnUpgrade()) {
            return false;
        }
        if (missingLicense && configParams.isFailOnMissingLicense()) {
            return false;
        }
        if (missingLicenseFile && configParams.isFailOnMissingLicenseFile()) {
            return false;
        }
        if (missingNotice && configParams.isFailOnMissingNotice()) {
            return false;
        }
        return true;
    }

    @Getter
    public static class InventoryReportAdapters {
        final AssetReportAdapter assetReportAdapter;
        final VulnerabilityReportAdapter vulnerabilityReportAdapter;
        final AssessmentReportAdapter assessmentReportAdapter;
        final InventoryReportAdapter inventoryReportAdapter;

        private InventoryReportAdapters(AssetReportAdapter assetReportAdapter, VulnerabilityReportAdapter vulnerabilityReportAdapter,
                    AssessmentReportAdapter assessmentReportAdapter, InventoryReportAdapter inventoryReportAdapter) {
            this.assetReportAdapter = assetReportAdapter;
            this.vulnerabilityReportAdapter = vulnerabilityReportAdapter;
            this.assessmentReportAdapter = assessmentReportAdapter;
            this.inventoryReportAdapter = inventoryReportAdapter;
        }
    }

    protected void writeReports(Inventory projectInventory, Inventory filteredInventory, InventoryReportAdapters report,
                String templateBaseDir, String templateGroup, ReportContext reportContext) throws IOException {

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final String vtClasspathResourcePattern = templateBaseDir + SEPARATOR_SLASH + templateGroup + SEPARATOR_SLASH + PATTERN_ANY_VT;
        final Resource[] resources = resolver.getResources(vtClasspathResourcePattern);
        final Resource parentResource = resolver.getResource(templateBaseDir);
        final String parentPath = parentResource.getURI().toASCIIString();

        for (Resource r : resources) {
            String filePath = r.getURI().toASCIIString();
            String path = filePath.replace(parentPath, "");
            filePath = templateBaseDir + path;
            String targetFileName = r.getFilename().replace(".vt", "");

            File relPath = new File(path.replace("/" + templateGroup + "/", "")).getParentFile();
            final File targetReportPath = new File(this.targetReportDir, new File(relPath, targetFileName).toString());

            produceDita(projectInventory, filteredInventory, report, filePath, targetReportPath, reportContext);
        }
    }

    private void produceDita(Inventory projectInventory, Inventory filteredInventory,
                             InventoryReportAdapters inventoryReportAdapters,
                             String templateResourcePath, File target, ReportContext reportContext) throws IOException {
        LOG.info("Producing Dita for template [{}]", templateResourcePath);

        final Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADER, "class, file");
        properties.put("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        properties.put(Velocity.INPUT_ENCODING, FileUtils.ENCODING_UTF_8);
        properties.put(Velocity.OUTPUT_ENCODING, FileUtils.ENCODING_UTF_8);
        properties.put(Velocity.SET_NULL_ALLOWED, true);
        //https://velocity.apache.org/engine/1.7/developer-guide.html#velocimacro
        properties.put("velocimacro.arguments.strict", "true");

        final VelocityEngine velocityEngine = new VelocityEngine(properties);
        final Template template = velocityEngine.getTemplate(templateResourcePath);
        final StringWriter sw = new StringWriter();
        final VelocityContext context = new VelocityContext();
        final ReportUtils reportUtils = new ReportUtils();
        reportUtils.setLang(configParams.getReportLanguage());
        reportUtils.setContext(context);

        // regarding the report we only use the filtered inventory for the time being
        context.put("inventory", filteredInventory);
        context.put("vulnerabilityAdapter", inventoryReportAdapters.getVulnerabilityReportAdapter());
        context.put("assessmentReportAdapter", inventoryReportAdapters.getAssessmentReportAdapter());
        context.put("assetAdapter", inventoryReportAdapters.getAssetReportAdapter());
        context.put("inventoryReportAdapter", inventoryReportAdapters.getInventoryReportAdapter());
        context.put("report", this);
        context.put("StringEscapeUtils", org.apache.commons.lang.StringEscapeUtils.class);
        context.put("RegExUtils", RegExUtils.class);
        context.put("utils", reportUtils);

        context.put("Double", Double.class);
        context.put("Float", Float.class);
        context.put("String", String.class);

        context.put("targetReportDir", this.targetReportDir);

        context.put("reportContext", reportContext);
        context.put("configParams", this.configParams);
        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    public boolean evaluateNotices(Inventory projectInventory, List<String> licenses) {
        boolean missingNotice = false;
        final List<LicenseMetaData> licenseMetaDataList = projectInventory.getLicenseMetaData();
        final Set<String> licensesRequiringNotice = new HashSet<>();
        for (String license : licenses) {
            for (LicenseMetaData licenseMetaData : licenseMetaDataList) {
                if (licenseMetaData.getLicense().equalsIgnoreCase(license)) {
                    licensesRequiringNotice.add(license);
                }
            }
        }
        for (Artifact artifact : projectInventory.getArtifacts()) {
            if (!artifact.isEnabledForDistribution()) {
                continue;
            }
            final String license = artifact.getLicense();
            if (StringUtils.isNotBlank(license) && artifact.isRelevant()) {
                if (licensesRequiringNotice.contains(license)) {
                    LicenseMetaData licenseMetaData = projectInventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData == null) {
                        final String messagePattern = "No notice for artifact '{}' with license '{}'.";
                        if (configParams.isFailOnMissingNotice()) {
                            LOG.error(messagePattern, artifact.createStringRepresentation(), license);
                            return true;
                        } else {
                            LOG.warn(messagePattern, artifact.createStringRepresentation(), license);
                        }
                        missingNotice = true;
                    }
                }
            }
        }
        return missingNotice;
    }

    /**
     * Checks whether the required folders and files exist.
     *
     * @param licenseFolderName
     * @param targetDir
     * @param reportedLicenseFolders
     * @return true if all information is available.
     */
    private boolean checkAndCopyLicenseFolder(String licenseFolderName, File targetDir,
                                              Set<String> reportedLicenseFolders) {

        File sourceLicenseRootDir = new File(getGlobalInventoryDir(), referenceLicensePath);

        File derivedFile = new File(sourceLicenseRootDir, licenseFolderName);
        if (derivedFile.exists()) {
            copyFolderContent(sourceLicenseRootDir, licenseFolderName, targetDir);
        } else {
            if (!reportedLicenseFolders.contains(licenseFolderName)) {
                if (configParams.isFailOnMissingLicenseFile()) {
                    LOG.error("[missing license file] in folder [{}]", licenseFolderName);
                } else {
                    if (reportedLicenseFolders.size() <= 10) {
                        LOG.warn("[missing license file] in folder [{}]", licenseFolderName);
                    }
                    if (reportedLicenseFolders.size() == 10) {
                        LOG.warn("[missing license file] - logging no further warnings for classifier.");
                    }
                }
                reportedLicenseFolders.add(licenseFolderName);
                return true;
            }
        }
        return false;
    }

    private boolean checkAndCopyComponentFolder(String componentFolderName, File targetDir,
                                                Set<String> reportedLicenseFolders) {

        File sourceComponentRootDir = new File(getGlobalInventoryDir(), referenceComponentPath);

        File derivedFile = new File(sourceComponentRootDir, componentFolderName);
        if (derivedFile.exists()) {
            copyFolderContent(sourceComponentRootDir, componentFolderName, targetDir);
        } else {
            if (!reportedLicenseFolders.contains(componentFolderName)) {
                if (configParams.isFailOnMissingComponentFiles()) {
                    LOG.error("[missing component specific license file] in folder '{}'", componentFolderName);
                } else {
                    if (reportedLicenseFolders.size() <= 10) {
                        LOG.warn("[missing component specific license file] in folder '{}'", componentFolderName);
                    }
                    if (reportedLicenseFolders.size() == 10) {
                        LOG.warn("[missing component specific license file] - logging no further warnings for classifier.");
                    }
                }
                reportedLicenseFolders.add(componentFolderName);
                return true;
            }
        }
        return false;
    }

    private boolean evaluateLicenseFiles(Inventory projectInventory) {
        final Set<String> reportedSourceFolders = new HashSet<>();

        boolean missingFiles = false;

        for (Artifact artifact : projectInventory.getArtifacts()) {
            // skip artifact which are not ready for distribution
            if (!artifact.isEnabledForDistribution()) {
                continue;
            }

            final String componentName = artifact.getComponent();
            final String sourceLicense = artifact.getLicense();

            // without source license, no license meta data, no license texts / notices
            if (!StringUtils.isNotBlank(sourceLicense)) {
                continue;
            }
            if (!StringUtils.isNotBlank(componentName)) {
                continue;
            }

            final String version = artifact.getVersion();
            boolean isArtifactVersionWildcard =
                    // when a wildcard version was not resolved
                    ASTERISK.equalsIgnoreCase(version) ||
                            // when a wildcard version was resolved and the artifact was marked
                            STRING_TRUE.equalsIgnoreCase(artifact.get(Constants.KEY_WILDCARD_MATCH)) ||
                            // when (unresolved) version placeholders are used
                            (version != null && version.startsWith(VERSION_PLACHOLDER_PREFIX) &&
                                    version.endsWith(VERSION_PLACHOLDER_SUFFIX));
            boolean isUndefinedVersion = artifact.getVersion() == null;

            // try to resolve component license meta data if available
            final LicenseMetaData matchingLicenseMetaData = projectInventory.
                    findMatchingLicenseMetaData(componentName, sourceLicense, version);

            // derive effective licenses
            String effectiveLicense = artifact.getLicense();
            boolean isMetaDataVersionWildcard = false;
            if (matchingLicenseMetaData != null) {
                effectiveLicense = matchingLicenseMetaData.deriveLicenseInEffect();

                // NOTE: whether the version-specific component folder is used sololy dependeds on the version
                // attached to the artifact. A wildcard on LMD-level has different semantics. Therefore
                // the matchingLicenseMetaData.getVersion() is not relevant here.
            }
            effectiveLicense = effectiveLicense.replaceAll("/s*,/s*", "|");

            // derive version (unspecific, specific)
            final String versionUnspecificComponentFolder = LicenseMetaData.deriveComponentFolderName(componentName);
            final String versionSpecificComponentFolder = LicenseMetaData.deriveComponentFolderName(componentName, version);

            // determine source path on license meta data level the version in wildcard; we derived all versions
            // have the same license; one notice for all; one source folder for all
            final String sourcePath = (isMetaDataVersionWildcard || isArtifactVersionWildcard || isUndefinedVersion) ?
                    versionUnspecificComponentFolder : versionSpecificComponentFolder;

            // determine target path (always artifact version centric; no information may be available)
            final String targetPath = isArtifactVersionWildcard || isUndefinedVersion ?
                    versionUnspecificComponentFolder : versionSpecificComponentFolder;

            // copy touched components to target component folder
            if (targetComponentDir != null) {
                missingFiles |= checkAndCopyComponentFolder(sourcePath,
                        new File(targetComponentDir, targetPath), reportedSourceFolders);
            }

            // now sort component folders into the effective license folders
            if (targetLicenseDir != null) {
                final String[] effectiveLicenses = effectiveLicense.split("\\|");
                for (String licenseInEffect : effectiveLicenses) {
                    final String effectiveLicenseFolderName = LicenseMetaData.deriveLicenseFolderName(licenseInEffect);

                    // in any case copy the license license folder
                    missingFiles |= checkAndCopyLicenseFolder(effectiveLicenseFolderName,
                            new File(targetLicenseDir, effectiveLicenseFolderName), reportedSourceFolders);

                    // copy the component folder into the effective license folder
                    final File licenseTargetDir = new File(targetLicenseDir, effectiveLicenseFolderName);
                    missingFiles |= checkAndCopyComponentFolder(sourcePath,
                            new File(licenseTargetDir, targetPath), reportedSourceFolders);
                }
            }
        }
        return missingFiles;
    }

    protected void writeDiffReport(Inventory referenceInventory, Inventory projectInventory, ReportContext reportContext) throws IOException {
        if (referenceInventory != null) {
            for (Artifact artifact : projectInventory.getArtifacts()) {
                if (artifact.getId() == null) {
                    continue;
                }
                Artifact foundArtifact = referenceInventory.findArtifact(artifact, true);
                // if not found try to match current using the find current strategies
                // this supports to manipulate the match based on the content in the
                // reference inventory.
                if (foundArtifact == null) {
                    foundArtifact = referenceInventory.findArtifactClassificationAgnostic(artifact);
                }
                if (foundArtifact != null) {
                    artifact.set(KEY_PREVIOUS_VERSION, foundArtifact.getVersion());
                }
            }
        }

        // filter all artifacts were the version did not change
        final Inventory baseFilteredInventory = new Inventory();
        for (Artifact artifact : projectInventory.getArtifacts()) {
            if (artifact.getVersion() != null &&
                    !artifact.getVersion().trim().equals(artifact.get(KEY_PREVIOUS_VERSION))) {
                baseFilteredInventory.getArtifacts().add(artifact);
            }
        }

        final Inventory filteredInventory = baseFilteredInventory.getFilteredInventory();

        final InventoryReportAdapters inventoryReportAdapters = new InventoryReportAdapters(
                new AssetReportAdapter(baseFilteredInventory),
                new VulnerabilityReportAdapter(baseFilteredInventory, securityPolicy),
                new AssessmentReportAdapter(baseFilteredInventory, securityPolicy),
                new InventoryReportAdapter(baseFilteredInventory));

        writeReports(baseFilteredInventory, filteredInventory, inventoryReportAdapters, TEMPLATES_BASE_DIR, TEMPLATE_GROUP_INVENTORY_REPORT_DIFF, reportContext);
    }

    /**
     * Copies all files located in sourceRootDir/sourcePath to targetRootDir. No subdirectories are
     * copied.
     *
     * @param sourceRootDir
     * @param sourcePath
     * @param targetDir
     */
    private void copyFolderContent(File sourceRootDir, String sourcePath, File targetDir) {
        Copy copy = new Copy();
        copy.setProject(new Project());
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File(sourceRootDir, sourcePath));
        fileSet.setIncludes("**/*");
        copy.setIncludeEmptyDirs(false);
        copy.setFailOnError(true);
        copy.setOverwrite(true);
        copy.addFileset(fileSet);
        copy.setTodir(targetDir);
        copy.perform();

        // verify something was copied
        if (!new File(sourceRootDir, sourcePath).exists()) {
            throw new IllegalStateException("Files copied, but no result in found in target location");
        }
    }

    private static void logHeaderBox(String str) {
        LOG.info(repeat("*", str.length() + 4));
        LOG.info("* {} *", str);
        LOG.info(repeat("*", str.length() + 4));
    }

    private void logConfiguration() {
        LOG.debug("Report configuration:");

        LOG.debug("- Source/Target paths:");
        logConfigurationLogFile("referenceInventoryDir", referenceInventoryDir);
        logConfigurationLogToString("referenceInventoryIncludes", referenceInventoryIncludes);
        logConfigurationLogToString("referenceComponentPath", referenceComponentPath);
        logConfigurationLogToString("referenceLicensePath", referenceLicensePath);
        logConfigurationLogFile("targetReportDir", targetReportDir);
        logConfigurationLogFile("diffInventoryFile", diffInventoryFile);
        logConfigurationLogFile("targetInventoryDir", targetInventoryDir);
        logConfigurationLogToString("targetInventoryPath", targetInventoryPath);
        logConfigurationLogFile("targetComponentDir", targetComponentDir);
        logConfigurationLogFile("targetLicenseDir", targetLicenseDir);
        logConfigurationLogToString("relativeLicensePath", relativeLicensePath);

        LOG.debug("- Validation fail flags:");
        LOG.debug(" - [failOnError: {}] [failOnBanned: {}] [failOnDowngrade: {}] [failOnUnknown: {}] [failOnUnknownVersion: {}] [failOnDevelopment: {}] [failOnInternal: {}]", configParams.isFailOnError(),  configParams.isFailOnBanned(),  configParams.isFailOnDowngrade(),  configParams.isFailOnUnknown(),  configParams.isFailOnUnknownVersion(),  configParams.isFailOnDevelopment(),  configParams.isFailOnInternal());
        LOG.debug("   [failOnUpgrade: {}] [failOnMissingLicense: {}] [failOnMissingLicenseFile: {}] [failOnMissingNotice: {}] [failOnMissingComponentFiles: {}]",  configParams.isFailOnUpgrade(),  configParams.isFailOnMissingLicense(),  configParams.isFailOnMissingLicenseFile(),  configParams.isFailOnMissingNotice(),  configParams.isFailOnMissingComponentFiles());

        LOG.debug("- Data display settings:");
        logConfigurationLogToString("artifactFilter", artifactFilter);
        logConfigurationLogToString("filterVulnerabilitiesNotCoveredByArtifacts", configParams.isFilterVulnerabilitiesNotCoveredByArtifacts());
        logConfigurationLogToString("filterAdvisorySummary", configParams.isFilterAdvisorySummary());

        LOG.debug("- Template settings:");
        LOG.debug(" - [inventoryBomReportEnabled: {}] [inventoryDiffReportEnabled: {}] [inventoryPomEnabled: {}] [inventoryVulnerabilityReportEnabled: {}]",  configParams.isInventoryBomReportEnabled(),  configParams.isInventoryDiffReportEnabled(),  configParams.isInventoryPomEnabled(),  configParams.isInventoryVulnerabilityReportEnabled());
        LOG.debug("   [inventoryVulnerabilityReportSummaryEnabled: {}] [inventoryVulnerabilityStatisticsReportEnabled: {}]",  configParams.isInventoryVulnerabilityReportSummaryEnabled(),  configParams.isInventoryVulnerabilityStatisticsReportEnabled());
        logConfigurationLogToString("templateLanguageSelector", configParams.getReportLanguage());

        LOG.debug("- Addon data:");
        if (addOnArtifacts == null) {
            LOG.debug(" - addOnArtifacts: <null>");
        } else if (addOnArtifacts.isEmpty()) {
            LOG.debug(" - addOnArtifacts: <empty>");
        } else {
            LOG.debug(" - addOnArtifacts: {}", addOnArtifacts.stream().map(Artifact::getId).collect(Collectors.toList()));
        }

        securityPolicy.debugLogConfiguration();
    }

    private void logConfigurationLogFile(String property, File file) {
        if (file == null) {
            LOG.info(" - {}: <null>", property);
        } else {
            LOG.info(" - {}: file://{}", property, file.getAbsolutePath());
        }
    }

    private void logConfigurationLogToString(String property, Object data) {
        if (data == null) {
            LOG.info(" - {}: <null>", property);
        } else {
            final String asString = String.valueOf(data);

            final File fileAttempt = new File(asString);
            if (fileAttempt.exists()) {
                this.logConfigurationLogFile(property, fileAttempt);
                return;
            }

            LOG.info(" - {}: {}", property, asString);
        }
    }

    private static String repeat(String str, int count) {
        if (count <= 0) return "";
        return IntStream.range(0, count).mapToObj(i -> str).collect(Collectors.joining());
    }

    public static String xmlEscapeContentString(String string) {
        if (string == null) return "";

        String s = StringEscapeUtils.escapeXml(string.trim());
        s = s.replace("-PSTART-", "<p>");
        s = s.replace("-PEND-", "</p>");
        s = s.replace("-NEWLINE-", "<p/>");

        return s;
    }

    public String xmlEscapeSvgId(String text) {
        if (StringUtils.isBlank(text)) {
            // NOTE: fail early, fail fast; not resolvable SVG links will be reported when generating the document;
            //  this prevents that these links are generated in the first place
            throw new IllegalStateException("Label for SVG must be set.");
        }
        return text.trim().toLowerCase().replace(" ", "-");
    }

    private PreFormattedEscapeUtils preFormattedEscapeUtils = new PreFormattedEscapeUtils();

    public String xmlEscapePreformattedContentString(String string) {
        if (string == null) return "";

        String s = preFormattedEscapeUtils.xml(string.trim());

        s = s.replace("-PSTART-", "<p>");
        s = s.replace("-PEND-", "</p>");
        s = s.replace("-NEWLINE-", "<p/>");

        return s;
    }

    /**
     * Escapes a string to be used as a valid and safe HTML (id) attribute.<br>
     * It will replace all characters that are not alphanumeric, underscore or dash with an underscore.<br>
     * If the string is blank, a default ID will be generated from a UUID.<br>
     * If the string starts with a digit, an underscore will be prepended.
     * <p>
     * Example usage and expected outputs:
     * <pre>
     * xmlEscapeStringAttribute(" ")          = starts with "defaultId"
     * xmlEscapeStringAttribute(null)         = starts with "defaultId"
     * xmlEscapeStringAttribute("normalId")   = "normalId"
     * xmlEscapeStringAttribute("invalid!Id") = "invalid_Id"
     * xmlEscapeStringAttribute("invalid Id") = "invalid_Id"
     * xmlEscapeStringAttribute("1invalidId") = "_1invalidId"
     * xmlEscapeStringAttribute("invalidChars!@#$%^&amp;*()\"'") = "invalidChars____________"
     * </pre>
     *
     * @param input the string to be escaped
     * @return a safe HTML attribute value
     */
    public static String xmlEscapeStringAttribute(String input) {
        if (StringUtils.isBlank(input)) {
            return "defaultId" + UUID.randomUUID();
        }

        final String safeId = input.replaceAll("[^A-Za-z0-9\\-_]", "_");

        if (!safeId.isEmpty() && Character.isDigit(safeId.charAt(0))) {
            return "_" + safeId;
        } else {
            return safeId;
        }
    }

    public static String xmlEscapeString(String string) {
        return xmlEscapeNameOptionallyInsertNbsp(string, true);
    }

    public static String xmlEscapeDate(String string) {
        return xmlEscapeNameOptionallyInsertNbsp(string, false);
    }

    public static String xmlEscapeLicense(String license) {
        return xmlEscapeNameOptionallyInsertNbsp(license, false);
    }

    public static String xmlEscapeArtifactId(String artifactFileId) {
        return xmlEscapeNameOptionallyInsertNbsp(artifactFileId, true);
    }

    public static String xmlEscapeId(String id) {
        return StringEscapeUtils.escapeXml(id.trim());
    }

    public static String xmlEscapeComponentName(String componentName) {
        return xmlEscapeNameOptionallyInsertNbsp(componentName, true);
    }

    public static String xmlEscapeGAV(String gavElement) {
        return xmlEscapeNameOptionallyInsertNbsp(gavElement, true);
    }

    private static String xmlEscapeNameOptionallyInsertNbsp(String artifactFileId, boolean insertBreakingSpaces) {
        if (artifactFileId == null) return "&nbsp;";

        // escape the remainder
        String escaped = StringEscapeUtils.escapeXml(artifactFileId.trim());

        if (insertBreakingSpaces) {
            // support line wrapping
            escaped = escaped.replaceAll("\\.", ".&#8203;");
            escaped = escaped.replaceAll("-", "-&#8203;");
            escaped = escaped.replaceAll("_", "_&#8203;");
            escaped = escaped.replaceAll(":", ":&#8203;");
            escaped = escaped.replaceAll("([^&]+)#", "$1#&#8203;");
        }

        return escaped;
    }

    public File getGlobalInventoryDir() {
        return referenceInventoryDir;
    }

    public AssetData getAssetData(Inventory inventory) {
        return AssetData.fromInventory(inventory);
    }

    public CharSequence joinStrings(Collection<CharSequence> strings, CharSequence delimiter) {
        if (strings == null) {
            return "";
        }
        if (delimiter == null) {
            return strings.stream().collect(Collectors.joining());
        }
        return strings.stream().collect(Collectors.joining(delimiter));
    }
}