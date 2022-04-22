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
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.probe.MavenJarIdProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenJarMetadataExtractor extends AbstractInventoryProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MavenJarMetadataExtractor.class);

    @Override
    public void process(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            try {
                MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
                probe.runCompletion();
            } catch (Exception e) {
                LOG.error("Error while running MavenJarIdProbe on artifact " + artifact.toString() + ":"
                        + e.getMessage());
            }
        }
    }
}
