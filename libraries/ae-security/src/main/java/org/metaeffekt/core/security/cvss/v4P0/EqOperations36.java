/*
 * Copyright 2009-2026 the original author or authors.
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

import java.util.List;
import java.util.function.Consumer;

public class EqOperations36 implements EqOperations {
    private static final EqOperations36 instance36 = new EqOperations36();

    public static EqOperations36 getInstance() {
        return instance36;
    }

    @Override
    public String[] getHighestSeverityVectors(Cvss4P0MacroVector thisMacroVector) {
        return thisMacroVector.getJointEq3AndEq6().getHighestSeverityVectorsUnparsed();
    }

    @Override
    public List<Consumer<Cvss4P0>> getHighestSeverityVectorsFn(Cvss4P0MacroVector thisMacroVector) {
        return thisMacroVector.getJointEq3AndEq6().getApplyHighestSeverityVectors();
    }

    @Override
    public String[] getRelevantAttributes() {
        return new String[]{"VC", "VI", "VA", "CR", "IR", "AR"};
    }

    @Override
    public Cvss4P0MacroVector[] deriveNextLowerMacro(Cvss4P0MacroVector macroVector) {
        final int eq3_val = macroVector.getEq3().getLevelAsInt();
        final int eq6_val = macroVector.getEq6().getLevelAsInt();
        final Cvss4P0MacroVector eq3eq6_next_lower_macro_left;
        final Cvss4P0MacroVector eq3eq6_next_lower_macro_right;

        if (eq3_val == 1 && eq6_val == 1) {
            // 11 -> 21
            return new Cvss4P0MacroVector[]{macroVector.deriveNextLower(3)};
        } else if (eq3_val == 0 && eq6_val == 1) {
            // 01 -> 11
            return new Cvss4P0MacroVector[]{macroVector.deriveNextLower(3)};
        } else if (eq3_val == 1 && eq6_val == 0) {
            // 10 -> 11
            return new Cvss4P0MacroVector[]{macroVector.deriveNextLower(6)};
        } else if (eq3_val == 0 && eq6_val == 0) {
            // 00 -> 01, 10
            eq3eq6_next_lower_macro_left = macroVector.deriveNextLower(3);
            eq3eq6_next_lower_macro_right = macroVector.deriveNextLower(6);
            return new Cvss4P0MacroVector[]{eq3eq6_next_lower_macro_left, eq3eq6_next_lower_macro_right};
        } else {
            // 21 -> 32 (does not exist)
            return new Cvss4P0MacroVector[]{macroVector.deriveNextLower(3).deriveNextLower(6)};
        }
    }

    @Override
    public double lookupScoresForNextLowerMacro(Cvss4P0MacroVector[] nextLowerMacros) {
        double score_eq3eq6_next_lower_macro_left = Double.NaN;
        double score_eq3eq6_next_lower_macro_right = Double.NaN;

        if (nextLowerMacros.length > 0 && nextLowerMacros[0] != null) {
            score_eq3eq6_next_lower_macro_left = nextLowerMacros[0].getLookupTableScore();
        }

        if (nextLowerMacros.length > 1 && nextLowerMacros[1] != null) {
            score_eq3eq6_next_lower_macro_right = nextLowerMacros[1].getLookupTableScore();
        }

        // choose the higher score among the two, if they exist
        if (!Double.isNaN(score_eq3eq6_next_lower_macro_left) && !Double.isNaN(score_eq3eq6_next_lower_macro_right)) {
            return Math.max(score_eq3eq6_next_lower_macro_left, score_eq3eq6_next_lower_macro_right);
        } else if (!Double.isNaN(score_eq3eq6_next_lower_macro_left)) {
            return score_eq3eq6_next_lower_macro_left;
        } else {
            return score_eq3eq6_next_lower_macro_right;
        }
    }

    @Override
    public int lookupMacroVectorDepth(Cvss4P0MacroVector thisMacroVector) {
        return thisMacroVector.getJointEq3AndEq6().getVectorDepth();
    }
}
