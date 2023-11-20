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

import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

public abstract class MultiScoreCvssVector extends CvssVector {

    private final static Logger LOG = LoggerFactory.getLogger(MultiScoreCvssVector.class);

    public abstract double getImpactScore();

    public abstract double getExploitabilityScore();

    public abstract double getTemporalScore();

    public abstract double getEnvironmentalScore();

    public abstract double getAdjustedImpactScore();

    public abstract CvssSeverityRanges.SeverityRange getDefaultSeverityCategory();

    public CvssSeverityRanges.SeverityRange getSeverityCategory(CvssSeverityRanges ranges) {
        return ranges.getRange(getOverallScore());
    }

    public abstract boolean isTemporalDefined();

    public abstract boolean isEnvironmentalDefined();

    public abstract void clearTemporal();

    public abstract void clearEnvironmental();

    public static double getMaxScore(Function<MultiScoreCvssVector, Double> scoreType, MultiScoreCvssVector... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .map(scoreType)
                .max(Double::compare)
                .orElse(-1.0);
    }

    public static <T extends MultiScoreCvssVector> T getMaxVector(Function<T, Double> scoreType, T... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(scoreType::apply))
                .orElse(null);
    }

    public static double getMaxScore(Function<BakedCvssVectorScores, Double> scoreType, BakedCvssVectorScores... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .map(scoreType)
                .max(Double::compare)
                .orElse(-1.0);
    }
}
