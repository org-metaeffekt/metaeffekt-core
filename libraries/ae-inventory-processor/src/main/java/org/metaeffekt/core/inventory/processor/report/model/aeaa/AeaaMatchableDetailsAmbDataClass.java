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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaAttackPattern;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaCapecEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaWeakness;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.*;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaThreatReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"affectedArtifacts", "weaknesses", "attackPatterns"})
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
        add(VulnerabilityMetaData.Attribute.REFERENCED_THREAT_MODEL_ELEMENTS.getKey());
        add(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey());
        add(VulnerabilityMetaData.Attribute.WEAKNESS.getKey());
        add(VulnerabilityMetaData.Attribute.WEAKNESS_DATA.getKey());
        add(VulnerabilityMetaData.Attribute.CAPEC_DATA.getKey());
        add(VulnerabilityMetaData.Attribute.REFERENCED_OTHER.getKey());

        add(AeaaInventoryAttribute.RETAINED_VULNERABLE_SOFTWARE_CONFIGURATIONS.getKey());
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAmbDataClass.CONVERSION_KEYS_MAP) {{
        addAll(Arrays.asList(
                "source", "sourceImplementation",
                "dataFillingSources", "matchingSources",
                "referencedIds", // legacy property
                "referencedSecurityAdvisories", "referencedVulnerabilities",
                "referencedWeaknesses", "referencedAttackPatterns", "referencedOtherIds",
                "cwe", "cweData", "capecData",
                "retainedVulnerableSoftwareConfigurations"
        ));
    }};

    protected final Map<AeaaVulnerabilityTypeIdentifier<?>, Set<String>> referencedVulnerabilities = new HashMap<>();
    protected final Map<AeaaAdvisoryTypeIdentifier<?>, Set<String>> referencedSecurityAdvisories = new HashMap<>();
    protected final Map<AeaaThreatTypeIdentifier<?>, Set<String>> referencedThreats = new HashMap<>();
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

    @Getter
    private final Set<AeaaWeakness> weaknesses = new HashSet<>();

    @Getter
    private final Set<AeaaAttackPattern> attackPatterns = new HashSet<>();

    public abstract AeaaContentIdentifierStore.AeaaContentIdentifier getSourceIdentifier();

    public DC addMatchingSource(AeaaDataSourceIndicator matchingSource) {
        try {
            if (matchingSource == null) {
                LOG.warn("Attempted to add null matching source to {}", this.getClass().getSimpleName());
                return (DC) this;
            }
            synchronized (this.matchingSources) {
                this.matchingSources.add(matchingSource);
            }
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

    public DC addDataSource(AeaaContentIdentifierStore.AeaaContentIdentifier dataSource) {
        this.dataSources.add(dataSource);
        return (DC) this;
    }

    public Set<Artifact> getAffectedArtifactsByKey(String key) {
        synchronized (this.affectedArtifacts) {
            return affectedArtifacts.getOrDefault(key, Collections.emptySet());
        }
    }

    public Set<Artifact> getAffectedArtifactsByDefaultKey() {
        return getAffectedArtifactsByKey(Artifact.Attribute.VULNERABILITY.getKey());
    }

    public DC manuallyAffectsArtifact(String key, Artifact artifact) {
        if (artifact != null) {
            synchronized (this.affectedArtifacts) {
                this.affectedArtifacts.computeIfAbsent(key, k -> new HashSet<>())
                        .add(artifact);
            }

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

    public DC manuallyAffectsArtifact(AbstractModelBase.Attribute key, Artifact artifact) {
        return manuallyAffectsArtifact(key.getKey(), artifact);
    }

    public DC removeAffectsArtifact(String key, Artifact artifact) {
        if (artifact != null) {
            synchronized (this.affectedArtifacts) {
                this.affectedArtifacts.computeIfAbsent(key, k -> new HashSet<>())
                        .remove(artifact);
            }

            final String presentVulnerabilities = artifact.get(key);
            final Set<String> vulnerabilities = StringUtils.hasText(presentVulnerabilities) ? Arrays.stream(presentVulnerabilities.split(", ")).collect(Collectors.toSet()) : new HashSet<>();
            vulnerabilities.remove(this.getId());
            artifact.set(key, String.join(", ", vulnerabilities));
        }
        return (DC) this;
    }

    public void addWeaknessUnmanaged(AeaaWeakness weakness) {
        this.weaknesses.add(weakness);
        this.addReferencedWeaknessId(weakness);
    }

    public void addAttackPatternUnmanaged(AeaaAttackPattern attackPattern) {
        this.attackPatterns.add(attackPattern);
        this.addReferencedAttackPatternId(attackPattern);
    }

    // START: MANAGE REFERENCED SECURITY ADVISORIES

    public void addReferencedSecurityAdvisory(AeaaAdvisoryTypeIdentifier<?> source, String id) {
        if (source == null || id == null) {
            LOG.warn("Cannot add referenced security advisory with null source or id on advisory [{}]", this.id);
            return;
        }
        synchronized (this.referencedSecurityAdvisories) {
            this.referencedSecurityAdvisories.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(id);
        }
    }

    public void addReferencedSecurityAdvisories(Map<AeaaAdvisoryTypeIdentifier<?>, Set<String>> referencedSecurityAdvisories) {
        synchronized (this.referencedSecurityAdvisories) {
            for (Map.Entry<AeaaAdvisoryTypeIdentifier<?>, Set<String>> entry : referencedSecurityAdvisories.entrySet()) {
                this.referencedSecurityAdvisories.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
            }
        }
    }

    public void addReferencedSecurityAdvisories(AeaaAdvisoryTypeIdentifier<?> source, Set<String> referencedSecurityAdvisories) {
        synchronized (this.referencedSecurityAdvisories) {
            this.referencedSecurityAdvisories.computeIfAbsent(source, k -> new LinkedHashSet<>()).addAll(referencedSecurityAdvisories);
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
        synchronized (this.referencedSecurityAdvisories) {
            this.referencedSecurityAdvisories.computeIfPresent(source, (k, v) -> {
                v.remove(id);
                return v;
            });

            if (this.referencedSecurityAdvisories.get(source) == null || this.referencedSecurityAdvisories.get(source).isEmpty()) {
                this.referencedSecurityAdvisories.remove(source);
            }
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

    public void addReferencedVulnerabilities(AeaaVulnerabilityTypeIdentifier<?> source, Collection<String> referencedVulnerabilities) {
        this.referencedVulnerabilities.computeIfAbsent(source, k -> new LinkedHashSet<>()).addAll(referencedVulnerabilities);
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

    // START: MANAGE REFERENCED THREATS

    public void addReferencedThreatId(AeaaThreatTypeIdentifier<? extends AeaaThreatReference> source, String id) {
        if (source == null || id == null) {
            LOG.warn("Cannot add referenced threat id with null source or id [{}]", this.id);
            return;
        }
        this.referencedThreats.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(id);
    }

    public void addReferencedThreatIds(Map<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>, Set<String>> referencedThreatIds) {
        for (Map.Entry<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>, Set<String>> entry : referencedThreatIds.entrySet()) {
            this.referencedThreats.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).addAll(entry.getValue());
        }
    }

    public void addReferencedThreatIds(AeaaThreatTypeIdentifier<? extends AeaaThreatReference> source, Collection<String> referencedOtherIds) {
        this.referencedThreats.computeIfAbsent(source, k -> new LinkedHashSet<>()).addAll(referencedOtherIds);
    }

    public void addReferencedWeaknessId(AeaaWeakness weakness) {
        this.referencedThreats.computeIfAbsent((AeaaThreatTypeIdentifier<? extends AeaaThreatReference>) weakness.getSourceIdentifier(), k -> new LinkedHashSet<>()).add(weakness.getId());
    }

    public void addReferencedAttackPatternId(AeaaAttackPattern attackPattern) {
        this.referencedThreats.computeIfAbsent((AeaaThreatTypeIdentifier<? extends AeaaThreatReference>) attackPattern.getSourceIdentifier(), k -> new LinkedHashSet<>()).add(attackPattern.getId());
    }

    public void removeReferencedThreatId(AeaaThreatTypeIdentifier<? extends AeaaThreatReference> source, String id) {
        synchronized (this.referencedThreats) {
            this.referencedThreats.computeIfPresent(source, (k, v) -> {
                v.remove(id);
                return v;
            });

            if (this.referencedThreats.get(source) == null || this.referencedThreats.get(source).isEmpty()) {
                this.referencedThreats.remove(source);
            }
        }
    }

    public void removeAllReferencedWeaknesses() {
        synchronized (this.referencedThreats) {
            this.referencedThreats.keySet().stream().filter(AeaaThreatTypeIdentifier::isWeakness).forEach(referencedThreats::remove);
        }
    }

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

    public Set<String> getReferencedSecurityAdvisories(AeaaAdvisoryTypeIdentifier<?> source) {
        synchronized (this.referencedSecurityAdvisories) {
            return this.referencedSecurityAdvisories.getOrDefault(source, Collections.emptySet());
        }
    }

    public Set<String> getReferencedVulnerabilities(AeaaVulnerabilityTypeIdentifier<?> source) {
        synchronized (this.referencedVulnerabilities) {
            return this.referencedVulnerabilities.getOrDefault(source, Collections.emptySet());
        }
    }

    public Map<AeaaThreatTypeIdentifier<? extends AeaaWeakness>, Set<String>> getAllReferencedWeaknessIds() {
        synchronized (this.referencedThreats) {
            return this.referencedThreats.entrySet().stream()
                    .filter(entry -> entry.getKey().isWeakness())
                    .collect(Collectors.toMap(entry -> ((AeaaThreatTypeIdentifier<? extends AeaaWeakness>) entry.getKey()),
                            Map.Entry::getValue));
        }
    }

    public Map<AeaaThreatTypeIdentifier<? extends AeaaAttackPattern>, Set<String>> getAllReferencedAttackPatternIds() {
        synchronized (this.referencedThreats) {
            return this.referencedThreats.entrySet().stream()
                    .filter(entry -> entry.getKey().isAttackPattern())
                    .collect(Collectors.toMap(entry -> ((AeaaThreatTypeIdentifier<? extends AeaaAttackPattern>) entry.getKey()),
                            Map.Entry::getValue));
        }
    }

    public Map<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>, Set<String>> getAllReferencedThreatModelIds() {
        synchronized (this.referencedThreats) {
            return new HashMap<>(this.referencedThreats);
        }
    }

    public Set<String> getReferencedWeaknessIds(AeaaThreatTypeIdentifier<? extends AeaaWeakness> source) {
        synchronized (this.referencedThreats) {
            return this.referencedThreats.getOrDefault(source, Collections.emptySet());
        }
    }

    public Set<String> getAllWeaknessIds() {
        synchronized (this.referencedThreats) {
            return referencedThreats.keySet().stream().filter(AeaaThreatTypeIdentifier::isWeakness).flatMap(key -> (referencedThreats.getOrDefault(key, Collections.emptySet()).stream())).collect(Collectors.toSet());
        }
    }

    public Set<String> getReferencedAttackPatternIds(AeaaThreatTypeIdentifier<? extends AeaaAttackPattern> source) {
        synchronized (this.referencedThreats) {
            return this.referencedThreats.getOrDefault(source, Collections.emptySet());
        }
    }


    public Set<String> getReferencedOtherIds(AeaaOtherTypeIdentifier source) {
        synchronized (this.referencedOtherIds) {
            return this.referencedOtherIds.getOrDefault(source, Collections.emptySet());
        }
    }

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

        final String referencedThreats = amb.get(VulnerabilityMetaData.Attribute.REFERENCED_THREAT_MODEL_ELEMENTS);
        if (referencedThreats != null) {
            this.addReferencedThreatIds(AeaaThreatTypeStore.get().fromJsonMultipleReferencedIds(new JSONArray(referencedThreats)));
        }

        final String referencedOtherIds = amb.get(AdvisoryMetaData.Attribute.REFERENCED_OTHER);
        if (referencedOtherIds != null) {
            extractOtherIdsAndPotentialThreatIds(referencedOtherIds);
        }

        if (!foundNewReferenceFormat) {
            {
                final String legacyReferencedIds = amb.get(AdvisoryMetaData.Attribute.REFERENCED_IDS);
                if (legacyReferencedIds != null) {
                    appendLegacyReferencedIds(AeaaContentIdentifierStore.parseLegacyJsonReferencedIds(new JSONObject(legacyReferencedIds)));
                }
            }
            {
                final String legacyReferencedIds = amb.get(AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
                if (legacyReferencedIds != null) {
                    appendLegacyReferencedIds(AeaaContentIdentifierStore.parseLegacyJsonReferencedIds(new JSONObject(legacyReferencedIds)));
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

        if (StringUtils.hasText(amb.get(VulnerabilityMetaData.Attribute.WEAKNESS_DATA))) {
            for (AeaaWeakness entry : AeaaWeakness.fromJson(new JSONArray(amb.get(VulnerabilityMetaData.Attribute.WEAKNESS_DATA)))) {
                this.addReferencedWeaknessId(entry);
            }
        } else if (StringUtils.hasText(amb.get(VulnerabilityMetaData.Attribute.WEAKNESS))) {
            for (String id : Arrays.asList(amb.get(VulnerabilityMetaData.Attribute.WEAKNESS).split(", ?"))) {
                Optional<AeaaThreatTypeIdentifier<?>> threatTypeIdentifier = AeaaThreatTypeStore.get().fromId(id);
                threatTypeIdentifier.ifPresent(type -> this.addReferencedThreatId(type, id));
            }
        }

        if (StringUtils.hasText(amb.get(VulnerabilityMetaData.Attribute.CAPEC_DATA))) {
            for (AeaaAttackPattern entry : AeaaCapecEntry.fromJson(new JSONArray(amb.get(VulnerabilityMetaData.Attribute.CAPEC_DATA)))) {
                this.addReferencedThreatId(AeaaThreatTypeStore.CAPEC, entry.getId());
            }
        }
    }

    @Override
    public void appendToBaseModel(AMB modelBase) {
        super.appendToBaseModel(modelBase);

        synchronized (this.referencedVulnerabilities) {
            if (!this.referencedVulnerabilities.isEmpty()) {
                modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey(), AeaaContentIdentifierStore.toJson(this.referencedVulnerabilities).toString());
            } else {
                modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey(), null);
            }
        }

        synchronized (this.referencedSecurityAdvisories) {
            if (!this.referencedSecurityAdvisories.isEmpty()) {
                modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey(), AeaaContentIdentifierStore.toJson(this.referencedSecurityAdvisories).toString());
            } else {
                modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey(), null);
            }
        }

        if (!this.referencedThreats.isEmpty()) {
            modelBase.set(VulnerabilityMetaData.Attribute.REFERENCED_THREAT_MODEL_ELEMENTS.getKey(), AeaaContentIdentifierStore.toJson(this.referencedThreats).toString());
        } else {
            modelBase.set(VulnerabilityMetaData.Attribute.REFERENCED_THREAT_MODEL_ELEMENTS.getKey(), null);
        }

        if (!this.referencedOtherIds.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey(), AeaaContentIdentifierStore.toJson(this.referencedOtherIds).toString());
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_OTHER.getKey(), null);
        }

        if (!this.getReferencedWeaknessIds(AeaaThreatTypeStore.CWE).isEmpty()) {
            modelBase.set(VulnerabilityMetaData.Attribute.WEAKNESS, String.join(", ", this.getReferencedWeaknessIds(AeaaThreatTypeStore.CWE)));
        } else {
            modelBase.set(VulnerabilityMetaData.Attribute.WEAKNESS, null);
        }

        final AeaaContentIdentifierStore.AeaaContentIdentifier sourceIdentifier = this.getSourceIdentifier();
        final String sourceKey = modelBase instanceof VulnerabilityMetaData ? VulnerabilityMetaData.Attribute.SOURCE.getKey() : AdvisoryMetaData.Attribute.SOURCE.getKey();
        final String sourceImplementationKey = modelBase instanceof VulnerabilityMetaData ? VulnerabilityMetaData.Attribute.SOURCE_IMPLEMENTATION.getKey() : AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION.getKey();
        if (sourceIdentifier != null) {
            modelBase.set(sourceKey, sourceIdentifier.getName());
            modelBase.set(sourceImplementationKey, sourceIdentifier.getImplementation());
        } else {
            LOG.warn("[{}] does not have source to write into [{}] when converting to [{}]", this.getId(), sourceKey, modelBase.getClass().getSimpleName());
            modelBase.set(sourceKey, null);
            modelBase.set(sourceImplementationKey, null);
        }

        final JSONObject legacyReferencedIds = AeaaContentIdentifierStore.mergeIntoLegacyJson(Arrays.asList(this.referencedVulnerabilities, this.referencedSecurityAdvisories, this.referencedOtherIds));
        if (!legacyReferencedIds.isEmpty()) {
            modelBase.set(AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS.getKey(), legacyReferencedIds.toString());
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey(), legacyReferencedIds.toString());
        } else {
            modelBase.set(AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS.getKey(), null);
            modelBase.set(AdvisoryMetaData.Attribute.REFERENCED_IDS.getKey(), null);
        }

        synchronized (this.matchingSources) {
            if (!this.matchingSources.isEmpty()) {
                modelBase.set(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(), AeaaDataSourceIndicator.toJson(this.matchingSources).toString());

                // extract all CPE from the CPE sources into VulnerabilityMetaData.Attribute.PRODUCT_URIS
                try {
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
                } catch (Exception e) {
                    LOG.error("Error while converting to [{}] while extracting product URIs from CPE sources for [{}]: {}", modelBase.getClass().getSimpleName(), this.getId(), this.matchingSources, e);
                }

            } else {
                modelBase.set(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(), null);
            }
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
        this.addReferencedThreatIds(dataClass.getReferencedThreats());

        this.addOtherReferencedIds(dataClass.getReferencedOtherIds());

        this.matchingSources.addAll(dataClass.getMatchingSources());
        this.dataSources.addAll(dataClass.getDataSources());
    }

    @Override
    public void appendFromMap(Map<String, Object> input) {
        super.appendFromMap(input);

        boolean hadReferencedIds = false;
        if (input.containsKey("referencedVulnerabilities") && input.get("referencedVulnerabilities") instanceof List) {
            this.addReferencedVulnerabilities(AeaaVulnerabilityTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedVulnerabilities")));
            hadReferencedIds = true;
        }

        if (input.containsKey("referencedSecurityAdvisories") && input.get("referencedSecurityAdvisories") instanceof List) {
            this.addReferencedSecurityAdvisories(AeaaAdvisoryTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedSecurityAdvisories")));
            hadReferencedIds = true;
        }

        if (input.containsKey("referencedWeaknesses") && input.get("referencedWeaknesses") instanceof List) {
            this.addReferencedThreatIds(AeaaThreatTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedWeaknesses")));
        }

        if (input.containsKey("referencedAttackPatterns") && input.get("referencedAttackPatterns") instanceof List) {
            this.addReferencedThreatIds(AeaaThreatTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedAttackPatterns")));
        }

        if (input.containsKey("referencedThreats") && input.get("referencedThreats") instanceof List) {
            this.addReferencedThreatIds(AeaaThreatTypeStore.get().fromListMultipleReferencedIds((List<Map<String, Object>>) input.get("referencedThreats")));
        }

        if (input.containsKey("referencedOtherIds") && input.get("referencedOtherIds") instanceof List) {
            Map<AeaaContentIdentifierStore.AeaaContentIdentifier, Set<String>> referencedOtherIdsAndThreatIds = AeaaOtherTypeStore.get().fromListMultipleReferencedIdsConvertDeprecated((List<Map<String, Object>>) input.get("referencedOtherIds"));
            for (AeaaContentIdentifierStore.AeaaContentIdentifier contentIdentifier : referencedOtherIdsAndThreatIds.keySet()) {
                if (contentIdentifier instanceof AeaaThreatTypeIdentifier) {
                    this.addReferencedThreatIds((AeaaThreatTypeIdentifier<? extends AeaaThreatReference>) contentIdentifier, referencedOtherIdsAndThreatIds.get(contentIdentifier));
                } else if (contentIdentifier instanceof AeaaOtherTypeIdentifier) {
                    this.addOtherReferencedIds((AeaaOtherTypeIdentifier) contentIdentifier, referencedOtherIdsAndThreatIds.get(contentIdentifier));
                }
            }
            hadReferencedIds = true;
        }

        if (!hadReferencedIds && input.containsKey("referencedIds") && input.get("referencedIds") instanceof Map) {
            appendLegacyReferencedIds(AeaaContentIdentifierStore.parseLegacyJsonReferencedIds((Map<String, Collection<String>>) input.get("referencedIds")));
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

        final Map<AeaaThreatTypeIdentifier<? extends AeaaWeakness>, Set<String>> weaknessIds = getAllReferencedWeaknessIds();
        final Map<AeaaThreatTypeIdentifier<? extends AeaaAttackPattern>, Set<String>> attackPatternIds = getAllReferencedAttackPatternIds();

        if (!weaknessIds.isEmpty()) {
            json.put("referencedWeaknesses", AeaaContentIdentifierStore.toJson(weaknessIds));
        }
        if (!attackPatternIds.isEmpty()) {
            json.put("referencedAttackPatterns", AeaaContentIdentifierStore.toJson(attackPatternIds));
        }
        if (!this.referencedOtherIds.isEmpty()) {
            json.put("referencedOtherIds", AeaaContentIdentifierStore.toJson(this.referencedOtherIds));
        }

        final JSONObject legacyReferencedIds = AeaaContentIdentifierStore.mergeIntoLegacyJson(Arrays.asList(this.referencedVulnerabilities, this.referencedSecurityAdvisories));
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

    private void extractOtherIdsAndPotentialThreatIds(String referencedOtherIds) {
        Map<AeaaContentIdentifierStore.AeaaContentIdentifier, Set<String>> referencedOtherIdsAndThreatIds = AeaaOtherTypeStore.get().fromJsonMultipleReferencedIdsConvertDeprecated(new JSONArray(referencedOtherIds));
        for (AeaaContentIdentifierStore.AeaaContentIdentifier contentIdentifier : referencedOtherIdsAndThreatIds.keySet()) {
            if (contentIdentifier instanceof AeaaThreatTypeIdentifier) {
                this.addReferencedThreatIds((AeaaThreatTypeIdentifier<? extends AeaaThreatReference>) contentIdentifier, referencedOtherIdsAndThreatIds.get(contentIdentifier));
            } else if (contentIdentifier instanceof AeaaOtherTypeIdentifier) {
                this.addOtherReferencedIds((AeaaOtherTypeIdentifier) contentIdentifier, referencedOtherIdsAndThreatIds.get(contentIdentifier));
            }
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
