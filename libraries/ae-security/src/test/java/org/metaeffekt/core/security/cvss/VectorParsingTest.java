package org.metaeffekt.core.security.cvss;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P0;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;

public class VectorParsingTest {

    @Test
    public void parseVectorWithUnknownPartsTest() {
        Assert.assertNull(CvssVector.parseVector("AV:P/AC:L/AT:P/PR:H/UI:P/VC:N/VI:N/VA:N/SC:N/SI:N/SA:H/NO:O"));
        Assert.assertNull(CvssVector.parseVector("AR:N/NO:O"));
    }

    @Test
    public void parseVectorWithKnownPartsTest() {
        Assert.assertEquals(
                Cvss2.parseVector("AV:N/AC:L/Au:N/C:P/I:N/A:N/E:U/RL:W/RC:UR/CDP:L/TD:M/CR:M/IR:H/AR:L"),
                CvssVector.parseVector("AV:N/AC:L/Au:N/C:P/I:N/A:N/E:U/RL:W/RC:UR/CDP:L/TD:M/CR:M/IR:H/AR:L"));
        Assert.assertEquals(
                Cvss3P0.parseVector("CVSS:3.0/AV:P/AC:H/PR:L/UI:R/S:C/C:H/I:L/A:H"),
                CvssVector.parseVector("CVSS:3.0/AV:P/AC:H/PR:L/UI:R/S:C/C:H/I:L/A:H"));
        Assert.assertEquals(
                Cvss3P1.parseVector("CVSS:3.1/AV:N/AC:L/PR:H/UI:R/S:U/C:L/I:N/A:L"),
                CvssVector.parseVector("CVSS:3.1/AV:N/AC:L/PR:H/UI:R/S:U/C:L/I:N/A:L"));
        Assert.assertEquals(
                Cvss3P1.parseVector("AV:N/AC:L/PR:H/UI:R/S:U/C:L/I:N/A:L"),
                CvssVector.parseVector("AV:N/AC:L/PR:H/UI:R/S:U/C:L/I:N/A:L"));
        Assert.assertEquals(
                Cvss4P0.parseVector("CVSS:4.0/AV:P/AC:L/AT:P/PR:H/UI:P/VC:N/VI:N/VA:N/SC:N/SI:N/SA:H"),
                CvssVector.parseVector("CVSS:4.0/AV:P/AC:L/AT:P/PR:H/UI:P/VC:N/VI:N/VA:N/SC:N/SI:N/SA:H"));
    }
}
