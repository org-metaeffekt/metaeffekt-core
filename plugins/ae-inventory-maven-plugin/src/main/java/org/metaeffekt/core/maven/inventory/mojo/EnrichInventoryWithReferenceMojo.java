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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.CspLoader;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;
import java.io.IOException;

/**
 * Creates a report based on an existing inventory.
 */
@Mojo(name = "enrich-inventory-with-reference")
public class EnrichInventoryWithReferenceMojo extends AbstractInventoryReportCreationMojo {

    @Parameter(required = true)
    private File inventory;

    @Override
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {

        // use this to modify the config parameters specific to this mojo
        ReportConfigurationParameters.ReportConfigurationParametersBuilder configParams = configureParameters();
        configParams.failOnError(false);
        configParams.failOnBanned(false);
        configParams.failOnInternal(false);
        configParams.failOnUnknownVersion(false);
        configParams.failOnUnknown(false);
        configParams.failOnDevelopment(false);
        configParams.failOnMissingLicense(false);
        configParams.failOnMissingLicenseFile(false);
        configParams.failOnMissingNotice(false);
        configParams.failOnDowngrade(false);
        configParams.failOnUpgrade(false);


        InventoryReport report = new InventoryReport(configParams.build());

        // apply standard configuration (parent class)
        configureInventoryReport(report);
        report.setTargetInventoryDir(inventory.getParentFile());
        report.setTargetInventoryPath(inventory.getName());

        try {
            getLog().info("Starting inventory report creation for " + inventory.getAbsolutePath());
            final Inventory readInventory = new InventoryReader().readInventory(inventory);
            getLog().debug("Parsed inventory data: " + readInventory.getInventorySizePrintString());
            report.setInventory(readInventory);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot create inventory report.", e);
        }

        return report;
    }

}
