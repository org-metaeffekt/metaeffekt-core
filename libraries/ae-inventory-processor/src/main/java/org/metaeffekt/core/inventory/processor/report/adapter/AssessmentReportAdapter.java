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
package org.metaeffekt.core.inventory.processor.report.adapter;

import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerabilityContextInventory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable.SeverityToStatusRow;
import static org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable.buildTable;

public class AssessmentReportAdapter {

    private final Inventory inventory;
    private final CentralSecurityPolicyConfiguration securityPolicy;

    public AssessmentReportAdapter(Inventory inventory, CentralSecurityPolicyConfiguration securityPolicy) {
        this.inventory = inventory;
        this.securityPolicy = securityPolicy;
    }

    public List<AssetMetaData> getAssets() {
        return inventory.getAssetMetaData().stream()
                .sorted(Comparator.comparing(o -> o.get(AssetMetaData.Attribute.NAME), String::compareToIgnoreCase))
                .collect(Collectors.toList());
    }

    public VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useEffectiveSeverity) {
        final AeaaVulnerabilityContextInventory vAssetInventory = AeaaVulnerabilityContextInventory.fromInventory(inventory, assetMetaData);
        vAssetInventory.calculateEffectiveCvssVectorsForVulnerabilities(securityPolicy);
        vAssetInventory.applyEffectiveVulnerabilityStatus(securityPolicy);

        final Set<AeaaVulnerability> vulnerabilities = vAssetInventory.getShallowCopyVulnerabilities();

        final VulnerabilityCounts counts = new VulnerabilityCounts();

        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            final StatisticsOverviewTable statisticsOverviewTable = buildTable(this.securityPolicy, vulnerabilities, null, useEffectiveSeverity);

            counts.assessedCounter = statisticsOverviewTable.getRows().stream()
                    .map(SeverityToStatusRow::getAssessedCount)
                    .reduce(0, Integer::sum);
            counts.totalCounter = statisticsOverviewTable.getRows().stream()
                    .map(SeverityToStatusRow::getTotal)
                    .reduce(0, Integer::sum);

            applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("critical"), counts::setCriticalCounter);
            applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("high"), counts::setHighCounter);
            applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("medium"), counts::setMediumCounter);
            applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("low"), counts::setLowCounter);
            applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("none"), counts::setNoneCounter);
        }

        return counts;
    }

    private void applyTotalCountIfNotNull(SeverityToStatusRow row, Consumer<Integer> consumer) {
        if (row != null) {
            consumer.accept(row.getTotal());
        }
    }

    /**
     * Helper class to collect information per {@link AssetMetaData} instance.
     */
    public static class VulnerabilityCounts {
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

        public void setCriticalCounter(long criticalCounter) {
            this.criticalCounter = criticalCounter;
        }

        public void setHighCounter(long highCounter) {
            this.highCounter = highCounter;
        }

        public void setMediumCounter(long mediumCounter) {
            this.mediumCounter = mediumCounter;
        }

        public void setLowCounter(long lowCounter) {
            this.lowCounter = lowCounter;
        }

        public void setNoneCounter(long noneCounter) {
            this.noneCounter = noneCounter;
        }

        public void setAssessedCounter(long assessedCounter) {
            this.assessedCounter = assessedCounter;
        }

        public void setTotalCounter(long totalCounter) {
            this.totalCounter = totalCounter;
        }
    }

}
