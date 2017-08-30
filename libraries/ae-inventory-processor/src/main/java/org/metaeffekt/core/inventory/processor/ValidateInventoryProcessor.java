/**
 * Copyright 2009-2017 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ValidateInventoryProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateInventoryProcessor.class);

    public ValidateInventoryProcessor() {
        super();
    }

    public ValidateInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();
        }

        for (Artifact artifact : inventory.getArtifacts()) {
            // check whether we have duplicate ids
            if (StringUtils.hasText(artifact.getId())) {
                Set<Artifact> alreadyReported = new HashSet<>();
                if (!alreadyReported.contains(artifact)) {
                    Artifact duplicateArtifact = inventory.findMatchingId(artifact);
                    if (duplicateArtifact != null) {
                        LOG.warn("Duplicate artifact detected: {} / {}",
                                artifact.getId() + "-" + artifact.createStringRepresentation(),
                                duplicateArtifact.getId() + "-" + duplicateArtifact.createStringRepresentation());
                        alreadyReported.add(duplicateArtifact);
                    }
                }
            }
        }

        for (Artifact artifact : inventory.getArtifacts()) {
            // check whether there is another current and provide hints
            if (artifact.getClassification() != null &&
                    artifact.getClassification().contains(Inventory.CLASSIFICATION_CURRENT)) {
                Set<Artifact> alreadyReported = new HashSet<>();
                if (!alreadyReported.contains(artifact)) {
                    if (StringUtils.hasText(artifact.getArtifactId())) {
                        Artifact currentArtifact = inventory.findCurrent(artifact);
                        if (currentArtifact != null) {
                            LOG.warn("Inconsistent classification (at least one and only one " +
                                            "with classification 'current' expected): {} / {}",
                                    artifact.getId() + "-" + artifact.createStringRepresentation(),
                                    currentArtifact.getId() + "-" + currentArtifact.createStringRepresentation());
                            alreadyReported.add(currentArtifact);
                        }
                    }
                }
            }
        }
    }
}
