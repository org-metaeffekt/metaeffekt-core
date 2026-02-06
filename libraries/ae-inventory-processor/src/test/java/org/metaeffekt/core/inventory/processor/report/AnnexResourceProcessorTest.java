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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;
import java.io.IOException;

public class AnnexResourceProcessorTest {

    @Test
    public void testLicenseResourceProcessor () throws IOException {

        InventoryReader reader = new InventoryReader();
        String resourceDir = "src/test/resources/license-resource-processor/reference-inventory";
        Inventory inventory = reader.readInventory(new File(resourceDir, "inventory/artifact-inventory-01.xls"));
        Inventory referenceInventory = reader.readInventory(new File(resourceDir, "inventory/artifact-inventory-01.xls"));
        ReportConfigurationParameters configParams = ReportConfigurationParameters.builder().build();
        File referenceInventoryDir = new File (resourceDir);
        String referenceComponentPath = "components";
        String referenceLicensePath = "licenses";
        File targetComponentDir = new File("target/license-resource-processor/inventory/components");
        File targetLicenseDir = new File("target/license-resource-processor/inventory/licenses");

        AnnexResourceProcessor processor = new AnnexResourceProcessor(inventory, referenceInventory, configParams, referenceInventoryDir, referenceComponentPath, referenceLicensePath, targetComponentDir, targetLicenseDir);
        processor.execute();
    }
}

