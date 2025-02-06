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
import org.metaeffekt.core.security.cvss.v3.Cvss3P0;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;

public class CvssVectorTest {

    @Test
    public void parseStaticVersionTest() {
        Assert.assertEquals(Cvss3P1.class, CvssVector.classFromVersionName(Cvss3P1.getVersionName()));
    }

    @Test
    public void cloneVectorsTest() {
        assertCloneEquals(new Cvss2("AV:N/AC:M/Au:S/C:C/I:C/A:N/E:U/RL:TF/RC:UR/CDP:L/TD:M/CR:M/IR:L/AR:H"));
        assertCloneEquals(new Cvss3P0("CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:C/C:N/I:H/A:N/E:U/RL:W/RC:R/MAV:N/MAC:H/MPR:N/MUI:R/MS:X/MC:L/MI:X/MA:H/CR:M/IR:M/AR:L"));
        assertCloneEquals(new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:C/C:N/I:H/A:N/E:U/RL:W/RC:R/MAV:N/MAC:H/MPR:N/MUI:R/MS:X/MC:L/MI:X/MA:H/CR:M/IR:M/AR:L"));
        assertCloneEquals(new Cvss4P0("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:H/VA:N/SC:N/SI:L/SA:H/E:U/CR:H/IR:L/AR:M/MAV:A/MAC:H/MAT:N/MPR:H/MUI:P/MVC:H/MVI:H/MVA:N/MSC:N/MSI:H/MSA:S/S:N/AU:Y/R:U/V:D/RE:H/U:Green"));
    }

    private void assertCloneEquals(CvssVector cvss) {
        CvssVector clone = cvss.clone();
        Assert.assertEquals(cvss, clone);
        Assert.assertNotSame(cvss, clone);
        Assert.assertEquals(cvss.getBakedScores(), clone.getBakedScores());
    }
}
