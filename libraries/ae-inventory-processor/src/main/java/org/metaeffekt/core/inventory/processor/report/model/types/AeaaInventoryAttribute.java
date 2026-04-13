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
package org.metaeffekt.core.inventory.processor.report.model.types;

import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.Artifact;

/**
 * More attributes for the inventory artifacts that are not in core.
 */
public enum AeaaInventoryAttribute implements AbstractModelBase.Attribute {
    @Deprecated
    CERTFR("CertFr"),
    @Deprecated // stored in the REVIEWED_ADVISORIES field now
    REVIEWED_CERTFR("Reviewed CertFr"),
    @Deprecated
    CERTSEI("CertSei"),
    @Deprecated // stored in the REVIEWED_ADVISORIES field now
    REVIEWED_CERTSEI("Reviewed CertSei"),
    REVIEWED_ADVISORIES("Reviewed Advisories"),
    @Deprecated
    ADVISORIES("Advisories"),
    /**
     * The vulnerability description text.
     */
    DESCRIPTION("Description"),
    @Deprecated // use the TAG csv attribute
    ADDED_VIA_STATUS("Added by status file"),
    VULNERABILITIES_FIXED_BY_KB("Vulnerability fixed by KB"),
    IS_CUSTOM_VULNERABILITY("Is custom vulnerability"),
    /**
     * Contains a JSON Object representing a vulnerability status.
     */
    @Deprecated
    VULNERABILITY_STATUS("Vulnerability Status"),
    /**
     * A JSON Array of {@code VulnerabilityAssessmentEvent}
     * instances, all of which have been matched to the vulnerability and are applicable.
     */
    VULNERABILITY_ASSESSMENT_EVENTS("Assessment Events"),
    STATUS_TITLE("Title"),
    STATUS_HISTORY("Status history"),
    STATUS_ACCEPTED("Accepted by"),
    STATUS_REPORTED("Reported by"),
    MEASURES("Measures"),
    @Deprecated
    MS_VULNERABILITY_INFORMATION("MSRC"),
    KEYWORDS("Matched Keyword Sets"),
    KEYWORDS_SCORE("Matched Keyword Total Score"),
    /**
     * All cpes that match the current artifact version.
     */
    MATCHED_CPES("Matched CPEs"),
    /**
     * Still WIP, unused
     */
    CVE_INDICATION("CVE Indication"),
    /**
     * Package URLs added by e.g. the CycloneDX converter.
     */
    PURL("PURL"),
    /**
     * Package URLs derived from the artifact information.
     */
    DERIVED_PURLS("Derived PURLs"),
    /**
     * By adding a dependency track findings file, the cpe findings will be added in this column.
     */
    DT_PURL_FINDINGS("DT PURL Findings"),
    /**
     * The ecosystem of the artifact. Extracted by e.g. the CycloneDX converter from PURLs.
     */
    ECOSYSTEM("Ecosystem"),
    /**
     * A CSV List of (partially or fully defined) PURLs that the artifact is not represented by.
     * Use <code>*</code> for any value on all fields except for the <code>type</code>, which does not allow the asterisk character.
     * Use <code>any</code> for the <code>type</code> instead.
     */
    INAPPLICABLE_PURLS("Inapplicable PURLs"),
    /**
     * By adding a dependency track findings file, the cve findings will be added in this column.
     */
    DT_CVE_FINDINGS("DT CVE Findings"),
    /**
     * The MS product id is used to match MS vulnerabilities.
     */
    MS_PRODUCT_ID("MS Product ID"),
    /**
     * A comma separated list of microsoft knowledge base ids that will exclude MS vulnerabilities.
     */
    MS_KB_IDENTIFIER("MS Knowledge Base ID"),
    /**
     * MS advisories identifiers.<br>
     * Now stored in the regular vulnerability field.
     */
    @Deprecated
    MS_ADVISORIES("MS Advisories"),
    /**
     * KB identifiers that have been superseded by the given KB identifiers.
     */
    MS_SUPERSEDED_KB_IDENTIFIER("MS Superseded Knowledge Base ID"),
    MS_FIXING_KB_IDENTIFIER("MS Fixing Knowledge Base ID"),
    /**
     * An array list containing the MS CVE remediations applicable to at least one artifact.
     */
    MS_REMEDIATIONS("MS Remediations"),
    MS_AFFECTED_PRODUCTS("MS Affected Products"),
    MS_THREATS("MS Threats"),
    /**
     * A comma-separated list of Ids from artifact correlation applied to the artifact.
     */
    APPLIED_CORRELATION_IDS("Applied Correlations"),
    /**
     * Cpe uris that will be removed from the derived cpe uris.
     */
    INAPPLICABLE_CPE("Inapplicable CPE URIs"),
    /**
     * Cpe uris that will be added to the derived cpe uris.
     */
    ADDITIONAL_CPE("Additional CPE URIs"),
    /**
     * Key for getting the manually added CPE URIs in an artifact.<br>
     * If initial cpe uris are given, they will override the derived cpe uris.
     */
    INITIAL_CPE_URIS("CPE URIs"),
    /**
     * Cpe uris that have been derived from the artifact information.
     */
    ACTIVATE_CPE_URI_DERIVATION("Derive CPE URIs"),
    /**
     * Cpe uris that have been derived from the artifact information.
     */
    DERIVED_CPE_URIS("Derived CPE URIs"),
    /**
     * Contains a JSON Object that contains details on what parts of what artifact attribute caused a certain CPE to match on the artifact.<br>
     * This data is only added to the inventory if the {@code com.metaeffekt.artifact.enrichment.configurations.CpeDerivationEnrichmentConfiguration#setAddDetailedMatchingInformation(boolean)} attribute is set to <code>true</code>.
     */
    DERIVED_CPE_URIS_MATCHING_DETAILS("Derived CPE URIs Details"),
    /**
     * Will remove these cve from the matched vulnerabilities.
     */
    INAPPLICABLE_CVE("Inapplicable CVE"),
    /**
     * Will add these cve to the matched vulnerabilities.
     */
    ADDON_CVES("Addon CVEs"),
    @Deprecated // use the Tags csv attribute
    IS_MARKER_VULNERABILITY("Is Marker"),
    /**
     * Represents an EPSS Data point, which can be parsed by the {@code com.metaeffekt.mirror.contents.epss.EpssData} class.*
     */
    EPSS_DATA("EPSS Data"),
    /**
     * Used in {@code VulnerabilityDiffer} to store the vulnerability
     * status of the other inventory.
     */
    VULNERABILITY_DIFF_NEW_STATUS("New Status"),
    /**
     * Used in {@code VulnerabilityDiffer} to indicate the type of status
     * change that has been detected.
     */
    VULNERABILITY_DIFF_STATUS_CHANGE("Status change"),
    /**
     * Represents a KEV entry that can be parsed by the {@code com.metaeffekt.mirror.contents.kev.KevData} class.
     */
    KEV_DATA("KEV Data"),
    /**
     * Represents an EOL (End-of-Line) identifier to be used my the {@code com.metaeffekt.artifact.enrichment.other.EolEnrichment}.
     */
    EOL_ID("EOL Id"),
    EOL_OVERWRITE_CYCLE_QUERY_VERSION("EOL Overwrite Cycle Query Version"),
    EOL_OVERWRITE_LATEST_VERSION_QUERY_VERSION("EOL Overwrite Latest Version Query Version"),
    /**
     * Filled by {@code com.metaeffekt.artifact.enrichment.other.EolEnrichment}. Contains a JSON object with the EOL
     * information built by the {@code ExportedCycleState#toJson()} method.
     */
    EOL_FULL_STATE("EOL State"),
    EOL_RECOMMENDED_CYCLE_VERSION("EOL Latest Cycle Version"),
    EOL_RECOMMENDED_LIFECYCLE_VERSION("EOL Latest Lifecycle Version"),
    EOL_RECOMMENDED_NEXT_SUPPORTED_VERSION("EOL Next Supported Version"),
    EOL_RECOMMENDED_NEXT_SUPPORTED_EXTENDED_VERSION("EOL Next Extended Supported Version"),
    EOL_RECOMMENDED_CLOSEST_SUPPORTED_LTS_VERSION("EOL Closest Supported LTS Version"),
    EOL_RECOMMENDED_LATEST_SUPPORTED_LTS_VERSION("EOL Latest Supported LTS Version"),
    EOL_IS_EOL("EOL Is EOL"),
    EOL_IS_SUPPORTED("EOL Is Support"),
    EOL_IS_SUPPORTED_EXTENDED("EOL Is Extended Support"),
    EOL_RATING("EOL Rating"),
    INVENTORY_CONTEXT("Context"),
    /**
     * CSV attribute for marking entries with tags.
     * <ul>
     *     <li><code>marker</code></li>
     *     <li><code>added by status</code></li>
     * </ul>
     */
    TAGS("Tags"),
    VULNERABILITY_UPDATED_DATE_TIMESTAMP("Last Updated Timestamp"),
    VULNERABILITY_UPDATED_DATE_FORMATTED("Last Updated Date"),
    VULNERABILITY_CREATED_DATE_TIMESTAMP("Created Timestamp"),
    VULNERABILITY_CREATED_DATE_FORMATTED("Created Date"),
    /**
     * Stores Ids of referenced content, such as advisories.
     *
     * @deprecated Use the {@link org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData.Attribute#REFERENCED_VULNERABILITIES}
     * and {@link org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData.Attribute#REFERENCED_SECURITY_ADVISORIES} instead.
     */
    @Deprecated
    VULNERABILITY_REFERENCED_CONTENT_IDS("Referenced Ids"),
    /**
     * @deprecated Use the {@link Artifact.Attribute#TYPE} instead.
     */
    @Deprecated
    ARTIFACT_TYPE("Type"),
    VAD_DETAIL_LEVEL_CONFIGURATIONS("VAD Detail Level Configurations"),
    ADVISOR_OSV_GHSA_REVIEWED_STATE("GHSA Reviewed State"),
    ADVISOR_OSV_GHSA_REVIEWED_DATE("GHSA Reviewed Date"),
    NVD_EQUIVALENT("NVD Equivalent"),
    NVD_EQUIVALENT_ADVISORIES("NVD Equivalent Advisories"),
    RETAINED_VULNERABLE_SOFTWARE_CONFIGURATIONS("Version Ranges"),
    /**
     * Stores a JSON Object of arbitrary data which may be used to evaluate the applicability condition of CVSS vectors in {@code com.metaeffekt.mirror.contents.vulnerability.Vulnerability.isCvssVectorApplicable()}.
     * <p>
     * Currently known uses of this are:
     * <ul>
     *     <li>The {@code InventoryPostProcessingEnrichmentConfiguration.storeArtifactCvssRelevantAttributesInVulnerabilities} stores the {@code InventoryAttribute#MS_PRODUCT_ID}s of referenced artifacts as a JSON Array using the {@code com.metaeffekt.mirror.contents.base.CvssConditionAttributes#MATCHES_ON_MS_PRODUCT_ID} key.</li>
     * </ul>
     */
    CVSS_APPLICABILITY_CONDITION_ATTRIBUTES("CVSS Applicability Condition Attributes");

    private final String key;

    AeaaInventoryAttribute(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
