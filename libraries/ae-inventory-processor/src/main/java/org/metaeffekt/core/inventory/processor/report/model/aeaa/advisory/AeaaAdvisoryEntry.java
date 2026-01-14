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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.*;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaContentIdentifierStore;
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
                AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION.getKey(),
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
                AdvisoryMetaData.Attribute.CREATE_DATE.getKey(),
                AdvisoryMetaData.Attribute.CREATE_DATE_FORMATTED.getKey(),
                AdvisoryMetaData.Attribute.UPDATE_DATE.getKey(),
                AdvisoryMetaData.Attribute.UPDATE_DATE_FORMATTED.getKey(),
                AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(),
                AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(),
                AdvisoryMetaData.Attribute.CVSS_VECTORS.getKey(),
                AeaaInventoryAttribute.NVD_EQUIVALENT.getKey()
        ));
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_MAP) {{
        addAll(Arrays.asList(
                "id", "url", "summary", "description", "threat", "recommendations", "workarounds",
                "references", "acknowledgements", "keywords", "createDate", "updateDate",
                "cvss", "nvdEquivalent"
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
    /**
     * Stores what provider this advisory originates from.<br>
     * Must be defined on all advisory instances.
     */
    protected AeaaAdvisoryTypeIdentifier<?> sourceIdentifier;

    protected String summary;
    protected final List<AeaaDescriptionParagraph> description = new ArrayList<>();

    @Setter
    protected String url;

    protected String threat;
    protected String recommendations;
    protected String workarounds;

    protected final Set<String> acknowledgements = new LinkedHashSet<>();
    protected final Set<AeaaReference> references = new LinkedHashSet<>();
    protected final Set<String> keywords = new LinkedHashSet<>();

    protected Date createDate;
    protected Date updateDate;

    private final CvssVectorSet cvssVectors = new CvssVectorSet();

    public final static Comparator<AeaaAdvisoryEntry> UPDATE_CREATE_TIME_COMPARATOR = Comparator
            .comparing(AeaaAdvisoryEntry::getUpdateDate)
            .thenComparing(AeaaAdvisoryEntry::getCreateDate);

    public AeaaAdvisoryEntry(AeaaAdvisoryTypeIdentifier<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("Advisory source must not be null");
        } else {
            this.sourceIdentifier = source;
        }
    }

    public AeaaAdvisoryEntry(AeaaAdvisoryTypeIdentifier<?> source, String id) {
        this(source);
        this.id = id;
    }

    public void setSourceIdentifier(AeaaAdvisoryTypeIdentifier<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("Advisory source must not be null");
        }
        if (LOG.isDebugEnabled() && source != this.sourceIdentifier) {
            LOG.warn("Explicitly assigned source differs from originally assigned [{}] --> [{}]", this.sourceIdentifier.toExtendedString(), source.toExtendedString());
        }
        this.sourceIdentifier = source;
    }

    @Override
    public AeaaAdvisoryTypeIdentifier<?> getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * <p>Compute the effective state of this instance based on the given other instance.
     * The effective instances is either a new instance or the same instance, depending on the implementation.</p>
     * <p><i>'Effective'</i> is defined as the state of the instance that is the result of the computation of the
     * instance and the other instance. What this means specifically is up to the implementation.
     * This method returns <code>this</code> instance (itself) by default.</p>
     * <p>The effective instance should not be written back into an inventory, rather should be recalculated whenever
     * access to the effective instance is required to ensure data is not duplicated or left unmaintained in
     * inventories. Only use this to generate e.g. vulnerability-specific views on the VAD or other Reports.</p>
     *
     * @param other The other instance to compute the effective state from.
     * @return A new effective instance or the same instance, depending on the implementation.
     */
    public AeaaAdvisoryEntry computeEffectiveState(AeaaVulnerability other) {
        return this;
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

    public Date getCreateDate() {
        return createDate;
    }

    public String getCreateDateFormatted() {
        if (createDate == null) {
            return "unknown";
        }
        try {
            return AeaaTimeUtils.formatNormalizedDate(createDate);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String getCreateDateFormattedDateLevel() {
        if (createDate == null) {
            return "unknown";
        }
        try {
            return AeaaTimeUtils.formatNormalizedDateOnlyDate(createDate);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public String getUpdateDateFormatted() {
        if (updateDate == null) {
            return "unknown";
        }
        try {
            return AeaaTimeUtils.formatNormalizedDate(updateDate);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String getUpdateFormattedDateLevel() {
        if (updateDate == null) {
            return "unknown";
        }
        try {
            return AeaaTimeUtils.formatNormalizedDateOnlyDate(updateDate);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public boolean hasBeenUpdatedSince(long timestamp) {
        final Date date = updateDate != null ? updateDate : createDate;
        return date != null && date.getTime() > timestamp;
    }

    public String getUrl() {
        return url;
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
        return constructor.get().performAction(v -> v.appendFromBaseModel(amd));
    }

    public static AeaaAdvisoryEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        final AeaaAdvisoryTypeIdentifier<?> foundType = AeaaAdvisoryTypeStore.get().fromAdvisoryMetaData(amd).getIdentifier();
        return fromAdvisoryMetaData(amd, foundType.getAdvisoryFactory());
    }

    public static <T extends AeaaAdvisoryEntry> T fromInputMap(Map<String, Object> map, Supplier<T> constructor) {
        if (map == null) return null;
        return constructor.get()
                .performAction(v -> v.appendFromMap(map));
    }

    public static AeaaAdvisoryEntry fromJson(JSONObject json) {
        final AeaaContentIdentifierStore.AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> foundType = AeaaAdvisoryTypeStore.get().fromJsonNameAndImplementation(json);
        return fromJson(json, foundType.getIdentifier().getAdvisoryFactory());
    }

    public static <T extends AeaaAdvisoryEntry> T fromJson(JSONObject json, Supplier<T> constructor) {
        if (json == null) return null;
        return fromInputMap(json.toMap(), constructor);
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData advisoryMetaData) {
        super.appendFromBaseModel(advisoryMetaData);

        this.setId(advisoryMetaData.get(AdvisoryMetaData.Attribute.NAME));

        final String source = advisoryMetaData.get(AdvisoryMetaData.Attribute.SOURCE);
        final String sourceImplementation = advisoryMetaData.get(AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION);
        if (StringUtils.hasText(source) || StringUtils.hasText(sourceImplementation)) {
            this.setSourceIdentifier(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(source, sourceImplementation));
        } else {
            AeaaAdvisoryTypeStore.get().fromId(this.getId()).ifPresent(inferred -> {
                LOG.debug("Inferred source identifier [{}] for advisory [{}]", inferred.toExtendedString(), this.getId());
                setSourceIdentifier(inferred);
            });
        }

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

        if (input.containsKey("id")) {
            this.setId(String.valueOf(input.get("id")));
        }

        final String source = (String) input.getOrDefault("source", null);
        final String sourceImplementation = (String) input.getOrDefault("sourceImplementation", null);
        if (source != null || sourceImplementation != null) {
            this.setSourceIdentifier(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(source, sourceImplementation));
        }

        this.setSummary((String) input.getOrDefault("summary", null));

        final Object inputDescriptionParagraphs = input.getOrDefault("description", new ArrayList<>());
        if (inputDescriptionParagraphs instanceof List) {
            final List<AeaaDescriptionParagraph> descriptionParagraphs = (List<AeaaDescriptionParagraph>) inputDescriptionParagraphs;
            for (AeaaDescriptionParagraph descriptionParagraph : descriptionParagraphs) {
                this.addDescription(descriptionParagraph);
            }
        } else if (inputDescriptionParagraphs instanceof String) {
            final String descriptionParagraphs = (String) inputDescriptionParagraphs;
            this.addDescription(AeaaDescriptionParagraph.fromContent(descriptionParagraphs));
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
        json.put("url", getUrl());

        json.put("summary", summary);
        json.put("description", new JSONArray(description.stream().map(AeaaDescriptionParagraph::toJson).collect(Collectors.toList())));

        json.put("threat", threat);
        json.put("recommendations", recommendations);
        json.put("workarounds", workarounds);

        json.put("references", references);
        json.put("acknowledgements", acknowledgements);
        json.put("keywords", keywords);

        if (createDate != null) json.put("createDate", createDate.getTime());
        if (updateDate != null) json.put("updateDate", updateDate.getTime());

        json.put("cvss", this.cvssVectors.toJson());
    }

    @Override
    public String toString() {
        return "{" + id + ": " + summary + "}";
    }
}
