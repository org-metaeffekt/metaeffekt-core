package org.metaeffekt.core.inventory.processor.report.adapter;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;

import java.util.List;

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

        final List<AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts> grouped = adapter.groupAssetsByAssetGroup(testInventory.getAssetMetaData());

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
Asset Group:  Other Assets
Total Counts: AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=1, mediumCounter=0, lowCounter=1, noneCounter=2, assessedCounter=4, totalCounter=4)
 - Asset:     asset3 (in Other Assets)
   Counts:    AssessmentReportAdapter.VulnerabilityCounts(criticalCounter=0, highCounter=1, mediumCounter=0, lowCounter=1, noneCounter=2, assessedCounter=4, totalCounter=4)
         */

        Assert.assertEquals(3, grouped.size());
        Assert.assertEquals("Other Assets", grouped.get(2).getAssetGroupDisplayName()); // "Other Assets" should be last

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts group1 = grouped.get(0);
        Assert.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 3;
            highCounter = 0;
            mediumCounter = 3;
            lowCounter = 0;
            noneCounter = 2;
            assessedCounter = 5;
            totalCounter = 8;
        }}, group1.totalVulnerabilityCounts);
        Assert.assertEquals("asset1", group1.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);
        Assert.assertEquals("asset2", group1.groupedAssetVulnerabilityCounts.get(1).assetDisplayName);

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts group4 = grouped.get(1);
        Assert.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 0;
            highCounter = 0;
            mediumCounter = 0;
            lowCounter = 2;
            noneCounter = 2;
            assessedCounter = 4;
            totalCounter = 4;
        }}, group4.totalVulnerabilityCounts);
        Assert.assertEquals("asset4", group4.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);

        final AssessmentReportAdapter.GroupedAssetsVulnerabilityCounts otherAssets = grouped.get(2);
        Assert.assertEquals(new AssessmentReportAdapter.VulnerabilityCounts() {{
            criticalCounter = 0;
            highCounter = 1;
            mediumCounter = 0;
            lowCounter = 1;
            noneCounter = 2;
            assessedCounter = 4;
            totalCounter = 4;
        }}, otherAssets.totalVulnerabilityCounts);
        Assert.assertEquals("asset3", otherAssets.groupedAssetVulnerabilityCounts.get(0).assetDisplayName);
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