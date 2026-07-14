
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationProtocolTest {

    @Test
    public void testAggregationProtocolWrite() throws IOException {
        File tempFile = File.createTempFile("protocol", ".txt");
        tempFile.deleteOnExit();

        AggregationProtocol protocol = new AggregationProtocol(tempFile);

        ArtifactProtocolEntry entry1 = new ArtifactProtocolEntry();
        entry1.setArtifactRepresentation("org.example:test-artifact:jar:1.0.0");
        entry1.setIncludeStatus("INCLUDED");
        entry1.setIncludeReason("pattern match");
        entry1.setDownloadStatus("SUCCESS");
        entry1.setDownloadedLocation("https://example.com/test-oss/test-artifact-1.0.0-sources.jar");
        entry1.addAttemptedLocation("https://example.com/test-oss/test-artifact-1.0.0-sources.jar");
        entry1.addAttemptedLocation("https://example.com/other-oss/test-artifact-1.0.0-sources.jar");
        
        protocol.addEntry(entry1);

        ArtifactProtocolEntry entry2 = new ArtifactProtocolEntry();
        entry2.setArtifactRepresentation("org.example:skipped-artifact:jar:1.0.0");
        entry2.setIncludeStatus("EXCLUDED");
        entry2.setIncludeReason("no license");
        entry2.setDownloadStatus("SKIPPED");
        
        protocol.addEntry(entry2);

        protocol.writeToFile();

        List<String> lines = Files.readAllLines(tempFile.toPath());
        
        // Assert successful download formatting
        assertTrue(lines.stream().anyMatch(line -> line.contains("Artifact:  org.example:test-artifact:jar:1.0.0")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Inclusion: INCLUDED (Reason: pattern match)")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Download:  SUCCESS")));
        // Assert the redundant location line is absent
        assertFalse(lines.stream().anyMatch(line -> line.startsWith("Location:")));
        // Assert attempted locations formatting
        assertTrue(lines.stream().anyMatch(line -> line.contains("  * https://example.com/test-oss/test-artifact-1.0.0-sources.jar [SUCCESS]")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("  - https://example.com/other-oss/test-artifact-1.0.0-sources.jar")));
        
        // Assert skipped artifact formatting
        assertTrue(lines.stream().anyMatch(line -> line.contains("Artifact:  org.example:skipped-artifact:jar:1.0.0")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Inclusion: EXCLUDED (Reason: no license)")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Download:  SKIPPED")));

        // Assert summary formatting
        assertTrue(lines.stream().anyMatch(line -> line.contains("SUMMARY")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Total Artifacts: 2 | Included: 1 | Excluded: 1 | Downloaded: 1 | Failed: 0")));
    }
}
