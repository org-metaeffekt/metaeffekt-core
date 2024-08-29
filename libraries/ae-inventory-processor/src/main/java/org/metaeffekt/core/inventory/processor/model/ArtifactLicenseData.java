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
package org.metaeffekt.core.inventory.processor.model;


import java.util.ArrayList;
import java.util.List;

/**
 * Data container for artifacts sharing a {@link LicenseMetaData}. Currently, this is merely used as container / grouping
 * of artifacts.
 *
 * @author Karsten Klein
 */
public class ArtifactLicenseData {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    private final List<Artifact> artifacts;

    private final String componentName;

    private final String componentVersion;

    private String qualifier;

    public ArtifactLicenseData(String componentName, String componentVersion, String qualifier) {
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.qualifier = qualifier;
        this.artifacts = new ArrayList<>();
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void add(Artifact artifact) {
        artifacts.add(artifact);
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String deriveComponentQualifierForCounting() {
        // in case name and version are available we use this as unique component qualifer
        if (componentName != null && componentVersion != null) {
            return componentName + "|" + componentVersion;
        } else {
            // if one or the other misses we use the full qualifier for grouping and counting
            return qualifier;
        }
    }
}
