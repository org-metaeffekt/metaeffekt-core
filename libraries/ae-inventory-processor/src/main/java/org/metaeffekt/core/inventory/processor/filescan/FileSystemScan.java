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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSystemScan {

    private final Inventory inventory;

    private final File baseDir;

    private final ScanParam scanParam;

    private ScanTaskListener scanTaskListener;

    private List<ScanTask> taskQueue = new ArrayList<>();

    public FileSystemScan(File baseDir, ScanParam scanParam) {
        this.baseDir = baseDir;
        this.scanParam = scanParam;

        this.inventory = new Inventory();
    }

    public synchronized void push(ScanTask scanTask) {
        taskQueue.add(scanTask);
        if (scanTaskListener != null) {
            scanTaskListener.notifyOnTask(scanTask);;
        }
    }

    public synchronized void pop(ScanTask scanTask) {
        taskQueue.remove(scanTask);

        if (taskQueue.isEmpty()) {
            if (scanTaskListener != null) {
                scanTaskListener.notifyEmptyQueue();
            }
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public ScanParam getScanParam() {
        return scanParam;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setScanTaskListener(ScanTaskListener scanTaskListener) {
        this.scanTaskListener = scanTaskListener;
    }

    public void trigger() {
        push(new DirectoryScanTask(baseDir));
    }
}
