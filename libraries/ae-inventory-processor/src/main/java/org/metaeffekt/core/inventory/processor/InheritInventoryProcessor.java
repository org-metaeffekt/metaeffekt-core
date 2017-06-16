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
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class InheritInventoryProcessor extends AbstractInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateVersionRecommendationProcessor.class);

    public static final String INPUT_INVENTORY = "input.inventory.path";

    public InheritInventoryProcessor() {
        super();
    }

    public InheritInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        final Inventory inputInventory = loadInputInventory();
        inheritArifacts(inputInventory, inventory);
        inheritLicenseMetaData(inputInventory, inventory);
    }

    protected Inventory loadInputInventory() {
        final String inventoryFileName = getProperties().getProperty(INPUT_INVENTORY);

        if (inventoryFileName == null) {
            throw new IllegalArgumentException("Please specify the '" + INPUT_INVENTORY + "' property.");
        }

        File inventoryFile = new File(inventoryFileName);

        Inventory inputInventory;
        try {
            inputInventory = new GlobalInventoryReader().readInventory(inventoryFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load inventory.", e);
        }
        return inputInventory;
    }

    private void inheritLicenseMetaData(Inventory inputInventory, Inventory inventory) {
        // Iterate through all license meta data. Generate qualifier based on component name, version and license.
        // Test whether the qualifier is present in current. If yes skip; otherwise add.
        final Map<String, LicenseMetaData> currentLicenseMetaDataMap = new HashMap<>();
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            currentLicenseMetaDataMap.put(qualifier, licenseMetaData);
        }

        for (LicenseMetaData licenseMetaData : inputInventory.getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            if (currentLicenseMetaDataMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                LicenseMetaData currentLicenseMetaData = currentLicenseMetaDataMap.get(qualifier);

                if (currentLicenseMetaData.createCompareStringRepresentation().equals(licenseMetaData.createCompareStringRepresentation())) {
                    LOG.info("License meta data {} overwritten. Relevant content nevertheless matches. " +
                            "Consider removing the overwrite.", qualifier);
                } else {
                    LOG.info("License meta data {} overwritten.", qualifier);
                }
            } else {
                // add the license meta data
                inventory.getLicenseMetaData().add(licenseMetaData);
            }
        }
    }

    private void inheritArifacts(Inventory inputInventory, Inventory inventory) {
        // Iterate through all artifacts in the input repository. If the artifact is present in the current repository
        // then skip (but log some information); otherwise add the artifact to current.
        final Map<String, Artifact> currentArtifactMap = new HashMap<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();
            String qualifier = deriveQualifier(artifact);
            currentArtifactMap.put(qualifier, artifact);
        }
        for (Artifact artifact : inputInventory.getArtifacts()) {
            artifact.deriveArtifactId();
            String qualifier = deriveQualifier(artifact);
            // NOTE: the qualifier will be extended by the checksum once we have it.
            if (currentArtifactMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                Artifact currentArtifact = currentArtifactMap.get(qualifier);
                if (artifact.createCompareStringRepresentation().equals(currentArtifact.createCompareStringRepresentation())) {
                    LOG.info("Artifact {} overwritten. Relevant content nevertheless matches. " +
                            "Consider removing the overwrite.", qualifier);
                } else {
                    LOG.info("Artifact {} overwritten. %n{} / %n{}", qualifier, artifact.createCompareStringRepresentation(), currentArtifact.createCompareStringRepresentation());
                }
            } else {
                // add the artifact
                inventory.getArtifacts().add(artifact);
            }
        }
    }

    private String deriveQualifier(Artifact artifact) {
        StringBuilder artifactQualifierBuilder = new StringBuilder(artifact.getId());
        return artifactQualifierBuilder.toString();
    }

}
