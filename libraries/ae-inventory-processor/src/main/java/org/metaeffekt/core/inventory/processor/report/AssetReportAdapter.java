/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AssetReportAdapter {

    private final Inventory inventory;

    AssetReportAdapter(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<AssetMetaData> listAssets() {

        List<AssetMetaData> assetMetaData = new ArrayList<>(inventory.getAssetMetaData());
        assetMetaData.sort(new Comparator<AssetMetaData>() {
            @Override
            public int compare(AssetMetaData o1, AssetMetaData o2) {
                return o1.get("Name").toLowerCase().compareTo(o2.get("Name").toLowerCase());
            }
        });
        return assetMetaData;
    }

    Pair<String, String>[] containerKeyList = new Pair[] {
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Repository", "Repository"),
            Pair.of("Tag", "Version"),
            Pair.of("Size", "Size"),
            Pair.of("Operating System", "Os"),
            Pair.of("Architecture", "Architecture"),
            Pair.of("Created", "Created"),
            Pair.of("Image Id / Hash", "Image Id"),
    };

    Pair<String, String>[] applianceKeyList = new Pair[] {
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Tag", "Machine Tag"),
            Pair.of("Snapshot Timestamp", "Snapshot Timestamp")
    };

    Pair<String, String>[] defaultKeyList = new Pair[] {
            Pair.of("Type", "Type"),
            Pair.of("Name", "Name"),
            Pair.of("Version", "Version")
    };

    public Pair<String, String>[] listKeys(AssetMetaData assetMetaData) {
        switch (assetMetaData.get("Type")) {
            case "Container":
                return containerKeyList;
            case "Appliance":
                return applianceKeyList;
        }
        return defaultKeyList;
    }
}
