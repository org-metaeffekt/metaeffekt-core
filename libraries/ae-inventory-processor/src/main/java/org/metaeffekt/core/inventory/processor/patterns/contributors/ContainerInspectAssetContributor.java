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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.metaeffekt.core.inventory.processor.adapter.ContainerImageInspectAdapter;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ContainerInspectAssetContributor extends ComponentPatternContributor {

    private static final List<String> SUFFIX_LIST = Collections.singletonList(".json");

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".json");
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIX_LIST;
    }

    @Override
    public int getExecutionPhase() {
        return 4;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        try {
            final File inspectFile = new File(baseDir, relativeAnchorPath);

            if (inspectFile.exists()) {

                final ContainerImageInspectAdapter containerImageInspectAdapter = new ContainerImageInspectAdapter();
                final AssetMetaData assetMetaData = containerImageInspectAdapter.deriveAssetMetadata(inspectFile);

                if (assetMetaData != null) {

                    final String name = assetMetaData.get(AssetMetaData.Attribute.NAME);
                    final String version = assetMetaData.get(AssetMetaData.Attribute.VERSION);

                    final ComponentPatternData cpd = new ComponentPatternData();
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);

                    final String repository = assetMetaData.get("Repository");
                    if (repository != null && version != null) {
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, repository + "-" + version);
                    } else {
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, inspectFile.getName());
                    }

                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, inspectFile.getName());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, inspectFile.getName());
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_CONTAINER);

                    cpd.set(assetMetaData.get(AssetMetaData.Attribute.ASSET_ID), Constants.MARKER_CROSS);

                    cpd.set("Container Image - Id", assetMetaData.get(ContainerImageInspectAdapter.KEY_IMAGE_ID));

                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "container-inspect");

                    cpd.setExpansionInventorySupplier(() -> createAssetInventory(assetMetaData, relativeAnchorPath));

                    return Collections.singletonList(cpd);
                }
             }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    private Inventory createAssetInventory(AssetMetaData assetMetaData, String relativeAnchorPath) {
        // extend asset metadata
        assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), relativeAnchorPath);

        final Inventory inventory = new Inventory();
        inventory.getAssetMetaData().add(assetMetaData);

        return inventory;

    }

}