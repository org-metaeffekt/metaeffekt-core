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
package org.metaeffekt.core.security.cvss.processor;


import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.v3.Cvss3P0;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;

import java.util.Objects;

/**
 * Pre-calculated scores for all available scores calculated from a {@link CvssVector} instance.<br>
 * Either call the constructor of this class, passing a vector, or use the {@link CvssVector#getBakedScores()} method to
 * obtain a cached version.<br>
 * Make sure to regenerate the baked scores after updating the vector yourself. If a value was unable to be
 * calculated, the score will have the value NaN.
 * <p>
 * Use the normalization methods of this class to get a score that has its possible value range mapped to 0.0-10.0. This
 * is used for the CVSS:3.1 exploitability and impact scores, which only range to 3.9 and 6.0/6.1.
 */
public class BakedCvssVectorScores {

    // FIXME-KKL: consider replacing implementation
    private final static LruLinkedHashMap<String, BakedCvssVectorScores> cache = new LruLinkedHashMap<>(5000);

    private final Class<? extends CvssVector> cvssVersion;
    private final String vector;

    private final double base;
    private final double impact;
    private final double exploitability;
    private final double temporal;
    private final double threat;
    private final double environmental;
    private final double adjustedImpact;
    private final double overall;

    protected BakedCvssVectorScores(CvssVector vector) {
        this.cvssVersion = vector.getClass();
        this.vector = vector.toString();

        this.base = vector.getBaseScore();
        this.overall = vector.getOverallScore();

        if (vector instanceof MultiScoreCvssVector) {
            final MultiScoreCvssVector cast = ((MultiScoreCvssVector) vector);
            this.impact = cast.getImpactScore();
            this.exploitability = cast.getExploitabilityScore();
            this.threat = Double.NaN;
            this.temporal = cast.getTemporalScore();
            this.environmental = cast.getEnvironmentalScore();
            this.adjustedImpact = cast.getAdjustedImpactScore();

        } else if (this.cvssVersion == Cvss4P0.class) {
            this.impact = Double.NaN;
            this.exploitability = Double.NaN;
            this.threat = ((Cvss4P0) vector).getThreatScore();
            this.temporal = Double.NaN;
            this.environmental = ((Cvss4P0) vector).getEnvironmentalScore();
            this.adjustedImpact = Double.NaN;

        } else {
            this.impact = Double.NaN;
            this.exploitability = Double.NaN;
            this.threat = Double.NaN;
            this.temporal = Double.NaN;
            this.environmental = Double.NaN;
            this.adjustedImpact = Double.NaN;
        }
    }

    public static BakedCvssVectorScores fromNullableCvss(CvssVector cvss) {
        if (cvss == null) {
            return null;
        }
        final String cvssString = cvss.toString();
        if (cache.containsKey(cvssString)) {
            return cache.get(cvssString);
        }
        final BakedCvssVectorScores scores = new BakedCvssVectorScores(cvss);
        cache.put(cvssString, scores);
        return scores;
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

    public double getThreatScore() {
        return threat;
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

    public boolean isBaseScoreAvailable() {
        return !Double.isNaN(base);
    }

    public boolean isImpactScoreAvailable() {
        return !Double.isNaN(impact);
    }

    public boolean isExploitabilityScoreAvailable() {
        return !Double.isNaN(exploitability);
    }

    public boolean isTemporalScoreAvailable() {
        return !Double.isNaN(temporal);
    }

    public boolean isThreatScoreAvailable() {
        return !Double.isNaN(threat);
    }

    public boolean isEnvironmentalScoreAvailable() {
        return !Double.isNaN(environmental);
    }

    public boolean isAdjustedImpactScoreAvailable() {
        return !Double.isNaN(adjustedImpact);
    }

    public boolean isOverallScoreAvailable() {
        return !Double.isNaN(overall);
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
        if (cvssVersion == Cvss3P1.class || cvssVersion == Cvss3P0.class) {
            return 6.0;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedExploitabilityScoreMax() {
        if (cvssVersion == Cvss3P1.class || cvssVersion == Cvss3P0.class) {
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
        if (cvssVersion == Cvss3P1.class || cvssVersion == Cvss3P0.class) {
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
        return vector;
    }

    /**
     * Maps the range (0 - max) of the score to the range of the normalized score (0 - 10).<br>
     * If the score is NaN, the score is returned as is.
     *
     * @param score the score to normalize
     * @param max   the maximum value of the score
     * @return the normalized score
     */
    public double normalizeScore(double score, double max) {
        if (Double.isNaN(score)) {
            return score;
        }
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BakedCvssVectorScores that = (BakedCvssVectorScores) o;
        return Double.compare(base, that.base) == 0 && Double.compare(impact, that.impact) == 0 && Double.compare(exploitability, that.exploitability) == 0 && Double.compare(temporal, that.temporal) == 0 && Double.compare(environmental, that.environmental) == 0 && Double.compare(adjustedImpact, that.adjustedImpact) == 0 && Double.compare(overall, that.overall) == 0 && Objects.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vector, base, impact, exploitability, temporal, environmental, adjustedImpact, overall);
    }
}
