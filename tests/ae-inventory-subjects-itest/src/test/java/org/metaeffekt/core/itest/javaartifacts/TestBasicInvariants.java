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
package org.metaeffekt.core.itest.javaartifacts;

import org.metaeffekt.core.itest.common.Preparer;
import org.metaeffekt.core.itest.genericTests.CheckInvariants;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.inventory.processor.model.Inventory;

public abstract class TestBasicInvariants implements AnalysisTemplate {

    static protected Preparer preparer;

    private Inventory inventory;

    private Analysis analysis;

    public Inventory getInventory() throws Exception {
        if (inventory == null) {
            this.inventory = preparer.getInventory();
        }
        return inventory;
    }

    public Analysis getAnalysis() {
        try {
            if (analysis == null) {
                this.analysis = new Analysis(getInventory());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return analysis;
    }

    public Analysis getAnalysisAfterInvariants() {
        CheckInvariants.assertInvariants(getAnalysis());
        return analysis;
    }
}
