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
package org.metaeffekt.core.inventory.processor.report.adapter;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AssetReportAdapter {

    private final Inventory inventory;

    public AssetReportAdapter(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<AssetMetaData> listAssets() {
        final List<AssetMetaData> assetMetaData = new ArrayList<>(inventory.getAssetMetaData());
        assetMetaData.sort(Comparator.comparing(o -> ObjectUtils.firstNonNull(o.get(AssetMetaData.Attribute.NAME), o.get(AssetMetaData.Attribute.ASSET_ID), "").toLowerCase()));
        return assetMetaData;
    }

    public List<Artifact> getRelatedArtifacts(AssetMetaData assetMetaData) {
        return new ArrayList<>(InventoryUtils.getArtifactsForAsset(inventory, assetMetaData));
    }

    final Pair<String, String>[] containerKeyList = new Pair[]{
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Repository", "Repository"),
            Pair.of("Tag", "Tag"),
            Pair.of("Size", "Size"),
            Pair.of("Operating System", "Os"),
            Pair.of("Architecture", "Architecture"),
            Pair.of("Created", "Created"),
            Pair.of("Image Id", "Image Id"),
            Pair.of("Image Digest", "Digest"),
            Pair.of("Supplier", "Supplier"),
    };

    final Pair<String, String>[] applianceKeyList = new Pair[]{
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Tag", "Machine Tag"),
            Pair.of("Snapshot Timestamp", "Snapshot Timestamp")
    };

    final Pair<String, String>[] defaultKeyList = new Pair[]{
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Version", "Version"),
            Pair.of("Checksum (MD5)", "Checksum (MD5)"),
            Pair.of("Hash (SHA-256)", "Hash (SHA-256)")
    };

    final Pair<String, String>[] directoryKeyList = new Pair[]{
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
    };

    public Pair<String, String>[] listKeys(AssetMetaData assetMetaData) {
        final String type = assetMetaData.get(Constants.KEY_TYPE);
        if (!StringUtils.isEmpty(type)) {
            switch (type.toLowerCase(Locale.US)) {
                case "container":
                    return containerKeyList;
                case "appliance":
                    return applianceKeyList;
                case "directory":
                    return directoryKeyList;
            }
        }
        return defaultKeyList;
    }
}
