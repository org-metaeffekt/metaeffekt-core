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
package org.metaeffekt.core.maven.inventory.extractor.windows;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsInventoryExtractor.WindowsAnalysisFiles.Class_Win32_BaseBoard;

public class WindowsPartExtractorMotherboard extends WindowsPartExtractorBase {

    public void parse(Inventory inventory, JSONObject baseBoardJson, JSONObject motherboardJson) {
        final String pnpDeviceID = getJsonFieldValue(motherboardJson, "PNPDeviceID");
        final String serialNumber = getJsonFieldValue(baseBoardJson, "SerialNumber");
        final String product = getJsonFieldValue(baseBoardJson, "Product");

        final Artifact motherboardPnpDriverArtifact = findArtifactOrElseAppendNew(inventory,
                artifact -> {
                    if (pnpDeviceID != null) {
                        return StringUtils.equals(artifact.get("PNPDeviceID"), pnpDeviceID);
                    } else if (serialNumber != null) {
                        return StringUtils.equals(artifact.get("SerialNumber"), serialNumber);
                    } else if (product != null) {
                        return StringUtils.equals(artifact.getId(), product);
                    } else {
                        return false;
                    }
                });

        if (motherboardPnpDriverArtifact.getId() != null) {
            motherboardPnpDriverArtifact.setId(motherboardPnpDriverArtifact.getId() + " Driver"); // TODO: move this into the pnp driver extractor
        }

        final Artifact motherboardArtifact = new Artifact();

        mapBaseJsonInformationToInventory(baseBoardJson, motherboardArtifact);
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, Artifact.Attribute.ID, "Product");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, Artifact.Attribute.VERSION, "Version");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "Organisation", "Manufacturer");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "SerialNumber", "SerialNumber");

        mapJsonFieldToInventory(motherboardJson, motherboardArtifact, "PNPDeviceID", "PNPDeviceID");

        if (motherboardArtifact.getAttributes().isEmpty()) {
            inventory.getArtifacts().remove(motherboardArtifact);
        }

        motherboardArtifact.set("WMI Class", Class_Win32_BaseBoard.getTypeName());
    }
}
