package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "infer-inventory-details", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class InventoryInferenceMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * The root inventory dir.
     *
     * @parameter
     * @required
     */
    protected File sourceInventoryDir;

    /**
     * Includes of the source inventory; relative to the sourceInventoryDir.
     *
     * @parameter default-value="*.xls*"
     * @required
     */
    protected String sourceInventoryIncludes;

    /**
     * Location of components relative to the sourceInventoryDir.
     *
     * @parameter default-value="components"
     * @required
     */
    protected String sourceComponentPath;

    /**
     * Location of licenses relative to the sourceInventoryDir.
     *
     * @parameter default-value="licenses"
     * @required
     */
    protected String sourceLicensePath;


    @Parameter(required = true)
    protected File sourceInventory;

    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Inventory sourceInv = new InventoryReader().readInventory(sourceInventory);
            Inventory targetInv = new InventoryReader().readInventory(targetInventory);

            new InventoryWriter().writeInventory(targetInv, targetInventory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
