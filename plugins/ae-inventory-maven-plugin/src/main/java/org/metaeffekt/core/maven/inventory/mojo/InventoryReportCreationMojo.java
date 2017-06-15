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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.plugin.MojoExecutionException;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.InventoryScanReport;

import java.io.File;
import java.io.IOException;

/**
 * Creates a report based on an existing inventory.
 * 
 * @goal create-inventory-report
 */
public class InventoryReportCreationMojo extends AbstractInventoryReportCreationMojo {

    /**
     * @parameter
     * @required
     */
    private File inventory;
    
    @Override
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = new InventoryReport();

        // apply standard configuration (parent class)
        configureInventoryReport(report);

        try {
            report.setRepositoryInventory(new GlobalInventoryReader().readInventory(inventory));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot create inventory report.", e);
        }

        return report;
    }

}
