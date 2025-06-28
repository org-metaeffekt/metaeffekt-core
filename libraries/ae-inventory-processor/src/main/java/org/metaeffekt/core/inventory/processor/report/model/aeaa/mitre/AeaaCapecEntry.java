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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

@Getter
@Setter
@Slf4j
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

    // FIXME-JKO: Why a static store for parent relations?
    //  will successive parsings of entries result in different data being parsed or is this simply a cache?
    //  also, it's never used.
    private static Map<String, List<String>> parentOfs = new HashMap<>();

    public void setId(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("CAPEC-Entry Id cannot be null or empty");
        } else {
            if (id.startsWith("CAPEC-")) this.id = id;
            else this.id = "CAPEC-" + id;
        }
    }

    public JSONObject toJson() {
        // FIXME-JKO: Please do not use all the getters, they cause an overhead of having to call a method for each value.
        //  just access the values via this.id, etc.
        final JSONObject json = new JSONObject()
                .put("id", getId())
                .put("name", getName())
                .put("abstraction", getAbstraction())
                .put("status", getStatus().toString())
                .put("description", getDescription())
                .put("alternateTerms", new JSONObject(getAlternateTerms()));

        if (getLikelihoodOfAttack() != null) {
            json.put("likelihoodOfAttack", getLikelihoodOfAttack().toString());
        }
        if (getTypicalSeverity() != null) {
            json.put("typicalSeverity", getTypicalSeverity().toString());
        }
        json.put("prerequisites", new JSONArray(getPrerequisites()));

        final JSONObject relatedAttackPatterns = new JSONObject();
        for (Map.Entry<AeaaMitre.Relation, List<String>> relationEntry : getRelatedAttackPatterns().entrySet()) {
            relatedAttackPatterns.put(relationEntry.getKey().toString(), new JSONArray(relationEntry.getValue()));
        }
        json.put("relatedAttackPatterns", relatedAttackPatterns);

        final JSONArray consequences = new JSONArray();
        getConsequences().forEach(entry -> consequences.put(entry.toJson()));
        json.put("consequences", consequences);
        json.put("mitigations", new JSONArray(getMitigations()));
        json.put("relatedWeaknesses", new JSONArray(getRelatedWeaknesses()));

        final JSONObject taxonomyMappings = new JSONObject();
        for (String source : getTaxonomyMappings().keySet()) {
            final JSONArray mappings = new JSONArray();
            getTaxonomyMappings().get(source).forEach(mapping -> mappings.put(mapping.toJson()));
            taxonomyMappings.put(source, mappings);
        }
        json.put("taxonomyMappings", taxonomyMappings);

        final JSONObject referenceData = new JSONObject();
        for (Map.Entry<String, Map<String, String>> referenceEntry : getReferencesData().entrySet()) {
            referenceData.put(referenceEntry.getKey(), new JSONObject(referenceEntry.getValue()));
        }
        json.put("referenceData", referenceData);

        return json;
    }

    public static AeaaCapecEntry fromJson(JSONObject json) {
        final AeaaCapecEntry capecEntry = new AeaaCapecEntry();

        // FIXME-JKO: Are you sure the values ALWAYS exist on the JSON Object? Otherwise you must use the opt-versions of the methods, e.g. json.optString("name", null)
        capecEntry.setId(json.getString("id"));
        capecEntry.setName(json.getString("name"));
        capecEntry.setAbstraction(json.getString("abstraction"));
        capecEntry.setStatus(AeaaMitre.Status.of(json.getString("status")));
        capecEntry.setDescription(json.getString("description"));

        final JSONArray jsonArray = json.getJSONArray("prerequisites");
        for (int i = 0; i < jsonArray.length(); i++) {
            capecEntry.getPrerequisites().add(jsonArray.getString(i));
        }

        final JSONObject alternateTerms = json.getJSONObject("alternateTerms");
        for (String key : alternateTerms.keySet()) {
            capecEntry.getAlternateTerms().put(key, alternateTerms.getString(key));
        }
        capecEntry.setLikelihoodOfAttack(AeaaMitre.Severity.of(json.optString("likelihoodOfAttack", "Unknown")));
        capecEntry.setTypicalSeverity(AeaaMitre.Severity.of(json.optString("typicalSeverity", "Unknown")));

        final JSONObject relatedAttackPatterns = json.getJSONObject("relatedAttackPatterns");
        relatedAttackPatterns.keySet().forEach(key -> {
            AeaaMitre.Relation relation = AeaaMitre.Relation.of(key);
            ArrayList<String> capecs = new ArrayList<>();
            relatedAttackPatterns.optJSONArray(key).forEach(capec -> capecs.add(capec.toString()));
            capecEntry.getRelatedAttackPatterns().put(relation, capecs);
        });
        final JSONArray consequences = json.getJSONArray("consequences");
        if (!consequences.isEmpty()) {
            for (int i = 0; i < consequences.length(); i++) {
                capecEntry.getConsequences().add(AeaaWeaknessConsequence.fromJson(consequences.optJSONObject(i)));
            }
        }
        final JSONArray mitigations = json.getJSONArray("mitigations");
        for (int i = 0; i < mitigations.length(); i++) {
            capecEntry.getMitigations().add(mitigations.getString(i));
        }
        final JSONArray relatedWeaknesses = json.getJSONArray("relatedWeaknesses");
        for (int i = 0; i < relatedWeaknesses.length(); i++) {
            capecEntry.getRelatedWeaknesses().add(relatedWeaknesses.getString(i));
        }

        final JSONObject taxonomyMappings = json.getJSONObject("taxonomyMappings");
        for (String taxonomyKey : taxonomyMappings.keySet()) {
            final JSONArray mappings = taxonomyMappings.getJSONArray(taxonomyKey);

            for (int i = 0; i < mappings.length(); i++) {
                Map<String, ArrayList<AeaaTaxonomyMapping>> cweTaxonomyRef = capecEntry.getTaxonomyMappings();
                if (cweTaxonomyRef.containsKey(taxonomyKey)) {
                    cweTaxonomyRef.get(taxonomyKey).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                } else {
                    cweTaxonomyRef.put(taxonomyKey, new ArrayList<>());
                    cweTaxonomyRef.get(taxonomyKey).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                }
            }
        }

        final JSONArray references = json.optJSONArray("references");
        if (references != null && !references.isEmpty()) {
            for (Object entry : references) {
                // FIXME-JKO: String.valueOf is safer than .toString() for null cases, either that or check it explicitly
                capecEntry.getReferences().add(String.valueOf(entry));
            }
        }

        final JSONObject referencesData = json.optJSONObject("referenceData");
        if (referencesData != null && !referencesData.isEmpty()) {
            for (String key : referencesData.keySet()) {
                final Map<String, String> reference = new HashMap<>();
                // FIXME-JKO: I'm not gonna change it now to make a point, but jsonObject1 is only one of the few examples of a var name that could really be better set.
                final JSONObject jsonObject1 = referencesData.optJSONObject(key);
                for (String innerKey : jsonObject1.keySet()) {
                    reference.put(innerKey, jsonObject1.optString(innerKey));
                }
                capecEntry.getReferencesData().put(key, reference);
            }
        }

        return capecEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AeaaCapecEntry that = (AeaaCapecEntry) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(abstraction, that.abstraction) &&
                status == that.status &&
                Objects.equals(description, that.description) &&
                Objects.equals(alternateTerms, that.alternateTerms) &&
                likelihoodOfAttack == that.likelihoodOfAttack &&
                typicalSeverity == that.typicalSeverity &&
                Objects.equals(relatedAttackPatterns, that.relatedAttackPatterns) &&
                Objects.equals(consequences, that.consequences) &&
                Objects.equals(mitigations, that.mitigations) &&
                Objects.equals(relatedWeaknesses, that.relatedWeaknesses) &&
                Objects.equals(prerequisites, that.prerequisites) &&
                Objects.equals(taxonomyMappings, that.taxonomyMappings) &&
                Objects.equals(references, that.references);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, abstraction, status, description, alternateTerms, likelihoodOfAttack, typicalSeverity, relatedAttackPatterns, consequences, mitigations, relatedWeaknesses, prerequisites, taxonomyMappings, references);
    }
}
