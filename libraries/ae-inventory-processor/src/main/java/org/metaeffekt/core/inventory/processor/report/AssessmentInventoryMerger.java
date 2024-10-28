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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AssessmentInventoryMerger {

    private final static Logger LOG = LoggerFactory.getLogger(AssessmentInventoryMerger.class);

    private List<File> inputInventoryFiles;
    private List<Inventory> inputInventories;

    public AssessmentInventoryMerger(List<File> inputInventoryFiles, List<Inventory> inputInventories) {
        this.inputInventoryFiles = inputInventoryFiles;
        this.inputInventories = inputInventories;
    }

    public AssessmentInventoryMerger() {
        this.inputInventoryFiles = new ArrayList<>();
        this.inputInventories = new ArrayList<>();
    }

    public Inventory mergeInventories() throws IOException {
        final Inventory outputInventory = new Inventory();

        final Map<Inventory, File> collectedInventories = new LinkedHashMap<>();
        inputInventories.forEach(i -> collectedInventories.put(i, null));

        final List<File> inventoryFiles = collectInventoryFiles();

        LOG.info("Processing [{}] inventories", collectedInventories.size() + inventoryFiles.size());

        {
            final InventoryReader reader = new InventoryReader();
            for (File inventoryFile : inventoryFiles) {
                collectedInventories.put(reader.readInventory(inventoryFile), inventoryFile);
            }
        }

        final Map<String, List<Inventory>> assessmentContextInventoryMap = new HashMap<>();
        final Map<String, Inventory> assessmentInventoryMap = new HashMap<>();

        for (Map.Entry<Inventory, File> inputInventoryWithFile : collectedInventories.entrySet()) {
            final Inventory inputInventory = inputInventoryWithFile.getKey();
            final File inputInventoryFile = inputInventoryWithFile.getValue();

            if (inputInventory.getAssetMetaData().size() != 1) {
                throw new IllegalStateException("Inventory must contain exactly one asset meta data entry" + (inputInventoryFile != null ? " in file [" + inputInventoryFile.getAbsolutePath() + "]" : ""));
            }

            final AssetMetaData assetMetaData = inputInventory.getAssetMetaData().get(0);

            // in this case the assessment context is the asset (assessment by asset case)
            final String assetName = assetMetaData.get(AssetMetaData.Attribute.NAME);
            if (StringUtils.isEmpty(assetName)) {
                throw new IllegalStateException("Asset name must not be empty on asset meta data" + (inputInventoryFile != null ? " in file [" + inputInventoryFile.getAbsolutePath() + "]" : ""));
            }
            final String assessmentContext = formatNormalizedAssessmentContextName(assetName);
            LOG.info("Processing inventory with asset [{}] and assessment context [{}]", assetName, assessmentContext);

            final List<Inventory> commonInventories = assessmentContextInventoryMap.computeIfAbsent(assessmentContext, a -> new ArrayList<>());
            final int inventoryDisplayIndex = commonInventories.size() + 1;
            final String localUniqueAssessmentId = String.format("%s-%03d", assessmentContext, inventoryDisplayIndex);

            assetMetaData.set(AssetMetaData.Attribute.ASSESSMENT, localUniqueAssessmentId);
            assetMetaData.set(AssetMetaData.Attribute.NAME, assetName);

            commonInventories.add(inputInventory);
            assessmentInventoryMap.put(localUniqueAssessmentId, inputInventory);

            // contribute asset metadata to output inventory
            outputInventory.getAssetMetaData().add(assetMetaData);
        }

        // iterate over each asset in the output inventory and add its respective vulnerabilities
        for (AssetMetaData assetMetaData : outputInventory.getAssetMetaData()) {
            final String localUniqueAssessmentId = assetMetaData.get(AssetMetaData.Attribute.ASSESSMENT);
            if (StringUtils.isNotEmpty(localUniqueAssessmentId)) {
                final Inventory singleInventory = assessmentInventoryMap.get(localUniqueAssessmentId);
                if (singleInventory != null) {
                    outputInventory.getVulnerabilityMetaData(localUniqueAssessmentId).addAll(singleInventory.getVulnerabilityMetaData());
                }
            }
        }

        // add all security advisories to the output inventory, only once
        final Set<String> knownAdvisoryIds = new LinkedHashSet<>();
        for (Inventory inputInventory : collectedInventories.keySet()) {
            for (AdvisoryMetaData securityAdvisory : inputInventory.getAdvisoryMetaData()) {
                final String advisoryId = securityAdvisory.get(AdvisoryMetaData.Attribute.NAME);
                if (knownAdvisoryIds.add(advisoryId)) {
                    outputInventory.getAdvisoryMetaData().add(securityAdvisory);
                }
            }
        }

        return outputInventory;
    }

    protected String formatNormalizedAssessmentContextName(String assetName) {
        String assessmentContext = assetName.toUpperCase().replace(" ", "_");
        final int maxAllowedChars = 25;
        assessmentContext = assessmentContext.substring(0, Math.min(assessmentContext.length(), maxAllowedChars - InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX.length()));

        // remove trailing "_-"
        while (assessmentContext.endsWith("_") || assessmentContext.endsWith("-")) {
            assessmentContext = assessmentContext.substring(0, assessmentContext.length() - 1);
        }
        return assessmentContext;
    }

    private List<File> collectInventoryFiles() {
        final List<File> inventoryFiles = new ArrayList<>();

        for (File inputInventory : inputInventoryFiles) {
            if (inputInventory.isDirectory()) {
                final String[] files = FileUtils.scanDirectoryForFiles(inputInventory, "*.xls");
                for (String file : files) {
                    inventoryFiles.add(new File(inputInventory, file));
                }
            } else {
                inventoryFiles.add(inputInventory);
            }
        }

        return inventoryFiles;
    }

    public void addInputInventoryFile(File inputInventory) {
        this.inputInventoryFiles.add(inputInventory);
    }

    public void setInputInventoryFiles(List<File> inputInventoryFiles) {
        this.inputInventoryFiles = inputInventoryFiles;
    }

    public List<File> getInputInventoryFiles() {
        return inputInventoryFiles;
    }

    public void addInputInventory(Inventory inputInventory) {
        this.inputInventories.add(inputInventory);
    }

    public void setInputInventories(List<Inventory> inputInventories) {
        this.inputInventories = inputInventories;
    }

    public List<Inventory> getInputInventories() {
        return inputInventories;
    }
}
