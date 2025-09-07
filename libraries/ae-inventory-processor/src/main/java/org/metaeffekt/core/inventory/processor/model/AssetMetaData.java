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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Model class that supports to aggregate data around assets.
 */
public class AssetMetaData extends AbstractModelBase {

    private static final Logger LOG = LoggerFactory.getLogger(AssetMetaData.class);

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    public AssetMetaData(AssetMetaData ld) {
        super(ld);
    }

    public AssetMetaData() {
    }

    /**
     * Core attributes to support license data.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        ASSET_ID("Asset Id"),
        ASSET_PATH("Asset Path"),
        NAME("Name"),
        VERSION("Version"),
        ASSESSMENT_ID("Assessment Id"),
        @Deprecated // use AUDIENCE instead; introduced for compatibility with downstream branches; remove once resolved
        ROLE("Audience"),
        AUDIENCE("Audience"),
        ASSESSMENT("Assessment"),

        CHECKSUM("Checksum"),

        HASH_SHA1("Hash (SHA1)"),

        HASH_SHA256("Hash (SHA-256)"),
        HASH_SHA512("Hash (SHA-512)"),

        URL("URL"),

        // FIXME: consolidate
        SOURCE_CODE_URL("Source Code URL"),
        SUPPLIER("Supplier"),
        TYPE("Type");

        private String key;

        Attribute(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();
    static {
        // fix selection and order
        CORE_ATTRIBUTES.add(Attribute.ASSET_ID.getKey());
    }

    public static ArrayList<String> ORDERED_ATTRIBUTES = new ArrayList<>();
    static {
        // fix selection and order
        ORDERED_ATTRIBUTES.add(Attribute.ASSET_ID.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.NAME.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.TYPE.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.ASSET_PATH.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.VERSION.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.ASSESSMENT_ID.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.ASSESSMENT.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.AUDIENCE.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.CHECKSUM.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.HASH_SHA1.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.HASH_SHA256.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.HASH_SHA512.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.URL.getKey());
        ORDERED_ATTRIBUTES.add(Attribute.SUPPLIER.getKey());
    }

    public String deriveAssessmentId(Inventory inventory) {
        final Set<String> knownIds = inventory.getAssetMetaData().stream()
                .map(asset -> asset.get(Attribute.ASSESSMENT_ID))
                .collect(Collectors.toSet());
        return deriveAssessmentId(inventory, knownIds);
    }

    public String deriveAssessmentId(Inventory inventory, Set<String> knownIds) {
        if (StringUtils.isEmpty(get(Attribute.ASSESSMENT_ID))) {
            String baseAssessmentId;
            if (StringUtils.isNotBlank(get(Attribute.ASSET_ID))) {
                baseAssessmentId = get(Attribute.ASSET_ID);
            } else if (StringUtils.isNotBlank(get(Attribute.NAME))) {
                baseAssessmentId = get(Attribute.NAME);
            } else {
                baseAssessmentId = null;
            }

            final String assessmentId = baseAssessmentId != null ? baseAssessmentId + "-" + generateUniqueAssessmentSuffix(inventory) : generateUniqueAssessmentSuffix(inventory);
            if (knownIds.contains(assessmentId)) {
                set(Attribute.ASSESSMENT_ID, baseAssessmentId + "-" + seededRandomUniqueAssessmentSuffix(UUID.randomUUID().toString()));
            } else {
                set(Attribute.ASSESSMENT_ID, assessmentId);
            }
        }

        return get(Attribute.ASSESSMENT_ID);
    }

    private String generateUniqueAssessmentSuffix(Inventory inventory) {
        final InventoryInfo inventoryEnrichmentInfo = inventory.findInventoryInfo("inventory-enrichment");

        if (inventoryEnrichmentInfo != null) {
            final String inventoryEnrichmentStepsString = inventoryEnrichmentInfo.get("Steps");
            if (StringUtils.isNotEmpty(inventoryEnrichmentStepsString) && inventoryEnrichmentStepsString.startsWith("[") && inventoryEnrichmentStepsString.endsWith("]")) {

                try {
                    final JSONArray inventoryEnrichmentSteps = new JSONArray(inventoryEnrichmentStepsString);
                    final StringBuilder assessmentFilesSeed = new StringBuilder();

                    for (int i = 0; i < inventoryEnrichmentSteps.length(); i++) {
                        // locate steps "Vulnerability Status" and "Vulnerability Keywords" and combine the file paths
                        final String stepName = inventoryEnrichmentSteps.getJSONObject(i).getString("name");
                        if ("Vulnerability Status".equals(stepName) || "Vulnerability Keywords".equals(stepName)) {
                            final JSONObject configuration = inventoryEnrichmentSteps.getJSONObject(i).getJSONObject("configuration");
                            final JSONArray yamlFiles = configuration.optJSONArray("yamlFiles");
                            if (yamlFiles != null) {
                                for (int j = 0; j < yamlFiles.length(); j++) {
                                    assessmentFilesSeed.append(yamlFiles.getString(j));
                                }
                            }
                        }
                    }

                    return seededRandomUniqueAssessmentSuffix(assessmentFilesSeed.toString());
                } catch (Exception e) {
                    LOG.warn("Failed to parse inventory enrichment steps from inventory info [inventory-enrichment] --> [Steps]: " + inventoryEnrichmentStepsString, e);
                }
            }
        }

        final InventoryInfo vulnerabilityStatusInfo = inventory.findInventoryInfo("vulnerability-status");
        if (vulnerabilityStatusInfo != null) {
            final String statusFilesString = vulnerabilityStatusInfo.get("Vulnerability Inventory Status");
            if (StringUtils.isNotEmpty(statusFilesString) && statusFilesString.startsWith("[") && statusFilesString.endsWith("]")) {
                try {
                    final JSONArray statusFiles = new JSONArray(statusFilesString);
                    final StringBuilder assessmentEntrySeed = new StringBuilder();

                    for (int i = 0; i < statusFiles.length(); i++) {
                        final JSONObject statusFile = statusFiles.getJSONObject(i);
                        if (statusFile.has("cvss2")) {
                            assessmentEntrySeed.append(statusFile.getString("cvss2"));
                        }
                        if (statusFile.has("cvss3")) {
                            assessmentEntrySeed.append(statusFile.getString("cvss3"));
                        }
                        if (statusFile.has("cvss4")) {
                            assessmentEntrySeed.append(statusFile.getString("cvss4"));
                        }
                    }

                    return seededRandomUniqueAssessmentSuffix(assessmentEntrySeed.toString());
                } catch (Exception e) {
                    LOG.warn("Failed to parse status files from inventory info [vulnerability-status] --> [statusFiles]: " + statusFilesString, e);
                }
            }
        }

        return UUID.randomUUID().toString();
    }

    private String seededRandomUniqueAssessmentSuffix(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes()).toString().substring(0, 8);
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     *
     * @return Boolean indicating whether the instance is valid.
     */
    public boolean isValid() {
        if (!StringUtils.isNotBlank(get(Attribute.ASSET_ID))) return false;
        return true;
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        final StringBuilder sb = new StringBuilder();
        sb.append(get(Attribute.ASSET_ID));
        final String assetPath = get(Attribute.ASSET_PATH.getKey());
        if (assetPath != null) {
            sb.append("-").append(assetPath);
        }
        return sb.toString();
    }

    /**
     * The compare string representations is built from the core attributes.
     *
     * @return String to compare component patterns.
     */
    public String createCompareStringRepresentation() {
        return createCompareStringRepresentation(CORE_ATTRIBUTES);
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    public void set(Attribute attribute, Boolean value) {
        if (value == null) {
            set(attribute.getKey(), null);
        } else {
            set(attribute.getKey(), value.toString());
        }
    }

    public boolean is(Attribute attribute) {
        return Boolean.TRUE.equals(get(attribute.getKey(), "false"));
    }

    public boolean isPrimary() {
        return Constants.MARKER_CROSS.equalsIgnoreCase(get(Constants.KEY_PRIMARY));
    }

    @Override
    public String toString() {
        return "Asset Id: " + get(Attribute.ASSET_ID);
    }

    public void merge(AssetMetaData a) {
        super.merge(a);
    }
}
