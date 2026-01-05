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

package org.metaeffekt.core.inventory.processor.model;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.filescan.ArtifactFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects information for file aggregation and intersection management.
 */
@Getter
@Setter
public class FilePatternQualifierMapper {

    /**
     * The artifact for which information is aggregated. May represent a single file or a component-pattern-derived
     * group.
     */
    private Artifact artifact;

    /**
     * Qualifier based on component part (artifact id), component name and version.
     */
    private String qualifier;

    /**
     * The list of files covered (single file or matching files for component-pattern-derived artifacts).
     */
    private List<File> files;

    /**
     * Mapping a boolean to a list of files.
     *
     * In the 'true' bucket xxx are collected.
     *
     * In the 'false' bucket all XXX are collected.
     */
    private Map<Boolean, List<File>> fileMap;

    /**
     * List of ComponentPatternData contributing to the artifact.
     */
    private List<ComponentPatternData> componentPatternDataList;

    /**
     * The path in asset for the given artifact or the component pattern anchor.
     */
    private String pathInAsset;

    /**
     * All included files with parent / owner containment information
     */
    private List<ArtifactFile> includedComponentFiles;

    /**
     * All files managed as shared includes.
     */
    private List<ArtifactFile> sharedIncludedPatternFiles;

    /**
     * All files managed as shared excludes.
     */
    private List<ArtifactFile> sharedExcludedPatternFiles;

    // FIXME: review and comment required.
    private Map<String, List<File>> subSetMap;

    /**
     * Used to prohibit mutual deletion.
     */
    private boolean locked;

    public FilePatternQualifierMapper() {
        this.files = new ArrayList<>();
        this.componentPatternDataList = new ArrayList<>();
    }

}
