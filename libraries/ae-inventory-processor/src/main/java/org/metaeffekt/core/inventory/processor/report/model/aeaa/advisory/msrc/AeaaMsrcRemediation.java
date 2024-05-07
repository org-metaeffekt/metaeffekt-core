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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaReference;

import java.util.*;
import java.util.stream.Collectors;

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
}
