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

import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanAggregatorConfiguration;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ComponentPatternValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternValidator.class);

    public static List<FilePatternQualifierMapper> evaluateComponentPatterns(Inventory referenceInventory, Inventory resultInventory, File baseDir) {
        final DirectoryScanAggregatorConfiguration configuration =
                new DirectoryScanAggregatorConfiguration(referenceInventory, resultInventory, baseDir);
        return evaluateComponentPatterns(configuration);
    }

    private static List<FilePatternQualifierMapper> evaluateComponentPatterns(final DirectoryScanAggregatorConfiguration configuration) {
        try {
            // establish qualifier mappers
            final List<FilePatternQualifierMapper> filePatternQualifierMapperList = configuration.mapArtifactsToCoveredFiles();

            // build map from qualifiers to file sets
            final Map<String, FilePatternQualifierMapper> qualifierToMapperMap = buildQualifierToMapperMap(filePatternQualifierMapperList);

            // compute duplicate files
            detectDuplicateFiles(qualifierToMapperMap);

            return filePatternQualifierMapperList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, FilePatternQualifierMapper> buildQualifierToMapperMap(List<FilePatternQualifierMapper> mapperList) {
        final Map<String, FilePatternQualifierMapper> qualifierToMapperMap = new HashMap<>();
        for (FilePatternQualifierMapper mapper : mapperList) {
            qualifierToMapperMap.put(mapper.getQualifier(), mapper);
        }
        return qualifierToMapperMap;
    }

    private static void detectDuplicateFiles(final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        final Set<String> qualifiers = qualifierToMapperMap.keySet();

        for (String leftQualifier : qualifiers) {
            final FilePatternQualifierMapper leftMapper = qualifierToMapperMap.get(leftQualifier);
            final List<File> leftFiles = leftMapper.getFiles();

            if (leftFiles.isEmpty()) continue;

            for (final String rightQualifier : qualifiers) {
                // each pair is processed once
                if (leftQualifier.equals(rightQualifier)) break;

                final FilePatternQualifierMapper rightMapper = qualifierToMapperMap.get(rightQualifier);
                final List<File> rightFiles = rightMapper.getFiles();

                if (rightFiles.isEmpty()) continue;

                if (!hasIntersection(leftFiles, rightFiles)) continue;

                // for each tuple both directions are evaluated...

                identifyAndLogSubsets(leftQualifier, rightQualifier, leftFiles, rightFiles, qualifierToMapperMap);
                identifyAndLogSubsets(rightQualifier, leftQualifier, rightFiles, leftFiles, qualifierToMapperMap);
            }
        }
    }

    private static boolean hasIntersection(Collection<File> leftSet, Collection<File> rightSet) {
        if (leftSet.size() > rightSet.size()) {
            for (final File file : rightSet) {
                if (leftSet.contains(file)) return true;
            }
        } else {
            for (final File file : leftSet) {
                if (rightSet.contains(file)) return true;
            }
        }
        return false;
    }

    private static void identifyAndLogSubsets(
            String parentQualifier, String childQualifier,
            Collection<File> parentFiles, Collection<File> childFiles,
            Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        final Set<File> duplicateAllowedFiles = new HashSet<>();
        final Set<File> sharedExcludedFiles = new HashSet<>();

        final Set<File> childOnlyFiles = new HashSet<>(childFiles);
        childOnlyFiles.removeAll(parentFiles);

        // process child-level files to identify whether there are files which where explicit excluded on parent-level; these must not contribute to the coverage criteria
        final Set<File> filesToRemoveFromParent = collectFilesExcludedOnParentLevel(childOnlyFiles, parentQualifier, qualifierToMapperMap);

        // process childOnlyFiles for shared include patterns
        processSharedIncludePatterns(childOnlyFiles, parentQualifier, duplicateAllowedFiles, qualifierToMapperMap);
        moveFilesToDuplicateAllowed(parentQualifier, duplicateAllowedFiles, qualifierToMapperMap);

        // process childOnlyFiles for shared exclude patterns
        processSharedExcludePatterns(childOnlyFiles, parentQualifier, sharedExcludedFiles, qualifierToMapperMap);
        removeSharedExcludedFiles(parentQualifier, sharedExcludedFiles, qualifierToMapperMap);

        // remove the excluded files from the childOnlyFiles; these may obscure that child if fully covered by parent
        childOnlyFiles.removeAll(filesToRemoveFromParent);

        // if (intersectionFiles.size() == childFiles.size()) {
        if (childOnlyFiles.isEmpty()) {
            LOG.info("Qualifier [{}] is a full subset of qualifier [{}].", childQualifier, parentQualifier);
            removeAllFilesFromParent(parentQualifier, childQualifier, parentFiles, qualifierToMapperMap);
        }
    }

    private static void moveFilesToDuplicateAllowed(
            String qualifier, Set<File> duplicateAllowedFiles,
            final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        final FilePatternQualifierMapper mapper = qualifierToMapperMap.get(qualifier);

        final Map<Boolean, List<File>> fileMap = mapper.getFileMap();
        for (final File file : duplicateAllowedFiles) {
            LOG.info("Moving file [{}] to allowed duplicates for qualifier [{}].", file, qualifier);
            fileMap.get(false).remove(file);
            fileMap.computeIfAbsent(true, a -> new ArrayList<>()).add(file);
        }
    }

    private static void removeSharedExcludedFiles(String qualifier,
            Set<File> sharedExcludedFiles,
            final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        FilePatternQualifierMapper mapper = qualifierToMapperMap.get(qualifier);

        for (File file : sharedExcludedFiles) {
            mapper.getFileMap().get(false).remove(file);
            LOG.info("Removing file [{}] from qualifier [{}].", file, qualifier);
        }
    }

    private static Set<File> collectFilesExcludedOnParentLevel(
            Set<File> removedParentFiles, String parentQualifier,
            final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        final Set<File> filesToRemoveFromParent = new HashSet<>();

        final FilePatternQualifierMapper parentMapper = qualifierToMapperMap.get(parentQualifier);

        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (ComponentPatternData cpd : parentMapper.getComponentPatternDataList()) {
                String excludePattern = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
                if (excludePattern != null && !excludePattern.isEmpty()) {
                    if (FileUtils.matches(excludePattern, normalizedPath)) {
                        filesToRemoveFromParent.add(file);
                    }
                }
            }
        }

        return filesToRemoveFromParent;
    }

    private static void processSharedIncludePatterns(Set<File> removedParentFiles, String parentQualifier,
                                                     Set<File> duplicateAllowedFiles,
                                                     Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        FilePatternQualifierMapper parentMapper = qualifierToMapperMap.get(parentQualifier);

        List<ArtifactFile> sharedIncludePatternFiles = new ArrayList<>();
        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (ComponentPatternData cpd : parentMapper.getComponentPatternDataList()) {
                String sharedIncludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN);
                if (sharedIncludePattern != null && !sharedIncludePattern.isEmpty()) {
                    if (FileUtils.matches(sharedIncludePattern, normalizedPath)) {
                        // mark the file as allowed for duplicates
                        duplicateAllowedFiles.add(file);
                        ArtifactFile artifactFile = new ArtifactFile(file);
                        artifactFile.addOwningComponent(parentMapper);
                        sharedIncludePatternFiles.add(artifactFile);
                    }
                }
            }
            parentMapper.setSharedIncludedPatternFiles(sharedIncludePatternFiles);
        }
    }


    private static void processSharedExcludePatterns(
            Set<File> removedParentFiles, String parentQualifier, Set<File> sharedExcludedFiles,
            final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        FilePatternQualifierMapper parentQualifierMapper = qualifierToMapperMap.get(parentQualifier);

        List<ArtifactFile> sharedExcludePatternFiles = new ArrayList<>();
        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (ComponentPatternData cpd : parentQualifierMapper.getComponentPatternDataList()) {
                String sharedExcludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN);
                if (sharedExcludePattern != null && !sharedExcludePattern.isEmpty()) {
                    if (FileUtils.matches(sharedExcludePattern, normalizedPath)) {
                        sharedExcludedFiles.add(file);
                        ArtifactFile artifactFile = new ArtifactFile(file);
                        artifactFile.addOwningComponent(parentQualifierMapper);
                        sharedExcludePatternFiles.add(artifactFile);
                    }
                }
            }
            parentQualifierMapper.setSharedExcludedPatternFiles(sharedExcludePatternFiles);
        }
    }

    private static void removeAllFilesFromParent(String parentQualifier, String childQualifier,
             Collection<File> childFileSet,
             final Map<String, FilePatternQualifierMapper> qualifierToMapperMap) {

        final FilePatternQualifierMapper parentMapper = qualifierToMapperMap.get(parentQualifier);
        final FilePatternQualifierMapper childMapper = qualifierToMapperMap.get(childQualifier);
        final Map<String, List<File>> subsetMap = new HashMap<>();

        if (parentMapper != null) {
            LOG.info("Removing all files of child qualifier [{}] from parent qualifier [{}].", childQualifier, parentQualifier);

            subsetMap.put(childQualifier, new ArrayList<>(childFileSet));

            final List<File> parentMapperFileList_false = parentMapper.getFileMap().get(false);
            parentMapperFileList_false.removeAll(childFileSet);

            parentMapper.setSubSetMap(subsetMap);
            if (childMapper != null) {
                final String assetId = "AID-" + parentMapper.getArtifact().getId() + "-" + parentMapper.getArtifact().getChecksum();
                childMapper.getArtifact().set(assetId, Constants.MARKER_CONTAINS);
                parentMapper.getArtifact().set(assetId, Constants.MARKER_CROSS);
            }
        }
    }

}
