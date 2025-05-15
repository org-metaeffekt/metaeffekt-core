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
import org.metaeffekt.core.security.cvss.processor.UniversalCvssCalculatorLinkGenerator;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P0;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CvssVectorTest {

    private static final Logger log = LoggerFactory.getLogger(CvssVectorTest.class);

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

    @Test
    public void normalizeVectorTest() {
        Assert.assertEquals("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", CvssVector.normalizeVector("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"));
        Assert.assertEquals("AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N", CvssVector.normalizeVector("(AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N)"));
        Assert.assertEquals("AV:A/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", CvssVector.normalizeVector("/AV:A/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"));
        Assert.assertEquals("AV:P/AC:M/PR:L/UI:R/S:U/C:L/I:L/A:L", CvssVector.normalizeVector("CVSS:3.1/AV:P/AC:M/PR:L/UI:R/S:U/C:L/I:L/A:L"));
        Assert.assertEquals("AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", CvssVector.normalizeVector("(CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H)"));
        Assert.assertEquals("AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N", CvssVector.normalizeVector("CVSS:4/AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N"));
        Assert.assertEquals("AV:A/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", CvssVector.normalizeVector("cvss:2/AV:A/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"));
        Assert.assertEquals("AV:P/AC:M/PR:L/UI:R/S:U/C:L/I:L/A:L", CvssVector.normalizeVector("(cvss:3.1/AV:P/AC:M/PR:L/UI:R/S:U/C:L/I:L/A:L)"));
        Assert.assertEquals("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", CvssVector.normalizeVector("CVSS:3.1/(AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H)"));
        Assert.assertEquals("AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N", CvssVector.normalizeVector("CVSS:3.1/AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N)"));
        Assert.assertEquals("AV:A/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", CvssVector.normalizeVector("(CVSS:3.0)/AV:A/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"));
        Assert.assertEquals("AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", CvssVector.normalizeVector("cvss:3.1AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"));
        Assert.assertEquals("AV:L/AC:H/PR:H/UI:R/S:C/C:N/I:L/A:N", CvssVector.normalizeVector("av:l/ac:h/pr:h/ui:r/s:c/c:n/i:l/a:n"));
    }

    @Test
    public void applyPartsIfMetricV2Test() {
        // base highest + lowest
        assertPartsLowerHigherApplied(
                "CVSS:2.0/AV:N/AC:L/Au:N/C:C/I:C/A:C", "CVSS:2.0/AV:L/AC:H/Au:M/C:N/I:N/A:N",
                "CVSS:2.0/AV:L/AC:H/Au:M/C:N/I:N/A:N",
                "CVSS:2.0/AV:N/AC:L/Au:N/C:C/I:C/A:C"
        );
        // base lowest + not defined
        assertPartsLowerHigherApplied(
                "CVSS:2.0/AV:L/AC:H/Au:M/C:N/I:N/A:N", "CVSS:2.0/AV:ND/AC:ND/Au:ND/C:ND/I:ND/A:ND",
                "CVSS:2.0",
                "CVSS:2.0/AV:L/AC:H/Au:M/C:N/I:N/A:N"
        );
        // temporal/environmental lowest + highest
        assertPartsLowerHigherApplied(
                "CVSS:2.0/E:U/RL:OF/RC:UC/CDP:N/TD:N/CR:L/IR:L/AR:L", "CVSS:2.0/E:H/RL:U/RC:C/CDP:H/TD:H/CR:H/IR:H/AR:H",
                "CVSS:2.0/E:U/RL:OF/RC:UC/CDP:N/TD:N/CR:L/IR:L/AR:L",
                "CVSS:2.0/E:H/RL:U/RC:C/CDP:H/TD:H/CR:H/IR:H/AR:H"
        );
        // temporal/environmental some interesting values
        assertPartsLowerHigherApplied(
                "CVSS:2.0/AV:A/AC:L/Au:N/C:P/I:P/A:N/E:F/RL:W/RC:UR/CDP:LM/TD:M/CR:L/IR:H/AR:M", "CVSS:2.0/AV:A/AC:L/Au:N/C:P/I:P/A:N/E:ND/RL:U/RC:ND/CDP:ND/TD:H/CR:ND/IR:ND/AR:ND",
                "CVSS:2.0/AV:A/AC:L/Au:N/C:P/I:P/A:N/E:F/RL:W/RC:UR/CDP:ND/TD:M/CR:L/IR:ND/AR:M",
                "CVSS:2.0/AV:A/AC:L/Au:N/C:P/I:P/A:N/E:ND/RL:U/RC:ND/CDP:LM/TD:H/CR:ND/IR:H/AR:ND"
        );
    }

    @Test
    public void applyPartsIfMetricV3Test() {
        assertPartsLowerHigherApplied(
                "CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:N/I:L/A:N/RL:O/MAC:H", "CVSS:3.1/AV:N/MAV:P/AC:H/MAC:H/E:U/RL:W/CR:M",
                "CVSS:3.1/AV:N/AC:H/PR:L/UI:N/S:C/C:N/I:L/A:N/E:U/RL:O/MAV:P/MAC:H/CR:M",
                "CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:N/I:L/A:N/RL:W/MAC:H");

        {
            final String i = "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:C/C:H/I:H/A:H/E:H/RL:U/RC:C";
            final String m = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N/E:U/RL:O/RC:U";
            final String lower = "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:U/C:N/I:N/A:N/E:U/RL:O/RC:U";
            final String higher = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H/E:H/RL:U/RC:C";
            assertPartsLowerHigherApplied(i, m, lower, higher);
            assertPartsLowerHigherApplied(m, i, lower, higher);
        }

        // base lowest possible values + modified highest possible values
        assertPartsLowerHigherApplied(
                "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:U/C:N/I:N/A:N", "CVSS:3.1/MAV:N/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:H/MA:H/CR:X/IR:X/AR:X",
                "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:U/C:N/I:N/A:N",
                "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:U/C:N/I:N/A:N/MAV:N/MAC:L/MPR:N/MUI:N/MS:C/MC:H/MI:H/MA:H");
        // base highest possible values + modified lowest possible values + modified requirement
        assertPartsLowerHigherApplied(
                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "CVSS:3.1/MAV:P/MAC:H/MPR:H/MUI:R/MS:C/MC:N/MI:N/MA:N/CR:H/IR:M/AR:L",
                // CR:H is not applied, since medium is the center where ND is equals
                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAV:P/MAC:H/MPR:H/MUI:R/MC:N/MI:N/MA:N/IR:M/AR:L",
                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MS:C/CR:H");

        // temporal lowest set values
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:U/RL:O/RC:U", "CVSS:3.1/E:X/RL:X/RC:X",
                "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1");
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:U/RL:O/RC:U", "CVSS:3.1/E:P/RL:T/RC:R",
                "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1/E:P/RL:T/RC:R");
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:U/RL:O/RC:U", "CVSS:3.1/E:F/RL:W/RC:C",
                "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1/E:F/RL:W/RC:C");
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:U/RL:O/RC:U", "CVSS:3.1/E:H/RL:U/RC:C",
                "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1/E:H/RL:U/RC:C");
        // temporal highest
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:H/RL:U/RC:C", "CVSS:3.1/E:F/RL:W/RC:R",
                "CVSS:3.1/E:F/RL:W/RC:R",
                "CVSS:3.1/E:H/RL:U/RC:C");
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:H/RL:U/RC:C", "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1/E:U/RL:O/RC:U",
                "CVSS:3.1/E:H/RL:U/RC:C");
        assertPartsLowerHigherApplied(
                "CVSS:3.1/E:H/RL:U/RC:C", "CVSS:3.1/E:X/RL:X/RC:X",
                "CVSS:3.1/E:H/RL:U/RC:C",
                "CVSS:3.1");

        // equal parts
        assertPartsLowerHigherApplied("CVSS:3.1/AV:N", "CVSS:3.1/MAV:N",
                "CVSS:3.1/AV:N/MAV:N",
                "CVSS:3.1/AV:N/MAV:N"
        );
        assertPartsLowerHigherApplied("CVSS:3.1/C:L", "CVSS:3.1/MC:L",
                "CVSS:3.1/C:L/MC:L",
                "CVSS:3.1/C:L/MC:L"
        );
        assertPartsLowerHigherApplied("CVSS:3.1/PR:L", "CVSS:3.1/MPR:L",
                "CVSS:3.1/PR:L/MPR:L",
                "CVSS:3.1/PR:L/MPR:L"
        );
    }

    @Test
    public void applyPartsIfMetricV4Test() {
        // base lowest + base highest
        assertPartsLowerHigherApplied(
                "CVSS:4.0/AV:P/AC:H/AT:P/PR:H/UI:A/VC:N/VI:N/VA:N/SC:N/SI:N/SA:N", "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H",
                "CVSS:4.0/AV:P/AC:H/AT:P/PR:H/UI:A/VC:N/VI:N/VA:N/SC:N/SI:N/SA:N",
                "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H"
        );
        // base highest + environment lowest
        assertPartsLowerHigherApplied(
                "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H", "CVSS:4.0/MAV:P/MAC:H/MAT:P/MPR:H/MUI:A/MVC:N/MVI:N/MVA:N/MSC:N/MSI:N/MSA:N",
                "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H/MAV:P/MAC:H/MAT:P/MPR:H/MUI:A/MVC:N/MVI:N/MVA:N/MSC:N/MSI:N/MSA:N",
                "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H"
        );
        // supplemental
        assertPartsLowerHigherApplied(
                "CVSS:4.0/S:N/AU:N/R:A/V:D/RE:L/U:Clear", "CVSS:4.0/S:P/AU:Y/R:I/V:C/RE:H/U:Red",
                "CVSS:4.0/S:N/AU:N/R:A/V:D/RE:L/U:Clear",
                "CVSS:4.0/S:P/AU:Y/R:I/V:C/RE:H/U:Red"
        );
        // environmental security requirement
        assertPartsLowerHigherApplied("CVSS:4.0/E:A/CR:H/IR:H/AR:H", "CVSS:4.0/E:U/CR:L/IR:L/AR:L",
                "CVSS:4.0/E:U/CR:L/IR:L/AR:L",
                "CVSS:4.0/E:A/CR:H/IR:H/AR:H"
        );
        assertPartsLowerHigherApplied("CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H", "CVSS:4.0/CR:L/IR:M/AR:H",
                "CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H/CR:L/IR:M/AR:H",
                "CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H"
        );
        assertPartsLowerHigherApplied("CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H/CR:L/IR:M/AR:H", "CVSS:4.0/CR:X/IR:X/AR:X",
                "CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H/CR:L/IR:M/AR:H",
                "CVSS:4.0/AV:P/AC:H/AT:N/PR:L/UI:A/VC:H/VI:H/VA:H/SC:H/SI:L/SA:H"
        );

        // equal parts
        assertPartsLowerHigherApplied("CVSS:4.0/AV:A", "CVSS:4.0/MAV:A",
                "CVSS:4.0/AV:A/MAV:A",
                "CVSS:4.0/AV:A/MAV:A"
        );
        assertPartsLowerHigherApplied("CVSS:4.0/SC:H", "CVSS:4.0/MSC:H",
                "CVSS:4.0/SC:H/MSC:H",
                "CVSS:4.0/SC:H/MSC:H"
        );

        // random
        assertPartsLowerHigherApplied("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:P/VC:N/VI:N/VA:L/SC:N/SI:L/SA:N", "CVSS:4.0/AV:P/AC:H/AT:P/PR:L/UI:N/VC:N/VI:L/VA:L/SC:L/SI:N/SA:L",
                "CVSS:4.0/AV:P/AC:H/AT:P/PR:L/UI:P/VC:N/VI:N/VA:L/SC:N/SI:N/SA:N",
                "CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L"
        );
        assertPartsLowerHigherApplied("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L", "CVSS:4.0/MAV:A/MAC:L/MAT:P/MPR:L/MUI:N/MVC:N/MVI:L/MVA:N/MSC:N/MSI:X/MSA:S",
                "CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L/MAT:P/MPR:L/MUI:N/MVC:N/MVI:L/MVA:N/MSC:N",
                "CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L/MAV:A/MAC:L/MAT:X/MPR:X/MUI:N/MVC:N/MVI:L/MVA:X/MSC:X/MSI:X/MSA:S"
        );
    }

    private void assertPartsLowerHigherApplied(String originalVector, String applyMetrics, String expectedLower, String expectedHigher) {
        final CvssVector lower = CvssVector.parseVector(originalVector);
        lower.applyVectorPartsIfMetricsLower(applyMetrics);
        Assert.assertEquals(
                "lower: " + originalVector + " + " + applyMetrics + " -> " + createLinkForHigherLowerMetrics(originalVector, applyMetrics, lower).generateOptimizedLink(),
                CvssVector.parseVector(expectedLower).toString(), lower.toString()
        );

        final CvssVector higher = CvssVector.parseVector(originalVector);
        higher.applyVectorPartsIfMetricsHigher(applyMetrics);
        Assert.assertEquals(
                "higher: " + originalVector + " + " + applyMetrics + " -> " + createLinkForHigherLowerMetrics(originalVector, applyMetrics, higher).generateOptimizedLink(),
                CvssVector.parseVector(expectedHigher).toString(), higher.toString()
        );
    }

    private static UniversalCvssCalculatorLinkGenerator createLinkForHigherLowerMetrics(String input, String mod, CvssVector result) {
        final UniversalCvssCalculatorLinkGenerator gen = new UniversalCvssCalculatorLinkGenerator();
        gen.addOpenSection("base").addOpenSection("temporal").addOpenSection("environmental");
        gen.setBaseUrl("http://localhost:63342/metaeffekt-cvss-web-calculator/site/index.html");
        gen.addVector(CvssVector.parseVector(input), "input", true);
        gen.addVector(CvssVector.parseVector(mod), "mod", true);
        gen.addVector(result, "result", true);
        return gen;
    }

    @Test
    public void vectorVersionTest() {
        Assert.assertEquals("CVSS:3.0", Cvss3P0.getVersionName());
        Assert.assertEquals("CVSS:3.0", new Cvss3P0().getName());
        Assert.assertEquals("CVSS:3.1", Cvss3P1.getVersionName());
        Assert.assertEquals("CVSS:3.1", new Cvss3P1().getName());
    }
}
