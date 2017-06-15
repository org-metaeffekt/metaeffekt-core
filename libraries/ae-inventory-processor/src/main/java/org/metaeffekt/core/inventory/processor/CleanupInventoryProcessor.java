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

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.Properties;


public class CleanupInventoryProcessor extends AbstractInventoryProcessor {

    public CleanupInventoryProcessor() {
    }

    public CleanupInventoryProcessor(Properties properties) {
    }

    @Override
    public void process(Inventory inventory) {
        // map the licenses and components according to the mappings 
        // configured in the inventory
        inventory.mapComponentNames();
        inventory.mapLicenseNames();

        // merge duplicates
        inventory.mergeDuplicates();

//        // remove unspecific covered by concrete versions
//        inventory.removeUnspecificCoveredByConcrete();

        // infer duplicates by name
//        inventory.inferDuplicatesByName();

        // merge remaining duplicates
//        inventory.mergeDuplicates();

        // remove not supported artifacts
//        inventory.removeArtifactWithMatchingId(".*-sources\\.jar");
//        inventory.removeArtifactWithMatchingId(".*-javadoc\\.jar");
//        inventory.removeArtifactWithMatchingId(".*-tests\\.jar");

        // remove all artifacts having no relevance
        inventory.cleanup();
    }

}
