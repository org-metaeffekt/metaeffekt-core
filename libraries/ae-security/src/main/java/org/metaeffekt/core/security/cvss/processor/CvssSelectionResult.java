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
package org.metaeffekt.core.security.cvss.processor;

import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CvssSelectionResult {

    private final static Logger LOG = LoggerFactory.getLogger(CvssSelectionResult.class);

    private final CvssVectorSet allVectors;

    private final Cvss2 baseCvss2;
    private final Cvss2 effectiveCvss2;

    private final Cvss3P1 baseCvss3;
    private final Cvss3P1 effectiveCvss3;

    private final Cvss4P0 baseCvss4;
    private final Cvss4P0 effectiveCvss4;

    private final CvssVector selectedBaseCvss;
    private final CvssVector selectedEffectiveCvss;

    public CvssSelectionResult(CvssVectorSet allVectors,
                               CvssSelector baseSelector, CvssSelector effectiveSelector,
                               List<CvssScoreVersionSelectionPolicy> versionSelectionPolicy
    ) {
        this.allVectors = allVectors;

        this.baseCvss2 = baseSelector.selectVector(allVectors, Cvss2.class);
        this.effectiveCvss2 = effectiveSelector.selectVector(allVectors, Cvss2.class);

        this.baseCvss3 = baseSelector.selectVector(allVectors, Cvss3P1.class);
        this.effectiveCvss3 = effectiveSelector.selectVector(allVectors, Cvss3P1.class);

        this.baseCvss4 = baseSelector.selectVector(allVectors, Cvss4P0.class);
        this.effectiveCvss4 = effectiveSelector.selectVector(allVectors, Cvss4P0.class);

        this.selectedBaseCvss = this.selectVersionedCvss(versionSelectionPolicy, this.baseCvss2, this.baseCvss3, this.baseCvss4);
        this.selectedEffectiveCvss = this.selectVersionedCvss(versionSelectionPolicy, this.effectiveCvss2, this.effectiveCvss3, this.effectiveCvss4);
    }

    public boolean hasBaseCvss() {
        return this.baseCvss2 != null || this.baseCvss3 != null || this.baseCvss4 != null;
    }

    public boolean hasEffectiveCvss() {
        return this.effectiveCvss2 != null || this.effectiveCvss3 != null || this.effectiveCvss4 != null;
    }

    public boolean hasAnyCvss() {
        return this.hasBaseCvss() || this.hasEffectiveCvss();
    }

    public CvssVector getSelectedEffectiveIfAvailableOtherwiseBase() {
        if (this.selectedEffectiveCvss != null) return this.selectedEffectiveCvss;
        else return this.selectedBaseCvss;
    }

    private CvssVector selectVersionedCvss(List<CvssScoreVersionSelectionPolicy> versionSelectionPolicy, CvssVector... vectorScores) {
        final CvssVector foundV2 = this.findOfVersion(Cvss2.class, vectorScores);
        final CvssVector foundV3 = this.findOfVersion(Cvss3P1.class, vectorScores);
        final CvssVector foundV4 = this.findOfVersion(Cvss4P0.class, vectorScores);

        if (foundV2 == null && foundV3 == null && foundV4 == null) return null;

        if (versionSelectionPolicy.isEmpty()) {
            // assume LATEST
            if (foundV4 != null) return foundV4;
            else if (foundV3 != null) return foundV3;
            else /*if (foundV2 != null)*/ return foundV2;
        }

        for (CvssScoreVersionSelectionPolicy cvssSelectionPolicy : versionSelectionPolicy) {
            switch (cvssSelectionPolicy) {
                case HIGHEST: {
                    int highestScore = -1;
                    CvssVector highestScoreVector = null;
                    for (CvssVector vectorScore : vectorScores) {
                        if (vectorScore != null && vectorScore.getOverallScore() > highestScore) {
                            highestScore = (int) vectorScore.getOverallScore();
                            highestScoreVector = vectorScore;
                        }
                    }
                    if (highestScoreVector != null) return highestScoreVector;
                }
                break;
                case LOWEST:
                    int lowestScore = Integer.MAX_VALUE;
                    CvssVector lowestScoreVector = null;
                    for (CvssVector vectorScore : vectorScores) {
                        if (vectorScore != null && vectorScore.getOverallScore() < lowestScore) {
                            lowestScore = (int) vectorScore.getOverallScore();
                            lowestScoreVector = vectorScore;
                        }
                    }
                    if (lowestScoreVector != null) return lowestScoreVector;
                    break;
                case LATEST:
                    if (foundV4 != null) return foundV4;
                    else if (foundV3 != null) return foundV3;
                    else /*if (foundV2 != null)*/ return foundV2;
                case OLDEST:
                    if (foundV2 != null) return foundV2;
                    else if (foundV3 != null) return foundV3;
                    else /*if (foundV2 != null)*/ return foundV4;
                case V2:
                    if (foundV2 != null) return foundV2;
                    break;
                case V3:
                    if (foundV3 != null) return foundV3;
                    break;
                case V4:
                    if (foundV4 != null) return foundV4;
                    break;
            }
        }

        LOG.warn("No matching CVSS version found for selection policy, consider adding a non-fixed version selector (like [{}] or [{}]) to the end of your versionSelectionPolicy: {}",
                CvssScoreVersionSelectionPolicy.LATEST, CvssScoreVersionSelectionPolicy.HIGHEST, versionSelectionPolicy);

        return null;
    }

    private <T extends CvssVector> T findOfVersion(Class<T> cvssVersion, CvssVector... vectors) {
        for (CvssVector vector : vectors) {
            if (vector != null && vector.getClass().equals(cvssVersion)) {
                return (T) vector;
            }
        }
        return null;
    }

    public CvssVectorSet getAllVectors() {
        return allVectors;
    }

    public Cvss2 getBaseCvss2() {
        return baseCvss2;
    }

    public Cvss2 getEffectiveCvss2() {
        return effectiveCvss2;
    }

    public Cvss3P1 getBaseCvss3() {
        return baseCvss3;
    }

    public Cvss3P1 getEffectiveCvss3() {
        return effectiveCvss3;
    }

    public Cvss4P0 getBaseCvss4() {
        return baseCvss4;
    }

    public Cvss4P0 getEffectiveCvss4() {
        return effectiveCvss4;
    }

    public CvssVector getSelectedBaseCvss() {
        return selectedBaseCvss;
    }

    public CvssVector getSelectedEffectiveCvss() {
        return selectedEffectiveCvss;
    }

    public enum CvssScoreVersionSelectionPolicy {
        HIGHEST,
        LOWEST,
        LATEST,
        OLDEST,
        V2,
        V3,
        V4,
    }
}
