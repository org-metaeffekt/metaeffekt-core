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

package org.metaeffekt.core.inventory.processor.inspector;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InventoryInspector implements ArtifactInspector {

    @Override
    public void run(Inventory inventory, Properties properties) {
        final ProjectPathParam projectPathParam = new ProjectPathParam(properties);

        final Stream<Artifact> filteredArtifacts = inventory.getArtifacts().stream()
                .filter(artifact -> artifact.getPathInAsset() != null &&
                        (artifact.getPathInAsset().endsWith(".xls") ||
                                artifact.getPathInAsset().endsWith(".xlsx") ||
                                artifact.getPathInAsset().endsWith(".ser")));

        // iterate inventories; parse and simply add all covered artifacts and assets
        for (Artifact artifact : filteredArtifacts.collect(Collectors.toList())) {
            final File inventoryFile = new File(projectPathParam.getProjectPath().getAbsolutePath(), artifact.getPathInAsset());
            if (inventoryFile.exists()) {
                try {
                    final Inventory detectedInventory = new InventoryReader().readInventory(inventoryFile);

                    inventory.getArtifacts().addAll(detectedInventory.getArtifacts());
                    inventory.getAssetMetaData().addAll(detectedInventory.getAssetMetaData());

                    // remove the original file (this is only a carrier)
                    inventory.getArtifacts().remove(artifact);
                } catch (Exception e) {
                    // ignore all; seems not an inventory as we know it
                    log.debug("Skipping inspection of potential inventory file due to: " + e.getMessage());
                }
            }
        }
    }
}

