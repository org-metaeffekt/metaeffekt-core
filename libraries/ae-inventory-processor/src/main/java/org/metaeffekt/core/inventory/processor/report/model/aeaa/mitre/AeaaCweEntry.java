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
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.WeaknessMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AeaaCweEntry extends AeaaWeakness {

    private String name;
    private String abstraction;
    private String description;

    private AeaaMitre.Status status;
    private AeaaMitre.Severity likelihoodOfExploit;

    private final List<String> extendedDescription = new ArrayList<>();

    private final List<Map<String, String>> modesOfIntroduction = new ArrayList<>();
    private final List<AeaaWeaknessConsequence> consequences = new ArrayList<>();
    private final List<Map<String, String>> detectionMethods = new ArrayList<>();
    private final List<Map<String, String>> mitigations = new ArrayList<>();

    private final Map<String, String> alternateTerms = new HashMap<>();
    private final Map<AeaaMitre.Relation, List<String>> relatedWeaknesses = new HashMap<>();
    private final Map<String, List<Map<String, String>>> applicablePlatforms = new HashMap<>();
    private final Map<String, Map<String, String>> referencesData = new HashMap<>();
    private final Map<String, List<AeaaTaxonomyMapping>> taxonomyMappings = new HashMap<>();

    public AeaaCweEntry(String id) {
        setId(id);
    }

    @Override
    public AeaaCweEntry setId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("CWE-Entry id cannot be null or empty");
        } else {
            if (id.startsWith("CWE-")) this.id = id;
            else this.id = "CWE-" + id;
        }
        return this;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("abstraction", this.abstraction)
                .put("description", this.description)
                .put("extendedDescription", new JSONArray(this.extendedDescription))
                .put("alternateTerms", new JSONObject(this.alternateTerms));

        if (getStatus() != null) json.put("status", getStatus().toString());

        JSONObject relatedWeaknesses = new JSONObject();
        for (AeaaMitre.Relation relation : this.relatedWeaknesses.keySet()) {
            relatedWeaknesses.put(relation.toString(), new JSONArray(this.relatedWeaknesses.get(relation)));
        }
        json.put("relatedWeaknesses", relatedWeaknesses);

        JSONObject applicablePlatformsJson = new JSONObject();
        this.applicablePlatforms.forEach((key, value) -> {
            JSONArray platforms = new JSONArray();
            value.forEach(entry -> platforms.put(new JSONObject(entry)));
            applicablePlatformsJson.put(key, platforms);
        });
        json.put("applicablePlatforms", applicablePlatformsJson);

        JSONArray modesOfIntroductionJsonArray = new JSONArray();
        this.modesOfIntroduction.forEach(entry -> modesOfIntroductionJsonArray.put(new JSONObject(entry)));
        json.put("modesOfIntroduction", modesOfIntroductionJsonArray);

        if (this.likelihoodOfExploit != null)
            json.put("likelihoodOfExploit", this.likelihoodOfExploit.toString());

        JSONArray consequences = new JSONArray();
        this.consequences.forEach(entry -> consequences.put(entry.toJson()));
        json.put("consequences", consequences);

        JSONArray detectionMethods = new JSONArray();
        this.getDetectionMethods().forEach(detectionMethods::put);
        json.put("detectionMethods", detectionMethods);

        JSONArray mitigationJsonArray = new JSONArray();
        this.getMitigations().forEach(mitigation -> mitigationJsonArray.put(new JSONObject(mitigation)));
        json.put("mitigations", mitigationJsonArray);

        JSONObject taxonomyMappings = new JSONObject();
        for (String source : this.taxonomyMappings.keySet()) {
            JSONArray mappings = new JSONArray();
            this.taxonomyMappings.get(source).forEach(mapping -> mappings.put(mapping.toJson()));
            taxonomyMappings.put(source, mappings);
        }
        json.put("taxonomyMappings", taxonomyMappings);

        json.put("relatedCapecs", new JSONArray(this.referencedThreats.get(AeaaThreatTypeStore.CAPEC)));

        JSONObject referenceData = new JSONObject();
        this.referencesData.keySet().forEach(key -> referenceData.put(key, new JSONObject(this.referencesData.get(key))));
        json.put("referenceData", referenceData);

        return json;
    }

    public static List<AeaaWeakness> fromJson(JSONArray json) {
        final List<AeaaWeakness> entries = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            entries.add(AeaaCweEntry.fromJson(json.getJSONObject(i)));
        }
        return entries;
    }

    public static AeaaCweEntry fromJson(JSONObject jsonObject) {
        AeaaCweEntry cweEntry = new AeaaCweEntry();
        cweEntry.fromJsonInternal(jsonObject);
        return cweEntry;
    }

    private void fromJsonInternal(JSONObject jsonObject) {
        this.setId(jsonObject.optString("id"));
        this.setName(jsonObject.optString("name"));
        this.setAbstraction(jsonObject.optString("abstraction"));
        this.setStatus(AeaaMitre.Status.of(jsonObject.optString("status", "Unknown")));
        this.setDescription(jsonObject.optString("description"));

        JSONArray extendedDescription = jsonObject.optJSONArray("extendedDescription");
        if (extendedDescription != null) {
            for (int i = 0; i < extendedDescription.length(); i++) {
                this.getExtendedDescription().add(extendedDescription.optString(i));
            }
        }

        JSONObject relatedWeaknesses = jsonObject.optJSONObject("relatedWeaknesses");
        relatedWeaknesses.keySet().forEach(key -> {
            AeaaMitre.Relation relation = AeaaMitre.Relation.of(key);
            ArrayList<String> weaknesses = new ArrayList<>();
            relatedWeaknesses.optJSONArray(key).forEach(weakness -> weaknesses.add(weakness.toString()));
            this.getRelatedWeaknesses().put(relation, weaknesses);
        });

        JSONObject applicablePlatformsJsonObject = jsonObject.optJSONObject("applicablePlatforms", new JSONObject());
        for (String applicableTypeKey : applicablePlatformsJsonObject.keySet()) {
            JSONArray jsonArray = applicablePlatformsJsonObject.optJSONArray(applicableTypeKey);
            List<Map<String, String>> platforms = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObjectEntries = jsonArray.optJSONObject(i);
                HashMap<String, String> platform = new HashMap<>();
                jsonObjectEntries.keySet().forEach(keyy -> {
                    platform.put(keyy, jsonObjectEntries.optString(keyy));
                });
                platforms.add(platform);
            }
            this.getApplicablePlatforms().put(applicableTypeKey, platforms);
        }

        JSONObject alternateTerms = jsonObject.optJSONObject("alternateTerms");
        for (String key : alternateTerms.keySet()) {
            this.getAlternateTerms().put(key, alternateTerms.optString(key));
        }

        JSONArray modesOfIntroductionJsonArray = jsonObject.optJSONArray("modesOfIntroduction");
        for (int i = 0; i < modesOfIntroductionJsonArray.length(); i++) {
            JSONObject modesOfIntroductionJsonArrayEntry = modesOfIntroductionJsonArray.optJSONObject(i);
            Map<String, String> modesOfIntroduction = new HashMap<>();
            modesOfIntroductionJsonArrayEntry.keySet().forEach(key -> modesOfIntroduction.put(key, modesOfIntroductionJsonArrayEntry.optString(key)));
            this.getModesOfIntroduction().add(modesOfIntroduction);
        }

        if (jsonObject.optString("likelihoodOfExploit") != null)
            this.setLikelihoodOfExploit(AeaaMitre.Severity.of(jsonObject.optString("likelihoodOfExploit", "Unknown")));

        JSONArray consequences = jsonObject.optJSONArray("consequences");
        if (!consequences.isEmpty()) {
            for (int i = 0; i < consequences.length(); i++) {
                this.getConsequences().add(AeaaWeaknessConsequence.fromJson(consequences.optJSONObject(i)));
            }
        }
        JSONArray detectionMethods = jsonObject.optJSONArray("detectionMethods");
        for (int i = 0; i < detectionMethods.length(); i++) {
            Map<String, String> detectionMethod = new HashMap<>();
            JSONObject detectionMethodEntry = detectionMethods.optJSONObject(i);
            detectionMethodEntry.keySet().forEach(key -> {
                detectionMethod.put(key, detectionMethodEntry.optString(key));
            });
            this.getDetectionMethods().add(detectionMethod);
        }

        JSONArray mitigations = jsonObject.optJSONArray("mitigations");
        for (int i = 0; i < mitigations.length(); i++) {
            Map<String, String> mitigation = new HashMap<>();
            JSONObject mitigationEntry = mitigations.optJSONObject(i);
            mitigationEntry.keySet().forEach(key -> mitigation.put(key, mitigationEntry.optString(key)));
            this.getMitigations().add(mitigation);
        }

        JSONObject taxonomyMappings = jsonObject.optJSONObject("taxonomyMappings");
        taxonomyMappings.keySet().forEach(key -> {
            JSONArray mappings = taxonomyMappings.optJSONArray(key);

            Map<String, List<AeaaTaxonomyMapping>> cweTaxonomyRef = this.getTaxonomyMappings();
            for (int i = 0; i < mappings.length(); i++) {
                if (cweTaxonomyRef.containsKey(key)) {
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.optJSONObject(i)));
                } else {
                    cweTaxonomyRef.put(key, new ArrayList<>());
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.optJSONObject(i)));
                }
            }
        });


        JSONArray relatedCapecs = jsonObject.optJSONArray("relatedCapecs");
        if (!relatedCapecs.isEmpty()) {
            relatedCapecs.forEach(jsonEntry -> this.addReferencedThreatId(AeaaThreatTypeStore.CAPEC, jsonEntry.toString()));
        }

        JSONObject referencesData = jsonObject.optJSONObject("referenceData");
        if (referencesData != null && !referencesData.isEmpty()) {
            referencesData.keySet().forEach(key -> {
                Map<String, String> reference = new HashMap<>();
                JSONObject jsonObject1 = referencesData.optJSONObject(key);
                jsonObject1.keySet().forEach(innerKey -> {
                    reference.put(innerKey, jsonObject1.optString(innerKey));
                });
                this.getReferencesData().put(key, reference);
            });
        }
    }

    @Override
    public AeaaThreatTypeIdentifier<AeaaCweEntry> getSourceIdentifier() {
        return AeaaThreatTypeStore.CWE;
    }

    public void appendFromBaseModel(WeaknessMetaData wmd) {
        super.appendFromBaseModel(wmd);

        this.fromJsonInternal(new JSONObject(wmd.get(WeaknessMetaData.Attribute.CONTENT)));
    }

    @Override
    public void appendToBaseModel(WeaknessMetaData modelBase) {
        super.appendToBaseModel(modelBase);

        modelBase.set(WeaknessMetaData.Attribute.CONTENT, String.valueOf(this.toJson()));
    }

    @Override
    protected JSONObject toReferencedWeaknesses() {
        return getSourceIdentifier().toJson().put("id", this.id);
    }

    @Override
    public String toString() {
        return super.id;
    }
}
