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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_VideoController;

public class WindowsPartExtractorVideoController extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorVideoController.class);

    public void parse(Inventory inventory, JSONArray videoControllerJson) {
        for (int i = 0; i < videoControllerJson.length(); i++) {
            final JSONObject videoController = videoControllerJson.getJSONObject(i);
            parse(inventory, videoController);
        }
    }

    public void parse(Inventory inventory, JSONObject videoControllerJson) {
        final String pnpDeviceID = getJsonFieldValue(videoControllerJson, "PNPDeviceID");
        final String product = getJsonFieldValue(videoControllerJson, "Name", "Caption", "Description");

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

        mapBaseJsonInformationToInventory(videoControllerJson, videoControllerArtifact);
        videoControllerArtifact.set("Windows Source", Class_Win32_VideoController.getTypeName());

        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, Artifact.Attribute.VERSION, "DriverVersion", "SpecificationVersion", "Version");
        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, Artifact.Attribute.COMPONENT, "VideoProcessor", "Name", "Caption", "Description");

        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "AdapterCompatibility", "AdapterCompatibility");
        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "InstalledDisplayDrivers", "InstalledDisplayDrivers");
        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "VideoModeDescription", "VideoModeDescription");
        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "InfFilename", "InfFilename");
        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "DriverDate", "DriverDate");

        mapJsonFieldToInventory(videoControllerJson, videoControllerArtifact, "PNPDeviceID", "PNPDeviceID");

        if (videoControllerArtifact.getAttributes().isEmpty()) {
            inventory.getArtifacts().remove(videoControllerArtifact);
        }
    }
}
