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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class FileSystemScanExecutor implements FileSystemScanTaskListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemScanExecutor.class);

    private final FileSystemScanContext fileSystemScanContext;

    private ExecutorService executor;

    private final Map<ScanTask, Future<?>> monitor;

    public FileSystemScanExecutor(FileSystemScanContext fileSystemScan) {
        this.fileSystemScanContext = fileSystemScan;
        fileSystemScan.setScanTaskListener(this);
        this.monitor = new ConcurrentHashMap<>();
    }

    public void execute() throws IOException {
        LOG.info("Triggering scan on {}...", fileSystemScanContext.getBaseDir().getPath());

        this.executor = Executors.newFixedThreadPool(4);

        // trigger an initial scan from basedir
        fileSystemScanContext.trigger();

        LOG.info("Awaiting triggered tasks to finish.");
        awaitTasks();

        // we use the scan parameter and the covered inventory to control the process.

        boolean iteration = true;
        while (iteration) {
            LOG.info("Collecting derived outstanding tasks.");
            iteration = false;

            // wait fo existing scan tasks to finish
            awaitTasks();

            // TODO: check invariants
            //  * either SCAN_DIRECTIVE=delete OR checksum != null
            //  * ASSET_ID_CHAIN?

            // collect tasks based on the current inventory (process markers)
            final List<ScanTask> scanTasks = collectOutstandingScanTasks();

            // push tasks for being processed and mark for another iteration
            LOG.info("Triggering derived outstanding tasks.");
            if (!scanTasks.isEmpty()) {
                scanTasks.forEach(fileSystemScanContext::push);
                iteration = true;
            }
        }

        executor.shutdown();

        final Inventory implicitRereferenceInventory = new Inventory();

        // TODO: move context into producer (constructor argument)
        final ComponentPatternProducer patternProducer = new ComponentPatternProducer();
        patternProducer.detectAndApplyComponentPatterns(implicitRereferenceInventory, fileSystemScanContext);

        LOG.info("Scan completed.");
    }

    private List<ScanTask> collectOutstandingScanTasks() {
        final List<ScanTask> scanTasks = new ArrayList<>();

        final Inventory inventory = fileSystemScanContext.getInventory();
        final Inventory referenceInventory = fileSystemScanContext.getScanParam().getReferenceInventory();

        final ComponentPatternProducer componentPatternProducer = new ComponentPatternProducer();

        synchronized (inventory) {
            // match and apply component patterns before performing unwrapping
            // only anticipates predefined component patterns
            componentPatternProducer.matchAndApplyComponentPatterns(referenceInventory, fileSystemScanContext);

            for (Artifact artifact : inventory.getArtifacts()) {
                if (!StringUtils.isEmpty(artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_UNWRAP))) {

                    // TODO: exclude artifacts removed (though collection in component)
                    scanTasks.add(new ArtifactUnwrapTask(artifact, null));
                }
            }
        }

        return scanTasks;
    }


    public void awaitTasks() {
        Collection<Future<?>> values = new HashSet<>(monitor.values());
        while (!values.isEmpty()) {
            try {
                for (Future<?> future : values) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                // nothing to do
            }
            values = new HashSet<>(monitor.values());
        }
    }

    @Override
    public void notifyOnTaskPushed(ScanTask scanTask) {
        final Future<?> future = executor.submit(() -> {
            try {
                scanTask.process(fileSystemScanContext);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                notifyOnTaskPopped(scanTask);
            }
        });
        monitor.put(scanTask, future);
    }

    @Override
    public void notifyOnTaskPopped(ScanTask scanTask) {
        monitor.remove(scanTask);
    }


}
