/**
 * Copyright 2009-2019 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.metaeffekt.core.inventory.processor.model.Constants.STRING_TRUE;

@Ignore
public class ExternalRepositoryReportTest {

    private static final File INVENTORY_DIR = new File("/Users/kklein/workspace/spring-boot-example/inventory/src/main/resources");
    private static final String INVENTORY_INCLUDES = "inventory/*.xls";
    private static final String LICENSE_FOLDER = "licenses";
    private static final String COMPONENT_FOLDER = "components";

    @Test
    public void testValidateInventoryProcessor() throws IOException {
        final Inventory inventory = InventoryUtils.readInventory(INVENTORY_DIR, INVENTORY_INCLUDES);

        Properties properties = new Properties();
        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, new File(INVENTORY_DIR, LICENSE_FOLDER).getAbsolutePath());
        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_DIR, new File(INVENTORY_DIR, COMPONENT_FOLDER).getAbsolutePath());

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        validateInventoryProcessor.process(inventory);
    }

    @Ignore
    @Test
    public void testValidateInventoryProcessor_WorkbenchInput() throws IOException {
        boolean enableDeleteObsolete = false;

        final File inventoryDir = new File("/Users/kklein/workspace/ae-workbench-input/common");
        final Inventory inventory = InventoryUtils.readInventory(inventoryDir, INVENTORY_INCLUDES);

        File licensesDir = new File(inventoryDir, LICENSE_FOLDER);
        File componentsDir = new File(inventoryDir, COMPONENT_FOLDER);

        Properties properties = new Properties();

        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, licensesDir.getPath());
//        properties.setProperty(ValidateInventoryProcessor.LICENSES_TARGET_DIR, licensesTargetDir.getPath());

        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_DIR, componentsDir.getPath());
//        properties.setProperty(ValidateInventoryProcessor.COMPONENTS_TARGET_DIR, componentsTargetDir.getPath());

        properties.setProperty(ValidateInventoryProcessor.FAIL_ON_ERROR, STRING_TRUE);
        properties.setProperty(ValidateInventoryProcessor.CREATE_LICENSE_FOLDERS, STRING_TRUE);
        properties.setProperty(ValidateInventoryProcessor.CREATE_COMPONENT_FOLDERS, STRING_TRUE);

        if (enableDeleteObsolete) {
            properties.setProperty(ValidateInventoryProcessor.DELETE_LICENSE_FOLDERS, STRING_TRUE);
            properties.setProperty(ValidateInventoryProcessor.DELETE_COMPONENT_FOLDERS, STRING_TRUE);
        }

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        validateInventoryProcessor.process(inventory);
    }

}
