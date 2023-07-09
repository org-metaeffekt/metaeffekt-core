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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;

import java.io.File;

import static org.metaeffekt.core.util.FileUtils.asRelativePath;

public class MatchResult {
    public ComponentPatternData componentPatternData;
    public File anchorFile;
    public File baseDir;

    public String assetIdChain;

    public MatchResult(ComponentPatternData componentPatternData, File anchorFile, File baseDir, String assetIdChain) {
        this.componentPatternData = componentPatternData;
        this.anchorFile = anchorFile;
        this.baseDir = baseDir;
        this.assetIdChain = assetIdChain;
    }

    public Artifact deriveArtifact(FileRef scanBaseDir) {
        final Artifact derivedArtifact = new Artifact();
        derivedArtifact.setId(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_PART));
        derivedArtifact.setComponent(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_NAME));
        derivedArtifact.setVersion(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
        derivedArtifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH, asRelativePath(scanBaseDir.getFile(), baseDir));
        derivedArtifact.set("ASSET_ID_CHAIN", assetIdChain);

        // also take over the type attribute
        derivedArtifact.set(Constants.KEY_TYPE, componentPatternData.get(Constants.KEY_TYPE));

        return derivedArtifact;
    }
}
