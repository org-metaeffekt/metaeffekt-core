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

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaContentIdentifiers;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.advisory.GhsaAdvisorEntry</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaGhsaAdvisorEntry extends AeaaAdvisoryEntry {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaGhsaAdvisorEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
        add("severity");
        add("githubReviewed");
        add("githubReviewedAt");
        add("nvdPublishedAt");
        add("vulnerableSoftware");
    }};


    private String severity;
    private boolean githubReviewed;

    private Date githubReviewedAt;
    private Date nvdPublishedAt;

    public AeaaGhsaAdvisorEntry() {
        super(AeaaContentIdentifiers.GHSA);
    }

    public AeaaGhsaAdvisorEntry(String id) {
        super(AeaaContentIdentifiers.GHSA, id);
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isGithubReviewed() {
        return githubReviewed;
    }

    public void setGithubReviewed(boolean githubReviewed) {
        this.githubReviewed = githubReviewed;
    }

    public Date getGithubReviewedAt() {
        return githubReviewedAt;
    }

    public void setGithubReviewedAt(Date githubReviewedAt) {
        this.githubReviewedAt = githubReviewedAt;
    }

    public Date getNvdPublishedAt() {
        return nvdPublishedAt;
    }

    public void setNvdPublishedAt(Date nvdPublishedAt) {
        this.nvdPublishedAt = nvdPublishedAt;
    }

    @Override
    public String getUrl() {
        return "https://github.com/advisories/" + getId();
    }

    @Override
    public String getType() {
        return AdvisoryUtils.normalizeType("alert");
    }

    /* TYPE CONVERSION METHODS */

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static AeaaGhsaAdvisorEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, AeaaGhsaAdvisorEntry::new);
    }

    public static AeaaGhsaAdvisorEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, AeaaGhsaAdvisorEntry::new);
    }

    public static AeaaGhsaAdvisorEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, AeaaGhsaAdvisorEntry::new);
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
    }

    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);

        this.setSeverity((String) map.getOrDefault("severity", null));
        this.setGithubReviewed((boolean) map.getOrDefault("githubReviewed", false));
        this.setGithubReviewedAt(AeaaTimeUtils.tryParse(map.getOrDefault("githubReviewedAt", null)));
        this.setNvdPublishedAt(AeaaTimeUtils.tryParse(map.getOrDefault("nvdPublishedAt", null)));

        final List<Object> vulnerableSoftwareArray = (List<Object>) map.getOrDefault("vulnerableSoftware", null);
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);

        json.put("severity", getSeverity());
        json.put("githubReviewed", isGithubReviewed());
        json.put("githubReviewedAt", ObjectUtils.defaultIfNull(githubReviewedAt == null ? null : githubReviewedAt.getTime(), JSONObject.NULL));
        json.put("nvdPublishedAt", ObjectUtils.defaultIfNull(nvdPublishedAt == null ? null : nvdPublishedAt.getTime(), JSONObject.NULL));
    }
}
