/*
 * Copyright 2009-2021 the original author or authors.
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;

import java.util.Arrays;

public class StatisticsOverviewTableTest {

    private enum SEVERITY {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        NONE
    }

    @Test
    public void createStatisticsOverviewTableTest() {
        Inventory inventory = new Inventory();
        VulnerabilityReportAdapter vra = new VulnerabilityReportAdapter(inventory);

        // start with an empty inventory
        {
            final StatisticsOverviewTable emptyStatistics = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, null);
            Assert.assertTrue(emptyStatistics.isEmpty());

        }

        // add two VMDs (one CERT-FR, one CERT-FR + CERT-SEI; both applicable)
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, SEVERITY.CRITICAL.toString(), "CERT-FR"));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, SEVERITY.CRITICAL.toString(), "CERT-FR", "CERT-SEI"));

        {
            final StatisticsOverviewTable nonEmptyStatistics = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertFalse(nonEmptyStatistics.isEmpty());
        }

        {
            final StatisticsOverviewTable frStatisitics = vra.createUnmodifiedStatisticsOverviewTable("CERT-FR", 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertFalse(frStatisitics.isEmpty());
            Assert.assertEquals(Arrays.asList("Severity", "Reviewed", "Total", "% Assessed"), frStatisitics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), frStatisitics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(2, 2, 100), frStatisitics.getCountsForSeverityCategory("critical"));
        }

        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.HIGH.toString()));

        {
            final StatisticsOverviewTable statistics = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertFalse(statistics.isEmpty());
            Assert.assertEquals(Arrays.asList("Severity", "Reviewed", "In Review", "Total", "% Assessed"), statistics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), statistics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(2, 1, 3, 66), statistics.getCountsForSeverityCategory("critical"));
        }

        {
            final StatisticsOverviewTable seiStatistics = vra.createUnmodifiedStatisticsOverviewTable("CERT-SEI", 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertEquals(Arrays.asList("Severity", "Reviewed", "Total", "% Assessed"), seiStatistics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), seiStatistics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(1, 1, 100), seiStatistics.getCountsForSeverityCategory("critical"));
        }

        {
            final StatisticsOverviewTable statistics = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertEquals(Arrays.asList("Severity", "Reviewed", "In Review", "Total", "% Assessed"), statistics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), statistics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(2, 1, 3, 66), statistics.getCountsForSeverityCategory("critical"));
            Assert.assertEquals(Arrays.asList(0, 1, 1, 0), statistics.getCountsForSeverityCategory("high"));
        }

        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_VOID, null, "MSRC"));

        {
            final StatisticsOverviewTable msrcStatistics = vra.createUnmodifiedStatisticsOverviewTable("MSRC", 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertEquals(Arrays.asList("Severity", "Void", "Total", "% Assessed"), msrcStatistics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low", "Unset"), msrcStatistics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(0, 0, 100), msrcStatistics.getCountsForSeverityCategory("high"));
            Assert.assertEquals(Arrays.asList(1, 1, 100), msrcStatistics.getCountsForSeverityCategory("unset"));
        }

        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, SEVERITY.MEDIUM.toString(), "MSRC"));

        {
            final StatisticsOverviewTable msrcStatistics = vra.createUnmodifiedStatisticsOverviewTable("MSRC", 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
            Assert.assertEquals(Arrays.asList("Severity", "Insignificant", "Void", "Total", "% Assessed"), msrcStatistics.getHeaders());
            Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low", "Unset"), msrcStatistics.getSeverityCategories());
            Assert.assertEquals(Arrays.asList(0, 0, 0, 100), msrcStatistics.getCountsForSeverityCategory("high"));
            Assert.assertEquals(Arrays.asList(0, 1, 1, 100), msrcStatistics.getCountsForSeverityCategory("unset"));
            Assert.assertEquals(Arrays.asList(1, 0, 1, 0), msrcStatistics.getCountsForSeverityCategory("medium"));
        }
    }

    @Test
    public void createStatisticsOverviewTableAddStatusAfterwardsTest() {
        Inventory inventory = new Inventory();
        VulnerabilityReportAdapter vra = new VulnerabilityReportAdapter(inventory);

        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.HIGH.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.LOW.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_VOID, null));

        final StatisticsOverviewTable table = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
        Assert.assertEquals(Arrays.asList(0, 1, 1, 100), table.getCountsForSeverityCategory("unset"));
    }

    @Test
    public void createStatisticsOverviewForEffectiveValuesTest() {
        Inventory inventory = new Inventory();
        VulnerabilityReportAdapter vra = new VulnerabilityReportAdapter(inventory);

        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("reviewed", SEVERITY.CRITICAL.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("in review", SEVERITY.CRITICAL.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("void", SEVERITY.CRITICAL.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("insignificant", SEVERITY.CRITICAL.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("not applicable", SEVERITY.CRITICAL.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("reviewed", SEVERITY.HIGH.toString(), SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("reviewed", SEVERITY.HIGH.toString(), SEVERITY.HIGH.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("in review", SEVERITY.MEDIUM.toString(), SEVERITY.HIGH.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDMultipleSeverities("in review", SEVERITY.MEDIUM.toString(), SEVERITY.MEDIUM.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("in review", SEVERITY.MEDIUM.toString()));

        final StatisticsOverviewTable table = vra.createModifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT);
        Assert.assertEquals(Arrays.asList("Severity", "Reviewed", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "% Assessed"), table.getHeaders());
        Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), table.getSeverityCategories());
        Assert.assertEquals(Arrays.asList(2, 1, 1, 1, 1, 6, 60), table.getCountsForSeverityCategory("critical"));
        Assert.assertEquals(Arrays.asList(1, 1, 0, 0, 0, 2, 50), table.getCountsForSeverityCategory("high"));
        Assert.assertEquals(Arrays.asList(0, 2, 0, 0, 0, 2, 0), table.getCountsForSeverityCategory("medium"));
        Assert.assertEquals(Arrays.asList(0, 0, 0, 0, 0, 0, 100), table.getCountsForSeverityCategory("low"));
    }

    @Test
    public void createStatisticsOverviewAbstractedStatusMapperTest() {
        final Inventory inventory = new Inventory();
        final VulnerabilityReportAdapter vra = new VulnerabilityReportAdapter(inventory);

        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("reviewed", SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("in review", SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("void", SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("insignificant", SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("not applicable", SEVERITY.CRITICAL.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("reviewed", SEVERITY.HIGH.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("reviewed", SEVERITY.HIGH.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("in review", SEVERITY.MEDIUM.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("in review", SEVERITY.MEDIUM.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("in review", SEVERITY.MEDIUM.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity(null, SEVERITY.LOW.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("", SEVERITY.LOW.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("reviewed", SEVERITY.LOW.toString()));
        inventory.getVulnerabilityMetaData().add(createVMDUnmodifiedSeverity("applicable", SEVERITY.LOW.toString()));

        final StatisticsOverviewTable table = vra.createUnmodifiedStatisticsOverviewTable(null, 0.0f, StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_ABSTRACTED);

        Assert.assertEquals(Arrays.asList("Severity", "Affected", "Potentially Affected", "Not Affected", "Total", "% Assessed"), table.getHeaders());
        Assert.assertEquals(Arrays.asList("Critical", "High", "Medium", "Low"), table.getSeverityCategories());
        Assert.assertEquals(Arrays.asList(1, 1, 3, 5, 80), table.getCountsForSeverityCategory("critical"));
        Assert.assertEquals(Arrays.asList(2, 0, 0, 2, 100), table.getCountsForSeverityCategory("high"));
        Assert.assertEquals(Arrays.asList(0, 3, 0, 3, 0), table.getCountsForSeverityCategory("medium"));
        Assert.assertEquals(Arrays.asList(2, 2, 0, 4, 50), table.getCountsForSeverityCategory("low"));
    }

    private VulnerabilityMetaData createVMDUnmodifiedSeverity(String status, String severity, String... cert) {
        VulnerabilityMetaData vmd = new VulnerabilityMetaData();

        vmd.set(VulnerabilityMetaData.Attribute.STATUS, status);
        vmd.set("CVSS Unmodified Severity (v3)", severity);
        JSONArray certs = new JSONArray();
        for (String c : cert) {
            certs.put(new JSONObject().put("source", c));
        }
        vmd.set("Advisories", certs.toString());

        return vmd;
    }

    private VulnerabilityMetaData createVMDMultipleSeverities(String status, String severityUnmodified, String severityModified, String... cert) {
        VulnerabilityMetaData vmd = new VulnerabilityMetaData();

        vmd.set(VulnerabilityMetaData.Attribute.STATUS, status);
        vmd.set("CVSS Unmodified Severity (v3)", severityUnmodified);
        vmd.set("CVSS Modified Severity (v3)", severityModified);
        JSONArray certs = new JSONArray();
        for (String c : cert) {
            certs.put(new JSONObject().put("source", c));
        }
        vmd.set("Advisories", certs.toString());

        return vmd;
    }
}