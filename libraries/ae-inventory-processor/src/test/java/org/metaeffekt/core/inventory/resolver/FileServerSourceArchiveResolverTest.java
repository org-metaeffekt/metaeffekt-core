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
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class FileServerSourceArchiveResolverTest {

    static class TestableFileServerSourceArchiveResolver extends FileServerSourceArchiveResolver {
        String lastAttemptedResolvedUrl;

        @Override
        protected boolean downloadFile(String url, File targetDir, SourceArchiveResolverResult result) {
            lastAttemptedResolvedUrl = url;
            return true;
        }
    }

    @Test
    public void testResolveUrlWithResolvedPlaceholders() {
        TestableFileServerSourceArchiveResolver resolver = new TestableFileServerSourceArchiveResolver();

        Artifact artifact = new Artifact();
        artifact.setId("test-artifact-1.0.0.jar");
        artifact.setGroupId("org.example");
        artifact.setArtifactId("test-artifact");
        artifact.setVersion("1.0.0");

        // Ensure that artifact extension is derived or set
        artifact.deriveArtifactId();

        Properties properties = new Properties();
        properties.put("host", "example.com");

        SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        String urlPattern = "https://$[host]/$[artifact.groupId]/$[artifact.artifactId]/$[artifact.version]/$[artifact.artifactId]-$[artifact.version].$[artifact.extension]";

        boolean success = resolver.resolveUrl(urlPattern, artifact, new File("target"), result, properties);

        assertTrue(success, "Should succeed because downloadFile is stubbed to true");
        assertEquals("https://example.com/org.example/test-artifact/1.0.0/test-artifact-1.0.0.jar", resolver.lastAttemptedResolvedUrl);
    }

    @Test
    public void testResolveUrlWithUnresolvedPlaceholdersSkipsDownload() {
        TestableFileServerSourceArchiveResolver resolver = new TestableFileServerSourceArchiveResolver();

        Artifact artifact = new Artifact();
        artifact.setGroupId("org.example");

        Properties properties = new Properties();

        SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        // Missing property 'host'
        String urlPattern = "https://$[host]/$[artifact.groupId]/$[artifact.artifactId]/$[artifact.version]/$[artifact.artifactId]-$[artifact.version].$[artifact.extension]";

        boolean success = resolver.resolveUrl(urlPattern, artifact, new File("target"), result, properties);

        assertFalse(success, "Should fail because placeholders remain unresolved");
        assertNull(resolver.lastAttemptedResolvedUrl, "Should not attempt to download");
        assertTrue(result.getAttemptedResourceLocations().get(0).contains("$[host]"), "Attempted locations should record the failed URL");
    }
}
