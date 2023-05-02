/*
 * Copyright 2022 the original author or authors.
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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.probe.MavenJarIdProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

public class MavenJarMetadataExtractor extends AbstractInventoryProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MavenJarMetadataExtractor.class);

    // where the jar files are found. this is called the scanDirectory in other classes
    public static final String KEY_PROJECT_PATH = "project.path";

    // enable detected of artifacts shaded into others
    public static final String KEY_INCLUDE_EMBEDDED = "include.embedded";

    public MavenJarMetadataExtractor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        // set to keep track of the reported artifact ids
        final HashSet<String> reportedArtifactIds = new HashSet<>();

        // we iterate a cloned list since we rely on the find method not to create duplicates
        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {

            try {
                final File projectDir = new File(this.getProperties().getProperty(KEY_PROJECT_PATH, "./"));
                final MavenJarIdProbe probe = new MavenJarIdProbe(projectDir, artifact);
                probe.runCompletion();

                if (Boolean.parseBoolean(this.getProperties().getProperty(KEY_INCLUDE_EMBEDDED, "false"))) {
                    includeEmbedded(inventory, artifact, probe, reportedArtifactIds);
                }
            } catch (Exception e) {
                LOG.error("Error while running MavenJarIdProbe on artifact '" + artifact.toString() + "':"
                        + e.getMessage());
            }
        }
    }

    private void includeEmbedded(Inventory inventory, Artifact artifact, MavenJarIdProbe probe, HashSet<String> reportedArtifactIds) {
        if (probe.getDetectedArtifactsInFatJar() != null && !probe.getDetectedArtifactsInFatJar().isEmpty()) {
            final String assetId = "AID-" + artifact.getId() + "-" + artifact.getChecksum();

            // construct asset metadata for shaded jars
            final AssetMetaData e = new AssetMetaData();
            e.set(AssetMetaData.Attribute.ASSET_ID, assetId);
            inventory.getAssetMetaData().add(e);

            for (Artifact detectedArtifact : probe.getDetectedArtifactsInFatJar()) {

                // filter artifacts that cannot be fully identified
                final String detectedArtifactId = detectedArtifact.getId();
                if (detectedArtifactId != null && detectedArtifactId.contains("${")) {
                    if (!reportedArtifactIds.contains(detectedArtifactId)) {
                        LOG.warn("Skipping embedded artifact without fully qualified artifact id: {}", detectedArtifactId);
                        reportedArtifactIds.add(detectedArtifactId);
                    }
                    continue;
                }

                final Artifact foundArtifact = inventory.findArtifact(detectedArtifact);
                if (foundArtifact != null) {
                    foundArtifact.set(assetId, "x");
                } else {
                    detectedArtifact.set(assetId, "x");
                    inventory.getArtifacts().add(detectedArtifact);
                }
            }
        }
    }
}
