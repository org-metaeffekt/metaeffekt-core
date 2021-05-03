/**
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.model;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;

import static java.lang.Boolean.FALSE;

public class LicenseDataTest {

    @Test
    public void testLicenseData() {
        LicenseData ld = new LicenseData();
        ld.set(LicenseData.Attribute.CANONICAL_NAME, "GNU Lesser General Public License 2.1");
        ld.set(LicenseData.Attribute.ID, "LGPL-2.1");
        ld.set(LicenseData.Attribute.SPDX_ID, "LGPL-2.1-only");
        ld.set(LicenseData.Attribute.COMMERCIAL, FALSE);
        ld.set(LicenseData.Attribute.COPYLEFT_TYPE, "limited");

        Assert.assertEquals("GNU Lesser General Public License 2.1-LGPL-2.1", ld.deriveQualifier());
        Assert.assertEquals("GNU Lesser General Public License 2.1:LGPL-2.1:LGPL-2.1-only::limited:false:",
                ld.createCompareStringRepresentation());

        Inventory inventory = new Inventory();
        inventory.getRepresentedLicenseNames(false).forEach(System.out::println);
        //inventory.evaluateLicenses(false).forEach(System.out::println);
    }


}
