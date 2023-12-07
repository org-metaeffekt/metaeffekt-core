/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.*;
import org.metaeffekt.core.security.cvss.processor.CvssVectorSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.advisory.AdvisoryEntry</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaAdvisoryEntry extends AeaaMatchableDetailsAmbDataClass<AdvisoryMetaData, AeaaAdvisoryEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(AeaaAdvisoryEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_AMB) {{
        addAll(Arrays.asList(
                AdvisoryMetaData.Attribute.NAME.getKey(),
                AdvisoryMetaData.Attribute.SOURCE.getKey(),
                AdvisoryMetaData.Attribute.URL.getKey(),
                AdvisoryMetaData.Attribute.TYPE.getKey(),
                AdvisoryMetaData.Attribute.SUMMARY.getKey(),
                AdvisoryMetaData.Attribute.DESCRIPTION.getKey(),
                AdvisoryMetaData.Attribute.THREAT.getKey(),
                AdvisoryMetaData.Attribute.RECOMMENDATIONS.getKey(),
                AdvisoryMetaData.Attribute.WORKAROUNDS.getKey(),
                AdvisoryMetaData.Attribute.ACKNOWLEDGEMENTS.getKey(),
                AdvisoryMetaData.Attribute.REFERENCES.getKey(),
                AdvisoryMetaData.Attribute.KEYWORDS.getKey(),
                AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey(),
                AdvisoryMetaData.Attribute.CREATE_DATE.getKey(),
                AdvisoryMetaData.Attribute.CREATE_DATE_FORMATTED.getKey(),
                AdvisoryMetaData.Attribute.UPDATE_DATE.getKey(),
                AdvisoryMetaData.Attribute.UPDATE_DATE_FORMATTED.getKey(),
                AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(),
                AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(),
                AdvisoryMetaData.Attribute.CVSS_VECTORS.getKey()
        ));
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_MAP) {{
        addAll(Arrays.asList(
                "source", "id", "url", "summary", "description", "threat", "recommendations", "workarounds",
                "references", "acknowledgements", "keywords", "referencedIds", "createDate", "updateDate",
                "cvss"
        ));
    }};

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    protected String summary;
    protected final List<AeaaDescriptionParagraph> description = new ArrayList<>();

    protected String threat;
    protected String recommendations;
    protected String workarounds;

    protected final Set<String> acknowledgements = new LinkedHashSet<>();
    protected final Set<AeaaReference> references = new LinkedHashSet<>();
    protected final Set<String> keywords = new LinkedHashSet<>();
    protected final Map<AeaaContentIdentifiers, Set<String>> referencedIds = new HashMap<>();

    protected Date createDate;
    protected Date updateDate;

    private final CvssVectorSet cvssVectors = new CvssVectorSet();


    public final static Comparator<AeaaAdvisoryEntry> UPDATE_CREATE_TIME_COMPARATOR = Comparator
            .comparing(AeaaAdvisoryEntry::getUpdateDate)
            .thenComparing(AeaaAdvisoryEntry::getCreateDate);

    public AeaaAdvisoryEntry(AeaaContentIdentifiers source) {
        if (source == null || source == AeaaContentIdentifiers.UNKNOWN) {
            LOG.warn("{} source is null or unknown: {}", AeaaAdvisoryEntry.class.getSimpleName(), source);
        } else {
            this.addDataSource(source);
        }
    }

    public AeaaAdvisoryEntry(String id) {
        this(AeaaContentIdentifiers.fromEntryIdentifier(id));
        this.id = id;
    }

    public AeaaAdvisoryEntry(AeaaContentIdentifiers source, String id) {
        this(source);
        this.id = id;
    }

    public AeaaAdvisoryEntry setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public void setDescription(AeaaDescriptionParagraph paragraph) {
        this.description.clear();
        if (paragraph.isEmpty()) return;
        this.description.add(paragraph);
    }

    public void addDescription(AeaaDescriptionParagraph paragraph) {
        if (paragraph.isEmpty()) return;
        this.description.add(paragraph);
    }

    public void addDescription(List<AeaaDescriptionParagraph> paragraphs) {
        paragraphs.stream()
                .filter(p -> !p.isEmpty())
                .forEach(this::addDescription);
    }

    public void clearDescription() {
        this.description.clear();
    }

    public void setThreat(String threat) {
        this.threat = threat;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public void setWorkarounds(String workarounds) {
        this.workarounds = workarounds;
    }

    public void addReference(AeaaReference reference) {
        if (StringUtils.hasText(reference.getUrl())) {
            this.references.add(reference);
        }
    }

    public void addReferences(Collection<AeaaReference> references) {
        references.forEach(this::addReference);
    }

    public void removeReference(String reference) {
        this.references.removeIf(r -> r.getUrl().equals(reference));
        this.references.removeIf(r -> r.getTitle().equals(reference));
    }

    public void removeReference(AeaaReference reference) {
        this.references.remove(reference);
    }

    public void addAcknowledgement(String author) {
        if (StringUtils.hasText(author)) {
            this.acknowledgements.add(author);
        }
    }

    public void addAcknowledgements(Collection<String> authors) {
        authors.forEach(this::addAcknowledgement);
    }

    public void removeAcknowledgement(String author) {
        this.acknowledgements.remove(author);
    }

    public void addKeyword(String keyword) {
        if (StringUtils.hasText(keyword)) {
            this.keywords.add(keyword);
        }
    }

    public void addKeywords(Collection<String> keywords) {
        keywords.forEach(this::addKeyword);
    }

    public void removeKeyword(String keyword) {
        this.keywords.remove(keyword);
    }

    public void addReferencedId(String id) {
        if (StringUtils.hasText(id)) {
            final AeaaContentIdentifiers type = AeaaContentIdentifiers.fromEntryIdentifier(id);
            final String normalizedId = type.normalizeEntryIdentifier(id);

            this.referencedIds.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(normalizedId);
        }
    }

    public void addReferencedIds(Collection<String> ids) {
        ids.forEach(this::addReferencedId);
    }

    public void addReferencedIds(Map<String, Object> ids) {
        for (Object value : ids.values()) {
            final List<String> specificIds;
            if (value instanceof List) {
                specificIds = ((List<?>) value).stream().map(Object::toString).collect(Collectors.toList());
            } else if (value instanceof String) {
                specificIds = Collections.singletonList((String) value);
            } else if (value instanceof JSONArray) {
                specificIds = ((JSONArray) value).toList().stream().map(Object::toString).collect(Collectors.toList());
            } else {
                specificIds = Collections.emptyList();
            }

            specificIds.forEach(this::addReferencedId);
        }
    }

    public void addReferencedIdsSpecific(Map<AeaaContentIdentifiers, Set<String>> ids) {
        ids.forEach((k, v) -> this.referencedIds.computeIfAbsent(k, k2 -> new LinkedHashSet<>()).addAll(v));
    }

    public void addReferencedIdsString(Map<String, String> ids) {
        ids.values().forEach(this::addReferencedId);
    }

    public void removeReferencedId(String id) {
        final AeaaContentIdentifiers type = AeaaContentIdentifiers.fromEntryIdentifier(id);
        final String normalizedId = type.normalizeEntryIdentifier(id);

        this.referencedIds.computeIfPresent(type, (k, v) -> {
            v.remove(normalizedId);
            return v;
        });

        if (this.referencedIds.get(type) == null || this.referencedIds.get(type).isEmpty()) {
            this.referencedIds.remove(type);
        }
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
        setCorrectUpdateCreateDateOrder();
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
        setCorrectUpdateCreateDateOrder();
    }

    private void setCorrectUpdateCreateDateOrder() {
        if (this.updateDate != null && this.createDate != null &&
                this.updateDate.before(this.createDate)) {
            final Date tmp = this.updateDate;
            this.updateDate = this.createDate;
            this.createDate = tmp;
        }
    }

    public void setCreateDate(long creationDate) {
        setCreateDate(new Date(creationDate));
    }

    public void setUpdateDate(long lastModifiedDate) {
        setUpdateDate(new Date(lastModifiedDate));
    }

    public String getSummary() {
        return summary;
    }

    public String getTextDescription() {
        return description.stream().map(AeaaDescriptionParagraph::toString).collect(Collectors.joining("\n\n"));
    }

    public List<AeaaDescriptionParagraph> getDescription() {
        return description;
    }

    private final static String[] DESCRIPTION_PARAGRAPHS_PREFERENCE_ORDER = new String[]{
            "details"
    };

    public String getNonEmptyDescription() {
        if (StringUtils.hasText(this.summary)) {
            return this.summary;
        }

        for (String paragraphType : DESCRIPTION_PARAGRAPHS_PREFERENCE_ORDER) {
            final Optional<AeaaDescriptionParagraph> paragraph = this.description.stream()
                    .filter(p -> p.getHeader().equalsIgnoreCase(paragraphType))
                    .findFirst();
            if (paragraph.isPresent()) {
                return paragraph.get().getContent();
            }
        }

        return this.getTextDescription();
    }

    public String getThreat() {
        return threat;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public String getWorkarounds() {
        return workarounds;
    }

    public Set<AeaaReference> getReferences() {
        return references;
    }

    public Set<String> getAcknowledgements() {
        return acknowledgements;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public Map<AeaaContentIdentifiers, Set<String>> getReferencedIds() {
        return referencedIds;
    }

    public Set<String> getReferencedIds(AeaaContentIdentifiers type) {
        return referencedIds.getOrDefault(type, Collections.emptySet());
    }

    public Date getCreateDate() {
        return createDate;
    }

    public String getCreateDateFormatted() {
        return AeaaTimeUtils.formatNormalizedDate(createDate);
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public String getUpdateDateFormatted() {
        return AeaaTimeUtils.formatNormalizedDate(updateDate);
    }

    public boolean hasBeenUpdatedSince(long timestamp) {
        final Date date = updateDate != null ? updateDate : createDate;
        return date != null && date.getTime() > timestamp;
    }

    public String getUrl() {
        throw new UnsupportedOperationException("getUrl() not implemented for " + this.getClass().getSimpleName());
    }

    public String getType() {
        throw new UnsupportedOperationException("getType() not implemented for " + this.getClass().getSimpleName());
    }

    /* CVSS */

    public CvssVectorSet getCvssVectors() {
        return this.cvssVectors;
    }

    /* TYPE CONVERSION METHODS */

    @Override
    public AdvisoryMetaData constructBaseModel() {
        return new AdvisoryMetaData();
    }

    public static <T extends AeaaAdvisoryEntry> T fromAdvisoryMetaData(AdvisoryMetaData amd, Supplier<T> constructor) {
        if (amd == null) return null;
        return constructor.get()
                .performAction(v -> v.appendFromBaseModel(amd));
    }

    public static AeaaAdvisoryEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        final AeaaContentIdentifiers source = AeaaContentIdentifiers.extractSourceFromAdvisor(amd);
        source.assertAdvisoryEntryClass(() -> amd.get(AdvisoryMetaData.Attribute.NAME));

        return fromAdvisoryMetaData(amd, source.getAdvisoryEntryFactory());
    }

    public static <T extends AeaaAdvisoryEntry> T fromInputMap(Map<String, Object> map, Supplier<T> constructor) {
        if (map == null) return null;
        return constructor.get()
                .performAction(v -> v.appendFromMap(map));
    }

    public static AeaaAdvisoryEntry fromJson(JSONObject json) {
        final AeaaContentIdentifiers source = AeaaContentIdentifiers.extractSourceFromJson(json);
        source.assertAdvisoryEntryClass(json::toString);

        return fromInputMap(json.toMap(), source.getAdvisoryEntryFactory());
    }

    public static <T extends AeaaAdvisoryEntry> T fromJson(JSONObject json, Supplier<T> constructor) {
        return fromInputMap(json.toMap(), constructor);
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData advisoryMetaData) {
        super.appendFromBaseModel(advisoryMetaData);

        this.setId(advisoryMetaData.get(AdvisoryMetaData.Attribute.NAME));
        this.setSummary(advisoryMetaData.get(AdvisoryMetaData.Attribute.SUMMARY));
        if (StringUtils.hasText(advisoryMetaData.get(AdvisoryMetaData.Attribute.DESCRIPTION))) {
            this.addDescription(AeaaDescriptionParagraph.fromJson(new JSONArray(advisoryMetaData.get(AdvisoryMetaData.Attribute.DESCRIPTION))));
        }

        this.setThreat(advisoryMetaData.get(AdvisoryMetaData.Attribute.THREAT));
        this.setRecommendations(advisoryMetaData.get(AdvisoryMetaData.Attribute.RECOMMENDATIONS));
        this.setWorkarounds(advisoryMetaData.get(AdvisoryMetaData.Attribute.WORKAROUNDS));

        if (StringUtils.hasText(advisoryMetaData.get(AdvisoryMetaData.Attribute.ACKNOWLEDGEMENTS))) {
            final JSONArray acknowledgements = new JSONArray(advisoryMetaData.get(AdvisoryMetaData.Attribute.ACKNOWLEDGEMENTS));
            for (int i = 0; i < acknowledgements.length(); i++) {
                this.addAcknowledgement(acknowledgements.optString(i));
            }
        }
        if (StringUtils.hasText(advisoryMetaData.get(AdvisoryMetaData.Attribute.KEYWORDS))) {
            final JSONArray keywords = new JSONArray(advisoryMetaData.get(AdvisoryMetaData.Attribute.KEYWORDS));
            for (int i = 0; i < keywords.length(); i++) {
                this.addKeyword(keywords.optString(i));
            }
        }

        final String referencesString = advisoryMetaData.get(AdvisoryMetaData.Attribute.REFERENCES);
        if (StringUtils.hasText(referencesString)) {
            final JSONArray references = new JSONArray(referencesString);
            for (int i = 0; i < references.length(); i++) {
                this.addReference(AeaaReference.fromJson(references.optJSONObject(i)));
            }
        }

        final String referencedIdsJsonString = advisoryMetaData.get(AdvisoryMetaData.Attribute.REFERENCED_IDS);
        if (StringUtils.hasText(referencedIdsJsonString)) {
            this.addReferencedIds(new JSONObject(referencedIdsJsonString).toMap());
        }

        this.setCreateDate(AeaaTimeUtils.tryParse(advisoryMetaData.get(AdvisoryMetaData.Attribute.CREATE_DATE)));
        this.setUpdateDate(AeaaTimeUtils.tryParse(advisoryMetaData.get(AdvisoryMetaData.Attribute.UPDATE_DATE)));

        if (StringUtils.hasText(advisoryMetaData.get(AdvisoryMetaData.Attribute.CVSS_VECTORS))) {
            final String cvssVectors = advisoryMetaData.get(AdvisoryMetaData.Attribute.CVSS_VECTORS);
            this.cvssVectors.addAllCvssVectors(CvssVectorSet.fromJson(new JSONArray(cvssVectors)));
        }
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);

        amd.set(AdvisoryMetaData.Attribute.NAME, id);
        amd.set(AdvisoryMetaData.Attribute.SOURCE, getEntrySource().name());
        amd.set(AdvisoryMetaData.Attribute.URL, getUrl());
        amd.set(AdvisoryMetaData.Attribute.TYPE, getType());

        amd.set(AdvisoryMetaData.Attribute.SUMMARY, summary);
        if (!description.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.DESCRIPTION, new JSONArray(description.stream().map(AeaaDescriptionParagraph::toJson).collect(Collectors.toList())).toString());
        }

        if (threat != null) amd.set(AdvisoryMetaData.Attribute.THREAT, threat);
        if (recommendations != null) amd.set(AdvisoryMetaData.Attribute.RECOMMENDATIONS, recommendations);
        if (workarounds != null) amd.set(AdvisoryMetaData.Attribute.WORKAROUNDS, workarounds);
        if (!acknowledgements.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.ACKNOWLEDGEMENTS, new JSONArray(acknowledgements).toString());
        }
        if (!references.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.REFERENCES, new JSONArray(references.stream().map(AeaaReference::toJson).collect(Collectors.toList())).toString());
        }
        if (!keywords.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.KEYWORDS, new JSONArray(keywords).toString());
        }
        if (!referencedIds.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.REFERENCED_IDS, new JSONObject(referencedIds).toString());
        }

        if (createDate != null) {
            amd.set(AdvisoryMetaData.Attribute.CREATE_DATE, Long.toString(this.createDate.getTime()));
            amd.set(AdvisoryMetaData.Attribute.CREATE_DATE_FORMATTED, AeaaTimeUtils.formatNormalizedDate(this.createDate));
        }
        if (updateDate != null) {
            amd.set(AdvisoryMetaData.Attribute.UPDATE_DATE, Long.toString(this.updateDate.getTime()));
            amd.set(AdvisoryMetaData.Attribute.UPDATE_DATE_FORMATTED, AeaaTimeUtils.formatNormalizedDate(this.updateDate));
        }

        if (!this.cvssVectors.isEmpty()) {
            amd.set(AdvisoryMetaData.Attribute.CVSS_VECTORS, this.cvssVectors.toJson().toString());
        }
    }

    @Override
    public void appendFromDataClass(AeaaAdvisoryEntry dataClass) {
        super.appendFromDataClass(dataClass);

        if (StringUtils.hasText(dataClass.getSummary())) {
            this.setSummary(dataClass.getSummary());
        }
        if (!dataClass.getDescription().isEmpty()) {
            this.addDescription(dataClass.getDescription());
        }

        if (StringUtils.hasText(dataClass.getThreat())) {
            this.setThreat(dataClass.getThreat());
        }
        if (StringUtils.hasText(dataClass.getRecommendations())) {
            this.setRecommendations(dataClass.getRecommendations());
        }
        if (StringUtils.hasText(dataClass.getWorkarounds())) {
            this.setWorkarounds(dataClass.getWorkarounds());
        }
        if (!dataClass.getReferences().isEmpty()) {
            this.addReferences(dataClass.getReferences());
        }
        if (!dataClass.getAcknowledgements().isEmpty()) {
            this.addAcknowledgements(dataClass.getAcknowledgements());
        }
        if (!dataClass.getKeywords().isEmpty()) {
            this.addKeywords(dataClass.getKeywords());
        }
        if (!dataClass.getReferencedIds().isEmpty()) {
            this.addReferencedIdsSpecific(dataClass.getReferencedIds());
        }

        if (dataClass.getCreateDate() != null) {
            this.setCreateDate(dataClass.getCreateDate());
        }
        if (dataClass.getUpdateDate() != null) {
            this.setUpdateDate(dataClass.getUpdateDate());
        }

        if (!dataClass.getCvssVectors().isEmpty()) {
            this.cvssVectors.addAllCvssVectors(dataClass.getCvssVectors());
        }
    }

    @Override
    public void appendFromMap(Map<String, Object> input) {
        super.appendFromMap(input);

        this.setId((String) input.getOrDefault("id", null));
        this.setSummary((String) input.getOrDefault("summary", null));

        List<AeaaDescriptionParagraph> AeaaDescriptionParagraphs = (List<AeaaDescriptionParagraph>) input.getOrDefault("description", new ArrayList<>());
        for (AeaaDescriptionParagraph AeaaDescriptionParagraph : AeaaDescriptionParagraphs) {
            this.addDescription(AeaaDescriptionParagraph);
        }

        this.setThreat((String) input.getOrDefault("threat", null));
        this.setRecommendations((String) input.getOrDefault("recommendations", null));
        this.setWorkarounds((String) input.getOrDefault("workarounds", null));

        List<Map<String, Object>> references = (List<Map<String, Object>>) input.getOrDefault("references", new ArrayList<>());
        for (Map<String, Object> referenceMap : references) {
            this.addReference(AeaaReference.fromMap(referenceMap));
        }

        List<String> acknowledgements = (List<String>) input.getOrDefault("acknowledgements", new ArrayList<>());
        for (String acknowledgement : acknowledgements) {
            this.addAcknowledgement(acknowledgement);
        }

        List<String> keywords = (List<String>) input.getOrDefault("keywords", new ArrayList<>());
        for (String keyword : keywords) {
            this.addKeyword(keyword);
        }

        Map<String, String> referencedIds = (Map<String, String>) input.getOrDefault("referencedIds", new HashMap<>());
        this.addReferencedIdsString(referencedIds);

        this.setCreateDate(AeaaTimeUtils.tryParse((String) input.getOrDefault("createDate", null)));
        this.setUpdateDate(AeaaTimeUtils.tryParse((String) input.getOrDefault("updateDate", null)));

        if (input.get("cvss") != null) {
            final String cvssVectors = (String) input.get("cvss");
            this.cvssVectors.addAllCvssVectors(CvssVectorSet.fromJson(new JSONArray(cvssVectors)));
        }
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);

        json.put("id", id);
        json.put("source", getEntrySource().name());
        json.put("url", getUrl());

        json.put("summary", summary);
        json.put("description", new JSONArray(description.stream().map(AeaaDescriptionParagraph::toJson).collect(Collectors.toList())));

        json.put("threat", threat);
        json.put("recommendations", recommendations);
        json.put("workarounds", workarounds);

        json.put("references", references);
        json.put("acknowledgements", acknowledgements);
        json.put("keywords", keywords);
        json.put("referencedIds", referencedIds);

        if (createDate != null) json.put("createDate", createDate.getTime());
        if (updateDate != null) json.put("updateDate", updateDate.getTime());

        json.put("cvss", this.cvssVectors.toJson());
    }

    @Override
    public String toString() {
        return "{" + id + ": " + summary + "}";
    }
}
