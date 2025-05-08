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

    private final Map<AssetMetaData, VulnerabilityCounts> initialCountsCache = new HashMap<>();
    private final Map<AssetMetaData, VulnerabilityCounts> effectiveCountsCache = new HashMap<>();

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
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get("Asset Group"), DEFAULT_ASSET_GROUP_NAME));
    }

    /* counting and grouping assets */

    private final static String DEFAULT_ASSET_GROUP_NAME = "Default";

    public List<GroupedAssetsVulnerabilityCounts> groupAssetsByAssetGroup(Collection<AssetMetaData> assets, boolean useEffectiveSeverity) {
        final List<GroupedAssetsVulnerabilityCounts> groupedAssets = assets.stream()
                .sorted(Comparator.comparing(this::assetGroupDisplayName, (s, str) -> {
                    // "Default" should be last
                    if (s.equals(DEFAULT_ASSET_GROUP_NAME)) return 1;
                    if (str.equals(DEFAULT_ASSET_GROUP_NAME)) return -1;
                    return s.compareToIgnoreCase(str);
                }))
                .collect(Collectors.groupingBy(this::assetGroupDisplayName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {

                    final List<GroupedAssetVulnerabilityCounts> groupedAssetVulnerabilityCounts = entry.getValue().stream()
                            .map(asset -> {
                                final VulnerabilityCounts counts = countVulnerabilities(asset, useEffectiveSeverity);
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
                    groupedAssetsCounts.setTotalVulnerabilityCounts(VulnerabilityCounts.sumFrom(groupedAssetVulnerabilityCounts.stream()
                            .map(GroupedAssetVulnerabilityCounts::getTotalCounts).collect(Collectors.toList())));

                    return groupedAssetsCounts;
                })
                .collect(Collectors.toList());

        // check if all groups contain exactly one asset
        final boolean allGroupsHaveOneAsset = !groupedAssets.isEmpty() &&
                groupedAssets.stream().allMatch(group -> group.getGroupedAssetVulnerabilityCounts().size() == 1);
        // if all groups have exactly one asset and there are more than one group, combine them into a single group
        boolean combineIntoOneGroup = allGroupsHaveOneAsset && groupedAssets.size() > 1;

        if (!combineIntoOneGroup) {
            return groupedAssets;
        }

        log.debug("Combining all groups into a single group [{}] as all groups have exactly one asset: {}", DEFAULT_ASSET_GROUP_NAME,
                groupedAssets.stream().map(c -> c.getAssetGroupDisplayName() + " (" + c.getGroupedAssetVulnerabilityCounts().stream().map(GroupedAssetVulnerabilityCounts::getAssetDisplayName).collect(Collectors.joining(", ")) + ")").collect(Collectors.joining(", ")));

        final GroupedAssetsVulnerabilityCounts combinedGroup = new GroupedAssetsVulnerabilityCounts();
        final List<GroupedAssetVulnerabilityCounts> combinedAssets = groupedAssets.stream()
                .flatMap(group -> group.getGroupedAssetVulnerabilityCounts().stream())
                .sorted(Comparator.comparing(GroupedAssetVulnerabilityCounts::getAssetDisplayName))
                .collect(Collectors.toList());

        combinedGroup.setGroupedAssetVulnerabilityCounts(combinedAssets);
        combinedGroup.setAssetGroupDisplayName(DEFAULT_ASSET_GROUP_NAME);
        combinedGroup.setAssetGroupAsXmlId(InventoryReport.xmlEscapeStringAttribute(DEFAULT_ASSET_GROUP_NAME));
        combinedGroup.setTotalVulnerabilityCounts(VulnerabilityCounts.sumFrom(combinedAssets.stream()
                .map(GroupedAssetVulnerabilityCounts::getTotalCounts).collect(Collectors.toList())));

        return Collections.singletonList(combinedGroup);
    }

    public VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useEffectiveSeverity) {
        final Map<AssetMetaData, VulnerabilityCounts> cache = useEffectiveSeverity ? effectiveCountsCache : initialCountsCache;

        final VulnerabilityCounts counts = cache.get(assetMetaData);

        if (counts != null) return counts;

        AeaaVulnerabilityContextInventory vAssetInventory = AeaaVulnerabilityContextInventory.fromInventory(inventory, assetMetaData);
        vAssetInventory.calculateEffectiveCvssVectorsForVulnerabilities(securityPolicy);

        // compute both effective and initial counts
        final VulnerabilityCounts effectiveCounts = computeCounts(true, vAssetInventory);
        final VulnerabilityCounts initialCounts = computeCounts(false, vAssetInventory);

        // and cache the information in case needed again
        effectiveCountsCache.put(assetMetaData, effectiveCounts);
        initialCountsCache.put(assetMetaData, initialCounts);

        return useEffectiveSeverity ? effectiveCounts : initialCounts;
    }

    private VulnerabilityCounts computeCounts(boolean useEffectiveSeverity, AeaaVulnerabilityContextInventory vAssetInventory) {
        final VulnerabilityCounts counts = new VulnerabilityCounts();
        final Set<AeaaVulnerability> vulnerabilities = vAssetInventory.getShallowCopyVulnerabilities();
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
