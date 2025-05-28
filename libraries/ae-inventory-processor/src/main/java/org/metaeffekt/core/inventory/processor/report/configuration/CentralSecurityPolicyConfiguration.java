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
package org.metaeffekt.core.inventory.processor.report.configuration;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.configuration.ProcessConfiguration;
import org.metaeffekt.core.inventory.processor.configuration.ProcessMisconfiguration;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.processor.CvssSelectionResult;
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

/**
 * Configuration class implementing {@link ProcessConfiguration}.<br>
 * Encapsulates parameters defining the way how data is handled, modified, filtered and displayed throughout the
 * inventory enrichment and finally the Vulnerability Assessment Dashboard and Inventory Report.<br>
 * Any filtering properties only apply at the last steps of the pipelines.
 */
public class CentralSecurityPolicyConfiguration extends ProcessConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CentralSecurityPolicyConfiguration.class);

    /**
     * cvssSeverityRanges &rarr; cachedCvssSeverityRanges<br>
     * <code>String &rarr; CvssSeverityRanges</code><p>
     * Used to convert a CVSS score into a severity category for displaying in the report/VAD.<p>
     * Default: JSON object value of {@link CvssSeverityRanges#CVSS_3_SEVERITY_RANGES}
     */
    private String cvssSeverityRanges = CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString();
    /**
     * See {@link CentralSecurityPolicyConfiguration#cvssSeverityRanges}.
     */
    private CvssSeverityRanges cachedCvssSeverityRanges;

    /**
     * priorityScoreSeverityRanges &rarr; cachedPriorityScoreSeverityRanges<br>
     * <code>String &rarr; CvssSeverityRanges</code><p>
     * Used to convert a numeric score, usually between 0.0 and 20.0 into a severity category for displaying in the report/VAD.<p>
     * Default: JSON object value of {@link CvssSeverityRanges#PRIORITY_SCORE_SEVERITY_RANGES}
     */
    private String priorityScoreSeverityRanges = CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString();
    /**
     * See {@link CentralSecurityPolicyConfiguration#priorityScoreSeverityRanges}.
     */
    private CvssSeverityRanges cachedPriorityScoreSeverityRanges;

    private String epssSeverityRanges = CvssSeverityRanges.EPSS_SCORE_SEVERITY_RANGES.toString();
    private CvssSeverityRanges cachedEpssSeverityRanges;

    /**
     * initialCvssSelector &rarr; cachedInitialCvssSelector<br>
     * <code>String &rarr; JSONObject &rarr; CvssSelector</code><p>
     * Specifies rules that are applied step by step to overlay several selected vectors from different sources to calculate a resulting vector. This rule will be applied per CVSS version, meaning there will be multiple selected vectors.
     * See the {@link CentralSecurityPolicyConfiguration#cvssVersionSelectionPolicy} parameter to change what vector version is selected for displaying and calculations (severity, status &hellip;).<br>
     * The default selector as seen on the right will only select provided vectors from several data sources, starting with the NVD and working its way through several other providers.<br>This selector will provide the &ldquo;provided&rdquo; or &ldquo;base&rdquo; vectors.<p>
     * Default: JSON object value of {@link CentralSecurityPolicyConfiguration#CVSS_SELECTOR_INITIAL}
     */
    private String initialCvssSelector = CVSS_SELECTOR_INITIAL.toJson().toString();
    /**
     * contextCvssSelector &rarr; cachedContextCvssSelector<br>
     * <code>String &rarr; JSONObject &rarr; CvssSelector</code><p>
     * See {@link CentralSecurityPolicyConfiguration#initialCvssSelector} for more information.<br>This selector will provide the &ldquo;effective&rdquo; (with assessment) vectors.<p>
     * Default: JSON object value of {@link CentralSecurityPolicyConfiguration#CVSS_SELECTOR_CONTEXT}
     */
    private String contextCvssSelector = CVSS_SELECTOR_CONTEXT.toJson().toString();
    /**
     * See {@link CentralSecurityPolicyConfiguration#initialCvssSelector}.
     */
    private CvssSelector cachedInitialCvssSelector;
    /**
     * See {@link CentralSecurityPolicyConfiguration#contextCvssSelector}.
     */
    private CvssSelector cachedContextCvssSelector;

    /**
     * cvssVersionSelectionPolicy<br>
     * <code>List&lt;CvssScoreVersionSelectionPolicy&gt;</code><p>
     * A list of <code>CvssScoreVersionSelectionPolicy</code> (enum) entries, where you can pick from:<br>HIGHEST, LOWEST, LATEST, OLDEST, V2, V3, V4,<br>The first selector finding a result in the baseCvssSelector / effectiveCvssSelector will be used to be displayed for the vulnerability and for further calculations.<br>The only selectors that can return no result even if there are multiple available are the version-specific selectors (V2, V3, V4), so it is wise to end your selector in one of the others if you consider using the version-specific ones as your main selector.<p>
     * Default: <code>[LATEST]</code>
     */
    private List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy = new ArrayList<>(Collections.singletonList(CvssScoreVersionSelectionPolicy.LATEST));

    /**
     * insignificantThreshold<br>
     * <code>double</code><p>
     * All vulnerabilities without a manually set status with a score equal/lower to the configured value will be considered &ldquo;insignificant&rdquo;. The vulnerability will obtain this status automatically when displayed in the report/VAD.<p>
     * Default: <code>7.0</code>
     */
    private double insignificantThreshold = 7.0;

    /**
     * includeScoreThreshold<br>
     * <code>double</code><p>
     * All vulnerabilities with a score lower than the configured value will be excluded from the report/VAD. <code>-1.0</code> can be used to disable this check.<p>
     * Default: <code>-1.0</code>
     */
    private double includeScoreThreshold = -1.0;

    /**
     * strictJsonSchemaValidation<br>
     * {@link JsonSchemaValidationErrorsHandling}<p>
     * If set to {@link JsonSchemaValidationErrorsHandling#LENIENT}, this will cause certain validation errors in JSON Schema validation processes to be ignored.
     * If such a JSON Schema validation error is encountered, the error will only be logged and the process will continue.
     * More possible values may be added in the future.<p>
     * This currently includes the following validation errors:
     * <ul>
     *     <li><code>ValidatorTypeCode.ADDITIONAL_PROPERTIES</code> - <code>additionalProperties</code> - <code>1001</code></li>
     * </ul>
     * Reasons for setting this to {@link JsonSchemaValidationErrorsHandling#LENIENT} could be:
     * <ul>
     *     <li>the JSON files are used by multiple versions of the processes, where older versions either did not know certain properties or new versions removed/moved properties.</li>
     * </ul>
     * Default: {@link JsonSchemaValidationErrorsHandling#STRICT} from {@link CentralSecurityPolicyConfiguration#JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT}
     */
    private JsonSchemaValidationErrorsHandling jsonSchemaValidationErrorsHandling = JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT;

    public final static JsonSchemaValidationErrorsHandling JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT = JsonSchemaValidationErrorsHandling.STRICT;

    public enum JsonSchemaValidationErrorsHandling {
        STRICT, LENIENT
    }

    /**
     * Only vulnerabilities that reference a security advisory that has one of the provided states will be included in
     * the report/VAD. The states are defined by the security advisories themselves.<br>
     * This data in {@link AdvisoryMetaData.Attribute#REVIEW_STATUS} is set by the
     * <code>AdvisorPeriodicEnrichment</code>.
     * Can be set to:
     * <ul>
     *     <li>
     *         <code>all</code> - apply no filter, include all vulnerabilities.
     *     </li>
     *     <li>
     *         <code>unclassified</code> - the security advisories are not present in the query period, but have been matched by the affected components.
     *     </li>
     *     <li>
     *         <code>unaffected</code> - the security advisories are included in the query period, but are not relevant in this context.
     *     </li>
     *     <li>
     *         <code>new</code> - the security advisories are new in the given context and have not yet been considered during vulnerability assessments.
     *     </li>
     *     <li>
     *         <code>in review</code> - the security advisories are new in the query period. The review of security advisories and verification of related vulnerability assessments are in progress.
     *     </li>
     *     <li>
     *         <code>reviewed</code> - the security advisories have already been considered in the assessment of the related vulnerabilities.
     *     </li>
     * </ul>
     * Default is <code>all</code>.
     */
    private final List<String> includeVulnerabilitiesWithAdvisoryReviewStatus = new ArrayList<>(Collections.singletonList("all"));
    /**
     * includeVulnerabilitiesWithAdvisoryProviders
     * <p>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value. If the implementation
     * is not specified, the name will be used as the implementation. Each list entry represents a single advisory type.
     * <p>
     * If a vulnerability does not have an advisory from one of the specified sources, it will be excluded from the
     * report. <code>all, all</code> can be used to ignore this check.<p>
     * <p>
     * Example:
     * <pre>
     *     [{"name":"CERT_FR"},
     *      {"name":"CERT_SEI"},
     *      {"name":"RHSA","implementation":"CSAF"}]
     * </pre>
     * Default: <code>{"name":"all","implementation":"all"}</code>
     */
    private final JSONArray includeVulnerabilitiesWithAdvisoryProviders = new JSONArray()
            .put(new JSONObject().put("name", "all").put("implementation", "all"));
    /**
     * includeAdvisoryProviders
     * <p>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value. If the implementation
     * is not specified, the name will be used as the implementation. Each list entry represents a single advisory type.
     * <p>
     * A list of advisory provider names with an optional implementation value that will be evaluated for each advisory.
     * If an advisory is not of one of the specified sources, it will be excluded. <code>all, all</code> can be used to
     * ignore this check.
     * <p>
     * Default: <code>{"name":"all","implementation":"all"}</code>
     */
    private final JSONArray includeAdvisoryProviders = new JSONArray()
            .put(new JSONObject().put("name", "all").put("implementation", "all"));
    /**
     * generateOverviewTablesForAdvisories
     * <p>
     * Used by the <code>AbstractInventoryReportCreationMojo</code> in all the vulnerability PDF report generations.
     * <p>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value. If the implementation
     * is not specified, the name will be used as the implementation. Each list entry represents a single advisory type.
     * <p>
     * For every provider, an additional overview table will be generated
     * only evaluating the vulnerabilities containing the respecting provider.
     * If left empty, no additional table will be created.<br>
     * See {@link org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore}
     * or all available providers.
     * <p>
     * Example:
     * <pre>
     *     [{"name":"CERT_FR"},
     *      {"name":"CERT_SEI"},
     *      {"name":"RHSA","implementation":"CSAF"}]
     * </pre>
     * <p>
     * Default: <code>[]</code>
     */
    private final JSONArray generateOverviewTablesForAdvisories = new JSONArray();
    /**
     * includeAdvisoryTypes<br>
     * <code>List&lt;String&gt;</code><p>
     * A list of advisory types that will be evaluated for each advisory. If the advisory type does not appear in the list of identifiers, it will not be included. <code>all</code> can be used to include all advisories.<p>
     * Example: <code>[alert, notice, news]</code><br>
     * Default: <code>[all]</code>
     */
    private final List<String> includeAdvisoryTypes = new ArrayList<>(Collections.singletonList("all"));

    /**
     * vulnerabilityStatusDisplayMapperName &rarr; vulnerabilityStatusDisplayMapper<br>
     * <code>String &rarr; VulnerabilityStatusMapper</code><p>
     * The mapping method to use when displaying vulnerability statuses. The specified mapping method will only be used in selected fields, in the other occasions the unmodified mapper is used, which will only mark unreviewed (no status) vulnerabilities as in review.<br>Available mappers are: default, unmodified, abstracted, review state<br>Where default is the same as unmodified.<br>review state only differs from unmodified such that it groups applicable and not applicable into a reviewed category.<p>
     * Default: <code>default</code> (see {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT} and {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED})<br>
     * Other examples: {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED}, {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE}
     */
    private String vulnerabilityStatusDisplayMapperName = "default";
    /**
     * See {@link CentralSecurityPolicyConfiguration#vulnerabilityStatusDisplayMapperName}.
     */
    private VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper = VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT;

    private VulnerabilityPriorityScoreConfiguration priorityScoreConfiguration = new VulnerabilityPriorityScoreConfiguration();

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

    public CentralSecurityPolicyConfiguration setPriorityScoreSeverityRanges(String priorityScoreSeverityRanges) {
        this.priorityScoreSeverityRanges = priorityScoreSeverityRanges == null ? CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString() : priorityScoreSeverityRanges;
        this.cachedPriorityScoreSeverityRanges = new CvssSeverityRanges(this.priorityScoreSeverityRanges);
        return this;
    }

    public CentralSecurityPolicyConfiguration setPriorityScoreSeverityRanges(CvssSeverityRanges priorityScoreSeverityRanges) {
        this.priorityScoreSeverityRanges = priorityScoreSeverityRanges.toString();
        this.cachedPriorityScoreSeverityRanges = priorityScoreSeverityRanges;
        return this;
    }

    public CvssSeverityRanges getPriorityScoreSeverityRanges() {
        if (this.cachedPriorityScoreSeverityRanges == null) {
            this.priorityScoreSeverityRanges = this.priorityScoreSeverityRanges == null ? CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString() : this.priorityScoreSeverityRanges;
            this.cachedPriorityScoreSeverityRanges = new CvssSeverityRanges(this.priorityScoreSeverityRanges);
        }
        return this.cachedPriorityScoreSeverityRanges;
    }

    public CentralSecurityPolicyConfiguration setEpssScoreSeverityRanges(String epssSeverityRanges) {
        this.epssSeverityRanges = epssSeverityRanges == null ? CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString() : epssSeverityRanges;
        this.cachedEpssSeverityRanges = new CvssSeverityRanges(this.epssSeverityRanges);
        return this;
    }

    public CentralSecurityPolicyConfiguration setEpssScoreSeverityRanges(CvssSeverityRanges epssSeverityRanges) {
        this.epssSeverityRanges = epssSeverityRanges.toString();
        this.cachedEpssSeverityRanges = epssSeverityRanges;
        return this;
    }

    public CvssSeverityRanges getEpssScoreSeverityRanges() {
        if (this.cachedEpssSeverityRanges == null) {
            this.epssSeverityRanges = this.epssSeverityRanges == null ? CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString() : this.epssSeverityRanges;
            this.cachedEpssSeverityRanges = new CvssSeverityRanges(this.epssSeverityRanges);
        }
        return this.cachedEpssSeverityRanges;
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
        final double insignificantThreshold = this.getInsignificantThreshold();
        if (insignificantThreshold == -1.0) return true;
        final CvssVector vector = vulnerability.getCvssSelectionResult(this).getSelectedContextIfAvailableOtherwiseInitial();
        if (vector == null) {
            return false;
        } else {
            return vector.getOverallScore() < insignificantThreshold;
        }
    }

    public double getIncludeScoreThreshold() {
        return this.includeScoreThreshold;
    }

    public boolean isVulnerabilityAboveIncludeScoreThreshold(AeaaVulnerability vulnerability) {
        if (includeScoreThreshold == -1.0 || includeScoreThreshold == Double.MIN_VALUE) return true;
        final CvssVector vector = vulnerability.getCvssSelectionResult().getSelectedByCustomMetric(CvssVector::getOverallScore, CvssSelectionResult.CUSTOM_VECTOR_SCORE_SELECTOR_MAX);
        final double score = vector == null ? 0.0 : vector.getOverallScore();
        return isVulnerabilityAboveIncludeScoreThreshold(score);
    }

    public boolean isVulnerabilityAboveIncludeScoreThreshold(double score) {
        if (includeScoreThreshold == -1.0 || includeScoreThreshold == Double.MIN_VALUE) return true;
        return score >= includeScoreThreshold;
    }

    public CentralSecurityPolicyConfiguration setJsonSchemaValidationErrorsHandling(JsonSchemaValidationErrorsHandling jsonSchemaValidationErrorsHandling) {
        this.jsonSchemaValidationErrorsHandling = jsonSchemaValidationErrorsHandling;
        return this;
    }

    public JsonSchemaValidationErrorsHandling getJsonSchemaValidationErrorsHandling() {
        return jsonSchemaValidationErrorsHandling;
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(Map<String, String> includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders.clear();
        includeVulnerabilitiesWithAdvisoryProviders.forEach((name, implementation) -> this.includeVulnerabilitiesWithAdvisoryProviders.put(new JSONObject().put("name", name).put("implementation", StringUtils.isNotEmpty(implementation) ? implementation : name)));
        return this;
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(JSONArray includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders.clear();
        this.includeVulnerabilitiesWithAdvisoryProviders.putAll(includeVulnerabilitiesWithAdvisoryProviders);
        return this;
    }

    public JSONArray getIncludeVulnerabilitiesWithAdvisoryProviders() {
        return includeVulnerabilitiesWithAdvisoryProviders;
    }

    public boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability) {
        if (containsAny(includeVulnerabilitiesWithAdvisoryProviders)) {
            return true;
        }

        final List<AeaaAdvisoryTypeIdentifier<?>> filter = AeaaAdvisoryTypeStore.get().fromJsonNamesAndImplementations(includeVulnerabilitiesWithAdvisoryProviders);
        return isVulnerabilityIncludedRegardingAdvisoryProviders(vulnerability, filter);
    }

    public static List<AeaaVulnerability> filterVulnerabilitiesForAdvisoryProviders(Collection<AeaaVulnerability> vulnerabilities, Collection<AeaaAdvisoryTypeIdentifier<?>> filter) {
        return vulnerabilities.stream()
                .filter(v -> isVulnerabilityIncludedRegardingAdvisoryProviders(v, filter))
                .collect(Collectors.toList());
    }

    public static boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability, Collection<AeaaAdvisoryTypeIdentifier<?>> filter) {
        return vulnerability.getSecurityAdvisories().stream().anyMatch(a -> filter.contains(a.getSourceIdentifier()));
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryReviewStatus(List<String> includeVulnerabilitiesWithAdvisoryReviewStatus) {
        this.includeVulnerabilitiesWithAdvisoryReviewStatus.clear();
        this.includeVulnerabilitiesWithAdvisoryReviewStatus.addAll(includeVulnerabilitiesWithAdvisoryReviewStatus);
        return this;
    }

    public List<String> getIncludeVulnerabilitiesWithAdvisoryReviewStatus() {
        return includeVulnerabilitiesWithAdvisoryReviewStatus;
    }

    public boolean isVulnerabilityIncludedRegardingAdvisoryReviewStatus(AeaaVulnerability vulnerability) {
        if (containsAny(includeVulnerabilitiesWithAdvisoryReviewStatus)) {
            return true;
        }
        return vulnerability.getSecurityAdvisories().stream()
                .map(a -> a.getAdditionalAttribute(AdvisoryMetaData.Attribute.REVIEW_STATUS))
                .filter(Objects::nonNull)
                .anyMatch(includeVulnerabilitiesWithAdvisoryReviewStatus::contains);
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryProviders(JSONArray includeAdvisoryProviders) {
        this.includeAdvisoryProviders.clear();
        this.includeAdvisoryProviders.putAll(includeAdvisoryProviders);
        return this;
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryProviders(Map<String, String> includeAdvisoryProviders) {
        this.includeAdvisoryProviders.clear();
        includeAdvisoryProviders.forEach((name, implementation) -> this.includeAdvisoryProviders.put(new JSONObject().put("name", name).put("implementation", StringUtils.isNotEmpty(implementation) ? implementation : name)));
        return this;
    }

    public JSONArray getIncludeAdvisoryProviders() {
        return includeAdvisoryProviders;
    }

    public CentralSecurityPolicyConfiguration setGenerateOverviewTablesForAdvisories(JSONArray generateOverviewTablesForAdvisories) {
        this.generateOverviewTablesForAdvisories.clear();
        this.generateOverviewTablesForAdvisories.putAll(generateOverviewTablesForAdvisories);
        return this;
    }

    public CentralSecurityPolicyConfiguration setGenerateOverviewTablesForAdvisories(Map<String, String> generateOverviewTablesForAdvisories) {
        this.generateOverviewTablesForAdvisories.clear();
        generateOverviewTablesForAdvisories.forEach((name, implementation) -> this.generateOverviewTablesForAdvisories.put(new JSONObject().put("name", name).put("implementation", StringUtils.isNotEmpty(implementation) ? implementation : name)));
        return this;
    }

    public JSONArray getGenerateOverviewTablesForAdvisories() {
        return generateOverviewTablesForAdvisories;
    }

    public List<AeaaAdvisoryTypeIdentifier<?>> getGenerateOverviewTablesForAdvisoriesInst() {
        return AeaaAdvisoryTypeStore.get().fromJsonNamesAndImplementations(generateOverviewTablesForAdvisories);
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
        if (containsAny(includeAdvisoryTypes)) {
            return true;
        }
        return includeAdvisoryTypes.contains(advisoryType);
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntryProvider(AeaaAdvisoryEntry advisory) {
        if (containsAny(includeAdvisoryProviders)) {
            return true;
        }
        for (AeaaAdvisoryTypeIdentifier<?> identifier : AeaaAdvisoryTypeStore.get().fromJsonNamesAndImplementations(includeAdvisoryProviders)) {
            if (identifier == advisory.getSourceIdentifier()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntryProvider(String providerName) {
        if (containsAny(includeAdvisoryProviders)) {
            return true;
        }
        for (AeaaAdvisoryTypeIdentifier<?> identifier : AeaaAdvisoryTypeStore.get().fromJsonNamesAndImplementations(includeAdvisoryProviders)) {
            if (identifier.getName().equals(providerName)) {
                return true;
            }
        }
        return false;
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

    public CentralSecurityPolicyConfiguration setPriorityScoreConfiguration(VulnerabilityPriorityScoreConfiguration priorityScoreConfiguration) {
        this.priorityScoreConfiguration = priorityScoreConfiguration;
        return this;
    }

    public VulnerabilityPriorityScoreConfiguration getPriorityScoreConfiguration() {
        return priorityScoreConfiguration;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        final LinkedHashMap<String, Object> configuration = new LinkedHashMap<>();

        configuration.put("cvssSeverityRanges", cvssSeverityRanges);
        configuration.put("priorityScoreSeverityRanges", priorityScoreSeverityRanges);
        configuration.put("epssSeverityRanges", epssSeverityRanges);
        configuration.put("initialCvssSelector", super.optionalConversion(getInitialCvssSelector(), CvssSelector::toJson));
        configuration.put("contextCvssSelector", super.optionalConversion(getContextCvssSelector(), CvssSelector::toJson));
        configuration.put("insignificantThreshold", insignificantThreshold);
        configuration.put("includeScoreThreshold", includeScoreThreshold);
        configuration.put("jsonSchemaValidationErrorsHandling", jsonSchemaValidationErrorsHandling);
        configuration.put("includeVulnerabilitiesWithAdvisoryProviders", includeVulnerabilitiesWithAdvisoryProviders);
        configuration.put("includeVulnerabilitiesWithAdvisoryReviewStatus", includeVulnerabilitiesWithAdvisoryReviewStatus);
        configuration.put("includeAdvisoryProviders", includeAdvisoryProviders);
        configuration.put("generateOverviewTablesForAdvisories", generateOverviewTablesForAdvisories);
        configuration.put("includeAdvisoryTypes", includeAdvisoryTypes);
        configuration.put("vulnerabilityStatusDisplayMapperName", vulnerabilityStatusDisplayMapperName);
        configuration.put("cvssVersionSelectionPolicy", cvssVersionSelectionPolicy);
        configuration.put("priorityScoreConfiguration", priorityScoreConfiguration.getProperties());

        return configuration;
    }

    @Override
    public void setProperties(LinkedHashMap<String, Object> properties) {
        super.loadStringProperty(properties, "cvssSeverityRanges", this::setCvssSeverityRanges);
        super.loadStringProperty(properties, "priorityScoreSeverityRanges", this::setPriorityScoreSeverityRanges);
        super.loadStringProperty(properties, "epssSeverityRanges", this::setEpssScoreSeverityRanges);

        super.loadProperty(properties, "baseCvssSelector", this::parseJsonObjectFromProperties, selector -> setBaseCvssSelector(CvssSelector.fromJson(selector))); // deprecated
        super.loadProperty(properties, "initialCvssSelector", this::parseJsonObjectFromProperties, selector -> setBaseCvssSelector(CvssSelector.fromJson(selector)));
        super.loadProperty(properties, "effectiveCvssSelector", this::parseJsonObjectFromProperties, selector -> setEffectiveCvssSelector(CvssSelector.fromJson(selector))); // deprecated
        super.loadProperty(properties, "contextCvssSelector", this::parseJsonObjectFromProperties, selector -> setEffectiveCvssSelector(CvssSelector.fromJson(selector)));

        super.loadProperty(properties, "jsonSchemaValidationErrorsHandling", value -> JsonSchemaValidationErrorsHandling.valueOf(String.valueOf(value)), this::setJsonSchemaValidationErrorsHandling);
        super.loadDoubleProperty(properties, "insignificantThreshold", this::setInsignificantThreshold);
        super.loadDoubleProperty(properties, "includeScoreThreshold", this::setIncludeScoreThreshold);
        super.loadJsonArrayProperty(properties, "includeVulnerabilitiesWithAdvisoryProviders", this::setIncludeVulnerabilitiesWithAdvisoryProviders);
        super.loadListProperty(properties, "includeVulnerabilitiesWithAdvisoryReviewStatus", String::valueOf, this::setIncludeVulnerabilitiesWithAdvisoryReviewStatus);
        super.loadJsonArrayProperty(properties, "includeAdvisoryProviders", this::setIncludeAdvisoryProviders);
        super.loadJsonArrayProperty(properties, "generateOverviewTablesForAdvisories", this::setGenerateOverviewTablesForAdvisories);
        super.loadListProperty(properties, "includeAdvisoryTypes", String::valueOf, this::setIncludeAdvisoryTypes);
        super.loadStringProperty(properties, "vulnerabilityStatusDisplayMapperName", this::setVulnerabilityStatusDisplayMapper);
        super.loadListProperty(properties, "cvssVersionSelectionPolicy", value -> CvssScoreVersionSelectionPolicy.valueOf(String.valueOf(value)), this::setCvssVersionSelectionPolicy);

        super.loadSubConfiguration(properties, "priorityScoreConfiguration", VulnerabilityPriorityScoreConfiguration::new, this::setPriorityScoreConfiguration);
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
        if (this.priorityScoreSeverityRanges == null) {
            misconfigurations.add(new ProcessMisconfiguration("priorityScoreSeverityRanges", "Priority score severity ranges must not be null"));
        }
        if (this.epssSeverityRanges == null) {
            misconfigurations.add(new ProcessMisconfiguration("epssSeverityRanges", "EPSS score severity ranges must not be null"));
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

        for (int i = 0; i < includeVulnerabilitiesWithAdvisoryProviders.length(); i++) {
            final JSONObject provider = includeVulnerabilitiesWithAdvisoryProviders.optJSONObject(i, null);
            if (provider == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeVulnerabilitiesWithAdvisoryProviders", "Advisory provider must not be null or is not a JSON object: " + includeVulnerabilitiesWithAdvisoryProviders));
            }
        }

        for (String advisoryType : includeAdvisoryTypes) {
            if (advisoryType == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryTypes", "Advisory type must not be null"));
            } else if (!CentralSecurityPolicyConfiguration.isAny(advisoryType) && AdvisoryUtils.TYPE_NORMALIZATION_MAP.get(advisoryType) == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryTypes", "Unknown advisory type: " + advisoryType + ", must be one of " + AdvisoryUtils.TYPE_NORMALIZATION_MAP.keySet()));
            }
        }

        for (int i = 0; i < includeAdvisoryProviders.length(); i++) {
            final JSONObject provider = includeAdvisoryProviders.optJSONObject(i, null);
            if (provider == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryProviders", "Advisory provider must not be null or is not a JSON object: " + includeAdvisoryProviders));
            }
        }

        for (int i = 0; i < generateOverviewTablesForAdvisories.length(); i++) {
            final JSONObject provider = generateOverviewTablesForAdvisories.optJSONObject(i, null);
            if (provider == null) {
                misconfigurations.add(new ProcessMisconfiguration("generateOverviewTablesForAdvisories", "Advisory provider must not be null or is not a JSON object: " + generateOverviewTablesForAdvisories));
            }
        }

        for (String status : includeVulnerabilitiesWithAdvisoryReviewStatus) {
            // must be valid status or any
            if (status == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeVulnerabilitiesWithAdvisoryReviewStatus", "Advisory review status must not be null"));
            } else if (!CentralSecurityPolicyConfiguration.isAny(status) && !AdvisoryMetaData.ADVISORY_REVIEW_STATUS_VALUES.contains(status)) {
                misconfigurations.add(new ProcessMisconfiguration("includeVulnerabilitiesWithAdvisoryReviewStatus", "Unknown advisory review status: " + status + ", must be a valid status from " + AdvisoryMetaData.ADVISORY_REVIEW_STATUS_VALUES + " or \"all\""));
            }
        }

        if (jsonSchemaValidationErrorsHandling == null) {
            misconfigurations.add(new ProcessMisconfiguration("strictJsonSchemaValidation", "JSON schema validation must not be null"));
        }

        if (this.priorityScoreConfiguration != null) {
            this.priorityScoreConfiguration.collectMisconfigurations(misconfigurations);
        }
    }

    public static CentralSecurityPolicyConfiguration fromFile(File jsonFile) throws IOException {
        if (jsonFile == null) {
            throw new IllegalArgumentException("Security policy configuration file must not be null");
        } else if (!jsonFile.exists()) {
            throw new IOException("Security policy configuration file does not exist: " + jsonFile.getAbsolutePath());
        } else if (!jsonFile.isFile()) {
            throw new IOException("Security policy configuration file is not a file: " + jsonFile.getAbsolutePath());
        }

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

        return fromJson(jsonObject, jsonFile + " - " + json);
    }

    public static CentralSecurityPolicyConfiguration fromConfiguration(
            CentralSecurityPolicyConfiguration baseConfiguration,
            File jsonFile,
            String jsonOverwrite
    ) throws IOException {

        if (baseConfiguration != null) {
            return baseConfiguration;
        }

        if (jsonFile == null && jsonOverwrite == null) {
            return new CentralSecurityPolicyConfiguration();
        }

        final JSONObject effectiveApplyJson = new JSONObject();

        try {
            if (jsonFile != null) {
                LOG.info("Reading security policy from securityPolicyFile: file://{}", jsonFile.getAbsolutePath());
                final JSONObject jsonFromFile = new JSONObject(FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8));
                for (String key : jsonFromFile.keySet()) {
                    effectiveApplyJson.put(key, jsonFromFile.get(key));
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to read security policy configuration file from: " + jsonFile.getAbsolutePath(), e);
        }

        try {
            if (jsonOverwrite != null) {
                LOG.info("Applying security policy from securityPolicyJson: {}", String.join("", jsonOverwrite.split("\n")));
                final JSONObject jsonFromOverwrite = new JSONObject(jsonOverwrite);
                for (String key : jsonFromOverwrite.keySet()) {
                    effectiveApplyJson.put(key, jsonFromOverwrite.get(key));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse security policy configuration from JSON: " + jsonOverwrite, e);
        }

        return fromJson(effectiveApplyJson, jsonFile + " - " + effectiveApplyJson);
    }

    public static CentralSecurityPolicyConfiguration fromJson(JSONObject jsonObject, String errorMessage) throws IOException {
        final CentralSecurityPolicyConfiguration configuration = new CentralSecurityPolicyConfiguration();
        final Map<String, Object> policyConfigurationMap = jsonObject.toMap();
        configuration.setProperties(new LinkedHashMap<>(policyConfigurationMap));

        final List<ProcessMisconfiguration> misconfigurations = new ArrayList<>();
        configuration.collectMisconfigurations(misconfigurations);
        if (!misconfigurations.isEmpty()) {
            throw new IOException(
                    "Security policy configuration file contains a misconfiguration from: " + errorMessage +
                            "\nWith content: " + jsonObject +
                            "\nWith misconfigurations: " + misconfigurations.stream().map(ProcessMisconfiguration::toString).collect(Collectors.joining("\n"))
            );
        }
        return configuration;
    }

    public final static CvssSelector CVSS_SELECTOR_INITIAL = new CvssSelector(Collections.singletonList(
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),

                    // OSV
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.GHSA),
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // CSAF
                    new SourceSelectorEntry(KnownCvssEntities.CSAF, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

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
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),

                    // OSV
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.GHSA),
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // CSAF
                    new SourceSelectorEntry(KnownCvssEntities.CSAF, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

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
            new CvssRule(MergingMethod.ALL,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)),
            new CvssRule(MergingMethod.LOWER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
            new CvssRule(MergingMethod.HIGHER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER)),
            new CvssRule(MergingMethod.LOWER_METRIC,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER_METRIC)),
            new CvssRule(MergingMethod.HIGHER_METRIC,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER_METRIC))
    ), Collections.singletonList(
            new SelectorStatsEvaluator("assessment", CvssSelector.StatsEvaluatorOperation.EQUAL, CvssSelector.EvaluatorAction.RETURN_NULL, 0)
    ), Collections.singletonList(
            new SelectorVectorEvaluator(CvssSelector.VectorEvaluatorOperation.IS_BASE_FULLY_DEFINED, true, CvssSelector.EvaluatorAction.RETURN_NULL)
    ));

    /**
     * Defines a java mapper {@link Function} and a string that represents the mapper function as a JavaScript
     * function.<br>
     * As of writing this entry, there are three distinct supported official mappers used for different purposes in the
     * reporting stage. Even if configured differently in the {@link CentralSecurityPolicyConfiguration}, some display
     * elements will still force a certain display mapper for consistency.
     * <table>
     *     <caption>Mapper overview</caption>
     *     <tr>
     *         <th>Initial Status</th>
     *         <th>unmodified / default</th>
     *         <th>abstracted</th>
     *         <th>review state</th>
     *     </tr>
     *     <tr>
     *         <td>null / empty</td>
     *         <td><code>in review</code></td>
     *         <td><code>potentially affected</code></td>
     *         <td><code>in review</code></td>
     *     </tr>
     *     <tr>
     *         <td>in review</td>
     *         <td><code>in review</code></td>
     *         <td><code>potentially affected</code></td>
     *         <td><code>in review</code></td>
     *     </tr>
     *     <tr>
     *         <td>applicable</td>
     *         <td><code>applicable</code></td>
     *         <td><code>affected</code></td>
     *         <td><code>reviewed</code></td>
     *     </tr>
     *     <tr>
     *         <td>not applicable</td>
     *         <td><code>not applicable</code></td>
     *         <td><code>not affected</code></td>
     *         <td><code>reviewed</code></td>
     *     </tr>
     *     <tr>
     *         <td>insignificant</td>
     *         <td><code>insignificant</code></td>
     *         <td><code>potentially affected</code></td>
     *         <td><code>insignificant</code></td>
     *     </tr>
     *     <tr>
     *         <td>void</td>
     *         <td><code>void</code></td>
     *         <td><code>not affected</code></td>
     *         <td><code>void</code></td>
     *     </tr>
     *     <tr>
     *         <td>other</td>
     *         <td><code>other</code></td>
     *         <td><code>other</code></td>
     *         <td><code>other</code></td>
     *     </tr>
     *     <tr>
     *         <td>Usage</td>
     *         <td>
     *             The VAD uses this mapper at almost all places and is not configurable otherwise.<br>
     *             Is the default mapper for the status chapters in the Report and the Report overview table.
     *         </td>
     *         <td>
     *             Is intended to be used in the Report, but never used as default, except for the summary report title page summary.
     *         </td>
     *         <td>
     *             Is currently only used for the overview chart in the VAD.
     *         </td>
     *     </tr>
     * </table>
     */
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
            Arrays.asList(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW,
                    VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT,
                    VulnerabilityMetaData.STATUS_VALUE_VOID),
            Arrays.asList(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE,
                    VulnerabilityMetaData.STATUS_VALUE_VOID),
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

    public static List<VulnerabilityStatusMapper> getRegisteredVulnerabilityStatusDisplayMappers() {
        return Collections.unmodifiableList(REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS);
    }

    public static VulnerabilityStatusMapper getStatusMapperByName(String name) {
        return REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status mapper: " + name));
    }

    public static boolean containsAny(Collection<String> collection) {
        return collection != null && !collection.isEmpty()
                && (collection.contains("ALL") || collection.contains("all") || collection.contains("ANY") || collection.contains("any"));
    }

    public static boolean containsAny(Map<String, String> collection) {
        return collection != null && !collection.isEmpty()
                && (collection.containsKey("ALL") || collection.containsKey("all") || collection.containsKey("ANY") || collection.containsKey("any"));
    }

    public static boolean containsAny(JSONArray collection) { // List<Map<String, String>>
        if (collection == null || collection.isEmpty()) {
            return false;
        }

        for (int i = 0; i < collection.length(); i++) {
            if (isAny(collection.getJSONObject(i))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAny(JSONObject entry) {
        return entry != null && isAny(entry.getString("name"));
    }

    public static boolean isAny(String value) {
        return StringUtils.isNotEmpty(value) && (value.equalsIgnoreCase("all") || value.equalsIgnoreCase("any"));
    }

    public static boolean isAny(Map.Entry<String, ?> value) {
        return value != null && isAny(value.getKey());
    }
}
