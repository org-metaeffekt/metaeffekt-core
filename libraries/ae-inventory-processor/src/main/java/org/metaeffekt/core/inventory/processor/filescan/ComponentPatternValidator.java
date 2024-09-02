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

import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanExtractorConfiguration;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
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

    public static boolean removeDuplicateComponentPatterns(Inventory referenceInventory, Inventory resultInventory, File baseDir) {

        DirectoryScanExtractorConfiguration configuration = new DirectoryScanExtractorConfiguration(
                referenceInventory,
                resultInventory,
                baseDir
        );

        List<FilePatternQualifierMapper> filePatternQualifierMapperList = loadComponentPatternFiles(configuration);
        Map<String, Set<File>> qualifierToComponentPatternFilesMap = buildQualifierToFileSetMap(filePatternQualifierMapperList);

        findDuplicateFiles(qualifierToComponentPatternFilesMap, filePatternQualifierMapperList);

        return hasRemainingDuplicates(filePatternQualifierMapperList);
    }

    private static List<FilePatternQualifierMapper> loadComponentPatternFiles(DirectoryScanExtractorConfiguration configuration) {
        try {
            return configuration.mapArtifactsToComponentPatterns();
        } catch (IOException e) {

            throw new RuntimeException(e);
        }
    }

    private static Map<String, Set<File>> buildQualifierToFileSetMap(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        Map<String, Set<File>> qualifierToComponentPatternFilesMap = new HashMap<>();
        for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
            qualifierToComponentPatternFilesMap.put(filePatternQualifierMapper.getQualifier(), new HashSet<>(filePatternQualifierMapper.getFiles()));
        }
        return qualifierToComponentPatternFilesMap;
    }

    private static void findDuplicateFiles(Map<String, Set<File>> qualifierToComponentPatternFilesMap, List<FilePatternQualifierMapper> filePatternQualifierMapperList) {

        for (String parentQualifier : qualifierToComponentPatternFilesMap.keySet()) {
            Set<File> parentFiles = qualifierToComponentPatternFilesMap.get(parentQualifier);

            for (String childQualifier : qualifierToComponentPatternFilesMap.keySet()) {
                if (!parentQualifier.equals(childQualifier)) {
                    Set<File> intersectionFiles = findIntersection(parentFiles, qualifierToComponentPatternFilesMap.get(childQualifier));

                    if (!intersectionFiles.isEmpty()) {
                        identifyAndLogSubsets(parentQualifier, childQualifier, qualifierToComponentPatternFilesMap, filePatternQualifierMapperList);
                    }
                }
            }
        }

    }

    private static Set<File> findIntersection(Set<File> set1, Set<File> set2) {
        Set<File> intersection = new HashSet<>(set1);
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
            LOG.warn("Qualifier [{}] is a subset of qualifier [{}].", childQualifier, parentQualifier);
            removeAllFilesFromParent(parentQualifier, childQualifier, qualifierToComponentPatternFilesMap, filePatternQualifierMapperList);
        }
    }

    private static void moveFilesToDuplicateAllowed(String qualifier,
                                                    List<FilePatternQualifierMapper> filePatternQualifierMapperList,
                                                    Set<File> duplicateAllowedFiles) {
        for (File file : duplicateAllowedFiles) {
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(qualifier)) {
                    filePatternQualifierMapper.getFileMap().get(false).remove(file);
                    if (filePatternQualifierMapper.getFileMap().get(true) == null) {
                        filePatternQualifierMapper.getFileMap().put(true, new ArrayList<>(Collections.singletonList(file)));
                    } else if (!filePatternQualifierMapper.getFileMap().get(true).contains(file)) {
                        filePatternQualifierMapper.getFileMap().get(true).add(file);
                    }
                    LOG.info("Moving file [{}] to allowed duplicates for qualifier [{}].", file, qualifier);
                }
            }
        }
    }

    private static void removeSharedExcludedFiles(String qualifier,
                                                  List<FilePatternQualifierMapper> filePatternQualifierMapperList,
                                                  Set<File> sharedExcludedFiles) {
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
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String excludePattern = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
                        if (excludePattern != null && !excludePattern.isEmpty()) {
                            if (FileUtils.matches(excludePattern, FileUtils.normalizePathToLinux(file))) {
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
        for (File file : removedParentFiles) {
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String sharedIncludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN);
                        if (sharedIncludePattern != null && !sharedIncludePattern.isEmpty()) {
                            if (FileUtils.matches(sharedIncludePattern, FileUtils.normalizePathToLinux(file))) {
                                // mark the file as allowed for duplicates
                                duplicateAllowedFiles.add(file);
                                /*List<File> files = filePatternQualifierMapper.getFileMap().get(true);
                                if (files == null) {
                                    filePatternQualifierMapper.getFileMap().put(true, new ArrayList<>(Collections.singletonList(file)));
                                    LOG.info("Adding file [{}] to allowed duplicates to be collected for qualifier [{}]. ", file, filePatternQualifierMapper.getQualifier());
                                } else if (!files.contains(file)) {
                                    filePatternQualifierMapper.getFileMap().get(true).add(file);
                                    LOG.info("Adding file [{}] to allowed duplicates to be collected for qualifier [{}]. ", file, filePatternQualifierMapper.getQualifier());
                                }
                                filePatternQualifierMapper.getFileMap().get(false).remove(file);*/
                            }
                        }
                    }
                }
            }
        }
    }


    private static void processSharedExcludePatterns(Set<File> removedParentFiles, String parentQualifier,
                                                     List<FilePatternQualifierMapper> filePatternQualifierMapperList, Set<File> sharedExcludedFiles) {
        for (File file : removedParentFiles) {
            for (FilePatternQualifierMapper filePatternQualifierMapper : filePatternQualifierMapperList) {
                if (filePatternQualifierMapper.getQualifier().equals(parentQualifier)) {
                    for (ComponentPatternData cpd : filePatternQualifierMapper.getComponentPatternDataList()) {
                        String sharedExcludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN);
                        if (sharedExcludePattern != null && !sharedExcludePattern.isEmpty()) {
                            if (FileUtils.matches(sharedExcludePattern, FileUtils.normalizePathToLinux(file))) {
                                sharedExcludedFiles.add(file);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void removeAllFilesFromParent(String parentQualifier, String childQualifier,
                                                 Map<String, Set<File>> qualifierToComponentPatternFilesMap,
                                                 List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        FilePatternQualifierMapper parentMapper = findMapperForQualifier(parentQualifier, filePatternQualifierMapperList);
        // TODO: implement logic so if a package modifies a file of a parent qualifier, the child qualifier package is now the true owner of the file, this can be done by a checksum check
        if (parentMapper != null) {
            LOG.info("Removing all files of child qualifier [{}] from parent qualifier [{}].", childQualifier, parentQualifier);
            qualifierToComponentPatternFilesMap.get(parentQualifier).removeAll(qualifierToComponentPatternFilesMap.get(childQualifier));
            parentMapper.getFileMap().get(false).removeAll(qualifierToComponentPatternFilesMap.get(childQualifier));
        }
    }

    private static FilePatternQualifierMapper findMapperForQualifier(String qualifier, List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        return filePatternQualifierMapperList.stream().filter(mapper -> mapper.getQualifier().equals(qualifier)).findFirst().orElse(null);
    }

    private static boolean hasRemainingDuplicates(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        Map<File, List<String>> fileToQualifierMap = new HashMap<>();
        boolean hasDuplicates = false;

        for (FilePatternQualifierMapper mapper : filePatternQualifierMapperList) {
            List<File> files = mapper.getFileMap().get(false);
            if (files != null) {
                for (File file : files) {
                    if (fileToQualifierMap.containsKey(file) && !fileToQualifierMap.get(file).contains(mapper.getQualifier())) {
                        fileToQualifierMap.get(file).add(mapper.getQualifier());
                    } else if (!fileToQualifierMap.containsKey(file)) {
                        fileToQualifierMap.computeIfAbsent(file, k -> new ArrayList<>()).add(mapper.getQualifier());
                    }
                }
            }
        }

        for (Map.Entry<File, List<String>> entry : fileToQualifierMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                LOG.warn("Component pattern file [{}] is STILL associated with multiple qualifiers {}.", entry.getKey(), entry.getValue());
                hasDuplicates = true;
            }
        }

        return hasDuplicates;
    }
}
