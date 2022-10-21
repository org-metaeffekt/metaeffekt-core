/*
 * Copyright 2009-2021 the original author or authors.
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

import org.metaeffekt.core.inventory.processor.model.*;
import org.springframework.util.StringUtils;

import java.util.*;

public class AssetData {

    private Map<String, Set<String>> assetIdAssociatedLicenseMap = new HashMap<>();
    private Map<String, Set<String>> representedLicenseLicensesMap = new HashMap<>();

    private Map<String, Set<String>> representedLicenseAssetIdMap = new HashMap<>();
    private Map<String, Set<String>> individualLicenseAssetIdMap = new HashMap<>();

    private Set<String> associatedLicenses = new HashSet<>();
    private Set<String> representedAssociatedLicenses = new HashSet<>();

    private Map<String, AssetLicenseData> assetIdAssetLicenseDataMap = new HashMap<>();

    private Inventory inventory;

    public static AssetData fromArtifacts(Inventory filteredInventory) {
        AssetData assetData = new AssetData();
        assetData.insertData(filteredInventory);
        assetData.inventory = filteredInventory;
        return assetData;
    }

    private void insertData(Inventory filteredInventory) {
        for (AssetMetaData assetMetaData : filteredInventory.getAssetMetaData()) {

            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

            if (!StringUtils.hasText(assetId)) continue;

            // set to collect all licenses associated with asset
            final Set<String> assetAssociatedLicenses = new HashSet<>();

            // derive licenses from artifacts
            for (Artifact artifact : filteredInventory.getArtifacts()) {
                // skip all artifacts that do not belong to an asset
                if (!StringUtils.hasText(artifact.get(assetId))) continue;

                final List<String> associatedLicenses = artifact.getLicenses();

                for (final String associatedLicense : associatedLicenses) {
                    if (!StringUtils.hasText(associatedLicense)) continue;

                    this.associatedLicenses.add(associatedLicense);

                    final LicenseData licenseData = filteredInventory.findMatchingLicenseData(associatedLicense);

                    String representedAs = licenseData != null ? licenseData.get(LicenseData.Attribute.REPRESENTED_AS) : null;

                    // the license represents itself
                    if (representedAs == null) {
                        representedAs = associatedLicense;
                    }

                    representedAssociatedLicenses.add(representedAs);

                    representedLicenseLicensesMap.computeIfAbsent(representedAs, c -> new HashSet<>()).add(associatedLicense);
                    representedLicenseAssetIdMap.computeIfAbsent(representedAs, c -> new HashSet<>()).add(assetId);
                    individualLicenseAssetIdMap.computeIfAbsent(associatedLicense, c -> new HashSet<>()).add(assetId);

                    assetIdAssociatedLicenseMap.computeIfAbsent(assetId, c -> new HashSet<>()).add(associatedLicense);

                    assetAssociatedLicenses.add(associatedLicense);
                }
                assetIdAssetLicenseDataMap.put(assetId, createAssetLicenseData(assetMetaData, assetAssociatedLicenses));
            }
        }
    }

    private AssetLicenseData createAssetLicenseData(AssetMetaData assetMetaData, Set<String> assetAssociatedLicenses) {
        final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
        final String assetName = assetMetaData.getAlternatives("Name", "Machine Tag", "Repository", "Repo");
        final String assetVersion = assetMetaData.getAlternatives("Version", "Tag", "Snapshot Timestamp", "Timestamp");
        final String assetType = assetMetaData.get("Type", "Appliance");

        return new AssetLicenseData(assetId, assetName, assetVersion, assetType, sortedList(assetAssociatedLicenses));
    }

    private List<String> sortedList(Set<String> assetAssociatedLicenses) {
        final List list = new ArrayList(assetAssociatedLicenses);
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    public Map<String, Set<String>> getAssetIdAssociatedLicenseMap() {
        return assetIdAssociatedLicenseMap;
    }

    public Map<String, Set<String>> getRepresentedLicenseLicensesMap() {
        return representedLicenseLicensesMap;
    }

    public List<String> getAssociatedLicenses() {
        return sortedList(this.associatedLicenses);
    }

    public List<String> getRepresentedAssociatedLicenses() {
        return sortedList(this.representedAssociatedLicenses);
    }

    public List<String> getLicensesForRepresentedLicense(String representedAssociatedLicense) {
        return sortedList(representedLicenseLicensesMap.get(representedAssociatedLicense));
    }

    public int countAssetsWithRepresentedAssociatedLicense(String representedAssociatedLicense) {
        final Set<String> assetIds = representedLicenseAssetIdMap.get(representedAssociatedLicense);

        if (assetIds != null) {
            return assetIds.size();
        }

        return 0;
    }

    public boolean isLicenseSubstructureRequired() {
        for (Set<String> representedLicenses : representedLicenseLicensesMap.values()) {
            if (representedLicenses != null && representedLicenses.size() > 1) {
                return true;
            }
        }
        return false;
    }

    public boolean isLicenseSubstructureRequired(String representedLicense) {
        final Set<String> representedLicenses = representedLicenseLicensesMap.get(representedLicense);
        if (representedLicenses != null && representedLicenses.size() > 1) {
            return true;
        }
        return false;
    }

    public List<AssetLicenseData> evaluateAssets(String individualLicense) {
        List<AssetLicenseData> assetLicenseDataList = new ArrayList<>();
        Set<String> assetIds = individualLicenseAssetIdMap.get(individualLicense);
        if (assetIds != null) {
            for (String assetId : assetIds) {
                final AssetLicenseData assetLicenseData = assetIdAssetLicenseDataMap.get(assetId);
                if (assetLicenseData != null) {
                    assetLicenseDataList.add(assetLicenseData);
                }
            }

        }

        Collections.sort(assetLicenseDataList, (o1, o2) ->
                Objects.compare(assetSortString(o1), assetSortString(o2), String::compareToIgnoreCase));

        return assetLicenseDataList;
    }

    private String assetSortString(AssetLicenseData o1) {
        return o1.getAssetName() + "-" + o1.getAssetVersion();
    }

    // FIXME: revise naming
    public boolean containsNonOsiApprovedLicense(String license) {
        if (license == null) return true;

        LicenseData licenseData = inventory.findMatchingLicenseData(license);
        if (licenseData != null) {
            if ("true".equalsIgnoreCase(licenseData.get("Non-OSI Approved Variant"))) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

}
