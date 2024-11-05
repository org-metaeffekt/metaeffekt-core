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
package org.metaeffekt.core.inventory.processor.report.adapter;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class InventoryReportAdapter {

    private Inventory inventory;

    /**
     * List to cover artifacts without license.
     */
    private List<Artifact> artifactsWithoutLicense = new ArrayList<>();

    public InventoryReportAdapter(Inventory inventory) {
        this.inventory = inventory;

        evaluateArtifactsWithoutLicense();
    }

    private void evaluateArtifactsWithoutLicense() {
        // collect artifacts that are components or component parts and have no license
        for (Artifact artifact : inventory.getArtifacts()) {
            if (StringUtils.isEmpty(artifact.getLicense())) {
                // only components or component parts are integrated
                if (artifact.isComponentOrComponentPart()) {
                    artifactsWithoutLicense.add(artifact);
                }
            }
        }
    }

}
