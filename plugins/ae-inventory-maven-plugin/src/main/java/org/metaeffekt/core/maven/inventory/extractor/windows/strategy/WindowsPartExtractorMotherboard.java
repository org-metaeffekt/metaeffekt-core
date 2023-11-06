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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.StringJoiner;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_BaseBoard;

public class WindowsPartExtractorMotherboard extends WindowsPartExtractorBase {

    public void parse(Inventory inventory, JSONObject baseBoardJson, JSONObject motherboardJson) {
        final String pnpDeviceID = getJsonFieldValue(motherboardJson, "PNPDeviceID");
        final String serialNumber = getJsonFieldValue(baseBoardJson, "SerialNumber");
        final String product = getJsonFieldValue(baseBoardJson, "Product");

        final Artifact motherboardArtifact = findArtifactOrElseAppendNew(inventory,
                artifact -> {
                    if (artifact.isDriver()) {
                        return false;
                    }
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

        mapBaseJsonInformationToInventory(baseBoardJson, motherboardArtifact);
        motherboardArtifact.set("Windows Source", Class_Win32_BaseBoard.getTypeName());

        final Pair<String, String> constructedIdComponent = constructArtifactIdComponent(baseBoardJson);
        if (constructedIdComponent != null) {
            motherboardArtifact.setId(constructedIdComponent.getLeft());
            motherboardArtifact.set("Component", constructedIdComponent.getRight());
        }

        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, Artifact.Attribute.VERSION, "Version");

        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "Organisation", "Manufacturer");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "Product", "Product");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "Description", "Description", "Caption", "Name", "Tag");
        mapJsonFieldToInventory(baseBoardJson, motherboardArtifact, "SerialNumber", "SerialNumber");

        {
            final StringJoiner busTypeJoiner = new StringJoiner(", ");

            final String primaryBusType = getJsonFieldValue(motherboardJson, "PrimaryBusType");
            final String secondaryBusType = getJsonFieldValue(motherboardJson, "SecondaryBusType");
            if (primaryBusType != null) busTypeJoiner.add(primaryBusType);
            if (secondaryBusType != null) busTypeJoiner.add(secondaryBusType);

            if (busTypeJoiner.length() > 0) motherboardArtifact.set("BusType", busTypeJoiner.toString());
        }

        mapJsonFieldToInventory(motherboardJson, motherboardArtifact, "PNPDeviceID", "PNPDeviceID");

        if (motherboardArtifact.getAttributes().isEmpty()) {
            inventory.getArtifacts().remove(motherboardArtifact);
        }
    }

    private Pair<String, String> constructArtifactIdComponent(JSONObject baseBoardJson) {
        // take fields ("Description"|"Caption"|"Name"|"Tag") + "Product" + "Version"
        final String description = getJsonFieldValue(baseBoardJson, "Description", "Caption", "Name", "Tag");
        final String product = getJsonFieldValue(baseBoardJson, "Product");
        final String version = getJsonFieldValue(baseBoardJson, "Version");

        final StringJoiner idJoiner = new StringJoiner(" ");
        final StringJoiner componentJoiner = new StringJoiner(" ");

        if (description != null) {
            idJoiner.add(description);
            componentJoiner.add(description);
        }
        if (product != null) {
            idJoiner.add(product);
            componentJoiner.add(product);
        }
        if (version != null) {
            idJoiner.add(version);
        }

        if (idJoiner.length() > 0) {
            return Pair.of(idJoiner.toString(), componentJoiner.toString());
        } else {
            return null;
        }
    }
}
