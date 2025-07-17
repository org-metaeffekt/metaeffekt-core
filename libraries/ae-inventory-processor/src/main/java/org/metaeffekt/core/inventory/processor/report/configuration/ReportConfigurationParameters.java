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

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReportConfigurationParameters {

    /**
     * If true, Open CoDE Status for licenses is included in the license tables.
     */
    @Builder.Default
    private final boolean enableOpenCodeStatus = true;

    /**
     * If true, disables the priority label in the document (all areas), hides the priority score and hides
     * the priority label columns in the Vulnerability list.
     */
    @Builder.Default
    private final boolean hidePriorityInformation = false;

    /**
     * Determines which language files are used in the report templates. Supports english and german.
     */
    @Builder.Default
    private final String reportLanguage = "en";

    /**
     * Flag indicating whether to include inofficial OSI status information, when detailing license characteristics.
     * Defaults to <code>false</code> due to backward expectation management.
     */
    @Builder.Default
    private final boolean includeInofficialOsiStatus = false;

    /**
     * Some report use cases do not require/desire the vulnerabilities in the inventory to be filtered. Adversely to
     * previous handling (all inventories were filtered) the default is here <code>false</code>. The relevance of the
     * vulnerability is determined by the use case of the inventory and not structural. Also, some inventories may not
     * detail the artifacts-level information and only provide assets and asset-level assessment information.
     */
    @Builder.Default
    private boolean filterVulnerabilitiesNotCoveredByArtifacts = false;

    /**
     * Whether to hide the periodic status "unclassified" in the vulnerability report. If set to <code>true</code>, the
     * section will simply not be generated.<br>
     * Defaults to <code>false</code>.
     */
    @Builder.Default
    private boolean filterAdvisorySummary = false;

    // fail behaviour section

    @Builder.Default
    private boolean failOnError = true;

    @Builder.Default
    private boolean failOnBanned = true;

    @Builder.Default
    private boolean failOnDowngrade = true;

    @Builder.Default
    private boolean failOnUnknown = true;

    @Builder.Default
    private boolean failOnUnknownVersion = true;

    @Builder.Default
    private boolean failOnDevelopment = true;

    @Builder.Default
    private boolean failOnInternal = true;

    @Builder.Default
    private boolean failOnUpgrade = true;

    @Builder.Default
    private boolean failOnMissingLicense = true;

    @Builder.Default
    private boolean failOnMissingLicenseFile = true;

    @Builder.Default
    private boolean failOnMissingNotice = true;

    @Builder.Default
    private boolean failOnMissingComponentFiles = false;

    // template inclusion section

    @Builder.Default
    private boolean inventoryBomReportEnabled = false;

    @Builder.Default
    private boolean inventoryDiffReportEnabled = false;

    @Builder.Default
    private boolean inventoryPomEnabled = false;

    @Builder.Default
    private boolean inventoryVulnerabilityReportEnabled = false;

    @Builder.Default
    private boolean inventoryVulnerabilityReportSummaryEnabled = false;

    @Builder.Default
    private boolean inventoryVulnerabilityStatisticsReportEnabled = false;

    @Builder.Default
    private boolean assetBomReportEnabled = false;

    @Builder.Default
    private boolean assessmentReportEnabled = false;

    public void setAllFailConditions(boolean shouldFail) {
        failOnError = shouldFail;
        failOnBanned = shouldFail;
        failOnDowngrade = shouldFail;
        failOnUnknown = shouldFail;
        failOnUnknownVersion = shouldFail;
        failOnDevelopment = shouldFail;
        failOnInternal = shouldFail;
        failOnUpgrade = shouldFail;
        failOnMissingLicense = shouldFail;
        failOnMissingLicenseFile = shouldFail;
        failOnMissingNotice = shouldFail;
        failOnMissingComponentFiles = shouldFail;
    }
}
