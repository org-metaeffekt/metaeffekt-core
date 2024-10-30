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
package org.metaeffekt.core.inventory.processor.filescan;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.Set;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.CLASSIFIER;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VIRTUAL_ROOT_PATH;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

public class MatchResult {

    public final ComponentPatternData componentPatternData;

    public final File anchorFile;
    public final File scanRootDir;
    public final File virtualRootDir;
    public final File versionAnchorRootDir;

    public String assetIdChain;

    public MatchResult(ComponentPatternData componentPatternData, File anchorFile, File scanRootDir, File virtualRootDir, File versionAnchorRootDir, String assetIdChain) {
        this.componentPatternData = componentPatternData;
        this.anchorFile = anchorFile;
        this.scanRootDir = scanRootDir;
        this.virtualRootDir = virtualRootDir;
        this.versionAnchorRootDir = versionAnchorRootDir;
        this.assetIdChain = assetIdChain;
    }

    public Artifact deriveArtifact() {
        final Artifact derivedArtifact = new Artifact();

        derivedArtifact.setId(componentPatternData.get(COMPONENT_PART));
        derivedArtifact.setComponent(componentPatternData.get(COMPONENT_NAME));
        derivedArtifact.setVersion(componentPatternData.get(COMPONENT_VERSION));


        final String relativePath = asRelativePath(scanRootDir.getPath(), FileUtils.normalizePathToLinux(versionAnchorRootDir));
        derivedArtifact.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), relativePath);
        derivedArtifact.set(ATTRIBUTE_KEY_ASSET_ID_CHAIN, assetIdChain);

        // FIXME: this attribute is of no use for component patterns; consider removing this line
        derivedArtifact.set(ATTRIBUTE_KEY_ARTIFACT_PATH, relativePath);
        derivedArtifact.set(Constants.KEY_PATH_IN_ASSET, relativePath);

        // also take over the type attribute
        derivedArtifact.set(Constants.KEY_TYPE, componentPatternData.get(Constants.KEY_TYPE));
        derivedArtifact.set(Constants.KEY_COMPONENT_SOURCE_TYPE, componentPatternData.get(Constants.KEY_COMPONENT_SOURCE_TYPE));

        derivedArtifact.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, componentPatternData.get(Constants.KEY_SPECIFIED_PACKAGE_LICENSE));
        derivedArtifact.set(Constants.KEY_SPECIFIED_PACKAGE_CONCLUDED_LICENSE, componentPatternData.get(Constants.KEY_SPECIFIED_PACKAGE_CONCLUDED_LICENSE));
        derivedArtifact.set(Constants.KEY_SCOPE, componentPatternData.get(Constants.KEY_SCOPE));
        derivedArtifact.set(Artifact.Attribute.URL, componentPatternData.get(Artifact.Attribute.URL.getKey()));
        derivedArtifact.set(Artifact.Attribute.PURL, componentPatternData.get(Artifact.Attribute.PURL.getKey()));

        // also the group id
        derivedArtifact.setGroupId(componentPatternData.get("Group Id"));
        derivedArtifact.setChecksum(componentPatternData.get("Component Checksum"));

        derivedArtifact.set(FileSystemScanConstants.ATTRIBUTE_KEY_COMPONENT_PATTERN_MARKER, Constants.MARKER_CROSS);

        derivedArtifact.set(VIRTUAL_ROOT_PATH, asRelativePath(scanRootDir.getPath(), virtualRootDir.getPath()));

        // take over assetId association
        final Set<String> assetIds = InventoryUtils.collectAssetIdFromGenericElement(componentPatternData);
        for (String assetId : assetIds) {
            final String assetAssociation = componentPatternData.get(assetId);
            if (StringUtils.isNotBlank(assetAssociation)) {
                derivedArtifact.set(assetId, assetAssociation);
            }
        }

        return derivedArtifact;
    }
}
