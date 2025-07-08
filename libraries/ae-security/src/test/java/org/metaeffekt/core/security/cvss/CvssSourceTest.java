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
package org.metaeffekt.core.security.cvss;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CvssSourceTest {

    private static final Logger log = LoggerFactory.getLogger(CvssSourceTest.class);

    @Test
    public void toColumnHeaderTest() {
        Assert.assertEquals(Cvss2.getVersionName() + " NVD-CNA-NVD", new CvssSource(KnownCvssEntities.NVD, CvssSource.CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss2.class).toColumnHeaderString());
        Assert.assertEquals(Cvss2.getVersionName() + " NVD-CNA-GitHub, Inc.", new CvssSource(KnownCvssEntities.NVD, CvssSource.CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA, Cvss2.class).toColumnHeaderString());

        Assert.assertEquals(Cvss2.getVersionName() + " Assessment-lower", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss2.class).toColumnHeaderString());
        Assert.assertEquals(Cvss2.getVersionName() + " Assessment-higher", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss2.class).toColumnHeaderString());
        Assert.assertEquals(Cvss2.getVersionName() + " Assessment", new CvssSource(KnownCvssEntities.ASSESSMENT, Cvss2.class).toColumnHeaderString());

        Assert.assertEquals(Cvss3P1.getVersionName() + " Assessment", new CvssSource(KnownCvssEntities.ASSESSMENT, Cvss3P1.class).toColumnHeaderString());
        Assert.assertEquals(Cvss4P0.getVersionName() + " Assessment", new CvssSource(KnownCvssEntities.ASSESSMENT, Cvss4P0.class).toColumnHeaderString());
    }

    @Test
    public void fromColumnHeaderTest() {
        {
            final CvssSource nvdCnaNvdEntitySource = CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " NVD-CNA-NVD");
            Assert.assertEquals(Cvss2.class, nvdCnaNvdEntitySource.getVectorClass());
            Assert.assertEquals(KnownCvssEntities.NVD, nvdCnaNvdEntitySource.getHostingEntity());
            Assert.assertEquals(KnownCvssEntities.NVD, nvdCnaNvdEntitySource.getIssuingEntity());
            Assert.assertEquals(CvssSource.CvssIssuingEntityRole.CNA, nvdCnaNvdEntitySource.getIssuingEntityRole());
        }

        {
            final CvssSource nvdCnaGhsaEntitySource = CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " NVD-CNA-GitHub, Inc.");
            Assert.assertEquals(Cvss2.class, nvdCnaGhsaEntitySource.getVectorClass());
            Assert.assertEquals(KnownCvssEntities.NVD, nvdCnaGhsaEntitySource.getHostingEntity());
            Assert.assertEquals(KnownCvssEntities.GHSA, nvdCnaGhsaEntitySource.getIssuingEntity());
            Assert.assertEquals(CvssSource.CvssIssuingEntityRole.CNA, nvdCnaGhsaEntitySource.getIssuingEntityRole());
        }

        {
            final CvssSource assessmentLowerEntitySource = CvssSource.fromColumnHeaderString(Cvss3P1.getVersionName() + " Assessment-lower");
            Assert.assertEquals(Cvss3P1.class, assessmentLowerEntitySource.getVectorClass());
            Assert.assertEquals(KnownCvssEntities.ASSESSMENT, assessmentLowerEntitySource.getHostingEntity());
            Assert.assertEquals(KnownCvssEntities.ASSESSMENT_LOWER, assessmentLowerEntitySource.getIssuingEntity());
            Assert.assertNull(assessmentLowerEntitySource.getIssuingEntityRole());
        }

        {
            final CvssSource assessmentLowerEntitySource = CvssSource.fromColumnHeaderString(Cvss4P0.getVersionName() + " Assessment");
            Assert.assertEquals(Cvss4P0.class, assessmentLowerEntitySource.getVectorClass());
            Assert.assertEquals(KnownCvssEntities.ASSESSMENT, assessmentLowerEntitySource.getHostingEntity());
            Assert.assertNull(assessmentLowerEntitySource.getIssuingEntity());
            Assert.assertNull(assessmentLowerEntitySource.getIssuingEntityRole());
        }
    }

    @Test
    public void fromUnknownColumnHeaderTest() {
        {
            final CvssSource nvdCnaNvdEntitySource = CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " DEMO:HOST-DEMO:ROLE-DEMO:ISSUER");
            Assert.assertEquals(Cvss2.class, nvdCnaNvdEntitySource.getVectorClass());
            Assert.assertEquals("DEMO:HOST", nvdCnaNvdEntitySource.getHostingEntity().getName());
            Assert.assertEquals("DEMO:ISSUER", nvdCnaNvdEntitySource.getIssuingEntity().getName());
            Assert.assertEquals("DEMO:ROLE", nvdCnaNvdEntitySource.getIssuingEntityRole().getName());
        }

        {
            final CvssSource nvdCnaNvdEntitySource = CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " DEMO:HOST-DEMO:ISSUER");
            Assert.assertEquals(Cvss2.class, nvdCnaNvdEntitySource.getVectorClass());
            Assert.assertEquals("DEMO:HOST", nvdCnaNvdEntitySource.getHostingEntity().getName());
            Assert.assertEquals("DEMO:ISSUER", nvdCnaNvdEntitySource.getIssuingEntity().getName());
            Assert.assertNull(nvdCnaNvdEntitySource.getIssuingEntityRole());
        }
    }

    @Test
    public void fromUnknownColumnHeaderEscapeCharactersTest() {
        final CvssSource nvdCnaNvdEntitySource = CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " DEMO_HOST-DEMO_\\_ROLE-DEMO_ISSUER\\_");
        Assert.assertEquals(Cvss2.class, nvdCnaNvdEntitySource.getVectorClass());
        Assert.assertEquals("DEMO-HOST", nvdCnaNvdEntitySource.getHostingEntity().getName());
        Assert.assertEquals("DEMO-ISSUER_", nvdCnaNvdEntitySource.getIssuingEntity().getName());
        Assert.assertEquals("DEMO-_ROLE", nvdCnaNvdEntitySource.getIssuingEntityRole().getName());
        Assert.assertEquals("DEMO_HOST-DEMO_\\_ROLE-DEMO_ISSUER\\_", nvdCnaNvdEntitySource.toColumnHeaderString(false, true));
        Assert.assertEquals("DEMO-HOST-DEMO-_ROLE-DEMO-ISSUER_", nvdCnaNvdEntitySource.toColumnHeaderString(false, false));
        Assert.assertEquals(Cvss2.getVersionName() + " DEMO-HOST-DEMO-_ROLE-DEMO-ISSUER_", nvdCnaNvdEntitySource.toColumnHeaderString(true, false));
    }

    @Test
    public void parseDashEmailTest() {
        final CvssSource.CvssEntity issuingEntity = KnownCvssEntities.findByNameOrMailOrCreateNew("security-alert@emc.com");
        final String escapedIssuingEntityName = issuingEntity.getEscapedName();
        Assert.assertEquals("security_alert@emc.com", escapedIssuingEntityName);

        final CvssSource nvdCnaNvdEntitySource = CvssSource.fromColumnHeaderString(Cvss3P1.getVersionName() + " NVD-CNA-" + escapedIssuingEntityName);
        Assert.assertEquals(Cvss3P1.class, nvdCnaNvdEntitySource.getVectorClass());
        Assert.assertEquals("NVD", nvdCnaNvdEntitySource.getHostingEntity().getName());
        Assert.assertEquals("CNA", nvdCnaNvdEntitySource.getIssuingEntityRole().getName());
        Assert.assertEquals("security-alert@emc.com", nvdCnaNvdEntitySource.getIssuingEntity().getName());
    }

    @Test
    public void failConditionsTest() {
        try {
            CvssSource.fromColumnHeaderString("NVD-CNA-NVD");
            Assert.fail("Parsing should fail if the version is not provided.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            CvssSource.fromColumnHeaderString("CVSS2 NVD-CNA-NVD");
            Assert.fail("Parsing should fail if the version is unknown.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            CvssSource.fromColumnHeaderString(Cvss2.getVersionName() + " NVD-CNA-NVD-TEST");
            Assert.fail("Parsing should fail if there are too many provided parts.");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void checkCvePartnersExistTest() {
        Assert.assertNotNull(KnownCvssEntities.findByNameOrMail("CVE_CNA_HONEYWELL"));
        Assert.assertNotNull(KnownCvssEntities.findByNameOrMail("CVE_CNA_INCIBE"));
        Assert.assertNotNull(KnownCvssEntities.findByNameOrMail("CVE_CNA_OKTA"));
    }
}
