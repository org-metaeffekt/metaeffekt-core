/*
 * Copyright 2009-2026 the original author or authors.
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
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

@Getter
@Setter
@Slf4j
@EqualsAndHashCode
@NoArgsConstructor
public class AeaaCapecEntry {

    private String id;
    private String name;
    private String abstraction;
    private AeaaMitre.Status status;
    private String description;
    private final Map<String, String> alternateTerms = new HashMap<>();
    private AeaaMitre.Severity likelihoodOfAttack;
    private AeaaMitre.Severity typicalSeverity;
    private final Map<AeaaMitre.Relation, List<String>> relatedAttackPatterns = new HashMap<>();
    private final ArrayList<AeaaWeaknessConsequence> consequences = new ArrayList<>();
    private final ArrayList<String> mitigations = new ArrayList<>();
    private final ArrayList<String> relatedWeaknesses = new ArrayList<>();
    private final List<String> prerequisites = new ArrayList<>();
    private final Map<String, ArrayList<AeaaTaxonomyMapping>> taxonomyMappings = new HashMap<>();
    private final List<String> references = new ArrayList<>();
    private final Map<String, Map<String, String>> referencesData = new HashMap<>();

    public AeaaCapecEntry(String id) {
        setId(id);
    }

    public void setId(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("CAPEC-Entry Id cannot be null or empty");
        } else {
            if (id.startsWith("CAPEC-")) this.id = id;
            else this.id = "CAPEC-" + id;
        }
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("abstraction", this.abstraction)
                .put("status", this.status.toString())
                .put("description", this.description)
                .put("alternateTerms", new JSONObject(this.alternateTerms));

        if (this.likelihoodOfAttack != null) {
            json.put("likelihoodOfAttack", this.likelihoodOfAttack.toString());
        }
        if (this.typicalSeverity != null) {
            json.put("typicalSeverity", this.typicalSeverity.toString());
        }
        json.put("prerequisites", new JSONArray(this.prerequisites));

        final JSONObject relatedAttackPatterns = new JSONObject();
        for (Map.Entry<AeaaMitre.Relation, List<String>> relationEntry : this.relatedAttackPatterns.entrySet()) {
            relatedAttackPatterns.put(relationEntry.getKey().toString(), new JSONArray(relationEntry.getValue()));
        }
        json.put("relatedAttackPatterns", relatedAttackPatterns);

        final JSONArray consequences = new JSONArray();
        this.consequences.forEach(entry -> consequences.put(entry.toJson()));
        json.put("consequences", consequences);
        json.put("mitigations", new JSONArray(this.mitigations));
        json.put("relatedWeaknesses", new JSONArray(this.relatedWeaknesses));

        final JSONObject taxonomyMappings = new JSONObject();
        for (String source : this.taxonomyMappings.keySet()) {
            final JSONArray mappings = new JSONArray();
            this.taxonomyMappings.get(source).forEach(mapping -> mappings.put(mapping.toJson()));
            taxonomyMappings.put(source, mappings);
        }
        json.put("taxonomyMappings", taxonomyMappings);

        final JSONObject referenceData = new JSONObject();
        for (Map.Entry<String, Map<String, String>> referenceEntry : this.referencesData.entrySet()) {
            referenceData.put(referenceEntry.getKey(), new JSONObject(referenceEntry.getValue()));
        }
        json.put("referenceData", referenceData);

        return json;
    }

    public static List<AeaaCapecEntry> fromJson(JSONArray json) {
        final List<AeaaCapecEntry> entries = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            entries.add(AeaaCapecEntry.fromJson(json.getJSONObject(i)));
        }
        return entries;
    }

    public static AeaaCapecEntry fromJson(JSONObject json) {
        final AeaaCapecEntry capecEntry = new AeaaCapecEntry();

        capecEntry.setId(json.optString("id"));
        capecEntry.setName(json.optString("name"));
        capecEntry.setAbstraction(json.optString("abstraction"));
        capecEntry.setStatus(AeaaMitre.Status.of(json.optString("status")));
        capecEntry.setDescription(json.optString("description"));

        final JSONArray jsonArray = json.optJSONArray("prerequisites");
        for (int i = 0; i < jsonArray.length(); i++) {
            capecEntry.getPrerequisites().add(jsonArray.optString(i));
        }

        final JSONObject alternateTerms = json.optJSONObject("alternateTerms");
        for (String key : alternateTerms.keySet()) {
            capecEntry.getAlternateTerms().put(key, alternateTerms.optString(key));
        }
        capecEntry.setLikelihoodOfAttack(AeaaMitre.Severity.of(json.optString("likelihoodOfAttack", "Unknown")));
        capecEntry.setTypicalSeverity(AeaaMitre.Severity.of(json.optString("typicalSeverity", "Unknown")));

        final JSONObject relatedAttackPatterns = json.optJSONObject("relatedAttackPatterns");
        relatedAttackPatterns.keySet().forEach(key -> {
            AeaaMitre.Relation relation = AeaaMitre.Relation.of(key);
            ArrayList<String> capecs = new ArrayList<>();
            relatedAttackPatterns.optJSONArray(key).forEach(capec -> capecs.add(capec.toString()));
            capecEntry.getRelatedAttackPatterns().put(relation, capecs);
        });
        final JSONArray consequences = json.optJSONArray("consequences");
        if (!consequences.isEmpty()) {
            for (int i = 0; i < consequences.length(); i++) {
                capecEntry.getConsequences().add(AeaaWeaknessConsequence.fromJson(consequences.optJSONObject(i)));
            }
        }
        final JSONArray mitigations = json.optJSONArray("mitigations");
        for (int i = 0; i < mitigations.length(); i++) {
            capecEntry.getMitigations().add(mitigations.optString(i));
        }
        final JSONArray relatedWeaknesses = json.optJSONArray("relatedWeaknesses");
        for (int i = 0; i < relatedWeaknesses.length(); i++) {
            capecEntry.getRelatedWeaknesses().add(relatedWeaknesses.optString(i));
        }

        final JSONObject taxonomyMappings = json.optJSONObject("taxonomyMappings");
        for (String taxonomyKey : taxonomyMappings.keySet()) {
            final JSONArray mappings = taxonomyMappings.optJSONArray(taxonomyKey);

            for (int i = 0; i < mappings.length(); i++) {
                Map<String, ArrayList<AeaaTaxonomyMapping>> cweTaxonomyRef = capecEntry.getTaxonomyMappings();
                if (cweTaxonomyRef.containsKey(taxonomyKey)) {
                    cweTaxonomyRef.get(taxonomyKey).add(AeaaTaxonomyMapping.fromJson(mappings.optJSONObject(i)));
                } else {
                    cweTaxonomyRef.put(taxonomyKey, new ArrayList<>());
                    cweTaxonomyRef.get(taxonomyKey).add(AeaaTaxonomyMapping.fromJson(mappings.optJSONObject(i)));
                }
            }
        }

        final JSONArray references = json.optJSONArray("references");
        if (references != null && !references.isEmpty()) {
            for (Object entry : references) {
                capecEntry.getReferences().add(String.valueOf(entry));
            }
        }

        final JSONObject referencesData = json.optJSONObject("referenceData");
        if (referencesData != null && !referencesData.isEmpty()) {
            for (String key : referencesData.keySet()) {
                final Map<String, String> reference = new HashMap<>();
                final JSONObject refObject = referencesData.optJSONObject(key);
                for (String innerKey : refObject.keySet()) {
                    reference.put(innerKey, refObject.optString(innerKey));
                }
                capecEntry.getReferencesData().put(key, reference);
            }
        }

        return capecEntry;
    }
}
