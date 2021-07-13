/*
 * Copyright 2009-2021 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractInputInventoryBasedProcessor extends AbstractInventoryProcessor {

    public static final String INPUT_INVENTORY = "input.inventory.path";

    public AbstractInputInventoryBasedProcessor() {
        super();
    }

    public AbstractInputInventoryBasedProcessor(Properties properties) {
        super(properties);
    }

    protected Inventory loadInputInventory() {
        final String inventoryFileName = getProperties().getProperty(INPUT_INVENTORY);

        if (inventoryFileName == null) {
            throw new IllegalArgumentException("Please specify the '" + INPUT_INVENTORY + "' property.");
        }

        File inventoryFile = new File(inventoryFileName);

        Inventory inputInventory;
        try {
            inputInventory = new InventoryReader().readInventory(inventoryFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load inventory.", e);
        }
        return inputInventory;
    }

}
