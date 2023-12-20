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
package org.metaeffekt.core.security.cvss.v2;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Class for modeling a CVSS:2.0 Vector, allowing for manipulating the vector components and calculating scores.
 * <p>
 * The scores provided by the 2.0 specification are:
 * <ul>
 *     <li>base (requires all base components)</li>
 *     <li>impact (requires all base components)</li>
 *     <li>exploitability (requires all base components)</li>
 *     <li>temporal (requires all base attributes and at least one temporal component)</li>
 *     <li>adjusted impact (requires all base attributes and at least one environmental component)</li>
 *     <li>environmental (requires all base attributes and at least one environmental component)</li>
 * </ul>
 * See the definitions of the individual scoring methods for the equations used for the scores.
 */
public class Cvss2 extends MultiScoreCvssVector {

    // base
    private AccessVector accessVector = AccessVector.NULL;
    private AccessComplexity accessComplexity = AccessComplexity.NULL;
    private Authentication authentication = Authentication.NULL;
    private CIAImpact confidentialityImpact = CIAImpact.NULL;
    private CIAImpact integrityImpact = CIAImpact.NULL;
    private CIAImpact availabilityImpact = CIAImpact.NULL;

    // temporal
    private Exploitability exploitability = Exploitability.NULL;
    private RemediationLevel remediationLevel = RemediationLevel.NULL;
    private ReportConfidence reportConfidence = ReportConfidence.NULL;

    // environmental
    private CollateralDamagePotential collateralDamagePotential = CollateralDamagePotential.NULL;
    private TargetDistribution targetDistribution = TargetDistribution.NULL;
    private CIARequirement confidentialityRequirement = CIARequirement.NULL;
    private CIARequirement integrityRequirement = CIARequirement.NULL;
    private CIARequirement availabilityRequirement = CIARequirement.NULL;

    public Cvss2() {
        super();
    }

    public Cvss2(String vector) {
        super.applyVector(vector);
    }

    public Cvss2(String vector, CvssSource source) {
        super(source);
        super.applyVector(vector);
    }

    public Cvss2(String vector, CvssSource source, JSONObject applicabilityCondition) {
        super(source, applicabilityCondition);
        super.applyVector(vector);
    }

    public Cvss2(String vector, Collection<CvssSource> sources, JSONObject applicabilityCondition) {
        super(sources, applicabilityCondition);
        super.applyVector(vector);
    }

