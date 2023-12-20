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
package org.metaeffekt.core.security.cvss.v4P0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * <a href="https://www.first.org/cvss/v4-0/cvss-v40-specification.pdf">https://www.first.org/cvss/v4-0/cvss-v40-specification.pdf</a><br>
 * <a href="https://www.first.org/cvss/v4.0/specification-document">https://www.first.org/cvss/v4.0/specification-document</a><br>
 * See the {@link Cvss4P0} class documentation for more information on how this is used.
 */
public class Cvss4P0MacroVector {

    private final static Logger LOG = LoggerFactory.getLogger(Cvss4P0MacroVector.class);

    private final EQ eq1;
    private final EQ eq2;
    private final EQ eq3;
    private final EQ eq4;
    private final EQ eq5;
    private final EQ eq6;
    private final EQ jointEq3AndEq6;

    public Cvss4P0MacroVector(Cvss4P0 sourceVector) {
        this.eq1 = findMatchingEQ("1", EQ1_DEFINITIONS, sourceVector);
        this.eq2 = findMatchingEQ("2", EQ2_DEFINITIONS, sourceVector);
        this.eq3 = findMatchingEQ("3", EQ3_DEFINITIONS, sourceVector);
        this.eq4 = findMatchingEQ("4", EQ4_DEFINITIONS, sourceVector);
        this.eq5 = findMatchingEQ("5", EQ5_DEFINITIONS, sourceVector);
        this.eq6 = findMatchingEQ("6", EQ6_DEFINITIONS, sourceVector);
        this.jointEq3AndEq6 = findMatchingEQ("3,6", JOINT_EQ3_EQ6_DEFINITIONS, sourceVector);
    }

    public Cvss4P0MacroVector(EQ eq1, EQ eq2, EQ eq3, EQ eq4, EQ eq5, EQ eq6, EQ jointEq3AndEq6) {
        this.eq1 = eq1;
        this.eq2 = eq2;
        this.eq3 = eq3;
        this.eq4 = eq4;
        this.eq5 = eq5;
        this.eq6 = eq6;
        this.jointEq3AndEq6 = jointEq3AndEq6;
    }

    private EQ findMatchingEQ(String eqType, EQ[] definitions, Cvss4P0 sourceVector) {
        for (EQ eq : definitions) {
            if (eq.matchesConstraints(sourceVector)) {
                return eq;
            }
        }
        throw new IllegalStateException("No matching EQ found for " + eqType + " and vector " + sourceVector);
    }

    public EQ getEq1() {
        return eq1;
    }

    public EQ getEq2() {
        return eq2;
    }

    public EQ getEq3() {
        return eq3;
    }

    public EQ getEq4() {
        return eq4;
    }

    public EQ getEq5() {
        return eq5;
    }

    public EQ getEq6() {
        return eq6;
    }

    public EQ getJointEq3AndEq6() {
        return jointEq3AndEq6;
    }

    public EQ getEQ(int i) {
        switch (i) {
            case 1:
                return eq1;
            case 2:
                return eq2;
            case 3:
                return eq3;
            case 4:
                return eq4;
            case 5:
                return eq5;
            case 6:
                return eq6;
            case 7:
                return jointEq3AndEq6;
            default:
                throw new IllegalArgumentException("Invalid EQ index: " + i);
        }
    }

    public double getLookupTableScore() {
        return Cvss4P0Lookup.getMacroVectorScore(this);
    }

    @Override
    public String toString() {
        return eq1.getLevel() + eq2.getLevel() + eq3.getLevel() + eq4.getLevel() + eq5.getLevel() + eq6.getLevel();
    }

