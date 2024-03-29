/*
 * Copyright 2009-2024 the original author or authors.
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

import java.io.File;

public class InventoryScanReport extends InventoryReport {

    private File inputDirectory;

    private File scanDirectory;

    private String[] scanIncludes = new String[]{"**/*"};

    private String[] scanExcludes;

    private boolean enableImplicitUnpack = true;

    private boolean enableDetectComponentPatterns = true;

    /**
     * Whether to include embedded POMs in the analysis. Defaults to false.
     */
    private boolean includeEmbedded = false;

    @Override
    public boolean createReport() throws Exception {
        Inventory globalInventory = readGlobalInventory();

        DirectoryInventoryScan directoryScan = new DirectoryInventoryScan(
                inputDirectory, scanDirectory, scanIncludes, scanExcludes, globalInventory);
        directoryScan.setEnableImplicitUnpack(isEnableImplicitUnpack());
        directoryScan.setIncludeEmbedded(isIncludeEmbedded());
        directoryScan.setEnableDetectComponentPatterns(isEnableDetectComponentPatterns());

        final Inventory scanInventory = directoryScan.createScanInventory();

        return createReport(globalInventory, scanInventory);
    }

    @Override
    public boolean createReport(Inventory globalInventory, Inventory scanInventory) throws Exception {
        if (globalInventory == null) globalInventory = readGlobalInventory();

        // insert (potentially incomplete) artifacts for reporting
        // this supports adding licenses and obligations into the
        // report, which are not covered by the repository.
        if (getAddOnArtifacts() != null) {
            scanInventory.getArtifacts().addAll(getAddOnArtifacts());
        }

        // produce reports and write files to disk
        return super.createReport(globalInventory, scanInventory);
    }

    public void setScanDirectory(File scanDirectory) {
        this.scanDirectory = scanDirectory;
    }

    public void setInputDirectory(File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    public void setScanIncludes(String[] scanIncludes) {
        this.scanIncludes = scanIncludes;
    }

    public void setScanExcludes(String[] scanExcludes) {
        this.scanExcludes = scanExcludes;
    }

    public void setEnableImplicitUnpack(boolean enableImplicitUnpack) {
        this.enableImplicitUnpack = enableImplicitUnpack;
    }

    public void setIncludeEmbedded(boolean includeEmbedded) {
        this.includeEmbedded = includeEmbedded;
    }

    public void setEnableDetectComponentPatterns(boolean enableDetectComponentPatterns) {
        this.enableDetectComponentPatterns = enableDetectComponentPatterns;
    }

    public boolean isEnableDetectComponentPatterns() {
        return enableDetectComponentPatterns;
    }

    public boolean isEnableImplicitUnpack() {
        return enableImplicitUnpack;
    }

    public String[] getScanExcludes() {
        return scanExcludes;
    }

    public String[] getScanIncludes() {
        return scanIncludes;
    }

    public File getScanDirectory() {
        return scanDirectory;
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public boolean isIncludeEmbedded() {
        return includeEmbedded;
    }

}
