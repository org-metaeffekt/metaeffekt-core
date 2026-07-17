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
package org.metaeffekt.core.inventory.resolver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArtifactPatternMatcherTest {

    @Test
    public void testEmptyPatternsReturnsMatchAll() {
        ArtifactPatternMatcher matcher = new ArtifactPatternMatcher();
        
        // When no patterns are registered, it should match everything
        ArtifactPattern result = matcher.findMatchingArtifactGroup("some-artifact", "some-component", "1.0", "MIT");
        
        assertNotNull(result);
        assertEquals("^.*", result.getArtifactPattern());
        assertEquals("^.*", result.getComponentPattern());
        assertEquals("^.*", result.getVersionPattern());
        assertEquals("^.*", result.getEffectiveLicensePattern());
    }

    @Test
    public void testRegisteredPatternMatches() {
        ArtifactPatternMatcher matcher = new ArtifactPatternMatcher();
        matcher.register(new ArtifactPattern("^some-.*", "^.*", "^.*", "^.*"));
        
        ArtifactPattern result = matcher.findMatchingArtifactGroup("some-artifact", "some-component", "1.0", "MIT");
        assertNotNull(result);
        assertEquals("^some-.*", result.getArtifactPattern());
        
        ArtifactPattern result2 = matcher.findMatchingArtifactGroup("other-artifact", "some-component", "1.0", "MIT");
        assertNull(result2);
    }
}
