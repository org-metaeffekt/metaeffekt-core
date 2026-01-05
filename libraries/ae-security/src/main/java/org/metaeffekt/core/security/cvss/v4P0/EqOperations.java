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

/**
 * In the calculation of the CVSS:4.0 score, five (3 and 6 have to be considered together, as they are not independent)
 * equivalence classes (EQs) with each contributing some part to the final score, have to be considered.<br>
 * Each of the five EQs has a set of relevant attributes and other properties that make their contribution unique form
 * the other EQs.<br>
 * This class serves as the base interface for these separate operations.
 */
public interface EqOperations {
    String[] getHighestSeverityVectors(Cvss4P0MacroVector thisMacroVector);

    List<Consumer<Cvss4P0>> getHighestSeverityVectorsFn(Cvss4P0MacroVector thisMacroVector);

    String[] getRelevantAttributes();

    Cvss4P0MacroVector[] deriveNextLowerMacro(Cvss4P0MacroVector thisMacroVector);

    double lookupScoresForNextLowerMacro(Cvss4P0MacroVector[] nextLowerMacro);

    /**
     * Max severity distance (vector depth) within the equivalence set that the macro vector represents.
     *
     * @param thisMacroVector the macro vector to look up the depth for
     * @return the depth of the equivalence set in integers.
     */
    int lookupMacroVectorDepth(Cvss4P0MacroVector thisMacroVector);

    static EqOperations[] getEqImplementations() {
        return new EqOperations[]{
                EqOperations1245.EqOperations1.getInstanceEq1(),
                EqOperations1245.EqOperations2.getInstanceEq2(),
                EqOperations1245.EqOperations4.getInstanceEq4(),
                EqOperations1245.EqOperations5.getInstanceEq5(),
                EqOperations36.getInstance()
        };
    }
}
