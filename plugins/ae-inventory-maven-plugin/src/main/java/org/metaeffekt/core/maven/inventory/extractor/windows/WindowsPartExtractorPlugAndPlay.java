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

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ArtifactType;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WindowsPartExtractorPlugAndPlay extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorPlugAndPlay.class);

    public void parse(Inventory inventory, JSONArray pnpEntityJson, JSONArray getPnpDeviceJson, JSONArray pnpSignedDriverJson) {
        // pnpEntityJson:    DeviceID | PNPDeviceID
        // getPnpDeviceJson: InstanceId | DeviceID | PNPDeviceID
        // pnpSignedDriver:  DeviceID

        final Map<String, Map<JSONArray, JSONObject>> pnpDeviceMap = new LinkedHashMap<>();

        groupPnpDevicesById(pnpDeviceMap, pnpSignedDriverJson, new HashSet<>(Collections.singletonList("DeviceID")), "No DeviceID found for PNP Signed Driver: {}");
        groupPnpDevicesById(pnpDeviceMap, pnpEntityJson, new HashSet<>(Arrays.asList("PNPDeviceID", "DeviceID")), "No PNPDeviceID or DeviceID found for PNP Entity: {}");
        groupPnpDevicesById(pnpDeviceMap, getPnpDeviceJson, new HashSet<>(Arrays.asList("PNPDeviceID", "DeviceID", "InstanceId")), "No PNPDeviceID, DeviceID or InstanceId found for PNP Device: {}");

        final Map<String, String> unknownDeviceClasses = new HashMap<>();
        final List<Artifact> pnpArtifacts = new ArrayList<>();

        for (Map.Entry<String, Map<JSONArray, JSONObject>> deviceDataEntry : pnpDeviceMap.entrySet()) {
            final Map<JSONArray, JSONObject> deviceData = deviceDataEntry.getValue();
            final JSONObject pnpEntity = deviceData.get(pnpEntityJson);
            final JSONObject getPnpDevice = deviceData.get(getPnpDeviceJson);
            final JSONObject pnpSignedDriver = deviceData.get(pnpSignedDriverJson);

            final String pnpDeviceId = deviceDataEntry.getKey();

            // merge pnpEntity into effectiveJson first, then getPnpDevice on top
            final JSONObject effectiveJson = new JSONObject();
            if (pnpSignedDriver != null) {
                pnpSignedDriver.keySet().forEach(key -> effectiveJson.put(key, pnpSignedDriver.get(key)));
            }
            if (pnpEntity != null) {
                pnpEntity.keySet().forEach(key -> effectiveJson.put(key, pnpEntity.get(key)));
            }
            if (getPnpDevice != null) {
                getPnpDevice.keySet().forEach(key -> effectiveJson.put(key, getPnpDevice.get(key)));
            }

            final Artifact artifact = new Artifact();
            pnpArtifacts.add(artifact);
            mapBaseJsonInformationToInventory(effectiveJson, artifact);
            artifact.set("Plug and Play ID", pnpDeviceId);
            artifact.set("WMI Class", "Win32_PnPEntity");

            artifact.set(Artifact.Attribute.ID, constructEffectivePnpDeviceId(effectiveJson));

            mapJsonFieldToInventory(effectiveJson, artifact, Artifact.Attribute.VERSION, "DriverVersion", "DriverDate");
            mapJsonFieldToInventory(effectiveJson, artifact, "Description", "Description");
            mapJsonFieldToInventory(effectiveJson, artifact, "Organisation", "Manufacturer", "DriverProviderName");

            mapJsonFieldToInventory(effectiveJson, artifact, "DeviceID", "DeviceID");
            mapJsonFieldToInventory(effectiveJson, artifact, "PNPDeviceID", "PNPDeviceID");
            mapJsonFieldToInventory(effectiveJson, artifact, "InstanceId", "InstanceId");

            // according to the documentation on https://learn.microsoft.com/en-us/windows/win32/cimwin32prov/win32-pnpentity
            // the PNPClass field is not always or never available, but in the dataset I got, it was always available.
            // luckily, the ClassGuid field is always available and contains a UUID representing the PNPClass.
            // lookups happen in the PnpClassGuid enum.
            mapJsonFieldToInventory(effectiveJson, artifact, "PNPClass", "PNPClass", "Class", "DeviceClass");
            mapJsonFieldToInventory(effectiveJson, artifact, "ClassGuid", "ClassGuid");
            final String pnpClass = artifact.get("PNPClass");
            final String pnpClassGuid = artifact.get("ClassGuid");
            final WindowsInventoryExtractor.PnpClassGuid pnpClassId = WindowsInventoryExtractor.PnpClassGuid.fromPnpClass(pnpClass);
            final WindowsInventoryExtractor.PnpClassGuid classGuidId = WindowsInventoryExtractor.PnpClassGuid.fromClassGuid(pnpClassGuid);
            if (pnpClassId != null && classGuidId != null) {
                if (pnpClassId != classGuidId) {
                    LOG.warn("PNPClass and ClassGuid do not match for PNP Device, picking from ClassGuid, PNPClass: {}, ClassGuid: {}", pnpClassId, classGuidId);
                }
                artifact.set(Artifact.Attribute.TYPE, classGuidId.getArtifactType().getCategory());
            } else if (pnpClassId != null) {
                artifact.set(Artifact.Attribute.TYPE, pnpClassId.getArtifactType().getCategory());
            } else if (classGuidId != null) {
                artifact.set(Artifact.Attribute.TYPE, classGuidId.getArtifactType().getCategory());
            } else {
                if (pnpClassGuid != null || pnpClass != null) {
                    unknownDeviceClasses.put(pnpClassGuid, ObjectUtils.firstNonNull(pnpClass, pnpClassGuid));
                }
            }

            // clear type if it is UNKNOWN
            if (ArtifactType.UNKNOWN.getCategory().equals(artifact.get(Artifact.Attribute.TYPE))) {
                artifact.set(Artifact.Attribute.TYPE, null);
            }

            mapJsonFieldToInventory(effectiveJson, artifact, "PNP Service", "Service");
            mapJsonFieldToInventory(effectiveJson, artifact, "PNP Availability", "Availability");
            mapJsonFieldToInventory(effectiveJson, artifact, "PNP ConfigManagerUserConfig", "ConfigManagerUserConfig");

            // pnp signed driver
            mapJsonFieldToInventory(effectiveJson, artifact, "Driver Location", "Location");
            mapJsonFieldToInventory(effectiveJson, artifact, "Signer", "Signer");
            mapJsonFieldToInventory(effectiveJson, artifact, "DriverName", "DriverName");
            mapJsonFieldToInventory(effectiveJson, artifact, "InfName", "InfName");
        }

        // find artifacts with identical 'Id' fields
        /*final Map<String, List<Artifact>> artifactMap = pnpArtifacts.stream().collect(Collectors.groupingBy(artifact -> artifact.get(Artifact.Attribute.ID)));
        for (Map.Entry<String, List<Artifact>> entry : artifactMap.entrySet()) {
            final List<Artifact> artifacts = entry.getValue();
            if (artifacts.size() > 1) {
                for (int i = 0; i < artifacts.size(); i++) {
                    final Artifact artifact = artifacts.get(i);
                    // final String deviceIdString = ObjectUtils.firstNonNull(artifact.get("DeviceID"), artifact.get("PNPDeviceID"), artifact.get("InstanceId"));
                    artifact.set(Artifact.Attribute.ID, artifact.get(Artifact.Attribute.ID) + " (" + (i + 1) + ")");
                }
            }
        }*/

        inventory.getArtifacts().addAll(pnpArtifacts);

        if (!unknownDeviceClasses.isEmpty()) {
            LOG.warn("[{}] unknown PNP Device Classes found, using [{}] as type", unknownDeviceClasses.size(), ArtifactType.CATEGORY_HARDWARE.getCategory());
            unknownDeviceClasses.forEach((classGuid, pnpClass) -> LOG.warn("  {}: {}", classGuid, pnpClass));
        }
    }

    private String constructEffectivePnpDeviceId(JSONObject effectiveJson) {
        final String baseId = getJsonFieldValue(effectiveJson, "FriendlyName", "Caption", "Name", "DeviceID", "InstanceId", "PNPDeviceID");

        // if the baseId is (a drive name E:\ or C:\) then build a replacement Id from the device information as follows:
        //    (Manufacturer | DriverProviderName) (Description) (Unique Identifier from DeviceID)
        //    example: "JetFlash Transcend 16GB 02NO6LGO2Q1BGUIP"
        if (baseId == null || (baseId.length() == 3 && Character.isLetter(baseId.charAt(0)) && baseId.charAt(1) == ':' && baseId.charAt(2) == '\\')) {
            final String manufacturer = getJsonFieldValue(effectiveJson, "Manufacturer", "DriverProviderName");
            final String description = getJsonFieldValue(effectiveJson, "Description");

            if (manufacturer != null && description != null) {
                return manufacturer.trim() + " " + description.trim();
            }

            final String deviceId = getJsonFieldValue(effectiveJson, "DeviceID", "PNPDeviceID", "InstanceId");
            final String uniqueIdentifier = deviceId == null ? null : constructReplacementPnpNamePartFromDeviceId(deviceId);

            if (manufacturer != null && uniqueIdentifier != null) {
                return manufacturer.trim() + " " + uniqueIdentifier;
            } else if (description != null && uniqueIdentifier != null) {
                return description.trim() + " " + uniqueIdentifier;
            } else if (baseId == null && uniqueIdentifier != null) {
                return uniqueIdentifier;
            } else {
                return baseId;
            }
        }

        // USB\VID_v(4)&PID_d(4)&REV_r(4)
        // STORAGE\VOLUME\_??_USBSTOR#DISK&VEN_KINGSTON&PROD_DATATRAVELER_3.0&REV_PMAP#08606E6D401FBFB1C70AAD7C&0#{53F56307-B6BF-11D0-94F2-00A0C91EFB8B}
        // STORAGE\VOLUME\{9BCB055E-111E-11EE-9DF8-806E6F6E6963}#0000000007500000
        if (baseId.equals("Volume") || baseId.equals("New_Volume")) {
            final String deviceId = getJsonFieldValue(effectiveJson, "DeviceID", "PNPDeviceID", "InstanceId");

            if (deviceId != null) {
                final int vendorIndex = deviceId.indexOf("&VEN_");
                final String vendor;
                if (vendorIndex != -1) {
                    final int endVendorIndex = deviceId.indexOf("&", vendorIndex + 5);
                    vendor = deviceId.substring(vendorIndex + 5, endVendorIndex == -1 ? deviceId.length() : endVendorIndex);
                } else {
                    vendor = null;
                }

                final int productIndex = deviceId.indexOf("&PROD_");
                final String product;
                if (productIndex != -1) {
                    final int endProductIndex = deviceId.indexOf("&", productIndex + 6);
                    product = deviceId.substring(productIndex + 6, endProductIndex == -1 ? deviceId.length() : endProductIndex);
                } else {
                    product = null;
                }

                final int guidStart = deviceId.indexOf("{");
                final int guidEnd = deviceId.indexOf("}#", guidStart) != -1 ? deviceId.length() : deviceId.indexOf("}", guidStart);
                final String guid;
                if (guidStart != -1 && guidEnd != -1) {
                    guid = deviceId.substring(guidStart + 1, guidEnd);
                } else {
                    guid = null;
                }

                if (vendor != null && product != null) {
                    return baseId + " " + vendor + " " + product;
                } else if (guid != null) {
                    if (vendor != null) {
                        return baseId + " " + vendor + " " + guid;
                    } else if (product != null) {
                        return baseId + " " + product + " " + guid;
                    } else {
                        return baseId + " " + guid;
                    }
                }
            }
        }

        return baseId;
    }

    private String constructReplacementPnpNamePartFromDeviceId(String deviceId) {
        // SWD\WPDBUSENUM\_??_USBSTOR#DISK&VEN_JETFLASH&PROD_TRANSCEND_16GB&REV_1100#02NO6LGO2Q1BGUIP&0#{53F56307-B6BF-11D0-94F2-00A0C91EFB8B}
        // 02NO6LGO2Q1BGUIP&0
        final String hashPart = deviceId.replaceAll(".*#([^#]+)#.*", "$1");
        if (!hashPart.equals(deviceId)) return hashPart;

        // SWD\WPDBUSENUM\{A46D2BCC-2AC2-11EE-9E0A-84699329049B}#0000000000100000
        // {A46D2BCC-2AC2-11EE-9E0A-84699329049B}
        final String guidPart = deviceId.replaceAll(".*\\\\([^\\\\#]+).*", "$1");
        if (!guidPart.equals(deviceId)) return guidPart;

        return deviceId;
    }

    public void groupPnpDevicesById(Map<String, Map<JSONArray, JSONObject>> groupedEntryMap, JSONArray jsonArray, Set<String> keys, String noIdFoundLogMessage) {
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject jsonObject = jsonArray.getJSONObject(i);
            final String id = findFirstNonNull(jsonObject, keys);
            if (id != null) {
                groupedEntryMap.computeIfAbsent(id, k -> new HashMap<>()).put(jsonArray, jsonObject);
            } else {
                LOG.warn(noIdFoundLogMessage, jsonObject);
            }
        }
    }
}
