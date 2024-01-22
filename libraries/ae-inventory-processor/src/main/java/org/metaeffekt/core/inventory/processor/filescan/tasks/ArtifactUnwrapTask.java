/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.AssetMetaData.Attribute.ASSET_ID;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_CHECKSUM;
import static org.metaeffekt.core.util.ArchiveUtils.unpackIfPossible;

/**
 * {@link ScanTask} unwrapping {@link Artifact}s (if possible).
 */
public class ArtifactUnwrapTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactUnwrapTask.class);

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

        final boolean explicitInclude = referenceArtifact.isPresent() &&
                referenceArtifact.get().hasClassification(HINT_INCLUDE);

        final boolean explicitExclude = !explicitInclude && referenceArtifact.isPresent() &&
                (referenceArtifact.get().hasClassification(HINT_EXCLUDE) || referenceArtifact.get().hasClassification(HINT_IGNORE));

        // read the scan classification on artifact level (created by FileSystemScanExecutor inspecting content)
        final boolean artifactWithScanClassification = artifact.hasClassification(HINT_SCAN);

        // we implicitly try to unwrap if the artifact is not known and ends with jar; FIXME: include also other suffixes, make configurable
        final boolean implicitUnwrap = artifactWithScanClassification ||
                (!referenceArtifact.isPresent() &&
                        !file.getName().toLowerCase().endsWith(".jar") &&
                        !file.getName().toLowerCase().endsWith(".xar"));

        if (!explicitNoUnrwap && (implicitUnwrap || explicitUnrwap) && unpackIfPossible(file, targetFolder, issues)) {
            // unpack successful...

            final boolean implicitExclude = !explicitInclude && !explicitUnrwap && !explicitNoUnrwap;

            boolean markForDelete = false;

            if (explicitExclude || implicitExclude) {
                if (implicitExclude) {
                    // apply implicit exclusion only for sub-artifacts
                    final FileRef parentPath = new FileRef(fileRef.getFile().getParentFile().getAbsolutePath());
                    if (!parentPath.getPath().equals(fileSystemScanContext.getBaseDir().getPath())) {
                        LOG.info("Excluding archive [{}] from resulting artifacts. Classified as intermediate archive.", artifact.getId());
                        markForDelete = true;
                    }
                } else {
                    LOG.info("Excluding archive [{}] from resulting artifacts. Explicitly classified for exclusion.", artifact.getId());
                    markForDelete = true;
                }
            }

            if (markForDelete) {
                artifact.set(ATTRIBUTE_KEY_SCAN_DIRECTIVE, SCAN_DIRECTIVE_DELETE);
            } else {
                addChecksumsAndHashes(fileRef);
            }

            // trigger collection of content
            LOG.info("Collecting subtree on [{}].", fileRef.getPath());
            final FileRef dirRef = new FileRef(targetFolder);
            fileSystemScanContext.push(new DirectoryScanTask(dirRef,
                    rebuildAndExtendAssetIdChain(fileSystemScanContext.getBaseDir(), artifact, fileRef, fileSystemScanContext)));
        } else {
            // compute md5 to support component patterns (candidates for unwrap did not receive a checksum before)
            addChecksumsAndHashes(fileRef);

            // mark artifacts matching a component pattern with anchor checksum
            if (!fileSystemScanContext.getScanParam().getComponentPatternsByChecksum(artifact.getChecksum()).isEmpty()) {
                artifact.set(FileCollectTask.ATTRIBUTE_KEY_ANCHOR, Constants.MARKER_CROSS);
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

    private void addChecksumsAndHashes(FileRef fileRef) {
        final File file = fileRef.getFile();
        artifact.setChecksum(FileUtils.computeChecksum(file));
        artifact.set(Constants.KEY_HASH_SHA256, FileUtils.computeSHA256Hash(file));
    }

    private static List<String> rebuildAndExtendAssetIdChain(FileRef baseDir, Artifact artifact,
                                                             FileRef file, FileSystemScanContext context) {
        // read existing
        final String assetChain = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);

        // decompose in list
        final List<String> assetIdChain = new ArrayList<>();
        if (StringUtils.isNotBlank(assetChain)) {
            final String[] split = assetChain.split("\\|\n");
            Collections.addAll(assetIdChain, split);
        }

        final String relativePath = FileUtils.asRelativePath(baseDir.getFile(), file.getFile());
        assetIdChain.add(relativePath);

        String fileChecksum = artifact.getChecksum();
        if (!StringUtils.isNotBlank(fileChecksum)) {
            fileChecksum = FileUtils.computeChecksum(file.getFile());
        }

        final String assetId = "AID-" + artifact.getId() + "-" + fileChecksum;

        final AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(ASSET_ID, assetId);
        assetMetaData.set(KEY_CHECKSUM, fileChecksum);
        assetMetaData.set(ATTRIBUTE_KEY_INSPECTION_SOURCE, ArtifactUnwrapTask.class.getName());

        assetMetaData.set(ATTRIBUTE_KEY_ASSET_PATH, relativePath);
        assetMetaData.set(ATTRIBUTE_KEY_ARTIFACT_PATH, relativePath);

        context.getInventory().getAssetMetaData().add(assetMetaData);

        context.getPathToAssetIdMap().put(relativePath, assetId);

        return assetIdChain;
    }

}
