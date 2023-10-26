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
     *     <li>any WMI class files</li>
     * </ul>
     *
     * @param analysisDir The analysis directory to be checked.
     * @return true if any of the above conditions are true, false otherwise.
     */
    @Override
    public boolean applies(File analysisDir) {
        return Arrays.stream(WindowsExtractorAnalysisFile.values())
                .anyMatch(wmiClass -> new File(analysisDir, wmiClass.getTypeName()).exists());
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
    }

    @Override
    public Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException {
        final Inventory inventory = new Inventory();

        new WindowsPartExtractorPlugAndPlay().parse(inventory,
                Class_Win32_PnPEntity.readJsonArrayOrEmpty(analysisDir),
                Get_PnpDevice.readJsonArrayOrEmpty(analysisDir),
                Class_Win32_PnpSignedDriver.readJsonArrayOrEmpty(analysisDir)
        );

        new WindowsPartExtractorBios().parse(inventory, Class_Win32_Bios.readJsonObjectOrEmpty(analysisDir));

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

        /*new WindowsPartExtractorMotherboard().parse(inventory,
                Class_Win32_BaseBoard.readJsonObjectOrEmpty(analysisDir),
                Class_Win32_MotherboardDevice.readJsonObjectOrEmpty(analysisDir)
        );*/

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

}
