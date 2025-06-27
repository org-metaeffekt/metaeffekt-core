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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class AeaaCweEntry {

    private String id;
    private String name;
    private String abstraction;
    private AeaaMitre.Status status;
    private String description;
    private final Map<AeaaMitre.Relation, List<String>> relatedWeaknesses = new HashMap<>();
    private final Map<String, List<Map<String, String>>> applicablePlatforms = new HashMap<>();
    private final Map<String, String> alternateTerms = new HashMap<>();
    private final List<Map<String, String>> modesOfIntroduction = new ArrayList<>();
    private AeaaMitre.Severity likelihoodOfExploit;
    private final List<AeaaConsequence> consequences = new ArrayList<>();
    private final List<Map<String, String>> detectionMethods = new ArrayList<>();
    private final List<Map<String, String>> mitigations = new ArrayList<>();
    private final Map<String, ArrayList<AeaaTaxonomyMapping>> taxonomyMappings = new HashMap<>();
    private final List<String> relatedCapecs = new ArrayList<>();
    private final Map<String, Map <String, String>> referencesData = new HashMap<>();

    private static Map<String, List<String>> parentOfs = new ConcurrentHashMap<>();

    protected static final Set<String> CONVERSION_KEYS_AMB = Collections.unmodifiableSet(new HashSet<>());

    protected static final Set<String> CONVERSION_KEYS_MAP = Collections.unmodifiableSet(new HashSet<String>() {{
        add("id");
        add("name");
        add("abstraction");
        add("status");
        add("description");
        add("relatedWeaknesses");
        add("applicablePlatforms");
        add("alternateTerms");
        add("modesOfIntroduction");
        add("likelihoodOfExploit");
        add("consequences");
        add("detectionMethods");
        add("mitigations");
        add("taxonomyMappings");
        add("relatedAttackPatterns");
        add("referenceData");
    }});


    public AeaaCweEntry() {
    }

    public AeaaCweEntry(String id) {
        setId(id);
    }

    public void setId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("CWE-Entry id cannot be null or empty");
        } else {
            if (id.startsWith("CWE-"))
                this.id = id;
            else
                this.id = "CWE-" + id;
        }
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("id", getId());
        jsonObject.put("name", getName());
        jsonObject.put("abstraction", getAbstraction());
        if (getStatus() != null)
            jsonObject.put("status", getStatus().toString());
        jsonObject.put("description", getDescription());

        JSONObject relatedWeaknesses = new JSONObject();
        for (AeaaMitre.Relation relation : getRelatedWeaknesses().keySet()) {
            relatedWeaknesses.put(relation.toString(), new JSONArray(getRelatedWeaknesses().get(relation)));
        }
        jsonObject.put("relatedWeaknesses", relatedWeaknesses);

        JSONObject applicablePlatformsJson = new JSONObject();
        getApplicablePlatforms().forEach((key, value) -> {
            JSONArray platforms = new JSONArray();
            value.forEach(entry -> platforms.put(new JSONObject(entry)));
            applicablePlatformsJson.put(key, platforms);
        });
        jsonObject.put("applicablePlatforms", applicablePlatformsJson);

        jsonObject.put("alternateTerms", new JSONObject(getAlternateTerms()));

        JSONArray modesOfIntroductionJsonArray = new JSONArray();
        getModesOfIntroduction().forEach(entry -> modesOfIntroductionJsonArray.put(new JSONObject(entry)));
        jsonObject.put("modesOfIntroduction", modesOfIntroductionJsonArray);

        if (getLikelihoodOfExploit() != null)
            jsonObject.put("likelihoodOfExploit", getLikelihoodOfExploit().toString());

        JSONArray consequences = new JSONArray();
        getConsequences().forEach(entry -> consequences.put(entry.toJson()));
        jsonObject.put("consequences", consequences);

        JSONArray detectionMethods = new JSONArray();
        getDetectionMethods().forEach(detectionMethods::put);
        jsonObject.put("detectionMethods", detectionMethods);

        JSONArray mitigationJsonArray = new JSONArray();
        getMitigations().forEach(mitigation -> mitigationJsonArray.put(new JSONObject(mitigation)));
        jsonObject.put("mitigations", mitigationJsonArray);

        JSONObject taxonomyMappings = new JSONObject();
        for (String source : getTaxonomyMappings().keySet()) {
            JSONArray mappings = new JSONArray();
            getTaxonomyMappings().get(source).forEach(mapping -> mappings.put(mapping.toJson()));
            taxonomyMappings.put(source, mappings);
        }
        jsonObject.put("taxonomyMappings", taxonomyMappings);

        jsonObject.put("relatedCapecs", new JSONArray(getRelatedCapecs()));

        JSONObject referenceData = new JSONObject();
        getReferencesData().keySet().forEach(key -> referenceData.put(key, new JSONObject(getReferencesData().get(key))));
        jsonObject.put("referenceData", referenceData);

        return jsonObject;
    }

    public static AeaaCweEntry fromJson(JSONObject jsonObject) {
        AeaaCweEntry cweEntry = new AeaaCweEntry();

        cweEntry.setId(jsonObject.optString("id"));
        cweEntry.setName(jsonObject.optString("name"));
        cweEntry.setAbstraction(jsonObject.optString("abstraction"));
        cweEntry.setStatus(AeaaMitre.Status.of(jsonObject.optString("status", "Unknown")));
        cweEntry.setDescription(jsonObject.optString("description"));

        JSONObject relatedWeaknesses = jsonObject.getJSONObject("relatedWeaknesses");
        relatedWeaknesses.keySet().forEach(key -> {
            AeaaMitre.Relation relation = AeaaMitre.Relation.of(key);
            ArrayList<String> weaknesses = new ArrayList<>();
            relatedWeaknesses.optJSONArray(key).forEach(weakness -> weaknesses.add(weakness.toString()));
            cweEntry.getRelatedWeaknesses().put(relation, weaknesses);
        });

        JSONObject applicablePlatformsJsonObject = jsonObject.optJSONObject("applicablePlatforms", new JSONObject());
        for (String applicableTypeKey : applicablePlatformsJsonObject.keySet()) {
            JSONArray jsonArray = applicablePlatformsJsonObject.optJSONArray(applicableTypeKey);
            List<Map<String, String>> platforms = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObjectEntries = jsonArray.getJSONObject(i);
                HashMap<String, String> platform = new HashMap<>();
                jsonObjectEntries.keySet().forEach(keyy -> {
                    platform.put(keyy, jsonObjectEntries.optString(keyy));
                });
                platforms.add(platform);
            }
            cweEntry.getApplicablePlatforms().put(applicableTypeKey, platforms);
        }

        JSONObject alternateTerms = jsonObject.getJSONObject("alternateTerms");
        for (String key : alternateTerms.keySet()) {
            cweEntry.getAlternateTerms().put(key, alternateTerms.optString(key));
        }

        JSONArray modesOfIntroductionJsonArray = jsonObject.optJSONArray("modesOfIntroduction");
        for (int i = 0; i < modesOfIntroductionJsonArray.length(); i++) {
            JSONObject modesOfIntroductionJsonArrayEntry = modesOfIntroductionJsonArray.getJSONObject(i);
            Map<String, String> modesOfIntroduction = new HashMap<>();
            modesOfIntroductionJsonArrayEntry.keySet().forEach(key -> modesOfIntroduction.put(key, modesOfIntroductionJsonArrayEntry.optString(key)));
            cweEntry.getModesOfIntroduction().add(modesOfIntroduction);
        }

        if (jsonObject.optString("likelihoodOfExploit") != null)
            cweEntry.setLikelihoodOfExploit(AeaaMitre.Severity.of(jsonObject.optString("likelihoodOfExploit", "Unknown")));

        JSONArray consequences = jsonObject.optJSONArray("consequences");
        if (!consequences.isEmpty()) {
            for (int i = 0; i < consequences.length(); i++) {
                cweEntry.getConsequences().add(AeaaConsequence.fromJson(consequences.optJSONObject(i)));
            }
        }
        JSONArray detectionMethods = jsonObject.optJSONArray("detectionMethods");
        for (int i = 0; i < detectionMethods.length(); i++) {
            Map<String, String> detectionMethod = new HashMap<>();
            JSONObject detectionMethodEntry = detectionMethods.getJSONObject(i);
            detectionMethodEntry.keySet().forEach(key -> {
                detectionMethod.put(key, detectionMethodEntry.optString(key));
            });
            cweEntry.getDetectionMethods().add(detectionMethod);
        }

        JSONArray mitigations = jsonObject.optJSONArray("mitigations");
        for (int i = 0; i < mitigations.length(); i++) {
            Map<String, String> mitigation = new HashMap<>();
            JSONObject mitigationEntry = mitigations.getJSONObject(i);
            mitigationEntry.keySet().forEach(key -> mitigation.put(key, mitigationEntry.optString(key)));
            cweEntry.getMitigations().add(mitigation);
        }

        JSONObject taxonomyMappings = jsonObject.getJSONObject("taxonomyMappings");
        taxonomyMappings.keySet().forEach(key -> {
            JSONArray mappings = taxonomyMappings.optJSONArray(key);

            Map<String, ArrayList<AeaaTaxonomyMapping>> cweTaxonomyRef = cweEntry.getTaxonomyMappings();
            for (int i = 0; i < mappings.length(); i++) {
                if (cweTaxonomyRef.containsKey(key)) {
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                } else {
                    cweTaxonomyRef.put(key, new ArrayList<>());
                    cweTaxonomyRef.get(key).add(AeaaTaxonomyMapping.fromJson(mappings.getJSONObject(i)));
                }
            }
        });


        JSONArray relatedCapecs = jsonObject.optJSONArray("relatedCapecs");
        if (!relatedCapecs.isEmpty()) {
            relatedCapecs.forEach(entry -> cweEntry.getRelatedCapecs().add(entry.toString()));
        }

        JSONObject referencesData = jsonObject.optJSONObject("referenceData");
        if (referencesData != null && !referencesData.isEmpty()) {
            referencesData.keySet().forEach(key -> {
                Map<String, String> reference = new HashMap<>();
                JSONObject jsonObject1 = referencesData.optJSONObject(key);
                jsonObject1.keySet().forEach(innerKey -> {
                    reference.put(innerKey, jsonObject1.optString(innerKey));
                });
                cweEntry.getReferencesData().put(key, reference);
            });
        }

        return cweEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AeaaCweEntry that = (AeaaCweEntry) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(abstraction, that.abstraction) &&
                status == that.status &&
                Objects.equals(description, that.description) &&
                Objects.equals(relatedWeaknesses, that.relatedWeaknesses) &&
                Objects.equals(applicablePlatforms, that.applicablePlatforms) &&
                Objects.equals(alternateTerms, that.alternateTerms) &&
                Objects.equals(modesOfIntroduction, that.modesOfIntroduction) &&
                Objects.equals(likelihoodOfExploit, that.likelihoodOfExploit) &&
                Objects.equals(consequences, that.consequences) &&
                Objects.equals(detectionMethods, that.detectionMethods) &&
                Objects.equals(mitigations, that.mitigations) &&
                Objects.equals(taxonomyMappings, that.taxonomyMappings) &&
                Objects.equals(relatedCapecs, that.relatedCapecs) &&
                Objects.equals(referencesData, that.referencesData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, abstraction, status, description, alternateTerms, consequences, mitigations, relatedWeaknesses, taxonomyMappings, referencesData);
    }
}
