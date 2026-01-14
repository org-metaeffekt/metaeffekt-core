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
import org.metaeffekt.core.inventory.InventoryMergeUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges inventories. Reference inventories are usually the target inventory. Otherwise, this Mojo is intended to
 * be applied to inventories from the extraction process, only.
 */
@Mojo(name = "merge-inventories", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class InventoryMergeMojo extends AbstractMultipleInputInventoriesMojo {

    /**
     * Boolean indicating whether to include the default artifact excluded attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactExcludedAttributes = true;

    /**
     * The excluded attributes will be dropped when merging. The list may also be considered
     * as selection of attributes, which must not be differentiated by the merge process.
     */
    @Parameter()
    protected Set<String> artifactExcludedAttributes = new HashSet<>();

    /**
     * Boolean indicating whether to include the default artifact merge attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactMergeAttributes = true;

    /**
     * The merge attributes will be merged on attribute level. Also, these attributes are not
     * differentiated by the merge process.
     */
    @Parameter()
    protected Set<String> artifactMergeAttributes = new HashSet<>();

    /**
     * The target inventory. The parameter must be specified. However, the target must not exist. If the target inventory
     * does not exist the complete source inventory is copied into the target location.
     */
    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            final List<File> sourceInventories = super.collectSourceInventories();

            // if the target does not exist we initiate a new inventory
            final Inventory targetInventory = this.targetInventory.exists() ?
                    new InventoryReader().readInventory(this.targetInventory) : new Inventory();

            final InventoryMergeUtils inventoryMergeUtils = new InventoryMergeUtils();

            inventoryMergeUtils.setAddDefaultArtifactExcludedAttributes(addDefaultArtifactExcludedAttributes);
            inventoryMergeUtils.setArtifactExcludedAttributes(artifactExcludedAttributes);

            inventoryMergeUtils.setAddDefaultArtifactMergeAttributes(addDefaultArtifactMergeAttributes);
            inventoryMergeUtils.setArtifactMergeAttributes(artifactMergeAttributes);

            inventoryMergeUtils.merge(sourceInventories, targetInventory);

            // create target directory if not existing yet
            final File targetParentFile = this.targetInventory.getParentFile();
            if (targetParentFile != null && !this.targetInventory.exists()) {
                FileUtils.forceMkdir(targetParentFile);
            }

            // write inventory to target location
            new InventoryWriter().writeInventory(targetInventory, this.targetInventory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
