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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerAssetContributor extends ComponentPatternContributor {

    private static List<String> PATHS = Collections.singletonList("manifest.json");

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/manifest.json");
    }

    @Override
    public List<String> getSuffixes() {
        return PATHS;
    }

    @Override
    public int getExecutionPhase() {
        return 4;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        try {
            final File manifestFile = new File(baseDir, relativeAnchorPath);

            if (manifestFile.exists()) {
                final String manifestContent = FileUtils.readFileToString(manifestFile, FileUtils.ENCODING_UTF_8);
                final JSONArray manifestJson = new JSONArray(manifestContent);
                final JSONObject jsonObject = manifestJson.getJSONObject(0);

                // isolate repo tags; pick single from list (de-prioritize latest tags)
                final JSONArray repoTags = jsonObject.getJSONArray("RepoTags");
                final List<Object> repoTagList = repoTags.toList();
                final List<String> repoTagStringList = repoTagList.stream().map(String::valueOf).collect(Collectors.toList());
                Optional<String> first = repoTagStringList.stream().filter(rt -> !rt.endsWith("latest")).sorted().findFirst();
                if (!first.isPresent()) {
                    first = repoTagStringList.stream().sorted().findFirst();
                }

                // isolate primary config file; the hash is the container id
                final String configPath = jsonObject.optString("Config");
                final String containerImageId = configPath.substring(configPath.lastIndexOf("/") + 1);

                // isolate last image hash
                final JSONArray layers = jsonObject.getJSONArray("Layers");
                final List<Object> layerList = layers.toList();
                if (layerList.size() > 0) {
                    String lastImageHash = String.valueOf(layerList.get(layerList.size() - 1));
                    lastImageHash = lastImageHash.substring(lastImageHash.lastIndexOf("/") + 1);

                    String name = lastImageHash;
                    String version = lastImageHash;
                    if (first.isPresent()) {
                        String repoTag = first.get();
                        final int colonIndex = repoTag.lastIndexOf(":");
                        if (colonIndex != -1) {
                            name = repoTag.substring(0, colonIndex);
                            version = repoTag.substring(colonIndex + 1);
                        }
                    }

                    final ComponentPatternData cpd = new ComponentPatternData();
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, name + "-" + version);
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "*");
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_CONTAINER);

                    final String containerAssetId = "CID-" + containerImageId;

                    cpd.set("Image Id", containerImageId);
                    cpd.set(containerAssetId, Constants.MARKER_CROSS);

                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "saved-container");

                    cpd.setExpansionInventorySupplier(() -> createAssetInventory(containerAssetId, cpd, baseDir, manifestFile));

                    return Collections.singletonList(cpd);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    private Inventory createAssetInventory(String containerAssetId, ComponentPatternData cpd, File baseDir, File manifestFile) {

        AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_CONTAINER);
        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, containerAssetId);
        assetMetaData.set(AssetMetaData.Attribute.NAME, cpd.get(ComponentPatternData.Attribute.COMPONENT_NAME));
        assetMetaData.set(AssetMetaData.Attribute.VERSION, cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION));

        assetMetaData.set("Image Id", cpd.get("Image Id"));

        assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), FileUtils.asRelativePath(baseDir, manifestFile.getParentFile()));

        Inventory inventory = new Inventory();
        inventory.getAssetMetaData().add(assetMetaData);

        return inventory;

    }

}