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
package org.metaeffekt.core.security.cvss.v4_0;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.security.cvss.CvssScoreResult;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * <a href="https://www.first.org/cvss/v4-0/cvss-v40-specification.pdf">https://www.first.org/cvss/v4-0/cvss-v40-specification.pdf</a><br>
 * <a href="https://www.first.org/cvss/v4.0/specification-document">https://www.first.org/cvss/v4.0/specification-document</a>
 */
public class Cvss4_0 extends CvssVector {

    private final static Logger LOG = LoggerFactory.getLogger(Cvss4_0.class);

    public final static String[] VECTOR_PARTS = new String[]{
            "AV", "AC", "AT", "PR", "UI",
            "VC", "VI", "VA",
            "SC", "SI", "SA",
            "S", "AU", "R", "V", "RE", "U",
            "MAV", "MAC", "MAT", "MPR", "MUI",
            "MVC", "MVI", "MVA",
            "MSC", "MSI", "MSA",
            "CR", "IR", "AR",
            "E"
    };

    // Base Metrics: Exploitability Metrics
    private AttackVector attackVector = AttackVector.NOT_DEFINED; // AV; NETWORK
    private AttackComplexity attackComplexity = AttackComplexity.NOT_DEFINED; // AC; LOW
    private AttackRequirements attackRequirements = AttackRequirements.NOT_DEFINED; // AT; NONE
    private PrivilegesRequired privilegesRequired = PrivilegesRequired.NOT_DEFINED; // PR; NONE
    private UserInteraction userInteraction = UserInteraction.NOT_DEFINED; // UI; NONE

    // Base Metrics: Vulnerable System Impact Metrics
    private VulnerabilityCia vulnConfidentialityImpact = VulnerabilityCia.NOT_DEFINED; // VC; NONE
    private VulnerabilityCia vulnIntegrityImpact = VulnerabilityCia.NOT_DEFINED; // VI; NONE
    private VulnerabilityCia vulnAvailabilityImpact = VulnerabilityCia.NOT_DEFINED; // VA; NONE

    // Base Metrics: Subsequent System Impact Metrics
    private SubsequentCia subConfidentialityImpact = SubsequentCia.NOT_DEFINED; // SC; NONE
    private SubsequentCia subIntegrityImpact = SubsequentCia.NOT_DEFINED; // SI; NONE
    private SubsequentCia subAvailabilityImpact = SubsequentCia.NOT_DEFINED; // SA; NONE

    // Supplemental Metrics
    private Safety safety = Safety.NOT_DEFINED; // S
    private Automatable automatable = Automatable.NOT_DEFINED; // AU
    private Recovery recovery = Recovery.NOT_DEFINED; // R
    private ValueDensity valueDensity = ValueDensity.NOT_DEFINED; // V
    private VulnerabilityResponseEffort vulnerabilityResponseEffort = VulnerabilityResponseEffort.NOT_DEFINED; // RE
    private ProviderUrgency providerUrgency = ProviderUrgency.NOT_DEFINED; // U

    // Environmental (Modified Base Metrics): Exploitability Metrics
    private ModifiedAttackVector modifiedAttackVector = ModifiedAttackVector.NOT_DEFINED; // MAV
    private ModifiedAttackComplexity modifiedAttackComplexity = ModifiedAttackComplexity.NOT_DEFINED; // MAC
    private ModifiedAttackRequirements modifiedAttackRequirements = ModifiedAttackRequirements.NOT_DEFINED; // MAT
    private ModifiedPrivilegesRequired modifiedPrivilegesRequired = ModifiedPrivilegesRequired.NOT_DEFINED; // MPR
    private ModifiedUserInteraction modifiedUserInteraction = ModifiedUserInteraction.NOT_DEFINED; // MUI

    // Environmental (Modified Base Metrics): Vulnerable System Impact Metrics
    private ModifiedVulnerabilityCia modifiedVulnConfidentialityImpact = ModifiedVulnerabilityCia.NOT_DEFINED; // MVC
    private ModifiedVulnerabilityCia modifiedVulnIntegrityImpact = ModifiedVulnerabilityCia.NOT_DEFINED; // MVI
    private ModifiedVulnerabilityCia modifiedVulnAvailabilityImpact = ModifiedVulnerabilityCia.NOT_DEFINED; // MVA

    // Environmental (Modified Base Metrics): Subsequent System Impact Metrics
    private ModifiedSubsequentConfidentiality modifiedSubConfidentialityImpact = ModifiedSubsequentConfidentiality.NOT_DEFINED; // MSC
    private ModifiedSubsequentIntegrityAvailability modifiedSubIntegrityImpact = ModifiedSubsequentIntegrityAvailability.NOT_DEFINED; // MSI
    private ModifiedSubsequentIntegrityAvailability modifiedSubAvailabilityImpact = ModifiedSubsequentIntegrityAvailability.NOT_DEFINED; // MSA

    // Environmental (Security Requirements)
    private RequirementsCia confidentialityRequirement = RequirementsCia.NOT_DEFINED; // CR
    private RequirementsCia integrityRequirement = RequirementsCia.NOT_DEFINED; // IR
    private RequirementsCia availabilityRequirement = RequirementsCia.NOT_DEFINED; // AR

    // Threat Metrics
    private ExploitMaturity exploitMaturity = ExploitMaturity.NOT_DEFINED; // E

    public Cvss4_0(String vector) {
        applyVector(vector);
    }

    public Cvss4_0() {
    }

    public Cvss4_0MacroVector getMacroVector() {
        return new Cvss4_0MacroVector(this);
    }

    @Override
    public boolean applyVectorArgument(String identifier, String value) {
        switch (identifier) {
            // Base Metrics: Exploitability Metrics
            case "AV":
                attackVector = AttackVector.fromString(value);
                break;
            case "AC":
                attackComplexity = AttackComplexity.fromString(value);
                break;
            case "AT":
                attackRequirements = AttackRequirements.fromString(value);
                break;
            case "PR":
                privilegesRequired = PrivilegesRequired.fromString(value);
                break;
            case "UI":
                userInteraction = UserInteraction.fromString(value);
                break;

            // Base Metrics: Vulnerable System Impact Metrics
            case "VC":
                vulnConfidentialityImpact = VulnerabilityCia.fromString(value);
                break;
            case "VI":
                vulnIntegrityImpact = VulnerabilityCia.fromString(value);
                break;
            case "VA":
                vulnAvailabilityImpact = VulnerabilityCia.fromString(value);
                break;

            // Base Metrics: Subsequent System Impact Metrics
            case "SC":
                subConfidentialityImpact = SubsequentCia.fromString(value);
                break;
            case "SI":
                subIntegrityImpact = SubsequentCia.fromString(value);
                break;
            case "SA":
                subAvailabilityImpact = SubsequentCia.fromString(value);
                break;

            // Supplemental Metrics
            case "S":
                safety = Safety.fromString(value);
                break;
            case "AU":
                automatable = Automatable.fromString(value);
                break;
            case "R":
                recovery = Recovery.fromString(value);
                break;
            case "V":
                valueDensity = ValueDensity.fromString(value);
                break;
            case "RE":
                vulnerabilityResponseEffort = VulnerabilityResponseEffort.fromString(value);
                break;
            case "U":
                providerUrgency = ProviderUrgency.fromString(value);
                break;

            // Environmental (Modified Base Metrics): Exploitability Metrics
            case "MAV":
                modifiedAttackVector = ModifiedAttackVector.fromString(value);
                break;
            case "MAC":
                modifiedAttackComplexity = ModifiedAttackComplexity.fromString(value);
                break;
            case "MAT":
                modifiedAttackRequirements = ModifiedAttackRequirements.fromString(value);
                break;
            case "MPR":
                modifiedPrivilegesRequired = ModifiedPrivilegesRequired.fromString(value);
                break;
            case "MUI":
                modifiedUserInteraction = ModifiedUserInteraction.fromString(value);
                break;

            // Environmental (Modified Base Metrics): Vulnerable System Impact Metrics
            case "MVC":
                modifiedVulnConfidentialityImpact = ModifiedVulnerabilityCia.fromString(value);
                break;
            case "MVI":
                modifiedVulnIntegrityImpact = ModifiedVulnerabilityCia.fromString(value);
                break;
            case "MVA":
                modifiedVulnAvailabilityImpact = ModifiedVulnerabilityCia.fromString(value);
                break;

            // Environmental (Modified Base Metrics): Subsequent System Impact Metrics
            case "MSC":
                modifiedSubConfidentialityImpact = ModifiedSubsequentConfidentiality.fromString(value);
                break;
            case "MSI":
                modifiedSubIntegrityImpact = ModifiedSubsequentIntegrityAvailability.fromString(value);
                break;
            case "MSA":
                modifiedSubAvailabilityImpact = ModifiedSubsequentIntegrityAvailability.fromString(value);
                break;

            // Environmental (Security Requirements)
            case "CR":
                confidentialityRequirement = RequirementsCia.fromString(value);
                break;
            case "IR":
                integrityRequirement = RequirementsCia.fromString(value);
                break;
            case "AR":
                availabilityRequirement = RequirementsCia.fromString(value);
                break;

            // Threat Metrics
            case "E":
                exploitMaturity = ExploitMaturity.fromString(value);
                break;

            default:
                LOG.debug("Unknown CVSS 4.0 vector argument: [{}] [{}]", identifier, value);
                return false;
        }

        return true;
    }

