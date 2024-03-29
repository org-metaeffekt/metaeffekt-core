/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

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
                if (StringUtils.isNotBlank(artifact.getArtifactId())) {
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
