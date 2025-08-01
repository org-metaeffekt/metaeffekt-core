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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.DirectoryScanTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.FileCollectTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.inspector.*;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class FileSystemScanExecutor implements FileSystemScanTaskListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemScanExecutor.class);

    private final FileSystemScanContext fileSystemScanContext;

    private ExecutorService executor;

    private final ConcurrentMap<ScanTask, Future<?>> monitor;

    private final AtomicBoolean iteration = new AtomicBoolean(true);

    public static ThreadLocal<Boolean> isExecutorThread = new ThreadLocal<>();

    public FileSystemScanExecutor(FileSystemScanContext fileSystemScan) {
        this.fileSystemScanContext = fileSystemScan;
        fileSystemScan.setScanTaskListener(this);
        this.monitor = new ConcurrentHashMap<>();
    }

    public void execute() {
        this.executor = Executors.newFixedThreadPool(4);

        // start with an empty assetIdChain on the root
        final ArrayList<String> assetIdChain = new ArrayList<>();

        // trigger an initial scan from basedir
        fileSystemScanContext.push(new DirectoryScanTask(fileSystemScanContext.getBaseDir(), assetIdChain));

        awaitTasks();

        // the scanner works in sequences
        while (iteration.get()) {
            iteration.set(false);

            // wait for existing scan tasks to finish
            awaitTasks();

            // marks artifacts with scan classifier
            runNestedComponentInspection();

            // TODO: check invariants
            //  * either SCAN_DIRECTIVE=delete OR checksum != null
            //  * ASSET_ID_CHAIN?

            // apply static component patterns
            applyStaticComponentPatterns(false);

            // collect tasks based on the current inventory (process markers)
            final List<ScanTask> scanTasks = collectOutstandingScanTasks();

            // push tasks for being processed and mark for another iteration
            if (!scanTasks.isEmpty()) {
                LOG.info("Triggering {} outstanding tasks...", scanTasks.size());
                scanTasks.forEach(fileSystemScanContext::push);
                iteration.set(true);
            }
        }

        awaitTasks();
        executor.shutdown();

        fileSystemScanContext.stopAcceptingNewTasks();

        if (fileSystemScanContext.getScanParam().isDetectComponentPatterns()) {
            // currently we detect component patterns on the final directory
            final ComponentPatternProducer patternProducer = new ComponentPatternProducer();
            final Inventory implicitRereferenceInventory = new Inventory();
            patternProducer.detectAndApplyComponentPatterns(implicitRereferenceInventory, fileSystemScanContext);
        }
        
        // second run static component patterns; run deferred set
        applyStaticComponentPatterns(true);

        // run remaining inspection last
        inspectArtifacts();

        setArtifactAssetMarker();

        final Inventory inventory = fileSystemScanContext.getInventory();

        final boolean removeIntermediateAttributes = true;
        if (removeIntermediateAttributes) {
            InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_INSPECTED, inventory);
            InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_SCAN_DIRECTIVE, inventory);
            InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ARTIFACT_PATH, inventory);
            InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ASSET_ID_CHAIN, inventory);
            InventoryUtils.removeArtifactAttribute(AssetMetaData.Attribute.ASSET_PATH.getKey(), inventory);
            InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_COMPONENT_PATTERN_MARKER, inventory);
            InventoryUtils.removeArtifactAttribute(FileCollectTask.ATTRIBUTE_KEY_ANCHOR, inventory);
        }

        mergeDuplicates(inventory);
    }

    private void setArtifactAssetMarker() {
        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            final String assetIdChainString = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);

            if (StringUtils.isNotBlank(assetIdChainString) && !(".").equals(assetIdChainString)) {
                final String[] split = assetIdChainString.split("\\|\n");
                for (String assetPath : split) {
                    String assetId = fileSystemScanContext.getPathToAssetIdMap().get(assetPath);

                    // fallback to asset path; more as a processing failure indication
                    if (StringUtils.isBlank(assetId)) {
                        LOG.warn("Cannot resolve asset id for path " + assetPath);
                    } else {
                        artifact.set(assetId, MARKER_CONTAINS);
                    }
                }
            }
        }
    }

    private void runNestedComponentInspection() {
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());

        final NestedJarInspector nestedJarInspector = new NestedJarInspector();

        for (Artifact artifact : fileSystemScanContext.getArtifactList()) {
            boolean inspected = StringUtils.isNotBlank(artifact.get(ATTRIBUTE_KEY_INSPECTED));
            if (!inspected) {

                // run executors here
                nestedJarInspector.run(artifact, properties);

                artifact.set(ATTRIBUTE_KEY_INSPECTED, MARKER_CROSS);

                if (artifact.hasClassification(HINT_SCAN)) {
                    artifact.set(ATTRIBUTE_KEY_UNWRAP, MARKER_CROSS);
                }
            }
        }
    }

    private List<ScanTask> collectOutstandingScanTasks() {

        final List<ScanTask> scanTasks = new ArrayList<>();

        for (Artifact artifact : fileSystemScanContext.getArtifactList()) {
            if (!StringUtils.isEmpty(artifact.get(ATTRIBUTE_KEY_UNWRAP))) {

                // TODO: exclude artifacts removed (though collection in component)
                scanTasks.add(new ArtifactUnwrapTask(artifact, null));
            }
        }

        return scanTasks;
    }

    private void applyStaticComponentPatterns(boolean applyDeferred) {
        final Inventory referenceInventory = fileSystemScanContext.getScanParam().getReferenceInventory();

        final ComponentPatternProducer componentPatternProducer = new ComponentPatternProducer();

        // match and apply component patterns before performing unwrapping
        // only anticipates predefined component patterns
        componentPatternProducer.matchAndApplyComponentPatterns(referenceInventory, fileSystemScanContext, applyDeferred);
    }

    public void awaitTasks() {
        Collection<Future<?>> futures = new HashSet<>(monitor.values());
        while (!futures.isEmpty()) {
            boolean allDone = true;
            for (Future<?> future : futures) {
                if (future.isCancelled()) continue;
                if (future.isDone()) continue;
                allDone = false;
            }
            if (!allDone) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                futures = new HashSet<>(monitor.values());
            } else {
                break;
            }
        }
    }

    @Override
    public void notifyOnTaskPushed(ScanTask scanTask) {
        final Future<?> future = executor.submit(() -> {
            isExecutorThread.set(true);
            try {
                scanTask.process(fileSystemScanContext);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } catch (Error error) {
                LOG.error(error.getMessage(), error);
                System.exit(-1);
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

    private void inspectArtifacts() {
        // attempt to extract artifactId, version, groupId from contained POMs
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());
        properties.put(JarInspectionParam.KEY_INCLUDE_EMBEDDED,Boolean.toString(fileSystemScanContext.getScanParam().isIncludeEmbedded()));

        // run further inspections on identified artifacts
        final InspectorRunner runner = InspectorRunner.builder()
                .queue(JarInspector.class)
                .queue(RpmMetadataInspector.class)
                .queue(InventoryInspector.class)
                .build();

        runner.executeAll(fileSystemScanContext.getInventory(), properties);

        final List<AssetMetaData> assetMetaDataList = fileSystemScanContext.getAssetMetaDataList();

        if (assetMetaDataList != null) {
            for (AssetMetaData assetMetaData : assetMetaDataList) {
                if (assetMetaData != null) {
                    final String path = assetMetaData.get(AssetMetaData.Attribute.ASSET_PATH.getKey());
                    final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
                    if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(assetId)) {
                        // FIXME: we may need a map to list; validated; refers to putIfAbsent
                        fileSystemScanContext.getPathToAssetIdMap().putIfAbsent(path, assetId);
                    }
                } else {
                    LOG.warn("Potential concurrency issue detected in AssetMetaData object.");
                }
            }
        } else {
            LOG.warn("Potential concurrency issue detected in AssetMetaData list.");
        }
    }

    public void mergeDuplicates(Inventory inventory) {

        modulateArtifactRootPaths(inventory);

        // NOTE: the list may contain duplicates by reference; these must be removed to not lose data
        final List<Artifact> artifacts = inventory.getArtifacts();
        final LinkedHashSet<Artifact> artifactSet = new LinkedHashSet<>(artifacts);
        if (artifactSet.size() != artifacts.size()) {
            LOG.warn("Detected duplicates by reference in inventory. Ensure analysis does not produce referential duplicates. Applying compensation.");
            artifacts.clear();
            artifacts.addAll(artifactSet);
        }

        final Map<String, List<Artifact>> qualifierArtifactMap = buildQualifierArtifactMap(inventory);

        for (List<Artifact> list : qualifierArtifactMap.values()) {
            final Artifact representativeArtifact = list.get(0);

            for (int i = 1; i < list.size(); i++) {
                final Artifact duplicateArtifact = list.get(i);
                if (representativeArtifact == duplicateArtifact) {
                    throw new IllegalStateException("Unresolved referential duplicate detected.");
                }
                artifacts.remove(duplicateArtifact);
                representativeArtifact.merge(duplicateArtifact);
            }
        }

        InventoryUtils.mergeDuplicateAssets(inventory);

        mergeRedundantContainerArtifacts(inventory);
    }

    /**
     * Normalize PATH_IN_ASSET. Only strips-off square brackets from last element. Does not introduce
     * anything new.
     *
     * @param inventory The inventory with artifacts to normalize.
     */
    private static void modulateArtifactRootPaths(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            Set<String> modulatedSet = new HashSet<>();
            final Set<String> relativePaths = artifact.getRootPaths();
            for (String path : relativePaths) {
                if (!StringUtils.isBlank(path)) {
                    final String modulatedRelativePath = stripSquareBraketsFromLastElement(path);
                    modulatedSet.add(modulatedRelativePath);
                }
            }
            artifact.setRootPaths(modulatedSet);
        }
    }

    private static String stripSquareBraketsFromLastElement(String relativePath) {
        final File file = new File(relativePath);
        String name = file.getName();
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        String path = "";
        if (file.getParentFile() != null) {
            path = file.getParentFile().getPath() + "/";
        }
        final String modulatedRelativePath = path + name;
        return modulatedRelativePath;
    }

    private void mergeRedundantContainerArtifacts(Inventory inventory) {
        // additionally merge scanned / decomposed artifacts
        final Set<Artifact> artifactsWithScanClassification = inventory.getArtifacts().stream()
                .filter(a -> a.hasClassification(HINT_SCAN)).collect(Collectors.toSet());

        for (Artifact scannedArtifact : artifactsWithScanClassification) {
            final String assetId = "AID-" + scannedArtifact.getId() + "-" + scannedArtifact.getChecksum();

            final File pathInAsset = new File(scannedArtifact.get(Artifact.Attribute.PATH_IN_ASSET));
            final String name = pathInAsset.getName();
            final File extractedPathInAsset = new File(pathInAsset.getParentFile(), "[" + name + "]");

            final String qualifier = scannedArtifact.getId() + "/" + extractedPathInAsset.getPath();

            final Set<Artifact> toBeDeleted = new HashSet<>();
            for (Artifact artifact : inventory.getArtifacts()) {
                final String q = deriveContainedArtifactQualifier(artifact);
                if (qualifier.equals(q)) {
                    if ("c".equalsIgnoreCase(artifact.get(assetId))) {
                        // manage attributes that should not be merged
                        artifact.set(assetId, null);
                        artifact.setRootPaths(Collections.emptySet());

                        // merge contained artifact into scanned
                        scannedArtifact.merge(artifact);

                        // collect merged sub-artifact to be deleted
                        toBeDeleted.add(artifact);
                    }
                }
            }
            inventory.getArtifacts().removeAll(toBeDeleted);
        }
    }

    private String deriveContainedArtifactQualifier(Artifact a) {
        return a.getId() + "/" + a.get(Artifact.Attribute.PATH_IN_ASSET);
    }

    public Map<String, List<Artifact>> buildQualifierArtifactMap(Inventory inventory) {
        final Map<String, List<Artifact>> qualifierArtifactMap = new LinkedHashMap<>();

        for (final Artifact artifact : inventory.getArtifacts()) {
            final Set<String> artifactQualifiers = qualifiersOf(artifact);
            for (String qualifier : artifactQualifiers) {
                final List<Artifact> artifacts = qualifierArtifactMap.computeIfAbsent(qualifier, a -> new ArrayList<>());
                artifacts.add(artifact);
            }
        }
        return qualifierArtifactMap;
    }

    /**
     * Produces multiple qualifiers that - if matching - map the same artifacts and can be merged.
     *
     * @param artifact The artifact to produce the qualifiers for.
     *
     * @return Set of qualifiers for the given artifact.
     */
    public Set<String> qualifiersOf(Artifact artifact) {
        final Set<String> qualifiers = new HashSet<>();

        final String id = artifact.getId();

        // id + checksum
        if (StringUtils.isNotBlank(artifact.getChecksum())) {
            // in case we have a checksum id and checksum are sufficient
            qualifiers.add("i:[" + id + "]-c:[" + artifact.getChecksum() + "]");
        }

        // id + gv
        if (StringUtils.isNotBlank(artifact.getGroupId()) && StringUtils.isNotBlank(artifact.getVersion())) {
            qualifiers.add("i:[" + id + "]-g:[" + artifact.getGroupId() + "]-v:[" + artifact.getVersion() + "]");
        }

        // id + PATH_IN_ASSET
        final String pathInAsset = artifact.get(KEY_PATH_IN_ASSET);
        if (StringUtils.isNotBlank(pathInAsset)) {
            // create qualifier
            qualifiers.add("i:[" + id + "]-p:[" + pathInAsset + "]");
        }

        // id + version + purl
        if (StringUtils.isNotBlank(artifact.getVersion()) && StringUtils.isNotBlank(artifact.get(Artifact.Attribute.PURL))) {
            qualifiers.add("i:[" + id + "]-v:[" + artifact.getVersion() + "]-purl:[" + artifact.get(Artifact.Attribute.PURL) + "]");
        }

        // id + component + version + type
        if (StringUtils.isNotBlank(artifact.getComponent()) && StringUtils.isNotBlank(artifact.getVersion()) && StringUtils.isNotBlank(artifact.getType())) {
            qualifiers.add("i:[" + id + "]-c:[" + artifact.getComponent() + "]-v:[" + artifact.getVersion() + "]-t:[" + artifact.getType() + "]");
        }

        return qualifiers;
    }

}
