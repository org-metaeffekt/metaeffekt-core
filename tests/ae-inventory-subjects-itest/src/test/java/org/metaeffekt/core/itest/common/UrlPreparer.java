package org.metaeffekt.core.itest.common;

import org.apache.commons.io.FileUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlPreparer extends AbstractPreparer {

    public boolean inventorize(boolean overwrite) throws Exception {
        String inventoryfile = getInventoryFolder()+"scan-inventory.ser";
        if (new File(inventoryfile).exists() && !overwrite) return true;

        FileUtils.deleteDirectory(new File(getInventoryFolder()));
        FileUtils.deleteDirectory(new File(getScanFolder()));

        new File(getScanFolder()).mkdirs();
        final File scanInputDir = new File(getDownloadFolder());
        final File scanDir = new File(getScanFolder());

        String[] scanIncludes = new String[]{"**/*"};
        String[] scanExcludes = new String[]{
                "**/.DS_Store", "**/._*",
                "**/.git/**/*", "**/.git*", "**/.git*"
        };

        String[] unwrapIncludes = new String[]{"**/*"};
        String[] unwrapExcludes = new String[]{
                "**/*.js.gz", "**/*.js.map.gz", "**/*.css.gz",
                "**/*.css.map.gz", "**/*.svg.gz", "**/*.json.gz",
                "**/*.ttf.gz", "**/*.eot.gz"
        };

        Inventory referenceInventory = readReferenceInventory();

        final DirectoryInventoryScan scan = new DirectoryInventoryScan(
                scanInputDir, scanDir,
                scanIncludes, scanExcludes,
                unwrapIncludes, unwrapExcludes,
                referenceInventory);

        scan.setIncludeEmbedded(true);
        scan.setEnableImplicitUnpack(true);
        scan.setEnableDetectComponentPatterns(true);

        final Inventory inventory = scan.createScanInventory();


        new File(getInventoryFolder()).mkdirs();
        new InventoryWriter().writeInventory(inventory, new File(getInventoryFolder()+"scan-inventory.xls"));
        new InventoryWriter().writeInventory(inventory, new File(inventoryfile));

        return true;
    }

    public boolean download(boolean overwrite) throws MalformedURLException {
        String[] filenameparts = url.split("/");
        String filename = filenameparts[filenameparts.length - 1];
        String artifactfile = getDownloadFolder()+ filename;
        if (overwrite || !new File(artifactfile).exists()) {
            new File(getDownloadFolder()).mkdirs();
                new WebAccess().fetchResponseBodyFromUrlToFile(new URL(url), new File(artifactfile));
        }
        return true;
    }

}
