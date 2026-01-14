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
package org.metaeffekt.core.inventory.processor.filescan.tasks;

import org.apache.commons.lang3.StringUtils;
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

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.ATTRIBUTE_KEY_ARTIFACT_PATH;
import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.ATTRIBUTE_KEY_ASSET_ID_CHAIN;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.ROOT_PATHS;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_PATH_IN_ASSET;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

/**
 * Contributes to the inventory managed by {@link FileSystemScanContext}.
 */
public class FileCollectTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(FileCollectTask.class);

    public static final String ATTRIBUTE_KEY_UNWRAP = "UNWRAP";

    public static final String ATTRIBUTE_KEY_ANCHOR = "ANCHOR";

    private FileRef fileRef;

    public FileCollectTask(FileRef fileRef, List<String> assetIdChain) {
        super(assetIdChain);
        this.fileRef = fileRef;
    }

    @Override
    public void process(FileSystemScanContext fileSystemScanContext) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing [{}] on [{}].", getClass().getName(), fileRef.getFile().getAbsolutePath());
        }

        final File file = fileRef.getFile();

        // ignore empty files
        if (file.length() == 0) return;

        // FIXME: how to deal with symlinks (ignore?; only include target)

        final String fileName = file.getName();
        final String filePath = fileRef.getPath();

        final Artifact artifact = new Artifact();
        artifact.setId(fileName);
        final String relativePath = asRelativePath(fileSystemScanContext.getBaseDir().getPath(), filePath);
        artifact.set(ATTRIBUTE_KEY_ARTIFACT_PATH, relativePath);
        artifact.set(KEY_PATH_IN_ASSET, relativePath);

        // evaluate conditions for unwrapping the file (file is not yet probed; any file may be subject to unwrapping)
        final boolean unwrap = fileSystemScanContext.getScanParam().isImplicitUnwrap() &&
                fileSystemScanContext.getScanParam().unwraps(filePath);

        if (unwrap) {
            // mark for unwrap
            artifact.set(ATTRIBUTE_KEY_UNWRAP, Constants.MARKER_CROSS);
            artifact.set(KEY_PATH_IN_ASSET, relativePath);
        } else {
            // compute SHA hashes
            artifact.set(Constants.KEY_HASH_SHA1, FileUtils.computeSHA1Hash(file));
            artifact.set(Constants.KEY_HASH_SHA256, FileUtils.computeSHA256Hash(file));
            artifact.set(Constants.KEY_HASH_SHA512, FileUtils.computeSHA512Hash(file));

            // compute md5 to support component patterns
            final String fileMd5Checksum = FileUtils.computeChecksum(file);

            // check whether the file should be added to the inventory
            artifact.setChecksum(fileMd5Checksum);

            // mark artifacts matching a component pattern with anchor checksum
            if (!fileSystemScanContext.getScanParam().getComponentPatternsByChecksum(fileMd5Checksum).isEmpty()) {
                artifact.set(ATTRIBUTE_KEY_ANCHOR, Constants.MARKER_CROSS);
            }
        }

        attachAssetIdChain(artifact, fileSystemScanContext);

        attachEmbeddedPath(artifact, relativePath);

        artifact.set(ROOT_PATHS, relativePath);

        fileSystemScanContext.contribute(artifact);
    }

    private static void attachEmbeddedPath(Artifact artifact, String value) {
        String assetChainId = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);
        if (StringUtils.isNotBlank(assetChainId)) {
            artifact.set(KEY_PATH_IN_ASSET, value);
        }
    }

    private void attachAssetIdChain(Artifact artifact, FileSystemScanContext fileSystemScanContext) {
        final List<String> assetIdChain = getAssetIdChain();
        if (assetIdChain != null && !assetIdChain.isEmpty()) {
            final String assetIdChainString = String.join("|\n", assetIdChain);
            artifact.set(ATTRIBUTE_KEY_ASSET_ID_CHAIN, assetIdChainString);

            for (String assetPath : assetIdChain) {
                String assetId = fileSystemScanContext.getPathToAssetIdMap().get(assetPath);
                if (assetId != null) {
                    artifact.set(assetId, Constants.MARKER_CONTAINS);
                }
            }
        }
    }
}
