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
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.Project;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.metaeffekt.core.inventory.processor.patterns.contributors.ContributorUtils;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_ARCHIVE_PATH;

public class DirectoryScanAggregatorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryScanAggregatorConfiguration.class);

    final private File referenceInventoryFile;

    final private Inventory referenceInventory;

    final private Inventory resultInventory;

    final private File scanBaseDir;

    final private File scanResultInventoryFile;

    public DirectoryScanAggregatorConfiguration(Inventory referenceInventory, Inventory resultInventory, File scanBaseDir) {
        this.scanBaseDir = scanBaseDir;
        this.referenceInventory = referenceInventory;
        this.referenceInventoryFile = null;
        this.scanResultInventoryFile = null;
        this.resultInventory = resultInventory;
    }

    public List<FilePatternQualifierMapper> mapArtifactsToCoveredFiles() throws IOException {

        // load reference inventory
        final Inventory referenceInventory = loadReferenceInventory();

        // load result inventory
        final Inventory resultInventory = loadResultInventory();

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

            // identify matching component patterns (this may overlap with real artifacts)
            final ComponentPatternMatches componentPatternMatches =
                    findComponentPatternMatches(qualifierToComponentPatternMap, componentName, componentVersion, componentPart);

            // build FilePatternQualifierMapper baseline
            final FilePatternQualifierMapper filePatternQualifierMapper = new FilePatternQualifierMapper();
            filePatternQualifierMapper.setArtifact(artifact);
            filePatternQualifierMapper.setQualifier(componentPart);

            final String derivedQualifier = deriveMapQualifier(componentName, componentPart, componentVersion);
            filePatternQualifierMapper.setDerivedQualifier(derivedQualifier);
            filePatternQualifierMapper.setPathInAsset(artifact.getPathInAsset());

            // iterate found component patterns for artifact
            if (componentPatternMatches.list != null) {
                final Map<Boolean, List<File>> duplicateToComponentPatternFilesMap =
                    new HashMap<>(mapCoveredFilesByDuplicateStatus(artifact, componentPatternMatches, filePatternQualifierMapper));
                filePatternQualifierMapper.setFileMap(duplicateToComponentPatternFilesMap);

                // collect component-pattern-covered files
                final List<File> componentPatternFiles = new ArrayList<>();
                for (List<File> files : duplicateToComponentPatternFilesMap.values()) {
                    componentPatternFiles.addAll(files);
                }

                filePatternQualifierMapper.setFiles(componentPatternFiles);
            } else {
                // handle artifacts that cannot be mapped to files; we need that the inventory is completely represented
                // even in case no files are directly or indirectly associated
                filePatternQualifierMapper.setFileMap(Collections.emptyMap());
                filePatternQualifierMapper.setFiles(Collections.emptyList());
                // FIXME: add comprehensive test case for this
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
            final Set<String> componentBaseDirs = getRelativePaths(artifact);

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
                    // this may be a unpacked artifact; if not existing we need to unpack the file again
                    if (!componentBaseDir.exists()) {
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

    private void aggregateComponentFiles(File baseDir, File componentBaseDir, String includes, String excludes, List<File> componentPatternCoveredFiles) {

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

    private Inventory loadResultInventory() throws IOException {
        if (resultInventory != null) {
            return resultInventory;
        } else {
            final File scanResultInventoryFile = getResultInventoryFile();
            FileUtils.validateExists(scanResultInventoryFile);
            final Inventory inventory = new InventoryReader().readInventory(scanResultInventoryFile);
            return inventory;
        }
    }

    private Inventory loadReferenceInventory() throws IOException {
        final Inventory referenceInventory;
        if (referenceInventoryFile != null) {
            FileUtils.validateExists(referenceInventoryFile);
            if (referenceInventoryFile.isDirectory()) {
                referenceInventory = InventoryUtils.readInventory(referenceInventoryFile, "*.xls");
            } else {
                referenceInventory = new InventoryReader().readInventory(referenceInventoryFile);
            }
        } else {
            Validate.notNull(this.referenceInventory);
            referenceInventory = this.referenceInventory;
        }
        return referenceInventory;
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

    // FIXME: this is bridle; we need to consolidate this
    private static Set<String> getRelativePaths(Artifact artifact) {
        // use information from projects (relative to original scanBaseDir)
        final Set<String> componentBaseDirs = new HashSet<>(artifact.getProjects());

        // in case information is not available fall back to PATH_IN_ASSET (also relative to original scanBaseDir)
        if (componentBaseDirs.isEmpty()) {
            final String artifactPath = artifact.get(Artifact.Attribute.PATH_IN_ASSET);
            if (!StringUtils.isBlank(artifactPath)) {
                componentBaseDirs.add(artifactPath);
            }
        }
        return componentBaseDirs;
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
        if (StringUtils.isNotBlank(componentName)) {
            sb.append(componentName);
        }
        sb.append("-");
        // NOTE: the componentPart is the artifact id; it is usually not blank; we nevertheless treat it equivalently
        if (StringUtils.isNotBlank(componentPart)) {
            sb.append(componentPart);
        }
        sb.append("-");
        if (StringUtils.isNotBlank(componentVersion)) {
            sb.append(componentVersion);
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
            // 1. produce one file with all ArtifactFile types

            final List<FilePatternQualifierMapper> filePatternQualifierMappers =
                    ComponentPatternValidator.evaluateComponentPatterns(referenceInventory, resultInventory, scanBaseDir);

            // 2. analyze component containment
            for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
                final String assetId = "AID-" + mapper.getArtifact().getId() + "-" + mapper.getArtifact().getChecksum();
                if (mapper.getSubSetMap() != null) {
                    for (String qualifier : mapper.getSubSetMap().keySet()) {
                        Artifact foundArtifact = resultInventory.getArtifacts().stream()
                                .filter(a -> matchQualifierToIdOrDerivedQualifier(qualifier, a))
                                .findFirst().orElse(null);
                        if (foundArtifact != null) {
                            String marker = foundArtifact.get(assetId);
                            if (!marker.equals(MARKER_CONTAINS) && !marker.equals(MARKER_CROSS)) {
                                LOG.error("Artifact [{}] does not contain asset [{}]", foundArtifact.getId(), assetId);
                            }
                        }
                    }
                }
            }

            // 3. build zips for all components
            aggregateFilesForAllArtifacts(scanBaseDir, filePatternQualifierMappers, aggregationDir);
        }
    }

    private void aggregateFilesForAllArtifacts(File scanBaseDir, List<FilePatternQualifierMapper> filePatternQualifierMappers, File targetDir) {

        final Set<Artifact> coveredArtifacts = new HashSet<>();

        // create an ant project
        Project antProject = new Project();
        antProject.init();

        for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
            final File tmpFolder = FileUtils.initializeTmpFolder(targetDir);

            // FIXME: why isn't that always the mapper.artifact
            final Artifact foundArtifact = resultInventory.getArtifacts().stream()
                    .filter(artifact -> matchQualifierToIdOrDerivedQualifier(mapper.getQualifier(), artifact))
                    .findFirst().orElse(null);
            try {
                // loop over each entry in the file map
                for (Map.Entry<Boolean, List<File>> entry : mapper.getFileMap().entrySet()) {
                    final List<File> files = entry.getValue();
                    if (files.isEmpty()) {
                        continue;
                    }

                    coveredArtifacts.add(mapper.getArtifact());

                    final File commonRootDir = determineCommonRootDir(scanBaseDir, files);

                    // add each file to the zip
                    for (File file : files) {
                        // copy the file to the tmp folder; use full path from scan
                        final String relativePath = FileUtils.asRelativePath(commonRootDir, file);
                        FileUtils.copyFile(file, new File(tmpFolder, relativePath));
                    }

                    final File contentChecksumFile = new File(tmpFolder, mapper.getArtifact().getId() + ".content.md5");
                    FileUtils.createDirectoryContentChecksumFile(tmpFolder, contentChecksumFile);

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

                    ArchiveUtils.zipAnt(tmpFolder, zipFile);
                    if (!zipFile.exists()) {
                        // protocol as error and continue
                        throw new IllegalStateException("Failed to create zip file for artifact: [" + mapper.getArtifact().getId() + "]");
                    }
                }
            } catch (IOException e) {
                LOG.error("Error processing artifact: [{}].", mapper.getArtifact().getId(), e);
            } finally {
                // ensure the tmp folder is deleted
                FileUtils.deleteDirectoryQuietly(tmpFolder);
            }
        }

        // copy remaining artifacts not covered by component-patterns to aggregation dir
        for (Artifact artifact : resultInventory.getArtifacts()) {
            if (!coveredArtifacts.contains(artifact)) {
                for (String project : artifact.getProjects()) {
                    File file = new File(scanBaseDir, project);
                    if (file.exists()) {
                        final String relativePath = FileUtils.asRelativePath(scanBaseDir, file);
                        try {
                            FileUtils.copyFile(file, new File(targetDir, relativePath));
                        } catch (IOException e) {
                            LOG.warn("Cannot copy file [{}] to aggregation folder [{}]", file.getAbsolutePath(), targetDir.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private File determineCommonRootDir(File scanBaseDir, List<File> files) {
        if (files == null || files.isEmpty()) return scanBaseDir;

        File guess = files.get(0).getParentFile();

        boolean commonRoot = true;
        do {
            for (File file : files) {
                final String path = guess.getPath();
                if (!file.getPath().startsWith(path)) {
                    commonRoot = false;
                    break;
                }
            }

            if (!commonRoot) {
                // try next level up
                guess = guess.getParentFile();
            }
        } while (!commonRoot && guess != null);

        if (guess == null) return scanBaseDir;

        return guess;
    }

    public static boolean matchQualifierToIdOrDerivedQualifier(String qualifier, Artifact a) {
        return a.getId().equals(qualifier) || qualifier.equals(deriveQualifier(a));
    }

    private static String deriveQualifier(Artifact a) {
        return DirectoryScanAggregatorConfiguration.deriveMapQualifier(a.getComponent(), a.getVersion(), a.getId());
    }

}
