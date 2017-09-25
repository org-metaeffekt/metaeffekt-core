/**
 * Copyright 2009-2017 the original author or authors.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.reader.LocalRepositoryInventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;

public class InventoryReport {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReport.class);

    // artifacts summaries (maven centric)
    private static final String DITA_MAVEN_ARTIFACT_REPORT_TEMPLATE =
            "/META-INF/templates/inventory-dita-report.vt";

    // artifacts summaries (component centric)
    private static final String DITA_COMPONENT_ARTIFACT_REPORT_TEMPLATE =
            "/META-INF/templates/inventory-dita-component-report.vt";

    // version diff
    private static final String DITA_DIFF_TEMPLATE = "/META-INF/templates/inventory-dita-diff.vt";

    // license summary
    private static final String DITA_LICENSE_TEMPLATE =
            "/META-INF/templates/inventory-dita-licenses.vt";

    // notice summary
    private static final String DITA_NOTICE_TEMPLATE = "/META-INF/templates/inventory-dita-notices.vt";

    // generating a pom from the inventory
    private static final String MAVEN_POM_TEMPLATE =  "/META-INF/templates/inventory-pom-xml.vt";

    private static final Map<String, File> STATIC_FILE_MAP = Collections.synchronizedMap(new HashMap<>());
    public static final String ASTERISK = "*";

    private String globalInventoryPath;

    // The reference inventory is used for version diffs.
    private String referenceInventoryPath;

    private String repositoryPath;
    private Inventory repositoryInventory;

    private String targetInventoryPath;
    private String targetDitaReportPath;
    private String targetDitaComponentReportPath;
    private String targetDitaDiffPath;
    private String targetDitaLicenseReportPath;
    private String targetDitaNoticeReportPath;
    private String targetMavenPomPath;

    private String projectName = "local project";
    private boolean failOnError = true;
    private boolean failOnBanned = true;
    private boolean failOnDowngrade = true;
    private boolean failOnUnknown = true;
    private boolean failOnUnknownVersion = true;
    private boolean failOnDevelopment = true;
    private boolean failOnInternal = true;
    private boolean failOnUpgrade = true;
    private boolean failOnMissingLicense = true;
    private ArtifactFilter artifactFilter;
    private String licenseSourcePath;
    private String licenseTargetPath;
    /**
     * This is the relative path as it will be used in the resulting dita templates. This needs
     * to be configured to match the relative path from the documentation to the licenses.
     */
    private String relativeLicensePath = "licenses";
    private List<Artifact> addOnArtifacts;
    private boolean failOnMissingLicenseFile = true;
    private boolean failOnMissingNotice = true;
    private transient Inventory lastProjectInventory;

    private String[] repositoryIncludes = new String[]{"**/*"};

    private String[] repositoryExcludes = new String[]{
            "**/*.pom", "**/*.sha1", "**/*.xml",
            "**/*.repositories", "**/*.jar-not-available"
    };

    public static Resource inferPathFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return new FileSystemResource(path);
        } else {
            // We have an issue with concurrent updates of the file during a reactor build.
            // For a VM we therefore copy the content once to a temporary file and then
            // (for all usages of the same path) use the copy instead of the classpath resource.
            File tmpfile;
            synchronized (STATIC_FILE_MAP) {
                tmpfile = STATIC_FILE_MAP.get(path);
                if (tmpfile == null || !tmpfile.exists()) {
                    tmpfile = File.createTempFile("inventory", null);
                    ClassPathResource classPathResource = new ClassPathResource(path);
                    InputStream in = null;
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(tmpfile);
                        in = classPathResource.getInputStream();
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                    STATIC_FILE_MAP.put(path, tmpfile);
                    tmpfile.deleteOnExit();
                }
            }
            return new FileSystemResource(tmpfile);
        }
    }

    public boolean createReport() throws Exception {

        LOG.info("****************************************************************");
        LOG.info("* Creating Inventory Report for project {} *", getProjectName());
        LOG.info("****************************************************************");

        File repositoryFile = repositoryPath != null ? new File(repositoryPath) : null;

        Inventory globalInventory = globalInventoryPath == null ? new Inventory() : readGlobalInventory();

        // read local repository
        Inventory localRepositoryInventory;
        if (this.repositoryInventory != null) {
            localRepositoryInventory = this.repositoryInventory;
        } else {
            LocalRepositoryInventoryReader localRepositoryInventoryReader =
                    new LocalRepositoryInventoryReader();
            localRepositoryInventoryReader.setIncludes(repositoryIncludes);
            localRepositoryInventoryReader.setExcludes(repositoryExcludes);
            localRepositoryInventory =
                    localRepositoryInventoryReader.readInventory(repositoryFile, projectName);
        }

        // insert (potentially incomplete) artifacts for reporting
        // this supports adding licenses and obligations into the
        // report, which are not covered by the repository.
        if (addOnArtifacts != null) {
            localRepositoryInventory.getArtifacts().addAll(addOnArtifacts);
        }

        return createReport(globalInventory, localRepositoryInventory);
    }

    protected boolean createReport(Inventory globalInventory, Inventory localInventory) throws Exception {
        Inventory referenceInventory = null;
        File referenceInventoryFile = referenceInventoryPath != null ? new File(referenceInventoryPath) : null;
        File targetInventoryFile = targetInventoryPath != null ? new File(targetInventoryPath) : null;

        // read the reference file if specified
        if (referenceInventoryFile != null) {
            referenceInventory = new InventoryReader().readInventory(referenceInventoryFile);
        }

        Inventory projectInventory = new Inventory();
        this.lastProjectInventory = projectInventory;

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

        for (Artifact artifact : list) {
            artifact.deriveArtifactId();

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

            Artifact foundArtifact = globalInventory.findArtifact(artifact);

            if (foundArtifact == null) {
                foundArtifact = globalInventory.findArtifact(artifact, true);
            }

            LOG.debug("Query for artifact:" + artifact);
            LOG.debug("Result artifact:   " + foundArtifact);

            String classifier = "";
            String comment = "";

            if (foundArtifact != null) {
                comment = foundArtifact.getComment();

                // in case the group id does not contain anything we
                // infer the group id from the artifact from the repository. Better than nothing.
                if (!StringUtils.hasText(foundArtifact.getGroupId())) {
                    foundArtifact.setGroupId(artifact.getGroupId());
                }

                String classification = foundArtifact.getClassification();
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
                if (!StringUtils.hasText(foundArtifact.getLicense())) {
                    classifier += "[no license]";
                    localMissingLicense = true;
                    if (failOnMissingLicense) {
                        localError = true;
                    }
                }
            } else {
                // branch: no exact match found
                if (artifactFilter == null || !artifactFilter.filter(artifact)) {

                    // try to find a similar artifact:
                    Artifact similar = globalInventory.findCurrent(artifact);
                    if (similar == null) {
                        // no current found, in this case we accept any artifact with matching attributes
                        // excluding version
                        similar = globalInventory.findArtifactClassificationAgnostic(artifact);
                    } else {
                        comment = "[recommended version: " + similar.getVersion() + "]";
                    }
                    if (similar != null) {
                        foundArtifact = new DefaultArtifact(artifact);

                        // the found artifact has at least id, version, artifactId (may be inferred),
                        // and groupId set. We merge the remaining content of the similar artifact

                        StringBuffer commentAggregation = new StringBuffer();
                        if (StringUtils.hasText(similar.getComment())) {
                            commentAggregation.append(similar.getComment().trim());
                        }
                        if (StringUtils.hasText(comment)) {
                            if (commentAggregation.length() > 0) {
                                commentAggregation.append(" ");
                            }
                            commentAggregation.append(comment.trim());
                        }

                        foundArtifact.setComment(commentAggregation.toString());
                        foundArtifact.setComponent(similar.getComponent());
                        foundArtifact.setLatestAvailableVersion(similar.getLatestAvailableVersion());
                        foundArtifact.setLicense(similar.getLicense());
                        foundArtifact.setSecurityCategory(similar.getSecurityCategory());
                        foundArtifact.setUrl(similar.getUrl());
                        foundArtifact.setSecurityRelevant(similar.isSecurityRelevant());
                        foundArtifact.setReported(true);
                        foundArtifact.setVersionReported(false);
                        foundArtifact.setGroupId(similar.getGroupId());

                        if (!StringUtils.hasText(artifact.getVersion())) {
                            String version = artifact.getId();
                            version = version.replace(similar.getArtifactId() + "-", "");
                            if (StringUtils.hasText(similar.getClassifier())) {
                                version = version.replace("-" + similar.getClassifier() + ".", ".");
                            }
                            version = version.replace("." + similar.getType(), "");
                            foundArtifact.setVersion(version);
                        }

                        // mark artifacts using classification
                        foundArtifact.setClassification("unknown version");

                        classifier = "[unknown version]";
                        localUnknownVersion = true;
                        if (failOnUnknownVersion) {
                            localError = true;
                        }
                    } else {
                        classifier += "[unknown]";
                        localUnknown = true;
                        if (failOnUnknown) {
                            localError = true;
                        }
                    }
                }
            }

            if (artifactFilter == null || !artifactFilter.filter(artifact)) {
                Artifact reportArtifact = artifact;
                if (foundArtifact != null) {
                    // ids may vary; we try to preserve the id here
                    DefaultArtifact copy = new DefaultArtifact(foundArtifact);
                    copy.setId(artifact.getId());

                    if (ASTERISK.equals(foundArtifact.getVersion())) {
                        copy.setVersionReported(false);

                        // in this case we also need to managed the version
                        int index = foundArtifact.getId().indexOf("*");
                        String version = artifact.getId().substring(index,
                            artifact.getId().length() - foundArtifact.getId().length() + index + 1);
                        copy.setVersion(version);
                    } else {
                        copy.setVersionReported(true);
                    }
                    copy.setReported(true);

                    // override flags (the original data does not include context)
                    copy.setRelevant(artifact.isRelevant());
                    copy.setManaged(artifact.isManaged());

                    projectInventory.getArtifacts().add(copy);

                    reportArtifact = foundArtifact;
                } else {
                    projectInventory.getArtifacts().add(artifact);
                }

                if (classifier.length() > 0) {
                    String artifactQualifier = reportArtifact.createStringRepresentation();
                    if (StringUtils.hasText(comment)) {
                        comment = "- " + comment;
                    }
                    if (artifact.isRelevant() || artifact.isManaged()) {
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
                            if (!artifact.isRelevant()) {
                                final String messagePattern = "{} {} {} (not relevant for report)";
                                LOG.info(messagePattern, classifier, artifactQualifier, comment);
                            } else {
                                final String messagePattern = "{} {} {} (not managed)";
                                LOG.info(messagePattern, classifier, artifactQualifier, comment);
                            }
                        }
                    }
                }

                // escalate the error only in case the artifact is relevant for reporting
                if (artifact.isManaged()) {
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
        projectInventory.setLicenseMetaData(globalInventory.getLicenseMetaData());

        // write reports
        writeArtifactReport(projectInventory);
        writeComponentReport(projectInventory);
        writeDiffReport(referenceInventoryFile, referenceInventory, projectInventory);
        writeObligationSummary(projectInventory);
        writeLicenseSummary(projectInventory);

        writeMavenPom(projectInventory);

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
                LOG.error("Unable to write inventory to {}. Skipping write.", getTargetInventoryPath());
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

    protected Inventory readGlobalInventory() throws IOException, FileNotFoundException {
        Resource globalInventoryResource = inferPathFile(globalInventoryPath);

        // read global inventory with manual managed meta data
        InputStream in = globalInventoryResource.getInputStream();
        Inventory globalInventory = null;
        try {
            globalInventory = new InventoryReader().readInventory(in);
        } finally {
            in.close();
        }
        return globalInventory;
    }

    protected void writeComponentReport(Inventory projectInventory) throws Exception {
        if (targetDitaComponentReportPath != null) {
            produceDita(projectInventory, DITA_COMPONENT_ARTIFACT_REPORT_TEMPLATE, new File(
                    targetDitaComponentReportPath));
        }
    }

    protected void writeArtifactReport(Inventory projectInventory) throws Exception {
        if (targetDitaReportPath != null) {
            produceDita(projectInventory, DITA_MAVEN_ARTIFACT_REPORT_TEMPLATE, new File(
                    targetDitaReportPath));
        }
    }

    public boolean evaluateNotices(Inventory projectInventory, List<String> licenses) {
        boolean missingNotice = false;
        final List<LicenseMetaData> licenseMetaDataList = projectInventory.getLicenseMetaData();
        final Set<String> licensesRequiringNotice = new HashSet();
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
            String license = artifact.getLicense();
            if (StringUtils.hasText(license) && artifact.isRelevant()) {
                if (licensesRequiringNotice.contains(license)) {
                    LicenseMetaData licenseMetaData =
                            projectInventory.findMatchingLicenseMetaData(
                                    artifact.getComponent(), license, artifact.getVersion());
                    if (licenseMetaData == null) {
                        final String messagePattern = "No notice for artifact '{}' with license '{}'.";
                        if (failOnMissingNotice) {
                            LOG.error(messagePattern, artifact.createStringRepresentation(), artifact.getLicense());
                            return true;
                        } else {
                            LOG.warn(messagePattern, artifact.createStringRepresentation(), artifact.getLicense());
                        }
                        missingNotice = true;
                    }
                }
            }
        }
        return missingNotice;
    }

    private boolean checkAndCopyLicenseFolder(String licenseFolderName, String targetLicenseFolder,
          boolean derived, Set<String> reportedLicenseFolders) {
        File sourceDir = new File(licenseSourcePath);
        File targetDir = new File(licenseTargetPath);
        File derivedFile = new File(sourceDir, licenseFolderName);
        if (derivedFile.exists()) {
            copyLicense(sourceDir, licenseFolderName, targetLicenseFolder, targetDir);
        } else {
            if (!reportedLicenseFolders.contains(licenseFolderName)) {
                final String derivedText = derived ? "derived " : "";
                if (failOnMissingLicenseFile) {
                    LOG.error("No {}license file in folder '{}'", derivedText, licenseFolderName);
                } else {
                    LOG.warn("No {}license file in folder '{}'", derivedText, licenseFolderName);
                }
                reportedLicenseFolders.add(licenseFolderName);
                return true;
            }
        }
        return false;
    }

    public boolean evaluateLicenseFiles(Inventory projectInventory) {
        Set<String> reportedLicenseFolders = new HashSet<>();
        boolean missingLicenseFile = false;
        if (licenseTargetPath != null && licenseSourcePath != null) {
            // copy all touched licenses to the given folder
            File sourceDir = new File(licenseSourcePath);
            if (!sourceDir.exists()) {
                sourceDir.mkdirs();
            }
            File targetDir = new File(licenseTargetPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            for (Artifact artifact : projectInventory.getArtifacts()) {
                // skip artifact which are not ready for distribution
                if (!artifact.isEnabledForDistribution()) {
                    continue;
                }

                String sourceLicense = artifact.getLicense();

                // without source license, no license meta data, no license texts / notices
                if (!StringUtils.hasText(sourceLicense)) {
                    continue;
                }

                // copy source license folder
                String licenseFolderName = LicenseMetaData.deriveLicenseFolderName(sourceLicense);
                missingLicenseFile |= checkAndCopyLicenseFolder(licenseFolderName, licenseFolderName,
                    false, reportedLicenseFolders);

                // try to resolve license meta data if available
                final LicenseMetaData matchingLicenseMetaData = projectInventory.
                        findMatchingLicenseMetaData(artifact.getComponent(), sourceLicense, artifact.getVersion());

                boolean isArtifactVersionSpecific = artifact.isVersionReported();
                boolean isSourceLicenseDataVersionSpecific = matchingLicenseMetaData == null ? isArtifactVersionSpecific :
                    !matchingLicenseMetaData.getVersion().equals("*");

                // copy derived, component-specific source license folder
                if (artifact.getComponent() != null) {
                    String componentFolderName = LicenseMetaData.deriveLicenseFolderName(artifact.getComponent());
                    String versionUnspecificPath =  licenseFolderName + "/" + componentFolderName;
                    String versionSpecificPath = versionUnspecificPath + "-" + artifact.getVersion();

                    if (!isArtifactVersionSpecific && !isSourceLicenseDataVersionSpecific) {
                        missingLicenseFile |= checkAndCopyLicenseFolder(versionUnspecificPath,
                                versionSpecificPath, true, reportedLicenseFolders);
                    } else {
                        missingLicenseFile |= checkAndCopyLicenseFolder(versionSpecificPath,
                                versionSpecificPath, true, reportedLicenseFolders);
                    }
                }

                // support for license mapping / effective licenses in license meta data
                if (matchingLicenseMetaData != null) {
                    final String[] effectiveLicenses = matchingLicenseMetaData.deriveLicenseInEffect().split("\\|");
                    for (String licenseInEffect : effectiveLicenses) {
                        String effectiveLicenseFolderName = LicenseMetaData.deriveLicenseFolderName(licenseInEffect);

                        // in any case copy the component/version unspecific license folder
                        checkAndCopyLicenseFolder(effectiveLicenseFolderName, effectiveLicenseFolderName, false, reportedLicenseFolders);

                        String componentFolderName = LicenseMetaData.deriveLicenseFolderName(artifact.getComponent());
                        String versionUnspecificPath =  licenseFolderName + "/" + componentFolderName;
                        String versionSpecificPath = versionUnspecificPath + "-" + artifact.getVersion();

                        String versionUnspecificTargetPath =  effectiveLicenseFolderName + "/" + componentFolderName;
                        String versionSpecificTargetPath = versionUnspecificTargetPath + "-" + artifact.getVersion();

                        if (!isArtifactVersionSpecific && !isSourceLicenseDataVersionSpecific) {
                            missingLicenseFile |= checkAndCopyLicenseFolder(versionUnspecificPath,
                                    versionSpecificTargetPath, true, reportedLicenseFolders);
                        } else {
                            missingLicenseFile |= checkAndCopyLicenseFolder(versionSpecificPath,
                                    versionSpecificTargetPath, true, reportedLicenseFolders);
                        }
                    }
                }
            }
        }
        return missingLicenseFile;
    }

    protected void writeLicenseSummary(Inventory projectInventory) throws Exception {
        if (targetDitaLicenseReportPath != null) {
            produceDita(projectInventory, DITA_LICENSE_TEMPLATE, new File(
                    targetDitaLicenseReportPath));
        }
    }

    protected void writeObligationSummary(Inventory projectInventory) throws Exception {
        if (targetDitaNoticeReportPath != null) {
            produceDita(projectInventory, DITA_NOTICE_TEMPLATE, new File(targetDitaNoticeReportPath));
        }
    }

    protected void writeMavenPom(Inventory projectInventory) throws Exception {
        if (targetMavenPomPath != null) {
            produceDita(projectInventory, MAVEN_POM_TEMPLATE, new File(targetMavenPomPath));
        }
    }

    protected void writeDiffReport(File referenceInventoryFile, Inventory referenceInventory,
                                   Inventory projectInventory) throws Exception {
        if (referenceInventoryFile != null && targetDitaDiffPath != null) {
            if (referenceInventory != null) {
                for (Artifact artifact : projectInventory.getArtifacts()) {
                    if (artifact.getGroupId() == null) {
                        continue;
                    }
                    if (artifact.getArtifactId() == null) {
                        continue;
                    }
                    Artifact foundArtifact = referenceInventory.
                            findArtifact(artifact.getGroupId(), artifact.getArtifactId());
                    // if not found try to match current using the find current strategies
                    // this supports to manipulate the match based on the content in the
                    // reference inventory.
                    if (foundArtifact == null) {
                        foundArtifact = referenceInventory.findCurrent(artifact);
                    }
                    if (foundArtifact != null) {
                        artifact.setPreviousVersion(foundArtifact.getVersion());
                    }
                }
            }

            // filter all artifacts were the version did not change
            Inventory filteredInventory = new Inventory();
            for (Artifact artifact : projectInventory.getArtifacts()) {
                if (artifact.getVersion() != null &&
                        !artifact.getVersion().trim().equals(artifact.getPreviousVersion())) {
                    filteredInventory.getArtifacts().add(artifact);
                }
            }

            produceDita(filteredInventory, DITA_DIFF_TEMPLATE, new File(targetDitaDiffPath));
        }
    }

    /**
     * Copies all files located in licenseSourceDir/licenseFolder to targetDir. No subdirectories are
     * copied.
     *
     * @param licenseSourceDir
     * @param licenseFolder
     * @param targetLicenseFolder
     * @param targetDir
     */
    private void copyLicense(File licenseSourceDir, String licenseFolder, String targetLicenseFolder, File targetDir) {
        Copy copy = new Copy();
        copy.setProject(new Project());
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File(licenseSourceDir, licenseFolder));
        fileSet.setIncludes("*");
        copy.setIncludeEmptyDirs(false);
        copy.setFailOnError(true);
        copy.setOverwrite(true);
        copy.addFileset(fileSet);
        copy.setTodir(new File(targetDir, targetLicenseFolder));
        copy.perform();

        // verify something was copied
        if (!new File(licenseSourceDir, licenseFolder).exists()) {
            throw new IllegalStateException("File copied, but not available in target location");
        }
    }

    private void produceDita(Inventory projectInventory, String templateResourcePath, File target)
            throws Exception {
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

        // regarding the report we only use the filtered inventory for the time being
        context.put("inventory", projectInventory.getFilteredInventory());
        context.put("report", this);
        template.merge(context, sw);

        FileUtils.write(target, sw.toString());
    }

    public String getGlobalInventoryPath() {
        return globalInventoryPath;
    }

    public void setGlobalInventoryPath(String globalInventoryPath) {
        this.globalInventoryPath = globalInventoryPath;
    }

    public String getReferenceInventoryPath() {
        return referenceInventoryPath;
    }

    public void setReferenceInventoryPath(String referenceInventoryPath) {
        this.referenceInventoryPath = referenceInventoryPath;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getTargetInventoryPath() {
        return targetInventoryPath;
    }

    public void setTargetInventoryPath(String targetInventoryPath) {
        this.targetInventoryPath = targetInventoryPath;
    }

    public String getTargetDitaReportPath() {
        return targetDitaReportPath;
    }

    public void setTargetDitaReportPath(String targetDitaReportPath) {
        this.targetDitaReportPath = targetDitaReportPath;
    }

    public String getTargetDitaComponentReportPath() {
        return targetDitaComponentReportPath;
    }

    public void setTargetDitaComponentReportPath(String targetDitaComponentReportPath) {
        this.targetDitaComponentReportPath = targetDitaComponentReportPath;
    }

    public void setTargetMavenPomPath(String targetMavenPomPath) {
        this.targetMavenPomPath = targetMavenPomPath;
    }

    public String getTargetDitaDiffPath() {
        return targetDitaDiffPath;
    }

    public void setTargetDitaDiffPath(String targetDitaDiffPath) {
        this.targetDitaDiffPath = targetDitaDiffPath;
    }

    public String getTargetDitaLicenseReportPath() {
        return targetDitaLicenseReportPath;
    }

    public void setTargetDitaLicenseReportPath(String targetDitaLicenseReportPath) {
        this.targetDitaLicenseReportPath = targetDitaLicenseReportPath;
    }

    public String getTargetDitaNoticeReportPath() {
        return targetDitaNoticeReportPath;
    }

    public void setTargetDitaNoticeReportPath(String targetDitaObligationReportPath) {
        this.targetDitaNoticeReportPath = targetDitaObligationReportPath;
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

    public Inventory getRepositoryInventory() {
        return repositoryInventory;
    }

    public void setRepositoryInventory(Inventory repositoryInventory) {
        this.repositoryInventory = repositoryInventory;
    }

    public ArtifactFilter getArtifactFilter() {
        return artifactFilter;
    }

    public void setArtifactFilter(ArtifactFilter artifactFilter) {
        this.artifactFilter = artifactFilter;
    }

    public String getLicenseTargetPath() {
        return licenseTargetPath;
    }

    public void setLicenseTargetPath(String licenseTargetPath) {
        this.licenseTargetPath = licenseTargetPath;
    }

    public String getLicenseSourcePath() {
        return licenseSourcePath;
    }

    public void setLicenseSourcePath(String licenseSourcePath) {
        this.licenseSourcePath = licenseSourcePath;
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

    public String[] getRepositoryIncludes() {
        return repositoryIncludes;
    }

    public void setRepositoryIncludes(String[] localRepositoryIncludes) {
        this.repositoryIncludes = localRepositoryIncludes;
    }

    public String[] getRepositoryExcludes() {
        return repositoryExcludes;
    }

    public void setRepositoryExcludes(String[] localRepositoryExcludes) {
        this.repositoryExcludes = localRepositoryExcludes;
    }


}
