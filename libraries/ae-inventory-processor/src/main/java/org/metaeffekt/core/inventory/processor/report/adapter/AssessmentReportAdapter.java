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
package org.metaeffekt.core.inventory.processor.report.adapter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerabilityContextInventory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable.SeverityToStatusRow;

@Slf4j
public class AssessmentReportAdapter {

    private final Inventory inventory;
    private final CentralSecurityPolicyConfiguration securityPolicy;

    public AssessmentReportAdapter(Inventory inventory, CentralSecurityPolicyConfiguration securityPolicy) {
        this.inventory = inventory;
        this.securityPolicy = securityPolicy;
    }

    public List<AssetMetaData> getAssets() {
        return inventory.getAssetMetaData().stream()
                .sorted(Comparator.comparing(this::assetDisplayName, String::compareToIgnoreCase))
                .collect(Collectors.toList());
    }

    /* display strings and names of the asset */

    public String assetDisplayName(AssetMetaData asset) {
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get(AssetMetaData.Attribute.NAME), asset.get(AssetMetaData.Attribute.ASSET_ID), "Unnamed Asset"));
    }

    public String assetDisplayType(AssetMetaData asset) {
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get("Type"), "Unknown Asset Type"));
    }

    public String assetGroupDisplayName(AssetMetaData asset) {
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get("Asset Group"), "Other Assets"));
    }

    /* counting and grouping assets */

    public List<GroupedAssetsVulnerabilityCounts> groupAssetsByAssetGroup(Collection<AssetMetaData> assets) {
        return assets.stream()
                .sorted(Comparator.comparing(this::assetGroupDisplayName, (s, str) -> {
                    // "Other Assets" should be last
                    if (s.equals("Other Assets")) return 1;
                    return s.compareToIgnoreCase(str);
                }))
                .collect(Collectors.groupingBy(this::assetGroupDisplayName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {

                    final List<GroupedAssetVulnerabilityCounts> groupedAssetVulnerabilityCounts = entry.getValue().stream()
                            .map(asset -> {
                                final VulnerabilityCounts counts = countVulnerabilities(asset, true);
                                final GroupedAssetVulnerabilityCounts groupedAssetCounts = new GroupedAssetVulnerabilityCounts();
                                groupedAssetCounts.setAsset(asset);
                                groupedAssetCounts.setAssetGroupDisplayName(entry.getKey());
                                groupedAssetCounts.setAssetPath(ObjectUtils.firstNonNull(asset.get("Path"), asset.get(AssetMetaData.Attribute.ASSET_PATH)));
                                groupedAssetCounts.setAssetDisplayName(assetDisplayName(asset));
                                groupedAssetCounts.setTotalCounts(counts);
                                return groupedAssetCounts;
                            })
                            .collect(Collectors.toList());

                    final GroupedAssetsVulnerabilityCounts groupedAssetsCounts = new GroupedAssetsVulnerabilityCounts();

                    groupedAssetsCounts.setGroupedAssetVulnerabilityCounts(groupedAssetVulnerabilityCounts);
                    groupedAssetsCounts.setAssetGroupDisplayName(entry.getKey());
                    groupedAssetsCounts.setAssetGroupAsXmlId(InventoryReport.xmlEscapeStringAttribute(entry.getKey()));
                    groupedAssetsCounts.setTotalVulnerabilityCounts(VulnerabilityCounts.sumFrom(groupedAssetVulnerabilityCounts.stream().map(GroupedAssetVulnerabilityCounts::getTotalCounts).collect(Collectors.toList())));

                    return groupedAssetsCounts;
                })
                .collect(Collectors.toList());
    }

    public VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useEffectiveSeverity) {
        final AeaaVulnerabilityContextInventory vAssetInventory = AeaaVulnerabilityContextInventory.fromInventory(inventory, assetMetaData);
        vAssetInventory.calculateEffectiveCvssVectorsForVulnerabilities(securityPolicy);
        vAssetInventory.applyEffectiveVulnerabilityStatus(securityPolicy);

        final Set<AeaaVulnerability> vulnerabilities = vAssetInventory.getShallowCopyVulnerabilities();

        final VulnerabilityCounts counts = new VulnerabilityCounts();

        if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
            final StatisticsOverviewTable statisticsOverviewTable = StatisticsOverviewTable.buildTable(this.securityPolicy, vulnerabilities, null, useEffectiveSeverity);

            counts.assessedCounter = statisticsOverviewTable.getRows().stream()
                    .map(SeverityToStatusRow::getAssessedCount)
                    .reduce(0, Integer::sum);
            counts.totalCounter = statisticsOverviewTable.getRows().stream()
                    .map(SeverityToStatusRow::getTotal)
                    .reduce(0, Integer::sum);

            VulnerabilityCounts.applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("critical"), counts::setCriticalCounter);
            VulnerabilityCounts.applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("high"), counts::setHighCounter);
            VulnerabilityCounts.applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("medium"), counts::setMediumCounter);
            VulnerabilityCounts.applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("low"), counts::setLowCounter);
            VulnerabilityCounts.applyTotalCountIfNotNull(statisticsOverviewTable.findRowBySeverity("none"), counts::setNoneCounter);
        }

        return counts;
    }

    @Data
    public static class GroupedAssetVulnerabilityCounts {
        public AssetMetaData asset;
        public String assetGroupDisplayName;
        public String assetPath;
        public String assetDisplayName;
        public VulnerabilityCounts totalCounts;

        public void log() {
            log.info(" - Asset:     {} (in {}) (path: {})", assetDisplayName, assetGroupDisplayName, assetPath);
            log.info("   Counts:    {}", totalCounts);
        }
    }

    @Data
    public static class GroupedAssetsVulnerabilityCounts {
        public List<GroupedAssetVulnerabilityCounts> groupedAssetVulnerabilityCounts;
        public VulnerabilityCounts totalVulnerabilityCounts;
        public String assetGroupDisplayName;
        public String assetGroupAsXmlId; // InventoryReport.xmlEscapeStringAttribute(assetGroup)

        public void log() {
            log.info("Asset Group:  {}", assetGroupDisplayName);
            log.info("Total Counts: {}", totalVulnerabilityCounts);
            groupedAssetVulnerabilityCounts.forEach(GroupedAssetVulnerabilityCounts::log);
        }
    }

    /**
     * Helper class to collect information per {@link AssetMetaData} instance.
     */
    @Data
    public static class VulnerabilityCounts {
        public long criticalCounter;
        public long highCounter;
        public long mediumCounter;
        public long lowCounter;
        public long noneCounter;
        public long assessedCounter;
        public long totalCounter;

        public static void applyTotalCountIfNotNull(SeverityToStatusRow row, Consumer<Integer> consumer) {
            if (row != null) consumer.accept(row.getTotal());
        }

        public static VulnerabilityCounts sumFrom(Collection<VulnerabilityCounts> other) {
            final VulnerabilityCounts result = new VulnerabilityCounts();
            other.forEach(counts -> {
                result.criticalCounter += counts.criticalCounter;
                result.highCounter += counts.highCounter;
                result.mediumCounter += counts.mediumCounter;
                result.lowCounter += counts.lowCounter;
                result.noneCounter += counts.noneCounter;
                result.assessedCounter += counts.assessedCounter;
                result.totalCounter += counts.totalCounter;
            });
            return result;
        }
    }
}
