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
package org.metaeffekt.core.inventory.processor.report.model;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.report.InventoryReport.xmlEscapeId;
import static org.metaeffekt.core.inventory.processor.report.InventoryReport.xmlEscapeString;

public class AssetData {

    private Map<String, Set<String>> assetIdAssociatedLicenseMap = new HashMap<>();
    private Map<String, Set<String>> representedLicenseLicensesMap = new HashMap<>();

    private Map<String, Set<String>> representedLicenseAssetIdMap = new HashMap<>();
    private Map<String, Set<String>> individualLicenseAssetIdMap = new HashMap<>();

    private Set<String> associatedLicenses = new HashSet<>();

    private Set<String> representedAssociatedLicenses = new HashSet<>();

    private Map<String, AssetLicenseData> assetIdAssetLicenseDataMap = new HashMap<>();

    private Inventory inventory;

    private boolean includesOpenCoDESimilarLicense = false;

    /**
     * List to cover artifacts without license
     */
    private List<Artifact> artifactsWithoutLicense = new ArrayList<>();

    /**
     * Map for identifying the assets belonging to a given artifacts. Used primarily for artifacts without license.
     */
    private Map<Artifact, List<AssetMetaData>> artifactToAssetMetaDataMap = new HashMap<>();

    public static AssetData fromInventory(Inventory filteredInventory) {
        AssetData assetData = new AssetData();

        final Map<AssetMetaData, Set<Artifact>> assetMetaDataToArtifactsMap = InventoryUtils.buildAssetToArtifactMap(filteredInventory);

        assetData.insertData(filteredInventory, assetMetaDataToArtifactsMap);

        assetData.evaluateArtifactsWithoutLicense(filteredInventory, assetMetaDataToArtifactsMap);

        assetData.inventory = filteredInventory;
        return assetData;
    }

