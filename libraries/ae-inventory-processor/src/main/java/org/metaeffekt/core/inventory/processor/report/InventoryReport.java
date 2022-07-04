/*
 * Copyright 2009-2021 the original author or authors.
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

import org.apache.commons.lang.StringEscapeUtils;
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
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class InventoryReport {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReport.class);

    private static final String SEPARATOR_SLASH = "/";
    private static final String PATTERN_ANY_VT = "**/*.vt";

    private static final String TEMPLATES_BASE_DIR = "/META-INF/templates";

    private static final String TEMPLATES_TECHNICAL_BASE_DIR = TEMPLATES_BASE_DIR + SEPARATOR_SLASH + "technical";

    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_BOM = "inventory-report-bom";
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY = "inventory-report-vulnerability";
    public static final String TEMPLATE_GROUP_INVENTORY_POM = "inventory-pom";
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_DIFF = "inventory-report-diff";

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

    // The diff inventory is used for version diffs.
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

    /**
     * Fields to control the report for inventory governance.
     */
    private boolean failOnError = true;
    private boolean failOnBanned = true;
    private boolean failOnDowngrade = true;
    private boolean failOnUnknown = true;
    private boolean failOnUnknownVersion = true;
    private boolean failOnDevelopment = true;
    private boolean failOnInternal = true;
    private boolean failOnUpgrade = true;
    private boolean failOnMissingLicense = true;
    private boolean failOnMissingLicenseFile = true;
    private boolean failOnMissingNotice = true;

    /**
     * Default to false for compatibility reasons
     */
    private boolean failOnMissingComponentFiles = false;

    private float vulnerabilityScoreThreshold = 7.0f;
    private final List<String> vulnerabilityAdvisoryFilter = new ArrayList<>();

    /**
     * For which advisory providers to generate additional tables in the overview section containing statistic data on
     * which vulnerabilities have already been reviewed.
     */
    private final List<String> generateOverviewTablesForAdvisories = new ArrayList<>();

    private ArtifactFilter artifactFilter;

    private boolean inventoryBomReportEnabled = false;
    private boolean inventoryDiffReportEnabled = false;
    private boolean inventoryPomEnabled = false;
    private boolean inventoryVulnerabilityReportEnabled = false;

    /**
     * This is the relative path as it will be used in the resulting dita templates. This needs
     * to be configured to match the relative path from the documentation to the licenses.
     */
    private String relativeLicensePath = "licenses";

    private List<Artifact> addOnArtifacts;

    // FIXME: do we want to support this? This is for aggregating information in a reactor project.
    private transient Inventory lastProjectInventory;

    /**
     * The inventory for which to create the report.
     */
    private Inventory inventory;

    /**
     * Default {@link ReportContext}.
     */
    private ReportContext reportContext = new ReportContext("default", null, null);

    private String templateLanguageSelector = "en";

    public boolean createReport() throws Exception {
        logHeaderBox("Creating Inventory Report for project [" + getProjectName() + "]");

        Inventory globalInventory = readGlobalInventory();

        // read local repository
        Inventory localRepositoryInventory;
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

        // WORKAROUND
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
        return InventoryUtils.readInventory(referenceInventoryDir, referenceInventoryIncludes);
    }

    protected boolean createReport(Inventory globalInventory, Inventory localInventory) throws Exception {
        Inventory diffInventory = null;
        File targetInventoryFile = targetInventoryDir != null ? new File(targetInventoryDir, targetInventoryPath) : null;

        // read the reference file if specified
        if (diffInventoryFile != null) {
            diffInventory = new InventoryReader().readInventory(diffInventoryFile);
        }

        Inventory projectInventory = new Inventory();
        this.lastProjectInventory = projectInventory;

        // transfer component patterns from scan inventory (these may include wildcard checksum replacements)
        projectInventory.inheritComponentPatterns(localInventory, false);

        // transfer identified assets from scan
        projectInventory.inheritAssetMetaData(localInventory, false);

        localInventory.sortArtifacts();

        List<Artifact> list = localInventory.getArtifacts();

        boolean unknown = false;
        boolean unknownVersion = false;
        boolean development = false;
        boolean internal = false;
        boolean error = false;
        boolean banned = false;
        boolean upgrade = false;
        boolean downgrade = false;

        boolean missingLicense = false;

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
                if (!StringUtils.hasText(matchedReferenceArtifact.getGroupId())) {
                    matchedReferenceArtifact.setGroupId(localArtifact.getGroupId());
                }

                String classification = matchedReferenceArtifact.getClassification();
                localWarn = true;
                if (classification != null) {
                    if (classification.contains("upgrade")) {
                        classifier += "[upgrade]";
                        localUpgrade = true;
                        if (failOnUpgrade) {
                            localError = true;
                        }
                    }
                    if (classification.contains("downgrade")) {
                        classifier += "[downgrade]";
                        localDowngrade = true;
                        if (failOnDowngrade) {
                            localError = true;
                        }
                    }
                    if (classification.contains("development")) {
                        classifier += "[development]";
                        localDevelopment = true;
                        localWarn = false;
                        if (failOnDevelopment) {
                            localError = true;
                        }
                    }
                    if (classification.contains("internal")) {
                        classifier += "[internal]";
                        localInternal = true;
                        if (failOnInternal) {
                            localError = true;
                        }
                    }
                    if (classification.contains("banned")) {
                        classifier += "[banned]";
                        localBanned = true;
                        if (failOnBanned) {
                            localError = true;
                        }
                    }
                    if (classification.contains("unknown")) {
                        classifier += "[unknown]";
                        localUnknown = true;
                        if (failOnUnknown) {
                            localError = true;
                        }
                    }
                    if (classification.contains("hint")) {
                        classifier += "[hint]";
                        localWarn = false;
                    }
                }
                if (!StringUtils.hasText(matchedReferenceArtifact.getLicense())) {
                    classifier += "[no license]";
                    localMissingLicense = true;
                    if (failOnMissingLicense) {
                        localError = true;
                    }
                }
            } else {
                classifier += "[unknown]";
                localUnknown = true;
                if (failOnUnknown) {
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
                    copy.setProjects(localArtifact.getProjects());
                    projectInventory.getArtifacts().add(copy);

                    // handle checksum
                    if (StringUtils.isEmpty(copy.getChecksum())) {
                        copy.setChecksum(localArtifact.getChecksum());
                    } else {
                        if (StringUtils.hasText(localArtifact.getChecksum()) && !copy.getChecksum().equalsIgnoreCase(localArtifact.getChecksum())) {
                            throw new IllegalStateException(String.format("Checksum mismatch for %s.", localArtifact.getId()));
                        }
                    }

                    // copy details not covered by reference artifact to resulting artifact
                    for (String attributeKey : localArtifact.getAttributes()) {
                        final String currentValue = copy.get(attributeKey);
                        if (!StringUtils.hasText(currentValue)) {
                            copy.set(attributeKey, localArtifact.get(attributeKey));
                        }
                    }

                    reportArtifact = matchedReferenceArtifact;
                } else {
                    projectInventory.getArtifacts().add(localArtifact);
                    localArtifact.setManaged(true);
                }

                // log information
                if (classifier.length() > 0) {
                    String artifactQualifier = reportArtifact.createStringRepresentation();
                    if (StringUtils.hasText(comment)) {
                        comment = "- " + comment;
                    }
                    if (localArtifact.isRelevant() || localArtifact.isManaged()) {
                        final String messagePattern = "{} {} {}";
                        if (localError) {
                            LOG.error(messagePattern, classifier, artifactQualifier, comment);
                        } else if (localWarn) {
                            LOG.warn(messagePattern, classifier, artifactQualifier, comment);
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

        // transfer license meta data into project inventory (required for the license related reports)
        projectInventory.inheritLicenseMetaData(globalInventory, false);
        projectInventory.filterLicenseMetaData();

        projectInventory.inheritLicenseData(globalInventory, false);

        // transfer available vulnerability information
        projectInventory.inheritVulnerabilityMetaData(globalInventory, false);

        // transfer available cert information
        projectInventory.inheritCertMetaData(globalInventory, false);

        // transfer available asset information
        projectInventory.inheritAssetMetaData(globalInventory, false);

        // filter the vulnerability metadata to only cover the items remaining in the inventory
        projectInventory.filterVulnerabilityMetaData();

        // write reports
        if (inventoryBomReportEnabled) {
            writeReports(projectInventory, deriveTemplateBaseDir(), TEMPLATE_GROUP_INVENTORY_REPORT_BOM, reportContext);
        }

        if (inventoryDiffReportEnabled) {
            writeDiffReport(diffInventory, projectInventory, reportContext);
        }

        if (inventoryVulnerabilityReportEnabled) {
            writeReports(projectInventory, deriveTemplateBaseDir(), TEMPLATE_GROUP_INVENTORY_REPORT_VULNERABILITY, reportContext);
        }

        if (inventoryPomEnabled) {
            writeReports(projectInventory, TEMPLATES_TECHNICAL_BASE_DIR, TEMPLATE_GROUP_INVENTORY_POM, reportContext);
        }

        // evaluate licenses only for managed artifacts
        List<String> licenses = projectInventory.evaluateLicenses(false, true);

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
                LOG.info("Report inventory written to {}.", getTargetInventoryPath());
            } catch (Exception e) {
                LOG.error("Unable to write inventory to {}. Skipping write.", getTargetInventoryPath(), e);
            }
        }

        if (!error && !unknown && !unknownVersion && !development && !internal && !upgrade && !downgrade &&
                !missingLicenseFile && !missingNotice) {
            LOG.info("No findings!");
        }

        if (error && failOnError) {
            return false;
        }
        if (banned && failOnBanned) {
            return false;
        }
        if (unknown && failOnUnknown) {
            return false;
        }
        if (unknownVersion && failOnUnknownVersion) {
            return false;
        }
        if (downgrade && failOnDowngrade) {
            return false;
        }
        if (development && failOnDevelopment) {
            return false;
        }
        if (internal && failOnInternal) {
            return false;
        }
        if (upgrade && failOnUpgrade) {
            return false;
        }
        if (missingLicense && failOnMissingLicense) {
            return false;
        }
        if (missingLicenseFile && failOnMissingLicenseFile) {
            return false;
        }
        if (missingNotice && failOnMissingNotice) {
            return false;
        }
        return true;
    }

    private String deriveTemplateBaseDir() {
        return TEMPLATES_BASE_DIR + SEPARATOR_SLASH + getTemplateLanguageSelector();
    }

    protected void writeReports(Inventory projectInventory, String templateBaseDir, String templateGroup, ReportContext reportContext) throws Exception {
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
            produceDita(projectInventory, filePath, targetReportPath, reportContext);
        }
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
            if (StringUtils.hasText(license) && artifact.isRelevant()) {
                if (licensesRequiringNotice.contains(license)) {
                    LicenseMetaData licenseMetaData = projectInventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData == null) {
                        final String messagePattern = "No notice for artifact '{}' with license '{}'.";
                        if (failOnMissingNotice) {
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
                if (failOnMissingLicenseFile) {
                    LOG.error("No license file in folder '{}'", licenseFolderName);
                } else {
                    LOG.warn("No license file in folder '{}'", licenseFolderName);
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
                if (failOnMissingComponentFiles) {
                    LOG.error("No component-specific license file in folder '{}'", componentFolderName);
                } else {
                    LOG.warn("No component-specific license file in folder '{}'", componentFolderName);
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
            if (!StringUtils.hasText(sourceLicense)) {
                continue;
            }
            if (!StringUtils.hasText(componentName)) {
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
            final String sourcePath = (isMetaDataVersionWildcard || isArtifactVersionWildcard) ?
                    versionUnspecificComponentFolder : versionSpecificComponentFolder;

            // determine target path (always artifact version centric; no information may be available)
            final String targetPath = isArtifactVersionWildcard ? versionUnspecificComponentFolder : versionSpecificComponentFolder;

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

    protected void writeDiffReport(Inventory referenceInventory, Inventory projectInventory, ReportContext reportContext) throws Exception {
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
        Inventory filteredInventory = new Inventory();
        for (Artifact artifact : projectInventory.getArtifacts()) {
            if (artifact.getVersion() != null &&
                    !artifact.getVersion().trim().equals(artifact.get(KEY_PREVIOUS_VERSION))) {
                filteredInventory.getArtifacts().add(artifact);
            }
        }

        writeReports(filteredInventory, deriveTemplateBaseDir(), TEMPLATE_GROUP_INVENTORY_REPORT_DIFF, reportContext);
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

    private void filterVulnerabilityMetadataByAdvisoryFilter(List<VulnerabilityMetaData> vulnerabilityMetaData) {
        if (vulnerabilityAdvisoryFilter.size() > 0) {
            vulnerabilityMetaData.removeIf(vmd ->
                    VulnerabilityReportAdapter.getAdvisories(vmd).stream()
                            .noneMatch(advisory -> vulnerabilityAdvisoryFilter.contains(advisory.getSource())));
        }
    }

    private void produceDita(Inventory projectInventory, String templateResourcePath, File target, ReportContext reportContext) throws Exception {
        LOG.info("Producing Dita for template [{}]", templateResourcePath);

        String ENCODING_UTF_8 = "UTF-8";
        Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADER, "class, file");
        properties.put("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        properties.put(Velocity.INPUT_ENCODING, ENCODING_UTF_8);
        properties.put(Velocity.OUTPUT_ENCODING, ENCODING_UTF_8);
        properties.put(Velocity.SET_NULL_ALLOWED, true);

        VelocityEngine velocityEngine = new VelocityEngine(properties);
        Template template = velocityEngine.getTemplate(templateResourcePath);
        StringWriter sw = new StringWriter();
        VelocityContext context = new VelocityContext();

        Inventory filteredInventory = projectInventory.getFilteredInventory();
        // if an advisory filter is set, filter out all vulnerabilities that do not contain a filter advisory source
        filterVulnerabilityMetadataByAdvisoryFilter(filteredInventory.getVulnerabilityMetaData());

        // regarding the report we only use the filtered inventory for the time being
        context.put("inventory", filteredInventory);
        context.put("vulnerabilityAdapter", new VulnerabilityReportAdapter(filteredInventory));
        context.put("report", this);
        context.put("StringEscapeUtils", org.apache.commons.lang.StringEscapeUtils.class);
        context.put("Float", Float.class);
        context.put("targetReportDir", this.targetReportDir);

        context.put("reportContext", reportContext);

        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    private static String repeat(String str, int count) {
        if (count <= 0) return "";
        return IntStream.range(0, count).mapToObj(i -> str).collect(Collectors.joining());
    }

    private static void logHeaderBox(String str) {
        LOG.info(repeat("*", str.length() + 4));
        LOG.info("* {} *", str);
        LOG.info(repeat("*", str.length() + 4));
    }

    public File getDiffInventoryFile() {
        return diffInventoryFile;
    }

    public void setDiffInventoryFile(File diffInventoryFile) {
        this.diffInventoryFile = diffInventoryFile;
    }

    public String getTargetInventoryPath() {
        return targetInventoryPath;
    }

    public void setTargetInventoryPath(String targetInventoryPath) {
        this.targetInventoryPath = targetInventoryPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isFailOnBanned() {
        return failOnBanned;
    }

    public void setFailOnBanned(boolean failOnBanned) {
        this.failOnBanned = failOnBanned;
    }

    public boolean isFailOnDowngrade() {
        return failOnDowngrade;
    }

    public void setFailOnDowngrade(boolean failOnDowngrade) {
        this.failOnDowngrade = failOnDowngrade;
    }

    public boolean isFailOnUnknown() {
        return failOnUnknown;
    }

    public void setFailOnUnknown(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    public boolean isFailOnUnknownVersion() {
        return failOnUnknownVersion;
    }

    public void setFailOnUnknownVersion(boolean failOnUnknownVersion) {
        this.failOnUnknownVersion = failOnUnknownVersion;
    }

    public boolean isFailOnDevelopment() {
        return failOnDevelopment;
    }

    public void setFailOnDevelopment(boolean failOnDevelopment) {
        this.failOnDevelopment = failOnDevelopment;
    }

    public boolean isFailOnInternal() {
        return failOnInternal;
    }

    public void setFailOnInternal(boolean failOnInternal) {
        this.failOnInternal = failOnInternal;
    }

    public boolean isFailOnUpgrade() {
        return failOnUpgrade;
    }

    public void setFailOnUpgrade(boolean failOnUpgrade) {
        this.failOnUpgrade = failOnUpgrade;
    }

    public boolean isFailOnMissingLicense() {
        return failOnMissingLicense;
    }

    public void setFailOnMissingLicense(boolean failOnMissingLicense) {
        this.failOnMissingLicense = failOnMissingLicense;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ArtifactFilter getArtifactFilter() {
        return artifactFilter;
    }

    public void setArtifactFilter(ArtifactFilter artifactFilter) {
        this.artifactFilter = artifactFilter;
    }

    public File getTargetLicenseDir() {
        return targetLicenseDir;
    }

    public File getTargetComponentDir() {
        return targetComponentDir;
    }

    public void setTargetComponentDir(File targetComponentPath) {
        this.targetComponentDir = targetComponentPath;
    }

    public void setTargetLicenseDir(File targetLicensePath) {
        this.targetLicenseDir = targetLicensePath;
    }

    public String getReferenceLicensePath() {
        return referenceLicensePath;
    }

    public void setReferenceLicensePath(String referenceLicensePath) {
        this.referenceLicensePath = referenceLicensePath;
    }

    public String getReferenceComponentPath() {
        return referenceComponentPath;
    }

    public void setReferenceComponentPath(String referenceComponentPath) {
        this.referenceComponentPath = referenceComponentPath;
    }

    public String getRelativeLicensePath() {
        return relativeLicensePath;
    }


    public void setRelativeLicensePath(String relativeLicensePath) {
        this.relativeLicensePath = relativeLicensePath;
    }

    public List<Artifact> getAddOnArtifacts() {
        return addOnArtifacts;
    }

    public void setAddOnArtifacts(List<Artifact> addOnArtifacts) {
        this.addOnArtifacts = addOnArtifacts;
    }

    public Inventory getLastProjectInventory() {
        return lastProjectInventory;
    }

    public boolean getFailOnMissingLicenseFile() {
        return failOnMissingLicenseFile;
    }

    public void setFailOnMissingLicenseFile(boolean failOnMissingLicenseFile) {
        this.failOnMissingLicenseFile = failOnMissingLicenseFile;
    }

    public boolean getFailOnMissingNotice() {
        return failOnMissingNotice;
    }

    public void setFailOnMissingNotice(boolean failOnMissingNotice) {
        this.failOnMissingNotice = failOnMissingNotice;
    }

    public String xmlEscapeString(String string) {
        return xmlEscapeName(string, true);
    }

    public String xmlEscapeContentString(String string) {
        if (string == null) return "";

        String s = StringEscapeUtils.escapeXml(string.trim());
        s = s.replace("-PSTART-", "<p>");
        s = s.replace("-PEND-", "</p>");
        s = s.replace("-NEWLINE-", "<p/>");

        return s;
    }

    public String xmlEscapeSvgId(String text) {
        if (text == null) return "";
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


    public String xmlEscapeLicense(String license) {
        return xmlEscapeName(license, false);
    }

    public String xmlEscapeArtifactId(String artifactFileId) {
        return xmlEscapeName(artifactFileId, true);
    }

    public String xmlEscapeComponentName(String componentName) {
        return xmlEscapeName(componentName, true);
    }

    public String xmlEscapeGAV(String gavElement) {
        return xmlEscapeName(gavElement, true);
    }

    private String xmlEscapeName(String artifactFileId, boolean insertBreakingSpaces) {
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

    public void setReferenceInventoryDir(File globalInventoryDir) {
        this.referenceInventoryDir = globalInventoryDir;
    }

    public String getReferenceInventoryIncludes() {
        return referenceInventoryIncludes;
    }

    public void setReferenceInventoryIncludes(String referenceInventoryIncludes) {
        this.referenceInventoryIncludes = referenceInventoryIncludes;
    }

    public File getTargetInventoryDir() {
        return targetInventoryDir;
    }

    public void setTargetInventoryDir(File targetInventoryDir) {
        this.targetInventoryDir = targetInventoryDir;
    }

    public ReportContext getReportContext() {
        return reportContext;
    }

    public void setReportContext(ReportContext reportContext) {
        this.reportContext = reportContext;
    }

    public float getVulnerabilityScoreThreshold() {
        return vulnerabilityScoreThreshold;
    }

    public void setVulnerabilityScoreThreshold(float vulnerabilityScoreThreshold) {
        this.vulnerabilityScoreThreshold = vulnerabilityScoreThreshold;
    }

    private final static String[] VALID_VULNERABILITY_ADVISORY_PROVIDERS = {"CERT-FR", "CERT-SEI", "MSRC"};

    public List<String> getVulnerabilityAdvisoryFilter() {
        return Collections.unmodifiableList(vulnerabilityAdvisoryFilter);
    }

    public void addVulnerabilityAdvisoryFilter(String... advisoryProvider) {
        splitAndAppendCsvAdvisoryProviders(vulnerabilityAdvisoryFilter, advisoryProvider);
    }

    public List<String> getGenerateOverviewTablesForAdvisories() {
        return generateOverviewTablesForAdvisories;
    }

    public void addGenerateOverviewTablesForAdvisories(String... advisoryProvider) {
        splitAndAppendCsvAdvisoryProviders(generateOverviewTablesForAdvisories, advisoryProvider);
    }

    private void splitAndAppendCsvAdvisoryProviders(List<String> listToAddProvidersTo, String... commaSeperatedProviders) {
        if (commaSeperatedProviders != null && commaSeperatedProviders.length > 0) {
            for (String commaSeperatedProvider : commaSeperatedProviders) {
                if (commaSeperatedProvider != null && commaSeperatedProvider.length() > 0) {
                    if (commaSeperatedProvider.contains(",")) {
                        splitAndAppendCsvAdvisoryProviders(listToAddProvidersTo, commaSeperatedProvider.split(", ?"));
                    } else {
                        if (Arrays.stream(VALID_VULNERABILITY_ADVISORY_PROVIDERS).anyMatch(e -> e.equals(commaSeperatedProvider.toUpperCase()))) {
                            listToAddProvidersTo.add(commaSeperatedProvider.toUpperCase());
                        } else if (commaSeperatedProvider.equals("ALL")) {
                            listToAddProvidersTo.addAll(Arrays.asList(VALID_VULNERABILITY_ADVISORY_PROVIDERS));
                        } else {
                            LOG.warn("Unknown vulnerability advisory provider [{}], must be one of {}", commaSeperatedProvider, Arrays.toString(VALID_VULNERABILITY_ADVISORY_PROVIDERS));
                        }
                    }
                }
            }
        }
    }

    public void setTargetReportDir(File reportTarget) {
        this.targetReportDir = reportTarget;
    }

    public File getReferenceInventoryDir() {
        return referenceInventoryDir;
    }

    public File getTargetReportDir() {
        return targetReportDir;
    }

    public boolean isFailOnMissingLicenseFile() {
        return failOnMissingLicenseFile;
    }

    public boolean isFailOnMissingNotice() {
        return failOnMissingNotice;
    }

    public boolean isInventoryBomReportEnabled() {
        return inventoryBomReportEnabled;
    }

    public void setInventoryBomReportEnabled(boolean inventoryBomReportEnabled) {
        this.inventoryBomReportEnabled = inventoryBomReportEnabled;
    }

    public boolean isInventoryDiffReportEnabled() {
        return inventoryDiffReportEnabled;
    }

    public void setInventoryDiffReportEnabled(boolean inventoryDiffReportEnabled) {
        this.inventoryDiffReportEnabled = inventoryDiffReportEnabled;
    }

    public boolean isInventoryPomEnabled() {
        return inventoryPomEnabled;
    }

    public void setInventoryPomEnabled(boolean inventoryPomEnabled) {
        this.inventoryPomEnabled = inventoryPomEnabled;
    }

    public boolean isInventoryVulnerabilityReportEnabled() {
        return inventoryVulnerabilityReportEnabled;
    }

    public void setInventoryVulnerabilityReportEnabled(boolean inventoryVulnerabilityReportEnabled) {
        this.inventoryVulnerabilityReportEnabled = inventoryVulnerabilityReportEnabled;
    }

    public void setLastProjectInventory(Inventory lastProjectInventory) {
        this.lastProjectInventory = lastProjectInventory;
    }

    public void setTemplateLanguageSelector(String templateLanguageSelector) {
        this.templateLanguageSelector = templateLanguageSelector;
    }

    public String getTemplateLanguageSelector() {
        return templateLanguageSelector;
    }

    public void setFailOnMissingComponentFiles(boolean failOnMissingComponentFiles) {
        this.failOnMissingComponentFiles = failOnMissingComponentFiles;
    }

    public boolean isFailOnMissingComponentFiles() {
        return failOnMissingComponentFiles;
    }

}
