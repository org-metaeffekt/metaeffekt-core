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

package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaTimeUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Represents an advisory entry sourced from the Open Source Vulnerabilities (OSV) database.
 * This class extends {@link AeaaAdvisoryEntry} and includes additional fields and methods
 * specific to OSV advisories, such as severity, GitHub review status, publication dates,
 * and affected software version ranges.
 *
 * <p>
 * Instances of this class can be constructed from various data sources including JSON objects,
 * maps, and Lucene documents. The class provides functionality to parse OSV-specific JSON
 * structures and integrate them into the advisory model.
 * </p>
 *
 * <p>
 * The class handles conversion of advisory data to and from different representations,
 * ensuring seamless integration with storage and processing systems.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>
 *     JSONObject osvJson = ...; // JSON object from OSV API
 *     String ecosystem = "npm";
 *     AeaaAdvisoryEntry entry = AeaaAdvisoryEntry.fromOsvDownloadJson(osvJson, ecosystem);
 * </pre>
 *
 * @see AeaaAdvisoryEntry
 */
public class AeaaOsvAdvisorEntry extends AeaaAdvisoryEntry {

    private static final Logger LOG = LoggerFactory.getLogger(AeaaAdvisoryEntry.class);

    @Getter
    @Setter
    private String originEcosystem;

    @Getter
    @Setter
    private String severity;

    @Getter
    @Setter
    private boolean githubReviewed;

    @Getter
    @Setter
    private Date githubReviewedAt;

    @Getter
    @Setter
    private Date nvdPublishedAt;

    @Getter
    @Setter
    private Set<String> affectedEcosystems = new HashSet<>();

    @Getter
    private final List<String> packageUrls = new ArrayList<>();
    
    protected static final Set<String> CONVERSION_KEYS_AMB = Collections.unmodifiableSet(
            new HashSet<>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB));

    protected static final Set<String> CONVERSION_KEYS_MAP = Collections.unmodifiableSet(
            new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
                add("ecosystem");
                add("affectedEcosystems");
                add("severity");
                add("githubReviewed");
                add("githubReviewedAt");
                add("nvdPublishedAt");
                add("purls");
            }});

    public AeaaOsvAdvisorEntry() {
        super(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER);
    }

    public AeaaOsvAdvisorEntry(AeaaAdvisoryTypeIdentifier<?> sourceIdentifier) {
        super(sourceIdentifier);
    }

    public AeaaOsvAdvisorEntry(AeaaAdvisoryTypeIdentifier<?> sourceIdentifier, String id) {
        super(sourceIdentifier, id);
    }

    /**
     * Returns the URL associated with this advisories Ecosystem.
     * @return the advisory URL
     */
    @Override
    public String getUrl() {
        if (super.getUrl() != null) {
            return super.getUrl();
        }
        return constructUrlFromId(super.getId(), super.getSourceIdentifier());
    }

    private static String constructUrlFromId(String id, AeaaAdvisoryTypeIdentifier<?> sourceIdentifier) {
        if (sourceIdentifier != null && sourceIdentifier.getName().equals("GHSA")) {
            return String.format("https://github.com/advisories/%s", id);
        }
        return String.format("https://osv.dev/vulnerability/%s", id);
    }

    @Override
    public String getType() {
        return AdvisoryUtils.normalizeType("alert");
    }

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static AeaaAdvisoryEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, () -> new AeaaAdvisoryEntry(identifyidentifier(amd.get("id"))));
    }

    public static AeaaAdvisoryEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, () -> new AeaaAdvisoryEntry(identifyidentifier((String) map.get("id"))));
    }

    public static AeaaAdvisoryEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, () -> new AeaaAdvisoryEntry(identifyidentifier(json.optString("id"))));
    }
    
    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);

        if (StringUtils.hasText(amd.get(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_STATE.getKey()))) {
            this.setGithubReviewed(Boolean.parseBoolean(amd.get(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_STATE.getKey())));
        }
        if (StringUtils.hasText(amd.get(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_DATE.getKey()))) {
            this.setGithubReviewedAt(AeaaTimeUtils.tryParse(amd.get(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_DATE.getKey())));
        }
    }
    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
        super.appendToBaseModel(amd);

        amd.set(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_STATE.getKey(), String.valueOf(isGithubReviewed()));
        amd.set(AeaaInventoryAttribute.ADVISOR_OSV_GHSA_REVIEWED_DATE.getKey(), githubReviewedAt == null ? null : String.valueOf(githubReviewedAt.getTime()));
    }

    @Override
    public void appendFromDataClass(AeaaAdvisoryEntry dataClass) {
        super.appendFromDataClass(dataClass);

        final AeaaOsvAdvisorEntry osvAdvisorEntry = (AeaaOsvAdvisorEntry) dataClass;

        this.setOriginEcosystem(osvAdvisorEntry.getOriginEcosystem());
        this.setAffectedEcosystems(osvAdvisorEntry.getAffectedEcosystems());
        this.setSeverity(osvAdvisorEntry.getSeverity());
        this.setGithubReviewed(osvAdvisorEntry.isGithubReviewed());
        this.setGithubReviewedAt(osvAdvisorEntry.getGithubReviewedAt());
        this.packageUrls.addAll(osvAdvisorEntry.getPackageUrls());
    }

    /**
     * Appends data from the provided map to this advisory entry.
     *
     * @param map the map containing advisory data
     */
    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);

        this.setOriginEcosystem((String) map.getOrDefault("ecosystem", null));
        this.setAffectedEcosystems((Set<String>) map.getOrDefault("affectedEcosystems",null));
        this.setSeverity((String) map.getOrDefault("severity", null));
        this.setGithubReviewed((boolean) map.getOrDefault("githubReviewed", false));
        this.setGithubReviewedAt(AeaaTimeUtils.tryParse(map.getOrDefault("githubReviewedAt", null)));
        this.setNvdPublishedAt(AeaaTimeUtils.tryParse(map.getOrDefault("nvdPublishedAt", null)));

        List<String> purls = (List) map.getOrDefault("purls", null);
        if (purls != null)
            this.packageUrls.addAll(purls);
    }

    /**
     * Appends data from this advisory entry to the provided JSON object.
     *
     * @param json the JSON object to populate with advisory data
     */
    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);

        json.put("ecosystem", getOriginEcosystem());
        json.put("affectedEcosystems", getAffectedEcosystems());
        json.put("severity", getSeverity());
        json.put("githubReviewed", isGithubReviewed());
        json.put("githubReviewedAt",
                ObjectUtils.defaultIfNull(githubReviewedAt == null ? null : githubReviewedAt.getTime(), JSONObject.NULL));
        json.put("nvdPublishedAt",
                ObjectUtils.defaultIfNull(nvdPublishedAt == null ? null : nvdPublishedAt.getTime(), JSONObject.NULL));

        final JSONArray purls = new JSONArray(packageUrls);
        json.put("purls", purls);
    }

    private static AeaaAdvisoryTypeIdentifier<?> identifyidentifier(String id){
        return AeaaAdvisoryTypeStore.get().osvValues().stream()
                .filter(identifier -> identifier.patternMatchesId(id))
                .findFirst()
                .orElse(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER);
    }
}
