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
package org.metaeffekt.core.inventory.processor.filescan.tasks;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.AssetMetaData.Attribute.ASSET_ID;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.util.ArchiveUtils.unpackIfPossible;

/**
 * {@link ScanTask} unwrapping {@link Artifact}s (if possible).
 */
public class ArtifactUnwrapTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactUnwrapTask.class);

    public static final String CONTAINER_AGGREGATION_FOLDER = "[root]";

    private final Artifact artifact;

    public ArtifactUnwrapTask(Artifact artifact, List<String> assetIdChain) {
        super(assetIdChain);
        this.artifact = artifact;
    }

    @Override
    public void process(FileSystemScanContext fileSystemScanContext) {
        final String path = artifact.get(ATTRIBUTE_KEY_ARTIFACT_PATH);

        final FileRef fileRef = new FileRef(fileSystemScanContext.getBaseDir().getPath() + "/" + path);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing [{}] on: [{}]", getClass().getName(), fileRef.getFile().getAbsolutePath());
        }

        // unknown or requires expansion
        final File file = fileRef.getFile();

        // unwrap applying simple convention
        final File targetFolder = new File(file.getParentFile(), "[" + file.getName() + "]");

        final List<String> issues = new ArrayList<>();

        // attempt to unwrap

        // clean unwrap indicator
        artifact.set(ATTRIBUTE_KEY_UNWRAP, null);

        // clean target folder (implicitly remove previous unwrapped content)
        FileUtils.deleteDirectoryQuietly(targetFolder);

        final List<Artifact> referenceArtifacts = fileSystemScanContext.getScanParam().
                getReferenceInventory().findAllWithId(file.getName());

        // seek for a matching artifact without checksum (file name matching only)
        final Optional<Artifact> referenceArtifact = referenceArtifacts.stream()
                .filter(a -> StringUtils.isBlank(a.getChecksum())).findFirst();

        // NOTE: this reference artifact must be explicitly matched; no wildcard version is supported (yet)
        // only include the artifact if the classification does not include HINT_IGNORE
        final boolean explicitUnrwap = referenceArtifact.isPresent() && (
                referenceArtifact.get().hasClassification(HINT_SCAN) ||
                referenceArtifact.get().hasClassification(HINT_COMPLEX));

        final boolean explicitNoUnrwap = !explicitUnrwap && referenceArtifact.isPresent() &&
                referenceArtifact.get().hasClassification(HINT_ATOMIC);

        final boolean explicitInclude = (referenceArtifact.isPresent() &&
                referenceArtifact.get().hasClassification(HINT_INCLUDE)) ||
                    file.getName().toLowerCase(Locale.ROOT).endsWith(".exe");

        final boolean explicitExclude = !explicitInclude && referenceArtifact.isPresent() &&
                (referenceArtifact.get().hasClassification(HINT_EXCLUDE) || referenceArtifact.get().hasClassification(HINT_IGNORE));

        // read the scan classification on artifact level (created by FileSystemScanExecutor inspecting content)
        final boolean artifactWithScanClassification = artifact.hasClassification(HINT_SCAN);

        // we implicitly try to unwrap if the artifact is not known and ends with jar; FIXME: include also other suffixes, make configurable
        final boolean implicitUnwrap = artifactWithScanClassification ||
                (!referenceArtifact.isPresent() &&
                        !file.getName().toLowerCase().endsWith(".jar") &&
                        !file.getName().toLowerCase().endsWith(".xar"));

        boolean unpacked = false;

        if (!explicitNoUnrwap && (implicitUnwrap || explicitUnrwap)) {

            if (unpackIfPossible(file, targetFolder, issues)) {
                // unpack successful...
                unpacked = true;

                postProcessUnwrapped(artifact, file, targetFolder, issues);

                final boolean implicitExclude = !explicitInclude && !explicitUnrwap;

                boolean markForDelete = false;

                if (explicitExclude || implicitExclude) {
                    if (implicitExclude) {
                        // apply implicit exclusion only for sub-artifacts
                        final FileRef parentPath = new FileRef(fileRef.getFile().getParentFile().getAbsolutePath());
                        if (!parentPath.getPath().equals(fileSystemScanContext.getBaseDir().getPath())) {
                            LOG.info("Excluding archive [{}] from resulting artifacts. Classified as intermediate archive.", artifact.getId());
                            markForDelete = true;
                        } else {
                            LOG.info("Including archive [{}] in resulting artifacts. Classified as intermediate top-level archive.", artifact.getId());
                        }
                    } else {
                        LOG.info("Excluding archive [{}] from resulting artifacts. Explicitly classified for exclusion.", artifact.getId());
                        markForDelete = true;
                    }
                }

                if (markForDelete) {
                    artifact.set(ATTRIBUTE_KEY_SCAN_DIRECTIVE, SCAN_DIRECTIVE_DELETE);

                    // the original archive file is deleted if the inventory entry is bound to be removed
                    // NOTE: otherwise the collection process will collect both the packed and the unpacked files.
                    FileUtils.deleteQuietly(file);
                } else {
                    addChecksumsAndHashes(fileRef);
                }

                // trigger collection of content
                LOG.info("Collecting subtree on [{}].", targetFolder.getPath());
                final FileRef dirRef = new FileRef(targetFolder);

                // currently we anticipate a virtual context with any unwrapped artifact; except for those marked for deletion (implicit archives)
                fileSystemScanContext.push(new DirectoryScanTask(dirRef,
                        rebuildAndExtendAssetIdChain(fileSystemScanContext.getBaseDir(), artifact, fileRef, fileSystemScanContext, markForDelete)));
            }
        }

        if (!unpacked) {
            // compute md5 to support component patterns (candidates for unwrap did not receive a checksum before)
            addChecksumsAndHashes(fileRef);

            // it is important to correct the classification in case the artifact was not scanned; causes different
            // downstream processing; e.g. certain attributes will not be computed.
            // FIXME: inspection should not modify the classification, but use another attribute
            if (explicitNoUnrwap && referenceArtifact.isPresent()) {
                artifact.setClassification(referenceArtifact.get().getClassification());
            }

            // mark artifacts matching a component pattern with anchor checksum
            if (!fileSystemScanContext.getScanParam().getComponentPatternsByChecksum(artifact.getChecksum()).isEmpty()) {
                artifact.set(FileCollectTask.ATTRIBUTE_KEY_ANCHOR, MARKER_CROSS);
            }

            // the asset id chain remains as is
        }

        // record issues in the artifact that is being processed
        StringJoiner issueJoiner = new StringJoiner(", ");
        if (artifact.get("Errors") != null) {
            issueJoiner.add(artifact.get("Errors"));
        }
        for (String issue : issues) {
            issueJoiner.add(issue);
        }

        String joinedErrors = issueJoiner.toString();
        if (StringUtils.isNotBlank(joinedErrors)) {
            artifact.set("Errors", joinedErrors);
        }
    }

    private void postProcessUnwrapped(Artifact artifact, File file, File targetFolder, List<String> issues) {
        try {
            deriveType(artifact, file);

            // unwrapped items mit aggregate directive skip
            artifact.set(Constants.KEY_AGGREGATE_DIRECTIVE, AGGREGATE_DIRECTIVE_SKIP);

            postProcessUnwrappedSavedContainer(targetFolder);
        } catch (Exception e) {
            issues.add("Detected saved container, but unable to postprocess.");
        }
    }

    private static void deriveType(Artifact artifact, File file) {
        artifact.set(KEY_TYPE, "archive");
        final String extension = FilenameUtils.getExtension(file.getName());
        if (extension != null) {
            artifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, extension + "-archive");
        }
    }

    private void postProcessUnwrappedSavedContainer(File targetFolder) throws IOException {
        final File manifestFile = new File(targetFolder, "manifest.json");

        if (manifestFile.exists()) {
            final String manifestContent = FileUtils.readFileToString(manifestFile, FileUtils.ENCODING_UTF_8);
            final JSONArray manifestJson = new JSONArray(manifestContent);
            final JSONObject jsonObject = manifestJson.getJSONObject(0);
            final JSONArray layers = jsonObject.getJSONArray("Layers");
            final List<Object> layerList = layers.toList();

            // unpack layers in the given order
            for (Object layer : layerList) {
                final File layerFile = new File(targetFolder, String.valueOf(layer));
                final File layerContentDir = new File(targetFolder, CONTAINER_AGGREGATION_FOLDER);
                ArchiveUtils.untar(layerFile, layerContentDir);

                // consume layer file (independent of the location)
                deleteLayerFiles(layerFile);
            }

            // isolate primary config file
            final String configPath = jsonObject.optString("Config");
            final File configFile = new File(targetFolder, String.valueOf(configPath));
            FileUtils.copyFile(configFile, new File(targetFolder, "config.json"));

            // consume blobs dir; we are not expecting any contribution anymore
            final File blobsDir = new File(targetFolder, "blobs");
            FileUtils.deleteDirectoryQuietly(blobsDir);
        }
    }

    private static void deleteLayerFiles(File layerFile) {
        FileUtils.deleteQuietly(layerFile);

        final File versionFile = new File(layerFile.getParent(), "VERSION");
        if (versionFile.exists()) {
            FileUtils.deleteQuietly(versionFile);
        }

        final File jsonFile = new File(layerFile.getParent(), "json");
        if (jsonFile.exists()) {
            FileUtils.deleteQuietly(jsonFile);
        }

    }

    private void addChecksumsAndHashes(FileRef fileRef) {
        final File file = fileRef.getFile();
        artifact.setChecksum(FileUtils.computeChecksum(file));
        artifact.set(Constants.KEY_HASH_SHA256, FileUtils.computeSHA256Hash(file));
        artifact.set(Constants.KEY_HASH_SHA1, FileUtils.computeSHA1Hash(file));
        artifact.set(Constants.KEY_HASH_SHA512, FileUtils.computeSHA512Hash(file));
    }

    private static List<String> rebuildAndExtendAssetIdChain(FileRef baseDir, Artifact artifact,
                                                             FileRef file, FileSystemScanContext context, boolean markForDeletion) {
        // read existing
        final String assetChain = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);

        // decompose in list
        final List<String> assetIdChain = new ArrayList<>();
        if (StringUtils.isNotBlank(assetChain)) {
            final String[] split = assetChain.split("\\|\n");
            Collections.addAll(assetIdChain, split);
        }

        // return unmodified list in case the artifact is marked for deletion
        if (markForDeletion) return assetIdChain;

        final String relativePath = FileUtils.asRelativePath(baseDir.getPath(), file.getPath());
        assetIdChain.add(relativePath);

        String fileChecksum = artifact.getChecksum();
        if (!StringUtils.isNotBlank(fileChecksum)) {
            fileChecksum = FileUtils.computeChecksum(file.getFile());
        }

        String assetId = artifact.getId() + "-" + fileChecksum;
        if (!assetId.startsWith("CID-")) {
            assetId = "AID-" + assetId;
        }

        final AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(KEY_TYPE, Constants.ARTIFACT_TYPE_ARCHIVE);
        assetMetaData.set(ASSET_ID, assetId);
        assetMetaData.set(KEY_CHECKSUM, fileChecksum);
        assetMetaData.set(ATTRIBUTE_KEY_INSPECTION_SOURCE, ArtifactUnwrapTask.class.getName());

        assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), relativePath);
        assetMetaData.set(ATTRIBUTE_KEY_ARTIFACT_PATH, relativePath);

        context.contribute(assetMetaData);

        context.getPathToAssetIdMap().put(relativePath, assetId);

        artifact.set(assetId, MARKER_CROSS);

        return assetIdChain;
    }

}