    private void evaluateArtifactsWithoutLicense(Inventory filteredInventory, Map<AssetMetaData, Set<Artifact>> assetMetaDataToArtifactsMap) {
        // collect artifacts that are components or component parts and have no license
        for (Artifact artifact : filteredInventory.getArtifacts()) {
            if (StringUtils.isEmpty(artifact.getLicense())) {
                if (artifact.isComponentOrComponentPart()) {
                    artifactsWithoutLicense.add(artifact);
                }
            }
        }

        // compute the artifact to assetId map
        for (Artifact artifact : artifactsWithoutLicense) {
            for (Map.Entry<AssetMetaData, Set<Artifact>> entry : assetMetaDataToArtifactsMap.entrySet()) {
                if (entry.getValue().contains(artifact)) {
                    artifactToAssetMetaDataMap.computeIfAbsent(artifact, (a) -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }
    }

    private void insertData(Inventory filteredInventory, Map<AssetMetaData, Set<Artifact>> assetMetaDataToArtifactsMap) {

        for (Map.Entry<AssetMetaData, Set<Artifact>> entry : assetMetaDataToArtifactsMap.entrySet()) {

            final AssetMetaData assetMetaData = entry.getKey();
            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

            final HashSet<String> assetAssociatedLicenses = new HashSet<>();

            for (Artifact artifact : entry.getValue()) {

                // iterate associated licenses
                final List<String> associatedLicenses = artifact.getLicenses();
                for (final String associatedLicense : associatedLicenses) {
                    if (StringUtils.isBlank(associatedLicense)) continue;

                    // contribute to overall license set
                    this.associatedLicenses.add(associatedLicense);

                    // add representedAs licenses (complete map of license to representedAs/license)
                    final LicenseData licenseData = filteredInventory.findMatchingLicenseData(associatedLicense);
                    String representedAsLicense = evaluateRepresentedAs(associatedLicense, licenseData);
                    representedAssociatedLicenses.add(representedAsLicense);

                    // contribute to representedLicenses maps
                    representedLicenseLicensesMap.computeIfAbsent(representedAsLicense, c -> new HashSet<>()).add(associatedLicense);
                    representedLicenseAssetIdMap.computeIfAbsent(representedAsLicense, c -> new HashSet<>()).add(assetId);

                    // contribute assetId to associatedLicense
                    individualLicenseAssetIdMap.computeIfAbsent(associatedLicense, c -> new HashSet<>()).add(assetId);
                    individualLicenseAssetIdMap.computeIfAbsent(representedAsLicense, c -> new HashSet<>()).add(assetId);

                    // contribute license to asset
                    assetIdAssociatedLicenseMap.computeIfAbsent(assetId, c -> new HashSet<>()).add(associatedLicense);

                    // contribute associated license to local set
                    assetAssociatedLicenses.add(associatedLicense);

                    // check whether includesOpenCoDESimilarLicense must be updated
                    if (licenseData != null) {
                        final String openCodeStatus = licenseData.get("Open CoDE Status");
                        if ("(approved)".equalsIgnoreCase(openCodeStatus)) {
                            includesOpenCoDESimilarLicense = true;
                        }
                    }
                }
            }

            // once iterated over all artifacts associated with assetId we contribute to assetIdAssetLicenseDataMap
            assetIdAssetLicenseDataMap.put(assetId, createAssetLicenseData(assetMetaData, assetAssociatedLicenses));
        }
    }

    private String evaluateRepresentedAs(String associatedLicense, LicenseData licenseData) {
        String representedAs = licenseData != null ? licenseData.get(LicenseData.Attribute.REPRESENTED_AS) : null;
        // the license represents itself
        if (representedAs == null) {
            representedAs = associatedLicense;
        }
        return representedAs;
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

    public List<String> getRepresentedAssociatedLicensesWithoutOption() {
        return sortedList(this.representedAssociatedLicenses).stream().filter(s -> !s.contains(" + ")).collect(Collectors.toList());
    }

    public List<String> getRepresentedAssociatedLicensesWithOption() {
        return sortedList(this.representedAssociatedLicenses).stream().filter(s -> s.contains(" + ")).collect(Collectors.toList());
    }

    public List<String> getLicensesForRepresentedLicense(String representedAssociatedLicense) {
        return sortedList(representedLicenseLicensesMap.get(representedAssociatedLicense));
    }

    public int countAssetsWithRepresentedAssociatedLicense(String representedAssociatedLicense, boolean handleSubstructure) {
        final Set<String> assetIds = handleSubstructure ?
                representedLicenseAssetIdMap.get(representedAssociatedLicense) :
                individualLicenseAssetIdMap.get(representedAssociatedLicense);

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
        final List<AssetLicenseData> assetLicenseDataList = new ArrayList<>();
        final Set<String> assetIds = individualLicenseAssetIdMap.get(individualLicense);
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

    public boolean isIncludesOpenCoDESimilarLicense() {
        return includesOpenCoDESimilarLicense;
    }

    public List<Artifact> getArtifactsWithoutLicense() {
        return artifactsWithoutLicense;
    }

    public List<AssetMetaData> getAssetMetaDataForArtifact(Artifact artifact) {
        return artifactToAssetMetaDataMap.getOrDefault(artifact, Collections.emptyList());
    }

    public String toXml(List<AssetMetaData> assetMetaDataList, String topicId) {
        return assetMetaDataList.stream().map(a -> toAssetXref(a, topicId)).collect(Collectors.joining(", "));
    }

    public String toAssetXref(AssetMetaData a, String topicId) {
        String name = a.get(AssetMetaData.Attribute.NAME);
        String id =  "asset-" + a.get(AssetMetaData.Attribute.ASSET_ID).toLowerCase();

        return String.format("<xref href=\"%s#%s\" type=\"topic\">%s</xref>", topicId, xmlEscapeId(id), xmlEscapeString(name));
    }

    public String toAssetXref(AssetLicenseData a, String topicId) {
        String name = a.getAssetName();
        String id =  "asset-" + a.deriveId().toLowerCase();

        return String.format("<xref href=\"%s#%s\" type=\"topic\">%s</xref>", topicId, xmlEscapeId(id), xmlEscapeString(name));
    }

}
