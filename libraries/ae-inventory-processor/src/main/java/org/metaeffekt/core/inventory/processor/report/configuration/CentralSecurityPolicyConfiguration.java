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
package org.metaeffekt.core.inventory.processor.report.configuration;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.configuration.ProcessConfiguration;
import org.metaeffekt.core.inventory.processor.configuration.ProcessMisconfiguration;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaContentIdentifiers;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.processor.CvssSelectionResult.CvssScoreVersionSelectionPolicy;
import org.metaeffekt.core.security.cvss.processor.CvssSelector;
import org.metaeffekt.core.security.cvss.processor.CvssSelector.*;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;

public class CentralSecurityPolicyConfiguration extends ProcessConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CentralSecurityPolicyConfiguration.class);

    private String cvssSeverityRanges = CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString();
    private CvssSeverityRanges cachedCvssSeverityRanges;

    private String initialCvssSelector = CVSS_SELECTOR_INITIAL.toJson().toString();
    private String contextCvssSelector = CVSS_SELECTOR_CONTEXT.toJson().toString();
    private CvssSelector cachedInitialCvssSelector;
    private CvssSelector cachedContextCvssSelector;

    private List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy = new ArrayList<>(Collections.singletonList(CvssScoreVersionSelectionPolicy.LATEST));

    private double insignificantThreshold = 7.0;
    private double includeScoreThreshold = -1.0;

    private final List<String> includeVulnerabilitiesWithAdvisoryProviders = new ArrayList<>(Collections.singletonList("all"));
    private final List<String> includeAdvisoryTypes = new ArrayList<>(Collections.singletonList("all"));

    private String vulnerabilityStatusDisplayMapperName = "default";
    private VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper = VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT;

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(String cvssSeverityRanges) {
        this.cvssSeverityRanges = cvssSeverityRanges == null ? CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString() : cvssSeverityRanges;
        this.cachedCvssSeverityRanges = new CvssSeverityRanges(this.cvssSeverityRanges);
        return this;
    }

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(CvssSeverityRanges cvssSeverityRanges) {
        this.cvssSeverityRanges = cvssSeverityRanges.toString();
        cachedCvssSeverityRanges = cvssSeverityRanges;
        return this;
    }

    public CvssSeverityRanges getCvssSeverityRanges() {
        if (this.cachedCvssSeverityRanges == null) {
            this.cvssSeverityRanges = this.cvssSeverityRanges == null ? CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString() : this.cvssSeverityRanges;
            this.cachedCvssSeverityRanges = new CvssSeverityRanges(this.cvssSeverityRanges);
        }
        return this.cachedCvssSeverityRanges;
    }

    public CentralSecurityPolicyConfiguration setInitialCvssSelector(JSONObject initialCvssSelector) {
        this.initialCvssSelector = initialCvssSelector.toString();
        this.cachedInitialCvssSelector = CvssSelector.fromJson(initialCvssSelector);
        return this;
    }

    public CentralSecurityPolicyConfiguration setBaseCvssSelector(CvssSelector baseCvssSelector) {
        this.initialCvssSelector = baseCvssSelector.toJson().toString();
        this.cachedInitialCvssSelector = baseCvssSelector;
        return this;
    }

    public CvssSelector getInitialCvssSelector() {
        if (this.cachedInitialCvssSelector == null) {
            this.initialCvssSelector = this.initialCvssSelector == null ? CVSS_SELECTOR_INITIAL.toJson().toString() : this.initialCvssSelector;
            this.cachedInitialCvssSelector = CvssSelector.fromJson(this.initialCvssSelector);
        }
        return this.cachedInitialCvssSelector;
    }

    public CentralSecurityPolicyConfiguration setContextCvssSelector(JSONObject contextCvssSelector) {
        this.contextCvssSelector = contextCvssSelector.toString();
        this.cachedContextCvssSelector = CvssSelector.fromJson(this.contextCvssSelector);
        return this;
    }

    public CentralSecurityPolicyConfiguration setEffectiveCvssSelector(CvssSelector effectiveCvssSelector) {
        this.contextCvssSelector = effectiveCvssSelector.toJson().toString();
        this.cachedContextCvssSelector = effectiveCvssSelector;
        return this;
    }

    public CvssSelector getContextCvssSelector() {
        if (this.cachedContextCvssSelector == null) {
            this.contextCvssSelector = this.contextCvssSelector == null ? CVSS_SELECTOR_CONTEXT.toJson().toString() : this.contextCvssSelector;
            this.cachedContextCvssSelector = CvssSelector.fromJson(this.contextCvssSelector);
        }
        return this.cachedContextCvssSelector;
    }

    public void setCvssVersionSelectionPolicy(List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy) {
        this.cvssVersionSelectionPolicy = cvssVersionSelectionPolicy;
    }

    public List<CvssScoreVersionSelectionPolicy> getCvssVersionSelectionPolicy() {
        return cvssVersionSelectionPolicy;
    }

    public CentralSecurityPolicyConfiguration setInsignificantThreshold(double insignificantThreshold) {
        this.insignificantThreshold = insignificantThreshold;
        return this;
    }

    public double getInsignificantThreshold() {
        return this.insignificantThreshold;
    }

    public CentralSecurityPolicyConfiguration setIncludeScoreThreshold(double includeScoreThreshold) {
        this.includeScoreThreshold = includeScoreThreshold;
        return this;
    }

    public boolean isVulnerabilityInsignificant(AeaaVulnerability vulnerability) {
        if (insignificantThreshold == -1.0) return true;
        final CvssVector vector = vulnerability.getCvssSelectionResult().getSelectedContextIfAvailableOtherwiseInitial();
        final double score = vector == null ? 0.0 : vector.getOverallScore();
        return score <= insignificantThreshold;
    }

    public double getIncludeScoreThreshold() {
        return this.includeScoreThreshold;
    }

    public boolean isVulnerabilityAboveIncludeScoreThreshold(AeaaVulnerability vulnerability) {
        if (includeScoreThreshold == -1.0 || includeScoreThreshold == Double.MIN_VALUE) return true;
        final CvssVector vector = vulnerability.getCvssSelectionResult().getSelectedContextIfAvailableOtherwiseInitial();
        final double score = vector == null ? 0.0 : vector.getOverallScore();
        return score >= includeScoreThreshold;
    }

    public boolean isVulnerabilityAboveIncludeScoreThreshold(double score) {
        if (includeScoreThreshold == -1.0 || includeScoreThreshold == Double.MIN_VALUE) return true;
        return score >= includeScoreThreshold;
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(List<String> includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders.clear();
        this.includeVulnerabilitiesWithAdvisoryProviders.addAll(includeVulnerabilitiesWithAdvisoryProviders);
        return this;
    }

    public List<String> getIncludeVulnerabilitiesWithAdvisoryProviders() {
        return includeVulnerabilitiesWithAdvisoryProviders;
    }

    public boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability) {
        if (includeVulnerabilitiesWithAdvisoryProviders.contains("all") || includeVulnerabilitiesWithAdvisoryProviders.contains("ALL")) {
            return true;
        }

        final List<AeaaContentIdentifiers> filter = includeVulnerabilitiesWithAdvisoryProviders.stream()
                .map(AeaaContentIdentifiers::fromContentIdentifierName)
                .collect(Collectors.toList());

        if (filter.contains(AeaaContentIdentifiers.UNKNOWN)) {
            LOG.warn("Unknown advisory provider in includeVulnerabilitiesWithAdvisoryProviders [{}], must be one of {}", includeVulnerabilitiesWithAdvisoryProviders, AeaaContentIdentifiers.values());
        }

        return isVulnerabilityIncludedRegardingAdvisoryProviders(vulnerability, filter);
    }

    public static List<AeaaVulnerability> filterVulnerabilitiesForAdvisories(Collection<AeaaVulnerability> vulnerabilities, Collection<AeaaContentIdentifiers> filter) {
        return vulnerabilities.stream()
                .filter(v -> isVulnerabilityIncludedRegardingAdvisoryProviders(v, filter))
                .collect(Collectors.toList());
    }

    public static boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability, Collection<AeaaContentIdentifiers> filter) {
        return vulnerability.getSecurityAdvisories().stream().anyMatch(a -> filter.contains(a.getEntrySource()));
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryTypes(List<String> includeAdvisoryTypes) {
        this.includeAdvisoryTypes.clear();
        this.includeAdvisoryTypes.addAll(includeAdvisoryTypes);
        return this;
    }

    public List<String> getIncludeAdvisoryTypes() {
        return includeAdvisoryTypes;
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntrySourceType(AeaaAdvisoryEntry advisory) {
        return isSecurityAdvisoryIncludedRegardingEntrySourceType(advisory.getType());
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntrySourceType(String advisoryType) {
        if (includeAdvisoryTypes.contains("all") || includeAdvisoryTypes.contains("ALL")) {
            return true;
        }
        return includeAdvisoryTypes.contains(advisoryType);
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(String vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper;
        this.vulnerabilityStatusDisplayMapper = CentralSecurityPolicyConfiguration.getStatusMapperByName(vulnerabilityStatusDisplayMapper);
        return this;
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper.getName();
        this.vulnerabilityStatusDisplayMapper = vulnerabilityStatusDisplayMapper;
        return this;
    }

    public VulnerabilityStatusMapper getVulnerabilityStatusDisplayMapper() {
        if (this.vulnerabilityStatusDisplayMapper == null
                || !this.vulnerabilityStatusDisplayMapperName.equals(this.vulnerabilityStatusDisplayMapper.getName())) {
            this.setVulnerabilityStatusDisplayMapper(this.vulnerabilityStatusDisplayMapperName);
        }
        return this.vulnerabilityStatusDisplayMapper;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        final LinkedHashMap<String, Object> configuration = new LinkedHashMap<>();

        configuration.put("cvssSeverityRanges", cvssSeverityRanges);
        configuration.put("initialCvssSelector", initialCvssSelector);
        configuration.put("contextCvssSelector", contextCvssSelector);
        configuration.put("insignificantThreshold", insignificantThreshold);
        configuration.put("includeScoreThreshold", includeScoreThreshold);
        configuration.put("includeVulnerabilitiesWithAdvisoryProviders", includeVulnerabilitiesWithAdvisoryProviders);
        configuration.put("includeAdvisoryTypes", includeAdvisoryTypes);
        configuration.put("vulnerabilityStatusDisplayMapperName", vulnerabilityStatusDisplayMapperName);
        configuration.put("cvssVersionSelectionPolicy", cvssVersionSelectionPolicy);

        return configuration;
    }

    @Override
    public void setProperties(LinkedHashMap<String, Object> properties) {
        super.loadStringProperty(properties, "cvssSeverityRanges", this::setCvssSeverityRanges);

        super.loadProperty(properties, "baseCvssSelector", this::parseJsonObjectFromProperties, selector -> setBaseCvssSelector(CvssSelector.fromJson(selector))); // deprecated
        super.loadProperty(properties, "initialCvssSelector", this::parseJsonObjectFromProperties, selector -> setBaseCvssSelector(CvssSelector.fromJson(selector)));
        super.loadProperty(properties, "effectiveCvssSelector", this::parseJsonObjectFromProperties, selector -> setEffectiveCvssSelector(CvssSelector.fromJson(selector))); // deprecated
        super.loadProperty(properties, "contextCvssSelector", this::parseJsonObjectFromProperties, selector -> setEffectiveCvssSelector(CvssSelector.fromJson(selector)));

        super.loadDoubleProperty(properties, "insignificantThreshold", this::setInsignificantThreshold);
        super.loadDoubleProperty(properties, "includeScoreThreshold", this::setIncludeScoreThreshold);
        super.loadListProperty(properties, "includeVulnerabilitiesWithAdvisoryProviders", String::valueOf, this::setIncludeVulnerabilitiesWithAdvisoryProviders);
        super.loadListProperty(properties, "includeAdvisoryTypes", String::valueOf, this::setIncludeAdvisoryTypes);
        super.loadStringProperty(properties, "vulnerabilityStatusDisplayMapperName", this::setVulnerabilityStatusDisplayMapper);
        super.loadListProperty(properties, "cvssVersionSelectionPolicy", value -> CvssScoreVersionSelectionPolicy.valueOf(String.valueOf(value)), this::setCvssVersionSelectionPolicy);
    }

    private JSONObject parseJsonObjectFromProperties(Object input) {
        if (input instanceof String) {
            return new JSONObject((String) input);
        } else if (input instanceof JSONObject) {
            return (JSONObject) input;
        } else if (input instanceof Map) {
            return new JSONObject((Map<?, ?>) input);
        } else {
            throw new IllegalArgumentException("Cannot parse JSON from input: " + input);
        }
    }

    @Override
    protected void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations) {
        if (this.cvssSeverityRanges == null) {
            misconfigurations.add(new ProcessMisconfiguration("cvssSeverityRanges", "CVSS severity ranges must not be null"));
        }
        if (this.cvssVersionSelectionPolicy == null || this.cvssVersionSelectionPolicy.isEmpty()) {
            misconfigurations.add(new ProcessMisconfiguration("cvssVersionSelectionPolicy", "CVSS version selection policy must not be null or empty"));
        }

        if (this.initialCvssSelector == null) {
            misconfigurations.add(new ProcessMisconfiguration("initialCvssSelector", "Initial CVSS selector must not be null"));
        }
        try {
            this.getInitialCvssSelector();
        } catch (IllegalArgumentException e) {
            misconfigurations.add(new ProcessMisconfiguration("initialCvssSelector", "Invalid initial selector syntax: " + initialCvssSelector));
        }
        if (contextCvssSelector == null) {
            misconfigurations.add(new ProcessMisconfiguration("contextCvssSelector", "Context CVSS selector must not be null"));
        }
        try {
            this.getContextCvssSelector();
        } catch (IllegalArgumentException e) {
            misconfigurations.add(new ProcessMisconfiguration("contextCvssSelector", "Invalid context selector syntax: " + contextCvssSelector));
        }

        if ((insignificantThreshold < 0.0 && insignificantThreshold != -1) || insignificantThreshold > 10.0) {
            misconfigurations.add(new ProcessMisconfiguration("insignificantThreshold", "Insignificant threshold must be between 0.0 and 10.0 or be -1.0"));
        }
        if ((includeScoreThreshold < 0.0 && includeScoreThreshold != -1) || includeScoreThreshold > 10.0) {
            misconfigurations.add(new ProcessMisconfiguration("includeScoreThreshold", "Include score threshold must be between 0.0 and 10.0 or be -1.0"));
        }

        try {
            this.getVulnerabilityStatusDisplayMapper();
        } catch (IllegalArgumentException e) {
            misconfigurations.add(new ProcessMisconfiguration("vulnerabilityStatusDisplayMapper", "Unknown status mapper: " + vulnerabilityStatusDisplayMapper));
        }

        for (String provider : includeVulnerabilitiesWithAdvisoryProviders) {
            if (provider == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeVulnerabilitiesWithAdvisoryProviders", "Advisory provider must not be null"));
            } else if ( !"all".equalsIgnoreCase(provider) && AeaaContentIdentifiers.fromContentIdentifierName(provider) == AeaaContentIdentifiers.UNKNOWN) {
                misconfigurations.add(new ProcessMisconfiguration("includeVulnerabilitiesWithAdvisoryProviders", "Unknown advisory provider: " + provider + ", must be one of " + Arrays.toString(AeaaContentIdentifiers.values())));
            }
        }

        for (String advisoryType : includeAdvisoryTypes) {
            if (advisoryType == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryTypes", "Advisory type must not be null"));
            } else if ( !"all".equalsIgnoreCase(advisoryType) && AdvisoryUtils.TYPE_NORMALIZATION_MAP.get(advisoryType) == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryTypes", "Unknown advisory type: " + advisoryType + ", must be one of " + AdvisoryUtils.TYPE_NORMALIZATION_MAP.keySet()));
            }
        }
    }

    public static CentralSecurityPolicyConfiguration fromFile(File jsonFile) throws IOException {
        LOG.info("Loading security policy configuration from: {}", jsonFile.getAbsolutePath());

        final String json;
        try {
            json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Failed to read security policy configuration file from: " + jsonFile.getAbsolutePath(), e);
        }

        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (Exception e) {
            throw new IOException("Failed to parse security policy configuration file as JSON object from: " + jsonFile.getAbsolutePath() + "\nWith content: " + json, e);
        }

        final CentralSecurityPolicyConfiguration configuration = new CentralSecurityPolicyConfiguration();
        final Map<String, Object> policyConfigurationMap = jsonObject.toMap();
        configuration.setProperties(new LinkedHashMap<>(policyConfigurationMap));

        final List<ProcessMisconfiguration> misconfigurations = new ArrayList<>();
        configuration.collectMisconfigurations(misconfigurations);
        if (!misconfigurations.isEmpty()) {
            throw new IOException(
                    "Security policy configuration file contains a misconfiguration from: " + jsonFile.getAbsolutePath() +
                            "\nWith content: " + json +
                            "\nWith misconfigurations: " + misconfigurations.stream().map(ProcessMisconfiguration::toString).collect(Collectors.joining("\n"))
            );
        }

        return configuration;
    }

    public final static CvssSelector CVSS_SELECTOR_INITIAL = new CvssSelector(Collections.singletonList(
            new CvssRule(CvssSelector.MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),
                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // any other, but not assessment
                    new SourceSelectorEntry(
                            Arrays.asList(new SourceSelectorEntryEntry<>(KnownCvssEntities.ASSESSMENT, true)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ROLE)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ENTITY))
                    )
            )
    ));

    public final static CvssSelector CVSS_SELECTOR_CONTEXT = new CvssSelector(Arrays.asList(
            new CvssRule(CvssSelector.MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),
                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // any other, but not assessment
                    new SourceSelectorEntry(
                            Arrays.asList(new SourceSelectorEntryEntry<>(KnownCvssEntities.ASSESSMENT, true)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ROLE)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ENTITY))
                    )
            ),
            // assessment
            new CvssRule(CvssSelector.MergingMethod.ALL,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)),
            new CvssRule(CvssSelector.MergingMethod.LOWER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
            new CvssRule(CvssSelector.MergingMethod.HIGHER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER))
    ), Collections.singletonList(
            new SelectorStatsEvaluator("assessment", CvssSelector.StatsEvaluatorOperation.EQUAL, CvssSelector.EvaluatorAction.RETURN_NULL, 0)
    ), Collections.singletonList(
            new SelectorVectorEvaluator(CvssSelector.VectorEvaluatorOperation.IS_BASE_FULLY_DEFINED, true, CvssSelector.EvaluatorAction.RETURN_NULL)
    ));

    public static class VulnerabilityStatusMapper {
        private final String name;
        private final Function<String, String> mapper;
        private final List<String> statusNames;
        private final List<String> assessedStatusNames;
        private final String jsMappingFunction;

        public VulnerabilityStatusMapper(String name, List<String> statusNames, List<String> assessedStatusNames, Function<String, String> mapper, String jsMappingFunction) {
            this.name = name;
            this.mapper = mapper;
            this.statusNames = statusNames;
            this.assessedStatusNames = assessedStatusNames;
            this.jsMappingFunction = jsMappingFunction;
        }

        public String getName() {
            return name;
        }

        public Function<String, String> getMapper() {
            return mapper;
        }

        public List<String> getStatusNames() {
            return statusNames;
        }

        public List<String> getAssessedStatusNames() {
            return assessedStatusNames;
        }

        public String getJsMappingFunction(String name) {
            return "function " + name + "(status) { " + jsMappingFunction + " }";
        }

        public VulnerabilityStatusMapper withName(String name) {
            return new VulnerabilityStatusMapper(name, statusNames, assessedStatusNames, mapper, jsMappingFunction);
        }
    }

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED = new VulnerabilityStatusMapper(
            "unmodified",
            Arrays.asList(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, VulnerabilityMetaData.STATUS_VALUE_VOID),
            Arrays.asList(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, VulnerabilityMetaData.STATUS_VALUE_VOID),
            name -> {
                if (StringUtils.isEmpty(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW;
                }

                if (VulnerabilityMetaData.STATUS_VALUE_APPLICABLE.equalsIgnoreCase(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_APPLICABLE;
                }

                // FIXME: when would the status be "Potential Vulnerability"?
                if ("potential vulnerability".equalsIgnoreCase(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_APPLICABLE;
                }

                return name;
            },
            "if (status === null || status === '') { " +
                    "return '" + VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW + "'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_APPLICABLE + "') { " +
                    "return '" + VulnerabilityMetaData.STATUS_VALUE_APPLICABLE + "'; " +
                    "} else if (status === 'potential vulnerability') { " +
                    "return '" + VulnerabilityMetaData.STATUS_VALUE_APPLICABLE + "'; " +
                    "} else { " +
                    "return status; " +
                    "}");

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT = VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED.withName("default");

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED = new VulnerabilityStatusMapper(
            "abstracted",
            Arrays.asList("affected", "potentially affected", "not affected"),
            Arrays.asList("affected", "not affected"),
            name -> {
                if (StringUtils.isEmpty(name)) { // in review (implicit)
                    return "potentially affected";
                }

                switch (name) {
                    case VulnerabilityMetaData.STATUS_VALUE_APPLICABLE:
                        return "affected";

                    case VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE:
                    case VulnerabilityMetaData.STATUS_VALUE_VOID:
                        return "not affected";

                    case VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT:
                    case VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW:
                        return "potentially affected";

                    default:
                        return name;
                }
            },
            "if (status === null || status === '') { " +
                    "return 'potentially affected'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_APPLICABLE + "') { " +
                    "return 'affected'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE + "') { " +
                    "return 'not affected'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_VOID + "') { " +
                    "return 'not affected'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT + "') { " +
                    "return 'potentially affected'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW + "') { " +
                    "return 'potentially affected'; " +
                    "} else { " +
                    "return status; " +
                    "}");

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE = new VulnerabilityStatusMapper(
            "review state",
            Arrays.asList("reviewed", VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, VulnerabilityMetaData.STATUS_VALUE_VOID),
            Arrays.asList("reviewed", VulnerabilityMetaData.STATUS_VALUE_VOID),
            name -> {
                final String baseMappedName = VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED.getMapper().apply(name);

                switch (baseMappedName) {
                    case VulnerabilityMetaData.STATUS_VALUE_APPLICABLE:
                    case VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE:
                        return "reviewed";
                    default:
                        return baseMappedName;
                }
            },
            VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED.getJsMappingFunction("internalUnmodifiedMapper") +
                    "status = internalUnmodifiedMapper(status);" +
                    "if (status === null || status === '') { " +
                    "return '" + VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW + "'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_APPLICABLE + "') { " +
                    "return 'reviewed'; " +
                    "} else if (status === '" + VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE + "') { " +
                    "return 'reviewed'; " +
                    "} else { " +
                    "return status; " +
                    "}");

    private final static List<VulnerabilityStatusMapper> REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS = Arrays.asList(
            VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED,
            VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT,
            VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED,
            VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE
    );

    public static VulnerabilityStatusMapper getStatusMapperByName(String name) {
        return REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status mapper: " + name));
    }
}
