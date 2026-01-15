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
package org.metaeffekt.core.inventory.processor.report.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.configuration.*;
import org.metaeffekt.core.inventory.processor.configuration.converter.FieldConverter;
import org.metaeffekt.core.inventory.processor.configuration.converter.JsonArrayConverter;
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
@Accessors(chain = true)
@NoArgsConstructor
public class CentralSecurityPolicyConfiguration extends ProcessConfiguration {

    @ExcludeProcessConfigurationProperty
    private static final Logger LOG = LoggerFactory.getLogger(CentralSecurityPolicyConfiguration.class);

    /**
     * Used to convert a CVSS score into a Severity Category (e.g. None, Low, Medium, High, Critical) for displaying in reports and dashboards.<br>
     * The syntax defines ranges using a format like <code>Label:color:min:max</code>.<p>
     * Default: string value of {@link CvssSeverityRanges#CVSS_3_SEVERITY_RANGES}<p>
     * Parsed property:<br>
     * <code>String &rarr; CvssSeverityRanges</code>
     */
    private String cvssSeverityRanges = CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString();

    /**
     * Used to convert a numeric Priority Score (usually between 0.0 and 20.0 depending on configuration of {@link CentralSecurityPolicyConfiguration#priorityScoreConfiguration}) into a Priority Label (e.g. none, elevated, due, escalate) for displaying in reports and dashboards.<br>
     * The Priority Score is a metric calculated based on various factors (CVSS, EOL, Exploitability, etc.).<p>
     * Default: string value of {@link CvssSeverityRanges#PRIORITY_SCORE_SEVERITY_RANGES}<p>
     * Parsed property:<br>
     * <code>String &rarr; CvssSeverityRanges</code>
     */
    private String priorityScoreSeverityRanges = CvssSeverityRanges.PRIORITY_SCORE_SEVERITY_RANGES.toString();

    /**
     * Used to convert an EPSS probability score (0.0 to 1.0) into a severity category for displaying in reports.<p>
     * Default: string value of {@link CvssSeverityRanges#EPSS_SCORE_SEVERITY_RANGES}<p>
     * Parsed property:<br>
     * <code>String &rarr; CvssSeverityRanges</code>
     */
    private String epssSeverityRanges = CvssSeverityRanges.EPSS_SCORE_SEVERITY_RANGES.toString();

    /**
     * Specifies rules that are applied step by step to overlay several selected vectors from different sources to calculate a resulting vector.
     * This selector will provide the &ldquo;provided&rdquo; or &ldquo;base&rdquo; vectors.<br>
     * By default, this excludes user assessment vectors, only selecting provided vectors from external data sources, starting with the NVD and working its way through several other providers.<br>
     * CVSS Selectors are applied per CVSS version, meaning there will be multiple selected vectors (one per version).
     * See the {@link CentralSecurityPolicyConfiguration#cvssVersionSelectionPolicy} parameter to change what vector version is then selected for displaying and calculations (severity, status &hellip;).<p>
     * Default: JSON object value of {@link CentralSecurityPolicyConfiguration#CVSS_SELECTOR_INITIAL}<p>
     * Parsed property:<br>
     * <code>String &rarr; JSONObject &rarr; CvssSelector</code>
     */
    @ProcessConfigurationProperty(alternativeNames = {"baseCvssSelector"}, converter = CvssSelectorConverter.class)
    private String initialCvssSelector = CVSS_SELECTOR_INITIAL.toJson().toString();

    /**
     * See {@link CentralSecurityPolicyConfiguration#initialCvssSelector} for more information.<br>
     * This selector will provide the &ldquo;effective&rdquo; vectors (including assessments).<br>
     * By default, it includes user-defined assessments that modify or override base vector metrics from external data sources.<p>
     * Default: JSON object value of {@link CentralSecurityPolicyConfiguration#CVSS_SELECTOR_CONTEXT}<p>
     * Parsed property:<br>
     * <code>String &rarr; JSONObject &rarr; CvssSelector</code>
     */
    @ProcessConfigurationProperty(alternativeNames = {"effectiveCvssSelector"}, converter = CvssSelectorConverter.class)
    private String contextCvssSelector = CVSS_SELECTOR_CONTEXT.toJson().toString();

