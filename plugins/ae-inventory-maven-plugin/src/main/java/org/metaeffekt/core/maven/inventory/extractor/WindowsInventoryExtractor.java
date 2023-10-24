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
package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_SOURCE_PROJECT;
import static org.metaeffekt.core.maven.inventory.extractor.WindowsInventoryExtractor.WindowsAnalysisFiles.*;

public class WindowsInventoryExtractor implements InventoryExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsInventoryExtractor.class);

    /**
     * Checks if the specified analysis directory contains:
     * <ul>
     *     <li>any WMI class files</li>
     * </ul>
     *
     * @param analysisDir The analysis directory to be checked.
     * @return true if any of the above conditions are true, false otherwise.
     */
    @Override
    public boolean applies(File analysisDir) {
        return Arrays.stream(WindowsAnalysisFiles.values())
                .anyMatch(wmiClass -> new File(analysisDir, wmiClass.getTypeName()).exists());
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
    }

    @Override
    public Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException {
        final Inventory inventory = new Inventory();

        parseBiosInformation(inventory, Class_Win32_Bios.readJsonObjectOrEmpty(analysisDir));

        parseWindowsOperatingSystemInformation(inventory,
                systeminfo.readJsonObjectOrEmpty(analysisDir),
                Class_Win32_OperatingSystem.readJsonObjectOrEmpty(analysisDir),
                OSversion.readLinesOrEmpty(analysisDir),
                Get_ComputerInfo.readJsonObjectOrEmpty(analysisDir)
        );

        parseWin32InstalledProduct(inventory, Class_Win32_Product.readJsonArrayOrEmpty(analysisDir), Class_Win32_Product);
        parseWin32InstalledProduct(inventory, Class_Win32_InstalledWin32Program.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledWin32Program);
        parseWin32InstalledProduct(inventory, Class_Win32_InstalledProgramFramework.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledProgramFramework);
        parseWin32InstalledProduct(inventory, Class_Win32_InstalledStoreProgram.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledStoreProgram);
        parseWin32InstalledProduct(inventory, Class_Win32_SoftwareElement.readJsonArrayOrEmpty(analysisDir), Class_Win32_SoftwareElement);
        // parseWin32InstalledProduct(inventory, Class_Win32_SoftwareFeature.readJsonArrayOrEmpty(analysisDir), Class_Win32_SoftwareFeature);

        parsePlugAndPlayDevices(inventory,
                Class_Win32_PnPEntity.readJsonArrayOrEmpty(analysisDir),
                Get_PnpDevice.readJsonArrayOrEmpty(analysisDir),
                Class_Win32_PnpSignedDriver.readJsonArrayOrEmpty(analysisDir)
        );

        // FIXME: combine with asset information (see AbstractInventoryExtractor#extractInventory)
        for (final Artifact artifact : inventory.getArtifacts()) {
            artifact.set(KEY_SOURCE_PROJECT, inventoryId);
        }

        LOG.info("Windows inventory extraction completed from analysis directory: {}", analysisDir);
        LOG.info("With results:"); // list count by "WMI Class"
        inventory.getArtifacts().stream()
                .collect(Collectors.groupingBy(a -> a.get("WMI Class") == null ? "other" : a.get("WMI Class")))
                .entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue().size()))
                .forEach(e -> LOG.info("  {}: {}", e.getKey(), e.getValue().size()));

        return inventory;
    }

    private void parseWindowsOperatingSystemInformation(Inventory inventory,
                                                        JSONObject systeminfoJson, JSONObject operatingSystemJson,
                                                        List<String> osVersionText, JSONObject getComputerInfoJson) {
        final Artifact windowsOsArtifact = new Artifact();

        mapBaseJsonInformationToInventory(operatingSystemJson, windowsOsArtifact);

        // find Windows version
        final String[] osVersionParts = parseOsVersionParts(osVersionText); // [10, 0, 19044, 0]
        if (osVersionParts != null) {
            windowsOsArtifact.set(Artifact.Attribute.VERSION, String.join(".", osVersionParts));

        } else {
            // final String operatingSystemBuildNumber = operatingSystemJson.optString("BuildNumber", null); // 19044
            final String operatingSystemOsVersion = operatingSystemJson.optString("Version", null); // 10.0.19044

            if (operatingSystemOsVersion != null) {
                windowsOsArtifact.set(Artifact.Attribute.VERSION, operatingSystemOsVersion);

            } else {
                final String systeminfoOsVersion = systeminfoJson.optString("OS Version", null); // 10.0.19044 N/A Build 19044

                if (systeminfoOsVersion != null) {
                    final String[] systeminfoOsVersionParts = systeminfoOsVersion.split(" +");
                    if (systeminfoOsVersionParts.length > 1 && !systeminfoOsVersionParts[0].isEmpty() && Character.isDigit(systeminfoOsVersionParts[0].charAt(0))) {
                        windowsOsArtifact.set(Artifact.Attribute.VERSION, systeminfoOsVersionParts[0]);
                    } else {
                        windowsOsArtifact.set(Artifact.Attribute.VERSION, systeminfoOsVersion);
                    }
                }
            }
        }

        // find hotfixes
        // OsHotFixes[ { HotFixID: "KB5004331" } ]
        final JSONArray getComputerInfoHotfixes = getComputerInfoJson.optJSONArray("OsHotFixes");
        if (getComputerInfoHotfixes != null && !getComputerInfoHotfixes.isEmpty()) {
            final List<String> hotfixes = new ArrayList<>();
            for (int i = 0; i < getComputerInfoHotfixes.length(); i++) {
                final JSONObject hotfixJson = getComputerInfoHotfixes.getJSONObject(i);
                final String hotfixId = hotfixJson.optString("HotFixID", null);
                if (hotfixId != null) {
                    hotfixes.add(hotfixId);
                }
            }
            if (!hotfixes.isEmpty()) {
                windowsOsArtifact.set("MS Knowledge Base ID", String.join(", ", hotfixes));
            }

        } else {
            // "Hotfix(s)": "4 Hotfix(s) Installed.,[01]: KB5004331,[02]: KB5003791,[03]: KB5006670,[04]: KB5005699" --> KB5004331, KB5003791, KB5006670, KB5005699
            final String systeminfoHotfixes = systeminfoJson.optString("Hotfix(s)", null);
            if (systeminfoHotfixes != null) {
                final Pattern hotfixPattern = Pattern.compile("\\[\\d+]: (KB\\d+)");
                final Matcher hotfixMatcher = hotfixPattern.matcher(systeminfoHotfixes);
                if (hotfixMatcher.find()) {
                    final List<String> hotfixes = new ArrayList<>();
                    do {
                        hotfixes.add(hotfixMatcher.group(1));
                    } while (hotfixMatcher.find());
                    windowsOsArtifact.set("MS Knowledge Base ID", String.join(", ", hotfixes));
                }
            }
        }

        // other information
        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, Artifact.Attribute.ID, "OS Name");
        if (!windowsOsArtifact.has(Artifact.Attribute.ID.getKey())) {
            mapJsonFieldToInventory(getComputerInfoJson, windowsOsArtifact, Artifact.Attribute.ID, "WindowsProductName");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "Product ID", "Product ID");
        if (!windowsOsArtifact.has("Product ID")) {
            mapJsonFieldToInventory(getComputerInfoJson, windowsOsArtifact, "Product ID", "WindowsProductId", "OsSerialNumber");
        }
        if (!windowsOsArtifact.has("Product ID")) {
            mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, "Product ID", "SerialNumber");
        }

        mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, Constants.KEY_ARCHITECTURE, "OSArchitecture");
        if (!windowsOsArtifact.has(Constants.KEY_ARCHITECTURE)) {
            mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, Constants.KEY_ARCHITECTURE, "OS Architecture", "System Type");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "Organisation", "OS Manufacturer");

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "Registered Owner", "Registered Owner", "Registered Organization");
        if (!windowsOsArtifact.has("Registered Owner")) {
            mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, "Registered Owner", "RegisteredUser");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "SerialNumber", "Product ID");
        if (!windowsOsArtifact.has("SerialNumber")) {
            mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, "SerialNumber", "SerialNumber");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "System Directory", "System Directory");
        if (!windowsOsArtifact.has("System Directory")) {
            mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, "System Directory", "SystemDirectory");
        }
        if (!windowsOsArtifact.has("System Directory")) {
            mapJsonFieldToInventory(getComputerInfoJson, windowsOsArtifact, "System Directory", "OsSystemDirectory");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "Boot Device", "Boot Device");
        if (!windowsOsArtifact.has("Boot Device")) {
            mapJsonFieldToInventory(operatingSystemJson, windowsOsArtifact, "Boot Device", "BootDevice");
        }
        if (!windowsOsArtifact.has("Boot Device")) {
            mapJsonFieldToInventory(getComputerInfoJson, windowsOsArtifact, "Boot Device", "OsBootDevice");
        }

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "InstallDate", "Original Install Date");

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, "System Manufacturer", "System Manufacturer");

        if (!windowsOsArtifact.getAttributes().isEmpty()) {
            windowsOsArtifact.set(Artifact.Attribute.TYPE, ArtifactType.OPERATING_SYSTEM.getCategory());
            inventory.getArtifacts().add(windowsOsArtifact);
        }
    }

    /**
     * Parses the OS version parts from the given list of strings.<br>
     * The method attempts to find a line that starts with a decimal and check if the line before starts with "-" and the line before that with "Major".<br>
     * If this condition is met, then the line is split using one or more spaces as delimiters and the resulting parts are returned.<br>
     * If the condition is not met, two fallback strategies are tried.<br><br>
     * Fallback 1: Try to find a line that starts with "-" and take the first line below that that starts with a decimal.<br>
     * Fallback 2: Try to find a line that starts with "Major" and take the first line below that that starts with a decimal.
     *
     * @param osVersionText The list of strings containing the OS version information.
     * @return An array of strings representing the OS version parts, or null if the parts cannot be determined.
     */
    private String[] parseOsVersionParts(List<String> osVersionText) {
        // Major  Minor  Build  Revision
        // -----  -----  -----  --------
        // 10     0      19044  0

        for (int i = 0; i < osVersionText.size(); i++) {
            final String line = osVersionText.get(i);
            if (line.isEmpty()) continue;
            if (Character.isDigit(line.charAt(0))) {
                if (i > 1 && osVersionText.get(i - 1).startsWith("-") && osVersionText.get(i - 2).startsWith("Major")) {
                    return line.trim().split(" +");
                }
            }
        }

        for (int i = 0; i < osVersionText.size(); i++) {
            final String line = osVersionText.get(i);
            if (line.isEmpty()) continue;
            if (line.startsWith("-")) {
                for (int j = i + 1; j < osVersionText.size(); j++) {
                    final String line2 = osVersionText.get(j);
                    if (line2.isEmpty()) continue;
                    if (Character.isDigit(line2.charAt(0))) {
                        return line2.trim().split(" +");
                    }
                }
            }
        }

        for (int i = 0; i < osVersionText.size(); i++) {
            final String line = osVersionText.get(i);
            if (line.isEmpty()) continue;
            if (line.startsWith("Major")) {
                for (int j = i + 1; j < osVersionText.size(); j++) {
                    final String line2 = osVersionText.get(j);
                    if (line2.isEmpty()) continue;
                    if (Character.isDigit(line2.charAt(0))) {
                        return line2.trim().split(" +");
                    }
                }
            }
        }

        return null;
    }

    private void parseBiosInformation(Inventory inventory, JSONObject biosJson) {
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

    private void parseWin32InstalledProduct(Inventory inventory, JSONArray win32ProductJson, WindowsAnalysisFiles type) {
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
            mapJsonFieldToInventory(productJson, artifact, "Organisation", "Vendor", "Manufacturer");
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
                artifact.set("WMI Class", type.getTypeName());
                inventory.getArtifacts().add(artifact);
            }
        }
    }

    private void parsePlugAndPlayDevices(Inventory inventory, JSONArray pnpEntityJson, JSONArray getPnpDeviceJson, JSONArray pnpSignedDriverJson) {
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
            final PnpClassGuid pnpClassId = PnpClassGuid.fromPnpClass(pnpClass);
            final PnpClassGuid classGuidId = PnpClassGuid.fromClassGuid(pnpClassGuid);
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

    public String findFirstNonNull(JSONObject jsonObject, Set<String> keys) {
        return keys.stream()
                .map(key -> jsonObject.optString(key, null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static <T extends AbstractModelBase> void mapBaseJsonInformationToInventory(JSONObject json, T model) {
        mapJsonFieldToInventory(json, model, "Computer", "Computer");
    }

    private static <T extends AbstractModelBase> void mapJsonFieldToInventory(JSONObject json, T model, AbstractModelBase.Attribute inventoryField, String... jsonFields) {
        mapJsonFieldToInventory(json, model, inventoryField.getKey(), jsonFields);
    }

    private static String getJsonFieldValue(JSONObject json, String... jsonFields) {
        for (String field : jsonFields) {
            if (isStringFieldPresent(json, field)) {
                return json.getString(field);
            } else if (isNumberFieldPresent(json, field)) {
                return String.valueOf(json.getInt(field));
            }
        }
        return null;
    }

    private static <T extends AbstractModelBase> void mapJsonFieldToInventory(JSONObject json, T model, String inventoryField, String... jsonFields) {
        final String fieldValue = getJsonFieldValue(json, jsonFields);
        if (fieldValue != null) {
            model.set(inventoryField, fieldValue);
        }
    }

    private static boolean isStringFieldPresent(JSONObject json, String field) {
        final Object fieldValue = json.opt(field);
        return fieldValue instanceof String && StringUtils.isNotEmpty((String) fieldValue);
    }

    private static boolean isNumberFieldPresent(JSONObject json, String field) {
        return json.has(field) && !json.isNull(field) && json.get(field) instanceof Number;
    }

    public enum WindowsAnalysisFiles {
        Class_CIM_SoftwareElement("CIM_SoftwareElement", "json", true),
        Class_Win32_BaseBoard("Win32_BaseBoard", "json", true),
        Class_Win32_Bios("Win32_Bios", "json", true),
        Class_Win32_CDROMDrive("Win32_CDROMDrive", "json", true),
        Class_Win32_ComputerSystem("Win32_ComputerSystem", "json", true),
        Class_Win32_ComputerSystemProduct("Win32_ComputerSystemProduct", "json", true),
        Class_Win32_DiskDrive("Win32_DiskDrive", "json", true),
        Class_Win32_DiskPartition("Win32_DiskPartition", "json", true),
        Class_Win32_DisplayConfiguration("Win32_DisplayConfiguration", "json", true),
        Class_Win32_InstalledProgramFramework("Win32_InstalledProgramFramework", "json", true),
        Class_Win32_InstalledStoreProgram("Win32_InstalledStoreProgram", "json", true),
        Class_Win32_InstalledWin32Program("Win32_InstalledWin32Program", "json", true),
        Class_Win32_LogicalDisk("Win32_LogicalDisk", "json", true),
        Class_Win32_NetworkAdapter("Win32_NetworkAdapter", "json", true),
        Class_Win32_NetworkAdapterConfiguration("Win32_NetworkAdapterConfiguration", "json", true),
        Class_Win32_NetworkLoginProfile("Win32_NetworkLoginProfile", "json", true),
        Class_Win32_NetworkProtocol("Win32_NetworkProtocol", "json", true),
        Class_Win32_OperatingSystem("Win32_OperatingSystem", "json", true),
        Class_Win32_OptionalFeature("Win32_OptionalFeature", "json", true),
        Class_Win32_PhysicalMemory("Win32_PhysicalMemory", "json", true),
        Class_Win32_PnPEntity("Win32_PnPEntity", "json", true),
        Class_Win32_Printer("Win32_Printer", "json", true),
        Class_Win32_PrinterDriver("Win32_PrinterDriver", "json", true),
        Class_Win32_Processor("Win32_Processor", "json", true),
        Class_Win32_Product("Win32_Product", "json", true),
        Class_Win32_Service("Win32_Service", "json", true),
        Class_Win32_SoftwareElement("Win32_SoftwareElement", "json", true),
        Class_Win32_SoftwareElementCondition("Win32_SoftwareElementCondition", "json", true),
        Class_Win32_SoftwareFeature("Win32_SoftwareFeature", "json", true),
        Class_Win32_SoftwareFeatureCheck("Win32_SoftwareFeatureCheck", "json", true),
        Class_Win32_SoftwareFeatureParent("Win32_SoftwareFeatureParent", "json", true),
        Class_Win32_SoftwareFeatureSoftwareElements("Win32_SoftwareFeatureSoftwareElements", "json", true),
        Class_Win32_SoundDevice("Win32_SoundDevice", "json", true),
        Class_Win32_SystemDriver("Win32_SystemDriver", "json", true),
        Class_Win32_USBController("Win32_USBController", "json", true),
        Class_Win32_VideoController("Win32_VideoController", "json", true),
        Class_Win32_PnpSignedDriver("Win32_PnpSignedDriver", "json", true),

        Get_PnpDevice("Get-PnpDevice", "json", false),

        FileSymlinksList("FileSymlinksList", "txt", false),
        FileSystemDirsList("FileSystemDirsList", "txt", false),
        FileSystemFilesList("FileSystemFilesList", "txt", false),

        RegistrySubtree("RegistrySubtree", "json", false),
        OSversion("OSversion", "txt", false),
        systeminfo("systeminfo", "json", false),
        Get_ComputerInfo("Get-ComputerInfo", "json", false),
        ;

        private final String typeName;
        private final String fileType;
        private final boolean isWmiClass;

        WindowsAnalysisFiles(String typeName, String fileType, boolean isWmiClass) {
            this.typeName = typeName;
            this.fileType = fileType;
            this.isWmiClass = isWmiClass;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getFileType() {
            return fileType;
        }

        public boolean isWmiClass() {
            return isWmiClass;
        }

        public File getFile(File baseDir) {
            return new File(baseDir, getTypeName() + "." + getFileType());
        }

        public File getExistingFile(File baseDir) {
            final File file = getFile(baseDir);
            if (!file.exists()) return null;
            return file;
        }

        public JSONObject readJsonObject(File baseDir) throws IOException {
            final File jsonFile = getFile(baseDir);
            if (!jsonFile.exists()) return null;
            return performParsingStrategyString(jsonFile, source -> {
                final String processed = source
                        .replace("�", "") // U+00EF U+00BF U+00BD
                        .replace("\uFEFF", "") // U+FEFF
                        .replace("\u0000", "") // U+0000
                        .trim();
                if (processed.length() <= 1) {
                    return null;
                }
                return new JSONObject(processed);
            });
        }

        public JSONObject readJsonObjectOrEmpty(File baseDir) throws IOException {
            return Optional.ofNullable(readJsonObject(baseDir)).orElse(new JSONObject());
        }

        public JSONArray readJsonArray(File baseDir) throws IOException {
            final File classFile = getFile(baseDir);
            if (!classFile.exists()) return null;
            return performParsingStrategyString(classFile, source -> {
                final String processed = source
                        .replace("�", "") // U+00EF U+00BF U+00BD
                        .replace("\uFEFF", "") // U+FEFF
                        .replace("\u0000", "") // U+0000
                        .trim();
                if (processed.length() <= 1) {
                    return null;
                }
                return new JSONArray(processed);
            });
        }

        public JSONArray readJsonArrayOrEmpty(File baseDir) throws IOException {
            return Optional.ofNullable(readJsonArray(baseDir)).orElse(new JSONArray());
        }

        public List<String> readLines(File baseDir) throws IOException {
            final File classFile = getFile(baseDir);
            if (!classFile.exists()) return null;
            return performParsingStrategyStringList(classFile, Function.identity());
        }

        public List<String> readLinesOrEmpty(File baseDir) throws IOException {
            return Optional.ofNullable(readLines(baseDir)).orElse(new ArrayList<>());
        }

        private <T> T performParsingStrategyString(File file, Function<String, T> conversionFunction) throws IOException {
            Exception lastException = null;

            for (Charset charset : charsets) {
                try {
                    final String content = FileUtils.readFileToString(file, charset);
                    return conversionFunction.apply(content);
                } catch (Exception e) {
                    LOG.debug("Failed to parse file {} using charset {}", file, charset, e);
                    lastException = e;
                }
            }

            if (lastException != null) {
                throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file + "\n" + lastException.getMessage(), lastException);
            } else {
                throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file);
            }
        }

        private <T> T performParsingStrategyStringList(File file, Function<List<String>, T> conversionFunction) throws IOException {
            Exception lastException = null;

            for (Charset charset : charsets) {
                try {
                    final List<String> content = FileUtils.readLines(file, charset);
                    return conversionFunction.apply(content);
                } catch (Exception e) {
                    LOG.debug("Failed to parse file {} using charset {}", file, charset, e);
                    lastException = e;
                }
            }

            if (lastException != null) {
                throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file + "\n" + lastException.getMessage(), lastException);
            } else {
                throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file);
            }
        }

        private final Charset[] charsets = new Charset[]{
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_16LE,
                StandardCharsets.ISO_8859_1
        };
    }

    /**
     * Enum that lists all PNPClass values and their corresponding ClassGuid values.<br>
     * These values are mapped to the most fitting hardware category.
     */
    public enum PnpClassGuid {
        // https://www.lifewire.com/device-class-guids-for-most-common-types-of-hardware-2619208
        // https://learn.microsoft.com/en-us/windows-hardware/drivers/install/system-defined-device-setup-classes-available-to-vendors
        BATTERY("Battery", "72631e54-78a4-11d0-bcf7-00aa00b7b32a", ArtifactType.POWER_SUPPLY), // UPSs and other battery devices
        BIOMETRIC("Biometric", "53d29ef7-377c-4d14-864b-eb3a85769359", ArtifactType.SCANNER), // Biometric-based devices
        BLUETOOTH("Bluetooth", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974", ArtifactType.EXTENSION_MODULE), // Bluetooth devices
        CAMERA("Camera", "ca3e7ab9-b4c3-4ae6-8251-579ef933890f", ArtifactType.IMAGING_HARDWARE), // Camera devices
        CDROM("CDROM", "4d36e965-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE), // CD/DVD/Blu-ray drives
        DISKDRIVE("DiskDrive", "4d36e967-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE), // Hard drives
        DISPLAY("Display", "4d36e968-e325-11ce-bfc1-08002be10318", ArtifactType.DISPLAY_DRIVER), // Video adapters; display drivers and video miniport drivers
        EXTENSION("Extension", "e2f84ce7-8efa-411c-aa69-97454ca4cb57", ArtifactType.EXTENSION_MODULE), // Devices requiring customizations
        FDC("FDC", "4d36e969-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER), // floppy disk drive controllers
        FLOPPYDISK("FloppyDisk", "4d36e980-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE), // Floppy drives
        HDC("HDC", "4d36e96a-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER), // hard disk controllers, including ATA/ATAPI controllers but not SCSI and RAID disk controllers
        HIDCLASS("HIDClass", "745a17a0-74d3-11d0-b6fe-00a0c90f57da", ArtifactType.INPUT_DEVICE), // interactive input devices that are operated by the system-supplied HID class driver. This includes USB devices that comply with the USB HID Standard and non-USB devices that use a HID minidriver. For more information, see HIDClass Device Setup Class. (See also the Keyboard or Mouse classes later in this list.)
        IEEE1394("1394", "6bdd1fc1-810f-11d0-bec7-08002be2092f", ArtifactType.CONTROLLER), // IEEE 1394 host controller
        IMAGE("Image", "6bdd1fc6-810f-11d0-bec7-08002be2092f", ArtifactType.IMAGING_HARDWARE), // Cameras and scanners
        INFRARED("Infrared", "6bdd1fc5-810f-11d0-bec7-08002be2092f", ArtifactType.NETWORKING_HARDWARE), // Infrared devices, Serial-IR and Fast-IR NDIS miniports
        KEYBOARD("Keyboard", "4d36e96b-e325-11ce-bfc1-08002be10318", ArtifactType.KEYBOARD), // Keyboards
        MEDIUMCHANGER("MediumChanger", "ce5939ae-ebde-11d0-b181-0000f8753ec4", ArtifactType.STORAGE_CONTROLLER), // SCSI media changer devices
        MTD("MTD", "4d36e970-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE), // Memory devices (e.g., flash memory cards)
        MODEM("Modem", "4d36e96d-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE), // Modems
        MONITOR("Monitor", "4d36e96e-e325-11ce-bfc1-08002be10318", ArtifactType.DISPLAY), // Monitors
        MOUSE("Mouse", "4d36e96f-e325-11ce-bfc1-08002be10318", ArtifactType.MOUSE), // Mice and pointing devices
        MULTIFUNCTION("Multifunction", "4d36e971-e325-11ce-bfc1-08002be10318", ArtifactType.APPLIANCE), // Combo cards (e.g., PCMCIA modem)
        MEDIA("Media", "4d36e96c-e325-11ce-bfc1-08002be10318", ArtifactType.MULTIMEDIA_OUTPUT_DEVICE), // Audio and video devices
        MULTIPORTSERIAL("MultiportSerial", "50906cb8-ba12-11d1-bf5d-0000f805f530", ArtifactType.EXTENSION_MODULE), // multiport serial cards, not peripheral devices that connect to its ports
        NET("Net", "4d36e972-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORK_DRIVER), // network adapter drivers
        NETCLIENT("NetClient", "4d36e973-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE), // network and/or print providers
        NETSERVICE("NetService", "4d36e974-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE), // network services, such as redirectors and servers
        PORTS("Ports", "4d36e978-e325-11ce-bfc1-08002be10318", ArtifactType.DEVICE_CONNECTOR), // Serial and parallel ports
        PRINTER("Printer", "4d36e979-e325-11ce-bfc1-08002be10318", ArtifactType.PRINTER), // Printers
        PPNPPRINTERS("PNPPrinters", "4658ee7e-f050-11d1-b6bd-00c04fa372a7", ArtifactType.PRINTER_DRIVER), // SCSI/1394-enumerated printers, but also Bus-specific class drivers, I will map this to printer drivers, as this is what I mostly found on the internet.
        PROCESSOR("Processor", "50127dc3-0f36-415e-a6cc-4cb3be910b65", ArtifactType.PROCESSING_CORE), // Processor types
        SCS_ADAPTER("SCSIAdapter", "4d36e97b-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER), // SCSI and RAID controllers
        SECURITYDEVICES("Securitydevices", "d94ee5d8-d189-4994-83d2-f68d7d41b0e6", ArtifactType.SECURITY_HARDWARE), // Trusted Platform Module chips; crypto-processor that helps with generating, storing, and limiting use of cryptographic keys
        SENSOR("Sensor", "5175d334-c371-4806-b3ba-71fd53c9258d", ArtifactType.SENSOR), // Sensor and location devices
        SMARTCARDREADER("SmartCardReader", "50dd5230-ba8a-11d1-bf5d-0000f805f530", ArtifactType.SECURITY_HARDWARE), // Smart card readers
        VOLUME("Volume", "71a27cdd-812a-11d0-bec7-08002be2092f", ArtifactType.DATA_STORAGE), // Storage volumes
        SYSTEM("System", "4d36e97d-e325-11ce-bfc1-08002be10318", ArtifactType.BOARD), // This class includes HALs, system buses, system bridges, the system ACPI driver, and the system volume manager driver.
        TAPEDRIVE("TapeDrive", "6d807884-7d21-11cf-801c-08002be10318", ArtifactType.DATA_STORAGE), // Tape drives
        USB("USB", "36fc9e60-c465-11cf-8056-444553540000", ArtifactType.STORAGE_CONTROLLER), // USB host controllers and USB hubs, but not USB peripherals
        USBDEVICE("USBDevice", "88bae032-5a81-49f0-bc3d-a4ff138216d6", ArtifactType.DATA_STORAGE), // USB devices that don't belong to another class
        WPD("WPD", "eec5ad98-8080-425f-922a-dabf3de3f69a", ArtifactType.APPLIANCE), // Windows Portable Devices

        AUDIO_PROCESSING_OBJECT("AudioProcessingObject", "5989fce8-9cd0-467d-8a6a-5419e31529d4", ArtifactType.SOUND_HARDWARE), // audio processing objects (APOs) https://learn.microsoft.com/en-us/windows-hardware/drivers/audio/windows-audio-processing-objects
        DOT4("Dot4", "48721b56-6795-11d2-b1a8-0080c72e74a2", ArtifactType.CONTROLLER), // input devices that control multifunction IEEE 1284.4 peripheral devices
        DOT4PRINT("Dot4Print", "49ce6ac8-6f86-11d2-b1e5-0080c72e74a2", ArtifactType.PRINTER), // Dot4 print functions; function on a Dot4 device and has a single child device
        AVC("AVC", "c06ff265-ae09-48f0-812c-16753d7cba83", ArtifactType.CONTROLLER), // IEEE 1394 devices that support the AVC protocol device class
        SBP2("SBP2", "d48179be-ec20-11d1-b6b8-00c04fa372a7", ArtifactType.DATA_STORAGE), // IEEE 1394 devices that support the SBP2 protocol device class
        NET_TRANS("NetTrans", "4d36e975-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE), // NDIS protocols CoNDIS stand-alone call managers, and CoNDIS clients, in addition to higher level drivers in transport stacks
        SECURITY_ACCELERATOR("SecurityAccelerator", "268c95a1-edfe-11d3-95c3-0010dc4050a5", ArtifactType.SECURITY_TOKEN), // devices that accelerate secure socket layer (SSL) cryptographic processing
        PCMCIA("PCMCIA", "4d36e977-e325-11ce-bfc1-08002be10318", ArtifactType.CONTROLLER), // PCMCIA and CardBus host controllers, but not PCMCIA or CardBus peripherals
        WCEUSBS("WCEUSBS", "25dbce51-6c8f-4a72-8a6d-b54c2b4fc835", ArtifactType.APPLIANCE), // Windows CE ActiveSync devices
        IEC61883("61883", "7ebefbc0-3200-11d2-b4c2-00a0c9697d07", ArtifactType.DEVICE_CONNECTOR), // IEEE 1394 devices that support the IEC 61883 protocol device class; speed computer data-transfer interface

        // https://learn.microsoft.com/en-us/windows-hardware/drivers/install/system-defined-device-setup-classes-reserved-for-system-use
        // Obsolete or reserved for system use; may not require direct categorization
        ADAPTER("Adapter", "4d36e964-e325-11ce-bfc1-08002be10318", ArtifactType.EXTENSION_MODULE), // class is obsolete
        APM_SUPPORT("APMSupport", "d45b1c18-c8fa-11d1-9f77-0000f805f530", ArtifactType.CONTROLLER), // reserved for system use; Advanced Power Management
        DECODER("Decoder", "6bdd1fc2-810f-11d0-bec7-08002be2092f", ArtifactType.INPUT_DEVICE), // reserved for future use; multimedia decoders
        DEBUG1394("1394Debug", "66f250d6-7801-4a64-b139-eea80a450b24", ArtifactType.DEVICE_CONNECTOR), // reserved for system use; host-side IEEE 1394 Kernel Debugger Support
        ENUM1394("Enum1394", "c459df55-db08-11d1-b009-00a0c9081ff6", ArtifactType.NETWORKING_HARDWARE), // reserved for system use; IEEE 1394 IP Network Enumerator
        NO_DRIVER("NoDriver", "4d36e976-e325-11ce-bfc1-08002be10318", ArtifactType.CATEGORY_HARDWARE), // class is obsolete; devices that do not require a driver
        LEGACY_DRIVER("LegacyDriver", "8ecc055d-047f-11d1-a537-0000f8753ed1", ArtifactType.DRIVER), // reserved for system use; legacy drivers (Non-Plug and Play Drivers)
        UNKNOWN("Unknown", "4d36e97e-e325-11ce-bfc1-08002be10318", ArtifactType.UNKNOWN), // reserved for system use; devices that could not be identified as a specific class
        PRINTER_UPGRADE("PrinterUpgrade", "4d36e97a-e325-11ce-bfc1-08002be10318", ArtifactType.PRINTER), // reserved for system use; printer upgrades
        SOUND("Sound", "4d36e97c-e325-11ce-bfc1-08002be10318", ArtifactType.SOUND_HARDWARE), // class is obsolete; sound devices
        VOLUME_SNAPSHOT("VolumeSnapshot", "533c5b84-ec70-11d2-9505-00c04f79deaf", ArtifactType.CONTROLLER), // reserved for system use; Volume Shadow Copy Service (VSS) volume snapshot providers

        // found via system scanning
        FIRMWARE("Firmware", "f2e7dd72-6468-4e36-b6f1-6488f42c1b52", ArtifactType.EXTENSION_MODULE), // firmware devices
        SOFTWARE_DEVICE("SoftwareDevice", "62f9c741-b25a-46ce-b54c-9bccce08b6f2", ArtifactType.APPLIANCE), // software-based devices
        AUDIO_ENDPOINT("AudioEndpoint", "c166523c-fe0c-4a94-a586-f1a80cfbbf3e", ArtifactType.SOUND_HARDWARE), // audio devices
        PRINT_QUEUE("PrintQueue", "1ed2bbf9-11f0-4084-b21f-ad83a8e6dcdc", ArtifactType.PRINTER), // print queue devices
        SD_HOST("SDHost", "a0a588a4-c46f-4b37-b7ea-c82fe89870c6", ArtifactType.STORAGE_CONTROLLER), // SD host controllers
        COMPUTER("Computer", "4d36e966-e325-11ce-bfc1-08002be10318", ArtifactType.APPLIANCE), // computer systems
        SOFTWARE_COMPONENT("SoftwareComponent", "5c4c3332-344d-483c-8739-259e934c9cc8", ArtifactType.APPLIANCE), // software components
        SMART_CARD_FILTER("SmartCardFilter", "db4f6ddd-9c0e-45e4-9597-78dbbad0f412", ArtifactType.SECURITY_TOKEN), // smart card filters
        ;

        private final String pnpClass;
        private final String classGuid;
        private final ArtifactType artifactType;

        PnpClassGuid(String pnpClass, String classGuid, ArtifactType artifactType) {
            this.pnpClass = pnpClass;
            this.classGuid = classGuid;
            this.artifactType = artifactType;
        }

        public String getPnpClass() {
            return pnpClass;
        }

        public String getClassGuid() {
            return classGuid;
        }

        public ArtifactType getArtifactType() {
            return artifactType;
        }

        public static PnpClassGuid fromPnpClass(String pnpClass) {
            if (pnpClass == null) return null;
            for (PnpClassGuid value : values()) {
                if (value.getPnpClass().equalsIgnoreCase(pnpClass)) {
                    return value;
                }
            }
            return null;
        }

        public static PnpClassGuid fromClassGuid(String classGuid) {
            if (classGuid == null) return null;
            for (PnpClassGuid value : values()) {
                if (value.getClassGuid().equalsIgnoreCase(classGuid)) {
                    return value;
                }
            }
            // check if classGuid from parameter is surrounded with { }
            // like: {4d36e96b-e325-11ce-bfc1-08002be10318} for the Keyboard class
            if (classGuid.startsWith("{") && classGuid.endsWith("}")) {
                return fromClassGuid(classGuid.substring(1, classGuid.length() - 1));
            }
            return null;
        }
    }
}
