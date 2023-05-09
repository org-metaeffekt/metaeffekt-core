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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileSystemScanExecutor implements ScanTaskListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemScanExecutor.class);

    private final FileSystemScan fileSystemScan;

    final ExecutorService executor;

    public FileSystemScanExecutor(FileSystemScan fileSystemScan) {
        this.fileSystemScan = fileSystemScan;
        this.executor = Executors.newFixedThreadPool(4);
        fileSystemScan.setScanTaskListener(this);
    }

    public void execute() {
        LOG.info("Triggering scan...");

        fileSystemScan.trigger();
        awaitTerminationOrCancelOnException(executor);

        LOG.info("Scan completed.");
    }

    public static void awaitTerminationOrCancelOnException(ExecutorService executor) {
        // shutdown and wait until all commands have finished or an error occurred
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // nothing to do
            }
        }
    }

    @Override
    public void notifyOnTask(ScanTask scanTask) {
        executor.submit(() -> {
            try {
                scanTask.process(fileSystemScan);
            } finally {
                fileSystemScan.pop(scanTask);
            }
        });
    }

    @Override
    public void notifyEmptyQueue() {
        executor.shutdown();
    }
}
