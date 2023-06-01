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
import org.metaeffekt.core.inventory.processor.filescan.tasks.FileCollectTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.util.FileUtils.*;

public class FileSystemScanExecutor implements FileSystemScanTaskListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemScanExecutor.class);

    private final FileSystemScanContext fileSystemScan;

    private ExecutorService executor;

    private Map<ScanTask, Future<?>> monitor;

    public FileSystemScanExecutor(FileSystemScanContext fileSystemScan) {
        this.fileSystemScan = fileSystemScan;
        fileSystemScan.setScanTaskListener(this);
        this.monitor = new ConcurrentHashMap<>();
    }

    public void execute() throws IOException {
        LOG.info("Triggering scan on {}...", fileSystemScan.getBaseDir().getPath());

        this.executor = Executors.newFixedThreadPool(4);

        // trigger an initial scan from basedir
        fileSystemScan.trigger();

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
                scanTasks.stream().forEach(fileSystemScan::push);
                iteration = true;
            }
        }

        executor.shutdown();

        // NOTE: until here only the static, pre-defined component patterns in the reference inventory have been
        //  anticipated. These prevented to further uwnrap parts which are already considered part of a greater
        //  component (otherwise the component pattern would not be precise).
        //  Here, we also have unwrapped the full subtree (except things already covered) and can now derive default
        //  component patterns applying the ComponentPatternProducer.
        final ComponentPatternProducer patternProducer = new ComponentPatternProducer();
        final Inventory componentPatternTargetInventory = new Inventory();
        patternProducer.extractComponentPatterns(fileSystemScan.getBaseDir().getFile(), componentPatternTargetInventory);
        matchAndApplyComponentPatterns(fileSystemScan.getInventory(), componentPatternTargetInventory);

        // delete artifacts marked with delete directive
        final List<Artifact> toBeDeleted = new ArrayList<>();
        for (Artifact artifact : fileSystemScan.getInventory().getArtifacts()) {
            final String scanDirectiveDelete = artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_SCAN_DIRECTIVE);
            if (!StringUtils.isEmpty(scanDirectiveDelete) && scanDirectiveDelete.contains(ArtifactUnwrapTask.SCAN_DIRECTIVE_DELETE)) {
                toBeDeleted.add(artifact);
            }
        }
        fileSystemScan.getInventory().getArtifacts().removeAll(toBeDeleted);

        LOG.info("Scan completed.");
    }

    private List<ScanTask> collectOutstandingScanTasks() throws IOException {
        final List<ScanTask> scanTasks = new ArrayList<>();

        final Inventory inventory = fileSystemScan.getInventory();
        final Inventory referenceInventory = fileSystemScan.getScanParam().getReferenceInventory();

        synchronized (inventory) {
            // match and apply component patterns before performing unwrapping
            // only anticipates predefined component patterns
            matchAndApplyComponentPatterns(inventory, referenceInventory);

            for (Artifact artifact : inventory.getArtifacts()) {
                if (!StringUtils.isEmpty(artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_UNWRAP))) {

                    // TODO: exclude artifacts removed (though collection in component)
                    scanTasks.add(new ArtifactUnwrapTask(artifact, null));
                }
            }
        }

        return scanTasks;
    }

    private void matchAndApplyComponentPatterns(final Inventory artifactInventory, final Inventory componentPatternSourceInventory) throws IOException {
        final List<MatchResult> matchedComponentPatterns = matchComponentPatterns(artifactInventory, componentPatternSourceInventory);
        LOG.info("Matching component patterns resulted in {} matches.", matchedComponentPatterns.size());

        final ArrayList<MatchResult> matchResultsWithoutFileMatches = new ArrayList<>();
        filterFilesMatchedByComponentPatterns(matchedComponentPatterns, matchResultsWithoutFileMatches);

        // add artifacts representing the component patterns
        deriveAddonArtifactsFromMatchResult(matchedComponentPatterns);

        // we need to remove those match results, which did not match any file. Such match results may be caused by
        // generic anchor matches and wildcard anchor checksums.
        if (!matchResultsWithoutFileMatches.isEmpty()) {
            matchedComponentPatterns.removeAll(matchResultsWithoutFileMatches);
        }

        // transfer matched component patterns to the artifact inventory
        for (MatchResult matchResult : matchedComponentPatterns) {
            artifactInventory.getComponentPatternData().add(matchResult.componentPatternData);
        }

    }

    private void deriveAddonArtifactsFromMatchResult(List<MatchResult> componentPatterns) {
        for (MatchResult matchResult : componentPatterns) {
            final Artifact derivedArtifact = matchResult.deriveArtifact(fileSystemScan.getBaseDir());
            fileSystemScan.contribute(derivedArtifact);
        }
    }

    private void filterFilesMatchedByComponentPatterns(List<MatchResult> matchedComponentDataOnAnchor, List<MatchResult> matchResultsWithoutFileMatches) throws IOException {

        // remove the matched files covered by the matched components
        for (MatchResult matchResult : matchedComponentDataOnAnchor) {
            final ComponentPatternData cpd = matchResult.componentPatternData;
            final File anchorFile = matchResult.anchorFile;

            // this is a relative path
            final String versionAnchor = normalizePathToLinux(cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR));

            final File baseDir = computeComponentBaseDir(fileSystemScan.getBaseDir().getFile(), anchorFile, versionAnchor);

            final String relativePathToComponentBaseDir = asRelativePath(fileSystemScan.getBaseDir().getFile(), baseDir);

            // build patterns to match (using scanBaseDir relative paths); this should be done during parsing?!
            final String normalizedIncludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN));
            final String normalizedExcludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN));

            final Inventory inventory = fileSystemScan.getInventory();
            synchronized (inventory) {
                boolean matched = false;
                for (final Artifact artifact : inventory.getArtifacts()) {
                    String relativePathFromBaseDir = artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH);

                    // FIXME: revise path matching; consider abstraction
                    if (pathBeginsWith(relativePathFromBaseDir, relativePathToComponentBaseDir)) {
                        if (!relativePathToComponentBaseDir.equalsIgnoreCase(".")) {
                            relativePathFromBaseDir = relativePathFromBaseDir.substring(relativePathToComponentBaseDir.length() + 1);
                        }
                        if (matches(normalizedIncludePattern, relativePathFromBaseDir)) {
                            if (org.springframework.util.StringUtils.isEmpty(normalizedExcludePattern) || !matches(normalizedExcludePattern, relativePathFromBaseDir)) {
                                LOG.debug("Filtered component file: {} for component pattern {}", relativePathFromBaseDir, cpd.deriveQualifier());
                                artifact.set("AID_" + matchResult.anchorFile, "x");
                                artifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_SCAN_DIRECTIVE, ArtifactUnwrapTask.SCAN_DIRECTIVE_DELETE);
                                artifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_ASSET_ID_CHAIN, matchResult.assetIdChain);
                                matched = true;
                            }
                        }
                    }
                }

                if (!matched) {
                    LOG.info("No files matched for component pattern {}.", matchResult.componentPatternData.createCompareStringRepresentation());
                    matchResultsWithoutFileMatches.add(matchResult);
                }
            }
        }
    }

    private static boolean pathBeginsWith(String relativePathFromBaseDir, String relativePathToComponentBaseDir) {
        if (relativePathToComponentBaseDir.equalsIgnoreCase(".")) {
            return true;
        } else {
            return relativePathFromBaseDir.startsWith(relativePathToComponentBaseDir + "/");
        }
    }

    private String normalizePattern(String pattern) {
        if (pattern == null) return pattern;

        String[] patterns = pattern.split(",");
        return Arrays.stream(patterns)
                .map(String::trim)
                .map(s -> normalizePathToLinux(s))
                .collect(Collectors.joining(","));
    }

    private static class MatchResult {
        ComponentPatternData componentPatternData;
        File anchorFile;
        File baseDir;

        String assetIdChain;

        MatchResult(ComponentPatternData componentPatternData, File anchorFile, File baseDir, String assetIdChain) {
            this.componentPatternData = componentPatternData;
            this.anchorFile = anchorFile;
            this.baseDir = baseDir;
            this.assetIdChain = assetIdChain;
        }

        Artifact deriveArtifact(FileRef scanBaseDir) {
            final Artifact derivedArtifact = new Artifact();
            derivedArtifact.setId(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_PART));
            derivedArtifact.setComponent(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_NAME));
            derivedArtifact.setVersion(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
            derivedArtifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH, asRelativePath(scanBaseDir.getFile(), baseDir));
            derivedArtifact.set(ArtifactUnwrapTask.ATTRIBUTE_KEY_ASSET_ID_CHAIN, assetIdChain);

            // also take over the type attribute
            derivedArtifact.set(Constants.KEY_TYPE, componentPatternData.get(Constants.KEY_TYPE));
            return derivedArtifact;
        }
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
                scanTask.process(fileSystemScan);
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

    public static final String DOUBLE_ASTERISK = Constants.ASTERISK + Constants.ASTERISK;

    /**
     * Matches the component patterns. The inventory remains unmodified.
     *
     * @param inputInventory The inventory carrying the current scan result that is examined for component patterns.
     * @param componentPatternSourceInventory The inventory to take component patterns from.
     *
     * @return List of matched / potential component patterns.
     */
    public List<MatchResult> matchComponentPatterns(final Inventory inputInventory, final Inventory componentPatternSourceInventory) {
        // match component patterns using version anchor; results in matchedComponentPatterns
        final List<MatchResult> matchedComponentPatterns = new ArrayList<>();

        for (final ComponentPatternData cpd : componentPatternSourceInventory.getComponentPatternData()) {
            LOG.debug("Checking component pattern: {}", cpd.createCompareStringRepresentation());

            final String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            final String versionAnchor = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            final String normalizedVersionAnchor = normalizePathToLinux(versionAnchor);

            if (versionAnchor == null) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (versionAnchor.contains(DOUBLE_ASTERISK)) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must not contain **. Use * only.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (anchorChecksum == null) {
                throw new IllegalStateException(String.format("The version anchor checksum of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }

            // memorize whether the path fragment of version anchor contains wildcards
            final boolean isVersionAnchorPattern = versionAnchor.contains(Constants.ASTERISK);

            // memorize whether version anchor checksum is specific (and not just *)
            final boolean isVersionAnchorChecksumSpecific = !anchorChecksum.equalsIgnoreCase(Constants.ASTERISK);

            if (versionAnchor.equalsIgnoreCase(Constants.ASTERISK) || versionAnchor.equalsIgnoreCase(Constants.DOT)) {

                if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK)) {
                    throw new IllegalStateException(String.format(
                            "The version anchor checksum of component pattern [%s] with version anchor [%s] must be '*'.",
                            cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN), versionAnchor));
                }

                // FIXME: why do we clone and adjust the anchor checksum here? May be instantiated multiple times?
                final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, Constants.ASTERISK);
                matchedComponentPatterns.add(new MatchResult(copyCpd, fileSystemScan.getBaseDir().getFile(),
                        fileSystemScan.getBaseDir().getFile(), null));

                // continue with next component pattern (otherwise this would produce a hugh amount of matched patterns)
                continue;
            }

            final String[] split = normalizedVersionAnchor.split("\\*");
            final List<String> quickCheck = Arrays.stream(split).sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
            final String containsCheckString = quickCheck.get(quickCheck.size() - 1);

            // TODO: we could determine a substring here that does not include any wildcard; must be quick
            //  then we use the contains() to have a quick pre check, before computing the accurate match

            // check whether the version anchor path fragment matches one of the file paths
            for (final Artifact artifact : inputInventory.getArtifacts()) {
                // generate normalized path relative to scanBaseDir (important; not to scanDir, which may vary as we
                // descend into the hierarchy on recursion)

                final String normalizedPath = fileSystemScan.getBaseDir().getPath() + "/" + artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH);

                if (normalizedPath.contains(containsCheckString)) {
                    if (versionAnchorMatches(normalizedVersionAnchor, normalizedPath, isVersionAnchorPattern)) {

                        // on match infer the checksum of the file
                        final String fileChecksumOrAsterisk = isVersionAnchorChecksumSpecific ? artifact.getChecksum() : Constants.ASTERISK;

                        if (!anchorChecksum.equalsIgnoreCase(fileChecksumOrAsterisk)) {
                            LOG.debug("Anchor fileChecksumOrAsterisk mismatch: " + normalizedPath);
                            LOG.debug("Expected fileChecksumOrAsterisk :{}; actual file fileChecksumOrAsterisk: {}", anchorChecksum, fileChecksumOrAsterisk);
                        } else {
                            final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                            copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, fileChecksumOrAsterisk);

                            final File file = new File(normalizedPath);
                            matchedComponentPatterns.add(new MatchResult(copyCpd, file,
                                    computeComponentBaseDir(fileSystemScan.getBaseDir().getFile(), file, normalizedVersionAnchor), artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_ASSET_ID_CHAIN)));
                        }
                    }
                }
            }
        }

        return matchedComponentPatterns;
    }

    private boolean versionAnchorMatches(String normalizedVersionAnchor, String normalizedPath, boolean isVersionAnchorPattern) {
        return (!isVersionAnchorPattern && normalizedPath.endsWith(normalizedVersionAnchor)) ||
                (isVersionAnchorPattern && matches("**/" + normalizedVersionAnchor, normalizedPath));
    }

    private File computeComponentBaseDir(File scanBaseDir, File anchorFile, String versionAnchor) {
        if (Constants.ASTERISK.equalsIgnoreCase(versionAnchor)) return scanBaseDir;
        if (Constants.DOT.equalsIgnoreCase(versionAnchor)) return scanBaseDir;

        final int versionAnchorFolderDepth = org.springframework.util.StringUtils.countOccurrencesOf(versionAnchor, "/") + 1;

        File baseDir = anchorFile;
        for (int i = 0; i < versionAnchorFolderDepth; i++) {
            baseDir = baseDir.getParentFile();

            // handle special case the the parent dir does not exist (for whatever reason)
            if (baseDir == null) {
                baseDir = scanBaseDir;
                break;
            }
        }
        return baseDir;
    }


}
