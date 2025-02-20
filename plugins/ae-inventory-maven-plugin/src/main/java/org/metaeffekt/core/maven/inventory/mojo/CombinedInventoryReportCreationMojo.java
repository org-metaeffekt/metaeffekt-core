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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Creates a report based on an set of inventories.
 *
 * @goal create-combined-inventory-report
 */
public class CombinedInventoryReportCreationMojo extends AbstractInventoryReportCreationMojo {

    /**
     * The inventory basedir.
     *
     * @parameter
     * @required
     */
    protected File inventoryDir;

    /**
     * Include pattern for inventories; relative to the inventoryDir.
     *
     * @parameter default-value="*.xls*"
     */
    protected String inventoryIncludes;

    @Override
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        try {
            InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder().
                    hidePriorityInformation(isHidePriorityScoreInformation()).build());

            // apply standard configuration (parent class)
            configureInventoryReport(report);

            Inventory inventory = new Inventory();

            String[] files = FileUtils.scanForFiles(inventoryDir, inventoryIncludes, "--nothing--");

            for (String file : files) {
                File inventoryFile = new File(inventoryDir, file);
                Inventory i = new InventoryReader().readInventory(inventoryFile);
                inventory.getArtifacts().addAll(i.getArtifacts());
            }

            report.setInventory(inventory);

            return report;
        } catch (IOException ex) {
            throw new MojoExecutionException("Cannot create inventory report.", ex);
        }
    }

}
