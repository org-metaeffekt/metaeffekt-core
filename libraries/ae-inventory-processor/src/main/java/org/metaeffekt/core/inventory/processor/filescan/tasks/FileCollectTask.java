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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contributes to the inventory managed by {@link FileSystemScanContext}.
 */
public class FileCollectTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(FileCollectTask.class);

    public static final String ATTRIBUTE_KEY_UNWRAP = "UNWRAP";
    public static final String ATTRIBUTE_KEY_ANCHOR = "ANCHOR";
    public static final String ATTRIBUTE_VALUE_MARK = "x";

    private FileRef fileRef;

    public FileCollectTask(FileRef fileRef, List<String> assetIdChain) {
        super(assetIdChain);
        this.fileRef = fileRef;
    }

    @Override
    public void process(FileSystemScanContext fileSystemScanContext) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing " + getClass().getName() + " on: " + fileRef);
        }

        final File file = fileRef.getFile();
        final String fileName = file.getName();
        final String filePath = fileRef.getPath();

        final Artifact artifact = new Artifact();
        artifact.setId(fileName);
        artifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH, FileUtils.asRelativePath(fileSystemScanContext.getBaseDir().getPath(), filePath));

        // evaluate conditions for unwrapping the file (file is not yet probed; any file may be subject to unwrapping)
        final boolean unwrap = fileSystemScanContext.getScanParam().isImplicitUnwrap() &&
                fileSystemScanContext.getScanParam().unwraps(filePath);

        if (unwrap) {
            // mark for unwrap
            artifact.set(ATTRIBUTE_KEY_UNWRAP, ATTRIBUTE_VALUE_MARK);
        } else {
            // compute SHA hashes
            artifact.set(Constants.KEY_HASH_SHA1, FileUtils.computeSHA1Hash(file));
            artifact.set(Constants.KEY_HASH_SHA256, FileUtils.computeSHA256Hash(file));

            // compute md5 to support component patterns
            final String fileMd5Checksum = FileUtils.computeChecksum(file);

            // check whether the file should be added to the inventory
            artifact.setChecksum(fileMd5Checksum);

            // mark artifacts matching a component pattern with anchor checksum
            if (!fileSystemScanContext.getScanParam().getComponentPatternsByChecksum(fileMd5Checksum).isEmpty()) {
                artifact.set(ATTRIBUTE_KEY_ANCHOR, ATTRIBUTE_VALUE_MARK);
            }


        }

        attachAssetIdChain(artifact, fileSystemScanContext);

        fileSystemScanContext.contribute(artifact);
    }

    private void attachAssetIdChain(Artifact artifact, FileSystemScanContext fileSystemScanContext) {
        final List<String> assetIdChain = getAssetIdChain();
        if (assetIdChain != null && !assetIdChain.isEmpty()) {
            String assetIdChainString = assetIdChain.stream().collect(Collectors.joining("|\n"));
            artifact.set("ASSET_ID_CHAIN", assetIdChainString);

            for (String assetPath : assetIdChain) {
                String assetId = fileSystemScanContext.getPathToAssetIdMap().get(assetPath);
                if (assetId != null) {
                    artifact.set(assetId, "x");
                }
            }
        }
    }

}