    public Cvss4P0MacroVector deriveNextLower(int i) {
        final EQ eq1 = i != 1 ? this.eq1 : getNextLower(EQ1_DEFINITIONS, this.eq1);
        final EQ eq2 = i != 2 ? this.eq2 : getNextLower(EQ2_DEFINITIONS, this.eq2);
        final EQ eq3 = i != 3 ? this.eq3 : getNextLower(EQ3_DEFINITIONS, this.eq3);
        final EQ eq4 = i != 4 ? this.eq4 : getNextLower(EQ4_DEFINITIONS, this.eq4);
        final EQ eq5 = i != 5 ? this.eq5 : getNextLower(EQ5_DEFINITIONS, this.eq5);
        final EQ eq6 = i != 6 ? this.eq6 : getNextLower(EQ6_DEFINITIONS, this.eq6);
        final EQ jointEq3AndEq6 = i != 7 ? this.jointEq3AndEq6 : getNextLower(JOINT_EQ3_EQ6_DEFINITIONS, this.jointEq3AndEq6);

        return new Cvss4P0MacroVector(eq1, eq2, eq3, eq4, eq5, eq6, jointEq3AndEq6);
    }

    private static int getIndexInDefinitions(EQ[] definitions, EQ eq) {
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i] == eq) {
                return i;
            }
        }
        throw new IllegalStateException("EQ not found in definitions: " + eq);
    }

    private static EQ getNextLower(EQ[] definitions, EQ eq) { // lower means + 1
        final int index = getIndexInDefinitions(definitions, eq);
        return definitions.length > index + 1 ? definitions[index + 1] : EQ_ERROR_DEFINITION;
    }

    private static boolean is(Cvss4P0 vector, String attribute, String value) {
        final String comparisonValue = getComparisonMetric(vector, attribute).getShortIdentifier();
        // LOG.info("Attribute [{}] has value [{}] -> [{}]", attribute, vector.getVectorArgument(attribute).getShortIdentifier(), comparisonValue); // debugging
        return value.equals(comparisonValue);
    }

    public static final EQ EQ_ERROR_DEFINITION = new EQ("9", -1, new String[]{}, vector -> true);

    /**
     * <table>
     *   <caption><strong>EQ1 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>AV:N and PR:N and UI:N</td>
     *     <td>AV:N/PR:N/UI:N</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>(AV:N or PR:N or UI:N) and not (AV:N and PR:N and UI:N) and not AV:P</td>
     *     <td>AV:A/PR:N/UI:N, AV:N/PR:L/UI:N, AV:N/PR:N/UI:P</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>AV:P or not(AV:N or PR:N or UI:N)</td>
     *     <td>AV:P/PR:N/UI:N, AV:A/PR:L/UI:L</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ1_DEFINITIONS = {
            new EQ("0",
                    1,
                    new String[]{"AV:N/PR:N/UI:N"},
                    vector -> is(vector, "AV", "N") && is(vector, "PR", "N") && is(vector, "UI", "N")
            ),
            new EQ("1",
                    4,
                    new String[]{"AV:A/PR:N/UI:N", "AV:N/PR:L/UI:N", "AV:N/PR:N/UI:P"},
                    vector -> (is(vector, "AV", "N") || is(vector, "PR", "N") || is(vector, "UI", "N")) &&
                            !(is(vector, "AV", "N") && is(vector, "PR", "N") && is(vector, "UI", "N")) &&
                            !is(vector, "AV", "P")
            ),
            new EQ("2",
                    5,
                    new String[]{"AV:P/PR:N/UI:N", "AV:A/PR:L/UI:P"},
                    vector -> is(vector, "AV", "P") || !(is(vector, "AV", "N") || is(vector, "PR", "N") || is(vector, "UI", "N"))
            )
    };

    /**
     * <table>
     *   <caption><strong>EQ2 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>AC:L and AT:N</td>
     *     <td>AC:L/AT:N</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>not (AC:L and AT:N)</td>
     *     <td>AC:L/AT:P, AC:H/AT:N</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ2_DEFINITIONS = {
            new EQ("0",
                    1,
                    new String[]{"AC:L/AT:N"},
                    vector -> is(vector, "AC", "L") && is(vector, "AT", "N")),
            new EQ("1",
                    2,
                    new String[]{"AC:H/AT:N", "AC:L/AT:P"},
                    vector -> !(is(vector, "AC", "L") && is(vector, "AT", "N")))
    };

    /**
     * <table>
     *   <caption><strong>EQ3 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>VC:H and VI:H</td>
     *     <td>VC:H/VI:H/VA:H</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>not (VC:H and VI:H) and (VC:H or VI:H or VA:H)</td>
     *     <td>VC:L/VI:H/VA:H, VC:H/VI:L/VA:H</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>not (VC:H or VI:H or VA:H)</td>
     *     <td>VC:L/VI:L/VA:L</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ3_DEFINITIONS = {
            new EQ("0",
                    -1,
                    new String[]{"VC:H/VI:H/VA:H"},
                    vector -> is(vector, "VC", "H") && is(vector, "VI", "H")
            ),
            new EQ("1",
                    -1,
                    new String[]{"VC:L/VI:H/VA:H", "VC:H/VI:L/VA:H"},
                    vector -> !(is(vector, "VC", "H") && is(vector, "VI", "H")) && (is(vector, "VC", "H") || is(vector, "VI", "H") || is(vector, "VA", "H"))
            ),
            new EQ("2",
                    -1,
                    new String[]{"VC:L/VI:L/VA:L"},
                    vector -> !(is(vector, "VC", "H") || is(vector, "VI", "H") || is(vector, "VA", "H"))
            )
    };

    /**
     * <table>
     *   <caption><strong>EQ4 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>MSI:S or MSA:S</td>
     *     <td>SC:H/SI:S/SA:S</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>not (MSI:S and MSA:S) and (SC:H or SI:H or SA:H)</td>
     *     <td>SC:H/SI:H/SA:H</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>not (MSI:S and MSA:S) and not (SC:H or SI:H or SA:H)</td>
     *     <td>SC:L/SI:L/SA:L</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ4_DEFINITIONS = {
            new EQ("0",
                    6,
                    new String[]{"SC:H/SI:S/SA:S"},
                    vector -> is(vector, "MSI", "S") || is(vector, "MSA", "S")
            ),
            new EQ("1",
                    5,
                    new String[]{"SC:H/SI:H/SA:H"},
                    vector -> !(is(vector, "MSI", "S") && is(vector, "MSA", "S")) && (is(vector, "SC", "H") || is(vector, "SI", "H") || is(vector, "SA", "H"))
            ),
            new EQ("2",
                    4,
                    new String[]{"SC:L/SI:L/SA:L"},
                    vector -> !(is(vector, "MSI", "S") && is(vector, "MSA", "S")) && !(is(vector, "SC", "H") || is(vector, "SI", "H") || is(vector, "SA", "H"))
            )
    };

    /**
     * <table>
     *   <caption><strong>EQ5 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>E:A</td>
     *     <td>E:A</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>E:P</td>
     *     <td>E:P</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>E:U</td>
     *     <td>E:U</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ5_DEFINITIONS = {
            new EQ("0",
                    1,
                    new String[]{"E:A"},
                    vector -> is(vector, "E", "A")
            ),
            new EQ("1",
                    1,
                    new String[]{"E:P"},
                    vector -> is(vector, "E", "P")
            ),
            new EQ("2",
                    1,
                    new String[]{"E:U"},
                    vector -> is(vector, "E", "U")
            )
    };

    /**
     * <strong>DOCUMENTATION IS NOT UP TO DATE, THIS IS INCORRECT</strong>
     * <table>
     *   <caption><strong>EQ6 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>AV:N and PR:N and UI:N</td>
     *     <td>AV:N/PR:N/UI:N</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>(CR:H and VC:H) or (IR:H and VI:H) or (AR:H and VA:H)</td>
     *     <td>Multiple vectors, see code for details</td>
     *   </tr>
     * </table>
     */
    @Deprecated
    protected static final EQ[] EQ6_DEFINITIONS_DOC = {
            new EQ("0",
                    -1,
                    new String[]{"AV:N/PR:N/UI:N"},
                    vector -> is(vector, "AV", "N") && is(vector, "PR", "N") && is(vector, "UI", "N")
            ),
            new EQ("1",
                    -1,
                    new String[]{
                            "VC:H/VI:H/VA:H/CR:M/IR:M/AR:M",
                            "VC:H/VI:H/VA:L/CR:M/IR:M/AR:H",
                            "VC:H/VI:L/VA:H/CR:M/IR:H/AR:M",
                            "VC:H/VI:L/VA:L/CR:M/IR:H/AR:H",
                            "VC:L/VI:H/VA:H/CR:H/IR:M/AR:M",
                            "VC:L/VI:H/VA:L/CR:H/IR:M/AR:H",
                            "VC:L/VI:L/VA:H/CR:H/IR:H/AR:M",
                            "VC:L/VI:L/VA:L/CR:H/IR:H/AR:H"
                    },
                    vector -> (is(vector, "CR", "H") && is(vector, "VC", "H")) ||
                            (is(vector, "IR", "H") && is(vector, "VI", "H")) ||
                            (is(vector, "AR", "H") && is(vector, "VA", "H"))
            )
    };

    /**
     * <table>
     *   <caption><strong>EQ6 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>(CR:H and VC:H) or (IR:H and VI:H) or (AR:H and VA:H)</td>
     *     <td>TBD</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>not[(CR:H and VC:H) or (IR:H and VI:H) or (AR:H and VA:H)]</td>
     *     <td>TBD</td>
     *   </tr>
     * </table>
     */
    public static final EQ[] EQ6_DEFINITIONS = {
            new EQ("0",
                    -1,
                    new String[]{"AV:N/PR:N/UI:N"},
                    vector -> (is(vector, "CR", "H") && is(vector, "VC", "H")) ||
                            (is(vector, "IR", "H") && is(vector, "VI", "H")) ||
                            (is(vector, "AR", "H") && is(vector, "VA", "H"))
            ),
            new EQ("1",
                    -1,
                    new String[]{
                            "VC:H/VI:H/VA:H/CR:M/IR:M/AR:M",
                            "VC:H/VI:H/VA:L/CR:M/IR:M/AR:H",
                            "VC:H/VI:L/VA:H/CR:M/IR:H/AR:M",
                            "VC:H/VI:L/VA:L/CR:M/IR:H/AR:H",
                            "VC:L/VI:H/VA:H/CR:H/IR:M/AR:M",
                            "VC:L/VI:H/VA:L/CR:H/IR:M/AR:H",
                            "VC:L/VI:L/VA:H/CR:H/IR:H/AR:M",
                            "VC:L/VI:L/VA:L/CR:H/IR:H/AR:H"
                    },
                    vector -> !((is(vector, "CR", "H") && is(vector, "VC", "H")) ||
                            (is(vector, "IR", "H") && is(vector, "VI", "H")) ||
                            (is(vector, "AR", "H") && is(vector, "VA", "H")))
            )
    };


    /**
     * <table border="1">
     *   <caption><strong>Joint EQ3+EQ6 - MacroVectors</strong></caption>
     *   <tr>
     *     <th>Level</th>
     *     <th>Constraints</th>
     *     <th>Highest Severity Vectors</th>
     *   </tr>
     *   <tr>
     *     <td>00</td>
     *     <td>VC:H and VI:H and [CR:H or IR:H or (AR:H and VA:H)]</td>
     *     <td>VC:H/VI:H/VA:H/CR:H/IR:H/AR:H</td>
     *   </tr>
     *   <tr>
     *     <td>01</td>
     *     <td>VC:H and VI:H and not (CR:H or IR:H) and not (AR:H and VA:H)</td>
     *     <td>VC:H/VI:H/VA:H/CR:M/IR:M/AR:M, VC:H/VI:H/VA:L/CR:M/IR:M/AR:H</td>
     *   </tr>
     *   <tr>
     *     <td>10</td>
     *     <td>not (VC:H and VI:H) and (VC:H or VI:H or VA:H) and (CR:H and VC:H) or (IR:H and VI:H) or (AR:H and VA:H)</td>
     *     <td>VC:L/VI:H/VA:H/CR:H/IR:H/AR:H, VC:H/VI:L/VA:H/CR:H/IR:H/AR:H</td>
     *   </tr>
     *   <tr>
     *     <td>11</td>
     *     <td>not (VC:H and VI:H) and (VC:H or VI:H or VA:H) and not (CR:H and VC:H) and not (IR:H and VI:H) and not (AR:H and VA:H)</td>
     *     <td>VC:H/VI:L/VA:H/CR:M/IR:H/AR:M, VC:H/VI:L/VA:L/CR:M/IR:H/AR:H, VC:L/VI:H/VA:H/CR:H/IR:M/AR:M, VC:L/VI:H/VA:L/CR:H/IR:M/AR:H, VC:L/VI:L/VA:H/CR:H/IR:H/AR:M</td>
     *   </tr>
     *   <tr>
     *     <td>20</td>
     *     <td>not (VC:H or VI:H or VA:H) and (CR:H and VC:H) or (IR:H and VI:H) or (AR:H and VA:H)</td>
     *     <td>Cannot exist</td>
     *   </tr>
     *   <tr>
     *     <td>21</td>
     *     <td>not (VC:H or VI:H or VA:H) and not (CR:H and VC:H) and not (IR:H and VI:H) and not (AR:H and VA:H)</td>
     *     <td>VC:L/VI:L/VA:L/CR:H/IR:H/AR:H</td>
     *   </tr>
     * </table>
     */
    protected static final EQ[] JOINT_EQ3_EQ6_DEFINITIONS = {
            new EQ("00",
                    7,
                    new String[]{"VC:H/VI:H/VA:H/CR:H/IR:H/AR:H"},
                    vector -> is(vector, "VC", "H") && is(vector, "VI", "H") && (is(vector, "CR", "H") || is(vector, "IR", "H") || (is(vector, "AR", "H") && is(vector, "VA", "H")))
            ),
            new EQ("01",
                    6,
                    new String[]{"VC:H/VI:H/VA:L/CR:M/IR:M/AR:H", "VC:H/VI:H/VA:H/CR:M/IR:M/AR:M"},
                    vector -> is(vector, "VC", "H") && is(vector, "VI", "H") && !(is(vector, "CR", "H") || is(vector, "IR", "H")) && !(is(vector, "AR", "H") && is(vector, "VA", "H"))
            ),
            new EQ("10",
                    8,
                    new String[]{"VC:L/VI:H/VA:H/CR:H/IR:H/AR:H", "VC:H/VI:L/VA:H/CR:H/IR:H/AR:H"},
                    vector -> !(is(vector, "VC", "H") && is(vector, "VI", "H")) && (is(vector, "VC", "H") || is(vector, "VI", "H") || is(vector, "VA", "H")) && ((is(vector, "CR", "H") && is(vector, "VC", "H")) || (is(vector, "IR", "H") && is(vector, "VI", "H")) || (is(vector, "AR", "H") && is(vector, "VA", "H")))
            ),
            new EQ("11",
                    8,
                    new String[]{"VC:L/VI:H/VA:L/CR:H/IR:M/AR:H", "VC:L/VI:H/VA:H/CR:H/IR:M/AR:M", "VC:H/VI:L/VA:H/CR:M/IR:H/AR:M", "VC:H/VI:L/VA:L/CR:M/IR:H/AR:H", "VC:L/VI:L/VA:H/CR:H/IR:H/AR:M"},
                    vector -> !(is(vector, "VC", "H") && is(vector, "VI", "H")) && (is(vector, "VC", "H") || is(vector, "VI", "H") || is(vector, "VA", "H")) && !(is(vector, "CR", "H") && is(vector, "VC", "H")) && !(is(vector, "IR", "H") && is(vector, "VI", "H")) && !(is(vector, "AR", "H") && is(vector, "VA", "H"))
            ),
            new EQ("20",
                    0,
                    new String[]{},
                    vector -> false // Cannot exist
            ),
            new EQ("21",
                    10,
                    new String[]{"VC:L/VI:L/VA:L/CR:H/IR:H/AR:H"},
                    vector -> !(is(vector, "VC", "H") || is(vector, "VI", "H") || is(vector, "VA", "H")) && !(is(vector, "CR", "H") && is(vector, "VC", "H")) && !(is(vector, "IR", "H") && is(vector, "VI", "H")) && !(is(vector, "AR", "H") && is(vector, "VA", "H"))
            )
    };

    private static Cvss4P0 v(String vector) {
        return new Cvss4P0(vector);
    }

    /**
     * Implementation based on
     * <a href="https://github.com/RedHatProductSecurity/cvss-v4-calculator/blob/5d7a90c66be1f0c1432f8c279e12e856943b7efb/app.js#L84">https://github.com/RedHatProductSecurity/cvss-v4-calculator/blob/5d7a90c66be1f0c1432f8c279e12e856943b7efb/app.js#L84</a>
     *
     * @param vector    the vector to get the value from
     * @param attribute the attribute to get the value for
     * @return the actual value of the attribute
     */
    public static Cvss4P0.Cvss4P0Attribute getComparisonMetric(Cvss4P0 vector, String attribute) {
        final Cvss4P0.Cvss4P0Attribute selected = vector.getVectorArgument(attribute);

        // E:X is the same as E:A
        if ("E".equals(attribute) && Cvss4P0.ExploitMaturity.NOT_DEFINED.equals(selected)) {
            return Cvss4P0.ExploitMaturity.ATTACKED;
        }

        // The three security requirements metrics have X equivalent to H.
        // CR:X, IR:X, AR:X are the same as CR:H, IR:H, AR:H
        if (("CR".equals(attribute) || "IR".equals(attribute) || "AR".equals(attribute)) && Cvss4P0.RequirementsCia.NOT_DEFINED.equals(selected)) {
            return Cvss4P0.RequirementsCia.HIGH;
        }

        // Special cases for MSI and MSA
        // the SI:S cannot happen in reality, but the reference implementation checks for it, so we do too
        if ("MSI".equals(attribute) && Cvss4P0.ModifiedSubsequentIntegrityAvailability.NOT_DEFINED.equals(selected)
                && "S".equals(vector.getVectorArgument("SI").getShortIdentifier())) {
            return Cvss4P0.ModifiedSubsequentIntegrityAvailability.SAFETY;
        }
        if ("MSA".equals(attribute) && Cvss4P0.ModifiedSubsequentIntegrityAvailability.NOT_DEFINED.equals(selected)
                && "S".equals(vector.getVectorArgument("SA").getShortIdentifier())) {
            return Cvss4P0.ModifiedSubsequentIntegrityAvailability.SAFETY;
        }

        // All other environmental metrics just overwrite base score values,
        // so if they're not defined just use the base score value.
        final Cvss4P0.Cvss4P0Attribute modifiedAttribute = vector.getVectorArgument("M" + attribute);
        if (modifiedAttribute != null) {
            String modifiedSelected = modifiedAttribute.getShortIdentifier();
            if (modifiedSelected != null && !"X".equals(modifiedSelected)) {
                return modifiedAttribute;
            }
        }

        return selected;
    }

    public static class EQ {
        private final String level;
        /**
         * Also described as the "max severity distance" within the EQ, meaning the space the EQ covers.
         */
        private final int vectorDepth;
        private final String[] highestSeverityVectorsUnparsed;
        private final Cvss4P0[] highestSeverityVectors;
        private final Predicate<Cvss4P0> predicate;

        public EQ(String level, int vectorDepth,
                  String[] highestSeverityVectors, Predicate<Cvss4P0> predicate) {
            this.level = level;
            this.vectorDepth = vectorDepth;
            this.highestSeverityVectorsUnparsed = highestSeverityVectors;
            this.highestSeverityVectors = Arrays.stream(highestSeverityVectors).map(Cvss4P0::new).toArray(Cvss4P0[]::new);
            this.predicate = predicate;
        }

        public String getLevel() {
            return level;
        }

        public int getLevelAsInt() {
            return Integer.parseInt(level);
        }

        public int getVectorDepth() {
            return vectorDepth;
        }

        public Cvss4P0[] getHighestSeverityVectors() {
            return highestSeverityVectors;
        }

        public String[] getHighestSeverityVectorsUnparsed() {
            return highestSeverityVectorsUnparsed;
        }

        public boolean matchesConstraints(Cvss4P0 vector) {
            return predicate.test(vector);
        }
    }
}
