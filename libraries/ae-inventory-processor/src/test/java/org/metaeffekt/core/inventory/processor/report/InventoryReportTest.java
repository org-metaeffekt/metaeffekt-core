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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.Assert;
import org.junit.Test;

public class InventoryReportTest {

    @Test
    public void testNormalCase() {
        Assert.assertEquals("normalId", InventoryReport.xmlEscapeStringAttribute("normalId"));
    }

    @Test
    public void testBlankInput() {
        Assert.assertTrue(InventoryReport.xmlEscapeStringAttribute(" ").startsWith("defaultId"));
    }

    @Test
    public void testInvalidCharacters() {
        Assert.assertEquals("invalid_Id", InventoryReport.xmlEscapeStringAttribute("invalid!Id"));
        Assert.assertEquals("invalid_Id", InventoryReport.xmlEscapeStringAttribute("invalid Id"));
    }

    @Test
    public void testStartsWithDigit() {
        Assert.assertEquals("_1invalidId", InventoryReport.xmlEscapeStringAttribute("1invalidId"));
    }

    @Test
    public void testLongString() {
        String longString = "longStringWithInvalidCharacters!@#$%^&*()\"'";
        String expected = "longStringWithInvalidCharacters____________";
        Assert.assertEquals(expected, InventoryReport.xmlEscapeStringAttribute(longString));
    }

    @Test
    public void testNullInput() {
        Assert.assertTrue(InventoryReport.xmlEscapeStringAttribute(null).startsWith("defaultId"));
    }
}
