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
package org.metaeffekt.core.inventory.processor.report.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaCertFrAdvisorEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaCertSeiAdvisorEntry;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.util.*;
import java.util.function.Function;

public class CentralSecurityPolicyConfigurationTest {

    @Test
    public void securityConfigurationSelectorTest() {
        final Cvss3P1 someOtherVector = new Cvss3P1("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.findByNameOrMailOrCreateNew("some-other"), Cvss3P1.class));
        final Cvss3P1 assessmentUnknownVector = new Cvss3P1("A:L", new CvssSource(KnownCvssEntities.ASSESSMENT, Cvss3P1.class));
        final Cvss3P1 assessmentAllVector = new Cvss3P1("A:L", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));
        final Cvss3P1 assessmentLowerVector = new Cvss3P1("A:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));
        final Cvss3P1 assessmentHigherVector = new Cvss3P1("A:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class));

        final Cvss3P1 someOtherAssessmentAllCombinedVector = someOtherVector.clone();
        someOtherAssessmentAllCombinedVector.applyVector(assessmentUnknownVector);

        final Cvss3P1 someOtherAssessmentLowerCombinedVector = someOtherVector.clone();
        someOtherAssessmentLowerCombinedVector.applyVector(assessmentLowerVector);

        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(someOtherVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(assessmentUnknownVector, someOtherVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(assessmentUnknownVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(assessmentLowerVector)));

        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assert.assertEquals(someOtherAssessmentAllCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentAllVector)));
        Assert.assertEquals(someOtherAssessmentLowerCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentLowerVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentHigherVector)));
    }

    @Test
    public void statusMapperUnmodifiedTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED.getMapper();
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(null));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(""));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_VOID, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, mapper.apply("potential vulnerability")); // apparently we had the case once that the status was "potential vulnerability"
        Assert.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void statusMapperAbstractedTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED.getMapper();
        Assert.assertEquals("potentially affected", mapper.apply(null));
        Assert.assertEquals("potentially affected", mapper.apply(""));
        Assert.assertEquals("potentially affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assert.assertEquals("affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assert.assertEquals("not affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assert.assertEquals("potentially affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assert.assertEquals("not affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assert.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void statusMapperReviewStateTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE.getMapper();
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(null));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(""));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assert.assertEquals("reviewed", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assert.assertEquals("reviewed", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assert.assertEquals(VulnerabilityMetaData.STATUS_VALUE_VOID, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assert.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void collectMisconfigurationIncludeAdvisoryTypesTest() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("all"));
        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("some-other"));
        Assert.assertFalse(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("notice"));
        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("alert"));
        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("news"));
        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Arrays.asList("notice", "alert", "news"));
        Assert.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Arrays.asList("some-other", "notice", "alert", "news"));
        Assert.assertFalse(securityPolicy.collectMisconfigurations().isEmpty());
    }

    @Test
    public void anyProviderTest() {
        Assert.assertFalse(CentralSecurityPolicyConfiguration.isAny((String) null));
        Assert.assertFalse(CentralSecurityPolicyConfiguration.isAny((Map.Entry<String, ?>) null));
        Assert.assertFalse(CentralSecurityPolicyConfiguration.isAny(""));

        Assert.assertTrue(CentralSecurityPolicyConfiguration.isAny("any"));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.isAny("ANY"));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.isAny("all"));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.isAny("ALL"));
    }

    @Test
    public void anyProviderListTest() {
        Assert.assertFalse(CentralSecurityPolicyConfiguration.containsAny((Collection<String>) null));
        Assert.assertFalse(CentralSecurityPolicyConfiguration.containsAny((Map<String, String>) null));
        Assert.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Collections.emptyList()));

        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("any")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("ANY")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("all")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("ALL")));

        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("any", "all")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("ANY", "ALL")));

        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("any", "some-other")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("ANY", "SOME-OTHER")));

        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("some-other", "any")));
        Assert.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("SOME-OTHER", "ANY")));

        Assert.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("some-other", "another-other")));
        Assert.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("SOME-OTHER", "ANOTHER-OTHER")));
    }

    @Test
    public void isAdvisoryContainedRegardingSourceTest() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();
        final AeaaCertFrAdvisorEntry certFr = new AeaaCertFrAdvisorEntry();
        final AeaaCertSeiAdvisorEntry certSei = new AeaaCertSeiAdvisorEntry();

        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("all", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("ANY", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-FR", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT_FR", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(new HashMap<String, String>() {{
            put("CERT_FR", "");
            put("CERT-SEI", "");
        }}).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertFalse(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT_SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assert.assertFalse(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));

        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certSei));
        Assert.assertTrue(securityPolicy.setIncludeAdvisoryProviders(new HashMap<String, String>() {{
            put("CERT_FR", "");
            put("any", "");
        }}).isSecurityAdvisoryIncludedRegardingEntryProvider(certSei));
    }

    @Test
    public void dumpDefaultPolicy() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

        securityPolicy.logConfiguration();

    }
}
