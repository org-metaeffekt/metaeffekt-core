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

public abstract class EqOperations1245 implements EqOperations {

    @Override
    public Cvss4P0MacroVector[] deriveNextLowerMacro(Cvss4P0MacroVector thisMacroVector) {
        return new Cvss4P0MacroVector[]{thisMacroVector.deriveNextLower(getEqNumber())};
    }

    /**
     * Array always has size 1, because EQs 1, 2, 4, 5 are independent of each other and no combinations have to be
     * considered.
     *
     * @param nextLowerMacro the next lower macro vector
     * @return the score for the next lower macro vector
     */
    @Override
    public double lookupScoresForNextLowerMacro(Cvss4P0MacroVector[] nextLowerMacro) {
        return nextLowerMacro[0].getLookupTableScore();
    }

    @Override
    public String[] getHighestSeverityVectors(Cvss4P0MacroVector thisMacroVector) {
        return getEq(thisMacroVector).getHighestSeverityVectorsUnparsed();
    }

    @Override
    public int lookupMacroVectorDepth(Cvss4P0MacroVector thisMacroVector) {
        return getEq(thisMacroVector).getVectorDepth();
    }

    public abstract int getEqNumber();

    public abstract Cvss4P0MacroVector.EQ getEq(Cvss4P0MacroVector thisMacroVector);

    public static class EqOperations1 extends EqOperations1245 {
        @Override
        public Cvss4P0MacroVector.EQ getEq(Cvss4P0MacroVector thisMacroVector) {
            return thisMacroVector.getEq1();
        }

        @Override
        public String[] getRelevantAttributes() {
            return new String[]{"AV", "PR", "UI"};
        }

        @Override
        public int getEqNumber() {
            return 1;
        }
    }

    public static class EqOperations2 extends EqOperations1245 {
        @Override
        public Cvss4P0MacroVector.EQ getEq(Cvss4P0MacroVector thisMacroVector) {
            return thisMacroVector.getEq2();
        }

        @Override
        public String[] getRelevantAttributes() {
            return new String[]{"AC", "AT"};
        }

        @Override
        public int getEqNumber() {
            return 2;
        }
    }

    public static class EqOperations4 extends EqOperations1245 {
        @Override
        public Cvss4P0MacroVector.EQ getEq(Cvss4P0MacroVector thisMacroVector) {
            return thisMacroVector.getEq4();
        }

        @Override
        public String[] getRelevantAttributes() {
            return new String[]{"SC", "SI", "SA"};
        }

        @Override
        public int getEqNumber() {
            return 4;
        }
    }

    public static class EqOperations5 extends EqOperations1245 {
        @Override
        public Cvss4P0MacroVector.EQ getEq(Cvss4P0MacroVector thisMacroVector) {
            return thisMacroVector.getEq5();
        }

        @Override
        public String[] getRelevantAttributes() {
            return new String[]{};
        }

        @Override
        public int getEqNumber() {
            return 5;
        }
    }

    private static final EqOperations1 instanceEq1;
    private static final EqOperations2 instanceEq2;
    private static final EqOperations4 instanceEq4;
    private static final EqOperations5 instanceEq5;

    static {
        instanceEq1 = new EqOperations1();
        instanceEq2 = new EqOperations2();
        instanceEq4 = new EqOperations4();
        instanceEq5 = new EqOperations5();
    }

    public static EqOperations1 getInstanceEq1() {
        return instanceEq1;
    }

    public static EqOperations2 getInstanceEq2() {
        return instanceEq2;
    }

    public static EqOperations4 getInstanceEq4() {
        return instanceEq4;
    }

    public static EqOperations5 getInstanceEq5() {
        return instanceEq5;
    }
}
