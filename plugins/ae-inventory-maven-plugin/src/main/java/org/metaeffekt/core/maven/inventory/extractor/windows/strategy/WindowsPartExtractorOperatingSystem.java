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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ArtifactType;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsPartExtractorOperatingSystem extends WindowsPartExtractorBase {

    public void parse(Inventory inventory,
                      JSONObject systeminfoJson, JSONObject operatingSystemJson,
                      List<String> osVersionText, JSONObject getComputerInfoJson) {
        final Artifact windowsOsArtifact = new Artifact();

        mapBaseJsonInformationToInventory(operatingSystemJson, windowsOsArtifact);
        windowsOsArtifact.set("Windows Source",
                WindowsExtractorAnalysisFile.systeminfo.getTypeName() + ", "
                        + WindowsExtractorAnalysisFile.Class_Win32_OperatingSystem.getTypeName() + ", "
                        + WindowsExtractorAnalysisFile.OSversion.getTypeName() + ", "
                        + WindowsExtractorAnalysisFile.Get_ComputerInfo.getTypeName());

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

        mapJsonFieldToInventory(systeminfoJson, windowsOsArtifact, Constants.KEY_ORGANIZATION, "OS Manufacturer");

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
}