    /**
     * A list of {@link CvssScoreVersionSelectionPolicy} (enum) entries used to pick the final vector for display and calculation.<br>
     * The available options are: <code>HIGHEST</code>, <code>LOWEST</code>, <code>LATEST</code>, <code>OLDEST</code>, <code>V2</code>, <code>V3</code>, <code>V4</code>.<br>
     * This selection logic is applied to both the <code>initialCvssSelector</code> and the <code>contextCvssSelector</code> results and for each the first selector that finds a matching result in will be used.<br>
     * <b>Note:</b> The version-specific selectors (<code>V2</code>, <code>V3</code>, <code>V4</code>) are the only ones that can return no result even if other vectors are available.
     * Therefore, if you use a version-specific selector, it is intended to end your list with a fallback strategy (like <code>LATEST</code>) to ensure a score is always selected.<p>
     * Default: <code>[LATEST]</code>
     */
    @Getter @Setter
    private List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy = Collections.singletonList(CvssScoreVersionSelectionPolicy.LATEST);

    /**
     * Sets the threshold at or below which a vulnerability is considered "insignificant".<br>
     * If a vulnerability has no manually assigned status, and its effective CVSS score is less than or equal to this value, it will automatically be assigned the status <code>insignificant</code>.<br/>
     * Otherwise, it will have the status <code>in review</code> assigned.
     */
    @Getter @Setter
    private double insignificantThreshold = 7.0;

    /**
     * Sets a hard filtering threshold based on the effective CVSS score.<br>
     * All vulnerabilities with a score strictly lower than this value will be excluded from reports entirely.<br>
     * A value of <code>-1.0</code> disables this filter.
     */
    @Getter @Setter
    private double includeScoreThreshold = -1.0;

    /**
     * Specifies a time period after which a vulnerability assessment is considered outdated and requires a re-review.<br>
     * The value is a string like "3 months, 5 hours".
     * If not set, no re-review based on time is triggered.
     */
    @Getter @Setter
    private String assessmentReviewPeriod;

    /**
     * Assessment Dashboard-exclusive property.
     * <p>
     * Specifies a pattern to identify whether assessments have been created for a certain context or for a foreign one (e.g. project or asset scope).<br>
     * The pattern string follows the same rules as some properties from the dashboard API configuration, with variables being exposed using the <code>$[varname]</code> sequence.
     * For example: <code>$[asset.current.Asset Id]/$[asset.current.Version]</code>
     * <p>
     * The string constructed from this will be written into the CreationContext.sourceAssessmentContext of assessments and assessment events created in a dashboard.
     * Loaded assessments will be compared against the local string.
     */
    @Getter @Setter
    private String sourceAssessmentContextPattern;

    /**
     * Configures how the process handles JSON Schema validation errors.<br>
     * If set to {@link JsonSchemaValidationErrorsHandling#LENIENT}, certain known validation errors will be logged only, and the process will continue instead of failing.<p>
     * This currently includes the following validation errors:
     * <ul>
     *     <li><code>ValidatorTypeCode.ADDITIONAL_PROPERTIES</code> - <code>additionalProperties</code> - <code>1001</code></li>
     * </ul>
     * Reasons for setting this to {@link JsonSchemaValidationErrorsHandling#LENIENT} could be:
     * <ul>
     *     <li>The JSON files are used by multiple versions of the processes, where older versions did not know certain properties.</li>
     *     <li>New versions of the input files removed or moved properties that the schema still expects.</li>
     * </ul>
     * Default: {@link JsonSchemaValidationErrorsHandling#STRICT} (from {@link CentralSecurityPolicyConfiguration#JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT})
     */
    @Getter @Setter
    private JsonSchemaValidationErrorsHandling jsonSchemaValidationErrorsHandling = JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT;

    public final static JsonSchemaValidationErrorsHandling JSON_SCHEMA_VALIDATION_ERRORS_DEFAULT = JsonSchemaValidationErrorsHandling.STRICT;

    public enum JsonSchemaValidationErrorsHandling {
        STRICT, LENIENT
    }

