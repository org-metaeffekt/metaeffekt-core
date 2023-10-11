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
package org.metaeffekt.core.inventory.processor.report;

import org.metaeffekt.core.inventory.processor.command.PrepareScanDirectoryCommand;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanExecutor;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanParam;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DirectoryInventoryScan {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryInventoryScan.class);

    private final Inventory referenceInventory;

    private final String[] scanIncludes;

    private final String[] scanExcludes;

    private final String[] unwrapIncludes;

    private final String[] unwrapExcludes;

    private final File inputDirectory;

    private final File scanDirectory;

    private boolean enableImplicitUnpack = true;

    private boolean includeEmbedded = false;

    private boolean enableDetectComponentPatterns = false;

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
                      String[] scanIncludes, String[] scanExcludes, Inventory referenceInventory) {
        this (inputDirectory, scanDirectory,
                scanIncludes, scanExcludes, new String[] { "**/*" },
                new String[0], referenceInventory);
    }

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
                                  String[] scanIncludes, String[] scanExcludes,
                                  String[] unwrapIncludes, String[] unwrapExcludes,
                                  Inventory referenceInventory) {
        this.inputDirectory = inputDirectory;

        this.scanDirectory = scanDirectory;
        this.scanIncludes = scanIncludes;
        this.scanExcludes = scanExcludes;

        this.unwrapIncludes = unwrapIncludes;
        this.unwrapExcludes = unwrapExcludes;

        this.referenceInventory = referenceInventory;
    }

    public Inventory createScanInventory() {
        prepareScanDirectory();
        return performScan();
    }

    private void prepareScanDirectory() {
        // if the input directory is not set; the scan directory is not managed
        if (inputDirectory != null) {
            final PrepareScanDirectoryCommand prepareScanDirectoryCommand = new PrepareScanDirectoryCommand();
            prepareScanDirectoryCommand.prepareScanDirectory(inputDirectory, scanDirectory, scanIncludes, scanExcludes);
        }
    }

    public Inventory performScan() {
        final File directoryToScan = scanDirectory;

        final FileSystemScanParam scanParam = new FileSystemScanParam().
                collectAllMatching(scanIncludes, scanExcludes).
                unwrapAllMatching(unwrapIncludes, unwrapExcludes).
                implicitUnwrap(enableImplicitUnpack).
                includeEmbedded(includeEmbedded).
                detectComponentPatterns(enableDetectComponentPatterns).
                withReferenceInventory(referenceInventory);

        LOG.info("Scanning directory [{}]...", directoryToScan.getAbsolutePath());

        final FileSystemScanContext fileSystemScan = new FileSystemScanContext(new FileRef(directoryToScan), scanParam);
        final FileSystemScanExecutor fileSystemScanExecutor = new FileSystemScanExecutor(fileSystemScan);

        fileSystemScanExecutor.execute();

        // NOTE: at this point, the component is fully unwrapped in the file system (expecting already detected component
        //   patterns).

        LOG.info("Scanning directory [{}] completed.", directoryToScan.getAbsolutePath());

        return fileSystemScan.getInventory();
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

}
