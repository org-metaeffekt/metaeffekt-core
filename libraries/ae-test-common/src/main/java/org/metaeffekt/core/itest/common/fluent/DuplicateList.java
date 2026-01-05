/*
 * Copyright 2009-2026 the original author or authors.
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

package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicateList {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicateList.class);

    private final Map<File, List<String>> fileToQualifierMap = new HashMap<>();

    private final Map<File, String> fileWithoutDuplicates = new HashMap<>();

    private final Map<File, List<String>> remainingDuplicates = new HashMap<>();

    private final Map<File, List<String>> allowedDuplicates = new HashMap<>();

    public DuplicateList(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        collectAllowedDuplicates(filePatternQualifierMapperList);
        collectRemainingDuplicates(filePatternQualifierMapperList);
    }

    public void identifyRemainingDuplicatesWithoutArtifact(String... filterArtifact) {
        List<String> filters = (filterArtifact != null && filterArtifact.length > 0)
                ? Arrays.stream(filterArtifact).collect(Collectors.toList())
                : Collections.emptyList();

        for (Map.Entry<File, List<String>> entry : fileToQualifierMap.entrySet()) {
            // check if we should filter the entry
            boolean containsFilter = !filters.isEmpty() && entry.getValue().stream()
                    .anyMatch(value -> filters.stream().anyMatch(value::contains));

            if (entry.getValue().size() > 1 && !containsFilter) {
                LOG.warn("Component pattern file [{}] is STILL associated with multiple qualifiers {}.", entry.getKey(), entry.getValue());
                remainingDuplicates.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue().size() == 1 && !containsFilter) {
                fileWithoutDuplicates.put(entry.getKey(), entry.getValue().get(0));
            }
        }
    }

    public void identifyRemainingDuplicatesWithoutFile(String... filterFile) {
        List<String> filters = (filterFile != null && filterFile.length > 0)
                ? Arrays.stream(filterFile).collect(Collectors.toList())
                : Collections.emptyList();

        for (Map.Entry<File, List<String>> entry : fileToQualifierMap.entrySet()) {
            // check if the file name contains any of the filter keywords
            boolean containsFilter = !filters.isEmpty() && filters.stream()
                    .anyMatch(filter -> entry.getKey().getName().contains(filter));

            if (entry.getValue().size() > 1 && !containsFilter) {
                LOG.warn("Component pattern file [{}] is STILL associated with multiple qualifiers {}.",
                        entry.getKey(), entry.getValue());
                remainingDuplicates.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue().size() == 1 && !containsFilter) {
                fileWithoutDuplicates.put(entry.getKey(), entry.getValue().get(0));
            }
        }
    }


    private void collectRemainingDuplicates(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        for (FilePatternQualifierMapper mapper : filePatternQualifierMapperList) {
            mapFilesToQualifiers(mapper.getQualifier(), mapper.getFileMap().get(false), fileToQualifierMap);
        }
    }

    private void collectAllowedDuplicates(List<FilePatternQualifierMapper> filePatternQualifierMapperList) {
        for (FilePatternQualifierMapper mapper : filePatternQualifierMapperList) {
            mapFilesToQualifiers(mapper.getQualifier(), mapper.getFileMap().get(true), allowedDuplicates);
        }
    }

    private void mapFilesToQualifiers(String qualifier, List<File> files, Map<File, List<String>> map) {
        if (files != null) {
            for (File file : files) {
                if (map.containsKey(file) && !map.get(file).contains(qualifier)) {
                    map.get(file).add(qualifier);
                } else if (!map.containsKey(file)) {
                    map.computeIfAbsent(file, k -> new ArrayList<>()).add(qualifier);
                }
            }
        }
    }

    public Map<File, List<String>> getAllowedDuplicates() {
        return allowedDuplicates;
    }

    public Map<File, List<String>> getRemainingDuplicates() {
        return remainingDuplicates;
    }

    public Map<File, String> getFileWithoutDuplicates() {
        return fileWithoutDuplicates;
    }
}
