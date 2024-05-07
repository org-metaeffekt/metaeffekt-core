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
package org.metaeffekt.core.inventory.resolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Matcher for {@link ArtifactPattern} instances. The class allows to aggregate multiple {@link ArtifactPattern}
 * instances and provided functions to match against these.
 */
public class ArtifactPatternMatcher {

    private List<ArtifactPattern> artifactPatterns = new ArrayList<>();

    public ArtifactPatternMatcher() {
    }

    public void register(ArtifactPattern artifactGroup) {
        artifactPatterns.add(artifactGroup);
    }

    public ArtifactPattern findMatchingArtifactGroup(String artifact, String component, String version, String effectiveLicense) {
        for (ArtifactPattern candidate : artifactPatterns) {
            boolean matches = matches(artifact, candidate.getArtifactPattern());
            matches &= matches(component, candidate.getComponentPattern());
            matches &= matches(version, candidate.getVersionPattern());
            matches &= matches(effectiveLicense, candidate.getEffectiveLicensePattern());
            if (matches) {
                return candidate;
            }
        }
        return null;
    }

    private boolean matches(String string, String pattern) {
        final String matchString = string == null ? "" : string;
        if (pattern.startsWith("^")) {
            return matchString.matches(pattern);
        } else {
            return matchString.equalsIgnoreCase(pattern);
        }
    }


}
