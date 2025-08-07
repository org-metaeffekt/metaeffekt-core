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

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaReference;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.msrcdata.MsrcRemediation</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaMsrcRemediation implements Comparable<AeaaMsrcRemediation> {

    private final String type;
    private final String subType;
    private final String description;
    private final AeaaReference url;
    private final Set<String> affectedProductIds;
    private final String fixedBuild;
    private final String supercedence;

    public AeaaMsrcRemediation(String type, String subType, String description, AeaaReference url, Set<String> affectedProductIds, String fixedBuild, String supercedence) {
        this.type = type;
        this.subType = subType;
        this.description = description;
        this.url = url;
        this.affectedProductIds = affectedProductIds;
        this.fixedBuild = fixedBuild;
        this.supercedence = supercedence;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public String getDescription() {
        return description;
    }

    public AeaaReference getUrl() {
        return url;
    }

    public Set<String> getAffectedProductIds() {
        return affectedProductIds;
    }

    public String getFixedBuild() {
        return fixedBuild;
    }

    public String getSupercedence() {
        return supercedence;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        json.put("type", type);
        json.put("subType", subType);
        json.put("description", description);
        json.put("url", url == null ? null : url.toJson());
        json.put("affectedProductIds", new JSONArray(affectedProductIds));
        json.put("fixedBuild", fixedBuild);
        json.put("supercedence", supercedence);

        return json;
    }

    public static AeaaMsrcRemediation fromJson(JSONObject json) {
        return new AeaaMsrcRemediation(
                json.optString("type", null),
                json.optString("subType", null),
                json.optString("description", null),
                json.has("url") ? AeaaReference.fromJson(json.getJSONObject("url")) : null,
                json.getJSONArray("affectedProductIds").toList().stream().map(Object::toString).collect(Collectors.toSet()),
                json.optString("fixedBuild", null),
                json.optString("supercedence", null)
        );
    }

    public static AeaaMsrcRemediation fromMap(Map<String, Object> map) {
        return new AeaaMsrcRemediation(
                (String) map.get("type"),
                (String) map.get("subType"),
                (String) map.get("description"),
                map.containsKey("url") ? AeaaReference.fromMap((Map<String, Object>) map.get("url")) : null,
                ((List<String>) map.get("affectedProductIds")).stream().collect(Collectors.toSet()),
                (String) map.get("fixedBuild"),
                (String) map.get("supercedence")
        );
    }

    public static List<AeaaMsrcRemediation> fromJson(JSONArray json) {
        final List<AeaaMsrcRemediation> threats = new ArrayList<>();

        for (Object object : json) {
            threats.add(fromJson((JSONObject) object));
        }

        return threats;
    }

    public static List<AeaaMsrcRemediation> fromMap(List<Map<String, Object>> map) {
        final List<AeaaMsrcRemediation> threats = new ArrayList<>();

        for (Map<String, Object> threat : map) {
            threats.add(fromMap(threat));
        }

        return threats;
    }

    @Override
    public int compareTo(AeaaMsrcRemediation o) {
        return Comparator.comparing((AeaaMsrcRemediation msrcRemediation) -> msrcRemediation == null ? "" : msrcRemediation.getDescription())
                .compare(this, o);
    }

    /**
     * Consolidates a collection of MsrcRemediation objects in-place.
     * <p>
     * This method groups remediations by a logical identifier and merges them.
     * It is designed to be generic: it distinguishes between numeric identifiers (like
     * KB articles) and non-numeric ones (like software components) to create the
     * correct grouping key without using hard-coded names.
     *
     * @param remediations The collection of MsrcRemediation objects to update.
     */
    public static void mergeRemediations(Collection<AeaaMsrcRemediation> remediations) {
        if (remediations == null || remediations.isEmpty()) {
            return;
        }

        final Map<String, AeaaMsrcRemediation> consolidatedMap = new LinkedHashMap<>();

        for (AeaaMsrcRemediation remediation : remediations) {
            if (remediation == null) continue;

            final String key = generateDeduplicationKey(remediation);
            if (key == null) continue;

            consolidatedMap.merge(key, remediation, AeaaMsrcRemediation::merge);
        }

        remediations.clear();
        remediations.addAll(consolidatedMap.values());
    }

    /**
     * Generates a key for grouping. For non-numeric identifiers (components),
     * it creates a composite key with the fixed build to differentiate versions.
     *
     * @param remediation The remediation to generate a key for.
     * @return The key string, or null if no identifier can be found.
     */
    private static String generateDeduplicationKey(AeaaMsrcRemediation remediation) {
        final String identifier = StringUtils.hasText(remediation.getDescription()) ?
                remediation.getDescription() :
                remediation.getSubType();
        if (identifier == null) return null;

        // non-numeric identifier with a fixed build is a versioned component.
        if (!identifier.matches("\\d+") && StringUtils.hasText(remediation.getFixedBuild())) {
            return identifier + "#" + remediation.getFixedBuild();
        }

        return identifier;
    }

    private static AeaaMsrcRemediation merge(AeaaMsrcRemediation existing, AeaaMsrcRemediation replacement) {
        final AeaaMsrcRemediation primary = getRemediationTypePriority(existing) >= getRemediationTypePriority(replacement) ? existing : replacement;
        final AeaaMsrcRemediation secondary = primary == existing ? replacement : existing;

        final Set<String> combinedProductIds = Stream.concat(
                primary.getAffectedProductIds().stream(),
                secondary.getAffectedProductIds().stream()
        ).collect(Collectors.toSet());

        final AeaaReference reference = (primary.getUrl() == null || StringUtils.isEmpty(primary.getUrl().getUrl())) ? secondary.getUrl() : primary.getUrl();

        return new AeaaMsrcRemediation(
                primary.getType(),
                primary.getSubType(),
                isEmptyDescription(primary.getDescription()) ? secondary.getDescription() : primary.getDescription(),
                reference,
                combinedProductIds,
                ObjectUtils.firstNonNull(primary.getFixedBuild(), secondary.getFixedBuild()),
                primary.getSupercedence()
        );
    }

    private static boolean isEmptyDescription(String description) {
        if (StringUtils.isEmpty(description)) return true;
        if ("No description provided.".equals(description)) return true;
        return false;
    }

    private static int getRemediationTypePriority(AeaaMsrcRemediation remediation) {
        if (remediation == null || !StringUtils.hasText(remediation.getType())) {
            return 0;
        }
        switch (remediation.getType()) {
            case "Vendor Fix":
                return 4;
            case "Security Update":
                return 3;
            case "Known Issue":
                return 2;
            case "Release Notes":
                return 1;
            default:
                return 0;
        }
    }
}
