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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.AssessmentInventoryMerger;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Mojo(name = "merge-inventories-assessment", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class AssessmentInventoryMergeMojo extends AbstractMultipleInputInventoriesMojo {

    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final List<File> inputInventoryFiles = super.collectSourceInventories();

            final AssessmentInventoryMerger merger = new AssessmentInventoryMerger(inputInventoryFiles, Collections.emptyList());
            final Inventory mergedInventory = merger.mergeInventories();

            // create target directory if not existing yet
            final File targetParentFile = this.targetInventory.getParentFile();
            if (targetParentFile != null && !this.targetInventory.exists()) {
                targetParentFile.mkdirs();
            }

            // write inventory to target location
            new InventoryWriter().writeInventory(mergedInventory, this.targetInventory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
