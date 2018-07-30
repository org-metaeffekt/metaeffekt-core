/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Delete;
import org.metaeffekt.core.inventory.processor.AbstractInventoryProcessor;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;

public class ValidateInventoryProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(org.metaeffekt.core.inventory.processor.ValidateInventoryProcessor.class);

    /**
     * In this folder the license files are expected. The folder needs only to contain the licenses beloning to the
     * artifacts in the inventory that is validated. In this folder missing structures are managed as proposals. This
     * means that empty folders are created if a license or component-related information is expected. Folders are
     * removed if they are not required and only if they contain no further information (empty folders).
     */
    public static final String LICENSES_DIR = "licenses.path";

    /**
     * Allows that the folder creations activities are performed in a different path that the path the license
     * information is read from. Validation is performed against both LICENSES_DIR and LICENSES_TARGET_DIR.
     *
     * This aspect supports the inheritance of inventories. While the target folder is meant to contain the complete
     * set of licenses resulting from the inheritance, the LICENSES_DIR is only expected to provide content of the
     * entries from the inventory (and not it's parent).
     */
    public static final String LICENSES_TARGET_DIR = "licenses.target.path";
    public static final String CREATE_LICENSE_FOLDERS = "create.license.folders";
    public static final String CREATE_COMPONENT_FOLDERS = "create.component.folders";

    public ValidateInventoryProcessor() {
        super();
    }

    public ValidateInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        boolean error = false;

        final String baseDir = getProperties().getProperty(LICENSES_DIR);
        if (baseDir == null) {
            throw new IllegalStateException(format("Property '%s' must be set.", LICENSES_DIR));
        }

        final String targetDir = getProperties().getProperty(LICENSES_TARGET_DIR, baseDir);

        final boolean manageLicenseFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_LICENSE_FOLDERS, FALSE.toString()));

        final boolean manageComponentFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_COMPONENT_FOLDERS, FALSE.toString()));

        final List<String> licenseFoldersFromInventory = new ArrayList<>();
        final List<String> componentsFromInventory = new ArrayList<>();
        final List<String> licenseReferenceFromInventory = new ArrayList<>();
        final Set<String> licenseMetaDataFromArtifacts = new HashSet<>();
        final Set<String> licensesRequiringNotice = new HashSet<>();

        // build sets for later checks
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();

            if (artifact.isEnabledForDistribution()) {
                final String license = artifact.getLicense();
                final String componentName = artifact.getComponent();
                final String version = artifact.getVersion();

                boolean missingFields = false;
                if (StringUtils.isBlank(license)) {
                    LOG.error(format("Artifact '%s' without license. Proposal: add license association.", artifact.getId()));
                    missingFields = true;
                }

                if (StringUtils.isBlank(componentName)) {
                    LOG.error(format("Artifact '%s' without component. Proposal: add component / group name.", artifact.getId()));
                    missingFields = true;
                }

                if (StringUtils.isBlank(version)) {
                    LOG.error(format("Artifact '%s' without version. Proposal: add version.", artifact.getId()));
                    missingFields = true;
                }
                if (missingFields) {
                    error = true;
                    continue;
                }

                List<String> splitLicense = getSplitLicenses(license, "\\|");

                for (String singleLicense : splitLicense) {
                    String licenseFolder = inventory.getLicenseFolder(singleLicense);
                    if (!licenseFoldersFromInventory.contains(licenseFolder)) {
                        licenseFoldersFromInventory.add(licenseFolder);
                    }
                }

                if (StringUtils.isNotBlank(componentName)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(LicenseMetaData.normalizeId(license));
                    sb.append("/");
                    if (version.equals("*")) {
                        sb.append(LicenseMetaData.normalizeId(componentName));
                    } else {
                        sb.append(LicenseMetaData.normalizeId(componentName + "-" + version));
                    }
                    if (!componentsFromInventory.contains(sb.toString())) {
                        componentsFromInventory.add(sb.toString());
                    }

                    StringBuilder refSb = new StringBuilder();
                    if (!version.equals("*")) {
                        refSb.append(componentName).append("/");
                        refSb.append(version);
                    }
                    refSb.append("/");
                    refSb.append(artifact.getLicense());
                    if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                        licenseReferenceFromInventory.add(refSb.toString());
                    }
                }

                final LicenseMetaData matchingLicenseMetaData =
                        inventory.findMatchingLicenseMetaData(componentName, artifact.getLicense(), version);

                if (matchingLicenseMetaData != null) {
                    final String licenseInEffect = matchingLicenseMetaData.getLicenseInEffect();
                    List<String> splitLicenseInEffect = getSplitLicenses(licenseInEffect, "[,\\|]");
                    for (String singleLicense : splitLicenseInEffect) {
                        String licenseFolder = inventory.getLicenseFolder(singleLicense);
                        if (!licenseFoldersFromInventory.contains(licenseFolder)) {
                            licenseFoldersFromInventory.add(licenseFolder);
                        }
                    }
                    licenseMetaDataFromArtifacts.add(matchingLicenseMetaData.deriveQualifier());
                    licensesRequiringNotice.add(matchingLicenseMetaData.getLicense());
                }
            }
        }

        // check whether we have duplicate ids
        for (Artifact artifact : inventory.getArtifacts()) {
            if (StringUtils.isNotBlank(artifact.getId())) {
                Set<Artifact> alreadyReported = new HashSet<>();
                if (!alreadyReported.contains(artifact)) {
                    Artifact duplicateArtifact = inventory.findMatchingId(artifact);
                    if (duplicateArtifact != null) {
                        LOG.error("Duplicate artifact detected: {} / {}. Proposal: remove duplicate artifacts from inventory.",
                                artifact.getId() + "-" + artifact.createStringRepresentation(),
                                duplicateArtifact.getId() + "-" + duplicateArtifact.createStringRepresentation());
                        alreadyReported.add(duplicateArtifact);
                        error = true;
                    }
                }
            }
        }

        // check whether there is another current and provide hints
        for (Artifact artifact : inventory.getArtifacts()) {
            if (artifact.getClassification() != null &&
                    artifact.getClassification().contains(Inventory.CLASSIFICATION_CURRENT)) {
                Set<Artifact> alreadyReported = new HashSet<>();
                if (!alreadyReported.contains(artifact)) {
                    if (org.springframework.util.StringUtils.hasText(artifact.getArtifactId())) {
                        Artifact currentArtifact = inventory.findCurrent(artifact);
                        if (currentArtifact != null) {
                            LOG.error("Inconsistent classification (at least one and only one " +
                                            "with classification 'current' expected): {} / {}. " +
                                            "Recommendation: revise artifact classification.",
                                    artifact.getId() + "-" + artifact.createStringRepresentation(),
                                    currentArtifact.getId() + "-" + currentArtifact.createStringRepresentation());
                            alreadyReported.add(currentArtifact);

                            error = true;
                        }
                    }
                }
            }
        }


        // check whether all artifacts which require a license notice have a license notice.
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();

            if (artifact.isEnabledForDistribution()) {
                final LicenseMetaData matchingLicenseMetaData = inventory.findMatchingLicenseMetaData(artifact);

                if (matchingLicenseMetaData == null) {
                    if (licensesRequiringNotice.contains(artifact.getLicense())) {
                        LOG.error(format("Artifact '%s' with license '%s' requires a license notice. " +
                                "Proposal: add license notice to notices in inventory.", artifact.getId(), artifact.getLicense()));
                        error = true;
                    }
                }
            }
        }

        // 1st level is the license level
        final String[] licenseDirectories = scanForDirectories(baseDir);

        final Set<String> licenseFoldersFromDirectory = new HashSet<>();
        for (String file : licenseDirectories) {
            licenseFoldersFromDirectory.add(file);
        }

        // iterate component level for each license folder
        for (String file : licenseFoldersFromInventory) {
            // check whether this folder is required
            final String[] componentDirectories = scanForDirectories(new File(new File(baseDir), file).getPath());
            for (String component : componentDirectories) {
                String licenseComponent = file + "/" + component;
                if (!componentsFromInventory.contains(licenseComponent)) {
                    if (manageComponentFolders && isEmptyFolder(baseDir, licenseComponent)) {
                        removeFolder(baseDir, licenseComponent);
                    } else {
                        LOG.error(format("Component folder '%s' does not match any artifact (not banned, not internal) in the inventory. Proposal: remove the folder.", licenseComponent));
                        error = true;
                    }
                }
            }
        }

        // check whether license exists in inventory
        for (String file : licenseDirectories) {
            if (!licenseFoldersFromInventory.contains(file)) {
                if (manageLicenseFolders && isEmptyFolder(targetDir, file)) {
                    removeFolder(baseDir, file);
                    licenseFoldersFromDirectory.remove(file);
                } else {
                    LOG.error(format("License folder '%s' does not match any artifact license / effective license in inventory. Proposal: remove license folder.", file));
                    error = true;
                }
            }
        }

        // check whether the license folder exists
        for (String file : licenseFoldersFromInventory) {
            if (!licenseFoldersFromDirectory.contains(file)) {
                if (manageLicenseFolders) {
                    if (!new File(baseDir, file).exists()) {
                        createFolder(baseDir, file);
                    }
                } else {
                    LOG.error("License folder missing: " + file);
                    error = true;
                }
            }
        }

        // check whether the license folder contains any files
        for (String file : licenseFoldersFromInventory) {
            if (isEmptyFolder(baseDir, file) && isEmptyFolder(targetDir, file)) {
                LOG.error(format("License folder '%s' does not contain any license or notice files. Proposal: add license and/or notice to the folder.", file));
                error = true;
            }
        }

        // create the appropriate folders for licenses
        for (String licenseFolder : licenseFoldersFromInventory) {
            if (manageLicenseFolders && !new File(baseDir, licenseFolder).exists()) {
                File folder = new File(new File(baseDir), licenseFolder);
                folder.mkdirs();
            }
        }

        // check that all component folders exist is missing
        for (String component : componentsFromInventory) {
            final File componentFolder = new File(new File(baseDir), component);
            if (!componentFolder.exists()) {
                if (manageComponentFolders) {
                    createFolder(baseDir, component);
                } else {
                    LOG.error(format("Component folder '%s' does not exist. Proposal: add component specific folder.", component));
                    error = true;
                }
            }
        }

        // check whether component folder is empty
        for (String component : componentsFromInventory) {
            if (isEmptyFolder(baseDir, component) && isEmptyFolder(targetDir, component )) {
                LOG.error(format("Component folder '%s' does not contain any license or notice files. Proposal: add component specific license and/or notice to the folder.", component));
                error = true;
            }
        }

        // iterate over inventory license notices to check whether these are matching an artifact or not
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            StringBuilder refSb = new StringBuilder();
            refSb.append(licenseMetaData.getComponent()).append("/");
            refSb.append(licenseMetaData.getVersion()).append("/");
            refSb.append(licenseMetaData.getLicense());

            if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                LOG.error(format("License notice '%s' not used in inventory. Proposal: remove the license notice from the inventory.", refSb.toString()));
                error = true;
            }

            if (StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                switch (sourceCategory) {
                    case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                    case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                        LOG.warn(format("Source category '%s' deprecated. Proposal: change source category to " +
                                "'annex' or 'retained'.", licenseMetaData.getSourceCategory()));

                    case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                    case LicenseMetaData.SOURCE_CATEGORY_ANNEX:
                        break;
                    default:
                        LOG.error(format("Source category '%s' not supported. Proposal: change source category to " +
                                "'annex' or 'retained' or remove the value.", licenseMetaData.getSourceCategory()));
                        error = true;
                }
            }
        }

        // verify unique match of licenses within one component
        for (Artifact artifact : inventory.getArtifacts()) {
            String license = artifact.getLicense();
            if (StringUtils.isNotBlank(license)) {
                license = license.trim();
                String componentRef = artifact.getComponent() + "/" + artifact.getVersion();
                for (Artifact candidateArtifact : inventory.getArtifacts()) {
                    String candiateComponentRef = candidateArtifact.getComponent() + "/" + candidateArtifact.getVersion();
                    if (componentRef.equals(candiateComponentRef)) {
                        String candidateLicense = candidateArtifact.getLicense();
                        if (candidateLicense != null) {
                            candidateLicense = candidateLicense.trim();
                        }
                        if (!license.equals(candidateLicense)) {
                            LOG.error(format("Component '%s' in version '%s' does not have a unique license association: '%s' <> '%s'. See '%s'. Proposal: split component.",
                                    artifact.getComponent(), artifact.getVersion(), license, candidateLicense, candidateArtifact.getId()));
                            error = true;
                        }
                    }
                }

            }
        }

        // verify unique match of licenses within one component
        for (Artifact artifact : inventory.getArtifacts()) {
            String version = artifact.getVersion();
            final String id = artifact.getId();
            final String groupId = artifact.getGroupId();
            if (StringUtils.isNotBlank(groupId) && version != null && id != null && !id.contains(version)) {
                LOG.error(format("Version information inconsistent. " +
                                "Mismatch between version and artifact file name. Version '%s' not contained in '%s'. " +
                                "Proposal: fix artifact file name to include correct version or remove the group id in case it is not a maven-managed artifact.",
                        artifact.getVersion(), id));
                error = true;
            }
        }

        // validate license notice content
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            String notice = licenseMetaData.getNotice();
            if (StringUtils.isBlank(notice)) {
                LOG.error(format("Empty license notice for '%s'.", licenseMetaData.deriveQualifier()));
                error = true;
            }

            error |= validateElement(licenseMetaData, notice, "p");
            error |= validateElement(licenseMetaData, notice, "i");
            error |= validateElement(licenseMetaData, notice, "li");
            error |= validateElement(licenseMetaData, notice, "ol");
            error |= validateElement(licenseMetaData, notice, "ul");
            error |= validateElement(licenseMetaData, notice, "strong");

            error |= validateEvenCount(licenseMetaData, notice, "\"");

            error |= validateNotContained(licenseMetaData, notice, " .</");
            // validateNotContained(licenseMetaData, notice, "> ");
            error |= validateNotContained(licenseMetaData, notice, "IS\"WITHOUT");
        }

        if (error && isFailOnError()) {
            throw new IllegalStateException("Validation error detected. See previous log output.");
        }
    }

    private void createFolder(String folder, String file) {
        final File componentFolder = new File(new File(folder), file);
        componentFolder.mkdirs();
    }

    private void removeFolder(String folder, String file) {
        final File dir = new File(new File(folder), file);

        Delete delete = new Delete();
        delete.setDir(dir);
        delete.execute();
    }

    private boolean isEmptyFolder(String baseDir, String file) {
        final File folder = new File(new File(baseDir), file);
        final String[] files = scanForFiles(folder.getPath());
        return files.length == 0;
    }

    private boolean validateElement(LicenseMetaData licenseMetaData, String notice, String element) {
        String openElement = "<" + element + ">";
        String closeElement = "</" + element + ">";
        return validateOpenClose(licenseMetaData, notice, openElement, closeElement);
    }

    private boolean validateOpenClose(LicenseMetaData licenseMetaData, String notice, String openElement, String closeElement) {
        if (StringUtils.countMatches(notice, openElement) != StringUtils.countMatches(notice, closeElement)) {
            LOG.error(format("License text '%s': number of '%s' does not match number of '%s'.", licenseMetaData.deriveQualifier(), openElement, closeElement));
            return true;
        }
        return false;
    }

    private boolean validateNotContained(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) > 0) {
            LOG.error(format("License text '%s': suspicious character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
            return true;
        }
        return false;
    }

    private boolean validateEvenCount(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) % 2 != 0) {
            LOG.error(format("License text '%s': expected even count of character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
            return true;
        }
        return false;
    }

    private List<String> getSplitLicenses(String license, String separatorRegExp) {
        List<String> licenses = new ArrayList<>();
        if (!StringUtils.isBlank(license)) {
            String[] splitLicense = license.split(separatorRegExp);
            for (int i = 0; i < splitLicense.length; i++) {
                if (StringUtils.isNotBlank(splitLicense[i])) {
                    licenses.add(splitLicense[i].trim());
                }
            }
        }
        return licenses;
    }

    private String[] scanForDirectories(String baseDir) {
        if (new File(baseDir).exists()) {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(baseDir);
            directoryScanner.setIncludes(new String[] {"*"});
            directoryScanner.scan();
            return directoryScanner.getIncludedDirectories();
        }
        return new String[0];
    }

    private String[] scanForFiles(String baseDir) {
        if (new File(baseDir).exists()) {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(baseDir);
            directoryScanner.setIncludes(new String[] {"*"});
            directoryScanner.setExcludes(new String[] {".*"});
            directoryScanner.scan();
            return directoryScanner.getIncludedFiles();
        }
        return new String[0];
    }

}
