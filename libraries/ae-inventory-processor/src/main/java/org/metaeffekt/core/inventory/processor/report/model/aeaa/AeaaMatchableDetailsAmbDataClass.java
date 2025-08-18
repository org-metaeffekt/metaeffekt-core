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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AeaaMatchableDetailsAmbDataClass<AMB extends AbstractModelBase, DC extends AeaaMatchableDetailsAmbDataClass<AMB, DC>> extends AeaaAmbDataClass<AMB, DC> {

    private static final Logger LOG = LoggerFactory.getLogger(AeaaMatchableDetailsAmbDataClass.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAmbDataClass.CONVERSION_KEYS_AMB) {{
        add(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey());
        add(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey());

        add(AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey());
        add(AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS.getKey());

        add(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey());
        add(VulnerabilityMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey());
        add(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey());
        add(VulnerabilityMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey());
        add(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey());
        add(VulnerabilityMetaData.Attribute.REFERENCED_OTHER.getKey());
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAmbDataClass.CONVERSION_KEYS_MAP) {{
        add("source");
        add("sourceImplementation");
        add("dataFillingSources");
        add("matchingSources");
        add("referencedIds");
        add("referencedSecurityAdvisories");
        add("referencedVulnerabilities");
        add("referencedOtherIds");
    }};

    protected final Map<AeaaVulnerabilityTypeIdentifier<?>, Set<String>> referencedVulnerabilities = new HashMap<>();
    protected final Map<AeaaAdvisoryTypeIdentifier<?>, Set<String>> referencedSecurityAdvisories = new HashMap<>();
    protected final Map<AeaaOtherTypeIdentifier, Set<String>> referencedOtherIds = new HashMap<>();

    protected final List<AeaaDataSourceIndicator> matchingSources = new ArrayList<>();
    protected final Set<AeaaContentIdentifierStore.AeaaContentIdentifier> dataSources = new HashSet<>();

    /**
     * Artifacts that are affected by this entry.<br>
     * Will not be written back into the inventory as a field, but rather as references from the artifact to this entry.
     * <pre>
     * Key:   Artifact field name
     * Value: Set&lt;Artifact&gt; affected artifacts
     * </pre>
     */
    protected final Map<String, Set<Artifact>> affectedArtifacts = new HashMap<>();

    public abstract AeaaContentIdentifierStore.AeaaContentIdentifier getSourceIdentifier();

    public DC addMatchingSource(AeaaDataSourceIndicator matchingSource) {
        try {
            this.matchingSources.add(matchingSource);
            if (matchingSource.getMatchReason() instanceof AeaaDataSourceIndicator.ArtifactReason) {
                final AeaaDataSourceIndicator.ArtifactReason artifactReason = (AeaaDataSourceIndicator.ArtifactReason) matchingSource.getMatchReason();
                if (artifactReason.hasArtifact()) {
                    this.manuallyAffectsArtifact(Artifact.Attribute.VULNERABILITY.getKey(), artifactReason.getArtifact());
                }
            }
            return (DC) this;
        } catch (Exception e) {
            LOG.error("Error while adding matching source [{}] to [{}]: {}", matchingSource, this.getClass().getSimpleName(), this.getId(), e);
            return (DC) this;
        }
    }

    public List<AeaaDataSourceIndicator> getMatchingSources() {
        return matchingSources;
    }

    public DC addDataSource(AeaaContentIdentifierStore.AeaaContentIdentifier dataSource) {
        this.dataSources.add(dataSource);
        return (DC) this;
    }

    public Set<AeaaContentIdentifierStore.AeaaContentIdentifier> getDataSources() {
        return dataSources;
    }

    public Set<String> getWellFormedDataSources() {
        final Set<AeaaContentIdentifierStore.AeaaContentIdentifier> sources = new HashSet<>(dataSources);
        sources.add(getSourceIdentifier());
        return sources.stream()
                .map(AeaaContentIdentifierStore.AeaaContentIdentifier::getWellFormedName)
                .collect(Collectors.toSet());
    }

    public Map<String, Set<Artifact>> getAffectedArtifacts() {
        return affectedArtifacts;
    }

    public Set<Artifact> getAffectedArtifactsByKey(String key) {
        return affectedArtifacts.getOrDefault(key, Collections.emptySet());
    }

    public Set<Artifact> getAffectedArtifactsByDefaultKey() {
        return getAffectedArtifactsByKey(Artifact.Attribute.VULNERABILITY.getKey());
    }

    /**
     * Manually append the id of this instance to the given artifact's attribute with the given key.
     * <p>
     * If the goal is to append the id to the attribute {@link Artifact.Attribute#VULNERABILITY}, use the
     * {@link AeaaMatchableDetailsAmbDataClass#matchingSources} instead, which will automatically append the id to the
     * {@link Artifact.Attribute#VULNERABILITY} attribute if the indicator reason is an
     * {@link AeaaDataSourceIndicator.ArtifactReason} instance.
     *
     * @param key      The key of the attribute to append the id to.
     * @param artifact The artifact to append the id to.
     * @return This instance for chaining.
     */
    public DC manuallyAffectsArtifact(String key, Artifact artifact) {
        if (artifact != null) {
            this.affectedArtifacts.computeIfAbsent(key, k -> new HashSet<>())
                    .add(artifact);

            if (!Artifact.Attribute.VULNERABILITY.getKey().equals(key)) {
                // some vulnerabilities are removed, such as the ones that are fixed by a KB (InventoryAttribute.VULNERABILITIES_FIXED_BY_KB),
                // so we must add the affected vulnerability to the artifact here as well, not only in the VulnerabilityContextInventory.
                final String presentVulnerabilities = artifact.get(key);
                final Set<String> vulnerabilities = StringUtils.hasText(presentVulnerabilities) ? Arrays.stream(presentVulnerabilities.split(", ")).collect(Collectors.toSet()) : new HashSet<>();
                vulnerabilities.add(this.getId());
                artifact.set(key, String.join(", ", vulnerabilities));
            }
        }
        return (DC) this;
    }

    /**
     * Manually append the id of this instance to the given artifact's attribute with the given key.
     * <p>
     * If the goal is to append the id to the attribute {@link Artifact.Attribute#VULNERABILITY}, use the
     * {@link AeaaMatchableDetailsAmbDataClass#matchingSources} instead, which will automatically append the id to the
     * {@link Artifact.Attribute#VULNERABILITY} attribute if the indicator reason is an
     * {@link AeaaDataSourceIndicator.ArtifactReason} instance.
     *
     * @param key      The key of the attribute to append the id to.
     * @param artifact The artifact to append the id to.
     * @return This instance for chaining.
     */
    public DC manuallyAffectsArtifact(AbstractModelBase.Attribute key, Artifact artifact) {
        return manuallyAffectsArtifact(key.getKey(), artifact);
    }

    public DC removeAffectsArtifact(String key, Artifact artifact) {
        if (artifact != null) {
            this.affectedArtifacts.computeIfAbsent(key, k -> new HashSet<>())
                    .remove(artifact);

            final String presentVulnerabilities = artifact.get(key);
            final Set<String> vulnerabilities = StringUtils.hasText(presentVulnerabilities) ? Arrays.stream(presentVulnerabilities.split(", ")).collect(Collectors.toSet()) : new HashSet<>();
            vulnerabilities.remove(this.getId());
            artifact.set(key, String.join(", ", vulnerabilities));
        }
        return (DC) this;
    }

    public Map<AeaaVulnerabilityTypeIdentifier<?>, Set<String>> getReferencedVulnerabilities() {
        return referencedVulnerabilities;
    }

    public Map<AeaaAdvisoryTypeIdentifier<?>, Set<String>> getReferencedSecurityAdvisories() {
        return referencedSecurityAdvisories;
    }

    public Map<AeaaOtherTypeIdentifier, Set<String>> getReferencedOtherIds() {
        return referencedOtherIds;
    }
// START: MANAGE REFERENCED SECURITY ADVISORIES

    public void addReferencedSecurityAdvisory(AeaaAdvisoryTypeIdentifier<?> source, String id) {
        if (source == null || id == null) {
            LOG.warn("Cannot add referenced security advisory with null source or id on advisory [{}]", this.id);
            return;
        }
        this.referencedSecurityAdvisories.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(id);
    }

    public void addReferencedSecurityAdvisories(Map<AeaaAdvisoryTypeIdentifier<?>, Set<String>> referencedSecurityAdvisories) {
        for (Map.Entry<AeaaAdvisoryTypeIdentifier<?>, Set<String>> entry : referencedSecurityAdvisories.entrySet()) {
            this.referencedSecurityAdvisories.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
        }
    }

    public void addReferencedSecurityAdvisory(AeaaAdvisoryEntry advisory) {
        if (advisory == null) {
            LOG.warn("Cannot add referenced security advisory with null advisory on advisory [{}]", this.id);
            return;
        }
        this.addReferencedSecurityAdvisory(advisory.getSourceIdentifier(), advisory.getId());
    }

    public void addReferencedSecurityAdvisories(Collection<AeaaAdvisoryEntry> advisories) {
        advisories.forEach(this::addReferencedSecurityAdvisory);
    }

    public void removeReferencedSecurityAdvisory(AeaaAdvisoryTypeIdentifier<?> source, String id) {
        this.referencedSecurityAdvisories.computeIfPresent(source, (k, v) -> {
            v.remove(id);
            return v;
        });

        if (this.referencedSecurityAdvisories.get(source) == null || this.referencedSecurityAdvisories.get(source).isEmpty()) {
            this.referencedSecurityAdvisories.remove(source);
        }
    }

    public void removeReferencedSecurityAdvisory(AeaaAdvisoryEntry advisory) {
        if (advisory == null) {
            LOG.warn("Cannot remove referenced security advisory with null advisory on advisory [{}]", this.id);
            return;
        }
        this.removeReferencedSecurityAdvisory(advisory.getSourceIdentifier(), advisory.getId());
    }

    public void removeReferencedSecurityAdvisories(Collection<AeaaAdvisoryEntry> advisories) {
        advisories.forEach(this::removeReferencedSecurityAdvisory);
    }

    // END: MANAGE REFERENCED SECURITY ADVISORIES

    // START: MANAGE REFERENCED VULNERABILITIES

    public void addReferencedVulnerability(AeaaVulnerabilityTypeIdentifier<?> source, String id) {
        if (source == null || id == null) {
            LOG.warn("Cannot add referenced vulnerability with null source or id on advisory [{}]", this.id);
            return;
        }
        this.referencedVulnerabilities.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(id);
    }

    public void addReferencedVulnerabilities(Map<AeaaVulnerabilityTypeIdentifier<?>, Set<String>> referencedVulnerabilities) {
        for (Map.Entry<AeaaVulnerabilityTypeIdentifier<?>, Set<String>> entry : referencedVulnerabilities.entrySet()) {
            this.referencedVulnerabilities.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
        }
    }

    public void addReferencedVulnerability(AeaaVulnerability vulnerability) {
        if (vulnerability == null) {
            LOG.warn("Cannot add referenced vulnerability with null vulnerability on advisory [{}]", this.id);
            return;
        }
        this.addReferencedVulnerability(vulnerability.getSourceIdentifier(), vulnerability.getId());
    }

    public void addReferencedVulnerabilities(Collection<AeaaVulnerability> vulnerabilities) {
        vulnerabilities.forEach(this::addReferencedVulnerability);
    }

    public void removeReferencedVulnerability(AeaaVulnerabilityTypeIdentifier<?> source, String id) {
        this.referencedVulnerabilities.computeIfPresent(source, (k, v) -> {
            v.remove(id);
            return v;
        });

        if (this.referencedVulnerabilities.get(source) == null || this.referencedVulnerabilities.get(source).isEmpty()) {
            this.referencedVulnerabilities.remove(source);
        }
    }

    public void removeReferencedVulnerability(AeaaVulnerability vulnerability) {
        if (vulnerability == null) {
            LOG.warn("Cannot remove referenced vulnerability with null vulnerability on advisory [{}]", this.id);
            return;
        }
        this.removeReferencedVulnerability(vulnerability.getSourceIdentifier(), vulnerability.getId());
    }

    public void removeReferencedVulnerabilities(Collection<AeaaVulnerability> vulnerabilities) {
        vulnerabilities.forEach(this::removeReferencedVulnerability);
    }

    // END: MANAGE REFERENCED VULNERABILITIES

    // START: MANAGE OTHER REFERENCED IDS

    public void addOtherReferencedId(AeaaOtherTypeIdentifier source, String id) {
        if (source == null || id == null) {
            LOG.warn("Cannot add other referenced id with null source or id on advisory [{}]", this.id);
            return;
        }
        this.referencedOtherIds.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(id);
    }

    public void addOtherReferencedIds(Map<AeaaOtherTypeIdentifier, Set<String>> referencedOtherIds) {
        for (Map.Entry<AeaaOtherTypeIdentifier, Set<String>> entry : referencedOtherIds.entrySet()) {
            this.referencedOtherIds.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
        }
    }

    public void addOtherReferencedIds(AeaaOtherTypeIdentifier source, Collection<String> referencedOtherIds) {
        this.referencedOtherIds.computeIfAbsent(source, k -> new LinkedHashSet<>()).addAll(referencedOtherIds);
    }

    public void removeOtherReferencedId(AeaaOtherTypeIdentifier source, String id) {
        this.referencedOtherIds.computeIfPresent(source, (k, v) -> {
            v.remove(id);
            return v;
        });

        if (this.referencedOtherIds.get(source) == null || this.referencedOtherIds.get(source).isEmpty()) {
            this.referencedOtherIds.remove(source);
        }
    }

    // END: MANAGE OTHER REFERENCED

    /* DATA TYPE CONVERSION METHODS */

    @Override
    public void appendFromBaseModel(AMB amb) {
        super.appendFromBaseModel(amb);

        boolean foundNewReferenceFormat = false;

        final String referencedVulnerabilities = amb.get(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES);
        if (referencedVulnerabilities != null) {
            this.addReferencedVulnerabilities(AeaaVulnerabilityTypeStore.get().fromJsonMultipleReferencedIds(new JSONArray(referencedVulnerabilities)));
            foundNewReferenceFormat = true;
        }

        final String referencedSecurityAdvisories = amb.get(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES);
        if (referencedSecurityAdvisories != null) {
            this.addReferencedSecurityAdvisories(AeaaAdvisoryTypeStore.get().fromJsonMultipleReferencedIds(new JSONArray(referencedSecurityAdvisories)));
            foundNewReferenceFormat = true;
        }

        final String referencedOtherIds = amb.get(AdvisoryMetaData.Attribute.REFERENCED_OTHER);
        if (referencedOtherIds != null) {
            this.addOtherReferencedIds(AeaaOtherTypeStore.get().fromJsonMultipleReferencedIds(new JSONArray(referencedOtherIds)));
        }

        if (!foundNewReferenceFormat) {
            {
                final String legacyReferencedIds = amb.get(AdvisoryMetaData.Attribute.REFERENCED_IDS);
                if (legacyReferencedIds != null) {
                    appendLegacyReferencedIds(AeaaVulnerabilityTypeStore.parseLegacyJsonReferencedIds(new JSONObject(legacyReferencedIds)));
                }
            }
            {
                final String legacyReferencedIds = amb.get(AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
                if (legacyReferencedIds != null) {
                    appendLegacyReferencedIds(AeaaVulnerabilityTypeStore.parseLegacyJsonReferencedIds(new JSONObject(legacyReferencedIds)));
                }
            }
        }

        final String matchingSource = amb.get(AdvisoryMetaData.Attribute.MATCHING_SOURCE);
        if (matchingSource != null) {
            AeaaDataSourceIndicator.fromJson(new JSONArray(matchingSource))
                    .forEach(this::addMatchingSource);
        }

        final String dataFillingSources = amb.get(AdvisoryMetaData.Attribute.DATA_SOURCE);
        if (dataFillingSources != null) {
            Arrays.stream(dataFillingSources.split(", ?"))
                    .filter(StringUtils::hasText)
                    .distinct()
                    .forEach(this::addDataSourceFromSourceString);
        }
    }

    @Override
    public void appendToBaseModel(AMB modelBase) {
        super.appendToBaseModel(modelBase);

        if (!this.referencedVulnerabilities.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey(), AeaaContentIdentifierStore.toJson(this.referencedVulnerabilities).toString());
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey(), null);
        }

        if (!this.referencedSecurityAdvisories.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey(), AeaaContentIdentifierStore.toJson(this.referencedSecurityAdvisories).toString());
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey(), null);
        }

        if (!this.referencedOtherIds.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey(), AeaaContentIdentifierStore.toJson(this.referencedOtherIds).toString());
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey(), null);
        }

        final AeaaContentIdentifierStore.AeaaContentIdentifier sourceIdentifier = this.getSourceIdentifier();
        final String sourceKey = modelBase instanceof VulnerabilityMetaData ? VulnerabilityMetaData.Attribute.SOURCE.getKey() : AdvisoryMetaData.Attribute.SOURCE.getKey();
        final String sourceImplementationKey = modelBase instanceof VulnerabilityMetaData ? VulnerabilityMetaData.Attribute.SOURCE_IMPLEMENTATION.getKey() : AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION.getKey();
        if (sourceIdentifier != null) {
            modelBase.set(sourceKey, sourceIdentifier.getName());
            modelBase.set(sourceImplementationKey, sourceIdentifier.getImplementation());
        } else {
            LOG.warn("[{}] does not have source to write into [{}] when converting to abstract model base", this.getId(), sourceKey);
            modelBase.set(sourceKey, null);
            modelBase.set(sourceImplementationKey, null);
        }

        final JSONObject legacyReferencedIds = AeaaContentIdentifierStore.mergeIntoLegacyJson(Arrays.asList(this.referencedVulnerabilities, this.referencedSecurityAdvisories, this.referencedOtherIds));
        if (!legacyReferencedIds.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey(), legacyReferencedIds.toString());
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey(), null);
        }

        if (!this.matchingSources.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(), AeaaDataSourceIndicator.toJson(this.matchingSources).toString());

            // extract all CPE from the CPE sources into VulnerabilityMetaData.Attribute.PRODUCT_URIS
            final Set<String> productUris = this.matchingSources.stream()
                    .filter(Objects::nonNull)
                    .map(AeaaDataSourceIndicator::getMatchReason)
                    .filter(s -> s instanceof AeaaDataSourceIndicator.ArtifactCpeReason)
                    .map(s -> ((AeaaDataSourceIndicator.ArtifactCpeReason) s).getCpe())
                    .collect(Collectors.toSet());
            if (!productUris.isEmpty()) {
                modelBase.set(VulnerabilityMetaData.Attribute.PRODUCT_URIS.getKey(), String.join(", ", productUris));
            } else {
                modelBase.set(VulnerabilityMetaData.Attribute.PRODUCT_URIS.getKey(), null);
            }

        } else {
            modelBase.set(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(), null);
        }

        if (!this.dataSources.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(), this.dataSources.stream().map(AeaaContentIdentifierStore.AeaaContentIdentifier::getName).collect(Collectors.joining(", ")));
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(), null);
        }
    }

    @Override
    public void appendFromDataClass(DC dataClass) {
        super.appendFromDataClass(dataClass);

        this.addReferencedVulnerabilities(dataClass.getReferencedVulnerabilities());
        this.addReferencedSecurityAdvisories(dataClass.getReferencedSecurityAdvisories());
        this.addOtherReferencedIds(dataClass.getReferencedOtherIds());

        this.matchingSources.addAll(dataClass.getMatchingSources());
        this.dataSources.addAll(dataClass.getDataSources());
    }

    @Override
    public void appendFromMap(Map<String, Object> input) {
        super.appendFromMap(input);

        if (input.containsKey("referencedVulnerabilities") && input.get("referencedVulnerabilities") instanceof List) {
            this.addReferencedVulnerabilities(AeaaVulnerabilityTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedVulnerabilities")));
        }

        if (input.containsKey("referencedSecurityAdvisories") && input.get("referencedSecurityAdvisories") instanceof List) {
            this.addReferencedSecurityAdvisories(AeaaAdvisoryTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedSecurityAdvisories")));
        }

        if (input.containsKey("referencedOtherIds") && input.get("referencedOtherIds") instanceof List) {
            this.addOtherReferencedIds(AeaaOtherTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedOtherIds")));
        }

        if (input.containsKey("referencedIds") && input.get("referencedIds") instanceof Map) {
            appendLegacyReferencedIds(AeaaVulnerabilityTypeStore.parseLegacyJsonReferencedIds((Map<String, Collection<String>>) input.get("referencedIds")));
        }

        if (input.containsKey("dataSources")) {
            final String sources = input.get("dataSources").toString();
            if (StringUtils.hasText(sources)) {
                for (String source : sources.split(", ?")) {
                    this.addDataSourceFromSourceString(source);
                }
            }
        }

        if (input.containsKey("matchingSources")) {
            final String sources = input.get("matchingSources").toString();
            AeaaDataSourceIndicator.fromJson(new JSONArray(sources)).forEach(this::addMatchingSource);
        }
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);

        final AeaaContentIdentifierStore.AeaaContentIdentifier sourceIdentifier = this.getSourceIdentifier();
        if (sourceIdentifier != null) {
            json.put("source", sourceIdentifier.getName());
            json.put("sourceImplementation", sourceIdentifier.getImplementation());
        }

        if (!this.referencedVulnerabilities.isEmpty()) {
            json.put("referencedVulnerabilities", AeaaContentIdentifierStore.toJson(this.referencedVulnerabilities));
        }
        if (!this.referencedSecurityAdvisories.isEmpty()) {
            json.put("referencedSecurityAdvisories", AeaaContentIdentifierStore.toJson(this.referencedSecurityAdvisories));
        }
        if (!this.referencedOtherIds.isEmpty()) {
            json.put("referencedOtherIds", AeaaContentIdentifierStore.toJson(this.referencedOtherIds));
        }

        final JSONObject legacyReferencedIds = AeaaContentIdentifierStore.mergeIntoLegacyJson(Arrays.asList(this.referencedVulnerabilities, this.referencedSecurityAdvisories, this.referencedOtherIds));
        if (!legacyReferencedIds.isEmpty()) {
            json.put("referencedIds", legacyReferencedIds);
        }

        json.put("dataSources", getDataSources().stream().map(AeaaContentIdentifierStore.AeaaContentIdentifier::getName).collect(Collectors.joining(", ")));
        json.put("matchingSources", AeaaDataSourceIndicator.toJson(getMatchingSources()).toString());
    }

    private void addDataSourceFromSourceString(String source) {
        final AeaaContentIdentifierStore.AeaaContentIdentifier dataSource = this.findDataSourceFromSourceString(source);
        if (dataSource != null) {
            this.addDataSource(dataSource);
        }
    }

    private AeaaContentIdentifierStore.AeaaContentIdentifier findDataSourceFromSourceString(String source) {
        final AeaaVulnerabilityTypeIdentifier<?> vulnerabilityTypeIdentifier = AeaaVulnerabilityTypeStore.get().fromNameWithoutCreation(source);
        final AeaaAdvisoryTypeIdentifier<?> advisoryTypeIdentifier = AeaaAdvisoryTypeStore.get().fromNameWithoutCreation(source);

        if (vulnerabilityTypeIdentifier != null) {
            return vulnerabilityTypeIdentifier;
        } else if (advisoryTypeIdentifier != null) {
            return advisoryTypeIdentifier;
        } else {
            if (!"NVD".equals(source)) {
                LOG.warn("Could not find source identifier for [{}] on [{}]", source, this.getId());
            }
            return null;
        }
    }

    private void appendLegacyReferencedIds(Map<AeaaContentIdentifierStore.AeaaContentIdentifier, Set<String>> legacyReferencedIds) {
        this.addReferencedVulnerabilities(legacyReferencedIds.entrySet().stream()
                .filter(e -> e.getKey() instanceof AeaaVulnerabilityTypeIdentifier)
                .collect(Collectors.toMap(e -> (AeaaVulnerabilityTypeIdentifier<?>) e.getKey(), Map.Entry::getValue)));
        this.addReferencedSecurityAdvisories(legacyReferencedIds.entrySet().stream()
                .filter(e -> e.getKey() instanceof AeaaAdvisoryTypeIdentifier)
                .collect(Collectors.toMap(e -> (AeaaAdvisoryTypeIdentifier<?>) e.getKey(), Map.Entry::getValue)));
        this.addOtherReferencedIds(legacyReferencedIds.entrySet().stream()
                .filter(e -> e.getKey() instanceof AeaaOtherTypeIdentifier)
                .collect(Collectors.toMap(e -> (AeaaOtherTypeIdentifier) e.getKey(), Map.Entry::getValue)));
    }
}
