/*
 * Copyright 2009-2024 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.Properties;


public class RemoveFilesWithoutChecksumProcessor extends AbstractInputInventoryBasedProcessor {

    public RemoveFilesWithoutChecksumProcessor() {
        super();
    }

    public RemoveFilesWithoutChecksumProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        removeFilesWithoutChecksum(inventory);
    }

    private void removeFilesWithoutChecksum(Inventory inventory) {
        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
            if (Constants.ARTIFACT_TYPE_FILE.equals(artifact.get(Constants.KEY_TYPE))) {
                if (!StringUtils.isNotBlank(artifact.getChecksum())) {
                    inventory.getArtifacts().remove(artifact);
                }
            }
        }
    }


}
