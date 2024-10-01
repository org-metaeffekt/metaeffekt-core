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
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants;
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

public class DirectoryScanExtractorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryScanExtractorConfiguration.class);

    // FIXME: create InventoryResource that hides the file/object
    final private File referenceInventoryFile;

    final private Inventory referenceInventory;

    final private Inventory resultInventory;

    final private File scanBaseDir;

    final private File scanResultInventoryFile;

    public DirectoryScanExtractorConfiguration(Inventory referenceInventory, Inventory resultInventory, File scanBaseDir) {
        this.scanBaseDir = scanBaseDir;
        this.referenceInventory = referenceInventory;
        this.referenceInventoryFile = null;
        this.scanResultInventoryFile = null;
        this.resultInventory = resultInventory;
    }

    public List<FilePatternQualifierMapper> mapArtifactsToComponentPatterns() throws IOException {

        // load reference inventory
        final Inventory referenceInventory = loadReferenceInventory();

        // load result inventory
        final Inventory resultInventory = loadResultInventory();

        // initialize component pattern map
        final Map<String, List<ComponentPatternData>> qualifierToComponentPatternMap = new HashMap<>();

        final List<FilePatternQualifierMapper> filePatternQualifierMapperList = new ArrayList<>();

        // contribute component pattern from reference inventory
        contributeReferenceComponentPatterns(referenceInventory, qualifierToComponentPatternMap);

        // contribute project component patterns (no overwrite; reference has the control)
        contributeReferenceComponentPatterns(resultInventory, qualifierToComponentPatternMap);

        // iterate extracted artifacts and match with component patterns
        for (Artifact artifact : resultInventory.getArtifacts()) {
            FilePatternQualifierMapper filePatternQualifierMapper = new FilePatternQualifierMapper();
            filePatternQualifierMapper.setArtifact(artifact);

            // check artifact is covered by a component pattern
            String componentName = artifact.getComponent();
            String componentVersion = artifact.getVersion();
            String componentPart = artifact.getId();

            // identify matching component patterns (this may overlap with real artifacts)
            final ComponentPatternMatches componentPatternMatches = findComponentPatternMatches(
                    qualifierToComponentPatternMap, componentName, componentVersion, componentPart);

            // iterate found component patterns for artifact
            if (componentPatternMatches.list != null) {
                Map<Boolean, List<File>> duplicateToComponentPatternFilesMap = new HashMap<>(mapCoveredFilesByDuplicateStatus(artifact, componentPatternMatches, filePatternQualifierMapper));
                filePatternQualifierMapper.setQualifier(componentPart);
                filePatternQualifierMapper.setDerivedQualifier(deriveMapQualifier(componentName, componentPart, componentVersion));
                filePatternQualifierMapper.setPathInAsset(artifact.getPathInAsset());
                List<File> componentPatternFiles = new ArrayList<>();
                for (List<File> files : duplicateToComponentPatternFilesMap.values()) {
                    componentPatternFiles.addAll(files);
                }
                filePatternQualifierMapper.setFiles(componentPatternFiles);
                filePatternQualifierMapper.setFileMap(duplicateToComponentPatternFilesMap);

                filePatternQualifierMapperList.add(filePatternQualifierMapper);
            }
        }

        return filePatternQualifierMapperList;
    }

    private Map<Boolean, List<File>> mapCoveredFilesByDuplicateStatus(Artifact artifact, ComponentPatternMatches componentPatternMatches, FilePatternQualifierMapper filePatternQualifierMapper) {
        HashMap<Boolean, List<File>> duplicateToComponentPatternFilesMap = new HashMap<>();

        // aggregate all covered files into one directory
        for (ComponentPatternData cpd : componentPatternMatches.list) {
            List<File> componentPatternCoveredFiles = new ArrayList<>();
            final String includes = cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
            final String excludes = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
            filePatternQualifierMapper.getComponentPatternDataList().add(cpd);

            // use the projects attribute to pre-select the folder to be scanned; can be multiple
            final Set<String> componentBaseDirs = getRelativePaths(artifact);

            for (final String baseDir : componentBaseDirs) {

                // derive absolute componentBaseDir
                final File componentBaseDir = new File(getExtractedFilesBaseDir(), baseDir);

                final String fileName = componentBaseDir.getName();
                if (fileName.startsWith("[")) {
                    // this may be a scanned artifact; if not existing we need to unpack the file again
                    if (!componentBaseDir.exists()) {
                        File archiveFile = new File(componentBaseDir.getParentFile(), fileName.substring(1, fileName.length() - 1));
                        ArchiveUtils.unpackIfPossible(archiveFile, componentBaseDir, new ArrayList<>());
                    }
                }

                // check the directory (now) exists and can be used to further evaluate the component patterns
                if (componentBaseDir.exists()) {

                    // aggregate matching files to dedicated folders
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Scanning {} including {} excluding {}", componentBaseDir, includes, excludes);
                    }

                    // differentiate directories and single files
                    if (componentBaseDir.isDirectory()) {
                        aggregateComponentFiles(cpd, getExtractedFilesBaseDir(), componentBaseDir, includes, excludes, componentPatternCoveredFiles);
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

    private void aggregateComponentFiles(ComponentPatternData cpd, File baseDir, File componentBaseDir, String includes, String excludes, List<File> componentPatternCoveredFiles) {

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

    private void contributeReferenceComponentPatterns(Inventory referenceInventory, Map<String, List<ComponentPatternData>> componentPatternMap) {
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
                        cpd.get(ComponentPatternData.Attribute.COMPONENT_PART),
                        cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION)), list);
            }
            list.add(cpd);
        }
    }

    private static Set<String> getRelativePaths(Artifact artifact) {
        // use information from projects (relative to original scanBaseDir)
        final Set<String> componentBaseDirs = new HashSet<>(artifact.getProjects());

        // in case information is not available fall back to ATTRIBUTE_KEY_ARTIFACT_PATH (also relative to original scanBaseDir)
        if (componentBaseDirs.isEmpty()) {
            final String artifactPath = artifact.get(FileSystemScanConstants.ATTRIBUTE_KEY_ARTIFACT_PATH);
            if (!StringUtils.isBlank(artifactPath)) {
                componentBaseDirs.add(artifactPath);
            }
        }
        return componentBaseDirs;
    }

    private File getExtractedFilesBaseDir() {
        return scanBaseDir;
    }

    private ComponentPatternMatches findComponentPatternMatches(Map<String, List<ComponentPatternData>> componentPatternMap, String componentName, String componentVersion, String componentPart) {
        ComponentPatternMatches componentPatternMatch = new ComponentPatternMatches();
        componentPatternMatch.key = deriveMapQualifier(componentName, componentPart, componentVersion);
        componentPatternMatch.list = componentPatternMap.get(componentPatternMatch.key);
        if (componentPatternMatch.list == null) {
            String modulatedKey = deriveFallbackMapQualifier(componentPart, componentVersion);
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
        sb.append(componentPart);
        if (!StringUtils.isBlank(componentVersion)) {
            sb.append("-");
            sb.append(componentVersion);
        }
        return sb.toString();
    }

    private String deriveMapQualifier(String componentName, String componentPart, String componentVersion) {
        final StringBuilder sb = new StringBuilder();
        if (!StringUtils.isBlank(componentName)) {
            sb.append(componentName).append("-");
        }
        sb.append(componentPart);
        if (!StringUtils.isBlank(componentVersion)) {
            sb.append(componentVersion);
        }
        return sb.toString();
    }

    private String deriveMapQualifier(ComponentPatternData cpd) {
        return deriveMapQualifier(
            cpd.get(ComponentPatternData.Attribute.COMPONENT_NAME),
            cpd.get(ComponentPatternData.Attribute.COMPONENT_PART),
            cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
    }

    public File getResultInventoryFile() {
        return scanResultInventoryFile;
    }

}
