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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_InstalledStoreProgram;
import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_SoftwareElement;

public class WindowsPartExtractorInstalledProduct extends WindowsPartExtractorBase {

    public void parse(Inventory inventory, JSONArray win32ProductJson, WindowsExtractorAnalysisFile type) {
        for (int i = 0; i < win32ProductJson.length(); i++) {
            final JSONObject productJson = win32ProductJson.getJSONObject(i);

            final Artifact artifact = new Artifact();

            mapBaseJsonInformationToInventory(productJson, artifact);

            mapJsonFieldToInventory(productJson, artifact, Artifact.Attribute.ID,
                    type == Class_Win32_InstalledStoreProgram ? "Program Id" : null,
                    "PackageName", "Name");
            if (type == Class_Win32_InstalledStoreProgram || type == Class_Win32_SoftwareElement) {
                mapJsonFieldToInventory(productJson, artifact, Artifact.Attribute.COMPONENT, "Name");
            }
            mapJsonFieldToInventory(productJson, artifact, Artifact.Attribute.VERSION, "Version");
            mapJsonFieldToInventory(productJson, artifact, Constants.KEY_ORGANIZATION, "Vendor", "Manufacturer");
            mapJsonFieldToInventory(productJson, artifact, "Description", "Description", "Caption", "Name");

            // identification
            mapJsonFieldToInventory(productJson, artifact, "Msi Package Code", "Msi Package Code"); // Win32_InstalledWin32Program
            mapJsonFieldToInventory(productJson, artifact, "Msi Product Code", "Msi Product Code"); // Win32_InstalledWin32Program
            mapJsonFieldToInventory(productJson, artifact, "Program Id", "Program Id"); // Win32_InstalledWin32Program, Win32_InstalledStoreProgram
            mapJsonFieldToInventory(productJson, artifact, "SoftwareElementID", "SoftwareElementID"); // Win32_SoftwareElement
            mapJsonFieldToInventory(productJson, artifact, "IdentificationCode", "IdentificationCode"); // Win32_SoftwareElement
            mapJsonFieldToInventory(productJson, artifact, "SerialNumber", "SerialNumber"); // Win32_SoftwareElement

            // references
            mapJsonFieldToInventory(productJson, artifact, Artifact.Attribute.URL, "URLInfoAbout", "HelpLink", "URLUpdateInfo"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "URLInfoAbout", "URLInfoAbout"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "HelpLink", "HelpLink"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "URLUpdateInfo", "URLUpdateInfo"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "HelpTelephone", "HelpTelephone"); // Win32_Product

            // paths
            mapJsonFieldToInventory(productJson, artifact, "Path",
                    "Path", // Win32_SoftwareElement
                    "InstallLocation"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "InstallLocation", "InstallLocation"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "InstallSource", "InstallSource"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "LocalPackage", "LocalPackage"); // Win32_Product

            mapJsonFieldToInventory(productJson, artifact, "InstallDate", "InstallDate"); // Win32_Product, Win32_SoftwareElement
            mapJsonFieldToInventory(productJson, artifact, "IdentifyingNumber", "IdentifyingNumber"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "InstallState", "InstallState"); // Win32_Product, Win32_SoftwareElement
            mapJsonFieldToInventory(productJson, artifact, "Registered Owner", "RegOwner"); // Win32_Product

            // other
            mapJsonFieldToInventory(productJson, artifact, "SKUNumber", "SKUNumber"); // Win32_Product
            mapJsonFieldToInventory(productJson, artifact, "WordCount", "WordCount"); // Win32_Product

            if (!artifact.getAttributes().isEmpty()) {
                artifact.set("Windows Source", type.getTypeName());
                inventory.getArtifacts().add(artifact);
            }
        }
    }
}