    /**
     * Filters vulnerabilities based on the review status of their associated security advisories.<br>
     * Only vulnerabilities that reference a security advisory having one of the provided states will be included in the reports.<br>
     * The data in {@link AdvisoryMetaData.Attribute#REVIEW_STATUS} is typically set by the <code>AdvisorPeriodicEnrichment</code>.
     * <p>
     * Possible values include:
     * <ul>
     *     <li><code>all</code> - Apply no filter, include all vulnerabilities.</li>
     *     <li><code>unclassified</code> - Advisory not present in query period, but matched by affected components.</li>
     *     <li><code>unaffected</code> - Advisory included in query period, but not relevant in this context.</li>
     *     <li><code>new</code> - Advisory is new in the context and not yet considered in assessment.</li>
     *     <li><code>in review</code> - Advisory is new in query period; review/verification in progress.</li>
     *     <li><code>reviewed</code> - Advisory has been considered in the assessment.</li>
     * </ul>
     */
    @Getter
    private final List<String> includeVulnerabilitiesWithAdvisoryReviewStatus = new ArrayList<>(Collections.singletonList("all"));
    /**
     * Filters vulnerabilities based on the provider of their security advisory.<br>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value.
     * If the implementation is not specified, the name will be used as the implementation.<br>
     * If a vulnerability does not have an advisory from one of the specified sources, it will be excluded from the reports.
     * See {@link AeaaAdvisoryTypeStore} or <a href="https://github.com/org-metaeffekt/metaeffekt-documentation/blob/main/metaeffekt-vulnerability-management/inventory-enrichment/content-identifiers.md#security-advisories-providers">content-identifiers.md#security-advisories-providers</a> for all available providers.<p>
     * Use <code>[{"name": "all", "implementation": "all"}]</code> to ignore this check.<p>
     * Example:
     * <pre>
     *     [{"name":"CERT_FR"},
     *      {"name":"CERT_SEI"},
     *      {"name":"RHSA","implementation":"CSAF"}]
     * </pre>
     */
    @ProcessConfigurationProperty(converter = JsonArrayConverter.class)
    private String includeVulnerabilitiesWithAdvisoryProviders = new JSONArray()
            .put(new JSONObject().put("name", "all").put("implementation", "all")).toString();
    /**
     * Filters the Security Advisories displayed in the reports based on their provider.<br>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value.
     * If the implementation is not specified, the name will be used as the implementation.<br>
     * If an advisory is not of one of the specified sources, it will be excluded.
     * See {@link AeaaAdvisoryTypeStore} or <a href="https://github.com/org-metaeffekt/metaeffekt-documentation/blob/main/metaeffekt-vulnerability-management/inventory-enrichment/content-identifiers.md#security-advisories-providers">content-identifiers.md#security-advisories-providers</a> for all available providers.<p>
     * Use <code>[{"name": "all", "implementation": "all"}]</code> to ignore this check.<p>
     */
    @ProcessConfigurationProperty(converter = JsonArrayConverter.class)
    private String includeAdvisoryProviders = new JSONArray()
            .put(new JSONObject().put("name", "all").put("implementation", "all")).toString();
    /**
     * Used by the <code>AbstractInventoryReportCreationMojo</code> in all the vulnerability PDF report generations.<br>
     * Triggers the generation of specific overview tables for the provided advisory sources.<br>
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * For every provider listed here, an additional overview table will be generated only evaluating the vulnerabilities referencing that provider via a security advisory or directly.
     * If left empty, no additional table will be created.<br>
     * See {@link AeaaAdvisoryTypeStore} or <a href="https://github.com/org-metaeffekt/metaeffekt-documentation/blob/main/metaeffekt-vulnerability-management/inventory-enrichment/content-identifiers.md#security-advisories-providers">content-identifiers.md#security-advisories-providers</a> for all available providers.<p>
     * Example:
     * <pre>
     *     [{"name":"CERT_FR"},
     *      {"name":"CERT_SEI"},
     *      {"name":"RHSA","implementation":"CSAF"}]
     * </pre>
     */
    @Getter
    @ProcessConfigurationProperty(converter = JsonArrayConverter.class)
    private String generateOverviewTablesForAdvisories = new JSONArray().toString();
    /**
     * Filters advisories based on their specific type identifier (e.g. <code>alert</code>, <code>notice</code>, <code>news</code>).<br>
     * If the advisory type does not appear in this list, it will not be included.
     * <code>all</code> can be used to include all advisories.<p>
     */
    @Getter
    private final List<String> includeAdvisoryTypes = new ArrayList<>(Collections.singletonList("all"));

    /**
     * The mapping method to use when displaying vulnerability statuses.<br>
     * The specified mapping method will only be used in selected fields; in other occasions the <code>unmodified</code> mapper is used (which only marks unreviewed/no-status vulnerabilities as "in review").<br>
     * Available mappers are:
     * <ul>
     *     <li><code>default</code> {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_DEFAULT} (uses <code>unmodified</code> internally)</li>
     *     <li><code>unmodified</code> {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED}</li>
     *     <li><code>abstracted</code> {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED}</li>
     *     <li><code>review state</code> {@link CentralSecurityPolicyConfiguration#VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE} (groups applicable/not applicable into a "reviewed" category)</li>
     * </ul>
     * Parsed property:<br>
     * <code>String &rarr; VulnerabilityStatusMapper</code>
     */
    @Getter
    private String vulnerabilityStatusDisplayMapperName = "default";

