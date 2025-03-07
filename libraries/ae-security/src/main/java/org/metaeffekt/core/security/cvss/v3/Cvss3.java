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
package org.metaeffekt.core.security.cvss.v3;

import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;

import java.util.*;

/**
 * Class for modeling a CVSS:3.1 Vector, allowing for manipulating the vector components and calculating scores.
 * <p>
 * The scores provided by the 3.1 specification are:
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
public abstract class Cvss3 extends MultiScoreCvssVector {

    // base
    protected AttackVector attackVector = AttackVector.NULL;
    protected AttackComplexity attackComplexity = AttackComplexity.NULL;
    protected PrivilegesRequired privilegesRequired = PrivilegesRequired.NULL;
    protected UserInteraction userInteraction = UserInteraction.NULL;
    protected Scope scope = Scope.NULL;
    protected CIAImpact confidentialityImpact = CIAImpact.NULL;
    protected CIAImpact integrityImpact = CIAImpact.NULL;
    protected CIAImpact availabilityImpact = CIAImpact.NULL;

    // temporal
    protected ExploitCodeMaturity exploitCodeMaturity = ExploitCodeMaturity.NULL;
    protected RemediationLevel remediationLevel = RemediationLevel.NULL;
    protected ReportConfidence reportConfidence = ReportConfidence.NULL;

    // environmental
    protected AttackVector modifiedAttackVector = AttackVector.NULL;
    protected AttackComplexity modifiedAttackComplexity = AttackComplexity.NULL;
    protected PrivilegesRequired modifiedPrivilegesRequired = PrivilegesRequired.NULL;
    protected UserInteraction modifiedUserInteraction = UserInteraction.NULL;
    protected Scope modifiedScope = Scope.NULL;
    protected CIAImpact modifiedConfidentialityImpact = CIAImpact.NULL;
    protected CIAImpact modifiedIntegrityImpact = CIAImpact.NULL;
    protected CIAImpact modifiedAvailabilityImpact = CIAImpact.NULL;
    protected CIARequirement confidentialityRequirement = CIARequirement.NULL;
    protected CIARequirement integrityRequirement = CIARequirement.NULL;
    protected CIARequirement availabilityRequirement = CIARequirement.NULL;

    public Cvss3() {
        super();
    }

    public Cvss3(String vector) {
        super();
        super.applyVector(vector);
    }

    public Cvss3(String vector, CvssSource source) {
        super(source);
        super.applyVector(vector);
    }

    public Cvss3(String vector, CvssSource source, JSONObject applicabilityCondition) {
        super(source, applicabilityCondition);
        super.applyVector(vector);
    }

    public Cvss3(String vector, Collection<CvssSource> sources, JSONObject applicabilityCondition) {
        super(sources, applicabilityCondition);
        super.applyVector(vector);
    }

    @Override
    public boolean applyVectorArgument(String identifier, String value) {
        switch (identifier) {
            case "AV": // base
                attackVector = AttackVector.fromString(value);
                break;
            case "AC":
                attackComplexity = AttackComplexity.fromString(value);
                break;
            case "PR":
                privilegesRequired = PrivilegesRequired.fromString(value);
                break;
            case "UI":
                userInteraction = UserInteraction.fromString(value);
                break;
            case "S":
                scope = Scope.fromString(value);
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
                exploitCodeMaturity = ExploitCodeMaturity.fromString(value);
                break;
            case "RL":
                remediationLevel = RemediationLevel.fromString(value);
                break;
            case "RC":
                reportConfidence = ReportConfidence.fromString(value);
                break;
            case "MAV": // environmental
                modifiedAttackVector = AttackVector.fromString(value);
                break;
            case "MAC":
                modifiedAttackComplexity = AttackComplexity.fromString(value);
                break;
            case "MPR":
                modifiedPrivilegesRequired = PrivilegesRequired.fromString(value);
                break;
            case "MUI":
                modifiedUserInteraction = UserInteraction.fromString(value);
                break;
            case "MS":
                modifiedScope = Scope.fromString(value);
                break;
            case "MC":
                modifiedConfidentialityImpact = CIAImpact.fromString(value);
                break;
            case "MI":
                modifiedIntegrityImpact = CIAImpact.fromString(value);
                break;
            case "MA":
                modifiedAvailabilityImpact = CIAImpact.fromString(value);
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
    public Cvss3Attribute getVectorArgument(String identifier) {
        switch (identifier) {
            case "AV": // base
                return attackVector;
            case "AC":
                return attackComplexity;
            case "PR":
                return privilegesRequired;
            case "UI":
                return userInteraction;
            case "S":
                return scope;
            case "C":
                return confidentialityImpact;
            case "I":
                return integrityImpact;
            case "A":
                return availabilityImpact;
            case "E": // temporal
                return exploitCodeMaturity;
            case "RL":
                return remediationLevel;
            case "RC":
                return reportConfidence;
            case "MAV": // environmental
                return modifiedAttackVector;
            case "MAC":
                return modifiedAttackComplexity;
            case "MPR":
                return modifiedPrivilegesRequired;
            case "MUI":
                return modifiedUserInteraction;
            case "MS":
                return modifiedScope;
            case "MC":
                return modifiedConfidentialityImpact;
            case "MI":
                return modifiedIntegrityImpact;
            case "MA":
                return modifiedAvailabilityImpact;
            case "CR":
                return confidentialityRequirement;
            case "IR":
                return integrityRequirement;
            case "AR":
                return availabilityRequirement;

            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cvss3)) return false;
        Cvss3 cvss3 = (Cvss3) o;
        return attackVector == cvss3.attackVector &&
                attackComplexity == cvss3.attackComplexity &&
                privilegesRequired == cvss3.privilegesRequired &&
                userInteraction == cvss3.userInteraction &&
                scope == cvss3.scope &&
                confidentialityImpact == cvss3.confidentialityImpact &&
                integrityImpact == cvss3.integrityImpact &&
                availabilityImpact == cvss3.availabilityImpact &&
                exploitCodeMaturity == cvss3.exploitCodeMaturity &&
                remediationLevel == cvss3.remediationLevel &&
                reportConfidence == cvss3.reportConfidence &&
                modifiedAttackVector == cvss3.modifiedAttackVector &&
                modifiedAttackComplexity == cvss3.modifiedAttackComplexity &&
                modifiedPrivilegesRequired == cvss3.modifiedPrivilegesRequired &&
                modifiedUserInteraction == cvss3.modifiedUserInteraction &&
                modifiedScope == cvss3.modifiedScope &&
                modifiedConfidentialityImpact == cvss3.modifiedConfidentialityImpact &&
                modifiedIntegrityImpact == cvss3.modifiedIntegrityImpact &&
                modifiedAvailabilityImpact == cvss3.modifiedAvailabilityImpact &&
                confidentialityRequirement == cvss3.confidentialityRequirement &&
                integrityRequirement == cvss3.integrityRequirement &&
                availabilityRequirement == cvss3.availabilityRequirement;
    }

    protected final static double EXPLOITABILITY_COEFFICIENT = 8.22;
    protected final static double SCOPE_COEFFICIENT = 1.08;

    /**
     * If Impact &lt;= 0:         0, else<br>
     * If Scope is Unchanged:   Roundup (Minimum [(Impact + Exploitability), 10])
     * If Scope is Changed:     Roundup (Minimum [1.08 × (Impact + Exploitability), 10])
     *
     * @return The Cvss Base Score.
     */
    @Override
    public double getBaseScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        double impact = calculateImpactScore();
        if (impact <= 0) return 0.0;
        if (!scope.changed)
            return roundUp(Math.min(impact + calculateExploitabilityScore(), 10));
        else return roundUp(Math.min(SCOPE_COEFFICIENT * (impact + calculateExploitabilityScore()), 10));
    }

    /**
     * If Scope is Unchanged: 6.42 × ISS<br>
     * If Scope is Changed: 7.52 × (ISS - 0.029) - 3.25 × (ISS - 0.02)<sup>15</sup>
     *
     * @return The Cvss Impact Score.
     */
    protected double calculateImpactScore() {
        double iss = calculateISS();
        if (scope.changed) return Scope.SCOPE_CHANGED_FACTOR * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15);
        else return Scope.SCOPE_UNCHANGED_FACTOR * iss;
    }

    @Override
    public double getImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        final double impactScore = calculateImpactScore();
        if (impactScore <= 0) {
            return 0.0;
        }
        return round(impactScore, 1);
    }

    /**
     * 1 - [ (1 - Confidentiality) × (1 - Integrity) × (1 - Availability) ]
     *
     * @return The ISS score.
     */
    protected double calculateISS() {
        return 1 - ((1 - confidentialityImpact.factor) * (1 - integrityImpact.factor) * (1 - availabilityImpact.factor));
    }

    /**
     * 8.22 × AttackVector × AttackComplexity × PrivilegesRequired × UserInteraction
     *
     * @return The Cvss Exploitability Score.
     */
    protected double calculateExploitabilityScore() {
        if (scope.changed)
            return EXPLOITABILITY_COEFFICIENT * attackVector.factor * attackComplexity.factor * privilegesRequired.factorChanged * userInteraction.factor;
        else
            return EXPLOITABILITY_COEFFICIENT * attackVector.factor * attackComplexity.factor * privilegesRequired.factorUnchanged * userInteraction.factor;
    }

    @Override
    public double getExploitabilityScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        return round(calculateExploitabilityScore(), 1);
    }

    /**
     * Roundup (BaseScore × ExploitCodeMaturity × RemediationLevel × ReportConfidence)
     *
     * @return The Cvss Temporal Score.
     */
    protected double calculateTemporalScore() {
        if (!isAnyTemporalDefined()) return Double.NaN;
        double exploitCodeMaturityFactor = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED.factor : exploitCodeMaturity.factor;
        double remediationLevelFactor = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED.factor : remediationLevel.factor;
        double reportConfidenceFactor = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED.factor : reportConfidence.factor;
        double baseScore = getBaseScore();
        return baseScore * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor;
    }

    @Override
    public double getTemporalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyTemporalDefined()) return Double.NaN;
        return roundUp(calculateTemporalScore());
    }

    /**
     * If ModifiedImpact &lt;= 0:         0, else<br>
     * If ModifiedScope is Unchanged:   Roundup ( Roundup [Minimum ([ModifiedImpact + ModifiedExploitability], 10) ] × ExploitCodeMaturity × RemediationLevel × ReportConfidence)<br>
     * If ModifiedScope is Changed:     Roundup ( Roundup [Minimum (1.08 × [ModifiedImpact + ModifiedExploitability], 10) ] × ExploitCodeMaturity × RemediationLevel × ReportConfidence)
     *
     * @return The Cvss Environmental Score.
     */
    @Override
    public double getEnvironmentalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;

        double modifiedImpact = calculateAdjustedImpact();
        if (modifiedImpact <= 0) return 0;

        double modifiedExploitability = calculateAdjustedExploitability();
        double exploitCodeMaturityFactor = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED.factor : exploitCodeMaturity.factor;
        double remediationLevelFactor = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED.factor : remediationLevel.factor;
        double reportConfidenceFactor = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED.factor : reportConfidence.factor;

        if (isModifiedScope())
            return roundUp(roundUp(Math.min((modifiedImpact + modifiedExploitability), 10)) * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor);
        else
            return roundUp(roundUp(Math.min(1.08 * (modifiedImpact + modifiedExploitability), 10)) * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor);
    }

    /**
     * Minimum ( 1 - [ (1 - ConfidentialityRequirement × ModifiedConfidentiality) × (1 - IntegrityRequirement × ModifiedIntegrity) × (1 - AvailabilityRequirement × ModifiedAvailability) ], 0.915)
     *
     * @return The MISS Score.
     */
    protected double calculateMISS() {
        double mci = (modifiedConfidentialityImpact == CIAImpact.NULL || modifiedConfidentialityImpact == CIAImpact.NOT_DEFINED ? (confidentialityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : confidentialityImpact.factor) : modifiedConfidentialityImpact.factor);
        double mii = (modifiedIntegrityImpact == CIAImpact.NULL || modifiedIntegrityImpact == CIAImpact.NOT_DEFINED ? (integrityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : integrityImpact.factor) : modifiedIntegrityImpact.factor);
        double mai = (modifiedAvailabilityImpact == CIAImpact.NULL || modifiedAvailabilityImpact == CIAImpact.NOT_DEFINED ? (availabilityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : availabilityImpact.factor) : modifiedAvailabilityImpact.factor);

        double crFactor = confidentialityRequirement == CIARequirement.NULL && (integrityRequirement != CIARequirement.NULL || availabilityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : confidentialityRequirement.factor;
        double irFactor = integrityRequirement == CIARequirement.NULL && (confidentialityRequirement != CIARequirement.NULL || availabilityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : integrityRequirement.factor;
        double arFactor = availabilityRequirement == CIARequirement.NULL && (confidentialityRequirement != CIARequirement.NULL || integrityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : availabilityRequirement.factor;

        return Math.min(1 - (
                (1 - crFactor * mci) *
                        (1 - irFactor * mii) *
                        (1 - arFactor * mai)
        ), 0.915);
    }

    /**
     * If ModifiedScope is Unchanged: 6.42 × MISS<br>
     * If ModifiedScope is Changed: 7.52 × (MISS - 0.029) - 3.25 × (MISS × 0.9731 - 0.02)<sup>13</sup>
     *
     * @return The Cvss Adjusted Impact Score.
     */


    protected abstract double calculateAdjustedImpact();

    @Override
    public double getAdjustedImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;

        return Math.max(0, round(calculateAdjustedImpact(), 1));
    }

    /**
     * 8.22 × ModifiedAttackVector × ModifiedAttackComplexity × ModifiedPrivilegesRequired × ModifiedUserInteraction<br>
     * Replace the modified version with the base when not defined
     *
     * @return The Cvss Adjusted Exploitability Score.
     */
    protected double calculateAdjustedExploitability() {
        double mav = (modifiedAttackVector == AttackVector.NULL || modifiedAttackVector == AttackVector.NOT_DEFINED ? attackVector.factor : modifiedAttackVector.factor);
        double mac = (modifiedAttackComplexity == AttackComplexity.NULL || modifiedAttackComplexity == AttackComplexity.NOT_DEFINED ? attackComplexity.factor : modifiedAttackComplexity.factor);
        double mui = (modifiedUserInteraction == UserInteraction.NULL || modifiedUserInteraction == UserInteraction.NOT_DEFINED ? userInteraction.factor : modifiedUserInteraction.factor);
        double mpr;
        if (isModifiedScope())
            mpr = (modifiedPrivilegesRequired == PrivilegesRequired.NULL || modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED ? privilegesRequired.factorUnchanged : modifiedPrivilegesRequired.factorUnchanged);
        else
            mpr = (modifiedPrivilegesRequired == PrivilegesRequired.NULL || modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED ? privilegesRequired.factorChanged : modifiedPrivilegesRequired.factorChanged);
        return 8.22 * mav * mac * mpr * mui;
    }

    protected boolean isModifiedScope() {
        return ((modifiedScope != Scope.NULL && modifiedScope != Scope.NOT_DEFINED) && !modifiedScope.changed) || ((modifiedScope == Scope.NULL || modifiedScope == Scope.NOT_DEFINED) && !scope.changed);
    }

    @Override
    public double getOverallScore() {
        if (isAnyEnvironmentalDefined()) return getEnvironmentalScore();
        else if (isAnyTemporalDefined()) return getTemporalScore();
        return getBaseScore();
    }

    @Override
    public CvssSeverityRanges.SeverityRange getDefaultSeverityCategory() {
        return getSeverityCategory(CvssSeverityRanges.CVSS_3_SEVERITY_RANGES);
    }

    @Override
    public boolean isBaseFullyDefined() {
        return attackVector != AttackVector.NULL
                && attackComplexity != AttackComplexity.NULL
                && privilegesRequired != PrivilegesRequired.NULL
                && userInteraction != UserInteraction.NULL
                && scope != Scope.NULL
                && confidentialityImpact != CIAImpact.NULL
                && integrityImpact != CIAImpact.NULL
                && availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyBaseDefined() {
        return attackVector != AttackVector.NULL
                || attackComplexity != AttackComplexity.NULL
                || privilegesRequired != PrivilegesRequired.NULL
                || userInteraction != UserInteraction.NULL
                || scope != Scope.NULL
                || confidentialityImpact != CIAImpact.NULL
                || integrityImpact != CIAImpact.NULL
                || availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyTemporalDefined() {
        return exploitCodeMaturity != ExploitCodeMaturity.NULL
                || remediationLevel != RemediationLevel.NULL
                || reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isTemporalFullyDefined() {
        return exploitCodeMaturity != ExploitCodeMaturity.NULL
                && remediationLevel != RemediationLevel.NULL
                && reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isAnyEnvironmentalDefined() {
        return modifiedAttackVector != AttackVector.NULL
                || modifiedAttackComplexity != AttackComplexity.NULL
                || modifiedPrivilegesRequired != PrivilegesRequired.NULL
                || modifiedUserInteraction != UserInteraction.NULL
                || modifiedScope != Scope.NULL
                || modifiedConfidentialityImpact != CIAImpact.NULL
                || modifiedIntegrityImpact != CIAImpact.NULL
                || modifiedAvailabilityImpact != CIAImpact.NULL
                || confidentialityRequirement != CIARequirement.NULL
                || integrityRequirement != CIARequirement.NULL
                || availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public boolean isEnvironmentalFullyDefined() {
        return modifiedAttackVector != AttackVector.NULL
                && modifiedAttackComplexity != AttackComplexity.NULL
                && modifiedPrivilegesRequired != PrivilegesRequired.NULL
                && modifiedUserInteraction != UserInteraction.NULL
                && modifiedScope != Scope.NULL
                && modifiedConfidentialityImpact != CIAImpact.NULL
                && modifiedIntegrityImpact != CIAImpact.NULL
                && modifiedAvailabilityImpact != CIAImpact.NULL
                && confidentialityRequirement != CIARequirement.NULL
                && integrityRequirement != CIARequirement.NULL
                && availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public void clearTemporal() {
        exploitCodeMaturity = ExploitCodeMaturity.NULL;
        remediationLevel = RemediationLevel.NULL;
        reportConfidence = ReportConfidence.NULL;
    }

    @Override
    public void clearEnvironmental() {
        modifiedAttackVector = AttackVector.NULL;
        modifiedAttackComplexity = AttackComplexity.NULL;
        modifiedPrivilegesRequired = PrivilegesRequired.NULL;
        modifiedUserInteraction = UserInteraction.NULL;
        modifiedScope = Scope.NULL;
        modifiedConfidentialityImpact = CIAImpact.NULL;
        modifiedIntegrityImpact = CIAImpact.NULL;
        modifiedAvailabilityImpact = CIAImpact.NULL;
        confidentialityRequirement = CIARequirement.NULL;
        integrityRequirement = CIARequirement.NULL;
        availabilityRequirement = CIARequirement.NULL;
    }

    @Override
    public BakedCvssVectorScores bakeScores() {
        return BakedCvssVectorScores.fromNullableCvss(this);
    }

    @Override
    public Map<String, CvssVectorAttribute[]> getAttributes() {
        final Map<String, CvssVectorAttribute[]> attributes = new LinkedHashMap<>();

        attributes.put("AV", Cvss3.AttackVector.values());
        attributes.put("AC", Cvss3.AttackComplexity.values());
        attributes.put("PR", Cvss3.PrivilegesRequired.values());
        attributes.put("UI", Cvss3.UserInteraction.values());
        attributes.put("S", Cvss3.Scope.values());
        attributes.put("C", Cvss3.CIAImpact.values());
        attributes.put("I", Cvss3.CIAImpact.values());
        attributes.put("A", Cvss3.CIAImpact.values());
        attributes.put("E", Cvss3.ExploitCodeMaturity.values());
        attributes.put("RL", Cvss3.RemediationLevel.values());
        attributes.put("RC", Cvss3.ReportConfidence.values());
        attributes.put("MAV", Cvss3.AttackVector.values());
        attributes.put("MAC", Cvss3.AttackComplexity.values());
        attributes.put("MPR", Cvss3.PrivilegesRequired.values());
        attributes.put("MUI", Cvss3.UserInteraction.values());
        attributes.put("MS", Cvss3.Scope.values());
        attributes.put("MC", Cvss3.CIAImpact.values());
        attributes.put("MI", Cvss3.CIAImpact.values());
        attributes.put("MA", Cvss3.CIAImpact.values());
        attributes.put("CR", Cvss3.CIARequirement.values());
        attributes.put("IR", Cvss3.CIARequirement.values());
        attributes.put("AR", Cvss3.CIARequirement.values());

        return attributes;
    }

    public String getAttackComplexity() {
        return attackComplexity.identifier;
    }

    public String getAttackVector() {
        return attackVector.identifier;
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

    public String getModifiedAttackComplexity() {
        return modifiedAttackComplexity.identifier;
    }

    public String getExploitCodeMaturity() {
        return exploitCodeMaturity.identifier;
    }

    public String getModifiedAttackVector() {
        return modifiedAttackVector.identifier;
    }

    public String getModifiedAvailabilityImpact() {
        return modifiedAvailabilityImpact.identifier;
    }

    public String getModifiedConfidentialityImpact() {
        return modifiedConfidentialityImpact.identifier;
    }

    public String getModifiedIntegrityImpact() {
        return modifiedIntegrityImpact.identifier;
    }

    public String getPrivilegesRequired() {
        return privilegesRequired.identifier;
    }

    public String getAvailabilityRequirement() {
        return availabilityRequirement.identifier;
    }

    public String getConfidentialityRequirement() {
        return confidentialityRequirement.identifier;
    }

    public String getModifiedPrivilegesRequired() {
        return modifiedPrivilegesRequired.identifier;
    }

    public String getIntegrityRequirement() {
        return integrityRequirement.identifier;
    }

    public String getRemediationLevel() {
        return remediationLevel.identifier;
    }

    public String getReportConfidence() {
        return reportConfidence.identifier;
    }

    public String getModifiedScope() {
        return modifiedScope.identifier;
    }

    public String getScope() {
        return scope.identifier;
    }

    public String getUserInteraction() {
        return userInteraction.identifier;
    }

    public String getModifiedUserInteraction() {
        return modifiedUserInteraction.identifier;
    }

    public void setAttackComplexity(AttackComplexity attackComplexity) {
        this.attackComplexity = attackComplexity;
    }

    public void setAttackVector(AttackVector attackVector) {
        this.attackVector = attackVector;
    }

    public void setAvailabilityImpact(CIAImpact availabilityImpact) {
        this.availabilityImpact = availabilityImpact;
    }

    public void setAvailabilityRequirement(CIARequirement availabilityRequirement) {
        this.availabilityRequirement = availabilityRequirement;
    }

    public void setConfidentialityImpact(CIAImpact confidentialityImpact) {
        this.confidentialityImpact = confidentialityImpact;
    }

    public void setConfidentialityRequirement(CIARequirement confidentialityRequirement) {
        this.confidentialityRequirement = confidentialityRequirement;
    }

    public void setExploitCodeMaturity(ExploitCodeMaturity exploitCodeMaturity) {
        this.exploitCodeMaturity = exploitCodeMaturity;
    }

    public void setIntegrityImpact(CIAImpact integrityImpact) {
        this.integrityImpact = integrityImpact;
    }

    public void setIntegrityRequirement(CIARequirement integrityRequirement) {
        this.integrityRequirement = integrityRequirement;
    }

    public void setModifiedAttackComplexity(AttackComplexity modifiedAttackComplexity) {
        this.modifiedAttackComplexity = modifiedAttackComplexity;
    }

    public void setModifiedAttackVector(AttackVector modifiedAttackVector) {
        this.modifiedAttackVector = modifiedAttackVector;
    }

    public void setModifiedAvailabilityImpact(CIAImpact modifiedAvailabilityImpact) {
        this.modifiedAvailabilityImpact = modifiedAvailabilityImpact;
    }

    public void setModifiedConfidentialityImpact(CIAImpact modifiedConfidentialityImpact) {
        this.modifiedConfidentialityImpact = modifiedConfidentialityImpact;
    }

    public void setModifiedIntegrityImpact(CIAImpact modifiedIntegrityImpact) {
        this.modifiedIntegrityImpact = modifiedIntegrityImpact;
    }

    public void setModifiedPrivilegesRequired(PrivilegesRequired modifiedPrivilegesRequired) {
        this.modifiedPrivilegesRequired = modifiedPrivilegesRequired;
    }

    public void setModifiedScope(Scope modifiedScope) {
        this.modifiedScope = modifiedScope;
    }

    public void setModifiedUserInteraction(UserInteraction modifiedUserInteraction) {
        this.modifiedUserInteraction = modifiedUserInteraction;
    }

    public void setPrivilegesRequired(PrivilegesRequired privilegesRequired) {
        this.privilegesRequired = privilegesRequired;
    }

    public void setRemediationLevel(RemediationLevel remediationLevel) {
        this.remediationLevel = remediationLevel;
    }

    public void setReportConfidence(ReportConfidence reportConfidence) {
        this.reportConfidence = reportConfidence;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setUserInteraction(UserInteraction userInteraction) {
        this.userInteraction = userInteraction;
    }

    protected static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * Returns the smallest number, specified to 1 decimal place, that is equal to or higher than its input.
     *
     * @param value The value to round.
     * @return The rounded value.
     */
    public abstract double roundUp(double value);

    public static String getVersionName() {
        return "CVSS:3.1";
    }

    @Override
    public String getName() {
        return getVersionName();
    }

    /**
     * Depending on whether the environmental or temporal attributes are defined, one of the following two URLs is generated:
     * <pre>
     * 1. https://www.first.org/cvss/calculator/3.1#%s
     * 2. https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=%s&amp;version=3.1
     * </pre>
     * Where <code>%s</code> is replaced with the current vector string. If the vector is to be used with the second URL, and it contains the <code>CVSS:3.1/</code> prefix, this prefix is removed.
     * Examples:
     * <ul>
     *     <li>https://www.first.org/cvss/calculator/3.1#CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H</li>
     *     <li>https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H&amp;version=3.1</li>
     * </ul>
     */
    @Override
    public String getNistFirstWebEditorLink() {
        final String vectorString = this.toString(!isAnyEnvironmentalDefined());
        if (this.isAnyEnvironmentalDefined() || this.isAnyTemporalDefined()) {
            return String.format("https://www.first.org/cvss/calculator/3.1#%s", vectorString);
        } else {
            return String.format("https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=%s&version=3.1", vectorString.replace(this.getName() + "/", ""));
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean filterUndefinedProperties) {
        StringBuilder vector = new StringBuilder();
        vector.append(getName()).append('/');

        appendIfValid(vector, "AV", attackVector.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "AC", attackComplexity.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "PR", privilegesRequired.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "UI", userInteraction.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "S", scope.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "C", confidentialityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "I", integrityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "A", availabilityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "E", exploitCodeMaturity.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "RL", remediationLevel.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "RC", reportConfidence.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MAV", modifiedAttackVector.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MAC", modifiedAttackComplexity.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MPR", modifiedPrivilegesRequired.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MUI", modifiedUserInteraction.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MS", modifiedScope.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MC", modifiedConfidentialityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MI", modifiedIntegrityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "MA", modifiedAvailabilityImpact.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "CR", confidentialityRequirement.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "IR", integrityRequirement.shortIdentifier, filterUndefinedProperties);
        appendIfValid(vector, "AR", availabilityRequirement.shortIdentifier, filterUndefinedProperties);

        if (vector.length() > 0 && vector.charAt(vector.length() - 1) == '/') {
            vector.setLength(vector.length() - 1);
        }

        return vector.toString();
    }

    private void appendIfValid(StringBuilder builder, String prefix, String value, boolean filterUndefinedProperties) {
        if (value != null && (!filterUndefinedProperties || !value.equals("X"))) {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '/') {
                builder.append('/');
            }
            builder.append(prefix).append(':').append(value);
        }
    }

    @Override
    public int size() {
        int size = 0;

        if (attackVector != AttackVector.NOT_DEFINED && attackVector != AttackVector.NULL) size++;
        if (attackComplexity != AttackComplexity.NOT_DEFINED && attackComplexity != AttackComplexity.NULL) size++;
        if (privilegesRequired != PrivilegesRequired.NOT_DEFINED && privilegesRequired != PrivilegesRequired.NULL) size++;
        if (userInteraction != UserInteraction.NOT_DEFINED && userInteraction != UserInteraction.NULL) size++;
        if (scope != Scope.NOT_DEFINED && scope != Scope.NULL) size++;
        if (confidentialityImpact != CIAImpact.NOT_DEFINED && confidentialityImpact != CIAImpact.NULL) size++;
        if (integrityImpact != CIAImpact.NOT_DEFINED && integrityImpact != CIAImpact.NULL) size++;
        if (availabilityImpact != CIAImpact.NOT_DEFINED && availabilityImpact != CIAImpact.NULL) size++;
        if (exploitCodeMaturity != ExploitCodeMaturity.NOT_DEFINED && exploitCodeMaturity != ExploitCodeMaturity.NULL) size++;
        if (remediationLevel != RemediationLevel.NOT_DEFINED && remediationLevel != RemediationLevel.NULL) size++;
        if (reportConfidence != ReportConfidence.NOT_DEFINED && reportConfidence != ReportConfidence.NULL) size++;
        if (modifiedAttackVector != AttackVector.NOT_DEFINED && modifiedAttackVector != AttackVector.NULL) size++;
        if (modifiedAttackComplexity != AttackComplexity.NOT_DEFINED && modifiedAttackComplexity != AttackComplexity.NULL) size++;
        if (modifiedPrivilegesRequired != PrivilegesRequired.NOT_DEFINED && modifiedPrivilegesRequired != PrivilegesRequired.NULL) size++;
        if (modifiedUserInteraction != UserInteraction.NOT_DEFINED && modifiedUserInteraction != UserInteraction.NULL) size++;
        if (modifiedScope != Scope.NOT_DEFINED && modifiedScope != Scope.NULL) size++;
        if (modifiedConfidentialityImpact != CIAImpact.NOT_DEFINED && modifiedConfidentialityImpact != CIAImpact.NULL) size++;
        if (modifiedIntegrityImpact != CIAImpact.NOT_DEFINED && modifiedIntegrityImpact != CIAImpact.NULL) size++;
        if (modifiedAvailabilityImpact != CIAImpact.NOT_DEFINED && modifiedAvailabilityImpact != CIAImpact.NULL) size++;
        if (confidentialityRequirement != CIARequirement.NOT_DEFINED && confidentialityRequirement != CIARequirement.NULL) size++;
        if (integrityRequirement != CIARequirement.NOT_DEFINED && integrityRequirement != CIARequirement.NULL) size++;
        if (availabilityRequirement != CIARequirement.NOT_DEFINED && availabilityRequirement != CIARequirement.NULL) size++;

        return size;
    }

    public enum AttackVector implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        NETWORK("NETWORK", "N", 0.85),
        ADJACENT_NETWORK("ADJACENT_NETWORK", "A", 0.62),
        LOCAL("LOCAL", "L", 0.55),
        PHYSICAL("PHYSICAL", "P", 0.2);

        public final String identifier, shortIdentifier;
        public final double factor;

        AttackVector(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AttackVector fromString(String part) {
            return Cvss3Attribute.fromString(part, AttackVector.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum AttackComplexity implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        LOW("LOW", "L", 0.77),
        HIGH("HIGH", "H", 0.44);

        public final String identifier, shortIdentifier;
        public final double factor;

        AttackComplexity(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AttackComplexity fromString(String part) {
            return Cvss3Attribute.fromString(part, AttackComplexity.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum PrivilegesRequired implements Cvss3Attribute {
        NULL("NULL", "X", 0.0, 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0, 1.0),
        HIGH("HIGH", "H", 0.27, 0.5),
        LOW("LOW", "L", 0.62, 0.68),
        NONE("NONE", "N", 0.85, 0.85);

        public final String identifier, shortIdentifier;
        public final double factorUnchanged, factorChanged;

        PrivilegesRequired(String identifier, String shortIdentifier, double factorUnchanged, double factorChanged) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factorUnchanged = factorUnchanged;
            this.factorChanged = factorChanged;
        }

        public static PrivilegesRequired fromString(String part) {
            return Cvss3Attribute.fromString(part, PrivilegesRequired.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum UserInteraction implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        REQUIRED("REQUIRED", "R", 0.62),
        NONE("NONE", "N", 0.85);

        public final String identifier, shortIdentifier;
        public final double factor;

        UserInteraction(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static UserInteraction fromString(String part) {
            return Cvss3Attribute.fromString(part, UserInteraction.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum Scope implements Cvss3Attribute {
        NULL("NULL", "X", false),
        NOT_DEFINED("NOT_DEFINED", "X", false),
        CHANGED("CHANGED", "C", true),
        UNCHANGED("UNCHANGED", "U", false);

        public final String identifier, shortIdentifier;
        public final boolean changed;

        Scope(String identifier, String shortIdentifier, boolean changed) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.changed = changed;
        }

        public static Scope fromString(String part) {
            return Cvss3Attribute.fromString(part, Scope.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        public final static double SCOPE_CHANGED_FACTOR = 7.52;
        public final static double SCOPE_UNCHANGED_FACTOR = 6.42;
    }

    public enum CIAImpact implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        NONE("NONE", "N", 0.0),
        LOW("LOW", "L", 0.22),
        HIGH("HIGH", "H", 0.56);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIAImpact(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIAImpact fromString(String part) {
            return Cvss3Attribute.fromString(part, CIAImpact.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum ExploitCodeMaturity implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        UNPROVEN("UNPROVEN", "U", 0.91),
        PROOF_OF_CONCEPT("PROOF_OF_CONCEPT", "P", 0.94),
        FUNCTIONAL("FUNCTIONAL", "F", 0.97),
        HIGH("HIGH", "H", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        ExploitCodeMaturity(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static ExploitCodeMaturity fromString(String part) {
            return Cvss3Attribute.fromString(part, ExploitCodeMaturity.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum RemediationLevel implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        OFFICIAL_FIX("OFFICIAL_FIX", "O", 0.95),
        TEMPORARY_FIX("TEMPORARY_FIX", "T", 0.96),
        WORKAROUND("WORKAROUND", "W", 0.97),
        UNAVAILABLE("UNAVAILABLE", "U", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        RemediationLevel(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static RemediationLevel fromString(String part) {
            return Cvss3Attribute.fromString(part, RemediationLevel.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum ReportConfidence implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        UNKNOWN("UNKNOWN", "U", 0.92),
        REASONABLE("REASONABLE", "R", 0.96),
        CONFIRMED("CONFIRMED", "C", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        ReportConfidence(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static ReportConfidence fromString(String part) {
            return Cvss3Attribute.fromString(part, ReportConfidence.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    public enum CIARequirement implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        LOW("LOW", "L", 0.5),
        MEDIUM("MEDIUM", "M", 1.0),
        HIGH("HIGH", "H", 1.5);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIARequirement(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIARequirement fromString(String part) {
            return Cvss3Attribute.fromString(part, CIARequirement.class, NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    @Override
    protected <T extends CvssVector> T cloneInternal(T clone) {
        super.cloneInternal(clone);
        if (!(clone instanceof Cvss3)) return clone;
        final Cvss3 clone3 = (Cvss3) clone;

        clone3.attackVector = attackVector;
        clone3.attackComplexity = attackComplexity;
        clone3.privilegesRequired = privilegesRequired;
        clone3.userInteraction = userInteraction;
        clone3.scope = scope;
        clone3.confidentialityImpact = confidentialityImpact;
        clone3.integrityImpact = integrityImpact;
        clone3.availabilityImpact = availabilityImpact;
        clone3.exploitCodeMaturity = exploitCodeMaturity;
        clone3.remediationLevel = remediationLevel;
        clone3.reportConfidence = reportConfidence;
        clone3.modifiedAttackVector = modifiedAttackVector;
        clone3.modifiedAttackComplexity = modifiedAttackComplexity;
        clone3.modifiedPrivilegesRequired = modifiedPrivilegesRequired;
        clone3.modifiedUserInteraction = modifiedUserInteraction;
        clone3.modifiedScope = modifiedScope;
        clone3.modifiedConfidentialityImpact = modifiedConfidentialityImpact;
        clone3.modifiedIntegrityImpact = modifiedIntegrityImpact;
        clone3.modifiedAvailabilityImpact = modifiedAvailabilityImpact;
        clone3.confidentialityRequirement = confidentialityRequirement;
        clone3.integrityRequirement = integrityRequirement;
        clone3.availabilityRequirement = availabilityRequirement;

        return clone;
    }

    @Override
    public void completeVector() {
        cleanupTemporalVectorParts();
        cleanupEnvironmentalVectorParts();
    }

    protected void cleanupTemporalVectorParts() {
        if (isAnyTemporalDefined()) {
            exploitCodeMaturity = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED : exploitCodeMaturity;
            remediationLevel = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED : remediationLevel;
            reportConfidence = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED : reportConfidence;
        }

        if (isTemporalAllPartsNotDefined()) {
            clearTemporal();
        }
    }

    protected void cleanupEnvironmentalVectorParts() {
        if (isAnyEnvironmentalDefined()) {
            modifiedAttackVector = modifiedAttackVector == AttackVector.NULL ? AttackVector.NOT_DEFINED : modifiedAttackVector;
            modifiedAttackComplexity = modifiedAttackComplexity == AttackComplexity.NULL ? AttackComplexity.NOT_DEFINED : modifiedAttackComplexity;
            modifiedPrivilegesRequired = modifiedPrivilegesRequired == PrivilegesRequired.NULL ? PrivilegesRequired.NOT_DEFINED : modifiedPrivilegesRequired;
            modifiedUserInteraction = modifiedUserInteraction == UserInteraction.NULL ? UserInteraction.NOT_DEFINED : modifiedUserInteraction;
            modifiedScope = modifiedScope == Scope.NULL ? Scope.NOT_DEFINED : modifiedScope;
            modifiedConfidentialityImpact = modifiedConfidentialityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedConfidentialityImpact;
            modifiedIntegrityImpact = modifiedIntegrityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedIntegrityImpact;
            modifiedAvailabilityImpact = modifiedAvailabilityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedAvailabilityImpact;
            confidentialityRequirement = confidentialityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : confidentialityRequirement;
            integrityRequirement = integrityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : integrityRequirement;
            availabilityRequirement = availabilityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : availabilityRequirement;
        }

        if (isEnvironmentalAllPartsNotDefined()) {
            clearEnvironmental();
        }
    }

    protected boolean isTemporalAllPartsNotDefined() {
        return exploitCodeMaturity == ExploitCodeMaturity.NOT_DEFINED &&
                remediationLevel == RemediationLevel.NOT_DEFINED &&
                reportConfidence == ReportConfidence.NOT_DEFINED;
    }

    protected boolean isEnvironmentalAllPartsNotDefined() {
        return modifiedAttackVector == AttackVector.NOT_DEFINED &&
                modifiedAttackComplexity == AttackComplexity.NOT_DEFINED &&
                modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED &&
                modifiedUserInteraction == UserInteraction.NOT_DEFINED &&
                modifiedScope == Scope.NOT_DEFINED &&
                modifiedConfidentialityImpact == CIAImpact.NOT_DEFINED &&
                modifiedIntegrityImpact == CIAImpact.NOT_DEFINED &&
                modifiedAvailabilityImpact == CIAImpact.NOT_DEFINED &&
                confidentialityRequirement == CIARequirement.NOT_DEFINED &&
                integrityRequirement == CIARequirement.NOT_DEFINED &&
                availabilityRequirement == CIARequirement.NOT_DEFINED;
    }

    public abstract <T extends Cvss3> Optional<T> optionalParse(String vector);

    private final static Map<Class<?>, Map<String, Object>> ATTRIBUTE_CACHE = new HashMap<>();

    public interface Cvss3Attribute extends CvssVectorAttribute {
        default boolean isSet() {
            return !getIdentifier().equals("NOT_DEFINED") && !getIdentifier().equals("NULL");
        }

        static <T extends Cvss3Attribute> T fromString(String part, Class<T> clazz, T defaultValue) {
            final Map<String, Object> cache;
            if (ATTRIBUTE_CACHE.containsKey(clazz)) {
                cache = ATTRIBUTE_CACHE.get(clazz);
            } else {
                cache = new HashMap<>();
                ATTRIBUTE_CACHE.put(clazz, cache);
            }
            if (cache.containsKey(part)) {
                return (T) cache.get(part);
            } else {
                for (T value : clazz.getEnumConstants()) {
                    if (value.getShortIdentifier().equalsIgnoreCase(part)) {
                        cache.put(part, value);
                        return value;
                    } else if (value.getIdentifier().equalsIgnoreCase(part)) {
                        cache.put(part, value);
                        return value;
                    }
                }
                cache.put(part, defaultValue);
                return defaultValue;
            }
        }
    }

    public final static List<Cvss3Attribute> ATTRIBUTE_SEVERITY_ORDER = Arrays.asList(
            // 0.0
            AttackVector.NULL,
            AttackComplexity.NULL,
            PrivilegesRequired.NULL,
            UserInteraction.NULL,
            Scope.NULL,
            Scope.UNCHANGED,
            CIAImpact.NULL,
            CIAImpact.NONE,

            AttackVector.PHYSICAL,
            CIAImpact.LOW,
            PrivilegesRequired.HIGH,
            AttackComplexity.HIGH,
            CIARequirement.LOW,
            AttackVector.LOCAL,
            CIAImpact.HIGH,
            AttackVector.ADJACENT_NETWORK,
            UserInteraction.REQUIRED,
            PrivilegesRequired.LOW,
            AttackComplexity.LOW,
            AttackVector.NETWORK,
            PrivilegesRequired.NONE,
            UserInteraction.NONE,
            ExploitCodeMaturity.UNPROVEN,
            ReportConfidence.UNKNOWN,
            ExploitCodeMaturity.PROOF_OF_CONCEPT,
            RemediationLevel.OFFICIAL_FIX,
            RemediationLevel.TEMPORARY_FIX,
            ReportConfidence.REASONABLE,
            ExploitCodeMaturity.FUNCTIONAL,
            RemediationLevel.WORKAROUND,
            AttackVector.NOT_DEFINED,
            AttackComplexity.NOT_DEFINED,
            PrivilegesRequired.NOT_DEFINED,
            UserInteraction.NOT_DEFINED,
            Scope.NOT_DEFINED,
            Scope.CHANGED,
            CIAImpact.NOT_DEFINED,
            ExploitCodeMaturity.HIGH,
            ExploitCodeMaturity.NOT_DEFINED,
            RemediationLevel.UNAVAILABLE,
            RemediationLevel.NOT_DEFINED,
            ReportConfidence.CONFIRMED,
            ReportConfidence.NOT_DEFINED,

            ExploitCodeMaturity.NULL,
            RemediationLevel.NULL,
            ReportConfidence.NULL,

            CIARequirement.MEDIUM,
            CIARequirement.NULL,
            CIARequirement.NOT_DEFINED,
            CIARequirement.HIGH
    );
}