    protected Cvss4_0Attribute getVectorArgument(String identifier) {
        switch (identifier) {
            // Base Metrics: Exploitability Metrics
            case "AV":
                return attackVector;
            case "AC":
                return attackComplexity;
            case "AT":
                return attackRequirements;
            case "PR":
                return privilegesRequired;
            case "UI":
                return userInteraction;

            // Base Metrics: Vulnerable System Impact Metrics
            case "VC":
                return vulnConfidentialityImpact;
            case "VI":
                return vulnIntegrityImpact;
            case "VA":
                return vulnAvailabilityImpact;

            // Base Metrics: Subsequent System Impact Metrics
            case "SC":
                return subConfidentialityImpact;
            case "SI":
                return subIntegrityImpact;
            case "SA":
                return subAvailabilityImpact;

            // Supplemental Metrics
            case "S":
                return safety;
            case "AU":
                return automatable;
            case "R":
                return recovery;
            case "V":
                return valueDensity;
            case "RE":
                return vulnerabilityResponseEffort;
            case "U":
                return providerUrgency;

            // Environmental (Modified Base Metrics): Exploitability Metrics
            case "MAV":
                return modifiedAttackVector;
            case "MAC":
                return modifiedAttackComplexity;
            case "MAT":
                return modifiedAttackRequirements;
            case "MPR":
                return modifiedPrivilegesRequired;
            case "MUI":
                return modifiedUserInteraction;

            // Environmental (Modified Base Metrics): Vulnerable System Impact Metrics
            case "MVC":
                return modifiedVulnConfidentialityImpact;
            case "MVI":
                return modifiedVulnIntegrityImpact;
            case "MVA":
                return modifiedVulnAvailabilityImpact;

            // Environmental (Modified Base Metrics): Subsequent System Impact Metrics
            case "MSC":
                return modifiedSubConfidentialityImpact;
            case "MSI":
                return modifiedSubIntegrityImpact;
            case "MSA":
                return modifiedSubAvailabilityImpact;

            // Environmental (Security Requirements)
            case "CR":
                return confidentialityRequirement;
            case "IR":
                return integrityRequirement;
            case "AR":
                return availabilityRequirement;

            // Threat Metrics
            case "E":
                return exploitMaturity;

            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cvss4_0)) return false;
        Cvss4_0 cvss4_0 = (Cvss4_0) o;
        return attackVector == cvss4_0.attackVector &&
                attackComplexity == cvss4_0.attackComplexity &&
                attackRequirements == cvss4_0.attackRequirements &&
                privilegesRequired == cvss4_0.privilegesRequired &&
                userInteraction == cvss4_0.userInteraction &&
                vulnConfidentialityImpact == cvss4_0.vulnConfidentialityImpact &&
                vulnIntegrityImpact == cvss4_0.vulnIntegrityImpact &&
                vulnAvailabilityImpact == cvss4_0.vulnAvailabilityImpact &&
                subConfidentialityImpact == cvss4_0.subConfidentialityImpact &&
                subIntegrityImpact == cvss4_0.subIntegrityImpact &&
                subAvailabilityImpact == cvss4_0.subAvailabilityImpact &&
                modifiedAttackVector == cvss4_0.modifiedAttackVector &&
                modifiedAttackComplexity == cvss4_0.modifiedAttackComplexity &&
                modifiedAttackRequirements == cvss4_0.modifiedAttackRequirements &&
                modifiedPrivilegesRequired == cvss4_0.modifiedPrivilegesRequired &&
                modifiedUserInteraction == cvss4_0.modifiedUserInteraction &&
                modifiedVulnConfidentialityImpact == cvss4_0.modifiedVulnConfidentialityImpact &&
                modifiedVulnIntegrityImpact == cvss4_0.modifiedVulnIntegrityImpact &&
                modifiedVulnAvailabilityImpact == cvss4_0.modifiedVulnAvailabilityImpact &&
                modifiedSubConfidentialityImpact == cvss4_0.modifiedSubConfidentialityImpact &&
                modifiedSubIntegrityImpact == cvss4_0.modifiedSubIntegrityImpact &&
                modifiedSubAvailabilityImpact == cvss4_0.modifiedSubAvailabilityImpact &&
                confidentialityRequirement == cvss4_0.confidentialityRequirement &&
                integrityRequirement == cvss4_0.integrityRequirement &&
                availabilityRequirement == cvss4_0.availabilityRequirement &&
                exploitMaturity == cvss4_0.exploitMaturity &&
                safety == cvss4_0.safety &&
                automatable == cvss4_0.automatable &&
                recovery == cvss4_0.recovery &&
                valueDensity == cvss4_0.valueDensity &&
                vulnerabilityResponseEffort == cvss4_0.vulnerabilityResponseEffort &&
                providerUrgency == cvss4_0.providerUrgency;
    }

    public void fillLeastSevereBaseMetrics() {
        this.attackVector = AttackVector.NETWORK;
        this.attackComplexity = AttackComplexity.LOW;
        this.attackRequirements = AttackRequirements.NONE;
        this.privilegesRequired = PrivilegesRequired.NONE;
        this.userInteraction = UserInteraction.NONE;
        this.vulnConfidentialityImpact = VulnerabilityCia.NONE;
        this.vulnIntegrityImpact = VulnerabilityCia.NONE;
        this.vulnAvailabilityImpact = VulnerabilityCia.NONE;
        this.subConfidentialityImpact = SubsequentCia.NONE;
        this.subIntegrityImpact = SubsequentCia.NONE;
        this.subAvailabilityImpact = SubsequentCia.NONE;
    }

    @Override
    public boolean isBaseDefined() {
        return attackVector != AttackVector.NOT_DEFINED
                && attackComplexity != AttackComplexity.NOT_DEFINED
                && attackRequirements != AttackRequirements.NOT_DEFINED
                && privilegesRequired != PrivilegesRequired.NOT_DEFINED
                && userInteraction != UserInteraction.NOT_DEFINED
                && vulnConfidentialityImpact != VulnerabilityCia.NOT_DEFINED
                && vulnIntegrityImpact != VulnerabilityCia.NOT_DEFINED
                && vulnAvailabilityImpact != VulnerabilityCia.NOT_DEFINED
                && subConfidentialityImpact != SubsequentCia.NOT_DEFINED
                && subIntegrityImpact != SubsequentCia.NOT_DEFINED
                && subAvailabilityImpact != SubsequentCia.NOT_DEFINED;
    }

    @Override
    public boolean isAnyBaseDefined() {
        return attackVector != AttackVector.NOT_DEFINED
                || attackComplexity != AttackComplexity.NOT_DEFINED
                || attackRequirements != AttackRequirements.NOT_DEFINED
                || privilegesRequired != PrivilegesRequired.NOT_DEFINED
                || userInteraction != UserInteraction.NOT_DEFINED
                || vulnConfidentialityImpact != VulnerabilityCia.NOT_DEFINED
                || vulnIntegrityImpact != VulnerabilityCia.NOT_DEFINED
                || vulnAvailabilityImpact != VulnerabilityCia.NOT_DEFINED
                || subConfidentialityImpact != SubsequentCia.NOT_DEFINED
                || subIntegrityImpact != SubsequentCia.NOT_DEFINED
                || subAvailabilityImpact != SubsequentCia.NOT_DEFINED;
    }

    public boolean isAnyEnvironmentalDefined() {
        return modifiedAttackVector != ModifiedAttackVector.NOT_DEFINED
                || modifiedAttackComplexity != ModifiedAttackComplexity.NOT_DEFINED
                || modifiedAttackRequirements != ModifiedAttackRequirements.NOT_DEFINED
                || modifiedPrivilegesRequired != ModifiedPrivilegesRequired.NOT_DEFINED
                || modifiedUserInteraction != ModifiedUserInteraction.NOT_DEFINED
                || modifiedVulnConfidentialityImpact != ModifiedVulnerabilityCia.NOT_DEFINED
                || modifiedVulnIntegrityImpact != ModifiedVulnerabilityCia.NOT_DEFINED
                || modifiedVulnAvailabilityImpact != ModifiedVulnerabilityCia.NOT_DEFINED
                || modifiedSubConfidentialityImpact != ModifiedSubsequentConfidentiality.NOT_DEFINED
                || modifiedSubIntegrityImpact != ModifiedSubsequentIntegrityAvailability.NOT_DEFINED
                || modifiedSubAvailabilityImpact != ModifiedSubsequentIntegrityAvailability.NOT_DEFINED
                || confidentialityRequirement != RequirementsCia.NOT_DEFINED
                || integrityRequirement != RequirementsCia.NOT_DEFINED
                || availabilityRequirement != RequirementsCia.NOT_DEFINED;
    }

    public boolean isAnyThreatDefined() {
        return exploitMaturity != ExploitMaturity.NOT_DEFINED;
    }

    @Override
    public double getOverallScore() {
        return getBaseScore();
    }

    @Override
    public double getBaseScore() {
        // check if base is undefined
        if (!isBaseDefined()) {
            return 0.0;
        }

        // check for no impact on system
        if (Stream.of("VC", "VI", "VA", "SC", "SI", "SA")
                .map(attr -> Cvss4_0MacroVector.getComparisonMetric(this, attr))
                .allMatch(value -> value.getShortIdentifier().equals("N"))) {
            return 0.0;
        }

        final Cvss4_0MacroVector thisMacroVector = this.getMacroVector();
        final double thisMacroVectorScore = thisMacroVector.getLookupTableScore();
        final EqOperations[] eqOperations = EqOperations.getEqImplementations();

        if (!(thisMacroVector.getEq3().getLevel() + thisMacroVector.getEq6().getLevel()).equalsIgnoreCase(thisMacroVector.getJointEq3AndEq6().getLevel())) {
            LOG.warn("CVSS 4.0: Joint Eq3 and Eq6 level [{}] does not match Eq3 [{}] and Eq6 [{}]", thisMacroVector.getJointEq3AndEq6().getLevel(), thisMacroVector.getEq3().getLevel(), thisMacroVector.getEq6().getLevel());
        }

        final String[][] allHighestSeverityVectors = Arrays.stream(eqOperations)
                .map(eqOp -> eqOp.getHighestSeverityVectors(thisMacroVector))
                .toArray(String[][]::new);

        final List<Cvss4_0> highestSeverityVectorCombinations = generateCvssPermutations(allHighestSeverityVectors[0], allHighestSeverityVectors[1], allHighestSeverityVectors[2], allHighestSeverityVectors[3], allHighestSeverityVectors[4]);

        if (highestSeverityVectorCombinations.isEmpty()) {
            LOG.warn("No max vectors found for {}", thisMacroVector);
            return 0.0;
        }

        final Map<String, Integer> highestSeverityHammingDistances = Cvss4_0.calculateHammingDistancesByComparingToHighestSeverityVectors(this, highestSeverityVectorCombinations);

        final Average meanScoreAdjustment = new Average();
        for (EqOperations eqOps : eqOperations) {
            // increases the number at the current EQ by 1, leading to a less severe EQ in this factor
            // EQ2: 201000 --> 211000
            // for EQ3/6, up to two vectors are returned due to them not being independent of each other
            final Cvss4_0MacroVector[] nextLessSevereMacroVector = eqOps.deriveNextLowerMacro(thisMacroVector);
            final double nextLowerMacroScore = eqOps.lookupScoresForNextLowerMacro(nextLessSevereMacroVector);
            // available_distance
            // if the next lower macro score does not exist, the result is NaN
            final double availableSeverityReduction = thisMacroVectorScore - nextLowerMacroScore;

            // = max hamming distance in this EQ level
            final int macroVectorDepth = eqOps.lookupMacroVectorDepth(thisMacroVector);
            // = how much the current macro vector differs from the highest severity vector in this EQ level
            final int hammingDistanceFromThisToHighestSeverity = Arrays.stream(eqOps.getRelevantAttributes()).mapToInt(highestSeverityHammingDistances::get).sum();

            // calculate the 'normalized' hamming distance
            // by converting the hamming distance from this vector to the next higher severity from the [hamming distance] scale into to the [score] scale
            if (!Double.isNaN(availableSeverityReduction) && macroVectorDepth != 0.0) {
                final double percentageToNextHammingDistance = hammingDistanceFromThisToHighestSeverity / (double) macroVectorDepth;
                final double normalizedHamming = percentageToNextHammingDistance * availableSeverityReduction;
                meanScoreAdjustment.add(normalizedHamming);
            }
        }

        // calculate mean distance based on normalized Hamming distances
        // and adjust the original macro vector score by the mean distance
        final double adjustedOriginalMacroVectorScore = thisMacroVectorScore - meanScoreAdjustment.get(0.0);

        if (adjustedOriginalMacroVectorScore < 0) {
            return 0.0;
        } else if (adjustedOriginalMacroVectorScore > 10) {
            return 10.0;
        } else {
            return Double.parseDouble(String.format(Locale.US, "%.1f", adjustedOriginalMacroVectorScore));
        }
    }

    private static class Average {
        private double sum = 0;
        private int count = 0;

        public void add(double value) {
            sum += value;
            count++;
        }

        public Double get(Double defaultValue) {
            if (count == 0) {
                return defaultValue;
            } else {
                return sum / count;
            }
        }
    }

    /**
     * Calculates the Hamming distances between a given CVSS vector and a list of highest severity vectors.
     * The method iterates through the list of highest severity vectors and computes the Hamming distance
     * for each attribute between the comparison vector and the highest severity vector.
     * <p>
     * The method stops iterating when it finds the first highest severity vector that has a non-negative
     * Hamming distance for all attributes when compared to the given CVSS vector. This ensures that the
     * highest severity vector is at least as severe as the comparison vector in every metric.
     * </p>
     *
     * @param comparisonVector       The CVSS vector to compare.
     * @param highestSeverityVectors The list of highest severity vectors to compare against.
     * @return A map containing the Hamming distances for each attribute. If no suitable highest severity vector
     * is found, the map will be empty. A warning will be logged in this case.
     */
    protected static Map<String, Integer> calculateHammingDistancesByComparingToHighestSeverityVectors(Cvss4_0 comparisonVector, List<Cvss4_0> highestSeverityVectors) {
        final Map<String, Integer> hammingDistances = new LinkedHashMap<>();

        for (Cvss4_0 maxVector : highestSeverityVectors) {
            hammingDistances.put("AV", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "AV"), maxVector.getAttackVector()));
            hammingDistances.put("PR", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "PR"), maxVector.getPrivilegesRequired()));
            hammingDistances.put("UI", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "UI"), maxVector.getUserInteraction()));
            hammingDistances.put("AC", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "AC"), maxVector.getAttackComplexity()));
            hammingDistances.put("AT", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "AT"), maxVector.getAttackRequirements()));
            hammingDistances.put("VC", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "VC"), maxVector.getVulnConfidentialityImpact()));
            hammingDistances.put("VI", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "VI"), maxVector.getVulnIntegrityImpact()));
            hammingDistances.put("VA", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "VA"), maxVector.getVulnAvailabilityImpact()));
            // SI and SA are handled below
            hammingDistances.put("SC", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "SC"), maxVector.getSubConfidentialityImpact()));
            hammingDistances.put("CR", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "CR"), maxVector.getConfidentialityRequirement()));
            hammingDistances.put("IR", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "IR"), maxVector.getIntegrityRequirement()));
            hammingDistances.put("AR", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, "AR"), maxVector.getAvailabilityRequirement()));


            // Handling different conditions for MSI and MSA depending on if SAFETY is selected on their modified counterparts
            final boolean isModifiedSubIntegrityImpactSafety = ModifiedSubsequentIntegrityAvailability.SAFETY == comparisonVector.getModifiedSubIntegrityImpact();
            final boolean isModifiedSubAvailabilityImpactSafety = ModifiedSubsequentIntegrityAvailability.SAFETY == comparisonVector.getModifiedSubAvailabilityImpact();

            final String subIntegrityImpactKey = isModifiedSubIntegrityImpactSafety ? "MSI" : "SI";
            final String subAvailabilityImpactKey = isModifiedSubAvailabilityImpactSafety ? "MSA" : "SA";

            hammingDistances.put("SI", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, subIntegrityImpactKey), maxVector.getSubIntegrityImpact()));
            hammingDistances.put("SA", hammingDistance(Cvss4_0MacroVector.getComparisonMetric(comparisonVector, subAvailabilityImpactKey), maxVector.getSubAvailabilityImpact()));


            // Check if any Hamming distance is negative
            final boolean anyNegative = hammingDistances.values().stream().anyMatch(val -> val < 0);

            if (!anyNegative) {
                // found the first vector that is >= the comparison vector in every metric
                break;
            } else {
                hammingDistances.clear();
            }
        }

        if (hammingDistances.isEmpty()) {
            LOG.warn("No hamming distances found for [{}]: {}", comparisonVector.getMacroVector(), comparisonVector);
            LOG.info("Max vectors:");
            for (Cvss4_0 maxVector : highestSeverityVectors) {
                LOG.info(" {}", toString(new Cvss4_0[]{maxVector}, "AV", "PR", "UI", "AC", "AT", "VC", "VI", "VA", "CR", "IR", "AR", "SC", "SI", "SA", "E"));
            }
        }

        return hammingDistances;
    }

    private List<Cvss4_0> generateCvssPermutations(String[] eq1_max_vectors, String[] eq2_max_vectors, String[] eq3_eq6_max_vectors, String[] eq4_max_vectors, String[] eq5_max_vectors) {
        final List<Cvss4_0> HighestSeverityVectors = new ArrayList<>();

        for (String eq1Max : eq1_max_vectors) {
            for (String eq2Max : eq2_max_vectors) {
                for (String eq3Eq6Max : eq3_eq6_max_vectors) {
                    for (String eq4Max : eq4_max_vectors) {
                        for (String eq5Max : eq5_max_vectors) {
                            final String combinedVector = eq1Max + "/" + eq2Max + "/" + eq3Eq6Max + "/" + eq4Max + "/" + eq5Max;
                            HighestSeverityVectors.add(new Cvss4_0(combinedVector));
                        }
                    }
                }
            }
        }
        return HighestSeverityVectors;
    }

    public static int hammingDistance(Cvss4_0Attribute part1, Cvss4_0Attribute part2) {
        final Cvss4_0Attribute worseCaseAttribute1;
        final Cvss4_0Attribute worseCaseAttribute2;

        if ("X".equals(part1.getShortIdentifier())) { // if the values are not set, assume the worst case
            worseCaseAttribute1 = part1.getWorseCase();
        } else {
            worseCaseAttribute1 = part1;
        }
        if ("X".equals(part2.getShortIdentifier())) {
            worseCaseAttribute2 = part2.getWorseCase();
        } else {
            worseCaseAttribute2 = part2;
        }

        final Class<?> clazz1 = worseCaseAttribute1.getClass();
        final Class<?> clazz2 = worseCaseAttribute2.getClass();

        if (!clazz1.isEnum() || !clazz2.isEnum()) {
            LOG.warn("Cannot compute hamming distance for [{}] and [{}], assuming distance is 0", worseCaseAttribute1, worseCaseAttribute2);
            return 0;
        }

        final Cvss4_0Attribute effectiveAttribute1;
        final Cvss4_0Attribute effectiveAttribute2;

        final Enum<?> worseCaseEnum1 = (Enum<?>) worseCaseAttribute1;
        final Enum<?> worseCaseEnum2 = (Enum<?>) worseCaseAttribute2;

        if (!clazz1.equals(clazz2)) {
            // the classes not being the same may happen if the user selected a modified version of the attribute.
            // in this case, the Cvss4_0MacroVector#getComparisonMetric method will return the modified version of the
            // attribute, which we have to use here for both attributes.
            // so, attempt to find an identical enum value in the modified enum.
            final boolean isModifiedAttribute1 = worseCaseAttribute1.getClass().getSimpleName().startsWith("Modified");
            final boolean isModifiedAttribute2 = worseCaseAttribute2.getClass().getSimpleName().startsWith("Modified");

            // use the enum arrays to determine the value in the other enum
            // unmodified --> modified enum based on the short identifier
            if (isModifiedAttribute1 && !isModifiedAttribute2) {
                final Cvss4_0Attribute[] enum1Values = (Cvss4_0Attribute[]) worseCaseEnum1.getClass().getEnumConstants();
                effectiveAttribute1 = worseCaseAttribute1;
                effectiveAttribute2 = Arrays.stream(enum1Values)
                        .filter(v -> v.getShortIdentifier().equals(worseCaseAttribute2.getShortIdentifier()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Cannot find modified enum value for " + worseCaseAttribute2));

            } else if (!isModifiedAttribute1 && isModifiedAttribute2) {
                final Cvss4_0Attribute[] enum2Values = (Cvss4_0Attribute[]) worseCaseEnum2.getClass().getEnumConstants();
                effectiveAttribute1 = Arrays.stream(enum2Values)
                        .filter(v -> v.getShortIdentifier().equals(worseCaseAttribute1.getShortIdentifier()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Cannot find modified enum value for " + worseCaseAttribute1));
                effectiveAttribute2 = worseCaseAttribute2;

            } else {
                LOG.warn("Cannot compute hamming distance for [{}] and [{}], assuming distance is 0", worseCaseAttribute1, worseCaseAttribute2);
                return 0;
            }

        } else {
            effectiveAttribute1 = worseCaseAttribute1;
            effectiveAttribute2 = worseCaseAttribute2;
        }

        final Enum<?> effectiveEnum1 = (Enum<?>) effectiveAttribute1;
        final Enum<?> effectiveEnum2 = (Enum<?>) effectiveAttribute2;

        final int ordinal1 = effectiveEnum1.ordinal();
        final int ordinal2 = effectiveEnum2.ordinal();

        return ordinal1 - ordinal2;
    }

    public static int hammingDistance(Cvss4_0 v1, Cvss4_0 v2) {
        return Arrays.stream(VECTOR_PARTS)
                .map(p -> hammingDistance(v1.getVectorArgument(p), v2.getVectorArgument(p)))
                .reduce(0, Integer::sum);
    }

    public int hammingDistance(Cvss4_0 other) {
        return hammingDistance(this, other);
    }

    @Override
    protected void completeVector() {
        // no need to normalize the vector, CVSS 4 uses a different way of computing the score that is
        // not disturbed by missing values.
        // also, it is intentional that the base attributes are not filled here, even though an empty vector is invalid,
        // since this is the behaviour of the other CVSS classes and is required for most status-based operations.
    }

    @Override
    public Cvss4_0 clone() {
        return new Cvss4_0(this.toString());
    }

    public static String getVersionName() {
        return "CVSS:4.0";
    }

    @Override
    public String getName() {
        return getVersionName();
    }

    /**
     * Creates a web editor link for calculating the CVSS v4 score using the following pattern:
     * <pre>https://www.first.org/cvss/calculator/4.0#%s</pre>
     * Where <code>%s</code> is replaced with the current vector string.
     * Examples:
     * <ul>
     *     <li>https://www.first.org/cvss/calculator/4.0#CVSS:4.0/AV:N/AC:M/PR:H/UI:N/S:C/C:H/I:H/A:H</li>
     *     <li>https://www.first.org/cvss/calculator/4.0#CVSS:4.0/AV:L/AC:H/AT:N/PR:H/UI:P/VC:L/VI:N/VA:H/SC:N/SI:N/SA:N/S:N/V:C/RE:L/MAT:N/MPR:L/MVI:L/MVA:L/MSI:L/CR:H/IR:M/E:P</li>
     * </ul>
     */
    @Override
    public String getWebEditorLink() {
        return String.format("https://www.first.org/cvss/calculator/4.0#%s", this);
    }

    public String getNomenclature() {
        final boolean baseDefined = isAnyBaseDefined();
        final boolean environmentalDefined = isAnyEnvironmentalDefined();
        final boolean threatDefined = isAnyThreatDefined();

        return "CVSS-" +
                (baseDefined ? "B" : "") +
                (threatDefined ? "T" : "") +
                (environmentalDefined ? "E" : "");
    }

    @Override
    public String toString() {
        final StringBuilder vector = new StringBuilder();
        vector.append(getName()).append("/");

        // Base Metrics: Exploitability Metrics
        appendIfNotDefault(vector, "AV", attackVector, AttackVector.NOT_DEFINED);
        appendIfNotDefault(vector, "AC", attackComplexity, AttackComplexity.NOT_DEFINED);
        appendIfNotDefault(vector, "AT", attackRequirements, AttackRequirements.NOT_DEFINED);
        appendIfNotDefault(vector, "PR", privilegesRequired, PrivilegesRequired.NOT_DEFINED);
        appendIfNotDefault(vector, "UI", userInteraction, UserInteraction.NOT_DEFINED);

        // Base Metrics: Vulnerable System Impact Metrics
        appendIfNotDefault(vector, "VC", vulnConfidentialityImpact, VulnerabilityCia.NOT_DEFINED);
        appendIfNotDefault(vector, "VI", vulnIntegrityImpact, VulnerabilityCia.NOT_DEFINED);
        appendIfNotDefault(vector, "VA", vulnAvailabilityImpact, VulnerabilityCia.NOT_DEFINED);

        // Base Metrics: Subsequent System Impact Metrics
        appendIfNotDefault(vector, "SC", subConfidentialityImpact, SubsequentCia.NOT_DEFINED);
        appendIfNotDefault(vector, "SI", subIntegrityImpact, SubsequentCia.NOT_DEFINED);
        appendIfNotDefault(vector, "SA", subAvailabilityImpact, SubsequentCia.NOT_DEFINED);

        // Supplemental Metrics
        appendIfNotDefault(vector, "S", safety, Safety.NOT_DEFINED);
        appendIfNotDefault(vector, "AU", automatable, Automatable.NOT_DEFINED);
        appendIfNotDefault(vector, "R", recovery, Recovery.NOT_DEFINED);
        appendIfNotDefault(vector, "V", valueDensity, ValueDensity.NOT_DEFINED);
        appendIfNotDefault(vector, "RE", vulnerabilityResponseEffort, VulnerabilityResponseEffort.NOT_DEFINED);
        appendIfNotDefault(vector, "U", providerUrgency, ProviderUrgency.NOT_DEFINED);

        // Environmental (Modified Base Metrics): Exploitability Metrics
        appendIfNotDefault(vector, "MAV", modifiedAttackVector, ModifiedAttackVector.NOT_DEFINED);
        appendIfNotDefault(vector, "MAC", modifiedAttackComplexity, ModifiedAttackComplexity.NOT_DEFINED);
        appendIfNotDefault(vector, "MAT", modifiedAttackRequirements, ModifiedAttackRequirements.NOT_DEFINED);
        appendIfNotDefault(vector, "MPR", modifiedPrivilegesRequired, ModifiedPrivilegesRequired.NOT_DEFINED);
        appendIfNotDefault(vector, "MUI", modifiedUserInteraction, ModifiedUserInteraction.NOT_DEFINED);

        // Environmental (Modified Base Metrics): Vulnerable System Impact Metrics
        appendIfNotDefault(vector, "MVC", modifiedVulnConfidentialityImpact, ModifiedVulnerabilityCia.NOT_DEFINED);
        appendIfNotDefault(vector, "MVI", modifiedVulnIntegrityImpact, ModifiedVulnerabilityCia.NOT_DEFINED);
        appendIfNotDefault(vector, "MVA", modifiedVulnAvailabilityImpact, ModifiedVulnerabilityCia.NOT_DEFINED);

        // Environmental (Modified Base Metrics): Subsequent System Impact Metrics
        appendIfNotDefault(vector, "MSC", modifiedSubConfidentialityImpact, ModifiedSubsequentConfidentiality.NOT_DEFINED);
        appendIfNotDefault(vector, "MSI", modifiedSubIntegrityImpact, ModifiedSubsequentIntegrityAvailability.NOT_DEFINED);
        appendIfNotDefault(vector, "MSA", modifiedSubAvailabilityImpact, ModifiedSubsequentIntegrityAvailability.NOT_DEFINED);

        // Environmental (Security Requirements)
        appendIfNotDefault(vector, "CR", confidentialityRequirement, RequirementsCia.NOT_DEFINED);
        appendIfNotDefault(vector, "IR", integrityRequirement, RequirementsCia.NOT_DEFINED);
        appendIfNotDefault(vector, "AR", availabilityRequirement, RequirementsCia.NOT_DEFINED);

        // Threat Metrics
        appendIfNotDefault(vector, "E", exploitMaturity, ExploitMaturity.NOT_DEFINED);

        return vector.toString().replaceAll("/$", "");
    }

    public String toString(String... attributes) {
        final StringBuilder vector = new StringBuilder();
        vector.append("CVSS:4.0/");

        for (String attribute : attributes) {
            vector.append(attribute).append(":").append(getVectorArgument(attribute).getShortIdentifier()).append("/");
        }

        return vector.toString().replaceAll("/$", "");
    }

    public static String toString(Cvss4_0[] vector, String... attributes) {
        final StringJoiner array = new StringJoiner(", ", "[", "]");

        for (Cvss4_0 v : vector) {
            array.add(v.toString(attributes));
        }

        return array.toString();
    }

    private <T extends Cvss4_0Attribute> void appendIfNotDefault(StringBuilder vector, String partName, T currentValue, T defaultValue) {
        if (currentValue != defaultValue) {
            vector.append(partName).append(":").append(currentValue.getShortIdentifier()).append("/");
        }
    }

    @Override
    public CvssScoreResult calculateScores() {
        return new CvssScoreResult(this);
    }

    // CVSS 4.0 attributes definitions

    public enum AttackVector implements Cvss4_0Attribute { // AV
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        NETWORK("NETWORK", "N"),
        ADJACENT_NETWORK("ADJACENT", "A"),
        LOCAL("LOCAL", "L"),
        PHYSICAL("PHYSICAL", "P");

        public final String identifier, shortIdentifier;

        AttackVector(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static AttackVector fromString(String part) {
            return Cvss4_0Attribute.fromString(part, AttackVector.class, NETWORK);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public AttackVector getWorseCase() {
            return NETWORK;
        }
    }

    public enum AttackComplexity implements Cvss4_0Attribute { // AC
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        LOW("LOW", "L"),
        HIGH("HIGH", "H");

        public final String identifier, shortIdentifier;

        AttackComplexity(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static AttackComplexity fromString(String part) {
            return Cvss4_0Attribute.fromString(part, AttackComplexity.class, LOW);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public AttackComplexity getWorseCase() {
            return LOW;
        }
    }

    public enum AttackRequirements implements Cvss4_0Attribute { // AT
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        PRESENT("PRESENT", "P");

        private final String identifier, shortIdentifier;

        AttackRequirements(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static AttackRequirements fromString(String part) {
            return Cvss4_0Attribute.fromString(part, AttackRequirements.class, NONE);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public AttackRequirements getWorseCase() {
            return NONE;
        }
    }

    public enum PrivilegesRequired implements Cvss4_0Attribute { // PR
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        LOW("LOW", "L"),
        HIGH("HIGH", "H");

        public final String identifier, shortIdentifier;

        PrivilegesRequired(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static PrivilegesRequired fromString(String part) {
            return Cvss4_0Attribute.fromString(part, PrivilegesRequired.class, NONE);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public PrivilegesRequired getWorseCase() {
            return NONE;
        }
    }

    public enum UserInteraction implements Cvss4_0Attribute { // UI
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        PASSIVE("PASSIVE", "P"),
        ACTIVE("ACTIVE", "A");

        private final String identifier, shortIdentifier;

        UserInteraction(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static UserInteraction fromString(String part) {
            return Cvss4_0Attribute.fromString(part, UserInteraction.class, NONE);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public UserInteraction getWorseCase() {
            return NONE;
        }
    }

    public enum VulnerabilityCia implements Cvss4_0Attribute { // VC, VI, VA
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        HIGH("HIGH", "H"),
        LOW("LOW", "L"),
        NONE("NONE", "N");

        public final String identifier, shortIdentifier;

        VulnerabilityCia(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static VulnerabilityCia fromString(String part) {
            return Cvss4_0Attribute.fromString(part, VulnerabilityCia.class, NONE);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public VulnerabilityCia getWorseCase() {
            return HIGH;
        }
    }

    public enum SubsequentCia implements Cvss4_0Attribute { // SC, SI, SA
        /**
         * <b>X IS NOT A VALID VALUE FOR THIS ATTRIBUTE!</b><br>
         * But in order to allow for building vectors with partial information that are applied onto other vectors, such
         * as with the <code>VulnerabilityStatus#applyCvss4(Cvss4_0)</code> method, this value is required as marker.
         */
        NOT_DEFINED("NOT_DEFINED", "X"),
        /**
         * <b>SAFETY IS NOT A VALID VALUE OF SC, SI, SA!</b><br>
         * This value is not defined in the standard, but still used in the calculations, so this value has to be
         * present in the enum.
         */
        SAFETY("SAFETY", "S"),
        HIGH("HIGH", "H"),
        LOW("LOW", "L"),
        NONE("NONE", "N");

        public final String identifier, shortIdentifier;

        SubsequentCia(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static SubsequentCia fromString(String part) {
            return Cvss4_0Attribute.fromString(part, SubsequentCia.class, NONE);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public SubsequentCia getWorseCase() {
            return HIGH;
        }
    }

    public enum Safety implements Cvss4_0Attribute { // S
        NOT_DEFINED("NOT_DEFINED", "X"),
        NEGLIGIBLE("NEGLIGIBLE", "N"),
        PRESENT("PRESENT", "P");

        private final String identifier, shortIdentifier;

        Safety(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static Safety fromString(String part) {
            return Cvss4_0Attribute.fromString(part, Safety.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public Safety getWorseCase() {
            return PRESENT;
        }
    }

    public enum Automatable implements Cvss4_0Attribute { // AU
        NOT_DEFINED("NOT_DEFINED", "X"),
        NO("NO", "N"),
        YES("YES", "Y");

        private final String identifier, shortIdentifier;

        Automatable(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static Automatable fromString(String part) {
            return Cvss4_0Attribute.fromString(part, Automatable.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public Automatable getWorseCase() {
            return YES;
        }
    }

    public enum Recovery implements Cvss4_0Attribute { // R
        NOT_DEFINED("NOT_DEFINED", "X"),
        AUTOMATIC("AUTOMATIC", "A"),
        USER("USER", "U"),
        IRRECOVERABLE("IRRECOVERABLE", "I");

        private final String identifier, shortIdentifier;

        Recovery(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static Recovery fromString(String part) {
            return Cvss4_0Attribute.fromString(part, Recovery.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public Recovery getWorseCase() {
            return IRRECOVERABLE;
        }
    }

    public enum ValueDensity implements Cvss4_0Attribute { // V
        NOT_DEFINED("NOT_DEFINED", "X"),
        DIFFUSE("DIFFUSE", "D"),
        CONCENTRATED("CONCENTRATED", "C");

        private final String identifier, shortIdentifier;

        ValueDensity(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ValueDensity fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ValueDensity.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ValueDensity getWorseCase() {
            return CONCENTRATED;
        }
    }

    public enum VulnerabilityResponseEffort implements Cvss4_0Attribute { // RE
        NOT_DEFINED("NOT_DEFINED", "X"),
        LOW("LOW", "L"),
        MODERATE("MODERATE", "M"),
        HIGH("HIGH", "H");

        private final String identifier, shortIdentifier;

        VulnerabilityResponseEffort(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static VulnerabilityResponseEffort fromString(String part) {
            return Cvss4_0Attribute.fromString(part, VulnerabilityResponseEffort.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public VulnerabilityResponseEffort getWorseCase() {
            return HIGH;
        }
    }

    public enum ProviderUrgency implements Cvss4_0Attribute { // U
        NOT_DEFINED("NOT_DEFINED", "X"),
        CLEAR("CLEAR", "Clear"),
        GREEN("GREEN", "Green"),
        AMBER("AMBER", "Amber"),
        RED("RED", "Red");

        private final String identifier, shortIdentifier;

        ProviderUrgency(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ProviderUrgency fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ProviderUrgency.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ProviderUrgency getWorseCase() {
            return RED;
        }
    }

    public enum ModifiedAttackVector implements Cvss4_0Attribute { // MAV
        NOT_DEFINED("NOT_DEFINED", "X"),
        NETWORK("NETWORK", "N"),
        ADJACENT_NETWORK("ADJACENT", "A"),
        LOCAL("LOCAL", "L"),
        PHYSICAL("PHYSICAL", "P");

        public final String identifier, shortIdentifier;

        ModifiedAttackVector(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedAttackVector fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedAttackVector.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedAttackVector getWorseCase() {
            return NETWORK;
        }
    }

    public enum ModifiedAttackComplexity implements Cvss4_0Attribute { // MAC
        NOT_DEFINED("NOT_DEFINED", "X"),
        LOW("LOW", "L"),
        HIGH("HIGH", "H");

        public final String identifier, shortIdentifier;

        ModifiedAttackComplexity(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedAttackComplexity fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedAttackComplexity.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedAttackComplexity getWorseCase() {
            return LOW;
        }
    }

    public enum ModifiedAttackRequirements implements Cvss4_0Attribute { // MAT
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        PRESENT("PRESENT", "P");

        private final String identifier, shortIdentifier;

        ModifiedAttackRequirements(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedAttackRequirements fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedAttackRequirements.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedAttackRequirements getWorseCase() {
            return PRESENT;
        }
    }

    public enum ModifiedPrivilegesRequired implements Cvss4_0Attribute { // MPR
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        LOW("LOW", "L"),
        HIGH("HIGH", "H");

        private final String identifier, shortIdentifier;

        ModifiedPrivilegesRequired(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedPrivilegesRequired fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedPrivilegesRequired.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedPrivilegesRequired getWorseCase() {
            return NONE;
        }
    }

    public enum ModifiedUserInteraction implements Cvss4_0Attribute { // MUI
        NOT_DEFINED("NOT_DEFINED", "X"),
        NONE("NONE", "N"),
        PASSIVE("PASSIVE", "P"),
        ACTIVE("ACTIVE", "A");

        private final String identifier, shortIdentifier;

        ModifiedUserInteraction(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedUserInteraction fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedUserInteraction.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedUserInteraction getWorseCase() {
            return NONE;
        }
    }

    public enum ModifiedVulnerabilityCia implements Cvss4_0Attribute { // MVC, MVI, MVA
        NOT_DEFINED("NOT_DEFINED", "X"),
        HIGH("HIGH", "H"),
        LOW("LOW", "L"),
        NONE("NONE", "N");

        private final String identifier, shortIdentifier;

        ModifiedVulnerabilityCia(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedVulnerabilityCia fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedVulnerabilityCia.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedVulnerabilityCia getWorseCase() {
            return HIGH;
        }
    }

    public enum ModifiedSubsequentConfidentiality implements Cvss4_0Attribute { // MSC
        NOT_DEFINED("NOT_DEFINED", "X"),
        HIGH("HIGH", "H"),
        LOW("LOW", "L"),
        NEGLIGIBLE("NEGLIGIBLE", "N");

        private final String identifier, shortIdentifier;

        ModifiedSubsequentConfidentiality(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedSubsequentConfidentiality fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedSubsequentConfidentiality.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedSubsequentConfidentiality getWorseCase() {
            return HIGH;
        }
    }

    public enum ModifiedSubsequentIntegrityAvailability implements Cvss4_0Attribute { // MSI, MSA
        NOT_DEFINED("NOT_DEFINED", "X"),
        SAFETY("SAFETY", "S"),
        HIGH("HIGH", "H"),
        LOW("LOW", "L"),
        NEGLIGIBLE("NEGLIGIBLE", "N");

        private final String identifier, shortIdentifier;

        ModifiedSubsequentIntegrityAvailability(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ModifiedSubsequentIntegrityAvailability fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ModifiedSubsequentIntegrityAvailability.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ModifiedSubsequentIntegrityAvailability getWorseCase() {
            return SAFETY;
        }
    }

    public enum RequirementsCia implements Cvss4_0Attribute { // CR, IR, AR
        NOT_DEFINED("NOT_DEFINED", "X"),
        HIGH("HIGH", "H"),
        MEDIUM("MEDIUM", "M"),
        LOW("LOW", "L");

        private final String identifier, shortIdentifier;

        RequirementsCia(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static RequirementsCia fromString(String part) {
            return Cvss4_0Attribute.fromString(part, RequirementsCia.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public RequirementsCia getWorseCase() {
            return HIGH;
        }
    }

    public enum ExploitMaturity implements Cvss4_0Attribute { // E
        NOT_DEFINED("NOT_DEFINED", "X"),
        UNREPORTED("UNREPORTED", "U"),
        POC("POC", "P"),
        ATTACKED("ATTACKED", "A");

        private final String identifier, shortIdentifier;

        ExploitMaturity(String identifier, String shortIdentifier) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
        }

        public static ExploitMaturity fromString(String part) {
            return Cvss4_0Attribute.fromString(part, ExploitMaturity.class, NOT_DEFINED);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        @Override
        public ExploitMaturity getWorseCase() {
            return ATTACKED;
        }
    }

    public interface Cvss4_0Attribute {
        String getShortIdentifier();

        Cvss4_0Attribute getWorseCase();

        static <T extends Cvss4_0Attribute> T fromString(String part, Class<T> clazz, T defaultValue) {
            return Arrays.stream(clazz.getEnumConstants()).filter(value -> value.getShortIdentifier().equalsIgnoreCase(part)).findFirst().orElse(defaultValue);
        }
    }

    // getters/setters

    public AttackVector getAttackVector() {
        return attackVector;
    }

    public void setAttackVector(AttackVector attackVector) {
        this.attackVector = attackVector;
    }

    public AttackComplexity getAttackComplexity() {
        return attackComplexity;
    }

    public void setAttackComplexity(AttackComplexity attackComplexity) {
        this.attackComplexity = attackComplexity;
    }

    public AttackRequirements getAttackRequirements() {
        return attackRequirements;
    }

    public void setAttackRequirements(AttackRequirements attackRequirements) {
        this.attackRequirements = attackRequirements;
    }

    public PrivilegesRequired getPrivilegesRequired() {
        return privilegesRequired;
    }

    public void setPrivilegesRequired(PrivilegesRequired privilegesRequired) {
        this.privilegesRequired = privilegesRequired;
    }

    public UserInteraction getUserInteraction() {
        return userInteraction;
    }

    public void setUserInteraction(UserInteraction userInteraction) {
        this.userInteraction = userInteraction;
    }

    public VulnerabilityCia getVulnConfidentialityImpact() {
        return vulnConfidentialityImpact;
    }

    public void setVulnConfidentialityImpact(VulnerabilityCia vulnConfidentialityImpact) {
        this.vulnConfidentialityImpact = vulnConfidentialityImpact;
    }

    public VulnerabilityCia getVulnIntegrityImpact() {
        return vulnIntegrityImpact;
    }

    public void setVulnIntegrityImpact(VulnerabilityCia vulnIntegrityImpact) {
        this.vulnIntegrityImpact = vulnIntegrityImpact;
    }

    public VulnerabilityCia getVulnAvailabilityImpact() {
        return vulnAvailabilityImpact;
    }

    public void setVulnAvailabilityImpact(VulnerabilityCia vulnAvailabilityImpact) {
        this.vulnAvailabilityImpact = vulnAvailabilityImpact;
    }

    public SubsequentCia getSubConfidentialityImpact() {
        return subConfidentialityImpact;
    }

    public void setSubConfidentialityImpact(SubsequentCia subConfidentialityImpact) {
        this.subConfidentialityImpact = subConfidentialityImpact;
    }

    public SubsequentCia getSubIntegrityImpact() {
        return subIntegrityImpact;
    }

    public void setSubIntegrityImpact(SubsequentCia subIntegrityImpact) {
        this.subIntegrityImpact = subIntegrityImpact;
    }

    public SubsequentCia getSubAvailabilityImpact() {
        return subAvailabilityImpact;
    }

    public void setSubAvailabilityImpact(SubsequentCia subAvailabilityImpact) {
        this.subAvailabilityImpact = subAvailabilityImpact;
    }

    public Safety getSafety() {
        return safety;
    }

    public void setSafety(Safety safety) {
        this.safety = safety;
    }

    public Automatable getAutomatable() {
        return automatable;
    }

    public void setAutomatable(Automatable automatable) {
        this.automatable = automatable;
    }

    public Recovery getRecovery() {
        return recovery;
    }

    public void setRecovery(Recovery recovery) {
        this.recovery = recovery;
    }

    public ValueDensity getValueDensity() {
        return valueDensity;
    }

    public void setValueDensity(ValueDensity valueDensity) {
        this.valueDensity = valueDensity;
    }

    public VulnerabilityResponseEffort getVulnerabilityResponseEffort() {
        return vulnerabilityResponseEffort;
    }

    public void setVulnerabilityResponseEffort(VulnerabilityResponseEffort vulnerabilityResponseEffort) {
        this.vulnerabilityResponseEffort = vulnerabilityResponseEffort;
    }

    public ProviderUrgency getProviderUrgency() {
        return providerUrgency;
    }

    public void setProviderUrgency(ProviderUrgency providerUrgency) {
        this.providerUrgency = providerUrgency;
    }

    public ModifiedAttackVector getModifiedAttackVector() {
        return modifiedAttackVector;
    }

    public void setModifiedAttackVector(ModifiedAttackVector modifiedAttackVector) {
        this.modifiedAttackVector = modifiedAttackVector;
    }

    public ModifiedAttackComplexity getModifiedAttackComplexity() {
        return modifiedAttackComplexity;
    }

    public void setModifiedAttackComplexity(ModifiedAttackComplexity modifiedAttackComplexity) {
        this.modifiedAttackComplexity = modifiedAttackComplexity;
    }

    public ModifiedAttackRequirements getModifiedAttackRequirements() {
        return modifiedAttackRequirements;
    }

    public void setModifiedAttackRequirements(ModifiedAttackRequirements modifiedAttackRequirements) {
        this.modifiedAttackRequirements = modifiedAttackRequirements;
    }

    public ModifiedPrivilegesRequired getModifiedPrivilegesRequired() {
        return modifiedPrivilegesRequired;
    }

    public void setModifiedPrivilegesRequired(ModifiedPrivilegesRequired modifiedPrivilegesRequired) {
        this.modifiedPrivilegesRequired = modifiedPrivilegesRequired;
    }

    public ModifiedUserInteraction getModifiedUserInteraction() {
        return modifiedUserInteraction;
    }

    public void setModifiedUserInteraction(ModifiedUserInteraction modifiedUserInteraction) {
        this.modifiedUserInteraction = modifiedUserInteraction;
    }

    public ModifiedVulnerabilityCia getModifiedVulnConfidentialityImpact() {
        return modifiedVulnConfidentialityImpact;
    }

    public void setModifiedVulnConfidentialityImpact(ModifiedVulnerabilityCia modifiedVulnConfidentialityImpact) {
        this.modifiedVulnConfidentialityImpact = modifiedVulnConfidentialityImpact;
    }

    public ModifiedVulnerabilityCia getModifiedVulnIntegrityImpact() {
        return modifiedVulnIntegrityImpact;
    }

    public void setModifiedVulnIntegrityImpact(ModifiedVulnerabilityCia modifiedVulnIntegrityImpact) {
        this.modifiedVulnIntegrityImpact = modifiedVulnIntegrityImpact;
    }

    public ModifiedVulnerabilityCia getModifiedVulnAvailabilityImpact() {
        return modifiedVulnAvailabilityImpact;
    }

    public void setModifiedVulnAvailabilityImpact(ModifiedVulnerabilityCia modifiedVulnAvailabilityImpact) {
        this.modifiedVulnAvailabilityImpact = modifiedVulnAvailabilityImpact;
    }

    public ModifiedSubsequentConfidentiality getModifiedSubConfidentialityImpact() {
        return modifiedSubConfidentialityImpact;
    }

    public void setModifiedSubConfidentialityImpact(ModifiedSubsequentConfidentiality modifiedSubConfidentialityImpact) {
        this.modifiedSubConfidentialityImpact = modifiedSubConfidentialityImpact;
    }

    public ModifiedSubsequentIntegrityAvailability getModifiedSubIntegrityImpact() {
        return modifiedSubIntegrityImpact;
    }

    public void setModifiedSubIntegrityImpact(ModifiedSubsequentIntegrityAvailability modifiedSubIntegrityImpact) {
        this.modifiedSubIntegrityImpact = modifiedSubIntegrityImpact;
    }

    public ModifiedSubsequentIntegrityAvailability getModifiedSubAvailabilityImpact() {
        return modifiedSubAvailabilityImpact;
    }

    public void setModifiedSubAvailabilityImpact(ModifiedSubsequentIntegrityAvailability modifiedSubAvailabilityImpact) {
        this.modifiedSubAvailabilityImpact = modifiedSubAvailabilityImpact;
    }

    public RequirementsCia getConfidentialityRequirement() {
        return confidentialityRequirement;
    }

    public void setConfidentialityRequirement(RequirementsCia confidentialityRequirement) {
        this.confidentialityRequirement = confidentialityRequirement;
    }

    public RequirementsCia getIntegrityRequirement() {
        return integrityRequirement;
    }

    public void setIntegrityRequirement(RequirementsCia integrityRequirement) {
        this.integrityRequirement = integrityRequirement;
    }

    public RequirementsCia getAvailabilityRequirement() {
        return availabilityRequirement;
    }

    public void setAvailabilityRequirement(RequirementsCia availabilityRequirement) {
        this.availabilityRequirement = availabilityRequirement;
    }

    public ExploitMaturity getExploitMaturity() {
        return exploitMaturity;
    }

    public void setExploitMaturity(ExploitMaturity exploitMaturity) {
        this.exploitMaturity = exploitMaturity;
    }

    // static methods

    public static Optional<Cvss4_0> optionalParse(String vector) {
        if (vector == null || StringUtils.isEmpty(MultiScoreCvssVector.normalizeVector(vector))) {
            return Optional.empty();
        }

        return Optional.of(new Cvss4_0(vector));
    }
}
