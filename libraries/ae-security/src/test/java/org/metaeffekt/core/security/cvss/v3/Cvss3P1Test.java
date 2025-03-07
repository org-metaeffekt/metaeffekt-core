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
package org.metaeffekt.core.security.cvss.v3;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cvss3P1Test {
    private final static Logger LOG = LoggerFactory.getLogger(Cvss3P0Test.class);

    @Test
    public void evaluateCvssVectorsTest() {
        calculateCvss3("AV:L/AC:H/PR:L/UI:R/S:C/C:L/I:H/A:N/E:P/RL:T/RC:C/CR:H/IR:L/AR:M/MAV:A/MAC:H/MPR:N/MUI:R/MS:U/MC:L/MI:N/MA:H",
                6.1, 4.7, 0.8, 5.6, 5.3, 4.5, 5.3);

        calculateCvss3("AV:A/AC:H/PR:N/UI:N/S:U/C:L/I:H/A:N/E:P/RL:T/RC:C/CR:M/IR:L/AR:H/MAV:A/MAC:X/MPR:L/MUI:X/MC:L/MI:N/MA:L",
                5.9, 4.2, 1.6, 5.4, 3.9, 3.1, 3.9);
        calculateCvss3("AV:L/AC:H/PR:L/UI:N/S:U/C:L/I:H/A:N/E:U/RL:W/RC:R/CR:X/IR:X/AR:X/MAV:A/MAC:X/MPR:X/MUI:X/MS:X/MC:X/MI:X/MA:X",
                5.3, 4.2, 1.0, 4.5, 4.6, 4.2, 4.6);
        calculateCvss3("AV:L/AC:H/PR:L/UI:N/S:U/C:L/I:H/A:N/E:U/RL:W/RC:R/CR:X/IR:X/AR:X/MAV:A/MAC:H/MPR:L/MUI:N/MS:U/MC:L/MI:H/MA:N",
                5.3, 4.2, 1.0, 4.5, 4.6, 4.2, 4.6);

        calculateCvss3("AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N/E:U/RL:W/RC:R/CR:H/IR:L/AR:H/MAV:A/MAC:H/MPR:L/MUI:N/MS:C/MC:N/MI:L/MA:N",
                5.3, 1.4, 3.9, 4.5, 1.8, 0.6, 1.8);

        calculateCvss3("AV:N/AC:L/PR:H/UI:R/S:C/C:N/I:N/A:H/E:P/RL:O/RC:U/CR:X/IR:M/AR:H/MAV:P/MAC:L/MPR:X/MUI:N/MS:X/MC:X/MI:N/MA:L",
                6.2, 4.0, 1.7, 5.1, 2.6, 2.3, 2.6);
        calculateCvss3("AV:N/AC:L/PR:H/UI:R/S:U/C:N/I:N/A:H/E:P/RL:O/RC:U/CR:X/IR:M/AR:H/MAV:P/MAC:L/MPR:X/MUI:N/MS:X/MC:X/MI:N/MA:L",
                4.5, 3.6, 0.9, 3.7, 2.1, 2.1, 2.1);

        calculateCvss3("AV:N/AC:L/PR:H/UI:R/S:U/C:N/I:N/A:H",
                4.5, 3.6, 0.9, Double.NaN, Double.NaN, Double.NaN, 4.5);

        calculateCvss3("AV:N/AC:L/PR:H/UI:R/S:U/C:N/I:N/A:H/E:P/RL:X/RC:X",
                4.5, 3.6, 0.9, 4.3, Double.NaN, Double.NaN, 4.3);

        calculateCvss3("AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H/E:U/RL:W/RC:R/CR:X/IR:L/AR:M/MAV:N/MAC:H/MPR:N/MUI:R/MS:C/MC:X/MI:X/MA:N",
                5.9, 3.6, 2.2, 5.0, 0, 0, 0);

        calculateCvss3("AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:H/A:H/E:P/RL:O/RC:C",
                7.1, 5.2, 1.8, 6.4, Double.NaN, Double.NaN, 6.4);

        calculateCvss3("CVSS:3.1/AV:N/AC:H/PR:H/UI:R/S:U/C:H/I:H/A:H/E:P/RL:O",
                6.4, 5.9, 0.5, 5.8, Double.NaN, Double.NaN, 5.8);

        calculateCvss3("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H/E:U/RL:W/RC:R/MAV:N/MAC:H/MPR:N/MUI:R/MS:C/MC:L/MA:N/IR:L/AR:M",
                5.9, 3.6, 2.2, 5.0, 2.9, 1.4, 2.9);

        calculateCvss3("CVSS:3.1/AV:A/AC:L/PR:H/UI:R/S:U/C:N/I:L/A:H/E:P/RL:X/RC:X/CR:L/IR:X/AR:X/MAV:A/MAC:X/MPR:X/MUI:N/MS:X/MC:X/MI:L/MA:X",
                4.9, 4.2, 0.7, 4.7, 4.9, 4.2, 4.9);

        calculateCvss3("AV:N/AC:H/PR:H/UI:N/S:C/C:H/I:H/A:H/E:H/RL:U/RC:C/CR:H/IR:H/AR:H/MAV:N/MAC:H/MPR:H/MUI:N/MS:C/MC:H/MI:H/MA:H",
                8.0, 6.0, 1.3, 8.0, 8.1, 6.1, 8.1);

        calculateCvss3("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
                6.8, 5.9, 0.9, Double.NaN, Double.NaN, Double.NaN, 6.8);

        calculateCvss3("AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H/E:H/RL:W/RC:C/CR:H/IR:L/AR:H/MAV:N/MAC:H/MPR:L/MUI:N/MC:H/MI:L/MA:H/MS:X",
                10.0, 6.0, 3.9, 9.7, 8.4, 6.1, 8.4);
        calculateCvss3("AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H/E:H/RL:W/RC:C/MAV:N/MAC:H/MPR:L/MUI:N/MC:H/MI:L/MA:H/CR:H/IR:L/AR:H",
                10.0, 6.0, 3.9, 9.7, 8.4, 6.1, 8.4);

        // not all base metrics defined
        calculateCvss3("CVSS:3.1/E:U/RL:T/MAV:P/MAC:H/MPR:H/MUI:R/MS:U/AR:M",
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        calculateCvss3("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H",
                5.5, 3.6, 1.8, Double.NaN, Double.NaN, Double.NaN, 5.5);

        calculateCvss3("CVSS:3.1/AV:A/AC:L/PR:N/UI:N/S:C/C:N/I:L/A:L/E:H/RC:R/MAV:N/MPR:L/MC:N/MI:H/MA:H/CR:L/IR:H/AR:H",
                6.1, 2.7, 2.8, 5.9, 9.6, 6.1, 9.6);
        calculateCvss3("CVSS:3.1/AV:L/AC:L/PR:H/UI:N/S:C/C:L/I:H/A:L/E:H/MAC:H/MPR:N/MUI:N/MC:N/MI:N/MA:N/IR:M",
                7.3, 5.3, 1.5, 7.3, 0.0, 0.0, 0.0);

        // negative impact score
        checkCvssScores(new Cvss3P1("CVSS:3.1/AV:P/AC:H/PR:H/UI:N/S:C/C:N/I:N/A:N"),
                0.0, 0.0, 0.3, Double.NaN, Double.NaN, Double.NaN, 0.0);
    }

    @Test
    public void evaluateCvssVectorsVerifyIncompleteVectorIsExtendedTest() {
        calculateCvss3("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H/CR:X/IR:X/AR:X/MAV:L/MAC:X/MPR:H/MUI:X/MS:X/MC:X/MI:X/MA:X",
                5.5, 3.6, 1.8, Double.NaN, 4.2, 3.6, 4.2);

        calculateCvss3("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H/MAV:L/MPR:H" + "/CR:X/IR:X/AR:X/MAC:X/MUI:X/MS:X/MC:X/MI:X/MA:X",
                5.5, 3.6, 1.8, Double.NaN, 4.2, 3.6, 4.2);

        calculateCvss3("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H/MAV:L/MPR:H",
                5.5, 3.6, 1.8, Double.NaN, 4.2, 3.6, 4.2);

        {
            Cvss3P1 vector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H");
            checkCvssScores(vector,
                    5.5, 3.6, 1.8, Double.NaN, Double.NaN, Double.NaN, 5.5);

            vector.applyVector("MAV:L/MPR:H");
            checkCvssScores(vector,
                    5.5, 3.6, 1.8, Double.NaN, 4.2, 3.6, 4.2);

            vector.applyVector("CR:X/IR:X/AR:X/MAC:X/MUI:X/MS:X/MC:X/MI:X/MA:X");
            checkCvssScores(vector,
                    5.5, 3.6, 1.8, Double.NaN, 4.2, 3.6, 4.2);

            Assert.assertEquals("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H/MAV:L/MPR:H", vector.toString());

            vector.applyVector("CR:X/IR:X/AR:X/MAC:X/MUI:X/MS:X/MC:X/MI:X/MA:X/MAV:X/MPR:X");
            checkCvssScores(vector,
                    5.5, 3.6, 1.8, Double.NaN, Double.NaN, Double.NaN, 5.5);

            Assert.assertEquals("CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H", vector.toString());
        }

        {
            Cvss3P1 vector = new Cvss3P1("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
            checkCvssScores(vector,
                    9.8, 5.9, 3.9, Double.NaN, Double.NaN, Double.NaN, 9.8);

            vector.applyVector("E:P/RL:T/RC:R/CR:X/IR:M/AR:X/MAV:A/MAC:H/MPR:L/MUI:N/MS:C/MC:L/MI:N/MA:X");
            checkCvssScores(vector,
                    9.8, 5.9, 3.9, 8.5, 5.7, 4.7, 5.7);

            Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/E:P/RL:T/RC:R/MAV:A/MAC:H/MPR:L/MUI:N/MS:C/MC:L/MI:N/MA:X/CR:X/IR:M/AR:X", vector.toString(false));
            Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/E:P/RL:T/RC:R/MAV:A/MAC:H/MPR:L/MUI:N/MS:C/MC:L/MI:N/IR:M", vector.toString());
        }
    }

    @Test
    @Ignore
    public void customTest() {
        // checkCvssScores(new Cvss3P1("CVSS:3.1/AV:P/AC:H/PR:H/UI:N/S:C/C:N/I:N/A:N"),
        //         6.1, 2.7, 3.4, Double.NaN, Double.NaN, Double.NaN, 6.1);
    }

    @Test
    public void modifyVectorTest() {
        // undefined RC, IR and AR
        applyVectorsAndCalculateCvss3("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:L",
                3.3, 1.4, 1.8, 2.9, 1.4, 1.4, 1.4,
                "E:U/RL:T/AR:M/MAV:P/MAC:H/MPR:H/MUI:R/MS:U");
        applyVectorsAndCalculateCvss3("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:L",
                3.3, 1.4, 1.8, 2.9, 1.4, 1.4, 1.4,
                "E:U/RL:T/RC:X/CR:X/IR:X/AR:M/MAV:P/MAC:H/MPR:H/MUI:R/MS:U");
    }

    @Test
    public void modifyVectorTest_NoBaseScores() {
        Cvss3P1 vector = new Cvss3P1();
        vector.applyVector("MAV:L/MPR:H");
        checkCvssScores(vector,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    @Test
    public void changeVectorPartsOnlyIfTest() {
        {
            Cvss3P1 vector = new Cvss3P1("AV:N/AC:H/PR:L/UI:R/S:C/C:L/I:L/A:N");
            Assert.assertEquals(4.4, vector.getBaseScore(), 0.0);

            vector.applyVectorPartsIfLower(new Cvss3P1("AV:N/AC:L/PR:H/UI:R/S:U/C:H/I:H/A:H"), MultiScoreCvssVector::getBaseScore);

            Assert.assertEquals("CVSS:3.1/AV:N/AC:H/PR:H/UI:R/S:U/C:L/I:L/A:N", vector.toString());
            Assert.assertEquals(3.1, vector.getBaseScore(), 0.0);

            vector.applyVectorPartsIfLower(new Cvss3P1("AV:N/AC:L/PR:H/UI:R/S:U/C:H/I:H/A:H"), MultiScoreCvssVector::getBaseScore);
            Assert.assertEquals("CVSS:3.1/AV:N/AC:H/PR:H/UI:R/S:U/C:L/I:L/A:N", vector.toString());
        }

        {
            Cvss3P1 vector = new Cvss3P1("AV:N/AC:H/PR:L/UI:R/S:C/C:L/I:L/A:N");
            Assert.assertEquals(4.4, vector.getBaseScore(), 0.0);

            vector.applyVectorPartsIfHigher(new Cvss3P1("AV:N/AC:L/PR:H/UI:R/S:U/C:H/I:H/A:H"), MultiScoreCvssVector::getBaseScore);

            Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:C/C:H/I:H/A:H", vector.toString());
            Assert.assertEquals(9.0, vector.getBaseScore(), 0.0);

            vector.applyVectorPartsIfHigher(new Cvss3P1("AV:N/AC:L/PR:H/UI:R/S:U/C:H/I:H/A:H"), MultiScoreCvssVector::getBaseScore);
            Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:C/C:H/I:H/A:H", vector.toString());
        }
    }

    private void applyVectorsAndCalculateCvss3(String vector, double base, double impact, double exploitability, double temporal, double environmental, double adjImpact, double overall, String... apply) {
        Cvss3P1 cvss = new Cvss3P1(vector);

        for (String v : apply) {
            cvss.applyVector(v);
        }

        checkCvssScores(cvss, base, impact, exploitability, temporal, environmental, adjImpact, overall);
    }

    private void calculateCvss3(String vector, double base, double impact, double exploitability, double temporal, double environmental, double adjImpact, double overall) {
        Cvss3P1 cvss3 = new Cvss3P1(vector);
        checkCvssScores(cvss3, base, impact, exploitability, temporal, environmental, adjImpact, overall);
    }

    private void checkCvssScores(Cvss3P1 vector, double base, double impact, double exploitability, double temporal, double environmental, double adjImpact, double overall) {

        // calculate values
        LOG.info("                Vector: [{}]", vector.toString(false));
        LOG.info("            Base score: [{}]", vector.getBaseScore());
        LOG.info("          Impact score: [{}]", vector.getImpactScore());
        LOG.info("  Exploitability score: [{}]", vector.getExploitabilityScore());
        LOG.info("        Temporal score: [{}]", vector.getTemporalScore());
        LOG.info("   Environmental score: [{}]", vector.getEnvironmentalScore());
        LOG.info(" Adjusted impact score: [{}]", vector.getAdjustedImpactScore());
        LOG.info("         Overall score: [{}]", vector.getOverallScore());
        LOG.info("            Has scores: [{} {} {}]", vector.isBaseFullyDefined(), vector.isAnyTemporalDefined(), vector.isAnyEnvironmentalDefined());
        LOG.info("                  Link: {}", vector.getWebEditorLink());
        LOG.info("\n");

        Assert.assertEquals(base, vector.getBaseScore(), 0.01);
        Assert.assertEquals(impact, vector.getImpactScore(), 0.01);
        Assert.assertEquals(exploitability, vector.getExploitabilityScore(), 0.01);
        Assert.assertEquals(temporal, vector.getTemporalScore(), 0.01);
        Assert.assertEquals(environmental, vector.getEnvironmentalScore(), 0.01);
        Assert.assertEquals(adjImpact, vector.getAdjustedImpactScore(), 0.01);
        Assert.assertEquals(overall, vector.getOverallScore(), 0.01);
    }

    @Test
    public void toStringAllPropertiesDefinedTest() {
        final String input = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H/E:U/RL:W/RC:R/MAV:A/MAC:H/MPR:L/MUI:N/MS:C/MC:N/MI:L/MA:N/CR:H/IR:L/AR:H";
        final Cvss3P1 cvss = new Cvss3P1(input);
        Assert.assertEquals(input, cvss.toString());
        Assert.assertEquals(input, cvss.toString(true));
        Assert.assertEquals(input, cvss.toString(false));
    }

    @Test
    public void toStringSomePropertiesUndefinedTest() {
        final String input = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H/E:U/RL:W/RC:R/MAV:A/MAC:H/MS:C/MC:N/MI:L/MA:N/CR:H";
        final Cvss3P1 cvss = new Cvss3P1(input);
        Assert.assertEquals(input, cvss.toString());
        Assert.assertEquals(input, cvss.toString(true));
        Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H/E:U/RL:W/RC:R/MAV:A/MAC:H/MPR:X/MUI:X/MS:C/MC:N/MI:L/MA:N/CR:H/IR:X/AR:X", cvss.toString(false));
    }
}
