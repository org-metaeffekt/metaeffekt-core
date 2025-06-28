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

package org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class AeaaWeaknessConsequence {

    private final List<String> scope = new ArrayList<>();
    private final List<String> impact = new ArrayList<>();
    private String likelihood;
    private String note;

    public JSONObject toJson() {
        return new JSONObject()
                .put("scope", new JSONArray(scope))
                .put("impact", new JSONArray(impact))
                .put("likelihood", likelihood)
                .put("note", note);
    }

    public static AeaaWeaknessConsequence fromJson(JSONObject json) {
        final AeaaWeaknessConsequence consequence = new AeaaWeaknessConsequence();
        final JSONArray jsonScope = json.optJSONArray("scope");
        if (jsonScope != null) {
            for (int i = 0; i < jsonScope.length(); i++) {
                consequence.getScope().add(jsonScope.optString(i));
            }
        }
        final JSONArray jsonImpact = json.optJSONArray("impact");
        if (jsonImpact != null) {
            for (int i = 0; i < jsonImpact.length(); i++) {
                consequence.getImpact().add(jsonImpact.optString(i));
            }
        }
        consequence.setLikelihood(json.optString("likelihood"));
        consequence.setNote(json.optString("note"));
        return consequence;
    }
}