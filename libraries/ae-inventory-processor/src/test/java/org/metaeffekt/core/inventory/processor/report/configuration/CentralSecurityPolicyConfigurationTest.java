package org.metaeffekt.core.inventory.processor.report.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.util.Arrays;

public class CentralSecurityPolicyConfigurationTest {

    @Test
    public void securityConfigurationSelectorTest() {
        Cvss3P1 someOtherVector = new Cvss3P1("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.findByNameOrMailOrCreateNew("some-other"), Cvss3P1.class));
        Cvss3P1 assessmentUnknownVector = new Cvss3P1("A:L", new CvssSource(KnownCvssEntities.ASSESSMENT, Cvss3P1.class));
        Cvss3P1 assessmentAllVector = new Cvss3P1("A:L", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));
        Cvss3P1 assessmentLowerVector = new Cvss3P1("A:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));
        Cvss3P1 assessmentHigherVector = new Cvss3P1("A:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class));

        Cvss3P1 someOtherAssessmentAllCombinedVector = someOtherVector.clone();
        someOtherAssessmentAllCombinedVector.applyVector(assessmentUnknownVector);

        Cvss3P1 someOtherAssessmentLowerCombinedVector = someOtherVector.clone();
        someOtherAssessmentLowerCombinedVector.applyVector(assessmentLowerVector);

        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_BASE.selectVector(Arrays.asList(someOtherVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_BASE.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_BASE.selectVector(Arrays.asList(assessmentUnknownVector, someOtherVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_BASE.selectVector(Arrays.asList(assessmentUnknownVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_BASE.selectVector(Arrays.asList(assessmentLowerVector)));

        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_EFFECTIVE.selectVector(Arrays.asList(someOtherVector)));
        Assert.assertNull(CentralSecurityPolicyConfiguration.CVSS_SELECTOR_EFFECTIVE.selectVector(Arrays.asList(someOtherVector, assessmentUnknownVector)));
        Assert.assertEquals(someOtherAssessmentAllCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_EFFECTIVE.selectVector(Arrays.asList(someOtherVector, assessmentAllVector)));
        Assert.assertEquals(someOtherAssessmentLowerCombinedVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_EFFECTIVE.selectVector(Arrays.asList(someOtherVector, assessmentLowerVector)));
        Assert.assertEquals(someOtherVector, CentralSecurityPolicyConfiguration.CVSS_SELECTOR_EFFECTIVE.selectVector(Arrays.asList(someOtherVector, assessmentHigherVector)));
    }
}