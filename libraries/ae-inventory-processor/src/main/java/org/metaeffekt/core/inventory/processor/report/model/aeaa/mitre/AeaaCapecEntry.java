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
    private final ArrayList<AeaaConsequence> consequences = new ArrayList<>();
    private final ArrayList<String> mitigations = new ArrayList<>();
    private final ArrayList<String> relatedWeaknesses = new ArrayList<>();
    private final List<String> prerequisites = new ArrayList<>();
    private final Map<String, ArrayList<AeaaTaxonomyMapping>> taxonomyMappings = new HashMap<>();
    private final List<String> references = new ArrayList<>();
    private final Map<String, Map <String, String>> referencesData = new HashMap<>();

    private static Map<String, List<String>> parentOfs = new HashMap<>();

    protected static final Set<String> CONVERSION_KEYS_AMB = Collections.unmodifiableSet(
            new HashSet<>());

    protected static final Set<String> CONVERSION_KEYS_MAP = Collections.unmodifiableSet(
            new HashSet<String>() {{
                add("id");
                add("name");
                add("abstraction");
                add("status");
                add("description");
                add("alternateTerms");
                add("likelihoodOfAttack");
                add("typicalSeverity");
                add("relatedAttackPatterns");
                add("consequences");
                add("mitigations");
                add("relatedWeaknesses");
                add("prerequisite");
                add("taxonomyMappings");
                add("references");
            }});

    public void setId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("CAPEC-Entry id cannot be null or empty");
        } else {
            if (id.startsWith("CAPEC-"))
                this.id = id;
            else
                this.id = "CAPEC-" + id;
        }
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("name", getName());
        jsonObject.put("abstraction", getAbstraction());
        jsonObject.put("status", getStatus().toString());
        jsonObject.put("description", getDescription());
        jsonObject.put("alternateTerms", new JSONObject(getAlternateTerms()));
        if (getLikelihoodOfAttack() != null)
            jsonObject.put("likelihoodOfAttack", getLikelihoodOfAttack().toString());
        if (getTypicalSeverity() != null)
            jsonObject.put("typicalSeverity", getTypicalSeverity().toString());
        jsonObject.put("prerequisites", new JSONArray(getPrerequisites()));

        JSONObject relatedAttackPatterns = new JSONObject();
        for (AeaaMitre.Relation relation : getRelatedAttackPatterns().keySet()) {
            JSONArray relationValues = new JSONArray();
            relationValues.putAll(getRelatedAttackPatterns().get(relation));
            relatedAttackPatterns.put(relation.toString(), relationValues);
        }
        jsonObject.put("relatedAttackPatterns", relatedAttackPatterns);
        JSONArray consequences = new JSONArray();
        getConsequences().forEach(entry -> consequences.put(entry.toJson()));
        jsonObject.put("consequences", consequences);
        jsonObject.put("mitigations", new JSONArray(getMitigations()));
        jsonObject.put("relatedWeaknesses", new JSONArray(getRelatedWeaknesses()));

        JSONObject taxonomyMappings = new JSONObject();
        for (String source : getTaxonomyMappings().keySet()) {
            JSONArray mappings = new JSONArray();
            getTaxonomyMappings().get(source).forEach(mapping -> mappings.put(mapping.toJson()));
            taxonomyMappings.put(source, mappings);
        }
        jsonObject.put("taxonomyMappings", taxonomyMappings);
        JSONObject referenceData = new JSONObject();
        getReferencesData().keySet().forEach(key -> referenceData.put(key, new JSONObject(getReferencesData().get(key))));
        jsonObject.put("referenceData", referenceData);;

        return jsonObject;
    }

    public static AeaaCapecEntry fromJson(JSONObject jsonObject) {
        AeaaCapecEntry capecEntry = new AeaaCapecEntry();
        capecEntry.setId(jsonObject.getString("id"));
        capecEntry.setName(jsonObject.getString("name"));
        capecEntry.setAbstraction(jsonObject.getString("abstraction"));
        capecEntry.setStatus(AeaaMitre.Status.of(jsonObject.getString("status")));
        capecEntry.setDescription(jsonObject.getString("description"));

        JSONArray jsonArray = jsonObject.getJSONArray("prerequisites");
        for (int i = 0; i < jsonArray.length(); i++) {
            capecEntry.getPrerequisites().add(jsonArray.getString(i));
        }
        JSONObject alternateTerms = jsonObject.getJSONObject("alternateTerms");
        for (String key : alternateTerms.keySet()) {
            capecEntry.getAlternateTerms().put(key, alternateTerms.getString(key));
        }
        capecEntry.setLikelihoodOfAttack(AeaaMitre.Severity.of(jsonObject.optString("likelihoodOfAttack", "Unknown")));
        capecEntry.setTypicalSeverity(AeaaMitre.Severity.of(jsonObject.optString("typicalSeverity", "Unknown")));

        JSONObject relatedAttackPatterns = jsonObject.getJSONObject("relatedAttackPatterns");
        relatedAttackPatterns.keySet().forEach(key -> {
            AeaaMitre.Relation relation = AeaaMitre.Relation.of(key);
            ArrayList<String> capecs = new ArrayList<>();
            relatedAttackPatterns.optJSONArray(key).forEach(capec -> capecs.add(capec.toString()));
            capecEntry.getRelatedAttackPatterns().put(relation, capecs);
        });
        JSONArray consequences = jsonObject.getJSONArray("consequences");
        if (!consequences.isEmpty()) {
            for (int i = 0; i < consequences.length(); i++) {
                capecEntry.getConsequences().add(AeaaConsequence.fromJson(consequences.optJSONObject(i)));
            }
        }
        JSONArray mitigations = jsonObject.getJSONArray("mitigations");
        for (int i = 0; i < mitigations.length(); i++) {
            capecEntry.getMitigations().add(mitigations.getString(i));
        }
        JSONArray relatedWeaknesses = jsonObject.getJSONArray("relatedWeaknesses");
        for (int i = 0; i < relatedWeaknesses.length(); i++) {
            capecEntry.getRelatedWeaknesses().add(relatedWeaknesses.getString(i));
        }

        JSONObject taxonomyMappings = jsonObject.getJSONObject("taxonomyMappings");
        taxonomyMappings.keySet().forEach(key -> {
            JSONArray mappings = taxonomyMappings.getJSONArray(key);

            for (int i = 0; i < mappings.length(); i++) {
                Map<String, ArrayList<AeaaTaxonomyMapping>> cweTaxonomyRef = capecEntry.getTaxonomyMappings();
                if (cweTaxonomyRef.containsKey(key)) {
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                } else {
                    cweTaxonomyRef.put(key, new ArrayList<>());
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                }
            }
        });

        JSONArray references = jsonObject.optJSONArray("references");
        if (references != null && !references.isEmpty()) {
            references.forEach(entry -> capecEntry.getReferences().add(entry.toString()));
        }

        JSONObject referencesData = jsonObject.optJSONObject("referenceData");
        if (referencesData != null && !referencesData.isEmpty()) {
            referencesData.keySet().forEach(key -> {
                Map<String, String> reference = new HashMap<>();
                JSONObject jsonObject1 = referencesData.optJSONObject(key);
                jsonObject1.keySet().forEach(innerKey -> {
                    reference.put(innerKey, jsonObject1.optString(innerKey));
                });
                capecEntry.getReferencesData().put(key, reference);
            });
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
