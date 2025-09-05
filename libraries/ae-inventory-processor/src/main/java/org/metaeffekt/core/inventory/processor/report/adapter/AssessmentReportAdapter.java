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
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerabilityContextInventory;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
        final String assetName = ObjectUtils.firstNonNull(asset.get(AssetMetaData.Attribute.NAME), asset.get(AssetMetaData.Attribute.ASSET_ID), "Unnamed Asset");
        final String assetVersion = asset.get(AssetMetaData.Attribute.VERSION);

        // name might contain the version at the end, separated by a " - ".
        // remove the version in this case, as it will be added below anyway.
        if (StringUtils.isNotEmpty(assetVersion) && assetName.endsWith(assetVersion)) {
            return InventoryReport.xmlEscapeString(assetName.replaceAll("( - |-)?" + Pattern.quote(assetVersion) + "$", ""));
        } else {
            return InventoryReport.xmlEscapeString(assetName);
        }
    }

    public String assetDisplayType(AssetMetaData asset) {
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get("Type"), "Unknown Asset Type"));
    }

    public String assetGroupDisplayName(AssetMetaData asset) {
        return InventoryReport.xmlEscapeString(ObjectUtils.firstNonNull(asset.get("Asset Group"), DEFAULT_ASSET_GROUP_NAME));
    }

    /* counting and grouping assets */

    public final static String DEFAULT_ASSET_GROUP_NAME = "Default";

    public List<GroupedAssetsVulnerabilityCounts> groupAssetsByAssetGroup(Collection<AssetMetaData> assets, boolean useEffectiveSeverity, boolean enableSingleAssetGroups) {
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
                            .map(asset -> new GroupedAssetVulnerabilityCounts()
                                    .setAsset(asset)
                                    .setAssetGroupDisplayName(entry.getKey())
                                    .setAssetPath(ObjectUtils.firstNonNull(asset.get("Path"), asset.get(AssetMetaData.Attribute.ASSET_PATH)))
                                    .setAssetDisplayName(assetDisplayName(asset))
                                    .setAssetVersion(asset.get(AssetMetaData.Attribute.VERSION))
                                    .setTotalCounts(countVulnerabilities(asset, useEffectiveSeverity)))
                            .collect(Collectors.toList());

                    return new GroupedAssetsVulnerabilityCounts()
                            .setGroupedAssetVulnerabilityCounts(groupedAssetVulnerabilityCounts)
                            .setAssetGroupDisplayName(entry.getKey())
                            .setAssetGroupAsXmlId(InventoryReport.xmlEscapeStringAttribute(entry.getKey()))
                            .setTotalVulnerabilityCounts(VulnerabilityCounts.sumFrom(groupedAssetVulnerabilityCounts.stream()
                                    .map(GroupedAssetVulnerabilityCounts::getTotalCounts).collect(Collectors.toList())));
                })
                .collect(Collectors.toList());

        final List<GroupedAssetsVulnerabilityCounts> combinedGroups = optionallyCombineAssetGroups(groupedAssets, enableSingleAssetGroups);
        if (combinedGroups != null) {
            groupedAssets.clear();
            groupedAssets.addAll(combinedGroups);
        }

        return groupedAssets;
    }

    private static List<GroupedAssetsVulnerabilityCounts> optionallyCombineAssetGroups(List<GroupedAssetsVulnerabilityCounts> groupedAssets, boolean enableSingleAssetGroups) {
        final List<GroupedAssetsVulnerabilityCounts> groupsWithSingleAsset = groupedAssets.stream()
                .filter(group -> group.getGroupedAssetVulnerabilityCounts().size() == 1)
                .collect(Collectors.toList());

        // check if any or all groups contain exactly one asset
        final boolean allGroupsHaveOneAsset = !groupedAssets.isEmpty() && groupsWithSingleAsset.size() == groupedAssets.size();
        final boolean anyGroupHasOneAsset = !groupedAssets.isEmpty() && !groupsWithSingleAsset.isEmpty();

        final List<GroupedAssetsVulnerabilityCounts> groupsToBeCombinedIntoDefaultGroup;
        if (allGroupsHaveOneAsset) {
            // if all groups have exactly one asset and there is more than one group, combine them into a single group
            groupsToBeCombinedIntoDefaultGroup = new ArrayList<>(groupedAssets);
        } else if (anyGroupHasOneAsset && !enableSingleAssetGroups) {
            // combine all assets from all groups with only one asset into a single default group
            groupsToBeCombinedIntoDefaultGroup = new ArrayList<>(groupsWithSingleAsset);
        } else {
            return null;
        }

        log.debug("Combining [{}] groups into a single group [{}] as all groups have exactly one asset: {}", groupsToBeCombinedIntoDefaultGroup.size(), DEFAULT_ASSET_GROUP_NAME,
                groupsToBeCombinedIntoDefaultGroup.stream().map(c -> c.getAssetGroupDisplayName() + " (" + c.getGroupedAssetVulnerabilityCounts().stream().map(GroupedAssetVulnerabilityCounts::getAssetDisplayName).collect(Collectors.joining(", ")) + ")").collect(Collectors.joining(", ")));

        final GroupedAssetsVulnerabilityCounts combinedGroup = new GroupedAssetsVulnerabilityCounts();
        final List<GroupedAssetVulnerabilityCounts> combinedAssets = groupsToBeCombinedIntoDefaultGroup.stream()
                .flatMap(group -> group.getGroupedAssetVulnerabilityCounts().stream())
                .sorted(Comparator.comparing(GroupedAssetVulnerabilityCounts::getAssetDisplayName))
                .collect(Collectors.toList());

        combinedGroup.setGroupedAssetVulnerabilityCounts(combinedAssets)
                .setAssetGroupDisplayName(DEFAULT_ASSET_GROUP_NAME)
                .setAssetGroupAsXmlId(InventoryReport.xmlEscapeStringAttribute(DEFAULT_ASSET_GROUP_NAME))
                .setTotalVulnerabilityCounts(VulnerabilityCounts.sumFrom(combinedAssets.stream()
                        .map(GroupedAssetVulnerabilityCounts::getTotalCounts).collect(Collectors.toList())));

        final List<GroupedAssetsVulnerabilityCounts> result = new ArrayList<>();
        result.add(combinedGroup);

        for (GroupedAssetsVulnerabilityCounts groupedAsset : groupedAssets) {
            if (!groupsToBeCombinedIntoDefaultGroup.contains(groupedAsset)) {
                result.add(groupedAsset);
            }
        }

        return result;
    }

    public VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useEffectiveSeverity) {
        final Map<AssetMetaData, VulnerabilityCounts> cache = useEffectiveSeverity ? effectiveCountsCache : initialCountsCache;

        final VulnerabilityCounts counts = cache.get(assetMetaData);

        if (counts != null) return counts;

        AeaaVulnerabilityContextInventory vAssetInventory = AeaaVulnerabilityContextInventory.fromInventory(inventory, assetMetaData);
        vAssetInventory.calculateEffectiveCvssVectorsForVulnerabilities(securityPolicy);
        // FIXME-YWI: taken over from 0.135.x; please review
        // vAssetInventory.applyEffectiveVulnerabilityStatus(securityPolicy);

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
    @Accessors(chain = true)
    public static class GroupedAssetVulnerabilityCounts {
        public AssetMetaData asset;
        public String assetGroupDisplayName;
        public String assetPath;
        public String assetDisplayName;
        public String assetVersion;
        public VulnerabilityCounts totalCounts;

        public void log() {
            log.info(" - Asset:     {} (v {}) (in {}) (path: {})", assetDisplayName, assetVersion, assetGroupDisplayName, assetPath);
            log.info("   Counts:    {}", totalCounts);
        }
    }

    @Data
    @Accessors(chain = true)
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
