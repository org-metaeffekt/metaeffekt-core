package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class InventoryMergeMojoTest {

    @Ignore
    @Test
    public void testMerge() throws MojoExecutionException, MojoFailureException {

        InventoryMergeMojo mojo = new InventoryMergeMojo();
        mojo.sourceInventoryBaseDir = new File("<path-to-source-inventory>");
        mojo.targetInventory = new File("<path-to-target-inventory>");

        mojo.execute();
    }

}