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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.AnnexResourceProcessor;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;
import java.io.IOException;

@Mojo(name = "aggregate-reference-licenses", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class AggregateReferenceLicensesMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(required = true)
    private File inventoryFile;

    @Parameter(required = true)
    private File referenceInventoryDir;

    @Parameter(defaultValue = "**/*.ser,**/*.xls,**/*.xlsx")
    private String referenceInventoryIncludes;

    @Parameter(defaultValue = "../components")
    private String referenceComponentsDir;

    @Parameter(defaultValue = "../licenses")
    private String referenceLicensesDir;

    @Parameter
    private File targetComponentsDir;

    @Parameter
    private File targetLicensesDir;

    @Parameter(defaultValue = "true")
    private boolean failOnMissingLicenseFile;

    @Override
    public void execute() throws MojoExecutionException {
        // validate mandatory inputs
        if (inventoryFile == null || !inventoryFile.exists()) {
            throw new MojoExecutionException("Inventory file is missing or invalid: " + inventoryFile);
        }

        // check if the path is null, empty, or contains an unexpanded Maven placeholder
        if (isInvalidPath(targetComponentsDir)) {
            File defaultDir = new File(getProject().getBuild().getDirectory(), "inventory/components");
            getLog().warn("Target component directory is not set or invalid. Falling back to: " + defaultDir);
            this.targetComponentsDir = defaultDir;
        }

        if (isInvalidPath(targetLicensesDir)) {
            File defaultDir = new File(getProject().getBuild().getDirectory(), "inventory/licenses");
            getLog().warn("Target license directory is not set or invalid. Falling back to: " + defaultDir);
            this.targetLicensesDir = defaultDir;
        }

        // sanitize reference paths (ensure they aren't "${...}")
        if (isInvalidString(referenceComponentsDir)) {
            this.referenceComponentsDir = "components";
        }
        if (isInvalidString(referenceLicensesDir)) {
            this.referenceLicensesDir = "licenses";
        }

        try {
            Inventory inventory = new InventoryReader().readInventory(inventoryFile);
            Inventory referenceInventory = InventoryUtils.readInventory(referenceInventoryDir, referenceInventoryIncludes);

            ReportConfigurationParameters configParams = ReportConfigurationParameters.builder()
                    .failOnMissingLicenseFile(failOnMissingLicenseFile)
                    .build();

            AnnexResourceProcessor processor = new AnnexResourceProcessor(
                    inventory, referenceInventory, configParams,
                    referenceInventoryDir, referenceComponentsDir, referenceLicensesDir,
                    targetComponentsDir, targetLicensesDir
            );

            boolean success = processor.execute();

            if (!success && failOnMissingLicenseFile) {
                throw new MojoExecutionException("Resource processing failed. Check logs for missing files.");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Error during resource aggregation", e);
        }
    }

    /**
     * Helper to check if a File path is effectively 'unset' or a literal Maven placeholder.
     */
    private boolean isInvalidPath(File file) {
        return file == null || file.getPath().isEmpty() || file.getPath().contains("${");
    }

    /**
     * Helper to check if a String parameter is a literal Maven placeholder.
     */
    private boolean isInvalidString(String str) {
        return str == null || str.isEmpty() || str.contains("${");
    }
}
