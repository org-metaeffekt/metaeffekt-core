/**
 * Copyright 2009-2017 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.reader.ProtexInventoryReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


public class MergeProtexInventoryProcessor extends AbstractInventoryProcessor {

    public static final String PROTEX_INVENTORY_PATH = "protex.inventory.path";
    public static final String PROPAGATE_NOT_EXISITING = "propagate.not.exiisting";

    public MergeProtexInventoryProcessor() {
        super();
    }

    public MergeProtexInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        String protexInventoryFileName = getProperties().getProperty(PROTEX_INVENTORY_PATH);

        if (protexInventoryFileName == null) {
            throw new IllegalArgumentException("Please specify the '" + PROTEX_INVENTORY_PATH + "' property.");
        }

        File protexInventoryFile = new File(protexInventoryFileName);

        Inventory protexInventory;
        try {
            protexInventory = new ProtexInventoryReader().readInventory(protexInventoryFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot load protex inventory.", e);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load protex inventory.", e);
        }

        // take over license and component mappings from input inventory
        protexInventory.setComponentNameMap(inventory.getComponentNameMap());
        protexInventory.setLicenseNameMap(inventory.getLicenseNameMap());

        // map the licenses and components according to the mappings (adaptation)
        protexInventory.mapComponentNames();
        protexInventory.mapLicenseNames();

        protexInventory.removeInconsistencies();

        protexInventory.expandArtifactsWithMultipleVersions();

        Boolean propagateNotExisting = Boolean.valueOf(
                getProperties().getProperty(PROPAGATE_NOT_EXISITING, "true"));

        // merge the protex inventory into the current inventory
        inventory.mergeLicenseData(protexInventory, propagateNotExisting);
    }

}
