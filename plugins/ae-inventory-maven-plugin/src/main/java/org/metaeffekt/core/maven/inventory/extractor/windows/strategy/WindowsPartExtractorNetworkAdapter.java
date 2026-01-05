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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_NetworkAdapter;

public class WindowsPartExtractorNetworkAdapter extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorNetworkAdapter.class);

    public void parse(Inventory inventory, JSONArray networkAdapterArrayJson) {
        for (int i = 0; i < networkAdapterArrayJson.length(); i++) {
            final JSONObject networkAdapterJson = networkAdapterArrayJson.getJSONObject(i);
            parse(inventory, networkAdapterJson);
        }
    }

    public void parse(Inventory inventory, JSONObject networkAdapterJson) {
        final String pnpDeviceID = getJsonFieldValue(networkAdapterJson, "PNPDeviceID");
        final String product = getJsonFieldValue(networkAdapterJson, "Name", "Caption", "Description");

        final Artifact videoControllerArtifact = findArtifactOrElseAppendNew(inventory,
                artifact -> {
                    if (pnpDeviceID != null) {
                        return StringUtils.equals(artifact.get("PNPDeviceID"), pnpDeviceID);
                    } else if (product != null) {
                        return StringUtils.equals(artifact.getId(), product);
                    } else {
                        return false;
                    }
                });

        mapBaseJsonInformationToInventory(networkAdapterJson, videoControllerArtifact);
        videoControllerArtifact.set("Windows Source", Class_Win32_NetworkAdapter.getTypeName());

        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, Constants.KEY_ORGANIZATION, "Manufacturer");

        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "MACAddress", "MACAddress");
        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "TimeOfLastReset", "TimeOfLastReset");
        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "NetConnectionID", "NetConnectionID");
        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "AdapterType", "AdapterType");

        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "PNPDeviceID", "PNPDeviceID");

        if (videoControllerArtifact.getAttributes().isEmpty()) {
            inventory.getArtifacts().remove(videoControllerArtifact);
        }
    }
}
