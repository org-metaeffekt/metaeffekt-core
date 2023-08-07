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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData.*;

public class AssessmentReportAdapter {

    private Inventory inventory;

    public AssessmentReportAdapter(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<AssetMetaData> getAssets() {
        final List<AssetMetaData> assetMetaData = new ArrayList<>(inventory.getAssetMetaData());
        assetMetaData.sort(Comparator.comparing(o -> o.get("Name"), String::compareToIgnoreCase));
        return assetMetaData;
    }

    public VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useModifiedSeverity) {
        // check for asset-specific assessment
        final String assessment = assetMetaData.get("Assessment");

        final List<VulnerabilityMetaData> vulnerabilities = inventory.getVulnerabilityMetaData(assessment);

        final VulnerabilityCounts counts = new VulnerabilityCounts();
        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            for (VulnerabilityMetaData vulnerabilityMetaData : vulnerabilities) {
                final String severity = getSeverity(vulnerabilityMetaData, useModifiedSeverity);

                switch (severity.toLowerCase()) {
                    case "critical":
                        counts.criticalCounter++;
                        break;
                    case "high":
                        counts.highCounter++;
                        break;
                    case "medium":
                        counts.mediumCounter++;
                        break;
                    case "low":
                        counts.lowCounter++;
                        break;
                    case "none":
                        counts.noneCounter++;
                        break;
                }

                final boolean isAssessed = vulnerabilityMetaData.isStatus(STATUS_VALUE_APPLICABLE) ||
                        vulnerabilityMetaData.isStatus(STATUS_VALUE_NOTAPPLICABLE) ||
                        vulnerabilityMetaData.isStatus(STATUS_VALUE_VOID);

                if (isAssessed) {
                    counts.assessedCounter++;
                }
                counts.totalCounter++;
            }
        }

        return counts;
    }

    private String getSeverity(VulnerabilityMetaData vulnerabilityMetaData, boolean useModifiedSeverities) {
        String severity = null;
        if (useModifiedSeverities) {
            severity = vulnerabilityMetaData.get("CVSS Modified Severity (v3)");
            if (StringUtils.isEmpty(severity)) {
                severity = vulnerabilityMetaData.get("CVSS Modified Severity (v2)");
            }
        }

        if (StringUtils.isEmpty(severity)) {
            severity = vulnerabilityMetaData.get("CVSS Unmodified Severity (v3)");
        }

        if (StringUtils.isEmpty(severity)) {
            severity = vulnerabilityMetaData.get("CVSS Unmodified Severity (v2)");
        }

        if (StringUtils.isEmpty(severity)) {
            severity = "none";
        }

        return severity;
    }

    /**
     * Helper class to collect information per {@link AssetMetaData} instance.
     */
    public class VulnerabilityCounts {
        public long criticalCounter;
        public long highCounter;
        public long mediumCounter;
        public long lowCounter;
        public long noneCounter;
        public long assessedCounter;
        public long totalCounter;

        public long getCriticalCounter() {
            return criticalCounter;
        }

        public long getHighCounter() {
            return highCounter;
        }

        public long getMediumCounter() {
            return mediumCounter;
        }

        public long getLowCounter() {
            return lowCounter;
        }

        public long getNoneCounter() {
            return noneCounter;
        }

        public long getTotalCounter() {
            return totalCounter;
        }

        public long getAssessedCounter() {
            return assessedCounter;
        }
    }

}
