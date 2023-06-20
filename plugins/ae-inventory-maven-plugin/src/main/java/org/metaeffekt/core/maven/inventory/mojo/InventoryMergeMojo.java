package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Merges inventories. Reference inventories are usually the target inventory. Otherwise this Mojo is intended to
 * be applied to inventories from the extraction process, only.
 */
@Mojo(name = "merge-inventories", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class InventoryMergeMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * The source inventory.
     */
    @Parameter(required = false)
    protected File sourceInventory;

    /**
     * Alternatively to the sourceInventory parameter a directory can be specified. The inventory is scanned for files
     * using sourceInventoryIncludes and sourceInventoryExcludes.
     */
    @Parameter(required = false)
    protected File sourceInventoryBaseDir;

    /**
     * Includes based on sourceInventoryBaseDir.
     */
    @Parameter(defaultValue = "**/*.xls")
    protected String sourceInventoryIncludes;

    /**
     * Excludes based on sourceInventoryBaseDir.
     */
    @Parameter
    protected String sourceInventoryExcludes;

    /**
     * Boolean indicating whether to include the default artifact excluded attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactExcludedAttributes = true;

    /**
     * The excluded attributes will be dropped when merging. The list may also be considered
     * as selection of attributes, which must not be differentiated by the merge process.
     */
    @Parameter(required = false)
    protected Set<String> artifactExcludedAttributes = new HashSet<>();

    /**
     * Boolean indicating whether to include the default artifact merge attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactMergeAttributes = true;

    /**
     * The merge attributes will be merged on attribute level. Also these attributes are not
     * differentiated by the merge process.
     */
    @Parameter(required = false)
    protected Set<String> artifactMergeAttributes = new HashSet<>();

    /**
     * The target inventory. The parameter must be specified. However the target must not exist. If the target inventory
     * does not exist the complete source inventory is copied into the target location.
     */
    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            final List<File> sourceInventories = new ArrayList<>();

            if (sourceInventory != null) {
                if (sourceInventory.exists() && sourceInventory.isFile()) {
                    sourceInventories.add(sourceInventory);
                } else {
                    throw new MojoExecutionException("The parameter [sourceInventory] is set, but the file does not exist: " +
                            sourceInventory.getAbsolutePath());
                }
            }

            if (sourceInventoryBaseDir != null) {
                if (sourceInventoryBaseDir.exists() && sourceInventoryBaseDir.isDirectory()) {
                    final String[] sourceFiles = FileUtils.scanForFiles(sourceInventoryBaseDir,
                            sourceInventoryIncludes, sourceInventoryExcludes);
                    Arrays.stream(sourceFiles).forEach(f -> sourceInventories.add(new File(sourceInventoryBaseDir, f)));
                } else {
                    throw new MojoExecutionException("The parameter [sourceInventoryBaseDir] parameter is set, but the directory does not exist: " +
                            sourceInventoryBaseDir.getAbsolutePath());
                }
            }

            // if the target does not exist we initiate a new inventory
            final Inventory targetInventory = this.targetInventory.exists() ?
                    new InventoryReader().readInventory(this.targetInventory) : new Inventory();

            final InventoryMergeUtils inventoryMergeUtils = new InventoryMergeUtils();

            inventoryMergeUtils.addDefaultArtifactExcludedAttributes = addDefaultArtifactExcludedAttributes;
            inventoryMergeUtils.artifactExcludedAttributes = artifactMergeAttributes;

            inventoryMergeUtils.addDefaultArtifactMergeAttributes = addDefaultArtifactMergeAttributes;
            inventoryMergeUtils.artifactMergeAttributes = artifactMergeAttributes;

            inventoryMergeUtils.merge(sourceInventories, targetInventory);

            // create target directory if not existing yet
            final File targetParentFile = this.targetInventory.getParentFile();
            if (targetParentFile != null && !this.targetInventory.exists()) {
                targetParentFile.mkdirs();
            }

            // write inventory to target location
            new InventoryWriter().writeInventory(targetInventory, this.targetInventory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
