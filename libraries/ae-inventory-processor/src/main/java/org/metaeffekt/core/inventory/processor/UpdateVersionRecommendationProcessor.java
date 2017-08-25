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

public class UpdateVersionRecommendationProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateVersionRecommendationProcessor.class);

    public UpdateVersionRecommendationProcessor() {
        super();
    }

    public UpdateVersionRecommendationProcessor(Properties properties) {
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

        for (Artifact artifact : inventory.getArtifacts()) {
            String comment = artifact.getComment();
            if (comment == null) {
                comment = "";
            }
            // remove existing recommendation (may be obsolete)
            int index = comment.indexOf("[recommended version:");
            if (index != -1) {
                int endIndex = comment.indexOf("]", index);
                if (endIndex != -1) {
                    comment = (comment.substring(0, index) + comment.substring(endIndex + 1)).trim();
                } else {
                    comment = comment.substring(0, index);
                    LOG.warn("Recommended version for [{}] not terminated with ']' ", artifact.createStringRepresentation());
                }
            }

            // update the recommended version
            if (artifact.getClassification() != null &&
                    !artifact.getClassification().contains(Inventory.CLASSIFICATION_CURRENT)) {
                if (StringUtils.hasText(artifact.getArtifactId())) {
                    // we have classified the artifact as not 'current' therefore
                    // we need to update the comment to include the recommended
                    // current version
                    Artifact currentArtifact = inventory.findCurrent(artifact);
                    if (currentArtifact != null) {
                        comment = comment + " [recommended version: " + currentArtifact.getVersion() + "]";
                    }
                }
            }
            artifact.setComment(comment.trim());
        }
    }
}
