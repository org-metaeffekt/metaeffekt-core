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
package org.metaeffekt.core.security.cvss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

public class BakedCvssVectorScoresTest {

    @Test
    public void correctNormalizedCvss3Test() {
        final BakedCvssVectorScores result = new Cvss3P1("AV:L/AC:H/PR:L/UI:R/S:C/C:L/I:H/A:N/E:P/RL:T/RC:C/CR:H/IR:L/AR:M/MAV:A/MAC:H/MPR:N/MUI:R/MS:U/MC:L/MI:N/MA:H").bakeScores();

        Assertions.assertEquals(6.1, result.getBaseScore(), 0.01);
        Assertions.assertEquals(4.7, result.getImpactScore(), 0.01);
        Assertions.assertEquals(0.8, result.getExploitabilityScore(), 0.01);
        Assertions.assertEquals(5.6, result.getTemporalScore(), 0.01);
        Assertions.assertEquals(5.3, result.getEnvironmentalScore(), 0.01);
        Assertions.assertEquals(4.5, result.getAdjustedImpactScore(), 0.01);
        Assertions.assertEquals(5.3, result.getOverallScore(), 0.01);

        Assertions.assertFalse(result.hasNormalizedBaseScore());
        Assertions.assertTrue(result.hasNormalizedImpactScore());
        Assertions.assertTrue(result.hasNormalizedExploitabilityScore());
        Assertions.assertFalse(result.hasNormalizedTemporalScore());
        Assertions.assertFalse(result.hasNormalizedEnvironmentalScore());
        Assertions.assertTrue(result.hasNormalizedAdjustedImpactScore());

        Assertions.assertEquals(6.1, result.getNormalizedBaseScore(), 0.1);
        Assertions.assertEquals(7.8, result.getNormalizedImpactScore(), 0.1);
        Assertions.assertEquals(2.1, result.getNormalizedExploitabilityScore(), 0.1);
        Assertions.assertEquals(5.6, result.getNormalizedTemporalScore(), 0.1);
        Assertions.assertEquals(5.3, result.getNormalizedEnvironmentalScore(), 0.1);
        Assertions.assertEquals(7.4, result.getNormalizedAdjustedImpactScore(), 0.1);
    }

    @Test
    public void correctNormalizedCvss2Test() {
        final BakedCvssVectorScores result = new Cvss2("AV:N/AC:L/Au:N/C:P/I:N/A:N/E:U/RL:W/RC:UR/CDP:L/TD:M/CR:M/IR:H/AR:L").bakeScores();

        Assertions.assertEquals(5.0, result.getBaseScore(), 0.01);
        Assertions.assertEquals(2.9, result.getImpactScore(), 0.01);
        Assertions.assertEquals(10.0, result.getExploitabilityScore(), 0.01);
        Assertions.assertEquals(3.8, result.getTemporalScore(), 0.01);
        Assertions.assertEquals(3.3, result.getEnvironmentalScore(), 0.01);
        Assertions.assertEquals(2.9, result.getAdjustedImpactScore(), 0.01);
        Assertions.assertEquals(3.3, result.getOverallScore(), 0.01);

        Assertions.assertFalse(result.hasNormalizedBaseScore());
        Assertions.assertFalse(result.hasNormalizedImpactScore());
        Assertions.assertFalse(result.hasNormalizedExploitabilityScore());
        Assertions.assertFalse(result.hasNormalizedTemporalScore());
        Assertions.assertFalse(result.hasNormalizedEnvironmentalScore());
        Assertions.assertFalse(result.hasNormalizedAdjustedImpactScore());

        Assertions.assertEquals(5.0, result.getNormalizedBaseScore(), 0.01);
        Assertions.assertEquals(2.9, result.getNormalizedImpactScore(), 0.01);
        Assertions.assertEquals(10.0, result.getNormalizedExploitabilityScore(), 0.01);
        Assertions.assertEquals(3.8, result.getNormalizedTemporalScore(), 0.01);
        Assertions.assertEquals(3.3, result.getNormalizedEnvironmentalScore(), 0.01);
        Assertions.assertEquals(2.9, result.getNormalizedAdjustedImpactScore(), 0.01);
    }
}