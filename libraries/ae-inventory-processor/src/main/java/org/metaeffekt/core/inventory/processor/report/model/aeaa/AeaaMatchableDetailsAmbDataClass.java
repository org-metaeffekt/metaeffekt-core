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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
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
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAmbDataClass.CONVERSION_KEYS_MAP) {{
        add("dataFillingSources");
        add("matchingSources");
    }};


    protected final List<AeaaDataSourceIndicator> matchingSources = new ArrayList<>();
    protected final Set<AeaaContentIdentifiers> dataSources = new HashSet<>();

    /**
     * Artifacts that are affected by this entry.<br>
     * Will not be written back into the inventory as a field, but rather as references from the artifact to this entry.
     * <pre>
     * Key:   Artifact field name
     * Value: Set&lt;Artifact&gt; affected artifacts
     * </pre>
     */
    protected final Map<String, Set<Artifact>> affectedArtifacts = new HashMap<>();

    public DC addMatchingSource(AeaaDataSourceIndicator matchingSource) {
        this.matchingSources.add(matchingSource);
        if (matchingSource.getMatchReason() instanceof AeaaDataSourceIndicator.ArtifactReason) {
            final AeaaDataSourceIndicator.ArtifactReason artifactReason = (AeaaDataSourceIndicator.ArtifactReason) matchingSource.getMatchReason();
            if (artifactReason.hasArtifact()) {
                this.manuallyAffectsArtifact(Artifact.Attribute.VULNERABILITY.getKey(), artifactReason.getArtifact());
            }
        }
        return (DC) this;
    }

    public List<AeaaDataSourceIndicator> getMatchingSources() {
        return matchingSources;
    }

    public DC addDataSource(AeaaContentIdentifiers dataSource) {
        this.dataSources.add(dataSource);
        return (DC) this;
    }

    public Set<AeaaContentIdentifiers> getDataSources() {
        return dataSources;
    }

    public Set<String> getWellFormedDataSources() {
        return dataSources.stream().map(AeaaContentIdentifiers::getWellFormedName).collect(Collectors.toSet());
    }

    public AeaaContentIdentifiers getEntrySource() {
        for (AeaaContentIdentifiers sourceIdentifier : AeaaContentIdentifiers.values()) {
            if (sourceIdentifier.isAdvisoryProvider() && sourceIdentifier.getAdvisoryEntryClass() == this.getClass()) {
                return sourceIdentifier;
            }
        }

        final AeaaContentIdentifiers idSource = AeaaContentIdentifiers.fromEntryIdentifier(id);

        if (idSource == AeaaContentIdentifiers.CVE && !dataSources.isEmpty()) {
            final AeaaContentIdentifiers firstDataSource = dataSources.iterator().next();
            if (firstDataSource != AeaaContentIdentifiers.CVE) {
                return firstDataSource;
            }
        }

        if (idSource != AeaaContentIdentifiers.UNKNOWN) {
            return idSource;
        } else if (!dataSources.isEmpty()) {
            LOG.warn("Could not infer advisor source from id: {}", id);
            return dataSources.iterator().next();
        } else {
            LOG.warn("Could not infer advisor source from id [{}] and no data sources are set, setting to [{}]", id, AeaaContentIdentifiers.UNKNOWN.name());
            return AeaaContentIdentifiers.UNKNOWN;
        }
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

    /* DATA TYPE CONVERSION METHODS */

    @Override
    public void appendFromBaseModel(AMB vmd) {
        super.appendFromBaseModel(vmd);

        final String matchingSource = vmd.get(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey());
        if (matchingSource != null) {
            AeaaDataSourceIndicator.fromJson(new JSONArray(matchingSource))
                    .forEach(this::addMatchingSource);
        }

        final String dataFillingSources = vmd.get(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey());
        if (dataFillingSources != null) {
            Arrays.stream(dataFillingSources.split(", ?"))
                    .filter(StringUtils::hasText)
                    .distinct()
                    .map(AeaaContentIdentifiers::fromContentIdentifierName)
                    .forEach(this::addDataSource);
        }
    }

    @Override
    public void appendToBaseModel(AMB modelBase) {
        super.appendToBaseModel(modelBase);

        if (!this.matchingSources.isEmpty()) {
            modelBase.set(AdvisoryMetaData.Attribute.MATCHING_SOURCE.getKey(), AeaaDataSourceIndicator.toJson(this.matchingSources).toString());

            // extract all CPE from the CPE sources into VulnerabilityMetaData.Attribute.PRODUCT_URIS
            final Set<String> productUris = this.matchingSources.stream()
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
            modelBase.set(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(), this.dataSources.stream().map(AeaaContentIdentifiers::getWellFormedName).collect(Collectors.joining(", ")));
        } else {
            modelBase.set(AdvisoryMetaData.Attribute.DATA_SOURCE.getKey(), null);
        }
    }

    @Override
    public void appendFromDataClass(DC dataClass) {
        super.appendFromDataClass(dataClass);

        this.matchingSources.addAll(dataClass.getMatchingSources());
        this.dataSources.addAll(dataClass.getDataSources());
    }

    @Override
    public void appendFromMap(Map<String, Object> input) {
        super.appendFromMap(input);

        if (input.containsKey("dataSources")) {
            final String sources = input.get("dataSources").toString();
            for (String source : sources.split(", ?")) {
                this.addDataSource(AeaaContentIdentifiers.fromContentIdentifierName(source));
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

        json.put("dataSources", getDataSources().stream().map(AeaaContentIdentifiers::getWellFormedName).collect(Collectors.joining(", ")));
        json.put("matchingSources", AeaaDataSourceIndicator.toJson(getMatchingSources()).toString());
    }
}
