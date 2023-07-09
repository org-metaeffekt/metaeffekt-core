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
import org.metaeffekt.core.inventory.processor.filescan.tasks.DirectoryScanTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.inspector.InspectorRunner;
import org.metaeffekt.core.inventory.processor.inspector.JarInspector;
import org.metaeffekt.core.inventory.processor.inspector.NestedJarInspector;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;
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

        // start with an empty assetIdChain on the root
        final ArrayList<String> assetIdChain = new ArrayList<>();

        // trigger an initial scan from basedir
        fileSystemScanContext.push(new DirectoryScanTask(fileSystemScanContext.getBaseDir(), assetIdChain));

        LOG.info("Awaiting triggered tasks to finish.");
        awaitTasks();

        // the scanner works in sequences
        boolean iteration = true;
        while (iteration) {

            LOG.info("Collecting derived outstanding tasks.");
            iteration = false;

            // wait for existing scan tasks to finish
            awaitTasks();

            // marks artifacts with scan classifier
            runNestedComponentInspection();

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

        // currently we detect component patterns on the final directory
        final ComponentPatternProducer patternProducer = new ComponentPatternProducer();
        final Inventory implicitRereferenceInventory = new Inventory();
        patternProducer.detectAndApplyComponentPatterns(implicitRereferenceInventory, fileSystemScanContext);

        // run remaining inspection last
        inspectArtifacts(fileSystemScanContext);

        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            String assetIdChainString = artifact.get(FileSystemScanConstants.ATTRIBUTE_KEY_ASSET_ID_CHAIN);
            if (StringUtils.isNotBlank(assetIdChainString)) {
                final String[] split = assetIdChainString.split("\\||\n");
                for (String assetPath : split) {
                    String assetId = fileSystemScanContext.getPathToAssetIdMap().get(assetPath);
                    if (StringUtils.isNotBlank(assetPath)) {
                        assetId = assetPath;
                    }
                    if (StringUtils.isNotBlank(assetId)) {
                        artifact.set(assetId, "x");
                    }
                }
            }

        }

        LOG.info("Scan completed.");
    }

    private void runNestedComponentInspection() {
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());

        final NestedJarInspector nestedJarInspector = new NestedJarInspector();

        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            boolean inspected = StringUtils.isNotBlank(artifact.get("INSPECTED"));
            if (!inspected) {

                // run executors here
                nestedJarInspector.run(artifact, properties);

                artifact.set("INSPECTED", "x");

                if (artifact.hasClassification(DirectoryInventoryScan.HINT_SCAN)) {
                    artifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_UNWRAP, "x");
                }
            }
        }
    }

    private List<ScanTask> collectOutstandingScanTasks() {
        final List<ScanTask> scanTasks = new ArrayList<>();

        final Inventory inventory = fileSystemScanContext.getInventory();
        final Inventory referenceInventory = fileSystemScanContext.getScanParam().getReferenceInventory();

        final ComponentPatternProducer componentPatternProducer = new ComponentPatternProducer();

        // match and apply component patterns before performing unwrapping
        // only anticipates predefined component patterns
        componentPatternProducer.matchAndApplyComponentPatterns(referenceInventory, fileSystemScanContext);

        for (Artifact artifact : inventory.getArtifacts()) {
            if (!StringUtils.isEmpty(artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_UNWRAP))) {

                // TODO: exclude artifacts removed (though collection in component)
                scanTasks.add(new ArtifactUnwrapTask(artifact, null));
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

    private void inspectArtifacts(FileSystemScanContext fileSystemScanContext) {
        // attempt to extract artifactId, version, groupId from contained POMs
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());
        properties.put(JarInspectionParam.KEY_INCLUDE_EMBEDDED,
                Boolean.toString(fileSystemScanContext.getScanParam().isIncludeEmbedded()));

        // run further inspections on identified artifacts
        final InspectorRunner runner = InspectorRunner.builder()
                .queue(JarInspector.class)
                .build();

        runner.executeAll(fileSystemScanContext.getInventory(), properties);

        final List<AssetMetaData> assetMetaDataList = fileSystemScanContext.getInventory().getAssetMetaData();

        if (assetMetaDataList != null) {
            for (AssetMetaData assetMetaData : assetMetaDataList) {
                final String path = assetMetaData.get("File Path");
                final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
                if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(assetId)) {
                    // FIXME; we may need a map  to list
                    if (fileSystemScanContext.getPathToAssetIdMap().get(path) == null) {
                        fileSystemScanContext.getPathToAssetIdMap().put(path, assetId);
                    }
                }
            }
        }
    }

}
