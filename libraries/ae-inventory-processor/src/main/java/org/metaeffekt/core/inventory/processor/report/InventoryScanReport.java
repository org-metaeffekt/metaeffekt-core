/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.io.File;

public class InventoryScanReport extends InventoryReport {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryScanReport.class);

    private File inputDirectory;

    private File scanDirectory;

    private String[] scanIncludes = new String[]{"**/*"};

    private String[] scanExcludes;

    @Override
    public boolean createReport() throws Exception {
        Inventory globalInventory = readGlobalInventory();

        DirectoryInventoryScan directoryScan = new DirectoryInventoryScan(inputDirectory, scanDirectory, scanIncludes, scanExcludes, globalInventory);
        Inventory scanInventory = directoryScan.createScanInventory();

        // insert (potentially incomplete) artifacts for reporting
        // this supports adding licenses and obligations into the
        // report, which are not covered by the repository.
        if (getAddOnArtifacts() != null) {
            scanInventory.getArtifacts().addAll(getAddOnArtifacts());
        }

        // produce reports and write files to disk
        return createReport(globalInventory, scanInventory);
    }


    public File getScanDirectory() {
        return scanDirectory;
    }

    public void setScanDirectory(File scanDirectory) {
        this.scanDirectory = scanDirectory;
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public void setInputDirectory(File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    public String[] getScanIncludes() {
        return scanIncludes;
    }

    public void setScanIncludes(String[] scanIncludes) {
        this.scanIncludes = scanIncludes;
    }

    public String[] getScanExcludes() {
        return scanExcludes;
    }

    public void setScanExcludes(String[] scanExcludes) {
        this.scanExcludes = scanExcludes;
    }

}
