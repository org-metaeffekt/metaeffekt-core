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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.io.IOException;

/**
 * Creates a report for the dependencies listed in the pom.
 * 
 * @goal aggregate-inventory-reports
 */
public class ReportAggregationMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * @parameter
     */
    private String[] scanIncludes = new String[] { "**/target/**/*-inventory.xls" };

    /**
     * @parameter expression="${project.build.directory}/inventory/${project.artifactId}-${project.version}-aggregate-inventory.xls"
     */
    private String targetInventoryPath;

    /**
     * @parameter expression="${project.name}"
     */
    private String projectName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
            // initialize empty multi-project inventory
            Inventory multiProjectInventory = new Inventory();
            File inventory = new File(targetInventoryPath);

            // scan project for other produced inventory files
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(getProject().getBasedir());
            scanner.setIncludes(scanIncludes);
            scanner.scan();

            // iterate the scanned files, read and extend.
            for (String file : scanner.getIncludedFiles()) {
                getLog().warn("Integrating content of inventory: " + file);
                File inventoryToRead = new File(scanner.getBasedir(), file);
                // skip the file that we are about to write (from a previous run)
                if (!inventoryToRead.equals(inventory)) {
                    Inventory readInventory = new GlobalInventoryReader().readInventory(inventoryToRead);
                    extendMultiProjectReport(multiProjectInventory, readInventory);
                }
            }

            // we write the resulting inventory
            writeMultiProjectInventory(multiProjectInventory, inventory);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            MavenLogAdapter.release();
        }
    }

    private void extendMultiProjectReport(Inventory multiProjectInventory, Inventory inventory) throws IOException {
        // update / extend project name
        for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {
            artifact.getProjects().clear();
            artifact.addProject(getProjectName());
        }

        // merge content
        multiProjectInventory.getArtifacts().addAll(inventory.getArtifacts());
        multiProjectInventory.mergeDuplicates();
    }

    private void writeMultiProjectInventory(Inventory multiProjectInventory, File inventory) throws IOException {
        if (multiProjectInventory != null && inventory != null) {
            // ensure that the target folder exists (this may not be the case due to the build 
            // sequence and goals invoked)
            if (inventory.getParentFile() != null) {
                inventory.getParentFile().mkdirs();
            }
            new InventoryWriter().writeInventory(multiProjectInventory, inventory);
            
            // check whether reading is possible
            try {
                new GlobalInventoryReader().readInventory(inventory);
            } catch (IOException e) {
                System.err.println("Inventory file corrupted after save. Please analyze and report this"
                    + " error.");
                inventory.deleteOnExit();
            }
        }
    }

    public String getProjectName() {
        return projectName;
    }
    
}
