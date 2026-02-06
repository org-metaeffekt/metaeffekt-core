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
package org.metaeffekt.core.inventory.processor.report;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

@Slf4j
public class AnnexResourceProcessor {
    private final Inventory inventory;
    private final Inventory referenceInventory;
    private final ReportConfigurationParameters configParams;

    private final File referenceInventoryDir;
    private final String referenceComponentPath;
    private final String referenceLicensePath;

    private final File targetComponentDir;
    private final File targetLicenseDir;

    public AnnexResourceProcessor(Inventory inventory, Inventory referenceInventory,
                                  ReportConfigurationParameters configParams,
                                  File referenceInventoryDir, String referenceComponentPath, String referenceLicensePath,
                                  File targetComponentDir, File targetLicenseDir) {
        this.inventory = inventory;
        this.referenceInventory = referenceInventory;
        this.configParams = configParams;
        this.referenceInventoryDir = referenceInventoryDir;
        this.referenceComponentPath = referenceComponentPath;
        this.referenceLicensePath = referenceLicensePath;
        this.targetComponentDir = targetComponentDir;
        this.targetLicenseDir = targetLicenseDir;
    }

    public boolean execute() {
        // Enrich the local inventory with reference data
        enrichInventory();

        // Perform folder creation
        final Set<String> reportedSourceFolders = new HashSet<>();
        boolean missingFiles = false;

        for (Artifact artifact : inventory.getArtifacts()) {
            if (!artifact.isEnabledForDistribution()) {
                continue;
            }

            final String componentName = artifact.getComponent();
            final String sourceLicense = artifact.getLicense();

            if (StringUtils.isBlank(sourceLicense) || StringUtils.isBlank(componentName)) {
                continue;
            }

            final String version = artifact.getVersion();
            boolean isArtifactVersionWildcard = isWildcard(version) ||
                    STRING_TRUE.equalsIgnoreCase(artifact.get(Constants.KEY_WILDCARD_MATCH));

            boolean isUndefinedVersion = version == null;

            // Resolve license metadata from the now-enriched inventory
            final LicenseMetaData matchingLicenseMetaData = inventory.
                    findMatchingLicenseMetaData(componentName, sourceLicense, version);

            String effectiveLicense = artifact.getLicense();
            if (matchingLicenseMetaData != null) {
                effectiveLicense = matchingLicenseMetaData.deriveLicenseInEffect();
            }
            // Normalize license string for folder splitting
            effectiveLicense = effectiveLicense.replaceAll("\\s*,\\s*", "|");

            final String versionUnspecificComponentFolder = LicenseMetaData.deriveComponentFolderName(componentName);
            final String versionSpecificComponentFolder = LicenseMetaData.deriveComponentFolderName(componentName, version);

            final String sourcePath = (isArtifactVersionWildcard || isUndefinedVersion) ?
                    versionUnspecificComponentFolder : versionSpecificComponentFolder;

            final String targetPath = (isArtifactVersionWildcard || isUndefinedVersion) ?
                    versionUnspecificComponentFolder : versionSpecificComponentFolder;

            // Copy logic
            if (targetComponentDir != null) {
                missingFiles |= checkAndCopyComponentFolder(sourcePath,
                        new File(targetComponentDir, targetPath), reportedSourceFolders);
            }

            if (targetLicenseDir != null) {
                for (String licenseInEffect : effectiveLicense.split("\\|")) {
                    final String licenseFolderName = LicenseMetaData.deriveLicenseFolderName(licenseInEffect);
                    File licenseTargetDir = new File(targetLicenseDir, licenseFolderName);

                    missingFiles |= checkAndCopyLicenseFolder(licenseFolderName,
                            licenseTargetDir, reportedSourceFolders);

                    missingFiles |= checkAndCopyComponentFolder(sourcePath,
                            new File(licenseTargetDir, targetPath), reportedSourceFolders);
                }
            }
        }
        return !missingFiles;
    }

    private void enrichInventory() {
        if (referenceInventory != null) {
            log.debug("Enriching inventory with reference inventory data.");
            inventory.inheritLicenseMetaData(referenceInventory, false);
            inventory.filterLicenseMetaData();
            inventory.inheritLicenseData(referenceInventory, false);
        }
    }

    private boolean isWildcard(String version) {
        return ASTERISK.equalsIgnoreCase(version) ||
                (version != null && version.startsWith(VERSION_PLACHOLDER_PREFIX) && version.endsWith(VERSION_PLACHOLDER_SUFFIX));
    }

    private boolean checkAndCopyLicenseFolder(String folderName, File targetDir, Set<String> reported) {
        File sourceDir = new File(referenceInventoryDir, referenceLicensePath);
        return performCopy(sourceDir, folderName, targetDir, reported, configParams.isFailOnMissingLicenseFile(), "[missing license file]");
    }

    private boolean checkAndCopyComponentFolder(String folderName, File targetDir, Set<String> reported) {
        File sourceDir = new File(referenceInventoryDir, referenceComponentPath);
        return performCopy(sourceDir, folderName, targetDir, reported, configParams.isFailOnMissingComponentFiles(), "[missing component specific license file]");
    }

    private boolean performCopy(File sourceRootDir, String folderName, File targetDir, Set<String> reported, boolean failFlag, String errorMsg) {
        File sourceFolder = new File(sourceRootDir, folderName);
        if (sourceFolder.exists()) {
            copyFolderContent(sourceRootDir, folderName, targetDir);
            return false;
        } else {
            if (!reported.contains(folderName)) {
                if (failFlag) {
                    log.error("{} in folder [{}]", errorMsg, folderName);
                } else if (reported.size() <= 10) {
                    log.warn("{} in folder [{}]", errorMsg, folderName);
                }
                reported.add(folderName);
                return true;
            }
            return false;
        }
    }

    private void copyFolderContent(File sourceRootDir, String sourcePath, File targetDir) {
        log.info("copied {} from {} to {}", sourceRootDir, sourcePath, targetDir);
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
    }
}