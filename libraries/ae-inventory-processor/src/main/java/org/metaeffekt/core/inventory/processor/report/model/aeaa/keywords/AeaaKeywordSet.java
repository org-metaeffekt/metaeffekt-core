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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.keywords;

import lombok.Setter;
import lombok.ToString;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.assessment.AeaaVulnerabilityAssessmentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Setter
@ToString
public class AeaaKeywordSet implements Comparable<AeaaKeywordSet> {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaKeywordSet.class);

    private Double score;
    private String name;
    private String category;
    private String notes;
    private List<AeaaVulnerabilityAssessmentEvent> status = new ArrayList<>();

    private final List<AeaaKeywordTokenList> mustContainAll = new ArrayList<>();
    private final List<AeaaKeywordTokenList> mustNotContain = new ArrayList<>();
    private final Map<List<AeaaKeywordTokenList>, Integer> minContain = new HashMap<>();
    private final Map<List<AeaaKeywordTokenList>, Integer> maxContain = new HashMap<>();

    public AeaaKeywordSet(String name) {
        this.name = name;
    }

    public AeaaKeywordSet() {
    }

    public List<AeaaKeywordTokenList> getMustContainAll() {
        return mustContainAll;
    }

    public List<AeaaKeywordTokenList> getMustNotContain() {
        return mustNotContain;
    }

    public Map<List<AeaaKeywordTokenList>, Integer> getMaxContain() {
        return maxContain;
    }

    public Map<List<AeaaKeywordTokenList>, Integer> getMinContain() {
        return minContain;
    }

    public String getNotes() {
        return notes;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public Double getScore() {
        return score;
    }

    public boolean hasScore() {
        return score != null;
    }

    public String getNameScore() {
        if (score == null) return name;

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (");

        if (score < 0) {
            sb.append("-");
        } else if (score > 0) {
            sb.append("+");
        }
        sb.append(score).append(")");

        return sb.toString();
    }

    public JSONObject toReducedInformationJson() {
        final JSONObject exportJson = new JSONObject();
        if (score != null) {
            exportJson.put(KEY_SCORE, score);
        }
        if (name != null) {
            exportJson.put(KEY_NAME, name);
        }
        if (category != null) {
            exportJson.put(KEY_CATEGORY, category);
        }
        if (notes != null) {
            exportJson.put(KEY_NOTES, notes);
        }
        return exportJson;
    }

    public JSONObject toFullInformationJson() {
        final JSONObject exportJson = toReducedInformationJson();

        exportJson.put(KEY_MUST_CONTAIN_ALL, mustContainAll.stream().map(AeaaKeywordTokenList::toJson).collect(Collectors.toList()));
        exportJson.put(KEY_MUST_NOT_CONTAIN, mustNotContain.stream().map(AeaaKeywordTokenList::toJson).collect(Collectors.toList()));

        final JSONArray minJson = new JSONArray();
        for (Map.Entry<List<AeaaKeywordTokenList>, Integer> entry : minContain.entrySet()) {
            final JSONObject minEntry = new JSONObject();
            minEntry.put(KEY_KEYWORDS, entry.getKey().stream().map(AeaaKeywordTokenList::toJson).collect(Collectors.toList()));
            minEntry.put(KEY_COUNT, entry.getValue());
            minJson.put(minEntry);
        }
        exportJson.put(KEY_MIN, minJson);

        final JSONArray maxJson = new JSONArray();
        for (Map.Entry<List<AeaaKeywordTokenList>, Integer> entry : maxContain.entrySet()) {
            final JSONObject maxEntry = new JSONObject();
            maxEntry.put(KEY_KEYWORDS, entry.getKey().stream().map(AeaaKeywordTokenList::toJson).collect(Collectors.toList()));
            maxEntry.put(KEY_COUNT, entry.getValue());
            maxJson.put(maxEntry);
        }
        exportJson.put(KEY_MAX, maxJson);

        if (status == null) {
            this.status = new ArrayList<>();
        }
        exportJson.put(KEY_STATUS, status.stream().map(AeaaVulnerabilityAssessmentEvent::toJsonObject).collect(Collectors.toList()));

        return exportJson;
    }

    public static AeaaVulnerabilityKeywords fromJson(JSONArray json) {
        final List<AeaaKeywordSet> keywordSets = new ArrayList<>();
        for (Object o : json) {
            if (o instanceof JSONObject) {
                keywordSets.add(AeaaKeywordSet.fromJson((JSONObject) o));
            }
        }
        return new AeaaVulnerabilityKeywords(keywordSets);
    }

    public static AeaaKeywordSet fromJson(JSONObject json) {
        return new AeaaKeywordSet().setProperties(json.toMap());
    }

    public static AeaaKeywordSet fromMap(Map<String, Object> properties) {
        return new AeaaKeywordSet().setProperties(properties);
    }

    public AeaaKeywordSet setProperties(Map<String, Object> properties) {
        if (notEmpty(properties.get(KEY_SCORE))) {
            final Object scoreObject = properties.get(KEY_SCORE);
            if (scoreObject instanceof Number) {
                score = ((Number) scoreObject).doubleValue();
            } else if (scoreObject instanceof String) {
                try {
                    score = Double.parseDouble((String) scoreObject);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (notEmpty(properties.get(KEY_NAME))) {
            name = (String) properties.get(KEY_NAME);
        }
        if (notEmpty(properties.get(KEY_CATEGORY))) {
            category = (String) properties.get(KEY_CATEGORY);
        }
        if (notEmpty(properties.get(KEY_NOTES))) {
            notes = (String) properties.get(KEY_NOTES);
        }

        parseAllNoneTokensFromToJson(properties, KEY_MUST_CONTAIN_ALL, mustContainAll);
        parseAllNoneTokensFromToJson(properties, KEY_MUST_NOT_CONTAIN, mustNotContain);

        parseMinMaxTokensFromToJson(properties, KEY_MIN, minContain);
        parseMinMaxTokensFromToJson(properties, KEY_MAX, maxContain);

        if (properties.containsKey(KEY_STATUS)) {
            final Object statusObject = properties.get(KEY_STATUS);
            if (statusObject instanceof List) {
                final List<Object> statusList = (List<Object>) statusObject;
                for (Object entry : statusList) {
                    if (entry instanceof Map) {
                        final Map<String, Object> statusMap = (Map<String, Object>) entry;
                        status.add(AeaaVulnerabilityAssessmentEvent.fromMap(statusMap));
                    }
                }
            }
        }

        return this;
    }

    private void parseAllNoneTokensFromToJson(Map<String, Object> properties, String keyMustContainAll, List<AeaaKeywordTokenList> mustContainAll) {
        if (properties.containsKey(keyMustContainAll)) {
            final Object mustContainAllObject = properties.get(keyMustContainAll);
            if (mustContainAllObject instanceof List) {
                final List<Object> mustContainAllList = (List<Object>) mustContainAllObject;
                for (Object entry : mustContainAllList) {
                    if (entry instanceof List) {
                        final List<String> keywords = (List<String>) entry;
                        mustContainAll.add(new AeaaKeywordTokenList(keywords));
                    }
                }
            }
        }
    }

    private void parseMinMaxTokensFromToJson(Map<String, Object> properties, String keyMax, Map<List<AeaaKeywordTokenList>, Integer> maxContain) {
        if (properties.containsKey(keyMax)) {
            final Object maxObject = properties.get(keyMax);
            if (maxObject instanceof List) {
                final List<Object> maxList = (List<Object>) maxObject;
                for (Object entry : maxList) {
                    if (entry instanceof Map) {
                        final Map<String, Object> maxEntry = (Map<String, Object>) entry;
                        if (maxEntry.containsKey(KEY_KEYWORDS) && maxEntry.containsKey(KEY_COUNT)) {
                            final List<List<String>> keywords = (List<List<String>>) maxEntry.get(KEY_KEYWORDS);
                            final int count = Integer.parseInt(String.valueOf(maxEntry.get(KEY_COUNT)));
                            final List<AeaaKeywordTokenList> tokenList = new ArrayList<>();
                            for (List<String> keyword : keywords) {
                                tokenList.add(new AeaaKeywordTokenList(keyword));
                            }
                            maxContain.put(tokenList, count);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int compareTo(AeaaKeywordSet o) {
        int cat = String.CASE_INSENSITIVE_ORDER.compare(o.category, category);
        if (cat != 0) return cat;
        if (o.score == null) return 0;
        return Double.compare(score, o.score);
    }

    private boolean notEmpty(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof String) {
            return org.apache.commons.lang3.StringUtils.isNotEmpty((String) o);
        } else if (o instanceof Collection) {
            return !((Collection<?>) o).isEmpty();
        } else if (o instanceof Map) {
            return !((Map<?, ?>) o).isEmpty();
        } else if (o instanceof Number) {
            return true;
        } else {
            return false;
        }
    }

    public final static String KEY_NAME = "name";
    public final static String KEY_CATEGORY = "category";
    public final static String KEY_NOTES = "notes";
    public final static String KEY_SCORE = "score";
    private final static String KEY_MUST_CONTAIN_ALL = "all";
    private final static String KEY_MUST_NOT_CONTAIN = "none";
    private final static String KEY_MIN = "min";
    private final static String KEY_MAX = "max";
    private final static String KEY_AMOUNT = "amount";
    private final static String KEY_KEYWORDS = "keywords";
    private final static String KEY_COUNT = "count";
    private final static String KEY_STATUS = "status";

    private final static String KEY_KEYWORD_SETS = "sets";

    // EXPORT TO JSON

    public static JSONArray toReducedInformationJson(List<AeaaKeywordSet> keywordSets) {
        final JSONArray exportJson = new JSONArray();

        for (AeaaKeywordSet keywordSet : keywordSets) {
            exportJson.put(keywordSet.toReducedInformationJson());
        }

        return exportJson;
    }

    public static JSONArray toFullInformationJson(List<AeaaKeywordSet> keywordSets) {
        final JSONArray exportJson = new JSONArray();

        for (AeaaKeywordSet keywordSet : keywordSets) {
            exportJson.put(keywordSet.toFullInformationJson());
        }

        return exportJson;
    }

    public static List<AeaaKeywordSet> fromVulnerability(AeaaVulnerability vulnerability) {
        final List<AeaaKeywordSet> keywordSets = new ArrayList<>();

        final String matchedKeywords = vulnerability.getAdditionalAttribute(AeaaInventoryAttribute.KEYWORDS);

        if (matchedKeywords != null) {
            try {
                final JSONArray keywordsArray = new JSONArray(matchedKeywords);

                for (int i = 0; i < keywordsArray.length(); i++) {
                    final JSONObject keywordEntry = keywordsArray.optJSONObject(i);
                    if (keywordEntry == null) {
                        continue;
                    }

                    AeaaKeywordSet keywordSet = new AeaaKeywordSet();
                    keywordSet.setCategory(keywordEntry.optString(KEY_CATEGORY, null));
                    keywordSet.setScore(keywordEntry.has(KEY_SCORE) ? keywordEntry.optDouble(KEY_SCORE) : null);
                    keywordSet.setName(keywordEntry.optString(KEY_NAME, null));
                    keywordSet.setNotes(keywordEntry.optString(KEY_NOTES, null));

                    keywordSets.add(keywordSet);
                }
            } catch (Exception e) {
                LOG.error("Unable to read keyword data: [{}]", matchedKeywords, e);
            }
        }

        return keywordSets;
    }
}
