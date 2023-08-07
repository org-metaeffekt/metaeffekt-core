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
