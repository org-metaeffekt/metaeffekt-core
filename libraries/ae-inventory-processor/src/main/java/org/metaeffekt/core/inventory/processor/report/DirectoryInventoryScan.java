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
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.command.PrepareScanDirectoryCommand;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanExecutor;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanParam;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DirectoryInventoryScan {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryInventoryScan.class);
    public static final String[] PATTERN_ARRAY_ALL = {"**/*"};

    private final Inventory referenceInventory;

    private final String[] scanIncludes;

    private final String[] scanExcludes;

    private final String[] postScanExcludes;

    private final String[] unwrapIncludes;

    private final String[] unwrapExcludes;

    private final File inputDirectory;

    private final File scanDirectory;

    @Setter
    private boolean enableImplicitUnpack = true;

    @Setter
    private boolean includeEmbedded = false;

    @Setter
    private boolean enableDetectComponentPatterns = false;

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
          String[] scanIncludes, String[] scanExcludes,
          Inventory referenceInventory) {

        this (inputDirectory, scanDirectory,
            scanIncludes, scanExcludes, PATTERN_ARRAY_ALL, null, null,
            referenceInventory);
    }

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
          String[] scanIncludes, String[] scanExcludes,
          String[] postScanExcludes,
          Inventory referenceInventory) {

        this (inputDirectory, scanDirectory,
            scanIncludes, scanExcludes, PATTERN_ARRAY_ALL, null, postScanExcludes,
            referenceInventory);
    }

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
          String[] scanIncludes, String[] scanExcludes,
          String[] unwrapIncludes, String[] unwrapExcludes,
          Inventory referenceInventory) {

        this(inputDirectory, scanDirectory,
            scanIncludes, scanExcludes, unwrapIncludes, unwrapExcludes, null,
            referenceInventory);
    }

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
          String[] scanIncludes, String[] scanExcludes,
          String[] unwrapIncludes, String[] unwrapExcludes,
          String[] postScanExcludes,
          Inventory referenceInventory) {

        this.inputDirectory = inputDirectory;

        this.scanDirectory = scanDirectory;
        this.scanIncludes = scanIncludes;
        this.scanExcludes = scanExcludes;

        this.unwrapIncludes = unwrapIncludes;
        this.unwrapExcludes = unwrapExcludes;

        this.referenceInventory = referenceInventory;
        this.postScanExcludes = postScanExcludes;
    }

    public Inventory createScanInventory() {
        prepareScanDirectory();

        return performScan();
    }

    private void prepareScanDirectory() {
        // if the input directory is not set; the scan directory is not managed
        if (inputDirectory != null) {
            final PrepareScanDirectoryCommand prepareScanDirectoryCommand = new PrepareScanDirectoryCommand();
            prepareScanDirectoryCommand.prepareScanDirectory(inputDirectory, scanDirectory, scanIncludes, scanExcludes);
        }
    }

    public Inventory performScan() {
        final File directoryToScan = scanDirectory;

        final FileSystemScanParam scanParam = new FileSystemScanParam().
                collectAllMatching(scanIncludes, scanExcludes).
                unwrapAllMatching(unwrapIncludes, unwrapExcludes).
                implicitUnwrap(enableImplicitUnpack).
                includeEmbedded(includeEmbedded).
                detectComponentPatterns(enableDetectComponentPatterns).
                withReferenceInventory(referenceInventory);

        LOG.info("Scanning directory [{}]...", directoryToScan.getAbsolutePath());

        final FileSystemScanContext fileSystemScan = new FileSystemScanContext(new FileRef(directoryToScan), scanParam);
        final FileSystemScanExecutor fileSystemScanExecutor = new FileSystemScanExecutor(fileSystemScan);

        fileSystemScanExecutor.execute();

        // NOTE: at this point, the component is fully unwrapped in the file system (expecting already detected component
        //  patterns).

        LOG.info("Scanning directory [{}] completed.", directoryToScan.getAbsolutePath());

        // post-process inventory; merge asset groups
        mergeAssetGroups(fileSystemScan.getInventory());

        // post-process inventory; remove post scan excludes
        if (postScanExcludes != null) {
            final String[] normalizedExcludePatterns = FileUtils.normalizePatterns(postScanExcludes);
            final List<Artifact> artifacts = fileSystemScan.getInventory().getArtifacts();
            artifacts.removeIf(a -> {
                String pathInAsset = a.getPathInAsset();

                // filename with fallback to id
                if (!pathInAsset.endsWith(a.getId())) {
                    if (pathInAsset.equals(".")) {
                        pathInAsset = a.getId();
                    } else {
                        pathInAsset += "/" + a.getId();
                    }
                }

                final String normalizedPath = FileUtils.normalizePathToLinux(pathInAsset);

                for (String postScanExclude : normalizedExcludePatterns) {
                    if (FileUtils.matches(postScanExclude, normalizedPath)) {
                        LOG.info("Removing artifact [{}] due to post scan exclude [{}].", a.getId(), postScanExclude);
                        return true;
                    }
                }
                return false;
            });
        }

        return fileSystemScan.getInventory();
    }

    private static void mergeAssetGroups(Inventory inventory) {
        final Set<String> containerGroupTypes = new HashSet<>();
        containerGroupTypes.add(Constants.ARTIFACT_TYPE_ARCHIVE);
        containerGroupTypes.add(Constants.ARTIFACT_TYPE_CONTAINER);
        containerGroupTypes.add(Constants.ARTIFACT_TYPE_DISTRO);

        // collect merge groups
        final Map<String, List<AssetMetaData>> mergeGroups = new HashMap<>();
        for (AssetMetaData assetMetaData : inventory.getAssetMetaData()) {
            if (containerGroupTypes.contains(assetMetaData.get(Constants.KEY_TYPE))) {
                String relativePath = assetMetaData.get(AssetMetaData.Attribute.ASSET_PATH.getKey());
                if (relativePath != null) {
                    // the group-common denominator is the modulated asset path
                    String path = relativePath;
                    path = path.replace("/" + ArtifactUnwrapTask.CONTAINER_AGGREGATION_FOLDER, "");
                    path = path.replace("[", "");
                    path = path.replace("]", "");
                    path = path.replace(".tar", "");
                    path = path.replace(".TAR", "");
                    path = path.replace(".json", "");
                    path = path.replace(".JSON", "");
                    mergeGroups.computeIfAbsent(path, (a) -> new ArrayList<>()).add(assetMetaData);
                }
            }
        }

        // list to collect the assetId for removal
        final Set<String> coveredAssetIds = new HashSet<>();

        final Set<String> primaryAssetIds = new HashSet<>();

        // process groups
        for (List<AssetMetaData> group : mergeGroups.values()) {

            if (group.size() == 1) continue;

            // find representative in group; in this case the first item claiming to be a container
            final Optional<AssetMetaData> first = group.stream()
                    .filter(a -> Constants.ARTIFACT_TYPE_CONTAINER.equalsIgnoreCase(a.get(Constants.KEY_TYPE)))
                    .findFirst();

            if (first.isPresent()) {
                final AssetMetaData primaryAssetMetaData = first.get();
                final String primaryAssetId = primaryAssetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

                primaryAssetIds.add(primaryAssetId);

                final List<String> assetPaths = new ArrayList<>();

                // further process group
                for (AssetMetaData assetMetaData : group) {
                    if (assetMetaData != primaryAssetMetaData) {
                        // merge covered assets
                        primaryAssetMetaData.merge(assetMetaData);
                        inventory.getAssetMetaData().remove(assetMetaData);

                        // track covered assetIds for removal
                        final String otherAssetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

                        // only add to coveredAssetIds if the id does not match the primaryAssetId
                        coveredAssetIds.add(otherAssetId);
                    }

                    // merge relative paths (in the end, we have a complete set; despite with which asset we started)
                    final String relativePath = assetMetaData.get(AssetMetaData.Attribute.ASSET_PATH.getKey());
                    if (relativePath != null) {
                        // ArtifactUnwrapTask.CONTAINER_AGGREGATION_FOLDER is an artificial sub-folder
                        final String path = relativePath.replace("/" + ArtifactUnwrapTask.CONTAINER_AGGREGATION_FOLDER, "");
                        if (!assetPaths.contains(path)) {
                            assetPaths.add(path);
                        }
                    }
                }

                // combine collected assetPaths
                primaryAssetMetaData.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), assetPaths.stream().sorted().collect(Collectors.joining(", ")));

                // consolidate asset relationships
                final List<Artifact> artifactsToBeDeleted = new ArrayList<>();
                for (Artifact artifact : inventory.getArtifacts()) {
                    for (String assetId : coveredAssetIds) {
                        final String assetAssociation = artifact.get(assetId);
                        if (assetAssociation != null) {
                            if (assetAssociation.equals(Constants.MARKER_CROSS)) {
                                artifactsToBeDeleted.add(artifact);
                            } else if (assetAssociation.endsWith(Constants.MARKER_CONTAINS)) {
                                // do not overwrite if already set; a previous process may know better
                                final String primaryAssetAssociation = artifact.get(primaryAssetId);
                                if (StringUtils.isBlank(primaryAssetAssociation)) {
                                    artifact.set(primaryAssetId, assetAssociation);
                                }
                            }
                        }
                    }
                }

                // remove artifacts covered
                inventory.getArtifacts().removeAll(artifactsToBeDeleted);
            }

            // remove the columns for covered/resolved assets
            for (String assetId : coveredAssetIds) {
                // only remove if not a primaryAssetId
                if (!primaryAssetIds.contains(assetId)) {
                    // to remove the obsolete assetId we remove the according attribute from all artifacts
                    InventoryUtils.removeArtifactAttribute(assetId, inventory);
                }
            }
        }
    }

}
