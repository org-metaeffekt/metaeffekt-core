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
package org.metaeffekt.core.maven.inventory.extractor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExcludePatternsConfig {
    private Set<String> excludes = new HashSet<>();
    private Map<String, String> artifactIdToVersionMap = new HashMap<>();
    private Set<String> unknownFilePatterns = new HashSet<>();

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes != null ? excludes : new HashSet<>();
    }

    public void setArtifactIdToVersionMap(Map<String, String> map) {
        this.artifactIdToVersionMap = map != null ? map : new HashMap<>();
    }

    public void setUnknownFilePatterns(Set<String> unknownPatternFiles) {
        this.unknownFilePatterns = unknownPatternFiles != null ? unknownPatternFiles : new HashSet<>();
    }
}
