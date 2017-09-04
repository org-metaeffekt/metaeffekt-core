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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public class ValidateLicensesProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateLicensesProcessor.class);

    public static final String LICENSES_DIR = "licenses.path";
    public static final String CREATE_LICENSE_FOLDERS = "create.license.folders";
    public static final String CREATE_COMPONENT_FOLDERS = "create.component.folders";

    public ValidateLicensesProcessor() {
        super();
    }

    public ValidateLicensesProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        final boolean createLicenseFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_LICENSE_FOLDERS, Boolean.FALSE.toString()));

        final boolean createComponentFolders = Boolean.parseBoolean(getProperties().
                getProperty(CREATE_COMPONENT_FOLDERS, Boolean.FALSE.toString()));

        final List<String> licenseFoldersFromInventory = new ArrayList<>();
        final List<String> componentsFromInventory = new ArrayList<>();
        final List<String> licenseReferenceFromInventory = new ArrayList<>();

        // build sets for later checks
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();

            final String license = artifact.getLicense();
            List<String> splitLicense = getSplitLicenses(license);

            for (String singleLicense : splitLicense) {
                String licenseFolder = inventory.getLicenseFolder(singleLicense);
                if (!licenseFoldersFromInventory.contains(licenseFolder)) {
                    licenseFoldersFromInventory.add(licenseFolder);
                }
            }

            if (artifact.isEnabledForDistribution()) {
                if (StringUtils.isNotBlank(artifact.getName())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(LicenseMetaData.normalizeId(artifact.getLicense()));
                    sb.append("/");
                    sb.append(LicenseMetaData.normalizeId(artifact.getName() + "-" + artifact.getVersion()));
                    if (!componentsFromInventory.contains(sb.toString())) {
                        componentsFromInventory.add(sb.toString());
                    }

                    StringBuilder refSb = new StringBuilder();
                    refSb.append(artifact.getName()).append("/");
                    refSb.append(artifact.getVersion()).append("/");
                    refSb.append(artifact.getLicense());
                    if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                        licenseReferenceFromInventory.add(refSb.toString());
                    }
                }
            }
        }

        // build further set for later checks
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            final String license = licenseMetaData.getLicenseInEffect();
            List<String> splitLicense = getSplitLicenses(license);
            for (String singleLicense : splitLicense) {
                String licenseFolder = inventory.getLicenseFolder(singleLicense);
                if (!licenseFoldersFromInventory.contains(licenseFolder)) {
                    licenseFoldersFromInventory.add(licenseFolder);
                }
            }
        }

        // 1st level is the license level
        final String baseDir = getProperties().getProperty(LICENSES_DIR);
        final String[] licenseDirectories = scanForDirectories(baseDir);

        // check whether license exists in inventory
        final HashSet<String> licenseFoldersFromDirectory = new HashSet<>();
        for (String file : licenseDirectories) {
            licenseFoldersFromDirectory.add(file);
            if (!licenseFoldersFromInventory.contains(file)) {
                LOG.warn(String.format("License folder '%s' does not match any artifact license / effective license in inventory.", file));
            }
        }

        // check whether the license folder exists
        for (String file : licenseFoldersFromInventory) {
            if (!licenseFoldersFromDirectory.contains(file)) {
                LOG.warn("License folder missing: " + file);
            }
        }

        // check whether the license folder contains any files
        for (String file : licenseFoldersFromInventory) {
            final String[] files = scanForFiles(new File(new File(baseDir), file).getPath());
            if (files.length == 0) {
                LOG.warn(String.format("License folder '%s' does not contain any license or notice files.", file));
            }
        }

        // create the appropriate folders for licenses
        for (String licenseFolder : licenseFoldersFromInventory) {
            if (createLicenseFolders) {
                File file = new File(baseDir);
                File folder = new File(file, licenseFolder);
                folder.mkdirs();
            }
        }

        // iterate component level for each license folder
        for (String file : licenseFoldersFromInventory) {
            // check whether this folder is required
            final String[] componentDirectories = scanForDirectories(new File(new File(baseDir), file).getPath());
            for (String component : componentDirectories) {
                String licenseComponent = file + "/" + component;
                if (!componentsFromInventory.contains(licenseComponent)) {
                    LOG.warn(String.format("Component folder '%s' does not match any artifact (not banned, not initernal) in the inventory.", licenseComponent));
                }
            }
        }

        // check whether component folder is empty
        for (String component : componentsFromInventory) {
            final File componentFolder = new File(new File(baseDir), component);
            final String[] files = scanForFiles(componentFolder.getPath());
            if (files.length == 0) {
                LOG.warn(String.format("Component folder '%s' does not contain any license or notice files.", component));
            }
        }

        // check whether component exists in inventory
        for (String component : componentsFromInventory) {
            // create the appropriate folders for licenses
            if (createComponentFolders) {
                final File componentFolder = new File(new File(baseDir), component);
                componentFolder.mkdirs();
            }
        }

        // create the appropriate folders for licenses
        for (String component : componentsFromInventory) {
            // check whether component exists in inventory
            if (createComponentFolders) {
                final File componentFolder = new File(new File(baseDir), component);
                componentFolder.mkdirs();
            }
        }

        // iterate over inventory license notices to check whether these are matching an artifact or not
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            StringBuilder refSb = new StringBuilder();
            refSb.append(licenseMetaData.getComponent()).append("/");
            refSb.append(licenseMetaData.getVersion()).append("/");
            refSb.append(licenseMetaData.getLicense());

            if (!licenseReferenceFromInventory.contains(refSb.toString())) {
                LOG.warn(String.format("License notice '%s' not used in inventory.", refSb.toString()));
            }
        }

        // verify unique match of licenses within one component
        for (Artifact artifact : inventory.getArtifacts()) {
            String license = artifact.getLicense();
            if (StringUtils.isNotBlank(license)) {
                license = license.trim();
                String componentRef = artifact.getName() + "/" + artifact.getVersion();
                for (Artifact candidateArtifact : inventory.getArtifacts()) {
                    String candiateComponentRef = candidateArtifact.getName() + "/" + candidateArtifact.getVersion();
                    if (componentRef.equals(candiateComponentRef)) {
                        String candidateLicense = candidateArtifact.getLicense();
                        if (candidateLicense != null) {
                            candidateLicense = candidateLicense.trim();
                        }
                        if (!license.equals(candidateLicense)) {
                            LOG.warn(String.format("Component '%s' in version '%s' does not have a unique license association: '%s' <> '%s'. See '%s'.",
                                    artifact.getName(), artifact.getVersion(), license, candidateLicense, candidateArtifact.getId()));
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
                LOG.warn(String.format("Version information inconsistent. " +
                        "Mismatch between version and artifact file name. Version '%s' not contained in '%s'.",
                        artifact.getVersion(), id));
            }
        }

        // validate license notice content
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            String notice = licenseMetaData.getNotice();
            if (StringUtils.isBlank(notice)) {
                LOG.warn(String.format("Empty license notice for '%s'.", licenseMetaData.deriveQualifier()));
            }

            validateElement(licenseMetaData, notice, "p");
            validateElement(licenseMetaData, notice, "i");
            validateElement(licenseMetaData, notice, "li");
            validateElement(licenseMetaData, notice, "ol");
            validateElement(licenseMetaData, notice, "ul");
            validateElement(licenseMetaData, notice, "strong");

            validateEvenCount(licenseMetaData, notice, "\"");

            validateNotContained(licenseMetaData, notice, " .</");
           // validateNotContained(licenseMetaData, notice, "> ");
            validateNotContained(licenseMetaData, notice, "IS\"WITHOUT");
        }
    }

    private void validateElement(LicenseMetaData licenseMetaData, String notice, String element) {
        String openElement = "<" + element + ">";
        String closeElement = "</" + element + ">";
        validateOpenClose(licenseMetaData, notice, openElement, closeElement);
    }

    private void validateOpenClose(LicenseMetaData licenseMetaData, String notice, String openElement, String closeElement) {
        if (StringUtils.countMatches(notice, openElement) != StringUtils.countMatches(notice, closeElement)) {
            LOG.warn(String.format("License text '%s': number of '%s' does not match number of '%s'.", licenseMetaData.deriveQualifier(), openElement, closeElement));
        }
    }

    private void validateNotContained(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) > 0) {
            LOG.warn(String.format("License text '%s': suspicious character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
        }
    }

    private void validateEvenCount(LicenseMetaData licenseMetaData, String notice, String text) {
        if (StringUtils.countMatches(notice, text) % 2 != 0) {
            LOG.warn(String.format("License text '%s': expected even count of character sequence '%s'.", licenseMetaData.deriveQualifier(), text));
        }
    }

    private List<String> getSplitLicenses(String license) {
        List<String> licenses = new ArrayList<>();
        if (!StringUtils.isBlank(license)) {
            String[] splitLicense = license.split("\\|");
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
