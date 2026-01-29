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

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaCertFrAdvisorEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaCertSeiAdvisorEntry;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.util.*;
import java.util.function.Function;

@Slf4j
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

        Assertions.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(List.of(someOtherVector)));
        Assertions.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assertions.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(assessmentUnknownVector, someOtherVector)));
        Assertions.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(List.of(assessmentUnknownVector)));
        Assertions.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_INITIAL.selectVector(List.of(assessmentLowerVector)));

        Assertions.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(List.of(someOtherVector)));
        Assertions.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assertions.assertEquals(someOtherAssessmentAllCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentAllVector)));
        Assertions.assertEquals(someOtherAssessmentLowerCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentLowerVector)));
        Assertions.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(someOtherVector, assessmentHigherVector)));
    }

    @Test
    public void statusMapperUnmodifiedTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED.getMapper();
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(null));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(""));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_VOID, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE, mapper.apply("potential vulnerability")); // apparently we had the case once that the status was "potential vulnerability"
        Assertions.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void statusMapperAbstractedTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED.getMapper();
        Assertions.assertEquals("potentially affected", mapper.apply(null));
        Assertions.assertEquals("potentially affected", mapper.apply(""));
        Assertions.assertEquals("potentially affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assertions.assertEquals("affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assertions.assertEquals("not affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assertions.assertEquals("potentially affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assertions.assertEquals("not affected", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assertions.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void statusMapperReviewStateTest() {
        final Function<String, String> mapper = CentralSecurityPolicyConfiguration.VULNERABILITY_STATUS_DISPLAY_MAPPER_REVIEW_STATE.getMapper();
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(null));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(""));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW));
        Assertions.assertEquals("reviewed", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_APPLICABLE));
        Assertions.assertEquals("reviewed", mapper.apply(VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT));
        Assertions.assertEquals(VulnerabilityMetaData.STATUS_VALUE_VOID, mapper.apply(VulnerabilityMetaData.STATUS_VALUE_VOID));
        Assertions.assertEquals("some other", mapper.apply("some other"));
    }

    @Test
    public void collectMisconfigurationIncludeAdvisoryTypesTest() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("all"));
        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("some-other"));
        Assertions.assertFalse(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("notice"));
        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("alert"));
        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Collections.singletonList("news"));
        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Arrays.asList("notice", "alert", "news"));
        Assertions.assertTrue(securityPolicy.collectMisconfigurations().isEmpty());

        securityPolicy.setIncludeAdvisoryTypes(Arrays.asList("some-other", "notice", "alert", "news"));
        Assertions.assertFalse(securityPolicy.collectMisconfigurations().isEmpty());
    }

    @Test
    public void anyProviderTest() {
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.isAny((String) null));
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.isAny((Map.Entry<String, ?>) null));
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.isAny(""));

        Assertions.assertTrue(CentralSecurityPolicyConfiguration.isAny("any"));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.isAny("ANY"));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.isAny("all"));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.isAny("ALL"));
    }

    @Test
    public void anyProviderListTest() {
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.containsAny((Collection<String>) null));
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.containsAny((Map<String, String>) null));
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Collections.emptyList()));

        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("any")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("ANY")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("all")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Collections.singletonList("ALL")));

        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("any", "all")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("ANY", "ALL")));

        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("any", "some-other")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("ANY", "SOME-OTHER")));

        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("some-other", "any")));
        Assertions.assertTrue(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("SOME-OTHER", "ANY")));

        Assertions.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("some-other", "another-other")));
        Assertions.assertFalse(CentralSecurityPolicyConfiguration.containsAny(Arrays.asList("SOME-OTHER", "ANOTHER-OTHER")));
    }

    @Test
    public void isAdvisoryContainedRegardingSourceTest() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();
        final AeaaCertFrAdvisorEntry certFr = new AeaaCertFrAdvisorEntry();
        final AeaaCertSeiAdvisorEntry certSei = new AeaaCertSeiAdvisorEntry();

        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("all", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("ANY", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-FR", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT_FR", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(new HashMap<String, String>() {{
            put("CERT_FR", "");
            put("CERT-SEI", "");
        }}).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertFalse(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT_SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));
        Assertions.assertFalse(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certFr));

        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(Collections.singletonMap("CERT-SEI", "")).isSecurityAdvisoryIncludedRegardingEntryProvider(certSei));
        Assertions.assertTrue(securityPolicy.setIncludeAdvisoryProviders(new HashMap<String, String>() {{
            put("CERT_FR", "");
            put("any", "");
        }}).isSecurityAdvisoryIncludedRegardingEntryProvider(certSei));
    }

    @Test
    public void dumpDefaultPolicy() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();
        securityPolicy.logConfiguration();
    }

    @Test
    public void conversionTest() {
        final CentralSecurityPolicyConfiguration securityPolicy = new CentralSecurityPolicyConfiguration();

        final JSONObject pc1Properties = new JSONObject(new JSONObject(securityPolicy.getProperties()).toString());
        securityPolicy.setProperties(pc1Properties.toMap());
        final JSONObject pc2Properties = new JSONObject(new JSONObject(securityPolicy.getProperties()).toString());

        log.info("{}", pc1Properties);
        log.info("{}", pc2Properties);
    }
}
