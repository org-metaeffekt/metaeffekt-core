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
import org.metaeffekt.core.inventory.processor.model.ArtifactType;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.Class_Win32_PrinterDriver;

public class WindowsPartExtractorPrinterDriver extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorPrinterDriver.class);

    public void parse(Inventory inventory, JSONArray networkAdapterArrayJson) {
        for (int i = 0; i < networkAdapterArrayJson.length(); i++) {
            final JSONObject networkAdapterJson = networkAdapterArrayJson.getJSONObject(i);
            parse(inventory, networkAdapterJson);
        }
    }

    public void parse(Inventory inventory, JSONObject networkAdapterJson) {
        // pnp device id should not exist, we check nonetheless
        final String pnpDeviceID = getJsonFieldValue(networkAdapterJson, "PNPDeviceID");
        final PrinterDriverNameParts parsedDriverNameParts = PrinterDriverNameParts.parse(getJsonFieldValue(networkAdapterJson, "Name"));

        final Artifact videoControllerArtifact = findArtifactOrElseAppendNew(inventory,
                artifact -> {
                    if (pnpDeviceID != null) {
                        return StringUtils.equals(artifact.get("PNPDeviceID"), pnpDeviceID);
                    } else if (parsedDriverNameParts != null) {
                        return StringUtils.equals(artifact.getId(), parsedDriverNameParts.getName());
                    } else {
                        return false;
                    }
                });

        mapBaseJsonInformationToInventory(networkAdapterJson, videoControllerArtifact);
        videoControllerArtifact.set("Windows Source", Class_Win32_PrinterDriver.getTypeName());
        videoControllerArtifact.set(Artifact.Attribute.TYPE, ArtifactType.PRINTER_DRIVER.getCategory());
        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "PNPDeviceID", "PNPDeviceID");

        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, Artifact.Attribute.COMPONENT, "Name", "Caption", "Description");
        mapJsonFieldToInventory(networkAdapterJson, videoControllerArtifact, "Platform", "SupportedPlatform");

        if (parsedDriverNameParts != null) {
            videoControllerArtifact.setId(parsedDriverNameParts.getName());
            videoControllerArtifact.set(Artifact.Attribute.COMPONENT, parsedDriverNameParts.getName());
            videoControllerArtifact.set(Artifact.Attribute.VERSION, parsedDriverNameParts.getVersion());
            videoControllerArtifact.set("Platform", parsedDriverNameParts.getSupportedPlatform());
        }


        // collect all paths into "System Paths"
        final Set<String> systemPaths = new HashSet<>();

        systemPaths.add(getJsonFieldValue(networkAdapterJson, "ConfigFile"));
        systemPaths.add(getJsonFieldValue(networkAdapterJson, "DataFile"));
        systemPaths.add(getJsonFieldValue(networkAdapterJson, "DriverPath"));
        systemPaths.add(getJsonFieldValue(networkAdapterJson, "FilePath"));
        systemPaths.add(getJsonFieldValue(networkAdapterJson, "HelpFile"));
        final JSONArray dependantFiles = networkAdapterJson.optJSONArray("DependentFiles");
        if (dependantFiles != null) {
            for (int i = 0; i < dependantFiles.length(); i++) {
                systemPaths.add(dependantFiles.getString(i));
            }
        }
        systemPaths.removeIf(StringUtils::isEmpty);

        if (!systemPaths.isEmpty()) {
            videoControllerArtifact.append("System Paths", String.join(", ", systemPaths), ", ");
        }


        if (videoControllerArtifact.getAttributes().isEmpty()) {
            inventory.getArtifacts().remove(videoControllerArtifact);
        }
    }

    protected static class PrinterDriverNameParts {
        // Microsoft XPS Document Writer v4,4,Windows x64
        // Microsoft Print To PDF,4,Windows x64

        private final String name;
        private final String version;
        private final String supportedPlatform;

        protected PrinterDriverNameParts(String name, String version, String supportedPlatform) {
            this.name = name;
            this.version = version;
            this.supportedPlatform = supportedPlatform;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getSupportedPlatform() {
            return supportedPlatform;
        }

        public static PrinterDriverNameParts parse(String name) {
            if (StringUtils.isEmpty(name)) return null;
            final String[] parts = name.split(",");
            if (parts.length == 3) {
                return new PrinterDriverNameParts(parts[0], parts[1], parts[2]);
            } else {
                return new PrinterDriverNameParts(name, null, null);
            }
        }

        @Override
        public String toString() {
            return name + "," + version + "," + supportedPlatform;
        }
    }
}
