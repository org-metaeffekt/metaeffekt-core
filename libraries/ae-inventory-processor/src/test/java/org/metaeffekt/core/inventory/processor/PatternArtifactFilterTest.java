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
package org.metaeffekt.core.inventory.processor;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;


public class PatternArtifactFilterTest {

    @Test
    public void testIncludeAndExclude_Null() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        patternArtifactFilter.setIncludePatterns(null);
        patternArtifactFilter.setExcludePatterns(null);
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertTrue(patternArtifactFilter.filter(defaultArtifact));
    }

    @Test
    public void testIncludeAndExclude_NotInitialized() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertTrue(patternArtifactFilter.filter(defaultArtifact));
    }

    @Test
    public void testIncludeAndExclude_WithInclude() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        patternArtifactFilter.addIncludePattern(ASTERISK);
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertTrue(patternArtifactFilter.filter(defaultArtifact));
    }

    @Test
    public void testIncludeAndExclude_WithIncludeAndExlude() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        patternArtifactFilter.addIncludePatterns(ASTERISK);
        patternArtifactFilter.addExcludePatterns("*:artifact:*");
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertFalse(patternArtifactFilter.filter(defaultArtifact));
    }

    @Test
    public void testIncludeAndExclude_WithIncludeAndExlude_ExcludeNotMatching() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        patternArtifactFilter.addIncludePattern(ASTERISK);
        patternArtifactFilter.addExcludePattern("*:nomatch:*");
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertTrue(patternArtifactFilter.filter(defaultArtifact));
    }

    @Test
    public void testIncludeAndExclude_WithIncludeAndExlude_RegExp() {
        PatternArtifactFilter patternArtifactFilter = new PatternArtifactFilter();
        patternArtifactFilter.addIncludePattern("^org\\.metaeffekt\\..*:*");
        Artifact defaultArtifact = createExampleArtifact();
        Assert.assertTrue(patternArtifactFilter.filter(defaultArtifact));
    }

    private Artifact createExampleArtifact() {
        Artifact artifact = new Artifact();
        artifact.setId("artifact-0.0.1.jar");
        artifact.setArtifactId("artifact");
        artifact.setGroupId("org.metaeffekt.core");
        artifact.setVersion("0.0.1");
        return artifact;
    }

}
