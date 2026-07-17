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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AggregationProtocol {

    private final List<ArtifactProtocolEntry> entries = new ArrayList<>();
    private final File protocolFile;

    public void addEntry(ArtifactProtocolEntry entry) {
        entries.add(entry);
    }

    public void writeToFile() throws IOException {
        if (protocolFile == null) {
            return;
        }

        File parent = protocolFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(protocolFile))) {
            writer.println("================================================================================");
            writer.println("SOURCE AGGREGATION PROTOCOL");
            writer.println("================================================================================");
            writer.println();

            int included = 0;
            int excluded = 0;
            int downloadSuccess = 0;
            int downloadFailed = 0;

            for (ArtifactProtocolEntry entry : entries) {
                writer.println("Artifact:  " + entry.getArtifactRepresentation());
                writer.println("Inclusion: " + entry.getIncludeStatus() + " (Reason: " + entry.getIncludeReason() + ")");
                writer.println("Download:  " + entry.getDownloadStatus());
                
                if ("INCLUDED".equals(entry.getIncludeStatus())) {
                    included++;
                } else {
                    excluded++;
                }

                if ("SUCCESS".equals(entry.getDownloadStatus())) {
                    downloadSuccess++;
                } else if ("FAILED".equals(entry.getDownloadStatus())) {
                    downloadFailed++;
                }

                if (!entry.getAttemptedLocations().isEmpty()) {
                    writer.println("Attempted:");
                    for (String location : entry.getAttemptedLocations()) {
                        if ("SUCCESS".equals(entry.getDownloadStatus()) && location.equals(entry.getDownloadedLocation())) {
                            writer.println("  * " + location + " [SUCCESS]");
                        } else {
                            writer.println("  - " + location);
                        }
                    }
                }

                writer.println();
                writer.println("--------------------------------------------------------------------------------");
                writer.println();
            }

            writer.println("================================================================================");
            writer.println("SUMMARY");
            writer.println("Total Artifacts: " + entries.size() + " | Included: " + included + " | Excluded: " + excluded + 
                           " | Downloaded: " + downloadSuccess + " | Failed: " + downloadFailed);
            writer.println("================================================================================");
        }
    }
}
