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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InventoryReportTest {

    @Test
    public void testNormalCase() {
        Assertions.assertEquals("normalId", InventoryReport.xmlEscapeStringAttribute("normalId"));
    }

    @Test
    public void testBlankInput() {
        Assertions.assertTrue(InventoryReport.xmlEscapeStringAttribute(" ").startsWith("defaultId"));
    }

    @Test
    public void testInvalidCharacters() {
        Assertions.assertEquals("invalid_Id", InventoryReport.xmlEscapeStringAttribute("invalid!Id"));
        Assertions.assertEquals("invalid_Id", InventoryReport.xmlEscapeStringAttribute("invalid Id"));
    }

    @Test
    public void testStartsWithDigit() {
        Assertions.assertEquals("_1invalidId", InventoryReport.xmlEscapeStringAttribute("1invalidId"));
    }

    @Test
    public void testLongString() {
        String longString = "longStringWithInvalidCharacters!@#$%^&*()\"'";
        String expected = "longStringWithInvalidCharacters____________";
        Assertions.assertEquals(expected, InventoryReport.xmlEscapeStringAttribute(longString));
    }

    @Test
    public void testNullInput() {
        Assertions.assertTrue(InventoryReport.xmlEscapeStringAttribute(null).startsWith("defaultId"));
    }
}
