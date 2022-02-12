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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Delete;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static java.lang.String.format;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class ValidateInventoryProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(org.metaeffekt.core.inventory.processor.ValidateInventoryProcessor.class);

    /**
     * In this folder the license files are expected. The folder needs only to contain the licenses beloning to the
     * artifacts in the inventory that is validated. In this folder missing structures are managed as proposals. This
     * means that empty folders are created if a license or component-related information is expected. Folders are
     * removed if they are not required and only if they contain no further information (empty folders).
     */
    public static final String LICENSES_DIR = "licenses.path";

    public static final String COMPONENTS_DIR = "components.path";

    public static final String COMPONENTS_TARGET_DIR = "components.target.path";

    /**
     * Allows that the folder creations activities are performed in a different path that the path the license
     * information is read from. Validation is performed against both LICENSES_DIR and LICENSES_TARGET_DIR.
     * <p>
     * This aspect supports the inheritance of inventories. While the target folder is meant to contain the complete
     * set of licenses resulting from the inheritance, the LICENSES_DIR is only expected to provide content of the
     * entries from the inventory (and not it's parent).
     */
    public static final String LICENSES_TARGET_DIR = "licenses.target.path";

    public static final String VALIDATE_LICENSE_FOLDERS = "validate.license.folders";
    public static final String VALIDATE_COMPONENT_FOLDERS = "validate.component.folders";

    public static final String CREATE_LICENSE_FOLDERS = "create.license.folders";
    public static final String CREATE_COMPONENT_FOLDERS = "create.component.folders";

    public static final String DELETE_LICENSE_FOLDERS = "delete.license.folders";
    public static final String DELETE_COMPONENT_FOLDERS = "delete.component.folders";

    public static final String SEPARATOR = "/";

    public ValidateInventoryProcessor() {
        super();
    }

    public ValidateInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        boolean error = false;

        final boolean validateLicenseFolders = Boolean.parseBoolean(getProperties().
                getProperty(VALIDATE_LICENSE_FOLDERS, STRING_TRUE));

        final boolean validateComponentFolders = Boolean.parseBoolean(getProperties().
                getProperty(VALIDATE_COMPONENT_FOLDERS, STRING_TRUE));

        final String licensesBaseDir = getProperties().getProperty(LICENSES_DIR);
        if (licensesBaseDir == null && validateLicenseFolders) {
            throw new IllegalStateException(format("Property '%s' must be set.", LICENSES_DIR));
        }

        final String componentsBaseDir = getProperties().getProperty(COMPONENTS_DIR);
        if (componentsBaseDir == null && validateComponentFolders) {
            throw new IllegalStateException(format("Property '%s' must be set.", COMPONENTS_DIR));
        }

        final String licensesTargetDir = getProperties().getProperty(LICENSES_TARGET_DIR, licensesBaseDir);
        final String componentsTargetDir = getProperties().getProperty(COMPONENTS_TARGET_DIR, licensesBaseDir);

        final boolean manageLicenseFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_LICENSE_FOLDERS, STRING_FALSE));

        final boolean manageComponentFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_COMPONENT_FOLDERS, STRING_FALSE));

        final boolean deleteLicenseFolders = Boolean.parseBoolean(getProperties().
                getProperty(DELETE_LICENSE_FOLDERS, STRING_FALSE));

        final boolean deleteComponentFolders = Boolean.parseBoolean(getProperties().
                getProperty(DELETE_COMPONENT_FOLDERS, STRING_FALSE));

        final List<String> licenseFoldersFromInventory = new ArrayList<>();

        // the component folders derived from the inventory
        final List<String> componentFoldersFromInventory = new ArrayList<>();

        final List<String> licenseReferenceFromInventory = new ArrayList<>();

        final Set<String> licenseMetaDataFromArtifacts = new HashSet<>();

        final Set<String> licensesRequiringNotice = new HashSet<>();

        int index = 1;

        // build sets for later checks
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();

            // check for '|' in associated license
            if (!StringUtils.isBlank(artifact.getLicense())) {
                if (artifact.getLicense().contains("|")) {
                    log(format("%04d: Artifact '%s' associated license contains '|'.", index++, artifact.getId()));
                    log(format("      Proposal: correct associated license."));
                    error = true;
                    continue;
                }
            }

            final String license = artifact.getLicense();
            final String componentName = artifact.getComponent();
            final String version = artifact.getVersion();

            if (!artifact.isBanned()) {
                // default meta information is required

                boolean missingFields = false;
                if (StringUtils.isBlank(license)) {
                    log(format("%04d: Artifact '%s' without license.", index++, artifact.getId()));
                    log(format("      Proposal: add license association."));
                    missingFields = true;
                }

                if (StringUtils.isBlank(componentName)) {
                    log(format("%04d: Artifact '%s' without component.", index++, artifact.getId()));
                    log(format("      Proposal: add component / group name."));
                    missingFields = true;
                }

                if (StringUtils.isBlank(version)) {
                    log(format("%04d: Artifact '%s' without version.", index++, artifact.getId()));
                    log(format("      Proposal: add version."));
                    missingFields = true;
                }
                if (missingFields) {
                    error = true;
                    continue;
                }
            }

            if (artifact.isEnabledForDistribution()) {

                List<String> splitLicenseCand = getSplitLicenses(license, "[\\|,\\+]");

                List<String> splitLicense = new ArrayList<>();
                for (String singleLicense : splitLicenseCand) {
                    String converted = singleLicense;

                    // remove addons that do not affect the license content
                    converted = converted.replace("(or any later version)", "");
                    converted = converted.replace("or any later version", "");
                    converted = converted.replace("(with subcomponents)", "");
                    converted = converted.replace("(with-subcomponents)", "");
                    converted = converted.trim();

                    // identify licenses without version
                    boolean undefined = converted.contains("(undefined)");

                    if (!undefined && !splitLicense.contains(converted)) {
                        splitLicense.add(converted);
                    }
                }

                for (String singleLicense : splitLicense) {
                    String licenseFolder = inventory.getLicenseFolder(singleLicense);
                    if (!licenseFoldersFromInventory.contains(licenseFolder)) {
                        licenseFoldersFromInventory.add(licenseFolder);
                    }
                }

                if (StringUtils.isNotBlank(componentName)) {
                    boolean wildcardMatchPropagation = Boolean.valueOf(artifact.get(KEY_WILDCARD_MATCH, STRING_FALSE));
                    boolean wildcardMatch = ASTERISK.equals(version);
                    boolean placeholderMatch = version != null &&
                            version.startsWith(VERSION_PLACHOLDER_PREFIX) && version.endsWith(VERSION_PLACHOLDER_SUFFIX);

                    String componentFolder;
                    if (wildcardMatch || wildcardMatchPropagation || placeholderMatch) {
                        componentFolder = LicenseMetaData.deriveComponentFolderName(componentName);
                    } else {
                        componentFolder = LicenseMetaData.deriveComponentFolderName(componentName, version);
                    }
                    if (!componentFoldersFromInventory.contains(componentFolder)) {
                        componentFoldersFromInventory.add(componentFolder);
                    }

                    // Add string representation using given version (including wildcard)
                    StringBuilder refSb = new StringBuilder();
                    refSb.append(componentName).append("/");
                    refSb.append(version);
                    refSb.append("/");
                    refSb.append(artifact.getLicense());
                    if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                        licenseReferenceFromInventory.add(refSb.toString());
                    }

                    // Add also a fully version agnostic variant to cover wildcards only on license notice level
                    StringBuilder refVersionWildcard = new StringBuilder();
                    refVersionWildcard.append(componentName).append(SEPARATOR);
                    refVersionWildcard.append(ASTERISK);
                    refVersionWildcard.append(SEPARATOR);
                    refVersionWildcard.append(artifact.getLicense());
                    if (!licenseReferenceFromInventory.contains(refVersionWildcard.toString())) {
                        licenseReferenceFromInventory.add(refVersionWildcard.toString());
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

                // associated licenses with '+' or ',' require explanation in a notice.
                if (matchingLicenseMetaData == null) {
                    if (artifact.getLicense().contains("+") || artifact.getLicense().contains(",")) {
                        licensesRequiringNotice.add(artifact.getLicense());
                    } else if (artifact.getLicense().contains(",")) {
                        licensesRequiringNotice.add(artifact.getLicense());
                    }
                }
            }
        }

        // check whether we have duplicate ids
        for (Artifact artifact : inventory.getArtifacts()) {
            String artifactChecksum = normalizedChecksum(artifact);
            String artifactVersion = normalizedVersion(artifact);

            if (StringUtils.isNotBlank(artifact.getId())) {
                Set<Artifact> alreadyReported = new HashSet<>();
                if (!alreadyReported.contains(artifact)) {
                    Artifact duplicateArtifact = inventory.findMatchingId(artifact);
                    if (duplicateArtifact != null) {
                        String duplicateChecksum = normalizedChecksum(duplicateArtifact);
                        String duplicateVersion = normalizedVersion(duplicateArtifact);

                        // artifacts with different checksum are not reported
                        if (!Objects.equals(artifactChecksum, duplicateChecksum)) {
                            continue;
                        }

                        // artifacts with different version are not reported (as long as they don't have (identical) checksums)
                        if (!Objects.equals(artifactVersion, duplicateVersion)) {
                            if (StringUtils.isEmpty(artifactChecksum)) {
                                continue;
                            }
                        }

                        log(format("%04d: Duplicate artifact detected: %s / %s.",
                                index++, artifact.getId(), duplicateArtifact.getId()));
                        log(format("      Proposal: remove duplicate artifacts from inventory."));
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
                            log(format("%04d: Inconsistent classification (at least one and only one " +
                                            "with classification 'current' expected): %s / %s. " +
                                            "Recommendation: revise artifact classification.",
                                    index++, artifact.getId() + "-" + artifact.createStringRepresentation(),
                                    currentArtifact.getId() + "-" + currentArtifact.createStringRepresentation()));
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
                        log(format("%04d: Artifact '%s', component '%s' with license '%s' requires a license notice. ",
                                index++, artifact.getId(), artifact.getComponent(), artifact.getLicense()));
                        log(format("      Proposal: add license notice to notices in inventory."));
                        error = true;
                    }
                }
            }
        }

        // check whether license exists in inventory
        if (validateLicenseFolders) {
            // 1st level is the license level
            final String[] licenseDirectories = scanForDirectories(licensesBaseDir);
            final Set<String> licenseFoldersFromDirectory = new HashSet<>();
            for (String file : licenseDirectories) {
                licenseFoldersFromDirectory.add(file);
            }

            for (String file : licenseDirectories) {
                if (!licenseFoldersFromInventory.contains(file)) {
                    if ((manageLicenseFolders && isEmptyFolder(licensesTargetDir, file)) || deleteLicenseFolders) {
                        removeFolder(licensesBaseDir, file);
                        licenseFoldersFromDirectory.remove(file);
                    } else {
                        log(format("%04d: License folder '%s' does not match any artifact license / effective license in inventory.", index++, file));
                        log(format("      Proposal: remove license folder."));
                        error = true;
                    }
                }
            }

            // check whether the license folder exists
            for (String file : licenseFoldersFromInventory) {
                if (!licenseFoldersFromDirectory.contains(file)) {
                    if (manageLicenseFolders) {
                        if (!new File(licensesBaseDir, file).exists() && !new File(licensesTargetDir, file).exists()) {
                            createFolder(licensesBaseDir, file);
                        }
                    } else {
                        log(format("%04d: License folder missing: %s", index++, file));
                        log(format("      Proposal: add license folder."));
                        error = true;
                    }
                }
            }

            // check whether the license folder contains any files
            for (String file : licenseFoldersFromInventory) {
                if (isEmptyFolder(licensesBaseDir, file) && isEmptyFolder(licensesTargetDir, file)) {
                    log(format("%04d: License folder '%s' does not contain any license or notice files.", index++, file));
                    log(format("      Proposal: add license to the license folder."));
                    error = true;
                }
            }
        }

        // iterate component level for each component folder
        if (validateComponentFolders) {
            // 1st level is the component level
            final String[] componentDirectories = scanForDirectories(componentsBaseDir);
            final Set<String> componentFoldersFromDirectory = new HashSet<>();
            for (String file : componentDirectories) {
                componentFoldersFromDirectory.add(file);
            }

            for (String componentFolder : componentFoldersFromDirectory) {
                // check whether this folder is required
                if (!componentFoldersFromInventory.contains(componentFolder)) {
                    if ((manageComponentFolders && isEmptyFolder(componentsBaseDir, componentFolder)) || deleteComponentFolders) {
                        removeFolder(componentsBaseDir, componentFolder);
                    } else {
                        log(format("%04d: Component folder '%s' does not match any artifact (not banned, not internal) in the inventory.", index++, componentFolder));
                        log(format("      Proposal: remove the folder."));
                        error = true;
                    }
                }
            }

            // check that all component folders exist
            for (String component : componentFoldersFromInventory) {
                final File componentFolder = new File(componentsBaseDir, component);
                if (!componentFolder.exists() && !new File(componentsTargetDir, component).exists()) {
                    if (manageComponentFolders) {
                        createFolder(componentsBaseDir, component);
                    } else {
                        log(format("%04d: Component folder '%s' does not exist.", index++, component));
                        log(format("      Proposal: add component specific folder."));
                        error = true;
                    }
                }
            }

            // check whether component folder is empty
            for (String component : componentFoldersFromInventory) {
                if (isEmptyFolder(componentsBaseDir, component) && isEmptyFolder(componentsTargetDir, component)) {
                    log(format("%04d: Component folder '%s' does not contain any license or notice files.", index++, component));
                    log(format("      Proposal: add component specific license and/or notice to the component folder."));
                    error = true;
                }
            }
        }

        // iterate over inventory license notices to check whether these are matching an artifact or not
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            StringBuilder refSb = new StringBuilder();
            refSb.append(licenseMetaData.getComponent()).append(SEPARATOR);
            refSb.append(licenseMetaData.getVersion()).append(SEPARATOR);
            refSb.append(licenseMetaData.getLicense());

            if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                log(format("%04d: License notice '%s' not used in inventory.", index++, refSb.toString()));
                log(format("      Proposal: remove the license notice from the inventory."));
                error = true;
            }

            if (StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                switch (sourceCategory) {
                    case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                    case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                        LOG.warn(format("%04d: Source category '%s' deprecated.", index++, licenseMetaData.getSourceCategory()));
                        log(format("      Proposal: change source category to 'annex' or 'retained'."));
                    case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                    case LicenseMetaData.SOURCE_CATEGORY_ANNEX:
                        break;
                    default:
                        log(format("%04d: Source category '%s' not supported.", index++, licenseMetaData.getSourceCategory()));
                        log(format("      Proposal: change source category to 'annex' or 'retained' or remove the value."));
                        error = true;
                }
            }

            String effectiveLicense = licenseMetaData.deriveLicenseInEffect();
            if (!StringUtils.isBlank(effectiveLicense)) {
                if (effectiveLicense.contains(",")) {
                    log(format("%04d: Effective license of '%s' contains ','.", index++,
                            licenseMetaData.deriveQualifier()));
                    log(format("      Proposal: replace ',' by '|'."));
                    error = true;
                }
            }
        }

        // verify unique match of licenses within one component
        for (Artifact artifact : inventory.getArtifacts()) {
            String license = artifact.getLicense();

            if (StringUtils.isEmpty(artifact.getComponent())) continue;
            if (StringUtils.isEmpty(artifact.getVersion())) continue;
            if (StringUtils.isEmpty(license)) continue;

            if (StringUtils.isNotBlank(license)) {
                license = license.trim();
                String componentRef = artifact.getComponent() + SEPARATOR + artifact.getVersion();
                for (Artifact candidateArtifact : inventory.getArtifacts()) {

                    if (StringUtils.isEmpty(candidateArtifact.getComponent())) continue;
                    if (StringUtils.isEmpty(candidateArtifact.getVersion())) continue;
                    if (StringUtils.isEmpty(candidateArtifact.getLicense())) continue;

                    String candiateComponentRef = candidateArtifact.getComponent() + SEPARATOR + candidateArtifact.getVersion();
                    if (componentRef.equals(candiateComponentRef)) {
                        String candidateLicense = candidateArtifact.getLicense();
                        if (candidateLicense != null) {
                            candidateLicense = candidateLicense.trim();
                        }
                        if (!license.equals(candidateLicense)) {
                            log(format("%04d: Component '%s' in version '%s' does not have a unique license association: '%s' <> '%s'.",
                                    index++, artifact.getComponent(), artifact.getVersion(), license, candidateLicense));
                            log(format("      Proposal: split component."));
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
                log(format("%04d: Version information inconsistent. " +
                                "Mismatch between version and artifact file name. Version '%s' not contained in '%s'. ",
                        index++, artifact.getVersion(), id));
                log(format("      Proposal: fix artifact file name to include correct version or remove the group id in case it is not a maven-managed artifact."));
                error = true;
            }
        }

        // validate license notice content
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            String notice = licenseMetaData.getCompleteNotice();
            if (StringUtils.isBlank(notice)) {
                log(format("%04d: Empty license notice for '%s'.", index++, licenseMetaData.deriveQualifier()));
                log(format("      Proposal: validate/add license notice content."));
                error = true;
            }

            error |= validateElement(licenseMetaData, notice, "p");
            error |= validateElement(licenseMetaData, notice, "codeph");
            error |= validateElement(licenseMetaData, notice, "filename");
            error |= validateElement(licenseMetaData, notice, "i");
            error |= validateElement(licenseMetaData, notice, "li");
            error |= validateElement(licenseMetaData, notice, "ol");
            error |= validateElement(licenseMetaData, notice, "ul");
            error |= validateElement(licenseMetaData, notice, "strong");
            error |= validateElement(licenseMetaData, notice, "line");
            error |= validateElement(licenseMetaData, notice, "lines");
            error |= validateElement(licenseMetaData, notice, "lq");
            error |= validateElement(licenseMetaData, notice, "b");

            error |= validateEvenCount(licenseMetaData, notice, "\"");

            error |= validateNotContained(licenseMetaData, notice, " .</");
            // validateNotContained(licenseMetaData, notice, "> ");
            error |= validateNotContained(licenseMetaData, notice, "IS\"WITHOUT");

            // detect obvious typos
            error |= validateNotContained(licenseMetaData, notice, "infromation");
            error |= validateNotContained(licenseMetaData, notice, "Infromation");
            error |= validateNotContained(licenseMetaData, notice, "sofware");
            error |= validateNotContained(licenseMetaData, notice, "Sofware");
        }

        if (error && isFailOnError()) {
            throw new IllegalStateException("Validation error detected. See previous log output.");
        }
    }

    private String normalizedVersion(Artifact artifact) {
        String normalizedVersion = artifact.getVersion();
        if (StringUtils.isBlank(normalizedVersion)) {
            normalizedVersion = null;
        }
        return normalizedVersion;
    }

    private String normalizedChecksum(Artifact artifact) {
        String normalizedChecksum = artifact.getChecksum();
        if (StringUtils.isBlank(normalizedChecksum)) {
            normalizedChecksum = null;
        }
        return normalizedChecksum;
    }

    private void createFolder(String folder, String file) {
        final File componentFolder = new File(new File(folder), file);
        componentFolder.mkdirs();
    }

    private void removeFolder(String folder, String file) {
        final File dir = new File(new File(folder), file);
        Delete delete = new Delete();
        delete.setDir(dir);
        delete.setIncludeEmptyDirs(true);
        delete.execute();
    }

    private boolean isEmptyFolder(String baseDir, String file) {
        final File folder = new File(new File(baseDir), file);
        return !hasFiles(folder.getPath());
    }

    private boolean validateElement(LicenseMetaData licenseMetaData, String notice, String element) {
        String openElement = "<" + element + ">";
        String closeElement = "</" + element + ">";
        return validateOpenClose(licenseMetaData, notice, openElement, closeElement);
    }

    private boolean validateOpenClose(LicenseMetaData licenseMetaData, String notice, String openElement, String closeElement) {
        if (StringUtils.countMatches(notice, openElement) != StringUtils.countMatches(notice, closeElement)) {
            log(format("License text '%s': number of '%s' does not match number of '%s'.", licenseMetaData.deriveQualifier(), openElement, closeElement));
            return true;
        }
        return false;
    }

    private boolean validateNotContained(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) > 0) {
            log(format("License text '%s': suspicious character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
            return true;
        }
        return false;
    }

    private boolean validateEvenCount(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) % 2 != 0) {
            log(format("License text '%s': expected even count of character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
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
            directoryScanner.setIncludes(new String[]{ASTERISK});
            directoryScanner.scan();
            return directoryScanner.getIncludedDirectories();
        }
        return new String[0];
    }

    private boolean hasFiles(String baseDir) {
        if (new File(baseDir).exists()) {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(baseDir);
            directoryScanner.setIncludes(new String[]{ASTERISK});
            directoryScanner.setExcludes(new String[]{"." + ASTERISK});
            directoryScanner.scan();
            return directoryScanner.getIncludedFilesCount() > 0;
        }
        return false;
    }

    protected void log(String string) {
        if (isFailOnError()) {
            LOG.error(string);
        } else {
            LOG.warn(string);
        }
    }

}
