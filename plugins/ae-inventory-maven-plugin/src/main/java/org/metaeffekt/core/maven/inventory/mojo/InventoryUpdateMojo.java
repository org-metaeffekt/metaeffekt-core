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
import org.apache.maven.plugin.MojoFailureException;
import org.metaeffekt.core.inventory.processor.AbstractInventoryProcessor;
import org.metaeffekt.core.inventory.processor.InventoryProcessor;
import org.metaeffekt.core.inventory.processor.InventoryUpdate;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Mojo dedicated to automated inventory updates and consolidation.
 *
 * @goal update-inventory
 * @requiresDependencyResolution runtime
 */
public class InventoryUpdateMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * @parameter
     * @required
     */
    private File sourceInventoryPath;

    /**
     * @parameter
     * @required
     */
    private File targetInventoryPath;

    /**
     * @parameter
     */
    private List<Processor> processors;

    /**
     * @parameter
     */
    private Map<String, String> properties;

    /**
     * @parameter
     */
    private Mapping componentNameMapping;

    /**
     * @parameter
     */
    private Mapping licenseNameMapping;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
            List<InventoryProcessor> inventoryProcessors =
                    new ArrayList<InventoryProcessor>();

            if (processors != null) {
                for (Processor processorConfig : processors) {
                    if (!processorConfig.isSkip()) {
                        AbstractInventoryProcessor processor = createInstanceOf(processorConfig.getClassName(), AbstractInventoryProcessor.class);

                        Properties aggregrateProperties = new Properties();
                        aggregrateProperties.putAll(System.getProperties());
                        aggregrateProperties.putAll(getProject().getProperties());
                        if (this.properties != null) {
                            aggregrateProperties.putAll(this.properties);
                        }
                        if (processorConfig.getProperties() != null) {
                            aggregrateProperties.putAll(processorConfig.getProperties());
                        }

                        processor.setProperties(aggregrateProperties);
                        inventoryProcessors.add(processor);
                    }
                }
            }

            getLog().info("Processors found: ");
            for (InventoryProcessor inventoryProcessor : inventoryProcessors) {
                getLog().info(inventoryProcessor.getClass().toString());
            }

            InventoryUpdate inventoryUpdate = new InventoryUpdate();
            inventoryUpdate.setInventoryProcessors(inventoryProcessors);
            inventoryUpdate.setSourceInventoryFile(sourceInventoryPath);
            inventoryUpdate.setTargetInventoryFile(targetInventoryPath);

            if (componentNameMapping != null) {
                inventoryUpdate.setComponentNameMap(componentNameMapping.getMap());
            }

            if (licenseNameMapping != null) {
                inventoryUpdate.setLicenseNameMap(licenseNameMapping.getMap());
            }

            inventoryUpdate.process();
        } finally {
            MavenLogAdapter.release();
        }
    }

}