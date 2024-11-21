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
package org.metaeffekt.core.model;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.LicenseData;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.*;

public class LicenseDataTest {

    @Test
    public void testLicenseData_AtomicNoOption() {
        final LicenseData ld = new LicenseData();
        ld.set(LicenseData.Attribute.CANONICAL_NAME, "GNU Lesser General Public License 2.1");
        ld.set(LicenseData.Attribute.ID, "LGPL-2.1");
        ld.set(LicenseData.Attribute.SPDX_ID, "LGPL-2.1-only");
        ld.set(LicenseData.Attribute.COMMERCIAL, FALSE);
        ld.set(LicenseData.Attribute.COPYLEFT_TYPE, "limited");

        assertEquals("GNU Lesser General Public License 2.1-LGPL-2.1",
                ld.deriveQualifier());
        assertEquals("GNU Lesser General Public License 2.1:LGPL-2.1:LGPL-2.1-only:",
                ld.createCompareStringRepresentation());

        assertTrue(ld.isAtomic());
        assertFalse(ld.isOption());
    }

    @Test
    public void testLicenseData_AtomicAndOption() {
        final LicenseData ld = new LicenseData();
        ld.set(LicenseData.Attribute.CANONICAL_NAME, "GNU Lesser General Public License 2.1 (or any later version)");
        ld.set(LicenseData.Attribute.ID, "LGPL-2.1+");
        ld.set(LicenseData.Attribute.SPDX_ID, "LGPL-2.1-or-later");
        ld.set(LicenseData.Attribute.COMMERCIAL, FALSE);
        ld.set(LicenseData.Attribute.COPYLEFT_TYPE, "limited");

        assertEquals("GNU Lesser General Public License 2.1 (or any later version)-LGPL-2.1+",
                ld.deriveQualifier());
        assertEquals("GNU Lesser General Public License 2.1 (or any later version):LGPL-2.1+:LGPL-2.1-or-later:",
                ld.createCompareStringRepresentation());

        assertTrue(ld.isAtomic());
        assertTrue(ld.isOption());
    }

    @Test
    public void testLicenseData_NotAtomicButOption() {
        final LicenseData ld = new LicenseData();
        ld.set(LicenseData.Attribute.CANONICAL_NAME, "BSD 3-Clause License + GNU General Public License 3.0");
        ld.set(LicenseData.Attribute.ID, "BSD-3-Clause + GPL-3.0+");

        assertEquals("BSD 3-Clause License + GNU General Public License 3.0-BSD-3-Clause + GPL-3.0+",
                ld.deriveQualifier());
        assertEquals("BSD 3-Clause License + GNU General Public License 3.0:BSD-3-Clause + GPL-3.0+::",
                ld.createCompareStringRepresentation());

        assertFalse(ld.isAtomic());
        assertTrue(ld.isOption());
    }

}
