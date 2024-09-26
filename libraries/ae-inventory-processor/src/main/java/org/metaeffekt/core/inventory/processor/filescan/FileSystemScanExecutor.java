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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.DirectoryScanTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.FileCollectTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.inspector.InspectorRunner;
import org.metaeffekt.core.inventory.processor.inspector.JarInspector;
import org.metaeffekt.core.inventory.processor.inspector.NestedJarInspector;
import org.metaeffekt.core.inventory.processor.inspector.RpmMetadataInspector;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
        fileSystemScanContext.push(new DirectoryScanTask(fileSystemScanContext.getBaseDir(), fileSystemScanContext.getVirtualContext(), assetIdChain));

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

        // post-processing steps
        // 1. produce one file with all ArtifactFile types
        final File baseDir = fileSystemScanContext.getBaseDir().getFile();

        List<FilePatternQualifierMapper> filePatternQualifierMappers = ComponentPatternValidator.detectDuplicateComponentPatternMatches(fileSystemScanContext.getScanParam().getReferenceInventory(), inventory, baseDir);


        // 2. analyze component containments
        for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
            final String assetId = "AID-" + mapper.getArtifact().getId() + "-" + mapper.getArtifact().getChecksum();
            if (mapper.getSubSetMap() != null) {
                for (String qualifier : mapper.getSubSetMap().keySet()) {
                    Artifact foundArtifact = fileSystemScanContext.getInventory().getArtifacts().stream().filter(a -> a.getId().equals(qualifier) || qualifier.equals(a.getComponent() + "-" + a.getId() + "-" + a.getVersion())).findFirst().orElse(null);
                    if (foundArtifact != null) {
                        if (StringUtils.isBlank(assetId)) {
                            LOG.warn("Cannot resolve asset id for qualifier " + mapper.getQualifier());
                        } else {
                            if (!foundArtifact.get(assetId).equals(MARKER_CONTAINS) && !foundArtifact.get(assetId).equals(MARKER_CROSS)) {
                                LOG.error("Artifact " + foundArtifact.getId() + " does not contain asset " + assetId);
                            }
                        }
                    }
                }
            }
        }

        // 3. build zips for all components
        buildZipsForAllComponents(baseDir, filePatternQualifierMappers);
    }

    public void buildZipsForAllComponents(File baseDir, List<FilePatternQualifierMapper> filePatternQualifierMappers) {
        // Create an Ant project
        Project antProject = new Project();
        antProject.init();

        for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
            // filter out big tar files
            mapper.getFileMap().values().forEach(files -> files.removeIf(f -> f.length() > 100000000));

            // Loop over each entry in the file map
            for (Map.Entry<Boolean, List<File>> entry : mapper.getFileMap().entrySet()) {
                List<File> files = entry.getValue();
                if (files.isEmpty()) {
                    continue;
                }

                final File targetDir = new File(baseDir, "components");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                final File tmpFolder = initializeTmpFolder(targetDir);

                final File zipFile = new File(targetDir, mapper.getQualifier() + ".zip");
                File relativeBaseDir;
                if (mapper.getPathInAsset().contains(".|\n")) {
                    relativeBaseDir = new File(baseDir.getPath(), ".");
                } else {
                    relativeBaseDir = new File(baseDir.getPath(), mapper.getPathInAsset());
                }

                // Create a new Ant Zip task
                Zip zipTask = new Zip();
                zipTask.setProject(antProject);
                zipTask.setDestFile(zipFile);

                // Add each file to the zip
                for (File file : files) {
                    ZipFileSet zipFileSet = new ZipFileSet();

                    if (file.isFile()) {
                        zipFileSet.setFile(file);
                    } else if (file.isDirectory()) {
                        zipFileSet.setDir(file);
                    }

                    try {
                        FileUtils.copyFile(file, new File(tmpFolder, file.getName()));
                    } catch (IOException e) {
                        LOG.error("Failed to copy file to tmp folder.", e);
                    }

                    // Set the prefix to preserve the relative directory structure
                    String relativePath = relativeBaseDir.toURI().relativize(file.getParentFile().toURI()).getPath();
                    zipFileSet.setPrefix(relativePath);

                    zipTask.addZipfileset(zipFileSet);
                }

                final File contentChecksumFile = new File(tmpFolder, zipFile.getName() + ".content.md5");
                try {
                    FileUtils.createDirectoryContentChecksumFile(tmpFolder, contentChecksumFile);
                } catch (IOException e) {
                    LOG.error("Failed to create content checksum file.", e);
                }
                // set the content checksum
                final String contentChecksum = FileUtils.computeChecksum(contentChecksumFile);
                mapper.getArtifact().set(KEY_CONTENT_CHECKSUM, contentChecksum);
                FileUtils.deleteDirectoryQuietly(tmpFolder);

                // Execute the zip task
                zipTask.execute();
            }
        }
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

    private static File initializeTmpFolder(File targetDir) {
        final File tmpFolder = new File(targetDir.getParentFile(), ".tmp");
        if (tmpFolder.exists()) {
            try {
                FileUtils.deleteDirectory(tmpFolder);
            } catch (IOException e) {
                LOG.error("Failed to delete tmp folder.", e);
            }
        }
        tmpFolder.mkdirs();
        return tmpFolder;
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

        final Map<String, List<Artifact>> stringListMap = buildQualifierArtifactMap(inventory);

        for (List<Artifact> list : stringListMap.values()) {
            Artifact artifact = list.get(0);

            Set<String> paths = ConcurrentHashMap.newKeySet();
            addPath(artifact, paths);
            for (int i = 1; i < list.size(); i++) {
                final Artifact a = list.get(i);
                inventory.getArtifacts().remove(a);
                artifact.merge(a);
                addPath(a, paths);
            }

            if (!paths.isEmpty()) {
                artifact.setProjects(paths);
                artifact.set(KEY_PATH_IN_ASSET, paths.stream().sorted(String::compareToIgnoreCase).collect(Collectors.joining("|\n")));
            }
        }

        InventoryUtils.mergeDuplicateAssets(inventory);

        mergeRedundantContainerArtifacts(inventory);
    }

    private void mergeRedundantContainerArtifacts(Inventory inventory) {
        // additionally merge scanned / decomposed artifacts
        final Set<Artifact> artifactsWithScanClassification = inventory.getArtifacts().stream()
                .filter(a -> a.hasClassification(HINT_SCAN)).collect(Collectors.toSet());

        for (Artifact scannedArtifact : artifactsWithScanClassification) {
            final String assetId = "AID-" + scannedArtifact.getId() + "-" + scannedArtifact.getChecksum();

            File pathInAsset = new File(scannedArtifact.get(Artifact.Attribute.PATH_IN_ASSET));
            final String name = pathInAsset.getName();
            File extractedPathInAsset = new File(pathInAsset.getParentFile(), "[" + name + "]");

            String qualifier = scannedArtifact.getId() + "/" + extractedPathInAsset.getPath();

            Set<Artifact> toBeDeleted = new HashSet<>();
            for (Artifact artifact : inventory.getArtifacts()) {
                final String q = deriveContainedArtifactQualifier(artifact);
                if (qualifier.equals(q)) {
                    if ("c".equalsIgnoreCase(artifact.get(assetId))) {
                        // manage attributes that should not be merged
                        artifact.set(assetId, null);
                        artifact.setProjects(Collections.EMPTY_SET);

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

    private static void addPath(Artifact a, Set<String> paths) {
        String path = a.get(KEY_PATH_IN_ASSET);
        if (StringUtils.isNotBlank(path)) {
            paths.add(path);
        }
    }

    public Map<String, List<Artifact>> buildQualifierArtifactMap(Inventory inventory) {
        final Map<String, List<Artifact>> qualifierArtifactMap = new LinkedHashMap<>();

        for (final Artifact artifact : inventory.getArtifacts()) {
            final String artifactQualifier = qualifierOf(artifact);
            final List<Artifact> artifacts = qualifierArtifactMap.computeIfAbsent(artifactQualifier, a -> new ArrayList<>());
            artifacts.add(artifact);
        }
        return qualifierArtifactMap;
    }

    public String qualifierOf(Artifact artifact) {
        if (StringUtils.isNotBlank(artifact.getChecksum())) {
            // in case we have a checksum id and checksum are sufficient
            return "[" +
                    artifact.getId() + "-" +
                    artifact.getChecksum() +
                    "]";
        } else {
            // in case there is no checksum use groupId and version as discriminator
            return "[" +
                    artifact.getId() + "-" +
                    artifact.getGroupId() + "-" +
                    artifact.getVersion() +
                    "]";

        }
    }

}