    @Override
    protected boolean applyVectorArgument(String identifier, String value) {
        switch (identifier) {
            case "AV": // base
                accessVector = AccessVector.fromString(value);
                break;
            case "AC":
                accessComplexity = AccessComplexity.fromString(value);
                break;
            case "AU":
                authentication = Authentication.fromString(value);
                break;
            case "C":
                confidentialityImpact = CIAImpact.fromString(value);
                break;
            case "I":
                integrityImpact = CIAImpact.fromString(value);
                break;
            case "A":
                availabilityImpact = CIAImpact.fromString(value);
                break;
            case "E": // temporal
                exploitability = Exploitability.fromString(value);
                break;
            case "RL":
                remediationLevel = RemediationLevel.fromString(value);
                break;
            case "RC":
                reportConfidence = ReportConfidence.fromString(value);
                break;
            case "CDP": // environmental
                collateralDamagePotential = CollateralDamagePotential.fromString(value);
                break;
            case "TD":
                targetDistribution = TargetDistribution.fromString(value);
                break;
            case "CR":
                confidentialityRequirement = CIARequirement.fromString(value);
                break;
            case "IR":
                integrityRequirement = CIARequirement.fromString(value);
                break;
            case "AR":
                availabilityRequirement = CIARequirement.fromString(value);
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cvss2)) return false;
        Cvss2 cvss2 = (Cvss2) o;
        return accessVector == cvss2.accessVector &&
                accessComplexity == cvss2.accessComplexity &&
                authentication == cvss2.authentication &&
                confidentialityImpact == cvss2.confidentialityImpact &&
                integrityImpact == cvss2.integrityImpact &&
                availabilityImpact == cvss2.availabilityImpact &&
                exploitability == cvss2.exploitability &&
                remediationLevel == cvss2.remediationLevel &&
                reportConfidence == cvss2.reportConfidence &&
                collateralDamagePotential == cvss2.collateralDamagePotential &&
                targetDistribution == cvss2.targetDistribution &&
                confidentialityRequirement == cvss2.confidentialityRequirement &&
                integrityRequirement == cvss2.integrityRequirement &&
                availabilityRequirement == cvss2.availabilityRequirement;
    }

    /**
     * BaseScore = round_to_1_decimal(((0.6 * Impact) + (0.4 * Exploitability) - 1.5) * f(Impact))
     *
     * @return The Cvss Base Score.
     */
    @Override
    public double getBaseScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        double impact = calculateImpactScore();
        return round(((0.6 * impact) + (0.4 * calculateExploitabilityScore()) - 1.5) * f(impact), 1);
    }

    /**
     * Impact = 10.41 * (1 - (1 - ConfImpact) * (1 - IntegImpact) * (1 - AvailImpact))
     *
     * @return The Cvss Impact Score.
     */
    private double calculateImpactScore() {
        return 10.41 * (1 - (1 - confidentialityImpact.factor) * (1 - integrityImpact.factor) * (1 - availabilityImpact.factor));
    }

    @Override
    public double getImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        return round(calculateImpactScore(), 1);
    }

    /**
     * Exploitability = 20 * AccessComplexity * Authentication * AccessVector
     *
     * @return The Cvss Exploitability Score.
     */
    private double calculateExploitabilityScore() {
        return 20 * accessComplexity.factor * authentication.factor * accessVector.factor;
    }

    @Override
    public double getExploitabilityScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        return round(calculateExploitabilityScore(), 1);
    }

    /**
     * TemporalScore = round_to_1_decimal(BaseScore * Exploitability * RemediationLevel * ReportConfidence)
     *
     * @return The Cvss Temporal Score.
     */
    private double calculateTemporalScore() {
        double exp, rem, rep;
        if (exploitability == Exploitability.NULL) exp = Exploitability.NOT_DEFINED.factor;
        else exp = exploitability.factor;
        if (remediationLevel == RemediationLevel.NULL) rem = RemediationLevel.NOT_DEFINED.factor;
        else rem = remediationLevel.factor;
        if (reportConfidence == ReportConfidence.NULL) rep = ReportConfidence.NOT_DEFINED.factor;
        else rep = reportConfidence.factor;
        return getBaseScore() * exp * rem * rep;
    }

    @Override
    public double getTemporalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyTemporalDefined()) return Double.NaN;
        return round(calculateTemporalScore(), 1);
    }

    /**
     * EnvironmentalScore = round_to_1_decimal((AdjustedTemporal + (10 - AdjustedTemporal) * CollateralDamagePotential) * TargetDistribution)
     *
     * @return The Cvss Environmental Score.
     */
    private double calculateEnvironmentalScore() {
        double col, tar;
        if (collateralDamagePotential == CollateralDamagePotential.NULL)
            col = CollateralDamagePotential.NOT_DEFINED.factor;
        else col = collateralDamagePotential.factor;
        if (targetDistribution == TargetDistribution.NULL) tar = TargetDistribution.NOT_DEFINED.factor;
        else tar = targetDistribution.factor;
        double adjustedTemporal = calculateAdjustedTemporalScore();
        return (adjustedTemporal
                + (10 - adjustedTemporal)
                * col)
                * tar;
    }

    @Override
    public double getEnvironmentalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;
        return round(calculateEnvironmentalScore(), 1);
    }

    /**
     * AdjustedTemporal = TemporalScore recomputed with the BaseScore's Impact sub-equation replaced with the AdjustedImpact equation<br>
     * TemporalScore = AdjustedBaseScore * Exploitability * RemediationLevel * ReportConfidence
     *
     * @return The Cvss Adjusted Temporal Score.
     */
    private double calculateAdjustedTemporalScore() {
        double exp, rem, rep;
        if (exploitability == Exploitability.NULL) exp = Exploitability.NOT_DEFINED.factor;
        else exp = exploitability.factor;
        if (remediationLevel == RemediationLevel.NULL) rem = RemediationLevel.NOT_DEFINED.factor;
        else rem = remediationLevel.factor;
        if (reportConfidence == ReportConfidence.NULL) rep = ReportConfidence.NOT_DEFINED.factor;
        else rep = reportConfidence.factor;
        return calculateAdjustedBaseScore() * exp * rem * rep;
    }

    /**
     * f(impact) = 0 if Impact=0, 1.176 otherwise
     *
     * @return Modified impact score.
     */
    private double f(double impact) {
        if (impact == 0) return 0;
        return 1.176;
    }

    private double calculateAdjustedBaseScore() {
        double adjustedImpact = calculateAdjustedImpact();
        return ((0.6 * adjustedImpact) + (0.4 * round(calculateExploitabilityScore(), 1)) - 1.5) * f(adjustedImpact);
    }

    /**
     * AdjustedImpact = min(10, 10.41 * (1 - (1 - ConfImpact * ConfReq) * (1 - IntegImpact * IntegReq) * (1 - AvailImpact * AvailReq)))
     *
     * @return The Cvss Adjusted Impact Score.
     */
    private double calculateAdjustedImpact() {
        double confr, intr, avar;
        if (confidentialityRequirement == CIARequirement.NULL) confr = CIARequirement.NOT_DEFINED.factor;
        else confr = confidentialityRequirement.factor;
        if (integrityRequirement == CIARequirement.NULL) intr = CIARequirement.NOT_DEFINED.factor;
        else intr = integrityRequirement.factor;
        if (availabilityRequirement == CIARequirement.NULL) avar = CIARequirement.NOT_DEFINED.factor;
        else avar = availabilityRequirement.factor;
        return Math.min(10,
                10.41 * (1 -
                        (1 - confidentialityImpact.factor * confr)
                                * (1 - integrityImpact.factor * intr)
                                * (1 - availabilityImpact.factor * avar)));
    }

    @Override
    public double getAdjustedImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;
        return round(calculateAdjustedImpact(), 1);
    }

    @Override
    public double getOverallScore() {
        if (isAnyEnvironmentalDefined()) return getEnvironmentalScore();
        else if (isAnyTemporalDefined()) return getTemporalScore();
        return getBaseScore();
    }

    @Override
    public CvssSeverityRanges.SeverityRange getDefaultSeverityCategory() {
        return getSeverityCategory(CvssSeverityRanges.CVSS_2_SEVERITY_RANGES);
    }

    public CvssSeverityRanges.SeverityRange getCvss3SeverityCategory() {
        return getSeverityCategory(CvssSeverityRanges.CVSS_3_SEVERITY_RANGES);
    }

    public String getAccessComplexity() {
        return accessComplexity.identifier;
    }

    public String getAccessVector() {
        return accessVector.identifier;
    }

    public String getAuthentication() {
        return authentication.identifier;
    }

    public String getAvailabilityImpact() {
        return availabilityImpact.identifier;
    }

    public String getConfidentialityImpact() {
        return confidentialityImpact.identifier;
    }

    public String getIntegrityImpact() {
        return integrityImpact.identifier;
    }

    public String getCollateralDamagePotential() {
        return collateralDamagePotential.identifier;
    }

    public String getExploitability() {
        return exploitability.identifier;
    }

    public String getConfidentialityRequirement() {
        return confidentialityRequirement.identifier;
    }

    public String getIntegrityRequirement() {
        return integrityRequirement.identifier;
    }

    public String getAvailabilityRequirement() {
        return availabilityRequirement.identifier;
    }

    public String getRemediationLevel() {
        return remediationLevel.identifier;
    }

    public String getReportConfidence() {
        return reportConfidence.identifier;
    }

    public String getTargetDistribution() {
        return targetDistribution.identifier;
    }

    public void setAccessComplexity(AccessComplexity accessComplexity) {
        this.accessComplexity = accessComplexity;
    }

    public void setAccessVector(AccessVector accessVector) {
        this.accessVector = accessVector;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public void setAvailabilityImpact(CIAImpact availabilityImpact) {
        this.availabilityImpact = availabilityImpact;
    }

    public void setCollateralDamagePotential(CollateralDamagePotential collateralDamagePotential) {
        this.collateralDamagePotential = collateralDamagePotential;
    }

    public void setConfidentialityImpact(CIAImpact confidentialityImpact) {
        this.confidentialityImpact = confidentialityImpact;
    }

    public void setIntegrityImpact(CIAImpact integrityImpact) {
        this.integrityImpact = integrityImpact;
    }

    public void setExploitability(Exploitability exploitability) {
        this.exploitability = exploitability;
    }

    public void setRemediationLevel(RemediationLevel remediationLevel) {
        this.remediationLevel = remediationLevel;
    }

    public void setReportConfidence(ReportConfidence reportConfidence) {
        this.reportConfidence = reportConfidence;
    }

    public void setTargetDistribution(TargetDistribution targetDistribution) {
        this.targetDistribution = targetDistribution;
    }

    public void setConfidentialityRequirement(CIARequirement confidentialityRequirement) {
        this.confidentialityRequirement = confidentialityRequirement;
    }

    public void setIntegrityRequirement(CIARequirement integrityRequirement) {
        this.integrityRequirement = integrityRequirement;
    }

    public void setAvailabilityRequirement(CIARequirement availabilityRequirement) {
        this.availabilityRequirement = availabilityRequirement;
    }

    @Override
    public boolean isBaseFullyDefined() {
        return accessVector != AccessVector.NULL
                && accessComplexity != AccessComplexity.NULL
                && authentication != Authentication.NULL
                && confidentialityImpact != CIAImpact.NULL
                && integrityImpact != CIAImpact.NULL
                && availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyBaseDefined() {
        return accessVector != AccessVector.NULL
                || accessComplexity != AccessComplexity.NULL
                || authentication != Authentication.NULL
                || confidentialityImpact != CIAImpact.NULL
                || integrityImpact != CIAImpact.NULL
                || availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyTemporalDefined() {
        return exploitability != Exploitability.NULL
                || remediationLevel != RemediationLevel.NULL
                || reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isTemporalFullyDefined() {
        return exploitability != Exploitability.NULL
                && remediationLevel != RemediationLevel.NULL
                && reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isAnyEnvironmentalDefined() {
        return collateralDamagePotential != CollateralDamagePotential.NULL
                || targetDistribution != TargetDistribution.NULL
                || confidentialityRequirement != CIARequirement.NULL
                || integrityRequirement != CIARequirement.NULL
                || availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public boolean isEnvironmentalFullyDefined() {
        return collateralDamagePotential != CollateralDamagePotential.NULL
                && targetDistribution != TargetDistribution.NULL
                && confidentialityRequirement != CIARequirement.NULL
                && integrityRequirement != CIARequirement.NULL
                && availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public void clearTemporal() {
        exploitability = Exploitability.NULL;
        remediationLevel = RemediationLevel.NULL;
        reportConfidence = ReportConfidence.NULL;
    }

    @Override
    public void clearEnvironmental() {
        collateralDamagePotential = CollateralDamagePotential.NULL;
        targetDistribution = TargetDistribution.NULL;
        confidentialityRequirement = CIARequirement.NULL;
        integrityRequirement = CIARequirement.NULL;
        availabilityRequirement = CIARequirement.NULL;
    }

    @Override
    public BakedCvssVectorScores bakeScores() {
        return new BakedCvssVectorScores(this);
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static String getVersionName() {
        return "CVSS:2.0";
    }

    @Override
    public String getName() {
        return getVersionName();
    }

    /**
     * <pre>https://nvd.nist.gov/vuln-metrics/cvss/v2-calculator?vector=(%s)&amp;version=2.0</pre>
     * Where <code>%s</code> is replaced with the current vector. Examples:
     * <ul>
     *     <li>https://nvd.nist.gov/vuln-metrics/cvss/v2-calculator?vector=(AV:N/AC:L/Au:N/C:P/I:P/A:P)&amp;version=2.0</li>
     *     <li>https://nvd.nist.gov/vuln-metrics/cvss/v2-calculator?vector=(AV:N/AC:L/Au:N/C:C/I:C/A:C/CDP:ND/TD:H/CR:ND/IR:ND/AR:ND)&amp;version=2.0</li>
     * </ul>
     */
    @Override
    public String getWebEditorLink() {
        return String.format("https://nvd.nist.gov/vuln-metrics/cvss/v2-calculator?vector=(%s)&version=2.0", this);
    }

    @Override
    public String toString() {
        final StringBuilder vector = new StringBuilder();

        vector.append("AV:").append(accessVector.shortIdentifier).append("/");
        vector.append("AC:").append(accessComplexity.shortIdentifier).append("/");
        vector.append("Au:").append(authentication.shortIdentifier).append("/");
        vector.append("C:").append(confidentialityImpact.shortIdentifier).append("/");
        vector.append("I:").append(integrityImpact.shortIdentifier).append("/");
        vector.append("A:").append(availabilityImpact.shortIdentifier).append("/");
        vector.append("E:").append(exploitability.shortIdentifier).append("/");
        vector.append("RL:").append(remediationLevel.shortIdentifier).append("/");
        vector.append("RC:").append(reportConfidence.shortIdentifier).append("/");
        vector.append("CDP:").append(collateralDamagePotential.shortIdentifier).append("/");
        vector.append("TD:").append(targetDistribution.shortIdentifier).append("/");
        vector.append("CR:").append(confidentialityRequirement.shortIdentifier).append("/");
        vector.append("IR:").append(integrityRequirement.shortIdentifier).append("/");
        vector.append("AR:").append(availabilityRequirement.shortIdentifier);

        return vector.toString().replaceAll("[^:/]+:X", "").replaceAll("/{2,}", "/").replaceAll("/$", "").replaceAll("^/", "");
    }

    @Override
    public int size() {
        int size = 0;

        if (accessVector != AccessVector.NULL) size++;
        if (accessComplexity != AccessComplexity.NULL) size++;
        if (authentication != Authentication.NULL) size++;
        if (confidentialityImpact != CIAImpact.NULL) size++;
        if (integrityImpact != CIAImpact.NULL) size++;
        if (availabilityImpact != CIAImpact.NULL) size++;
        if (exploitability != Exploitability.NULL) size++;
        if (remediationLevel != RemediationLevel.NULL) size++;
        if (reportConfidence != ReportConfidence.NULL) size++;
        if (collateralDamagePotential != CollateralDamagePotential.NULL) size++;
        if (targetDistribution != TargetDistribution.NULL) size++;
        if (confidentialityRequirement != CIARequirement.NULL) size++;
        if (integrityRequirement != CIARequirement.NULL) size++;
        if (availabilityRequirement != CIARequirement.NULL) size++;

        return size;
    }

    public enum AccessVector implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        LOCAL("LOCAL", "L", 0.395),
        ADJACENT_NETWORK("ADJACENT_NETWORK", "A", 0.646),
        NETWORK("NETWORK", "N", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        AccessVector(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AccessVector fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum AccessComplexity implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        HIGH("HIGH", "H", 0.35),
        MEDIUM("MEDIUM", "M", 0.61),
        LOW("LOW", "L", 0.71);

        public final String identifier, shortIdentifier;
        public final double factor;

        AccessComplexity(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AccessComplexity fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum Authentication implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        MULTIPLE("MULTIPLE", "M", 0.45),
        SINGLE("SINGLE", "S", 0.56),
        NONE("NONE", "N", 0.704);

        public final String identifier, shortIdentifier;
        public final double factor;

        Authentication(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static Authentication fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum CIAImpact implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        NONE("NONE", "N", 0.0),
        PARTIAL("PARTIAL", "P", 0.275),
        COMPLETE("COMPLETE", "C", 0.660);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIAImpact(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIAImpact fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum Exploitability implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        UNPROVEN("UNPROVEN", "U", 0.85),
        PROOF_OF_CONCEPT("PROOF_OF_CONCEPT", "POC", 0.9),
        FUNCTIONAL("FUNCTIONAL", "F", 0.95),
        HIGH("HIGH", "H", 1.0),
        NOT_DEFINED("NOT_DEFINED", "ND", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        Exploitability(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static Exploitability fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum RemediationLevel implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        OFFICIAL("OFFICIAL", "OF", 0.87),
        TEMPORARY("TEMPORARY", "TF", 0.9),
        WORKAROUND("WORKAROUND", "W", 0.95),
        UNAVAILABLE("UNAVAILABLE", "U", 1.0),
        NOT_DEFINED("NOT_DEFINED", "ND", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        RemediationLevel(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static RemediationLevel fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum ReportConfidence implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        UNCONFIRMED("UNCONFIRMED", "UC", 0.9),
        UNCORROBORATED("UNCORROBORATED", "UR", 0.95),
        CONFIRMED("CONFIRMED", "C", 1.0),
        NOT_DEFINED("NOT_DEFINED", "ND", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        ReportConfidence(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static ReportConfidence fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum CollateralDamagePotential implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        NONE("NONE", "N", 0.0),
        LOW("LOW", "L", 0.1),
        LOW_MEDIUM("LOW_MEDIUM", "LM", 0.3),
        MEDIUM_HIGH("MEDIUM_HIGH", "MH", 0.4),
        HIGH("HIGH", "H", 0.5),
        NOT_DEFINED("NOT_DEFINED", "ND", 0.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        CollateralDamagePotential(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CollateralDamagePotential fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum TargetDistribution implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        NONE("NONE", "N", 0.0),
        LOW("LOW", "L", 0.25),
        MEDIUM("MEDIUM", "M", 0.75),
        HIGH("HIGH", "H", 1.0),
        NOT_DEFINED("NOT_DEFINED", "ND", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        TargetDistribution(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static TargetDistribution fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum CIARequirement implements Cvss2Attribute {
        NULL("NULL", "X", 0.0),
        LOW("LOW", "L", 0.5),
        MEDIUM("MEDIUM", "M", 1.0),
        HIGH("HIGH", "H", 1.51),
        NOT_DEFINED("NOT_DEFINED", "ND", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIARequirement(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIARequirement fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    @Override
    public Cvss2 clone() {
        return new Cvss2(toString(), super.sources, super.applicabilityCondition);
    }

    @Override
    protected void completeVector() {
        cleanupTemporalVectorParts();
        cleanupEnvironmentalVectorParts();
    }

    private void cleanupTemporalVectorParts() {
        if (isAnyTemporalDefined()) {
            exploitability = exploitability == Exploitability.NULL ? Exploitability.NOT_DEFINED : exploitability;
            remediationLevel = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED : remediationLevel;
            reportConfidence = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED : reportConfidence;
        }

        if (isTemporalAllPartsNotDefined()) {
            clearTemporal();
        }
    }

    private void cleanupEnvironmentalVectorParts() {
        if (isAnyEnvironmentalDefined()) {
            collateralDamagePotential = collateralDamagePotential == CollateralDamagePotential.NULL ? CollateralDamagePotential.NOT_DEFINED : collateralDamagePotential;
            targetDistribution = targetDistribution == TargetDistribution.NULL ? TargetDistribution.NOT_DEFINED : targetDistribution;
            confidentialityRequirement = confidentialityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : confidentialityRequirement;
            integrityRequirement = integrityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : integrityRequirement;
            availabilityRequirement = availabilityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : availabilityRequirement;
        }

        if (isEnvironmentalAllPartsNotDefined()) {
            clearEnvironmental();
        }
    }

    private boolean isTemporalAllPartsNotDefined() {
        return exploitability == Exploitability.NOT_DEFINED &&
                remediationLevel == RemediationLevel.NOT_DEFINED &&
                reportConfidence == ReportConfidence.NOT_DEFINED;
    }

    private boolean isEnvironmentalAllPartsNotDefined() {
        return collateralDamagePotential == CollateralDamagePotential.NOT_DEFINED &&
                targetDistribution == TargetDistribution.NOT_DEFINED &&
                confidentialityRequirement == CIARequirement.NOT_DEFINED &&
                integrityRequirement == CIARequirement.NOT_DEFINED &&
                availabilityRequirement == CIARequirement.NOT_DEFINED;
    }

    public static Optional<Cvss2> optionalParse(String vector) {
        if (vector == null || StringUtils.isEmpty(CvssVector.normalizeVector(vector))) {
            return Optional.empty();
        }

        return Optional.of(new Cvss2(vector));
    }

    public interface Cvss2Attribute {
        String getShortIdentifier();
    }
}
