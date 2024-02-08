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
package org.metaeffekt.core.itest.common.setup;

import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.setup.TestSetup;
import org.metaeffekt.core.itest.common.asserts.AnalysisAsserts;
import org.metaeffekt.core.itest.common.Analysis;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public abstract class AbstractBasicInvariantsTest implements AnalysisAsserts {

    static protected TestSetup testSetup;

    private Inventory inventory;

    private Analysis analysis;

    protected Analysis getTemplate(String templatepath) throws IOException {
        final URL templateurl = this.getClass().getResource(templatepath);
        final File file = new File(templateurl.getFile());
        final Inventory template = InventoryUtils.readInventory(file, "*.xls");
        final Analysis analysis = new Analysis(template, templatepath);
        return analysis;
    }

    public Inventory getInventory() throws Exception {
        if (inventory == null) {
            this.inventory = testSetup.getInventory();
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

    public Analysis getAnalysisAfterInvariantCheck() {
        assertInvariants();
        return analysis;
    }
}
