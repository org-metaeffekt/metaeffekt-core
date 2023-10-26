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

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ArtifactType;
import org.metaeffekt.core.inventory.processor.model.Inventory;

public class WindowsPartExtractorBios extends WindowsPartExtractorBase {

    public void parse(Inventory inventory, JSONObject biosJson) {
        final Artifact artifact = new Artifact();

        mapBaseJsonInformationToInventory(biosJson, artifact);
        mapJsonFieldToInventory(biosJson, artifact, Artifact.Attribute.ID, "Version");

        // SMBIOSBIOSVersion: BIOS version as reported by SMBIOS.
        //   This value comes from the BIOS Version member of the BIOS Information structure in the SMBIOS information.
        //   The systeminfo command provides the same version in "BIOS Version".
        //   The "Version" field is used as fallback.
        mapJsonFieldToInventory(biosJson, artifact, "Version", "SMBIOSBIOSVersion", "Version");

        mapJsonFieldToInventory(biosJson, artifact, "Organisation", "Manufacturer");
        mapJsonFieldToInventory(biosJson, artifact, "SerialNumber", "SerialNumber");
        mapJsonFieldToInventory(biosJson, artifact, "ReleaseDate", "ReleaseDate");

        if (isNumberFieldPresent(biosJson, "SMBIOSMajorVersion") && isNumberFieldPresent(biosJson, "SMBIOSMinorVersion")) {
            artifact.set("SMBIOS Version", biosJson.getInt("SMBIOSMajorVersion") + "." + biosJson.getInt("SMBIOSMinorVersion"));
        }
        if (isNumberFieldPresent(biosJson, "EmbeddedControllerMajorVersion") && isNumberFieldPresent(biosJson, "EmbeddedControllerMinorVersion")) {
            artifact.set("BIOS Embedded Controller Version", biosJson.getInt("EmbeddedControllerMajorVersion") + "." + biosJson.getInt("EmbeddedControllerMinorVersion"));
        }
        if (isNumberFieldPresent(biosJson, "SystemBiosMajorVersion") && isNumberFieldPresent(biosJson, "SystemBiosMinorVersion")) {
            artifact.set("BIOS System Version", biosJson.getInt("SystemBiosMajorVersion") + "." + biosJson.getInt("SystemBiosMinorVersion"));
        }

        if (!artifact.getAttributes().isEmpty()) {
            artifact.set(Artifact.Attribute.TYPE, ArtifactType.BIOS.getCategory());
            inventory.getArtifacts().add(artifact);
        }
    }
}
