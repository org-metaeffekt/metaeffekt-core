/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
@Setter
public class InventoryMergeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryMergeUtils.class);

    private boolean addDefaultArtifactExcludedAttributes = true;

    private Set<String> artifactExcludedAttributes = new HashSet<>();

    private boolean addDefaultArtifactMergeAttributes = true;

    private Set<String> artifactMergeAttributes = new HashSet<>();

    public void merge(List<File> sourceInventories, Inventory targetInventory) throws IOException {
        List<Inventory> inventories = new ArrayList<>();
        for (File sourceInventoryFile : sourceInventories) {
            final Inventory sourceInventory = new InventoryReader().readInventory(sourceInventoryFile);
            inventories.add(sourceInventory);
        }
        mergeInventories(inventories, targetInventory);
    }

    public void mergeInventories(List<Inventory> sourceInventories, Inventory target) {
        // NOTE: we merge
        // - artifacts
        // - assets
        // - license data (resolving duplicates; merge on attribute level; input inventory should be consistent/up-to-date)
        // - license data (applying consecutive inherits)

        // TODO: revise the following once assets have been fully established
        // - we do not merge component patterns; the target component patterns are preserved (future: tbc; currently not required)
        // - we do not merge vulnerabilities; for inventories with vulnerabilities dedicated procedures exist.

        for (Inventory source : sourceInventories) {
            mergeArtifacts(source, target);
            mergeAssetMetaData(source, target);
            mergeLicenseData(source, target);
            mergeLicenseMetaData(source, target);
        }
    }

    private void mergeLicenseMetaData(Inventory sourceInventory, Inventory targetInventory) {
        if (sourceInventory.getLicenseMetaData() != null && !sourceInventory.getLicenseMetaData().isEmpty()) {
            targetInventory.inheritLicenseMetaData(sourceInventory, false);
        }
    }

    private void mergeArtifacts(Inventory sourceInv, Inventory targetInv) {
        // complete artifacts in targetInv with checksums (with matching project location)
        // NOTE: this assumes that the source inventory container extensions to the target inventory
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {

            // existing checksums must not be overwritten
            if (StringUtils.isNotBlank(artifact.getChecksum())) continue;

            final List<org.metaeffekt.core.inventory.processor.model.Artifact> candidates = sourceInv.findAllWithId(artifact.getId());

            // matches match on project location
            final List<org.metaeffekt.core.inventory.processor.model.Artifact> matches = new ArrayList<>();
            for (final org.metaeffekt.core.inventory.processor.model.Artifact candidate : candidates) {
                if (matchesRootPaths(artifact, candidate)) {
                    matches.add(candidate);
                }
            }
            for (final org.metaeffekt.core.inventory.processor.model.Artifact match : matches) {
                artifact.setChecksum(match.getChecksum());
            }
        }

        // add NOT COVERED artifacts from sourceInv in targetInv using id and checksum
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : sourceInv.getArtifacts()) {
            final org.metaeffekt.core.inventory.processor.model.Artifact candidate = targetInv.findArtifactByIdAndChecksum(artifact.getId(), artifact.getChecksum());
            if (candidate == null) {
                targetInv.getArtifacts().add(artifact);
            }
        }

        // remove duplicates (in the sense of exclude and merge attributes
        final Set<org.metaeffekt.core.inventory.processor.model.Artifact> toBeDeleted = new HashSet<>();
        final Map<String, org.metaeffekt.core.inventory.processor.model.Artifact> representationArtifactMap = new HashMap<>();
        final Set<String> attributes = new HashSet<>();
        final Set<String> excludedAttributes = new HashSet<>(artifactExcludedAttributes);

        if (addDefaultArtifactExcludedAttributes) {
            excludedAttributes.add("Verified");
            excludedAttributes.add("Archive Path");
            excludedAttributes.add("Latest Version");
            excludedAttributes.add("Security Relevance");
            excludedAttributes.add("Security Category");
            excludedAttributes.add("WILDCARD-MATCH");
        }

        // currently not configurable as these require specialized implementations
        final Set<String> mergeAttributes = new HashSet<>(artifactMergeAttributes);

        if (addDefaultArtifactMergeAttributes) {
            mergeAttributes.add(Artifact.Attribute.ROOT_PATHS.getKey());
            mergeAttributes.add("Source Project");
        }

        // compile attributes list covering all artifacts / clear excluded attributes
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {

            // the excluded attributes are eliminated; merge attributes persist (as these are required)
            for (final String attribute : excludedAttributes) {
                artifact.set(attribute, null);
            }

            // add the (remaining) attributes to the overall list
            attributes.addAll(artifact.getAttributes());
        }

        // the merge attributes do not contribute to the artifacts representation
        attributes.removeAll(mergeAttributes);

        // produce an ordered list of the attributes
        final List<String> attributesOrdered = new ArrayList<>(attributes);
        attributesOrdered.sort(String::compareToIgnoreCase);

        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {
            final StringBuilder stringRepresentation = new StringBuilder();
            for (String attribute : attributes) {
                if (stringRepresentation.length() == 0) {
                    stringRepresentation.append(";");
                }
                stringRepresentation.append(attribute).append("=").append(artifact.get(attribute));
            }

            final String rep = stringRepresentation.toString();
            if (representationArtifactMap.containsKey(rep)) {
                toBeDeleted.add(artifact);

                final Artifact retainedArtifact = representationArtifactMap.get(rep);
                for (final String key : mergeAttributes) {
                    retainedArtifact.append(key, artifact.get(key), ", ");
                }
            } else {
                representationArtifactMap.put(rep, artifact);
            }
        }

        targetInv.getArtifacts().removeAll(toBeDeleted);
    }

    private static void mergeAssetMetaData(Inventory sourceInv, Inventory targetInv) {
        for (AssetMetaData assetMetaData : sourceInv.getAssetMetaData()) {
            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

            // Check if the asset already exists in the target inventory
            AssetMetaData existingAsset = targetInv.findAssetMetaData(assetId, false);

            if (existingAsset == null) {
                // If it doesn't exist, add the asset metadata to the target
                targetInv.getAssetMetaData().add(assetMetaData);
            }
        }
    }

    private void mergeLicenseData(Inventory sourceInv, Inventory targetInv) {
        // build map
        final Map<String, LicenseData> canonicalNameLicenseDataMap = new HashMap<>();
        for (LicenseData licenseData : targetInv.getLicenseData()) {
            canonicalNameLicenseDataMap.put(licenseData.get(LicenseData.Attribute.CANONICAL_NAME), licenseData);
        }

        // compare, merge or insert
        for (LicenseData sourceLicenseData : sourceInv.getLicenseData()) {
            final String canonicalName = sourceLicenseData.get(LicenseData.Attribute.CANONICAL_NAME);
            final LicenseData targetLicenseData = canonicalNameLicenseDataMap.get(canonicalName);
            if (targetLicenseData != null) {
                targetLicenseData.merge(sourceLicenseData);
            } else {
                // does not exist in targetInv yet; add
                targetInv.getLicenseData().add(sourceLicenseData);

                // manage map
                canonicalNameLicenseDataMap.put(canonicalName, sourceLicenseData);
            }
        }
    }

    private boolean matchesRootPaths(Artifact artifact, Artifact candidate) {
        for (String location : artifact.getRootPaths()) {
            for (String candidateLocation : candidate.getRootPaths()) {
                if (candidateLocation.contains(location)) {
                    return true;
                }
            }
        }
        return false;
    }
}
