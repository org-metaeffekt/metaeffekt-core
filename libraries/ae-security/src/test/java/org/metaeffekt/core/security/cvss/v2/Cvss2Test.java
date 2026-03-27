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
package org.metaeffekt.core.security.cvss.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cvss2Test {
    private final static Logger LOG = LoggerFactory.getLogger(Cvss2Test.class);

    @Test
    public void cvss2Test() {
        // CVE-2021-29425
        calculateCvss2("AV:N/AC:L/Au:N/C:P/I:N/A:N",
                5.0, 2.9, 10.0, Double.NaN, Double.NaN, Double.NaN, 5.0);
        calculateCvss2("AV:N/AC:L/Au:N/C:P/I:N/A:N/E:U/RL:W/RC:UR/CDP:L/TD:M/CR:M/IR:H/AR:L",
                5.0, 2.9, 10.0, 3.8, 3.3, 2.9, 3.3);

        // CVE-2002-0392
        calculateCvss2("AV:N/AC:L/Au:N/C:P/I:P/A:P",
                7.5, 6.4, 10.0, Double.NaN, Double.NaN, Double.NaN, 7.5);
        calculateCvss2("AV:N/AC:L/Au:N/C:P/I:P/A:P/E:F/RL:OF/RC:C/CDP:H/TD:H/CR:M/IR:M/AR:H",
                7.5, 6.4, 10.0, 6.2, 8.3, 7.2, 8.3);

        // others
        calculateCvss2("AV:L/AC:M/Au:S/C:C/I:N/A:C/E:POC/RL:W/RC:UC/CDP:LM/TD:M/CR:L/IR:H/AR:ND",
                6.0, 9.2, 2.7, 4.6, 4.3, 8.0, 4.3);

        calculateCvss2("AV:N/AC:M/Au:N/C:N/I:N/A:P/E:ND/RL:W/RC:C/CDP:ND/TD:M/CR:M/IR:ND/AR:H",
                4.3, 2.9, 8.6, 4.1, 3.8, 4.3, 3.8);

        calculateCvss2("AV:N/AC:L/Au:N/C:N/I:N/A:P/E:ND/RL:W/RC:ND/CDP:LM/TD:ND/CR:M/IR:ND/AR:ND",
                5.0, 2.9, 10.0, 4.8, 6.3, 2.9, 6.3);

        calculateCvss2("AV:N/AC:L/Au:M/C:C/I:P/A:P",
                7.3, 8.5, 6.4, Double.NaN, Double.NaN, Double.NaN, 7.3);

        calculateCvss2("AV:A/AC:M/Au:N/C:P/I:N/A:P/CDP:MH/TD:L/CR:L/IR:L/AR:L",
                4.3, 4.9, 5.5, Double.NaN, 1.4, 2.7, 1.4);

        // these two vectors would previously have calculated an incorrect environmental score due to a rounding error
        // in the adjusted base score calculation
        calculateCvss2("AV:A/AC:H/Au:S/C:P/I:C/A:P/E:U/RL:TF/RC:UR/CDP:L/TD:M/CR:L/IR:M/AR:H",
                5.5, 8.5, 2.5, 4.0, 3.4, 8.6, 3.4);
        calculateCvss2("AV:N/AC:H/Au:M/C:P/I:N/A:N/E:U/RL:W/RC:UR/CDP:L/TD:M/CR:M/IR:L/AR:H",
                1.7, 2.9, 3.2, 1.3, 1.7, 2.9, 1.7);

        calculateCvss2("AV:N/AC:H/Au:M/C:P/I:P/A:C/E:H/RL:TF/RC:UR/CDP:LM/TD:L/CR:M/IR:H/AR:M",
                5.8, 8.5, 3.2, 5.0, 1.7, 8.9, 1.7);
    }

    @Test
    public void evaluateCvssVectorsVerifyIncompleteVectorIsExtendedTest() {
        calculateCvss2("AV:N/AC:M/Au:N/C:C/I:C/A:C/E:U/RL:OF/RC:UC/CDP:ND/TD:L/CR:H/IR:ND/AR:ND",
                9.3, 10.0, 8.6, 6.2, 1.6, 10.0, 1.6);
        calculateCvss2("AV:N/AC:M/Au:N/C:C/I:C/A:C/E:U/RL:OF/RC:UC/TD:L/CR:H/AR:ND",
                9.3, 10.0, 8.6, 6.2, 1.6, 10.0, 1.6);
        calculateCvss2("AV:N/AC:M/Au:N/C:C/I:C/A:C/E:U/RL:OF/RC:UC/TD:L/CR:H",
                9.3, 10.0, 8.6, 6.2, 1.6, 10.0, 1.6);

        calculateCvss2("AV:N/AC:M/Au:N/C:C/I:C/A:C/E:ND/RL:ND/RC:UC",
                9.3, 10.0, 8.6, 8.4, Double.NaN, Double.NaN, 8.4);
        calculateCvss2("AV:N/AC:M/Au:N/C:C/I:C/A:C/RC:UC",
                9.3, 10.0, 8.6, 8.4, Double.NaN, Double.NaN, 8.4);
    }

    @Test
    public void negativeEnvironmentalScoreTest() {
        // happens because CDP is None and TD is High/Medium
        calculateCvss2("AV:L/AC:H/Au:M/C:P/I:N/A:N/E:H/RL:OF/RC:UC/CDP:N/TD:H/CR:L/IR:L/AR:L",
                0.8, 2.9, 1.2, 0.6, -0.1, 1.4, -0.1);
        calculateCvss2("AV:L/AC:H/Au:M/C:P/I:N/A:N/E:H/RL:OF/RC:UC/CDP:N/TD:M/CR:L/IR:L/AR:L",
                0.8, 2.9, 1.2, 0.6, -0.1, 1.4, -0.1);
    }

    @Test
    public void modifyVectorTest_NoBaseScores() {
        Cvss2 vector = new Cvss2();
        vector.applyVector("CDP:LM");
        checkScores(vector, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private void calculateCvss2(String vector, double base, double impact, double exploitability, double temporal, double environmental, double adjImpact, double overall) {
        Cvss2 cvss2 = new Cvss2(vector);
        checkScores(cvss2, base, impact, exploitability, temporal, environmental, adjImpact, overall);
    }

    private void checkScores(Cvss2 vector, double base, double impact, double exploitability, double temporal, double environmental, double adjImpact, double overall) {
        // calculate values
        LOG.info("                 Input: [{}]", vector);
        LOG.info("                Vector: [{}]", vector);
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

        Assertions.assertEquals(base, vector.getBaseScore(), 0.01);
        Assertions.assertEquals(impact, vector.getImpactScore(), 0.01);
        Assertions.assertEquals(exploitability, vector.getExploitabilityScore(), 0.01);
        Assertions.assertEquals(temporal, vector.getTemporalScore(), 0.01);
        Assertions.assertEquals(environmental, vector.getEnvironmentalScore(), 0.01);
        Assertions.assertEquals(adjImpact, vector.getAdjustedImpactScore(), 0.01);
        Assertions.assertEquals(overall, vector.getOverallScore(), 0.01);
    }
}
