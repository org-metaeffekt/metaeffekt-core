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
package org.metaeffekt.core.inventory.processor.configuration;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryMergeUtils;
import org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.metaeffekt.core.inventory.processor.patterns.contributors.ContributorUtils;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.ArtifactUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator.evaluateComponentPatterns;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class DirectoryScanAggregatorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryScanAggregatorConfiguration.class);

    final private Inventory referenceInventory;

    final private Inventory resultInventory;

    final private File scanBaseDir;

    final private File scanResultInventoryFile;

    public DirectoryScanAggregatorConfiguration(Inventory referenceInventory, Inventory resultInventory, File scanBaseDir) {
        this.scanBaseDir = scanBaseDir;
        this.referenceInventory = referenceInventory;
        this.scanResultInventoryFile = null;
        this.resultInventory = resultInventory;
    }

    public List<FilePatternQualifierMapper> mapArtifactsToCoveredFiles() throws IOException {

        // initialize component pattern and file pattern map
        final Map<String, List<ComponentPatternData>> qualifierToComponentPatternMap = new HashMap<>();
        final List<FilePatternQualifierMapper> filePatternQualifierMapperList = new ArrayList<>();

        // contribute component pattern from reference inventory
        contributeComponentPatterns(referenceInventory, qualifierToComponentPatternMap);

        // contribute project component patterns (no overwrite; reference has the control)
        contributeComponentPatterns(resultInventory, qualifierToComponentPatternMap);

        // iterate extracted artifacts and match with component patterns
        for (Artifact artifact : resultInventory.getArtifacts()) {

            // use artifact attributes to identify component pattern
            final String componentName = artifact.getComponent();
            final String componentVersion = artifact.getVersion();
            final String componentPart = artifact.getId();

            // concluded derived qualifier
            final String derivedQualifier = deriveMapQualifier(componentName, componentPart, componentVersion);

            // identify matching component patterns (this may overlap with real artifacts)
            final ComponentPatternMatches componentPatternMatches = findComponentPatternMatches(
                    qualifierToComponentPatternMap, componentName, componentVersion, componentPart);

            // build FilePatternQualifierMapper baseline
            final FilePatternQualifierMapper filePatternQualifierMapper = new FilePatternQualifierMapper();
            filePatternQualifierMapper.setArtifact(artifact);
            filePatternQualifierMapper.setQualifier(derivedQualifier);
            filePatternQualifierMapper.setPathInAsset(artifact.getPathInAsset());

            // iterate found component patterns for artifact
            if (componentPatternMatches.list != null) {
                final Map<Boolean, List<File>> duplicateToComponentPatternFilesMap = new HashMap<>(
                        mapCoveredFilesByDuplicateStatus(artifact, componentPatternMatches, filePatternQualifierMapper));
                filePatternQualifierMapper.setFileMap(duplicateToComponentPatternFilesMap);

                // collect component-pattern-covered files
                final List<File> componentPatternFiles = new ArrayList<>();
                for (List<File> files : duplicateToComponentPatternFilesMap.values()) {
                    componentPatternFiles.addAll(files);
                }

                filePatternQualifierMapper.setFiles(componentPatternFiles);
            } else {
                // handle artifacts that cannot be mapped to files by component patterns; we need that the inventory is
                // completely represented even in case no files are directly or indirectly associated
                filePatternQualifierMapper.setFileMap(Collections.emptyMap());
                filePatternQualifierMapper.setFiles(Collections.emptyList());
            }

            // add mapper
            filePatternQualifierMapperList.add(filePatternQualifierMapper);
        }

        return filePatternQualifierMapperList;
    }

    private Map<Boolean, List<File>> mapCoveredFilesByDuplicateStatus(Artifact artifact,
              ComponentPatternMatches componentPatternMatches, FilePatternQualifierMapper filePatternQualifierMapper) {

        // initialize map
        final Map<Boolean, List<File>> duplicateToComponentPatternFilesMap = new HashMap<>();

        // iterate all component pattern matches
        for (ComponentPatternData cpd : componentPatternMatches.list) {

            // aggregate all component-pattern-covered files into one directory
            final List<File> componentPatternCoveredFiles = new ArrayList<>();
            final String includes = cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
            final String excludes = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
            filePatternQualifierMapper.getComponentPatternDataList().add(cpd);

            // use artifact data to pre-select the folder to be scanned; can be multiple
            final Set<String> componentBaseDirs = artifact.getRootPaths();

            for (final String baseDir : componentBaseDirs) {

                // derive componentBaseDir which may be an archive or a directory
                File componentBaseDir = new File(getExtractedFilesBaseDir(), baseDir);

                // ensure this is the original name
                String fileName = componentBaseDir.getName();
                if (fileName.startsWith("[") && fileName.endsWith("]")) {
                    final String name = fileName.substring(1, fileName.length() - 1);
                    componentBaseDir = new File(componentBaseDir.getParentFile(), name);
                }

                // compose unpacked folder name
                final String name = componentBaseDir.getName();
                final File unpackedComponentBaseDir = new File(componentBaseDir.getParentFile(), "[" + name + "]");

                // in case the plain component base dir is not a directory, attempt to unwrap
                if (componentBaseDir.exists() && !componentBaseDir.isDirectory()) {
                    // this may be an unpacked artifact; if not existing we need to unpack the file again
                    if (!unpackedComponentBaseDir.exists()) {
                        ArchiveUtils.unpackIfPossible(componentBaseDir, unpackedComponentBaseDir, new ArrayList<>());
                    }
                }

                // check is unwrapped exists and continue with this
                if (unpackedComponentBaseDir.exists()) {
                    componentBaseDir = unpackedComponentBaseDir;
                }

                // check the directory (now) exists and can be used to further evaluate the component patterns
                if (componentBaseDir.exists()) {

                    // aggregate matching files to dedicated folders
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Scanning {} including {} excluding {}", componentBaseDir, includes, excludes);
                    }

                    // differentiate directories and single files
                    if (componentBaseDir.isDirectory()) {
                        aggregateComponentFiles(getExtractedFilesBaseDir(), componentBaseDir, includes, excludes, componentPatternCoveredFiles);
                    } else {
                        // the component pattern matches a single file; this is what we add to the list
                        // TODO: check if we should add symbolic links as well
                        componentPatternCoveredFiles.add(componentBaseDir);
                    }
                }
            }
            duplicateToComponentPatternFilesMap.put(false, componentPatternCoveredFiles);
        }
        return duplicateToComponentPatternFilesMap;
    }

    private void aggregateComponentFiles(
            File baseDir, File componentBaseDir, String includes, String excludes, List<File> componentPatternCoveredFiles) {

        // split includes/excludes in relative and absolute paths
        final ComponentPatternProducer.NormalizedPatternSet includePatternSet = ComponentPatternProducer.normalizePattern(includes);
        final ComponentPatternProducer.NormalizedPatternSet excludePatternSet = ComponentPatternProducer.normalizePattern(excludes);

        final Set<String> relativizedIncludePatterns = relativizePatterns(includePatternSet.absolutePatterns);
        final Set<String> relativizedExcludePatterns = relativizePatterns(excludePatternSet.absolutePatterns);

        int count = 0;

        if (!includePatternSet.relativePatterns.isEmpty()) {
            for (String normalizedInclude : new HashSet<>(includePatternSet.relativePatterns)) {
                String bloatedNormalizedInclude = ContributorUtils.extendArchivePattern(normalizedInclude);
                if (bloatedNormalizedInclude == null) {
                    continue;
                }
                includePatternSet.relativePatterns.add(bloatedNormalizedInclude);
            }
            final String[] relativeCoveredFiles = FileUtils.scanDirectoryForFiles(componentBaseDir,
                    toArray(includePatternSet.relativePatterns), toArray(excludePatternSet.relativePatterns));
            aggregateFiles(componentBaseDir, relativeCoveredFiles, componentPatternCoveredFiles);
            count += relativeCoveredFiles.length;
        }

        if (!relativizedIncludePatterns.isEmpty()) {
            for (String relativeInclude : new HashSet<>(relativizedIncludePatterns)) {
                String bloatedRelativeInclude = ContributorUtils.extendArchivePattern(relativeInclude);
                if (bloatedRelativeInclude == null) {
                    continue;
                }
                relativizedIncludePatterns.add(bloatedRelativeInclude);
            }
            final String[] absoluteCoveredFiles = FileUtils.scanDirectoryForFiles(baseDir,
                    toArray(relativizedIncludePatterns), toArray(relativizedExcludePatterns));
            aggregateFiles(baseDir, absoluteCoveredFiles, componentPatternCoveredFiles);
            count += absoluteCoveredFiles.length;
        }

        if (count == 0) {
            // FIXME: activate exception or at least log a warning; perhaps control by parameter
            // throw new IllegalStateException("Identified component pattern does not match any file: " + cpd.deriveQualifier());
        }
    }

    private void aggregateFiles(File baseDir, String[] coveredFiles, List<File> componentPatternCoveredFiles) {
        for (String file : coveredFiles) {
            final File srcFile = new File(baseDir, file);
            // TODO: check if we should add symbolic links as well
            componentPatternCoveredFiles.add(srcFile);
        }
    }

    private String[] toArray(Set<String> patternSet) {
        if (patternSet.isEmpty()) return null;
        return patternSet.toArray(new String[0]);
    }

    private Set<String> relativizePatterns(Set<String> patternSet) {
        final Set<String> relativizedPatterns = new HashSet<>();
        if (patternSet != null) {
            for (String absoluteInclude : patternSet) {
                if (absoluteInclude.startsWith("/")) {
                    relativizedPatterns.add(absoluteInclude.substring(1));
                } else {
                    throw new IllegalStateException("Absolute normalized pattern does not start with '/': " + absoluteInclude);
                }
            }
        }
        return relativizedPatterns;
    }

    private void contributeComponentPatterns(Inventory referenceInventory, Map<String, List<ComponentPatternData>> componentPatternMap) {
        for (ComponentPatternData cpd : referenceInventory.getComponentPatternData()) {
            final String key = deriveMapQualifier(cpd);

            // the following  lines could be replaced by:
            // componentPatternMap.computeIfAbsent(key, k -> new ArrayList<>()).add(cpd);
            List<ComponentPatternData> list = componentPatternMap.get(key);
            if (list == null) {
                list = new ArrayList<>();
                componentPatternMap.put(key, list);

                // also include fallback mapping (in case component name does not match)
                componentPatternMap.put(deriveFallbackMapQualifier(
                        cpd.get(COMPONENT_PART),
                        cpd.get(COMPONENT_VERSION)), list);
            }
            list.add(cpd);
        }
    }

    private File getExtractedFilesBaseDir() {
        return scanBaseDir;
    }

    private ComponentPatternMatches findComponentPatternMatches(Map<String, List<ComponentPatternData>> componentPatternMap,
                String componentName, String componentVersion, String componentPart) {

        final ComponentPatternMatches componentPatternMatch = new ComponentPatternMatches();
        componentPatternMatch.key = deriveMapQualifier(componentName, componentPart, componentVersion);
        componentPatternMatch.list = componentPatternMap.get(componentPatternMatch.key);

        if (componentPatternMatch.list == null) {
            final String modulatedKey = deriveFallbackMapQualifier(componentPart, componentVersion);
            componentPatternMatch.list = componentPatternMap.get(modulatedKey);
        }
        return componentPatternMatch;
    }

    private static class ComponentPatternMatches {
        protected String key;
        protected List<ComponentPatternData> list;
    }

    private String deriveFallbackMapQualifier(String componentPart, String componentVersion) {
        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(componentPart)) {
            sb.append(componentPart);
        }
        sb.append("-");
        if (StringUtils.isNotBlank(componentVersion)) {
            sb.append(componentVersion);
        }
        return sb.toString();
    }

    public static String deriveMapQualifier(String componentName, String componentPart, String componentVersion) {
        final StringBuilder sb = new StringBuilder();
        // NOTE: the componentPart is the artifact id; it is usually not blank; we nevertheless treat it equivalently
        if (StringUtils.isNotBlank(componentPart)) {
            sb.append(componentPart);
        }
        sb.append("-");
        if (StringUtils.isNotBlank(componentVersion)) {
            sb.append(componentVersion);
        }
        sb.append("-");
        if (StringUtils.isNotBlank(componentName)) {
            sb.append(componentName);
        }
        return sb.toString();
    }

    private String deriveMapQualifier(ComponentPatternData cpd) {
        return deriveMapQualifier(cpd.get(COMPONENT_NAME), cpd.get(COMPONENT_PART), cpd.get(COMPONENT_VERSION));
    }

    public File getResultInventoryFile() {
        return scanResultInventoryFile;
    }

    public void aggregateFiles(File aggregationDir) {
        if (aggregationDir != null && !aggregationDir.exists()) {
            try {
                FileUtils.forceMkdir(aggregationDir);
            } catch (IOException e) {
                LOG.error("Cannot create aggregation directory [{}].", aggregationDir.getAbsolutePath(), e);
            }
        }

        if (aggregationDir != null && aggregationDir.exists()) {
            // post-processing steps

            // evaluate component patterns
            final List<FilePatternQualifierMapper> filePatternQualifierMappers =
                    evaluateComponentPatterns(referenceInventory, resultInventory, scanBaseDir);

            // aggregate files (atomic and component patterns)
            aggregateFilesForAllArtifacts(scanBaseDir, filePatternQualifierMappers, aggregationDir);
        }
    }

    private void aggregateFilesForAllArtifacts(File scanBaseDir, List<FilePatternQualifierMapper> filePatternQualifierMappers, File targetDir) {

        // perform aggregation for each map
        final Set<Artifact> coveredArtifacts = new HashSet<>();
        final String canonicalScanBaseDir = FileUtils.canonicalizeLinuxPath(scanBaseDir.getAbsolutePath());
        for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
            coveredArtifacts.addAll(aggregateFilesForMapper(mapper, canonicalScanBaseDir, targetDir));
        }

        // copy remaining artifacts not covered by component-patterns to aggregation dir (mapper-independent)
        for (Artifact artifact : resultInventory.getArtifacts()) {

            // skip artifact covered by component patterns
            if (coveredArtifacts.contains(artifact)) continue;

            // evaluate directive
            if (hasSkipAggregationDirective(artifact)) continue;

            for (String project : artifact.getRootPaths()) {
                final File file = new File(scanBaseDir, project);
                if (file.exists() && !FileUtils.isSymlink(file) && file.isFile()) {
                    final String relativePath = FileUtils.asRelativePath(scanBaseDir, file);
                    try {
                        final File targetFile = new File(targetDir, relativePath);
                        FileUtils.copyFile(file, targetFile);
                        artifact.set(KEY_ARCHIVE_PATH, targetFile.getAbsolutePath());
                    } catch (IOException e) {
                        LOG.warn("Cannot copy file [{}] to aggregation folder [{}]", file.getAbsolutePath(), targetDir.getAbsolutePath());
                    }
                }
            }
        }
    }

    private Set<Artifact> aggregateFilesForMapper(FilePatternQualifierMapper mapper, String canonicalScanBaseDir, File targetDir) {
        final Set<Artifact> coveredArtifacts = new HashSet<>();

        // the checksum must not be included in the archive; separate the two location
        final File tmpBaseDir = FileUtils.initializeTmpFolder(targetDir);
        final File tmpContentDir = new File(tmpBaseDir, "content");

        // FIXME: why isn't that always the mapper.artifact
        final Artifact foundArtifact = resultInventory.getArtifacts().stream()
                .filter(artifact -> matchQualifierToIdOrDerivedQualifier(mapper.getQualifier(), artifact))
                .findFirst().orElse(null);
        try {
            boolean contentDetected = false;

            // loop over each entry in the file map
            for (Map.Entry<Boolean, List<File>> entry : mapper.getFileMap().entrySet()) {
                final List<File> files = entry.getValue();
                if (foundArtifact != null) {
                    String noFileMatchAttribute = foundArtifact.get(KEY_NO_FILE_MATCH_REQUIRED);
                    if (files.isEmpty() && noFileMatchAttribute == null) {
                        continue;
                    }
                }

                coveredArtifacts.add(mapper.getArtifact());

                final String commonRootPath = determineCommonRootPath(canonicalScanBaseDir, files);

                // add each file to the zip
                for (File file : files) {
                    // copy the file to the tmp folder; use full path from scan
                    final String filePath = FileUtils.canonicalizeLinuxPath(file.getAbsolutePath());
                    final String relativePath = FileUtils.asRelativePath(commonRootPath, filePath);
                    if (file.exists()) {
                        FileUtils.copyFile(file, new File(tmpContentDir, relativePath));
                    }

                    contentDetected = true;
                }
            }

            // in case content was detected we create the content checksum and pack the files into a zip
            if (contentDetected) {
                final File contentChecksumFile = new File(tmpBaseDir, mapper.getArtifact().getId() + ".content.md5");
                FileUtils.createDirectoryContentChecksumFile(tmpContentDir, contentChecksumFile);

                // set the content checksum
                final String contentChecksum = FileUtils.computeChecksum(contentChecksumFile);
                final File zipFile = new File(targetDir, mapper.getArtifact().getId() + "-" + contentChecksum + ".zip");

                mapper.getArtifact().set(KEY_CONTENT_CHECKSUM, contentChecksum);
                mapper.getArtifact().set(KEY_ARCHIVE_PATH, zipFile.getAbsolutePath());

                if (foundArtifact != null) {
                    // FIXME: see above
                    foundArtifact.set(KEY_CONTENT_CHECKSUM, contentChecksum);
                    foundArtifact.set(KEY_ARCHIVE_PATH, zipFile.getAbsolutePath());
                }

                ArchiveUtils.zipAnt(tmpContentDir, zipFile);
                if (!zipFile.exists()) {
                    // protocol as error and continue
                    throw new IllegalStateException("Failed to create zip file for artifact: [" + mapper.getArtifact().getId() + "]");
                }
            }
        } catch (IOException e) {
            LOG.error("Error processing artifact: [{}] with following error: [{}]", mapper.getArtifact().getId(), e.getMessage());
        } finally {
            // ensure the tmp folder is deleted (content and checksum file)
            FileUtils.deleteDirectoryQuietly(tmpBaseDir);
        }

        return coveredArtifacts;
    }

    private boolean hasSkipAggregationDirective(Artifact artifact) {
        final String directive = artifact.get(KEY_AGGREGATE_DIRECTIVE);
        return AGGREGATE_DIRECTIVE_SKIP.equalsIgnoreCase(directive);
    }

    private String determineCommonRootPath(String canonicalScanBasePath, List<File> files) {
        if (files == null || files.isEmpty()) {
            return canonicalScanBasePath;
        }

        String candidatePath = FileUtils.canonicalizeLinuxPath(files.get(0).getParentFile().getAbsolutePath());
        boolean commonRoot = true;
        do {
            // match only with trailing slash not to match a folder starting with the last element in the path
            final String matchPath = candidatePath + "/";

            for (File file : files) {
                final String filePath = FileUtils.canonicalizeLinuxPath(file.getAbsolutePath());

                if (!filePath.startsWith(matchPath)) {
                    commonRoot = false;
                    break;
                }
            }

            if (!commonRoot) {
                // try next level up
                final File parentFile = new File(candidatePath).getParentFile();
                if (parentFile != null) {
                    candidatePath = FileUtils.canonicalizeLinuxPath(parentFile.getAbsolutePath());
                } else {
                    LOG.warn("Issue detected evaluating common root path. Inputs: scanBasePath={}, candidatePath={}", canonicalScanBasePath, candidatePath);
                    candidatePath = null;
                }
            }
        } while (!commonRoot && candidatePath != null && !canonicalScanBasePath.equals(candidatePath));

        return (candidatePath == null) ? canonicalScanBasePath : candidatePath;
    }

    public static boolean matchQualifierToIdOrDerivedQualifier(String qualifier, Artifact a) {
        return a.getId().equals(qualifier) || qualifier.equals(deriveQualifier(a));
    }

    private static String deriveQualifier(Artifact a) {
        return DirectoryScanAggregatorConfiguration.deriveMapQualifier(a.getComponent(), a.getVersion(), a.getId());
    }


    public void contribute(File targetDir, Inventory aggregatedInventory) throws IOException {
        aggregateFiles(targetDir);

        final InventoryMergeUtils inventoryMergeUtils = new InventoryMergeUtils();
        inventoryMergeUtils.setAddDefaultArtifactMergeAttributes(true);
        inventoryMergeUtils.setAddDefaultArtifactExcludedAttributes(false);
        inventoryMergeUtils.mergeInventories(Collections.singletonList(resultInventory), aggregatedInventory);

        checkCompletenessOfArchivePath(aggregatedInventory);
    }

    /**
     * There are multiple reasons for empty archive paths:
     * <ul>
     *     <li>
     *          The relevant component pattern is not included in the reference inventory, This is a configuration issue.
     *     </li>
     *     <li>
     *         There is no content available for the artifact. E.g. a logical package configuration without physical files.
     *         This may require provision of download urls or additional content (however at a later stage)
     *     </li>
     *     <li>
     *         Artifacts that have been unpacked for scanning the content (classification contains 'scan')
     *     </li>
     * </ul>
     *
     * @param inventory The inventory to check for KEY_ARCHIVE_PATH completeness.
     */
    private void checkCompletenessOfArchivePath(Inventory inventory) {
        for (final Artifact artifact : inventory.getArtifacts()) {
            final String archivePath = artifact.get(Constants.KEY_ARCHIVE_PATH);
            final String contentChecksum = artifact.get(KEY_CONTENT_CHECKSUM);
            final String checksum = artifact.getChecksum();

            final boolean hasChecksum = StringUtils.isNotBlank(checksum);
            final boolean hasContentChecksum = StringUtils.isNotBlank(contentChecksum);

            // deep scanned artifacts are not further scanned. It would be good to get an aggregated view however
            if (ArtifactUtils.hasScanClassification(artifact)) {
                if (hasContentChecksum || hasChecksum) {
                    LOG.warn("Artifact {} with scan classification must have checksum and a content checksum.", artifact);
                }
                continue;
            }

            // skipped artifacts have no archive path (no redundant aggregation)
            if (hasSkipAggregationDirective(artifact)) continue;

            if (StringUtils.isBlank(archivePath)) {
                // only report issue, when we have a checksum; implicitly excluded shaded subcomponents from being reported
                if (hasContentChecksum || hasChecksum) {
                    LOG.warn("Artifact {} with file content does not have an archive path! " +
                            "Validate that the component patterns for this process are complete.", artifact);
                }
            }
        }
    }

}
