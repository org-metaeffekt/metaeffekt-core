package org.metaeffekt.core.maven.inventory.mojo;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryMergeUtilsTest {

    @Ignore
    @Test
    public void testMerge() throws IOException {
        Inventory targetInventory = new Inventory();
        File sourceInventoryFile = new File("<path to file>");

        List<File> sourceInventories = new ArrayList<>();
        sourceInventories.add(sourceInventoryFile);

        new InventoryMergeUtils().merge(sourceInventories, targetInventory);

        System.out.println(targetInventory.getLicenseData().size());

        Assert.assertTrue(targetInventory.getLicenseData().size() > 0);
    }


}