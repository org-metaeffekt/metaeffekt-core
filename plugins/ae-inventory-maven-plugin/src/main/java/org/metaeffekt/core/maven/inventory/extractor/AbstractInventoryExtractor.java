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
package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * The {@link org.metaeffekt.core.maven.inventory.mojo.ContainerInventoryExtractionMojo} expects
 * information of an appliance or container being available in an analysis folder. The
 * content depends on the distribution an the extractor scripts.
 * <p>
 * Different implementations of {@link AbstractInventoryExtractor} support the different outputs.
 */
public abstract class AbstractInventoryExtractor implements InventoryExtractor {

    public static final String FOLDER_USR_SHARE_DOC = "usr-share-doc";
    public static final String FOLDER_USR_SHARE_LICENSE = "usr-share-licenses";

    /**
     * Anticipates a directory for each package in packageDir. The directory contains the
     * package name only (no other attribute is derived).
     *
     * @param analysisDir The analysis dir.
     * @param packageDir The specific (one out of potentially many) packageDocDir.
     * @param idToPackageInfoMap The resulting {@link PackageInfo} instances are added to the map.
     * @param docDir The documentation dir.
     */
    protected static void packagesFromDocumentationDir(File analysisDir, File packageDir, Map<String,
            PackageInfo> idToPackageInfoMap, boolean docDir) {
        if (packageDir.exists()) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(packageDir);
            scanner.setIncludes(new String[]{"*"});
            scanner.scan();
            for (String path : scanner.getIncludedDirectories()) {

                // check whether information already exists
                PackageInfo packageInfo = idToPackageInfoMap.get(path);
                if (packageInfo == null) {
                    packageInfo = new PackageInfo();

                    // path may include version; at this stage we do not differentiate
                    packageInfo.name = path;
                    packageInfo.id = path;
                    packageInfo.component = path;
                }
                if (docDir) {
                    packageInfo.documentationDir = new File(packageDir, path).getAbsolutePath();
                } else {
                    packageInfo.licenseDir = new File(packageDir, path).getAbsolutePath();
                }
                registerPackageInfo(packageInfo, idToPackageInfoMap);
            }
        }
    }

    protected static void registerPackageInfo(PackageInfo packageInfo, Map<String, PackageInfo> idToPackageInfoMap) {
        idToPackageInfoMap.put(packageInfo.id, packageInfo);
        idToPackageInfoMap.put(packageInfo.name, packageInfo);
    }

    public Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException {
        final String issue = extractIssue(analysisDir);

        // use specific inventory implementation to extract inventory
        final Inventory inventory = new Inventory();

        extendInventory(analysisDir, inventory);

        extendNotCoveredFiles(analysisDir, inventory, excludePatterns);

        // FIXME: combine with asset information
        for (final Artifact artifact : inventory.getArtifacts()) {
            artifact.set(KEY_SOURCE_PROJECT, inventoryId);
            // TODO extract container id
            // artifact.set(KEY_ATTRIBUTE_CONTAINER, analysisDir.getName());
            artifact.set(KEY_ISSUE, issue);
        }

        return inventory;
    }

    private void extendNotCoveredFiles(File analysisDir, Inventory inventory, List<String> excludePatterns) throws IOException {
        if (excludePatterns.contains("**/*")) return;
        final List<String> notCoveredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
        for (String fileName : notCoveredFiles) {
            Artifact artifact = new Artifact();
            File file = new File(fileName);
            artifact.setId(file.getName());
            // file.getPath() is the absolute path in the container
            artifact.addProject(file.getPath().substring(1));
            artifact.set(KEY_TYPE, ARTIFACT_TYPE_FILE);
            inventory.getArtifacts().add(artifact);
        }
    }

    protected abstract void extendInventory(File analysisDir, Inventory inventory) throws IOException;

    protected String extractIssue(File analysisDir) throws IOException {
        String issue = FileUtils.readFileToString(new File(analysisDir, "issue.txt"), "UTF-8");
        issue = issue.replace("Welcome to ", "");
        issue = issue.replace("Kernel \\r on an \\m (\\l)", "");
        issue = issue.replace("\\S\nKernel \\r on an \\m", "");
        issue = issue.replace(" \\n \\l", "");
        issue = issue.replace(" - Kernel %r (%t).", "");
        issue = issue.trim();
        String release = FileUtils.readFileToString(new File(analysisDir, "release.txt"), "UTF-8");
        if (!issue.contains(release)) {
            issue = issue + " (" + release.trim() + ")";
        }
        issue = issue.trim();
        return issue;
    }

    protected void addOrMerge(File analysisDir, Inventory inventory, PackageInfo p) {
        Artifact derivedFromPackage = p.createArtifact(analysisDir);
        Artifact referenceArtifact = inventory.findArtifact(derivedFromPackage.getId());
        if (versionEquals(derivedFromPackage, referenceArtifact)) {
            // already added --> merge
            referenceArtifact.merge(derivedFromPackage);
        } else {
            derivedFromPackage.set(KEY_TYPE, ARTIFACT_TYPE_PACKAGE);
            inventory.getArtifacts().add(derivedFromPackage);
        }
    }

    private boolean versionEquals(Artifact derivedFromPackage, Artifact referenceArtifact) {
        if (derivedFromPackage != null && derivedFromPackage.getVersion() != null && referenceArtifact != null) {
            return derivedFromPackage.getVersion().equals(referenceArtifact.getVersion());
        }
        return false;
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
        // here the common aspects are validated
        validateFileExists(new File(analysisDir, "filesystem/files.txt"));
        validateFileHasContent(new File(analysisDir, "uname.txt"));
    }

    private void validateFileExists(File file) {
        if (!file.exists()) {
            throw new IllegalStateException(
                    String.format("File %s expected, but does not exists.", file.getPath()));
        }
    }

    private void validateFileHasContent(File file) {
        validateFileExists(file);
        try {
            String content = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8);
            if (content == null || content.trim().length() == 0) {
                throw new IllegalStateException(
                        String.format("File %s does not contain any content.", file.getPath()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Unable to reed file %s.", file.getPath()));
        }
    }

}
