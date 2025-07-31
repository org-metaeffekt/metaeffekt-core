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
package org.metaeffekt.core.inventory.processor.report;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerabilityContextInventory;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.assessment.AeaaVulnerabilityAssessmentEvent;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class StatisticsOverviewTableTest {

    private final CentralSecurityPolicyConfiguration unmodifiedMapperSecurityPolicy = new CentralSecurityPolicyConfiguration()
            .setVulnerabilityStatusDisplayMapper(CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED)
            .setInsignificantThreshold(0.0);
    private final CentralSecurityPolicyConfiguration abstractedMapperSecurityPolicy = new CentralSecurityPolicyConfiguration()
            .setVulnerabilityStatusDisplayMapper(CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED)
            .setInsignificantThreshold(0.0);

    @Test
    @Ignore
    public void runOnInventoryTest() throws IOException {
        final Inventory inventory = new InventoryReader().readInventory(new File(""));
        final AeaaVulnerabilityContextInventory vInventory = AeaaVulnerabilityContextInventory.fromInventory(inventory);
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

        vInventory.calculateEffectiveCvssVectorsForVulnerabilities(securityPolicy);

        System.out.println(StatisticsOverviewTable.buildTableStrFilterAdvisor(securityPolicy, vInventory.getVulnerabilities(), null, false));
        System.out.println(StatisticsOverviewTable.buildTableStrFilterAdvisor(securityPolicy, vInventory.getVulnerabilities(), null, true));
    }

    private enum Severity {
        CRITICAL(new Cvss3P1("AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:H/A:H")),
        HIGH(new Cvss3P1("AV:L/AC:L/PR:N/UI:R/S:C/C:L/I:H/A:H")),
        MEDIUM(new Cvss3P1("AV:L/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:L")),
        LOW(new Cvss3P1("AV:L/AC:L/PR:H/UI:R/S:C/C:N/I:N/A:L")),
        NONE(new Cvss3P1("AV:P/AC:L/PR:H/UI:R/S:U/C:N/I:N/A:N"));

        private final Cvss3P1 v3;

        Severity(Cvss3P1 v3) {
            this.v3 = v3;
        }

        public Cvss3P1 getV3() {
            return v3;
        }
    }

    @Test
    public void assertSeverityEnumResultsInCorrectSeverityCategoriesTest() {
        for (Severity severityLevel : Severity.values()) {
            Assert.assertEquals(severityLevel.name().toLowerCase(), CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(severityLevel.getV3().getOverallScore()).getName().toLowerCase());
        }
    }

    @Test
    public void createStatisticsOverviewTableTest() {
        final List<AeaaVulnerability> vulnerabilities = new ArrayList<>();

        // start with an empty inventory
        {
            final StatisticsOverviewTable emptyStatistics = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, false);
            Assert.assertTrue(emptyStatistics.isEmpty());
        }

        // add two VMDs (one CERT-FR, one CERT-FR + CERT-SEI; both applicable)
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.CRITICAL.getV3(), AeaaAdvisoryTypeStore.CERT_FR));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.CRITICAL.getV3(), AeaaAdvisoryTypeStore.CERT_FR, AeaaAdvisoryTypeStore.CERT_SEI));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        {
            final StatisticsOverviewTable nonEmptyStatistics = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, false);
            Assert.assertFalse(nonEmptyStatistics.isEmpty());
        }

        {
            final StatisticsOverviewTable frStatisitics = StatisticsOverviewTable.buildTable(unmodifiedMapperSecurityPolicy, vulnerabilities, AeaaAdvisoryTypeStore.CERT_FR, false);
            Assert.assertFalse(frStatisitics.isEmpty());
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), frStatisitics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), frStatisitics.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 2, 0, 0, 0, 0, 2, "100,0 %"), frStatisitics.getTableRowValues("critical"));
        }

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.HIGH.getV3()));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        {
            final StatisticsOverviewTable statistics = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, false);
            Assert.assertFalse(statistics.isEmpty());
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), statistics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), statistics.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 2, 1, 0, 0, 0, 3, "66,7 %"), statistics.getTableRowValues("critical"));
        }

        {
            final StatisticsOverviewTable seiStatistics = StatisticsOverviewTable.buildTable(unmodifiedMapperSecurityPolicy, vulnerabilities, AeaaAdvisoryTypeStore.CERT_SEI, false);
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), seiStatistics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), seiStatistics.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 1, 0, 0, 0, 0, 1, "100,0 %"), seiStatistics.getTableRowValues("critical"));
        }

        {
            final StatisticsOverviewTable statistics = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, false);
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), statistics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), statistics.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 2, 1, 0, 0, 0, 3, "66,7 %"), statistics.getTableRowValues("critical"));
            Assert.assertEquals(constructList("High", 0, 1, 0, 0, 0, 1, "0,0 %"), statistics.getTableRowValues("high"));
        }

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_VOID, null, AeaaAdvisoryTypeStore.MSRC));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        {
            final StatisticsOverviewTable msrcStatistics = StatisticsOverviewTable.buildTable(unmodifiedMapperSecurityPolicy, vulnerabilities, AeaaAdvisoryTypeStore.MSRC, false);
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), msrcStatistics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low", "None"), msrcStatistics.getSeverityCategories());
            Assert.assertEquals(constructList("High", 0, 0, 0, 0, 0, 0, "n/a"), msrcStatistics.getTableRowValues("high"));
            Assert.assertEquals(constructList("None", 0, 0, 0, 0, 1, 1, "100,0 %"), msrcStatistics.getTableRowValues("none"));
        }

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, Severity.MEDIUM.getV3(), AeaaAdvisoryTypeStore.MSRC));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        {
            final StatisticsOverviewTable msrcStatistics = StatisticsOverviewTable.buildTable(unmodifiedMapperSecurityPolicy, vulnerabilities, AeaaAdvisoryTypeStore.MSRC, false);
            Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), msrcStatistics.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low", "None"), msrcStatistics.getSeverityCategories());
            Assert.assertEquals(constructList("High", 0, 0, 0, 0, 0, 0, "n/a"), msrcStatistics.getTableRowValues("high"));
            Assert.assertEquals(constructList("None", 0, 0, 0, 0, 1, 1, "100,0 %"), msrcStatistics.getTableRowValues("none"));
            Assert.assertEquals(constructList("Medium", 0, 0, 0, 1, 0, 1, "0,0 %"), msrcStatistics.getTableRowValues("medium"));
        }
    }

    @Test
    public void createStatisticsOverviewTableAddStatusAfterwardsTest() {
        final List<AeaaVulnerability> vulnerabilities = new ArrayList<>();

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.HIGH.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.LOW.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_VOID, null));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        final StatisticsOverviewTable table = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, false);
        Assert.assertEquals("\n" + table, constructList("None", 0, 0, 0, 0, 1, 1, "100,0 %"), table.getTableRowValues("None"));
    }

    @Test
    public void createStatisticsOverviewForEffectiveValuesTest() {
        final List<AeaaVulnerability> vulnerabilities = new ArrayList<>();

        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.CRITICAL.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, Severity.CRITICAL.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_VOID, Severity.CRITICAL.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, Severity.CRITICAL.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, Severity.CRITICAL.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.HIGH.getV3(), Severity.CRITICAL.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.HIGH.getV3(), Severity.HIGH.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, Severity.MEDIUM.getV3(), Severity.HIGH.getV3()));
        vulnerabilities.add(createVulnerabilityMultipleSeverities(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, Severity.MEDIUM.getV3(), Severity.MEDIUM.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, Severity.MEDIUM.getV3()));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        final StatisticsOverviewTable table = StatisticsOverviewTable.buildTableStrFilterAdvisor(unmodifiedMapperSecurityPolicy, vulnerabilities, null, true);
        Assert.assertEquals(constructList("Critical", "High", "Medium", "Low", "None"), table.getSeverityCategories());
        Assert.assertEquals(constructList("Severity", "Applicable", "In Review", "Not Applicable", "Insignificant", "Void", "Total", "Assessed"), table.getHeaders());
        Assert.assertEquals(constructList("Critical", 2, 1, 0, 1, 0, 4, "50,0 %"), table.getTableRowValues("critical"));
        Assert.assertEquals(constructList("High", 1, 1, 0, 0, 0, 2, "50,0 %"), table.getTableRowValues("high"));
        Assert.assertEquals(constructList("Medium", 0, 2, 0, 0, 0, 2, "0,0 %"), table.getTableRowValues("medium"));
        Assert.assertEquals(constructList("Low", 0, 0, 0, 0, 0, 0, "n/a"), table.getTableRowValues("low"));
        Assert.assertEquals(constructList("None", 0, 0, 1, 0, 1, 2, "100,0 %"), table.getTableRowValues("none"));
    }

    @Test
    public void createStatisticsOverviewAbstractedStatusMapperTest() {
        final List<AeaaVulnerability> vulnerabilities = new ArrayList<>();

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.CRITICAL.getV3())); // affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, Severity.CRITICAL.getV3())); // potentially affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.CRITICAL.getV3())); // potentially affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, Severity.CRITICAL.getV3())); // not affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_VOID, Severity.CRITICAL.getV3())); // not affected

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.HIGH.getV3())); // affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.HIGH.getV3())); // affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.MEDIUM.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.MEDIUM.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.MEDIUM.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(null, Severity.LOW.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity("", Severity.LOW.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.LOW.getV3()));
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.LOW.getV3()));
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        final StatisticsOverviewTable table = StatisticsOverviewTable.buildTableStrFilterAdvisor(abstractedMapperSecurityPolicy, vulnerabilities, null, false);

        Assert.assertEquals(constructList("Severity", "Affected", "Potentially Affected", "Not Affected", "Total", "Assessed"), table.getHeaders());
        Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), table.getSeverityCategories());
        Assert.assertEquals(constructList("Critical", 1, 2, 2, 5, "60,0 %"), table.getTableRowValues("critical"));
        Assert.assertEquals(constructList("High", 2, 0, 0, 2, "100,0 %"), table.getTableRowValues("high"));
        Assert.assertEquals(constructList("Medium", 0, 3, 0, 3, "0,0 %"), table.getTableRowValues("medium"));
        Assert.assertEquals(constructList("Low", 2, 2, 0, 4, "50,0 %"), table.getTableRowValues("low"));
    }

    @Test
    public void createStatisticsOverviewForNotApplicableCaseAbstractedStatusMapperTest() {
        final List<AeaaVulnerability> vulnerabilities = new ArrayList<>();

        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, Severity.CRITICAL.getV3())); // not affected
        vulnerabilities.add(createVulnerabilityUnmodifiedSeverity(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, Severity.HIGH.getV3())); // affected
        precalculateCvss(vulnerabilities, unmodifiedMapperSecurityPolicy);

        {
            final StatisticsOverviewTable table = StatisticsOverviewTable.buildTableStrFilterAdvisor(abstractedMapperSecurityPolicy, vulnerabilities, null, false);
            Assert.assertEquals(constructList("Severity", "Affected", "Potentially Affected", "Not Affected", "Total", "Assessed"), table.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low"), table.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 0, 0, 1, 1, "100,0 %"), table.getTableRowValues("critical"));
            Assert.assertEquals(constructList("High", 1, 0, 0, 1, "100,0 %"), table.getTableRowValues("high"));
            Assert.assertEquals(constructList("Medium", 0, 0, 0, 0, "n/a"), table.getTableRowValues("medium"));
            Assert.assertEquals(constructList("Low", 0, 0, 0, 0, "n/a"), table.getTableRowValues("low"));
            Assert.assertEquals(0, table.getTableRowValues("none").size());
        }

        {
            final StatisticsOverviewTable table = StatisticsOverviewTable.buildTableStrFilterAdvisor(abstractedMapperSecurityPolicy, vulnerabilities, null, true);
            Assert.assertEquals(constructList("Severity", "Affected", "Potentially Affected", "Not Affected", "Total", "Assessed"), table.getHeaders());
            Assert.assertEquals(constructList("Critical", "High", "Medium", "Low", "None"), table.getSeverityCategories());
            Assert.assertEquals(constructList("Critical", 0, 0, 0, 0, "n/a"), table.getTableRowValues("critical"));
            Assert.assertEquals(constructList("High", 1, 0, 0, 1, "100,0 %"), table.getTableRowValues("high"));
            Assert.assertEquals(constructList("Medium", 0, 0, 0, 0, "n/a"), table.getTableRowValues("medium"));
            Assert.assertEquals(constructList("Low", 0, 0, 0, 0, "n/a"), table.getTableRowValues("low"));
            Assert.assertEquals(constructList("None", 0, 0, 1, 1, "100,0 %"), table.getTableRowValues("none"));
        }
    }

    private AeaaVulnerability createVulnerabilityUnmodifiedSeverity(String status, CvssVector cvssVector, AeaaAdvisoryTypeIdentifier<?>... advisoryProviders) {
        return createVulnerabilityMultipleSeverities(status, cvssVector, null, advisoryProviders);
    }

    private AeaaVulnerability createVulnerabilityMultipleSeverities(String status, CvssVector cvssVectorProvided, CvssVector cvssVectorEffective, AeaaAdvisoryTypeIdentifier<?>... advisoryProviders) {
        final AeaaVulnerability vulnerability = new AeaaVulnerability(status + "-" + (cvssVectorProvided == null ? null : cvssVectorProvided.getOverallScore()) + "-" + (cvssVectorEffective == null ? null : cvssVectorEffective.getOverallScore()) + "-" + Arrays.toString(advisoryProviders) + "-" + UUID.randomUUID());

        if (cvssVectorProvided != null) {
            final CvssVector vector = cvssVectorProvided.deriveAddSource(new CvssSource(KnownCvssEntities.NVD, cvssVectorProvided.getClass()));
            vulnerability.getCvssVectors().addCvssVector(vector);
        }

        if (cvssVectorEffective != null) {
            final CvssVector vector = cvssVectorEffective.deriveAddSource(new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, cvssVectorEffective.getClass()));
            vulnerability.getCvssVectors().addCvssVector(vector);
        }

        if (status != null) {
            final AeaaVulnerabilityAssessmentEvent event = new AeaaVulnerabilityAssessmentEvent();
            event.setStatus(status);
            event.setDate(new Date().getTime());
            vulnerability.getAssessmentEvents().add(event);
        }

        if (advisoryProviders != null && advisoryProviders.length > 0) {
            final Random random = new Random(Arrays.hashCode(advisoryProviders));
            for (AeaaAdvisoryTypeIdentifier<?> advisoryProvider : advisoryProviders) {
                final AeaaAdvisoryEntry securityAdvisory = advisoryProvider.getAdvisoryFactory().get();
                securityAdvisory.setId(advisoryProvider.name() + "-" + random.nextInt(100000));
                vulnerability.addSecurityAdvisoryUnmanaged(securityAdvisory);
            }
        }

        return vulnerability;
    }

    private void precalculateCvss(Collection<AeaaVulnerability> vulnerability, CentralSecurityPolicyConfiguration securityPolicy) {
        vulnerability.forEach(v -> v.selectEffectiveCvssVectors(securityPolicy));
    }

    private List<String> constructList(Object... values) {
        return Arrays.stream(values).map(Object::toString).collect(Collectors.toList());
    }
}