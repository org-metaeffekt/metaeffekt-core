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

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class AeaaConsequence {

    private final List<String> scope = new ArrayList<>();
    private final List<String> impact = new ArrayList<>();
    private String likelihood;
    private String note;

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("scope", new JSONArray(scope));
        json.put("impact", new JSONArray(impact));
        json.put("likelihood", likelihood);
        json.put("note", note);
        return json;
    }

    public static AeaaConsequence fromJson(JSONObject json) {
        AeaaConsequence consequence = new AeaaConsequence();
        JSONArray jsonScope = json.optJSONArray("scope");
        if (jsonScope != null) {
            for (int i = 0; i < jsonScope.length(); i++) {
                consequence.getScope().add(jsonScope.optString(i));
            }
        }
        JSONArray jsonImpact = json.optJSONArray("impact");
        if (jsonImpact != null) {
            for (int i = 0; i < jsonImpact.length(); i++) {
                consequence.getImpact().add(jsonImpact.optString(i));
            }
        }
        consequence.setLikelihood(json.optString("likelihood"));
        consequence.setNote(json.optString("note"));
        return consequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AeaaConsequence that = (AeaaConsequence) o;
        return scope.equals(that.scope) && impact.equals(that.impact) && Objects.equals(likelihood, that.likelihood) && Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, impact, likelihood, note);
    }
}