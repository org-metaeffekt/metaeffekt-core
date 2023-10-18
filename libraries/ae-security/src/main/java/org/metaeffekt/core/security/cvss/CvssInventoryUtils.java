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

import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public abstract class CvssInventoryUtils {

    /**
     * Number formatter that formats a double to a string with 1 decimal place and a "." as decimal separator.
     */
    private final static NumberFormat CVSS_SCORE_FORMATTER = new DecimalFormat("#.#");

    /**
     * Sets the score information in the {@link VulnerabilityMetaData} instance from the given {@link CvssVector} instances.
     *
     * @param vmd                The {@link VulnerabilityMetaData} instance to be updated.
     * @param v2                 The {@link Cvss2} instance, or <code>null</code> if not available.
     * @param v3                 The {@link Cvss3} instance, or <code>null</code> if not available.
     * @param v4                 The {@link org.metaeffekt.core.security.cvss.v4_0.Cvss4_0} instance, or <code>null</code> if not available.
     * @param cvssSeverityRanges The {@link CvssSeverityRanges} instance to use to calculate the severity categories.
     * @param vectorsModified    If true, will set the modified fields, the unmodified otherwise.
     */
    public static void applyNewCvssInformationOnVulnerabilityMetaData(VulnerabilityMetaData vmd, MultiScoreCvssVector v2, MultiScoreCvssVector v3, CvssVector v4, CvssSeverityRanges cvssSeverityRanges, boolean vectorsModified) {
        /*final Map<String, Object> scores = CvssInventoryUtils.extractCvssScoreAttributesFromVulnerabilityMetaData(vmd);

        // do not apply the vector if:
        // - it is not defined
        // - 100% identical to the unmodified vector
        // this change was made because of GHSA (advisories), which provide the same vector as the NVD as modified vector, which is not correct in our case
        final boolean shouldV2BeApplied = v2 != null && Cvss2.optionalParse(vmd.get(CVSS_UNMODIFIED_VECTOR_V2.getKey()))
                .map(obj -> !v2.equals(obj))
                .orElse(true);
        final boolean shouldV3BeApplied = v3 != null && Cvss3.optionalParse(vmd.get(CVSS_UNMODIFIED_VECTOR_V3.getKey()))
                .map(obj -> !v3.equals(obj))
                .orElse(true);

        if (shouldV2BeApplied) {
            if (vectorsModified) {
                // if the base vector parts of the modified vector are not defined, apply the unmodified vector if available
                if (!v2.isBaseDefined() && scores.containsKey(CVSS_UNMODIFIED_VECTOR_V2.getKey())) {
                    v2.applyVector(String.valueOf(scores.get(CVSS_UNMODIFIED_VECTOR_V2.getKey())));
                }

                // we require the base vector to be defined, otherwise the base scores cannot be calculated.
                // in this case, reset the modified attributes
                if (!v2.isBaseDefined()) {
                    for (InventoryAttribute attr : getCvssModifiedAttributes("v2")) {
                        scores.put(attr.getKey(), null);
                    }
                } else {
                    scores.put(CVSS_MODIFIED_VECTOR_V2.getKey(), v2.toString());
                    scores.put(CVSS_MODIFIED_BASE_V2.getKey(), v2.getBaseScore());
                    scores.put(CVSS_MODIFIED_IMPACT_V2.getKey(), v2.getImpactScore());
                    scores.put(CVSS_MODIFIED_EXPLOITABILITY_V2.getKey(), v2.getExploitabilityScore());

                    if (v2.isTemporalDefined()) {
                        scores.put(CVSS_TEMPORAL_V2.getKey(), v2.getTemporalScore());
                    } else {
                        scores.put(CVSS_TEMPORAL_V2.getKey(), null);
                    }
                    if (v2.isEnvironmentalDefined()) {
                        scores.put(CVSS_ENVIRONMENTAL_V2.getKey(), v2.getEnvironmentalScore());
                        scores.put(CVSS_ADJUSTED_IMPACT_V2.getKey(), v2.getAdjustedImpactScore());
                    } else {
                        scores.put(CVSS_ENVIRONMENTAL_V2.getKey(), null);
                        scores.put(CVSS_ADJUSTED_IMPACT_V2.getKey(), null);
                    }

                    final double modifiedOverallScore = v2.getOverallScore();
                    final CvssSeverityRanges.SeverityRange modifiedRange = cvssSeverityRanges.getRange(modifiedOverallScore);

                    // only apply the values id the range is not undefined
                    if (!CvssSeverityRanges.UNDEFINED_SEVERITY_RANGE.equals(modifiedRange)) {
                        scores.put(CVSS_MODIFIED_OVERALL_V2.getKey(), modifiedOverallScore);
                        scores.put(CVSS_MODIFIED_SEVERITY_V2.getKey(), modifiedRange.getName());
                        scores.put(CVSS_MODIFIED_COLOR_V2.getKey(), modifiedRange.getColor().toHex());
                    }
                }
            } else {
                scores.put(CVSS_BASE_V2.getKey(), v2.getBaseScore());
                scores.put(CVSS_IMPACT_V2.getKey(), v2.getImpactScore());
                scores.put(CVSS_EXPLOITABILITY_V2.getKey(), v2.getExploitabilityScore());

                final double unmodifiedOverallScore = v2.getOverallScore();

                scores.put(CVSS_UNMODIFIED_VECTOR_V2.getKey(), v2.toString());
                scores.put(CVSS_UNMODIFIED_OVERALL_V2.getKey(), unmodifiedOverallScore);
                CvssSeverityRanges.SeverityRange unmodifiedRange = cvssSeverityRanges.getRange(unmodifiedOverallScore);
                scores.put(CVSS_UNMODIFIED_SEVERITY_V2.getKey(), unmodifiedRange.getName());
                scores.put(CVSS_UNMODIFIED_COLOR_V2.getKey(), unmodifiedRange.getColor().toHex());
            }
        }

        if (shouldV3BeApplied) {
            if (vectorsModified) {
                // if the base vector parts of the modified vector are not defined, apply the unmodified vector if available
                if (!v3.isBaseDefined() && scores.containsKey(CVSS_UNMODIFIED_VECTOR_V3.getKey())) {
                    v3.applyVector(String.valueOf(scores.get(CVSS_UNMODIFIED_VECTOR_V3.getKey())));
                }

                // we require the base vector to be defined, otherwise the base scores cannot be calculated.
                // in this case, reset the modified attributes
                if (!v3.isBaseDefined()) {
                    for (InventoryAttribute attr : getCvssModifiedAttributes("v3")) {
                        scores.put(attr.getKey(), null);
                    }
                } else {
                    scores.put(CVSS_MODIFIED_VECTOR_V3.getKey(), v3.toString());
                    scores.put(CVSS_MODIFIED_BASE_V3.getKey(), v3.getBaseScore());
                    scores.put(CVSS_MODIFIED_IMPACT_V3.getKey(), v3.getImpactScore());
                    scores.put(CVSS_MODIFIED_EXPLOITABILITY_V3.getKey(), v3.getExploitabilityScore());

                    if (v3.isTemporalDefined()) {
                        scores.put(CVSS_TEMPORAL_V3.getKey(), v3.getTemporalScore());
                    } else {
                        scores.put(CVSS_TEMPORAL_V3.getKey(), null);
                    }
                    if (v3.isEnvironmentalDefined()) {
                        scores.put(CVSS_ENVIRONMENTAL_V3.getKey(), v3.getEnvironmentalScore());
                        scores.put(CVSS_ADJUSTED_IMPACT_V3.getKey(), v3.getAdjustedImpactScore());
                    } else {
                        scores.put(CVSS_ENVIRONMENTAL_V3.getKey(), null);
                        scores.put(CVSS_ADJUSTED_IMPACT_V3.getKey(), null);
                    }

                    final double modifiedOverallScore = v3.getOverallScore();
                    final CvssSeverityRanges.SeverityRange modifiedRange = cvssSeverityRanges.getRange(modifiedOverallScore);

                    // only apply the values id the range is not undefined
                    if (!CvssSeverityRanges.UNDEFINED_SEVERITY_RANGE.equals(modifiedRange)) {
                        scores.put(CVSS_MODIFIED_OVERALL_V3.getKey(), modifiedOverallScore);
                        scores.put(CVSS_MODIFIED_SEVERITY_V3.getKey(), modifiedRange.getName());
                        scores.put(CVSS_MODIFIED_COLOR_V3.getKey(), modifiedRange.getColor().toHex());
                    }
                }
            } else {
                scores.put(CVSS_BASE_V3.getKey(), v3.getBaseScore());
                scores.put(CVSS_IMPACT_V3.getKey(), v3.getImpactScore());
                scores.put(CVSS_EXPLOITABILITY_V3.getKey(), v3.getExploitabilityScore());

                final double unmodifiedOverallScore = v3.getOverallScore();

                scores.put(CVSS_UNMODIFIED_VECTOR_V3.getKey(), v3.toString());
                scores.put(CVSS_UNMODIFIED_OVERALL_V3.getKey(), unmodifiedOverallScore);
                CvssSeverityRanges.SeverityRange unmodifiedRange = cvssSeverityRanges.getRange(unmodifiedOverallScore);
                scores.put(CVSS_UNMODIFIED_SEVERITY_V3.getKey(), unmodifiedRange.getName());
                scores.put(CVSS_UNMODIFIED_COLOR_V3.getKey(), unmodifiedRange.getColor().toHex());
            }
        }

        if (v4 != null) {
            if (vectorsModified) {
                scores.put(CVSS_MODIFIED_VECTOR_V4.getKey(), v4.toString());
                scores.put(CVSS_MODIFIED_BASE_V4.getKey(), v4.getBaseScore());

                final double modifiedOverallScore = v4.getOverallScore();
                final CvssSeverityRanges.SeverityRange modifiedRange = cvssSeverityRanges.getRange(modifiedOverallScore);

                // only apply the values id the range is not undefined
                if (!CvssSeverityRanges.UNDEFINED_SEVERITY_RANGE.equals(modifiedRange)) {
                    scores.put(CVSS_MODIFIED_OVERALL_V4.getKey(), modifiedOverallScore);
                    scores.put(CVSS_MODIFIED_SEVERITY_V4.getKey(), modifiedRange.getName());
                    scores.put(CVSS_MODIFIED_COLOR_V4.getKey(), modifiedRange.getColor().toHex());
                }
            } else {
                scores.put(CVSS_BASE_V4.getKey(), v4.getBaseScore());
                scores.put(CVSS_UNMODIFIED_VECTOR_V4.getKey(), v4.toString());

                final double unmodifiedOverallScore = v4.getOverallScore();

                scores.put(CVSS_UNMODIFIED_OVERALL_V4.getKey(), unmodifiedOverallScore);
                CvssSeverityRanges.SeverityRange unmodifiedRange = cvssSeverityRanges.getRange(unmodifiedOverallScore);
                scores.put(CVSS_UNMODIFIED_SEVERITY_V4.getKey(), unmodifiedRange.getName());
                scores.put(CVSS_UNMODIFIED_COLOR_V4.getKey(), unmodifiedRange.getColor().toHex());
            }
        }

        // find what score is larger (based on unmodified score)
        final double cvss2Overall = parseScore(String.valueOf(scores.getOrDefault(CVSS_UNMODIFIED_OVERALL_V2.getKey(), 0)));
        final double cvss3Overall = parseScore(String.valueOf(scores.getOrDefault(CVSS_UNMODIFIED_OVERALL_V3.getKey(), 0)));
        final double cvss4Overall = parseScore(String.valueOf(scores.getOrDefault(CVSS_UNMODIFIED_OVERALL_V4.getKey(), 0)));
        final CvssVector largestCvssVector = cvss2Overall > cvss3Overall && cvss2Overall > cvss4Overall ?
                v2 : cvss3Overall > cvss4Overall ?
                v3 : v4;

        // set the max scores depending on the max overall score (unmodified)
        if (largestCvssVector instanceof Cvss3) {
            copyMapValue(scores, CVSS_UNMODIFIED_VECTOR_V3, CVSS_UNMODIFIED_VECTOR_MAX);
            copyMapValue(scores, CVSS_MODIFIED_VECTOR_V3, CVSS_MODIFIED_VECTOR_MAX);

            copyMapValue(scores, CVSS_BASE_V3, CVSS_BASE_MAX);
            copyMapValue(scores, CVSS_IMPACT_V3, CVSS_IMPACT_MAX);
            copyMapValue(scores, CVSS_EXPLOITABILITY_V3, CVSS_EXPLOITABILITY_MAX);

            copyMapValue(scores, CVSS_MODIFIED_BASE_V3, CVSS_MODIFIED_BASE_MAX);
            copyMapValue(scores, CVSS_MODIFIED_IMPACT_V3, CVSS_MODIFIED_IMPACT_MAX);
            copyMapValue(scores, CVSS_MODIFIED_EXPLOITABILITY_V3, CVSS_MODIFIED_EXPLOITABILITY_MAX);

            copyMapValue(scores, CVSS_TEMPORAL_V3, CVSS_TEMPORAL_MAX);
            copyMapValue(scores, CVSS_ENVIRONMENTAL_V3, CVSS_ENVIRONMENTAL_MAX);
            copyMapValue(scores, CVSS_ADJUSTED_IMPACT_V3, CVSS_ADJUSTED_IMPACT_MAX);

            copyMapValue(scores, CVSS_MODIFIED_OVERALL_V3, CVSS_MODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_MODIFIED_SEVERITY_V3, CVSS_MODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_MODIFIED_COLOR_V3, CVSS_MODIFIED_COLOR_MAX);

            copyMapValue(scores, CVSS_UNMODIFIED_OVERALL_V3, CVSS_UNMODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_SEVERITY_V3, CVSS_UNMODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_COLOR_V3, CVSS_UNMODIFIED_COLOR_MAX);

        } else if (largestCvssVector instanceof Cvss2) {
            copyMapValue(scores, CVSS_UNMODIFIED_VECTOR_V2, CVSS_UNMODIFIED_VECTOR_MAX);
            copyMapValue(scores, CVSS_MODIFIED_VECTOR_V2, CVSS_MODIFIED_VECTOR_MAX);

            copyMapValue(scores, CVSS_BASE_V2, CVSS_BASE_MAX);
            copyMapValue(scores, CVSS_IMPACT_V2, CVSS_IMPACT_MAX);
            copyMapValue(scores, CVSS_EXPLOITABILITY_V2, CVSS_EXPLOITABILITY_MAX);

            copyMapValue(scores, CVSS_MODIFIED_BASE_V2, CVSS_MODIFIED_BASE_MAX);
            copyMapValue(scores, CVSS_MODIFIED_IMPACT_V2, CVSS_MODIFIED_IMPACT_MAX);
            copyMapValue(scores, CVSS_MODIFIED_EXPLOITABILITY_V2, CVSS_MODIFIED_EXPLOITABILITY_MAX);

            copyMapValue(scores, CVSS_TEMPORAL_V2, CVSS_TEMPORAL_MAX);
            copyMapValue(scores, CVSS_ENVIRONMENTAL_V2, CVSS_ENVIRONMENTAL_MAX);
            copyMapValue(scores, CVSS_ADJUSTED_IMPACT_V2, CVSS_ADJUSTED_IMPACT_MAX);

            copyMapValue(scores, CVSS_MODIFIED_OVERALL_V2, CVSS_MODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_MODIFIED_SEVERITY_V2, CVSS_MODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_MODIFIED_COLOR_V2, CVSS_MODIFIED_COLOR_MAX);

            copyMapValue(scores, CVSS_UNMODIFIED_OVERALL_V2, CVSS_UNMODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_SEVERITY_V2, CVSS_UNMODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_COLOR_V2, CVSS_UNMODIFIED_COLOR_MAX);

        } else if (largestCvssVector instanceof Cvss4_0) {
            copyMapValue(scores, CVSS_UNMODIFIED_VECTOR_V4, CVSS_UNMODIFIED_VECTOR_MAX);
            copyMapValue(scores, CVSS_MODIFIED_VECTOR_V4, CVSS_MODIFIED_VECTOR_MAX);

            copyMapValue(scores, CVSS_BASE_V4, CVSS_BASE_MAX);
            scores.put(CVSS_IMPACT_MAX.getKey(), "-1.0");
            scores.put(CVSS_EXPLOITABILITY_MAX.getKey(), "-1.0");
            copyMapValue(scores, CVSS_MODIFIED_BASE_V4, CVSS_MODIFIED_BASE_MAX);

            scores.put(CVSS_MODIFIED_IMPACT_MAX.getKey(), "-1.0");
            scores.put(CVSS_MODIFIED_EXPLOITABILITY_MAX.getKey(), "-1.0");

            scores.put(CVSS_TEMPORAL_MAX.getKey(), "-1.0");
            scores.put(CVSS_ENVIRONMENTAL_MAX.getKey(), "-1.0");
            scores.put(CVSS_ADJUSTED_IMPACT_MAX.getKey(), "-1.0");

            copyMapValue(scores, CVSS_MODIFIED_OVERALL_V4, CVSS_MODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_MODIFIED_SEVERITY_V4, CVSS_MODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_MODIFIED_COLOR_V4, CVSS_MODIFIED_COLOR_MAX);

            copyMapValue(scores, CVSS_UNMODIFIED_OVERALL_V4, CVSS_UNMODIFIED_OVERALL_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_SEVERITY_V4, CVSS_UNMODIFIED_SEVERITY_MAX);
            copyMapValue(scores, CVSS_UNMODIFIED_COLOR_V4, CVSS_UNMODIFIED_COLOR_MAX);
        }

        for (Map.Entry<String, Object> score : scores.entrySet()) {
            if (score.getValue() != null && !String.valueOf(score.getValue()).equals("-1.0")) {
                if (score.getValue() instanceof Double) {
                    vmd.set(score.getKey(), CVSS_SCORE_FORMATTER.format(score.getValue()));
                } else {
                    vmd.set(score.getKey(), String.valueOf(score.getValue()));
                }
            } else {
                vmd.set(score.getKey(), null);
            }
        }*/
    }

    /*private static void copyMapValue(Map<String, Object> scores, InventoryAttribute fromAttribute, InventoryAttribute toAttribute) {
        scores.put(toAttribute.getKey(), scores.getOrDefault(fromAttribute.getKey(), null));
    }*/

    private static Map<String, Object> extractCvssScoreAttributesFromVulnerabilityMetaData(VulnerabilityMetaData vmd) {
        final Map<String, Object> scoresMap = new HashMap<>();

        /*getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_VECTOR_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_VECTOR_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_VECTOR_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_VECTOR_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_VECTOR_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_VECTOR_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_BASE_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_BASE_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_BASE_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_IMPACT_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_IMPACT_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_IMPACT_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_EXPLOITABILITY_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_EXPLOITABILITY_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_EXPLOITABILITY_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_BASE_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_BASE_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_BASE_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_IMPACT_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_IMPACT_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_IMPACT_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_EXPLOITABILITY_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_EXPLOITABILITY_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_EXPLOITABILITY_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_TEMPORAL_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_TEMPORAL_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_TEMPORAL_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ENVIRONMENTAL_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ENVIRONMENTAL_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ENVIRONMENTAL_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ADJUSTED_IMPACT_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ADJUSTED_IMPACT_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_ADJUSTED_IMPACT_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_OVERALL_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_OVERALL_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_OVERALL_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_SEVERITY_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_SEVERITY_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_SEVERITY_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_COLOR_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_COLOR_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_COLOR_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_OVERALL_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_OVERALL_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_OVERALL_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_SEVERITY_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_SEVERITY_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_SEVERITY_MAX);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_COLOR_V2);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_COLOR_V3);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_COLOR_MAX);

        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_VECTOR_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_VECTOR_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_BASE_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_BASE_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_OVERALL_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_SEVERITY_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_UNMODIFIED_COLOR_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_OVERALL_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_SEVERITY_V4);
        getOrSkipVmdCvssScore(vmd, scoresMap, CVSS_MODIFIED_COLOR_V4);*/

        return scoresMap;
    }

    /*private static void getOrSkipVmdCvssScore(VulnerabilityMetaData vmd, Map<String, Object> scoresMap, InventoryAttribute a) {
        if (vmd.has(a.getKey())) {
            scoresMap.put(a.getKey(), vmd.get(a.getKey()));
        }
    }*/

    public static double parseScore(String scoreString) {
        return parseScore(scoreString, 0d);
    }

    public static double parseScore(String scoreString, double defaultValue) {
        if (scoreString == null) {
            return defaultValue;
        }
        // depending on which locale the scores have been created a comma may be used as separator
        return Double.parseDouble(scoreString.replace(",", "."));
    }
}
