package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is an abstract class that extends the AbstractProjectAwareConfiguredMojo class.<br>
 * It provides the common functionalities for handling multiple input inventories for maven plugins.
 */
public abstract class AbstractMultipleInputInventoriesMojo extends AbstractProjectAwareConfiguredMojo {

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

    protected List<File> collectSourceInventories() throws MojoExecutionException {
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

        return sourceInventories;
    }
}
