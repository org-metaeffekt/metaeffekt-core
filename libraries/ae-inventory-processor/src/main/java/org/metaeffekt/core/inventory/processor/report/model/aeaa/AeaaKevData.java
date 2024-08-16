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

package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

@Setter
@Getter
public class AeaaKevData {
    private String vulnerability;
    private String vendor;
    private String product;
    private String summary;
    private String description;
    private String notes;
    private String recommendation;
    private Date publishDate;
    private Date exploitDate;
    private Date dueDate;
    private RansomwareState ransomwareState;

    public AeaaKevData(String vulnerability, RansomwareState state) {
        this.vulnerability = vulnerability;
        this.ransomwareState = state;
    }

    public AeaaKevData() {
    }

    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("vulnerability", vulnerability);
        jsonObject.put("vendor", vendor);
        jsonObject.put("product", product);
        jsonObject.put("summary", summary);
        jsonObject.put("description", description);
        jsonObject.put("recommendation", recommendation);
        jsonObject.put("notes", notes);
        jsonObject.put("publishDate", publishDate == null ? null : publishDate.getTime());
        jsonObject.put("exploitDate", exploitDate == null ? null : exploitDate.getTime());
        jsonObject.put("dueDate", dueDate == null ? null : dueDate.getTime());
        jsonObject.put("knownRansomwareCampaignUse", ransomwareState);
        return jsonObject;
    }

    public static AeaaKevData fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return fromInputMap(json.toMap());
    }

    public static AeaaKevData fromInputMap(Map<String, Object> map) {
        final AeaaKevData aeaaKevData = new AeaaKevData();
        aeaaKevData.setVulnerability((String) map.get("vulnerability"));
        aeaaKevData.setVendor((String) map.get("vendor"));
        aeaaKevData.setProduct((String) map.get("product"));
        aeaaKevData.setSummary((String) map.get("summary"));
        aeaaKevData.setDescription((String) map.get("description"));
        aeaaKevData.setRecommendation((String) map.get("recommendation"));
        aeaaKevData.setNotes((String) map.get("notes"));
        aeaaKevData.setPublishDate(AeaaTimeUtils.tryParse(map.get("publishDate")));
        aeaaKevData.setExploitDate(AeaaTimeUtils.tryParse(map.get("exploitDate")));
        aeaaKevData.setDueDate(AeaaTimeUtils.tryParse(map.get("dueDate")));
        if (map.containsKey("knownRansomwareCampaignUse")) {
            aeaaKevData.setRansomwareState(RansomwareState.valueOf((String) map.get("knownRansomwareCampaignUse")));
        }
        return aeaaKevData;
    }

    public static AeaaKevData fromCisaKev(JSONObject json) {
        final Map<String, Object> map = json.toMap();
        final AeaaKevData aeaaKevData = new AeaaKevData();
        aeaaKevData.setVulnerability((String) map.getOrDefault("cveID", null));
        aeaaKevData.setVendor((String) map.getOrDefault("vendorProject", null));
        aeaaKevData.setProduct((String) map.getOrDefault("product", null));
        aeaaKevData.setSummary((String) map.getOrDefault("vulnerabilityName", null));
        aeaaKevData.setRecommendation((String) map.getOrDefault("requiredAction", null));
        aeaaKevData.setNotes((String) map.getOrDefault("notes", null));
        aeaaKevData.setDescription((String) map.getOrDefault("shortDescription", null));
        // publication date is not used as exploitation date, since CISA does not always publish KEV entries upon known exploitation,
        // but also sometimes (often) retroactively adds 20-year-old vulnerabilities as exploited vulnerabilities.
        // https://www.linkedin.com/pulse/epss-gaps-cisa-kev-real-world-examples-stack-aware-url2e/
        aeaaKevData.setPublishDate(AeaaTimeUtils.tryParse((String) map.getOrDefault("dateAdded", null)));
        aeaaKevData.setDueDate(AeaaTimeUtils.tryParse((String) map.getOrDefault("dueDate", null)));

        String ransomwareKnown = (String) map.getOrDefault("knownRansomwareCampaignUse", null);
        if (ransomwareKnown != null) {
            switch (ransomwareKnown) {
                case "Known":
                    aeaaKevData.setRansomwareState(RansomwareState.KNOWN);
                    break;
                case "Unknown":
                default:
                    aeaaKevData.setRansomwareState(RansomwareState.UNKNOWN);
            }
        }
        return aeaaKevData;
    }

    public String getFormatedPublishDate() {
        return AeaaTimeUtils.formatNormalizedDateOnlyDate(publishDate);
    }

    public String getFormatedDueDate() {
        return AeaaTimeUtils.formatNormalizedDateOnlyDate(dueDate);
    }

    private static String returnNullIfEmpty(String string) {
        return StringUtils.isEmpty(string) ? null : string;
    }

    public enum RansomwareState {
        UNKNOWN,
        KNOWN
    }
}

