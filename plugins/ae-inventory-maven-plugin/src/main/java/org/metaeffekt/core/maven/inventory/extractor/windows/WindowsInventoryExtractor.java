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

import org.json.JSONArray;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.maven.inventory.extractor.windows.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_SOURCE_PROJECT;
import static org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile.*;

public class WindowsInventoryExtractor implements InventoryExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsInventoryExtractor.class);

    /**
     * Checks if the specified analysis directory contains:
     * <ul>
     *     <li>any Windows Source files</li>
     * </ul>
     *
     * @param analysisDir The analysis directory to be checked.
     * @return true if any of the above conditions are true, false otherwise.
     */
    @Override
    public boolean applies(File analysisDir) {
        return Arrays.stream(WindowsExtractorAnalysisFile.values())
                .anyMatch(scanFile -> scanFile.getFile(analysisDir).exists());
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
    }

    @Override
    public Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException {
        LOG.info("Windows inventory extraction [{}] started with analysis directory: {}", inventoryId, analysisDir);

        final Inventory inventory = new Inventory();

        new WindowsPartExtractorPlugAndPlay().parse(inventory,
                Class_Win32_PnPEntity.readJsonArrayOrEmpty(analysisDir),
                Get_PnpDevice.readJsonArrayOrEmpty(analysisDir),
                Class_Win32_PnpSignedDriver.readJsonArrayOrEmpty(analysisDir)
        );

        new WindowsPartExtractorBios().parse(inventory,
                Class_Win32_Bios.readJsonObjectOrEmpty(analysisDir)
        );

        new WindowsPartExtractorOperatingSystem().parse(inventory,
                systeminfo.readJsonObjectOrEmpty(analysisDir),
                Class_Win32_OperatingSystem.readJsonObjectOrEmpty(analysisDir),
                OSversion.readLinesOrEmpty(analysisDir),
                Get_ComputerInfo.readJsonObjectOrEmpty(analysisDir)
        );

        final WindowsPartExtractorInstalledProduct productExtractor = new WindowsPartExtractorInstalledProduct();
        productExtractor.parse(inventory, Class_Win32_Product.readJsonArrayOrEmpty(analysisDir), Class_Win32_Product);
        productExtractor.parse(inventory, Class_Win32_InstalledWin32Program.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledWin32Program);
        productExtractor.parse(inventory, Class_Win32_InstalledProgramFramework.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledProgramFramework);
        productExtractor.parse(inventory, Class_Win32_InstalledStoreProgram.readJsonArrayOrEmpty(analysisDir), Class_Win32_InstalledStoreProgram);

        new WindowsPartExtractorSoftwareElement().parse(inventory,
                Class_Win32_SoftwareElement.readJsonArrayOrEmpty(analysisDir),
                Class_Win32_SoftwareFeature.readJsonArrayOrEmpty(analysisDir),
                Class_Win32_SoftwareFeatureSoftwareElements.readJsonArrayOrEmpty(analysisDir)
        );

        // fill details for specific PnP devices
        new WindowsPartExtractorMotherboard().parse(inventory,
                Class_Win32_BaseBoard.readJsonObjectOrEmpty(analysisDir),
                Class_Win32_MotherboardDevice.readJsonObjectOrEmpty(analysisDir)
        );
        new WindowsPartExtractorOptionalFeature().parse(inventory,
                Class_Win32_OptionalFeature.readJsonArrayOrEmpty(analysisDir)
        );
        new WindowsPartExtractorVideoController().parse(inventory,
                Class_Win32_VideoController.readJsonObject(analysisDir)
        );
        new WindowsPartExtractorNetworkAdapter().parse(inventory,
                Class_Win32_NetworkAdapter.readJsonArrayOrEmpty(analysisDir)
        );
        new WindowsPartExtractorPrinterDriver().parse(inventory,
                // Class_Win32_Driver does not contain any new information
                Class_Win32_PrinterDriver.readJsonArrayOrEmpty(analysisDir)
        );
        // Class_Win32_SoundDevice does not contain any new information


        new WindowsPartExtractorRegUninstall().parse(inventory,
                this.firstNonEmptyJsonArray(
                        RegistrySubtree_WindowsUninstall.readJsonArrayOrEmpty(analysisDir),
                        RegistrySubtree.readJsonArrayOrEmpty(analysisDir)
                )
        );

        // FIXME: combine with asset information (see AbstractInventoryExtractor#extractInventory)
        for (final Artifact artifact : inventory.getArtifacts()) {
            artifact.set(KEY_SOURCE_PROJECT, inventoryId);
        }

        try {
            inventory.getArtifacts().sort(Comparator
                    .comparing(a -> {
                        final String wmiClass = ((Artifact) a).get("Windows Source");
                        if (wmiClass == null) return 0;
                        if (wmiClass.equals("systeminfo, Win32_OperatingSystem, OSversion, Get-ComputerInfo")) return 1;
                        if (wmiClass.equals(Class_Win32_InstalledStoreProgram.getTypeName())) return 10;
                        if (wmiClass.equals(RegistrySubtree_WindowsUninstall.getTypeName())) return 20;
                        if (wmiClass.equals(Class_Win32_InstalledWin32Program.getTypeName())) return 30;
                        if (wmiClass.equals(Class_Win32_Product.getTypeName())) return 40;
                        if (wmiClass.equals(Class_Win32_SoftwareFeature.getTypeName())) return 50;
                        if (wmiClass.equals(Class_Win32_PnPEntity.getTypeName())) return 60;
                        return 70;
                    })
                    .thenComparing(a -> ((Artifact) a).get("Windows Source") == null ? "" : ((Artifact) a).get("Windows Source"))
                    .thenComparing(a -> ((Artifact) a).get(Artifact.Attribute.TYPE) == null ? "" : ((Artifact) a).get(Artifact.Attribute.TYPE))
                    .thenComparing(a -> ((Artifact) a).get(Artifact.Attribute.ID) == null ? "" : ((Artifact) a).get(Artifact.Attribute.ID)));
        } catch (Exception e) {
            LOG.warn("Failed to sort artifacts in windows extracted inventory", e);
        }

        LOG.info("Windows inventory extraction completed");
        LOG.info("With results:"); // list count by "Windows Source"
        inventory.getArtifacts().stream()
                .collect(Collectors.groupingBy(a -> a.get("Windows Source") == null ? "other" : a.get("Windows Source")))
                .entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getValue().size()))
                .forEach(e -> LOG.info("  {}: {}", e.getKey(), e.getValue().size()));

        return inventory;
    }

    private JSONArray firstNonEmptyJsonArray(JSONArray... jsonArrays) {
        return Arrays.stream(jsonArrays)
                .filter(jsonArray -> jsonArray != null && !jsonArray.isEmpty())
                .findFirst()
                .orElseGet(JSONArray::new);
    }
}
