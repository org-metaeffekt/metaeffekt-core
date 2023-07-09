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

import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link ScanTask} unwrapping {@link Artifact}s (if possible).
 */
public class ArtifactUnwrapTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactUnwrapTask.class);

    public static final String ATTRIBUTE_KEY_UNWRAP = "UNWRAP";

    public static final String HINT_SCAN = "scan";

    public static final String HINT_IGNORE = "ignore";

    public static final String ATTRIBUTE_KEY_SCAN_DIRECTIVE = "SCAN_DIRECTIVE";
    public static final String SCAN_DIRECTIVE_DELETE = "delete";
    public static final String ATTRIBUTE_KEY_ARTIFACT_PATH = "ARTIFACT PATH";

    private Artifact artifact;

    public ArtifactUnwrapTask(Artifact artifact, List<String> assetIdChain) {
        super(assetIdChain);
        this.artifact = artifact;
    }

    @Override
    public void process(FileSystemScanContext fileSystemScanContext) {
        final String path = artifact.get(ATTRIBUTE_KEY_ARTIFACT_PATH);
        final FileRef fileRef = new FileRef(fileSystemScanContext.getBaseDir().getPath() + "/" + path);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing " + getClass().getName() + " on: " + fileRef);
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
        final Optional<Artifact> referenceArtifact =
                referenceArtifacts.stream().filter(a -> StringUtils.isBlank(a.getChecksum())).findFirst();

        // we implicitly try to unwrap if the artifact is not known:
        final boolean implicitUnwrap = !referenceArtifact.isPresent();

        // only include the artifact if the classification does not include HINT_IGNORE
        final boolean explicitUnwrap = referenceArtifact.isPresent() && hasClassification(referenceArtifact.get(), HINT_SCAN);
        final boolean explicitIgnore = referenceArtifact.isPresent() && hasClassification(referenceArtifact.get(), HINT_IGNORE);

        final boolean artifactHasScanClassification = referenceArtifact.isPresent() ? false : artifact.hasClassification(HINT_SCAN);

        if ((implicitUnwrap || explicitUnwrap) && unpackIfPossible(file, targetFolder, artifactHasScanClassification, issues)) {
            // unpack successful...

            boolean markForDelete = false;

            if (implicitUnwrap && explicitIgnore) {
                markForDelete = true;
            }

            if (markForDelete) {
                artifact.set(ATTRIBUTE_KEY_SCAN_DIRECTIVE, SCAN_DIRECTIVE_DELETE);
            } else {
                addChecksum(fileRef);
            }

            // trigger collection of content
            LOG.info("Collecting subtree on {}.", fileRef.getPath());
            final FileRef dirRef = new FileRef(targetFolder);
            fileSystemScanContext.push(new DirectoryScanTask(dirRef,
                    rebuildAndExtendAssetIdChain(fileSystemScanContext.getBaseDir(), dirRef, artifact, fileRef, fileSystemScanContext)));
        } else {
            // compute md5 to support component patterns (candidates for unwrap did not receive a checksum before)
            addChecksum(fileRef);

            // mark artifacts matching a component pattern with anchor checksum
            if (!fileSystemScanContext.getScanParam().getComponentPatternsByChecksum(artifact.getChecksum()).isEmpty()) {
                artifact.set(FileCollectTask.ATTRIBUTE_KEY_ANCHOR, FileCollectTask.ATTRIBUTE_VALUE_MARK);
            }

            // the asset id chain remains as is
        }
    }

    private void addChecksum(FileRef fileRef) {
        final String fileMd5Checksum = FileUtils.computeChecksum(fileRef.getFile());
        artifact.setChecksum(fileMd5Checksum);
    }

    private boolean unpackIfPossible(File file, File targetDir, boolean includeModules, List<String> issues) {
        if (!includeModules) {
            if (file == null || file.getName().toLowerCase().endsWith(".jar")) {
                return false;
            }
        }
        return ArchiveUtils.unpackIfPossible(file, targetDir, issues);
    }

    private boolean hasClassification(Artifact artifact, String classification) {
        if (artifact != null && StringUtils.isNotBlank(artifact.getClassification())) {
            return artifact.getClassification().contains(classification);
        }
        return false;
    }

    private static List<String> rebuildAndExtendAssetIdChain(FileRef baseDir, FileRef dirRef, Artifact artifact, FileRef file, FileSystemScanContext context) {
        // read existing
        final String assetChain = artifact.get("ASSET_ID_CHAIN");

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
        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, assetId);
        assetMetaData.set("Checksum", fileChecksum);
        assetMetaData.set("Source", ArtifactUnwrapTask.class.getName());

        // FIXME: revise name; used not only here
        assetMetaData.set("File Path", relativePath);

        assetMetaData.set("ARTIFACT_PATH", file.getPath());
        context.getInventory().getAssetMetaData().add(assetMetaData);

        context.getPathToAssetIdMap().put(relativePath, assetId);

        return assetIdChain;
    }

}
