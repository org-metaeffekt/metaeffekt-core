/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.itest.common.setup;

import org.apache.commons.io.FileUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.itest.common.download.WebAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlBasedTestSetup extends AbstractTestSetup {

    private final Logger LOG = LoggerFactory.getLogger(UrlBasedTestSetup.class);


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

    private void httpDownload(String artifactfile) throws MalformedURLException {
        new WebAccess().fetchResponseBodyFromUrlToFile(new URL(url), new File(artifactfile));
    }

    private void filecopy(String artifactfile) throws IOException {
        File source = new File(new URL(url).getFile());
        File target = new File(artifactfile);
        FileUtils.copyFile(source,target);
    }

    public boolean load(boolean overwrite) throws IOException {
        String[] filenameparts = url.split("/");
        String filename = filenameparts[filenameparts.length - 1];
        String artifactfile = getDownloadFolder()+ filename;
        if (overwrite || !new File(artifactfile).exists()) {
            new File(getDownloadFolder()).mkdirs();
            if(url.startsWith("http"))
                httpDownload(artifactfile);
            if(url.startsWith("file"))
                filecopy(artifactfile);
        }
        return new File(artifactfile).exists();
    }

}
