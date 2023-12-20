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

import java.util.Collection;
import java.util.List;

/**
 * Contains the <strong>initial</strong> (aka provided, unmodified) and <strong>context</strong> (aka effective,
 * modified) matched results for all supported CVSS versions using the {@link CvssSelector#selectVector(Collection)}
 * method of the provided <code>initialSelector</code> and <code>contextSelector</code>.<br>
 * Finally, it will use the <code>versionSelectionPolicy</code> to select one of the versioned vectors per initial and
 * context to determine the selected (to be used) CVSS vectors.
 * <p>
 * The versioned vectors will be displayed in radar charts, tables and more, while the selected vectors will be used as
 * basis for the related scoring operations, such as vulnerability severity calculation and other overview charts.
 */
public class CvssSelectionResult {

    private final static Logger LOG = LoggerFactory.getLogger(CvssSelectionResult.class);

    private final CvssVectorSet allVectors;

    private final Cvss2 initialCvss2;
    private final Cvss2 contextCvss2;

    private final Cvss3P1 initialCvss3;
    private final Cvss3P1 contextCvss3;

    private final Cvss4P0 initialCvss4;
    private final Cvss4P0 contextCvss4;

    private final CvssVector selectedInitialCvss;
    private final CvssVector selectedContextCvss;

    public CvssSelectionResult(CvssVectorSet allVectors,
                               CvssSelector initialSelector, CvssSelector contextSelector,
                               List<CvssScoreVersionSelectionPolicy> versionSelectionPolicy
    ) {
        this.allVectors = allVectors;

        this.initialCvss2 = initialSelector.selectVector(allVectors, Cvss2.class);
        this.contextCvss2 = contextSelector.selectVector(allVectors, Cvss2.class);

        this.initialCvss3 = initialSelector.selectVector(allVectors, Cvss3P1.class);
        this.contextCvss3 = contextSelector.selectVector(allVectors, Cvss3P1.class);

        this.initialCvss4 = initialSelector.selectVector(allVectors, Cvss4P0.class);
        this.contextCvss4 = contextSelector.selectVector(allVectors, Cvss4P0.class);

        this.selectedInitialCvss = this.selectVersionedCvss(versionSelectionPolicy, this.initialCvss2, this.initialCvss3, this.initialCvss4);
        this.selectedContextCvss = this.selectVersionedCvss(versionSelectionPolicy, this.contextCvss2, this.contextCvss3, this.contextCvss4);
    }

    public boolean hasInitialCvss() {
        return this.initialCvss2 != null || this.initialCvss3 != null || this.initialCvss4 != null;
    }

    public boolean hasContextCvss() {
        return this.contextCvss2 != null || this.contextCvss3 != null || this.contextCvss4 != null;
    }

    public boolean hasAnyCvss() {
        return this.hasInitialCvss() || this.hasContextCvss();
    }

    public CvssVector getSelectedContextIfAvailableOtherwiseInitial() {
        if (this.selectedContextCvss != null) return this.selectedContextCvss;
        else return this.selectedInitialCvss;
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

    public Cvss2 getInitialCvss2() {
        return initialCvss2;
    }

    public Cvss2 getContextCvss2() {
        return contextCvss2;
    }

    public Cvss3P1 getInitialCvss3() {
        return initialCvss3;
    }

    public Cvss3P1 getContextCvss3() {
        return contextCvss3;
    }

    public Cvss4P0 getInitialCvss4() {
        return initialCvss4;
    }

    public Cvss4P0 getContextCvss4() {
        return contextCvss4;
    }

    public CvssVector getSelectedInitialCvss() {
        return selectedInitialCvss;
    }

    public CvssVector getSelectedContextCvss() {
        return selectedContextCvss;
    }

    @Override
    public String toString() {
        return "CvssSelectionResult{" +
                "allVectors=" + allVectors +
                ", initialCvss2=" + initialCvss2 +
                ", contextCvss2=" + contextCvss2 +
                ", initialCvss3=" + initialCvss3 +
                ", contextCvss3=" + contextCvss3 +
                ", initialCvss4=" + initialCvss4 +
                ", contextCvss4=" + contextCvss4 +
                ", selectedInitialCvss=" + selectedInitialCvss +
                ", selectedContextCvss=" + selectedContextCvss +
                '}';
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
