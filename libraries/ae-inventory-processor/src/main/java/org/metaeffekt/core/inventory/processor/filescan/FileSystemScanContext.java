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
package org.metaeffekt.core.inventory.processor.filescan;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for context information during file system scanning.
 */
public class FileSystemScanContext {

    /**
     * The baseDir of the scan.
     */
    @Getter
    private final FileRef baseDir;

    /**
     * The {@link FileSystemScanParam} instance contains information on includes and excludes to different file-level operations.
     */
    @Getter
    private final FileSystemScanParam scanParam;

    /**
     * This inventory is filled with details during scan.
     */
    @Getter
    private final Inventory inventory;

    /**
     * Listeners are used to integrate with the FileSystemScanning framework and allow derived tasks to be triggered.
     */
    @Setter
    private FileSystemScanTaskListener scanTaskListener;

    /**
     * Virtual contexts are used to get the virtual root path of a file.
     */
    @Getter
    private final VirtualContext virtualContext;

    @Getter
    private final Map<String, String> pathToAssetIdMap = new ConcurrentHashMap<>();

    private volatile boolean acceptingNewTasks = true;

    public FileSystemScanContext(FileRef baseDir, FileSystemScanParam scanParam) {
        this.baseDir = baseDir;
        this.scanParam = scanParam;
        this.virtualContext = new VirtualContext(baseDir);
        this.inventory = new Inventory();
    }

    public synchronized void push(ScanTask scanTask) {
        if (scanTaskListener != null && acceptingNewTasks) {
            scanTaskListener.notifyOnTaskPushed(scanTask);
        }
    }

    /**
     * Adds the artifact to the managed inventory. No deduplication is performed.
     *
     * @param artifact The {@link Artifact} to contribute.
     */
    public void contribute(final Artifact artifact) {

        // check scan invariants
        String errorMsg = null;
        if (artifact == null) {
            errorMsg = "Artifact <null> contributed to scan inventory.";
        } else {
            if (StringUtils.isBlank(artifact.getId())) {
                errorMsg = "Artifact with empty id contributed to scan inventory.";
            }
        }

        if (errorMsg != null) {
            throw new IllegalStateException(errorMsg);
        }

        synchronized (inventory) {
            inventory.getArtifacts().add(artifact);
        }
    }

    public void removeAll(List<Artifact> toBeDeleted) {
        synchronized (inventory) {
            inventory.getArtifacts().removeAll(toBeDeleted);
        }
    }

    public void contribute(ComponentPatternData componentPatternData) {
        synchronized (inventory) {
            inventory.getComponentPatternData().add(componentPatternData);
        }
    }

    public void stopAcceptingNewTasks() {
        acceptingNewTasks = false;
    }

}
