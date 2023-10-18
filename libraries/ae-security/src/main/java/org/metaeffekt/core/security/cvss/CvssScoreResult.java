/*
 * Copyright 2009-2022 the original author or authors.
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


import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3;
import org.metaeffekt.core.security.cvss.v4_0.Cvss4_0;

public class CvssScoreResult {
    private final CvssVector cvss;
    private final double base;
    private final double impact;
    private final double exploitability;
    private final double temporal;
    private final double environmental;
    private final double adjustedImpact;
    private final double overall;

    public CvssScoreResult(CvssVector cvss) {
        this.cvss = cvss;
        this.base = cvss.getBaseScore();
        this.overall = cvss.getOverallScore();
        if (cvss instanceof MultiScoreCvssVector) {
            final MultiScoreCvssVector cast = ((MultiScoreCvssVector) cvss);
            this.impact = cast.getImpactScore();
            this.exploitability = cast.getExploitabilityScore();
            this.temporal = cast.getTemporalScore();
            this.environmental = cast.getEnvironmentalScore();
            this.adjustedImpact = cast.getAdjustedImpactScore();
        } else {
            this.impact = -1.0;
            this.exploitability = -1.0;
            this.temporal = -1.0;
            this.environmental = -1.0;
            this.adjustedImpact = -1.0;
        }
    }

    public static CvssScoreResult fromNullableCvss(CvssVector cvss) {
        if (cvss == null) {
            return null;
        }
        return new CvssScoreResult(cvss);
    }

    public CvssVector getCvss() {
        return cvss;
    }

    public boolean isCvss2() {
        return cvss instanceof Cvss2;
    }

    public boolean isCvss3() {
        return cvss instanceof Cvss3;
    }

    public boolean isCvss4() {
        return cvss instanceof Cvss4_0;
    }

    public boolean isBaseDefined() {
        return cvss.isBaseDefined();
    }

    public boolean isTemporalDefined() {
        return cvss instanceof MultiScoreCvssVector && ((MultiScoreCvssVector) cvss).isTemporalDefined();
    }

    public boolean isEnvironmentalDefined() {
        return cvss instanceof MultiScoreCvssVector && ((MultiScoreCvssVector) cvss).isEnvironmentalDefined();
    }

    public double getBaseScore() {
        return base;
    }

    public double getImpactScore() {
        return impact;
    }

    public double getExploitabilityScore() {
        return exploitability;
    }

    public double getTemporalScore() {
        return temporal;
    }

    public double getEnvironmentalScore() {
        return environmental;
    }

    public double getAdjustedImpactScore() {
        return adjustedImpact;
    }

    public double getOverallScore() {
        return overall;
    }

    public boolean hasNormalizedBaseScore() {
        return this.getUnNormalizedBaseScoreMax() != 10.0;
    }

    public boolean hasNormalizedImpactScore() {
        return this.getUnNormalizedImpactScoreMax() != 10.0;
    }

    public boolean hasNormalizedExploitabilityScore() {
        return this.getUnNormalizedExploitabilityScoreMax() != 10.0;
    }

    public boolean hasNormalizedTemporalScore() {
        return this.getUnNormalizedTemporalScoreMax() != 10.0;
    }

    public boolean hasNormalizedEnvironmentalScore() {
        return this.getUnNormalizedEnvironmentalScoreMax() != 10.0;
    }

    public boolean hasNormalizedAdjustedImpactScore() {
        return this.getUnNormalizedAdjustedImpactScoreMax() != 10.0;
    }

    public boolean hasNormalizedOverallScore() {
        return this.getUnNormalizedOverallScoreMax() != 10.0;
    }

    public double getUnNormalizedBaseScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedImpactScoreMax() {
        if (cvss instanceof Cvss3) {
            return 6.0;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedExploitabilityScoreMax() {
        if (cvss instanceof Cvss3) {
            return 3.9;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedTemporalScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedEnvironmentalScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedAdjustedImpactScoreMax() {
        if (cvss instanceof Cvss3) {
            return 6.1;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedOverallScoreMax() {
        return 10.0;
    }

    public double getNormalizedBaseScore() {
        return normalizeScore(base, this.getUnNormalizedBaseScoreMax());
    }

    public double getNormalizedImpactScore() {
        return normalizeScore(impact, this.getUnNormalizedImpactScoreMax());
    }

    public double getNormalizedExploitabilityScore() {
        return normalizeScore(exploitability, this.getUnNormalizedExploitabilityScoreMax());
    }

    public double getNormalizedTemporalScore() {
        return normalizeScore(temporal, this.getUnNormalizedTemporalScoreMax());
    }

    public double getNormalizedEnvironmentalScore() {
        return normalizeScore(environmental, this.getUnNormalizedEnvironmentalScoreMax());
    }

    public double getNormalizedAdjustedImpactScore() {
        return normalizeScore(adjustedImpact, this.getUnNormalizedAdjustedImpactScoreMax());
    }

    public double getNormalizedOverallScore() {
        return normalizeScore(overall, this.getUnNormalizedOverallScoreMax());
    }

    @Override
    public String toString() {
        return cvss.toString();
    }

    /**
     * Maps the range (0 - max) of the score to the range of the normalized score (0 - 10).
     *
     * @param score the score to normalize
     * @param max   the maximum value of the score
     * @return the normalized score
     */
    public double normalizeScore(double score, double max) {
        if (max == 10.0) {
            return score;
        }
        return round(mapRange(score, 0, max, 0, 10), 1);
    }

    private double mapRange(double value, double min, double max, double newMin, double newMax) {
        return (value - min) / (max - min) * (newMax - newMin) + newMin;
    }

    private double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
}
