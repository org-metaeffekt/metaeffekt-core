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

import java.util.HashSet;
import java.util.Set;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;


public class PatternArtifactFilter implements ArtifactFilter {

    private Set<String> includePatterns = null;
    private Set<String> excludePatterns = null;

    @Override
    public boolean filter(Artifact artifact) {

        if (artifact == null) {
            return false;
        }

        if (includePatterns == null) {
            return true;
        }

        String artifactString = artifact.createStringRepresentation();

        boolean matches = false;

        for (String pattern : includePatterns) {
            if (matches(pattern, artifactString)) {
                matches = true;
                continue;
            }
        }

        if (!matches) {
            return false;
        } else {
            if (excludePatterns != null) {
                // the include pattern has matched
                // but we still need to the check the
                // exclude pattern
                for (String pattern : excludePatterns) {
                    if (matches(pattern, artifactString)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean matches(String pattern, String dependencyString) {
        String[] patternElements = pattern.split(":");
        String[] dependencyElements = dependencyString.split(":");

        for (int i = 0; i < patternElements.length; i++) {
            String pe = patternElements[i];
            if (!ASTERISK.equalsIgnoreCase(pe) && dependencyElements[i] != null) {
                // indication of a regular expression
                if (pe.startsWith("^")) {
                    if (!dependencyElements[i].matches(pe)) {
                        return false;
                    }
                } else {
                    if (!pe.equals(dependencyElements[i])) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public Iterable<String> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(Set<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public Iterable<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(Set<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public void addIncludePattern(String string) {
        if (this.includePatterns == null) {
            this.includePatterns = new HashSet<String>();
        }
        this.includePatterns.add(string);
    }

    public void addIncludePatterns(String... patterns) {
        for (String pattern : patterns) {
            addIncludePattern(pattern);
        }
    }

    public void addExcludePatterns(String... patterns) {
        for (String pattern : patterns) {
            addExcludePattern(pattern);
        }
    }

    public void addExcludePattern(String string) {
        if (this.excludePatterns == null) {
            this.excludePatterns = new HashSet<String>();
        }
        this.excludePatterns.add(string);
    }

}
