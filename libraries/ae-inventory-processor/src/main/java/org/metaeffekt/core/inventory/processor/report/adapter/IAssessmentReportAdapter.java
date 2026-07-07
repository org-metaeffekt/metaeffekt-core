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
package org.metaeffekt.core.inventory.processor.report.adapter;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.StatisticsOverviewTable.SeverityToStatusRow;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface IAssessmentReportAdapter extends ReportAdapter {

    IAssessmentReportAdapter setup(Inventory inventory, CentralSecurityPolicyConfiguration securityPolicy);

    List<AssetMetaData> getAssets();

    String assetDisplayName(AssetMetaData asset);

    String assetDisplayType(AssetMetaData asset);

    String assetGroupDisplayName(AssetMetaData asset);

    List<GroupedAssetsVulnerabilityCounts> groupAssetsByAssetGroup(Collection<AssetMetaData> assets, boolean useEffectiveSeverity, boolean enableSingleAssetGroups);

    VulnerabilityCounts countVulnerabilities(AssetMetaData assetMetaData, boolean useEffectiveSeverity);

    @Data
    @Slf4j
    @Accessors(chain = true)
    class GroupedAssetVulnerabilityCounts {
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
    @Slf4j
    @Accessors(chain = true)
    class GroupedAssetsVulnerabilityCounts {
        public List<GroupedAssetVulnerabilityCounts> groupedAssetVulnerabilityCounts;
        public VulnerabilityCounts totalVulnerabilityCounts;
        public String assetGroupDisplayName;
        public String assetGroupAsXmlId;

        public void log() {
            log.info("Asset Group:  {}", assetGroupDisplayName);
            log.info("Total Counts: {}", totalVulnerabilityCounts);
            if (groupedAssetVulnerabilityCounts != null) {
                groupedAssetVulnerabilityCounts.forEach(GroupedAssetVulnerabilityCounts::log);
            }
        }
    }

    @Data
    class VulnerabilityCounts {
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