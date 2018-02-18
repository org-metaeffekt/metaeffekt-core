/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InventoryUpdate {

    private File sourceInventoryFile;

    private File targetInventoryFile;

    private List<InventoryProcessor> inventoryProcessors;

    private Map<String, String> licenseNameMap = null;

    private Map<String, String> componentNameMap = null;

    public Inventory process() {
        try {
            File targetFolder = targetInventoryFile.getParentFile();
            if (targetFolder != null) {
                // ensure all folders to save target file later exist
                targetFolder.mkdirs();
            }

            // read global inventory with manual managed meta data
            Inventory inventory = null;

            if (sourceInventoryFile.exists()) {
                inventory = new InventoryReader().readInventory(sourceInventoryFile);
            } else {
                inventory = new Inventory();
            }

            if (getComponentNameMap() != null) {
                inventory.setComponentNameMap(getComponentNameMap());
            }

            if (getLicenseNameMap() != null) {
                inventory.setLicenseNameMap(getLicenseNameMap());
            }

            // store a text version for diff purposes
            inventory.dumpAsFile(new File(targetFolder, targetInventoryFile.getName()
                    + ".previous.txt"));

            // Note: it is not decided whether the consolidation of component
            // names and license names should be subject to a processor. Indeed
            // the activity is distributed. Therefore we apply it initially on this
            // level.
            inventory.mapComponentNames();
            inventory.mapLicenseNames();

            // iterate the inventory processors and apply them
            if (inventoryProcessors != null) {
                for (InventoryProcessor inventoryProcessor : inventoryProcessors) {
                    inventoryProcessor.process(inventory);
                }
            }

            // write the new inventory file
            new InventoryWriter().writeInventory(inventory, targetInventoryFile);

            // write the text representation for diff
            inventory.dumpAsFile(new File(targetFolder, targetInventoryFile.getName()
                    + ".update.txt"));

            return inventory;
        } catch (IOException e) {
            throw new RuntimeException("Cannot process inventory.", e);
        }
    }

    public File getSourceInventoryFile() {
        return sourceInventoryFile;
    }

    public void setSourceInventoryFile(File sourceInventoryFile) {
        this.sourceInventoryFile = sourceInventoryFile;
    }

    public File getTargetInventoryFile() {
        return targetInventoryFile;
    }

    public void setTargetInventoryFile(File targetInventoryFile) {
        this.targetInventoryFile = targetInventoryFile;
    }

    public List<InventoryProcessor> getInventoryProcessors() {
        return inventoryProcessors;
    }

    public void setInventoryProcessors(List<InventoryProcessor> inventoryProcessors) {
        this.inventoryProcessors = inventoryProcessors;
    }

    public Map<String, String> getLicenseNameMap() {
        return licenseNameMap;
    }

    public void setLicenseNameMap(Map<String, String> licenseNameMap) {
        this.licenseNameMap = licenseNameMap;
    }

    public Map<String, String> getComponentNameMap() {
        return componentNameMap;
    }

    public void setComponentNameMap(Map<String, String> componentNameMap) {
        this.componentNameMap = componentNameMap;
    }

}
