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

// FIXME: use this validator also in the subject-itests; such that this is not only covered in container-level tests.
// FIXME-KKL: while this class is currently not used in production code it must be reviewed and revised
public class ComponentPatternValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternValidator.class);

    @Deprecated // may be inline
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
            final Map<String, Set<File>> qualifierToComponentPatternFilesMap = buildQualifierToFileSetMap(filePatternQualifierMapperList);

            // compute duplicate files
            detectDuplicateFiles(qualifierToComponentPatternFilesMap, filePatternQualifierMapperList);

            return filePatternQualifierMapperList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Set<File>> buildQualifierToFileSetMap(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        final Map<String, Set<File>> qualifierToComponentPatternFilesMap = new HashMap<>();
        for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
            final HashSet<File> fileSet = new HashSet<>(filePatternQualifierMapper.getFiles());
            qualifierToComponentPatternFilesMap.put(filePatternQualifierMapper.getQualifier(), fileSet);
        }
        return qualifierToComponentPatternFilesMap;
    }

    private static void detectDuplicateFiles(Map<String, Set<File>> qualifierToFilesMap,
             List<FilePatternQualifierMapper> filePatternQualifierMapperList) {

        for (String parentQualifier : qualifierToFilesMap.keySet()) {
            final Set<File> parentFiles = qualifierToFilesMap.get(parentQualifier);

            for (final String childQualifier : qualifierToFilesMap.keySet()) {
                if (!parentQualifier.equals(childQualifier)) {
                    final Set<File> intersectionFiles = computeIntersection(parentFiles, qualifierToFilesMap.get(childQualifier));
                    if (!intersectionFiles.isEmpty()) {
                        identifyAndLogSubsets(parentQualifier, childQualifier, qualifierToFilesMap, filePatternQualifierMapperList);
                    }
                }
            }
        }
    }

    private static Set<File> computeIntersection(Set<File> leftSet, Set<File> set2) {
        final Set<File> intersection = new HashSet<>(leftSet);
        intersection.retainAll(set2);
        return intersection;
    }

    private static void identifyAndLogSubsets(String parentQualifier, String childQualifier,
                                              Map<String, Set<File>> qualifierToComponentPatternFilesMap,
                                              List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        Map<File, String[]> filesToRemoveFromParent = new HashMap<>();
        Set<File> duplicateAllowedFiles = new HashSet<>();
        Set<File> removedParentFiles = new HashSet<>(qualifierToComponentPatternFilesMap.get(childQualifier));
        Set<File> sharedExcludedFiles = new HashSet<>();
        removedParentFiles.removeAll(qualifierToComponentPatternFilesMap.get(parentQualifier));

        // process removedParentFiles for exclude patterns
        processExcludePatterns(removedParentFiles, parentQualifier, filePatternQualifierMapperList, filesToRemoveFromParent, childQualifier);

        // process removedParentFiles for shared include patterns
        processSharedIncludePatterns(removedParentFiles, parentQualifier, filePatternQualifierMapperList, duplicateAllowedFiles);
        moveFilesToDuplicateAllowed(parentQualifier, filePatternQualifierMapperList, duplicateAllowedFiles);

        // process removedParentFiles for shared exclude patterns
        processSharedExcludePatterns(removedParentFiles, parentQualifier, filePatternQualifierMapperList, sharedExcludedFiles);
        removeSharedExcludedFiles(parentQualifier, filePatternQualifierMapperList, sharedExcludedFiles);

        // remove all files of the processing above from the parent qualifier
        removedParentFiles.removeAll(filesToRemoveFromParent.keySet());

        if (removedParentFiles.isEmpty()) {
            LOG.info("Qualifier [{}] is a subset of qualifier [{}].", childQualifier, parentQualifier);
            removeAllFilesFromParent(parentQualifier, childQualifier, qualifierToComponentPatternFilesMap, filePatternQualifierMapperList);
        }
    }

    private static void moveFilesToDuplicateAllowed(String qualifier,
                                                    List<FilePatternQualifierMapper> filePatternQualifierMapperList,
                                                    Set<File> duplicateAllowedFiles) {
        for (File file : duplicateAllowedFiles) {
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(qualifier)) {
                    final Map<Boolean, List<File>> fileMap = filePatternQualifierMapper.getFileMap();
                    fileMap.get(false).remove(file);
                    if (fileMap.get(true) == null) {
                        fileMap.put(true, new ArrayList<>(Collections.singletonList(file)));
                    } else if (!fileMap.get(true).contains(file)) {
                        fileMap.get(true).add(file);
                    }
                    LOG.info("Moving file [{}] to allowed duplicates for qualifier [{}].", file, qualifier);
                }
            }
        }
    }

    private static void removeSharedExcludedFiles(String qualifier,
            List<FilePatternQualifierMapper> filePatternQualifierMapperList, Set<File> sharedExcludedFiles) {

        for (File file : sharedExcludedFiles) {
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(qualifier)) {
                    filePatternQualifierMapper.getFileMap().get(false).remove(file);
                    LOG.info("Removing file [{}] from qualifier [{}].", file, qualifier);
                }
            }
        }
    }

    private static void processExcludePatterns(Set<File> removedParentFiles, String parentQualifier,
                                               List<FilePatternQualifierMapper> filePatternQualifierMapperList,
                                               Map<File, String[]> filesToRemoveFromParent,
                                               String childQualifier) {
        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String excludePattern = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
                        if (excludePattern != null && !excludePattern.isEmpty()) {
                            if (FileUtils.matches(excludePattern, normalizedPath)) {
                                filesToRemoveFromParent.put(file, new String[]{parentQualifier, childQualifier});
                            }
                        }
                    }
                }
            }
        }
    }

    private static void processSharedIncludePatterns(Set<File> removedParentFiles, String parentQualifier,
                                                     List<FilePatternQualifierMapper> filePatternQualifierMapperList,
                                                     Set<File> duplicateAllowedFiles) {
        List<ArtifactFile> sharedIncludePatternFiles = new ArrayList<>();
        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String sharedIncludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN);
                        if (sharedIncludePattern != null && !sharedIncludePattern.isEmpty()) {
                            if (FileUtils.matches(sharedIncludePattern, normalizedPath)) {
                                // mark the file as allowed for duplicates
                                duplicateAllowedFiles.add(file);
                                ArtifactFile artifactFile = new ArtifactFile(file);
                                artifactFile.addOwningComponent(filePatternQualifierMapper);
                                sharedIncludePatternFiles.add(artifactFile);
                            }
                        }
                    }
                    filePatternQualifierMapper.setSharedIncludedPatternFiles(sharedIncludePatternFiles);
                }
            }
        }
    }


    private static void processSharedExcludePatterns(Set<File> removedParentFiles, String parentQualifier,
                                                     List<FilePatternQualifierMapper> filePatternQualifierMapperList, Set<File> sharedExcludedFiles) {
        List<ArtifactFile> sharedExcludePatternFiles = new ArrayList<>();
        for (File file : removedParentFiles) {
            String normalizedPath = FileUtils.normalizePathToLinux(file);
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String sharedExcludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN);
                        if (sharedExcludePattern != null && !sharedExcludePattern.isEmpty()) {
                            if (FileUtils.matches(sharedExcludePattern, normalizedPath)) {
                                sharedExcludedFiles.add(file);
                                ArtifactFile artifactFile = new ArtifactFile(file);
                                artifactFile.addOwningComponent(filePatternQualifierMapper);
                                sharedExcludePatternFiles.add(artifactFile);
                            }
                        }
                    }
                    filePatternQualifierMapper.setSharedExcludedPatternFiles(sharedExcludePatternFiles);
                }
            }
        }
    }

    private static void removeAllFilesFromParent(String parentQualifier, String childQualifier,
                                                 Map<String, Set<File>> qualifierToComponentPatternFilesMap,
                                                 List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        FilePatternQualifierMapper parentMapper = findMapperForQualifier(parentQualifier, filePatternQualifierMapperList);
        FilePatternQualifierMapper childMapper = findMapperForQualifier(childQualifier, filePatternQualifierMapperList);
        Map<String, List<File>> subsetMap = new HashMap<>();
        if (parentMapper != null) {
            LOG.info("Removing all files of child qualifier [{}] from parent qualifier [{}].", childQualifier, parentQualifier);
            subsetMap.put(childQualifier, new ArrayList<>(qualifierToComponentPatternFilesMap.get(childQualifier)));
            qualifierToComponentPatternFilesMap.get(parentQualifier).removeAll(qualifierToComponentPatternFilesMap.get(childQualifier));
            parentMapper.getFileMap().get(false).removeAll(qualifierToComponentPatternFilesMap.get(childQualifier));
            parentMapper.setSubSetMap(subsetMap);
            if (childMapper != null) {
                final String assetId = "AID-" + parentMapper.getArtifact().getId() + "-" + parentMapper.getArtifact().getChecksum();
                childMapper.getArtifact().set(assetId, Constants.MARKER_CONTAINS);
                parentMapper.getArtifact().set(assetId, Constants.MARKER_CROSS);
            }
        }
    }

    private static FilePatternQualifierMapper findMapperForQualifier(String qualifier, List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        return filePatternQualifierMapperList.stream().filter(mapper -> mapper.getQualifier().equals(qualifier)).findFirst().orElse(null);
    }
}
