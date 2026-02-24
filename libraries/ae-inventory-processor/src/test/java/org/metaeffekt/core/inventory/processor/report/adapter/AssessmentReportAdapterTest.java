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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AssessmentReportAdapterTest {
    @Test
    public void vulnerabilityAssetGroupVulnerabilityCountTest() {
        final Inventory testInventory = new Inventory();

        {
            final AssetMetaData asset = new AssetMetaData();
            asset.set(AssetMetaData.Attribute.ASSET_ID, "asset1");
            asset.set(AssetMetaData.Attribute.NAME, "asset1");
            asset.set(AssetMetaData.Attribute.ASSESSMENT, "assessment1");
            asset.set("Asset Group", "group1");
            testInventory.getAssetMetaData().add(asset);

            testInventory.getVulnerabilityMetaData("assessment1").add(constructVulnerability("low", "void"));
            testInventory.getVulnerabilityMetaData("assessment1").add(constructVulnerability("medium", "applicable"));
            testInventory.getVulnerabilityMetaData("assessment1").add(constructVulnerability("high", "not applicable"));
            testInventory.getVulnerabilityMetaData("assessment1").add(constructVulnerability("critical", ""));
        }

        {
            final AssetMetaData asset = new AssetMetaData();
            asset.set(AssetMetaData.Attribute.ASSET_ID, "asset2");
            asset.set(AssetMetaData.Attribute.NAME, "asset2");
            asset.set(AssetMetaData.Attribute.ASSESSMENT, "assessment2");
            asset.set("Asset Group", "group1");
            testInventory.getAssetMetaData().add(asset);

            testInventory.getVulnerabilityMetaData("assessment2").add(constructVulnerability("medium", ""));
            testInventory.getVulnerabilityMetaData("assessment2").add(constructVulnerability("medium", "applicable"));
            testInventory.getVulnerabilityMetaData("assessment2").add(constructVulnerability("critical", "applicable"));
            testInventory.getVulnerabilityMetaData("assessment2").add(constructVulnerability("critical", ""));
        }

        {
            final AssetMetaData asset = new AssetMetaData();
            asset.set(AssetMetaData.Attribute.ASSET_ID, "asset3");
            asset.set(AssetMetaData.Attribute.NAME, "asset3");
            asset.set(AssetMetaData.Attribute.ASSESSMENT, "assessment3");
            testInventory.getAssetMetaData().add(asset);

            testInventory.getVulnerabilityMetaData("assessment3").add(constructVulnerability("low", "not applicable"));
            testInventory.getVulnerabilityMetaData("assessment3").add(constructVulnerability("low", "applicable"));
            testInventory.getVulnerabilityMetaData("assessment3").add(constructVulnerability("high", "not applicable"));
            testInventory.getVulnerabilityMetaData("assessment3").add(constructVulnerability("high", "applicable"));
        }

        {
            final AssetMetaData asset = new AssetMetaData();
            asset.set(AssetMetaData.Attribute.ASSET_ID, "asset4");
            asset.set(AssetMetaData.Attribute.NAME, "asset4");
            asset.set(AssetMetaData.Attribute.ASSESSMENT, "assessment4");
            asset.set("Asset Group", "group4");
            testInventory.getAssetMetaData().add(asset);

            testInventory.getVulnerabilityMetaData("assessment4").add(constructVulnerability("low", "applicable"));
            testInventory.getVulnerabilityMetaData("assessment4").add(constructVulnerability("low", "void"));
            testInventory.getVulnerabilityMetaData("assessment4").add(constructVulnerability("low", "void"));
            testInventory.getVulnerabilityMetaData("assessment4").add(constructVulnerability("low", "applicable"));
        }

        final AssessmentReportAdapter adapter = new AssessmentReportAdapter(testInventory, new CentralSecurityPolicyConfiguration());

        final List<AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> grouped = adapter.groupAssetsByAssetGroup(testInventory.getAssetMetaData(), true, true);

        /*for (AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts group : grouped) {
            group.log();
        }*/

        /*
Asset Group:  group1
Total Counts: AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=3, highCounter=0, mediumCounter=3, lowCounter=0, noneCounter=2, assessedCounter=5, totalCounter=8)
 - Asset:     asset1 (in group1)
   Counts:    AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=1, highCounter=0, mediumCounter=1, lowCounter=0, noneCounter=2, assessedCounter=3, totalCounter=4)
 - Asset:     asset2 (in group1)
   Counts:    AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=2, highCounter=0, mediumCounter=2, lowCounter=0, noneCounter=0, assessedCounter=2, totalCounter=4)
Asset Group:  group4
Total Counts: AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=0, mediumCounter=0, lowCounter=2, noneCounter=2, assessedCounter=4, totalCounter=4)
 - Asset:     asset4 (in group4)
   Counts:    AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=0, mediumCounter=0, lowCounter=2, noneCounter=2, assessedCounter=4, totalCounter=4)
Asset Group:  Default
Total Counts: AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=1, mediumCounter=0, lowCounter=1, noneCounter=2, assessedCounter=4, totalCounter=4)
 - Asset:     asset3 (in Default)
   Counts:    AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=1, mediumCounter=0, lowCounter=1, noneCounter=2, assessedCounter=4, totalCounter=4)
         */

        Assertions.assertEquals(3, grouped.size());
        Assertions.assertEquals("Default", grouped.get(2).getAssetGroupDisplayName()); // "Default" should be last

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts group1 = grouped.get(0);
        Assertions.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 3;
            highCounter = 0;
            mediumCounter = 3;
            lowCounter = 0;
            noneCounter = 2;
            assessedCounter = 5;
            totalCounter = 8;
        }}, group1.totalVulnerabilityCounts);
        Assertions.assertEquals("asset1", group1.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);
        Assertions.assertEquals("asset2", group1.groupedAssetVulnerabilityCounts.get(1).assetDisplayName);

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts group4 = grouped.get(1);
        Assertions.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 0;
            highCounter = 0;
            mediumCounter = 0;
            lowCounter = 2;
            noneCounter = 2;
            assessedCounter = 4;
            totalCounter = 4;
        }}, group4.totalVulnerabilityCounts);
        Assertions.assertEquals("asset4", group4.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts otherAssets = grouped.get(2);
        Assertions.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 0;
            highCounter = 1;
            mediumCounter = 0;
            lowCounter = 1;
            noneCounter = 2;
            assessedCounter = 4;
            totalCounter = 4;
        }}, otherAssets.totalVulnerabilityCounts);
        Assertions.assertEquals("asset3", otherAssets.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);
    }

    @Test
    public void testGroupCollapsingWhenEachGroupHasOneAsset() {
        final Inventory testInventory = new Inventory();

        // Asset 1 in Group A
        final AssetMetaData asset1 = new AssetMetaData();
        asset1.set(AssetMetaData.Attribute.ASSET_ID, "asset1");
        asset1.set(AssetMetaData.Attribute.NAME, "asset1");
        asset1.set(AssetMetaData.Attribute.ASSESSMENT, "assessment1");
        asset1.set("Asset Group", "Group A");
        testInventory.getAssetMetaData().add(asset1);
        testInventory.getVulnerabilityMetaData("assessment1").add(constructVulnerability("low", "void"));

        // Asset 2 in Group B
        final AssetMetaData asset2 = new AssetMetaData();
        asset2.set(AssetMetaData.Attribute.ASSET_ID, "asset2");
        asset2.set(AssetMetaData.Attribute.NAME, "asset2");
        asset2.set(AssetMetaData.Attribute.ASSESSMENT, "assessment2");
        asset2.set("Asset Group", "Group B");
        testInventory.getAssetMetaData().add(asset2);
        testInventory.getVulnerabilityMetaData("assessment2").add(constructVulnerability("medium", "applicable"));

        // Asset 3 in Group C
        final AssetMetaData asset3 = new AssetMetaData();
        asset3.set(AssetMetaData.Attribute.ASSET_ID, "asset3");
        asset3.set(AssetMetaData.Attribute.NAME, "asset3");
        asset3.set(AssetMetaData.Attribute.ASSESSMENT, "assessment3");
        asset3.set("Asset Group", "Group C");
        testInventory.getAssetMetaData().add(asset3);
        testInventory.getVulnerabilityMetaData("assessment3").add(constructVulnerability("high", "not applicable"));

        final AssessmentReportAdapter adapter = new AssessmentReportAdapter(testInventory, new CentralSecurityPolicyConfiguration());

        final List<AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> grouped = adapter.groupAssetsByAssetGroup(testInventory.getAssetMetaData(), true, false);

        Assertions.assertEquals(1, grouped.size());
        Assertions.assertEquals("Default", grouped.get(0).getAssetGroupDisplayName());
        Assertions.assertEquals(3, grouped.get(0).getGroupedAssetVulnerabilityCounts().size());

        // asset order
        final List<String> assetNames = grouped.get(0).getGroupedAssetVulnerabilityCounts().stream()
                .map(AssessmentReportAdapter.GroupedAssetVulnerabilityCounts::getAssetDisplayName)
                .collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("asset1", "asset2", "asset3"), assetNames);

        final AssessmentReportAdapter.VulnerabilityCounts totalCounts = grouped.get(0).getTotalVulnerabilityCounts();
        log.info("{}", totalCounts);
        Assertions.assertEquals(0, totalCounts.criticalCounter);
        Assertions.assertEquals(0, totalCounts.highCounter);
        Assertions.assertEquals(1, totalCounts.mediumCounter);
        Assertions.assertEquals(0, totalCounts.lowCounter);
        Assertions.assertEquals(2, totalCounts.noneCounter);
        Assertions.assertEquals(3, totalCounts.assessedCounter);
        Assertions.assertEquals(3, totalCounts.totalCounter);
    }

    @Test
    public void testEnableSingleAssetGroupsParameter() {
        final Inventory testInventory = new Inventory();

        final String[][] assetsData = {
                {"Asset Id 1", "001", "GROUP A", "Asset 1 - Version 1"},
                {"Asset Id 2", "002", "GROUP B", "Asset 2 - Version 2"},
                {"Asset Id 3", "003", "GROUP C", "Asset 3 - Version 3"},
                {"Asset Id 4", "004", "GROUP D", "Asset 4-Version 4"},
                {"Asset Id 5", "005", "GROUP E", "Asset 5 - Version 5"},
                {"Asset Id 6", "006", "GROUP F", "Asset 6"},
                {"Asset Id 7", "007", "GROUP G", "Asset 7"},
                {"Asset Id 8", "008", "GROUP G", "Asset 8"},
        };

        for (String[] assetData : assetsData) {
            final AssetMetaData asset = new AssetMetaData();
            asset.set(AssetMetaData.Attribute.ASSET_ID, assetData[0]);
            asset.set(AssetMetaData.Attribute.ASSESSMENT, assetData[1]);
            asset.set("Asset Group", assetData[2]);
            asset.set(AssetMetaData.Attribute.NAME, assetData[3]);
            testInventory.getAssetMetaData().add(asset);
            testInventory.getVulnerabilityMetaData(assetData[1]).add(constructVulnerability("critical", "applicable"));
        }

        final AssessmentReportAdapter adapter = new AssessmentReportAdapter(testInventory, new CentralSecurityPolicyConfiguration());

        // enableSingleAssetGroups = false
        // single-asset groups should be merged into the "Default" group
        {
            final List<AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> grouped = adapter.groupAssetsByAssetGroup(testInventory.getAssetMetaData(), true, false);

            Assertions.assertEquals(2, grouped.size());

            final Map<String, AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> groupMap = grouped.stream()
                    .collect(Collectors.toMap(AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts::getAssetGroupDisplayName, Function.identity()));

            Assertions.assertTrue(groupMap.containsKey("GROUP G"));
            Assertions.assertTrue(groupMap.containsKey("Default"));

            final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts groupG = groupMap.get("GROUP G");
            Assertions.assertEquals(2, groupG.getGroupedAssetVulnerabilityCounts().size());
            final List<String> groupGAssetNames = groupG.getGroupedAssetVulnerabilityCounts().stream()
                    .map(AssessmentReportAdapter.GroupedAssetVulnerabilityCounts::getAsset)
                    .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                    .collect(Collectors.toList());
            Assertions.assertTrue(groupGAssetNames.containsAll(Arrays.asList("Asset Id 7", "Asset Id 8")));

            final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts defaultGroup = groupMap.get("Default");
            Assertions.assertEquals(6, defaultGroup.getGroupedAssetVulnerabilityCounts().size());
            final List<String> defaultGroupAssetNames = defaultGroup.getGroupedAssetVulnerabilityCounts().stream()
                    .map(AssessmentReportAdapter.GroupedAssetVulnerabilityCounts::getAsset)
                    .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                    .collect(Collectors.toList());
            Assertions.assertTrue(defaultGroupAssetNames.containsAll(Arrays.asList("Asset Id 1", "Asset Id 2", "Asset Id 3", "Asset Id 4", "Asset Id 5", "Asset Id 6")));
        }

        // enableSingleAssetGroups = true
        // all asset groups should be preserved
        {
            final List<AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> grouped = adapter.groupAssetsByAssetGroup(testInventory.getAssetMetaData(), true, true);
            Assertions.assertEquals(7, grouped.size());

            final Map<String, AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> groupMap = grouped.stream()
                    .collect(Collectors.toMap(AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts::getAssetGroupDisplayName, Function.identity()));

            Assertions.assertTrue(groupMap.containsKey("GROUP A"));
            Assertions.assertTrue(groupMap.containsKey("GROUP B"));
            Assertions.assertTrue(groupMap.containsKey("GROUP C"));
            Assertions.assertTrue(groupMap.containsKey("GROUP D"));
            Assertions.assertTrue(groupMap.containsKey("GROUP E"));
            Assertions.assertTrue(groupMap.containsKey("GROUP F"));
            Assertions.assertTrue(groupMap.containsKey("GROUP G"));

            Assertions.assertEquals(1, groupMap.get("GROUP A").getGroupedAssetVulnerabilityCounts().size());
            Assertions.assertEquals(1, groupMap.get("GROUP B").getGroupedAssetVulnerabilityCounts().size());
            Assertions.assertEquals(2, groupMap.get("GROUP G").getGroupedAssetVulnerabilityCounts().size());
        }
    }

    private static int vulnerabilityCount = 0;

    private static VulnerabilityMetaData constructVulnerability(String severity, String status) {
        final VulnerabilityMetaData vulnerability = new VulnerabilityMetaData();
        vulnerability.set(VulnerabilityMetaData.Attribute.NAME, String.format("CVE-2021-%05d", ++vulnerabilityCount));
        vulnerability.set(VulnerabilityMetaData.Attribute.SOURCE, "CVE");

        switch (severity) {
            case "low":
                vulnerability.set("CVSS:3.1 NVD-CNA-NVD", "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:L");
                break;
            case "medium":
                vulnerability.set("CVSS:3.1 NVD-CNA-NVD", "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H");
                break;
            case "high":
                vulnerability.set("CVSS:3.1 NVD-CNA-NVD", "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H");
                break;
            case "critical":
                vulnerability.set("CVSS:3.1 NVD-CNA-NVD", "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
                break;
        }

        switch (status) {
            case "void":
                vulnerability.set("Vulnerability Status", "{\"statusHistory\":[{\"status\":\"void\"}]}");
                break;
            case "applicable":
                vulnerability.set("Vulnerability Status", "{\"statusHistory\":[{\"status\":\"applicable\"}]}");
                break;
            case "not applicable":
                vulnerability.set("Vulnerability Status", "{\"statusHistory\":[{\"status\":\"not applicable\"}]}");
                break;
        }

        return vulnerability;
    }
}
