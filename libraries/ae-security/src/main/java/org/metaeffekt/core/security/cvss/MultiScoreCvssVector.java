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

import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

public abstract class MultiScoreCvssVector<T extends CvssVector<T>> extends CvssVector<T> {

    private final static Logger LOG = LoggerFactory.getLogger(MultiScoreCvssVector.class);

    public MultiScoreCvssVector() {
        super();
    }

    public MultiScoreCvssVector(CvssSource<T> source) {
        super(source);
    }

    public MultiScoreCvssVector(CvssSource<T> source, JSONObject applicabilityCondition) {
        super(source, applicabilityCondition);
    }

    public MultiScoreCvssVector(Collection<CvssSource<T>> sources, JSONObject applicabilityCondition) {
        super(sources, applicabilityCondition);
    }

    public abstract double getImpactScore();

    public abstract double getExploitabilityScore();

    public abstract double getTemporalScore();

    public abstract double getEnvironmentalScore();

    public abstract double getAdjustedImpactScore();

    public abstract CvssSeverityRanges.SeverityRange getDefaultSeverityCategory();

    public CvssSeverityRanges.SeverityRange getSeverityCategory(CvssSeverityRanges ranges) {
        return ranges.getRange(getOverallScore());
    }

    public abstract boolean isAnyTemporalDefined();

    public abstract boolean isTemporalFullyDefined();

    public abstract boolean isAnyEnvironmentalDefined();

    public abstract boolean isEnvironmentalFullyDefined();

    public abstract void clearTemporal();

    public abstract void clearEnvironmental();

    public static <T extends MultiScoreCvssVector<T>> double getMaxScore(Function<MultiScoreCvssVector<T>, Double> scoreType, MultiScoreCvssVector<T>... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .map(scoreType)
                .max(Double::compare)
                .orElse(-1.0);
    }

    public static <T extends MultiScoreCvssVector<T>> T getMaxVector(Function<T, Double> scoreType, T... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(scoreType::apply))
                .orElse(null);
    }

    public static <T extends MultiScoreCvssVector<T>> double getMaxScore(Function<BakedCvssVectorScores<T>, Double> scoreType, BakedCvssVectorScores<T>... cvsses) {
        return Arrays.stream(cvsses)
                .filter(Objects::nonNull)
                .map(scoreType)
                .max(Double::compare)
                .orElse(-1.0);
    }
}