    @Getter @Setter
    private VulnerabilityPriorityScoreConfiguration priorityScoreConfiguration = new VulnerabilityPriorityScoreConfiguration();

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(String cvssSeverityRanges) {
        if (cvssSeverityRanges != null) {
            this.cvssSeverityRanges = cvssSeverityRanges;
        }
        return this;
    }

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(CvssSeverityRanges cvssSeverityRanges) {
        this.cvssSeverityRanges = cvssSeverityRanges.toString();
        return this;
    }

    public CvssSeverityRanges getCvssSeverityRanges() {
        return super.accessCachedProperty("cvssSeverityRanges", this.cvssSeverityRanges, CvssSeverityRanges::new);
    }

    public CentralSecurityPolicyConfiguration setPriorityScoreSeverityRanges(String priorityScoreSeverityRanges) {
        this.priorityScoreSeverityRanges = priorityScoreSeverityRanges;
        return this;
    }

    public CentralSecurityPolicyConfiguration setPriorityScoreSeverityRanges(CvssSeverityRanges priorityScoreSeverityRanges) {
        this.priorityScoreSeverityRanges = priorityScoreSeverityRanges.toString();
        return this;
    }

    public CvssSeverityRanges getPriorityScoreSeverityRanges() {
        return super.accessCachedProperty("priorityScoreSeverityRanges", this.priorityScoreSeverityRanges, CvssSeverityRanges::new);
    }

    public CentralSecurityPolicyConfiguration setEpssScoreSeverityRanges(String epssSeverityRanges) {
        this.epssSeverityRanges = epssSeverityRanges;
        return this;
    }

    public CentralSecurityPolicyConfiguration setEpssScoreSeverityRanges(CvssSeverityRanges epssSeverityRanges) {
        this.epssSeverityRanges = epssSeverityRanges.toString();
        return this;
    }

    public CvssSeverityRanges getEpssScoreSeverityRanges() {
        return super.accessCachedProperty("epssSeverityRanges", this.epssSeverityRanges, CvssSeverityRanges::new);
    }

