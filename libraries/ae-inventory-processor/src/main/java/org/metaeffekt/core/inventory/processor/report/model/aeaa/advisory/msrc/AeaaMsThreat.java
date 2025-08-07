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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.msrc;

import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.msrcdata.MsThreat</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaMsThreat implements Comparable<AeaaMsThreat> {

    private final String type;
    private final String productId;
    private final String description;

    public AeaaMsThreat(String type, String productId, String description) {
        this.type = type;
        this.productId = productId;
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        json.put("type", type);
        json.put("productId", productId);
        json.put("description", description);

        return json;
    }

    public static AeaaMsThreat fromJson(JSONObject json) {
        return new AeaaMsThreat(
                json.optString("type", null),
                json.optString("productId", null),
                json.optString("description", null)
        );
    }

    public static AeaaMsThreat fromMap(Map<String, Object> map) {
        return new AeaaMsThreat(
                (String) map.get("type"),
                (String) map.get("productId"),
                (String) map.get("description")
        );
    }

    public static List<AeaaMsThreat> fromJson(JSONArray json) {
        final List<AeaaMsThreat> threats = new ArrayList<>();

        for (int i = 0; i < json.length(); i++) {
            final JSONObject threat = json.optJSONObject(i);
            if (threat != null) {
                threats.add(fromJson(threat));
            }
        }

        return threats;
    }

    public static List<AeaaMsThreat> fromMap(List<Map<String, Object>> map) {
        final List<AeaaMsThreat> threats = new ArrayList<>();

        for (Map<String, Object> threat : map) {
            if (threat != null) {
                threats.add(fromMap(threat));
            }
        }

        return threats;
    }

    public final static Comparator<AeaaMsThreat> ID_TYPE_DESC_COMPARATOR = Comparator.comparing(AeaaMsThreat::getProductId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AeaaMsThreat::getType, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AeaaMsThreat::getDescription, Comparator.nullsLast(Comparator.naturalOrder()));


    @Override
    public int compareTo(AeaaMsThreat o) {
        if (o == null) return 1;
        return ID_TYPE_DESC_COMPARATOR.compare(this, o);
    }

    /**
     * Deduplicates a collection of MsThreat objects in-place based on their productId.
     * <p>
     * For each unique productId, it keeps only the threat that has the most
     * information, defined by the number of non-empty fields.
     *
     * @param threats The collection of threat objects to update. It is modified directly.
     */
    public static void deduplicateByProductId(Collection<AeaaMsThreat> threats) {
        if (threats == null || threats.isEmpty()) return;

        final Set<String> uniqueKeys = new HashSet<>();
        boolean hasDuplicate = false;
        for (AeaaMsThreat threat : threats) {
            if (threat == null || StringUtils.isEmpty(threat.getProductId())) continue;
            if (!uniqueKeys.add(threat.getProductId() + "-" + threat.getType())) {
                hasDuplicate = true;
                break;
            }
        }
        if (!hasDuplicate) return;

        final Map<String, AeaaMsThreat> bestThreatsMap = new LinkedHashMap<>();
        for (AeaaMsThreat threat : threats) {
            if (threat == null || StringUtils.isEmpty(threat.getProductId())) continue;
            bestThreatsMap.merge(threat.getProductId() + "-" + threat.getType(), threat, (existing, replacement) -> calculateInformationScore(replacement) >= calculateInformationScore(existing) ? replacement : existing);
        }

        threats.clear();
        threats.addAll(bestThreatsMap.values());
    }

    /**
     * Calculates a score for a threat based on how many of its fields contain text.
     * The score is the sum of points for each populated field (type, description).
     *
     * @param threat The AeaaMsThreat object to score. Must not be null.
     * @return An integer score representing the amount of information in the threat.
     */
    private static int calculateInformationScore(@NonNull AeaaMsThreat threat) {
        return (StringUtils.hasText(threat.getType()) ? 1 : 0) + (StringUtils.hasText(threat.getDescription()) ? 1 : 0);
    }
}
