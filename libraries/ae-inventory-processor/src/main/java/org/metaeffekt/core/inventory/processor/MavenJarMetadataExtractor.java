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

import java.io.File;
import java.util.Properties;

public class MavenJarMetadataExtractor extends AbstractInventoryProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MavenJarMetadataExtractor.class);

    // where the jar files are found. this is called the scanDirectory in other classes
    public static final String PROJECT_PATH = "project.path";

    public MavenJarMetadataExtractor() {
        super();
    }

    public MavenJarMetadataExtractor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            try {
                File projectDir = new File(this.getProperties().getProperty(PROJECT_PATH, "./"));
                MavenJarIdProbe probe = new MavenJarIdProbe(projectDir, artifact);
                probe.runCompletion();
            } catch (Exception e) {
                LOG.error("Error while running MavenJarIdProbe on artifact '" + artifact.toString() + "':"
                        + e.getMessage());
            }
        }
    }
}