    public CentralSecurityPolicyConfiguration setInitialCvssSelector(JSONObject initialCvssSelector) {
        this.initialCvssSelector = initialCvssSelector.toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setBaseCvssSelector(CvssSelector baseCvssSelector) {
        this.initialCvssSelector = baseCvssSelector.toJson().toString();
        return this;
    }

    public CvssSelector getInitialCvssSelector() {
        return super.accessCachedProperty("initialCvssSelector", this.initialCvssSelector, CvssSelector::fromJson);
    }

    public CentralSecurityPolicyConfiguration setContextCvssSelector(JSONObject contextCvssSelector) {
        this.contextCvssSelector = contextCvssSelector.toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setEffectiveCvssSelector(CvssSelector effectiveCvssSelector) {
        this.contextCvssSelector = effectiveCvssSelector.toJson().toString();
        return this;
    }

    public CvssSelector getContextCvssSelector() {
        return super.accessCachedProperty("contextCvssSelector", this.contextCvssSelector, CvssSelector::fromJson);
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

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(Map<String, String> includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders = includeVulnerabilitiesWithAdvisoryProviders.entrySet().stream()
                .map((entry) -> new JSONObject().put("name", entry.getKey()).put("implementation", StringUtils.isNotEmpty(entry.getValue()) ? entry.getValue() : entry.getKey()))
                .collect(JSONArray::new, JSONArray::put, JSONArray::putAll)
                .toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(JSONArray includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders = includeVulnerabilitiesWithAdvisoryProviders.toString();
        return this;
    }

    public JSONArray getIncludeVulnerabilitiesWithAdvisoryProviders() {
        return new JSONArray(this.includeVulnerabilitiesWithAdvisoryProviders);
    }

    public boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability) {
        if (containsAny(new JSONArray(includeVulnerabilitiesWithAdvisoryProviders))) {
            return true;
        }

        final List<AeaaAdvisoryTypeIdentifier<?>> filter = getIncludeAdvisoryProviders();
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
        this.includeAdvisoryProviders = includeAdvisoryProviders.toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryProviders(Map<String, String> includeAdvisoryProviders) {
        this.includeAdvisoryProviders = includeAdvisoryProviders.entrySet().stream()
                .map((entry) -> new JSONObject().put("name", entry.getKey()).put("implementation", StringUtils.isNotEmpty(entry.getValue()) ? entry.getValue() : entry.getKey()))
                .collect(JSONArray::new, JSONArray::put, JSONArray::putAll)
                .toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setGenerateOverviewTablesForAdvisories(JSONArray generateOverviewTablesForAdvisories) {
        this.generateOverviewTablesForAdvisories = generateOverviewTablesForAdvisories.toString();
        return this;
    }

    public CentralSecurityPolicyConfiguration setGenerateOverviewTablesForAdvisories(Map<String, String> generateOverviewTablesForAdvisories) {
        this.generateOverviewTablesForAdvisories = generateOverviewTablesForAdvisories.entrySet().stream()
                .map((entry) -> new JSONObject().put("name", entry.getKey()).put("implementation", StringUtils.isNotEmpty(entry.getValue()) ? entry.getValue() : entry.getKey()))
                .collect(JSONArray::new, JSONArray::put, JSONArray::putAll)
                .toString();

        return this;
    }

    public List<AeaaAdvisoryTypeIdentifier<?>> getGenerateOverviewTablesForAdvisoriesInst() {
        return super.accessCachedProperty("generateOverviewTablesForAdvisories", this.generateOverviewTablesForAdvisories, AeaaAdvisoryTypeStore::parseAdvisoryProviders);
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryTypes(List<String> includeAdvisoryTypes) {
        this.includeAdvisoryTypes.clear();
        this.includeAdvisoryTypes.addAll(includeAdvisoryTypes);
        return this;
    }

    public void setIncludeAdvisoryProviders(String advisoryProviders) {
        this.includeAdvisoryProviders = advisoryProviders;
    }

    public List<AeaaAdvisoryTypeIdentifier<?>> getIncludeAdvisoryProviders() {
        return super.accessCachedProperty("includeAdvisoryProviders", this.includeAdvisoryProviders, AeaaAdvisoryTypeStore::parseAdvisoryProviders);
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
        if (containsAny(new JSONArray(includeAdvisoryProviders))) {
            return true;
        }
        for (AeaaAdvisoryTypeIdentifier<?> identifier : getIncludeAdvisoryProviders()) {
            if (identifier == advisory.getSourceIdentifier()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntryProvider(String providerName) {
        if (containsAny(new JSONArray(includeAdvisoryProviders))) {
            return true;
        }
        for (AeaaAdvisoryTypeIdentifier<?> identifier : getIncludeAdvisoryProviders()) {
            if (identifier.getName().equals(providerName)) {
                return true;
            }
        }
        return false;
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(String vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper;
        return this;
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper.getName();
        return this;
    }

    public VulnerabilityStatusMapper getVulnerabilityStatusDisplayMapper() {
        return super.accessCachedProperty("vulnerabilityStatusDisplayMapper", vulnerabilityStatusDisplayMapperName, CentralSecurityPolicyConfiguration::getStatusMapperByName);
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
            misconfigurations.add(new ProcessMisconfiguration("vulnerabilityStatusDisplayMapperName", "Unknown status mapper: " + vulnerabilityStatusDisplayMapperName));
        }

        JSONArray includeVulnerabilitiesWithAdvisoryProvidersJ = new JSONArray(includeVulnerabilitiesWithAdvisoryProviders);
        for (int i = 0; i < includeVulnerabilitiesWithAdvisoryProvidersJ.length(); i++) {
            final JSONObject provider = includeVulnerabilitiesWithAdvisoryProvidersJ.optJSONObject(i, null);
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

        JSONArray includeAdvisoryProvidersJ = new JSONArray(includeAdvisoryProviders);
        for (int i = 0; i < includeAdvisoryProvidersJ.length(); i++) {
            final JSONObject provider = includeAdvisoryProvidersJ.optJSONObject(i, null);
            if (provider == null) {
                misconfigurations.add(new ProcessMisconfiguration("includeAdvisoryProviders", "Advisory provider must not be null or is not a JSON object: " + includeAdvisoryProviders));
            }
        }

        JSONArray generateOverviewTablesForAdvisoriesJ = new JSONArray(generateOverviewTablesForAdvisories);
        for (int i = 0; i < generateOverviewTablesForAdvisoriesJ.length(); i++) {
            final JSONObject provider = generateOverviewTablesForAdvisoriesJ.optJSONObject(i, null);
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
            throw new IOException("Failed because no security policy configuration file was provided.");
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

    private static class CvssSelectorConverter implements FieldConverter<String, Map<?, ?>> {
        @Override
        public Map<?, ?> serialize(String external) {
            return new JSONObject(external).toMap();
        }

        @Override
        public String deserialize(Map<?, ?> internal) {
            return new JSONObject(internal).toString();
        }
    }
}
