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
package org.metaeffekt.core.security.cvss;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.util.ColorScheme;

import static org.junit.Assert.fail;

public class CvssSeverityRangesTest {

    @Test
    public void parseRangesTest() {
        new CvssSeverityRanges("Undefined:strong-gray:-100.0:100.0");
        new CvssSeverityRanges("Low:strong-red:0.0:3.9,Medium:strong-red:4.0:6.9,High:strong-red:7.0:10.0");
        new CvssSeverityRanges("Low:strong-yellow:0.0:2.9,Average:strong-light-orange:3.0:6.9,Strong:strong-dark-orange:7.0:8.9,Maximum:strong-red:9.0:10.0");
        parseMustFail("Color should not have existed", "Undefined:dummy-color:-100.0:100.0");
        parseMustFail("Range should not have been valid", "Undefined:strong-gray:3.5:2.1");
        parseMustFail("Too few parts", "Undefined:strong-gray:-100.0");
        parseMustFail("Too few parts", "dummy-color:-100.0:100.0");
        parseMustFail("Too few parts", "dummy-color:-100.0");
        parseMustFail("Too few parts", "dummy-color");
        parseMustFail("Too few parts", "Low:strong-red:0.0:3.9,Medium:4.0:6.9,High:strong-red:7.0:10.0");
        parseMustFail("Wrong delimiter ';'", "Low:strong-red;0.0:3.9,Medium:strong-red:4.0:6.9,High:strong-red:7.0:10.0");
        parseMustFail("Empty part", "Low:strong-red:0.0:3.9,Medium:strong-red:4.0:6.9,:strong-red:7.0:10.0");
        parseMustFail("Empty part", "Low:strong-red:0.0:3.9,Medium:strong-red:4.0:6.9,High::7.0:10.0");
        parseMustFail("Empty part", "Low:strong-red:0.0:3.9,Medium:strong-red:4.0:6.9,High:strong-red::10.0");
        parseMustFail("Empty part", "Low:strong-red:0.0:3.9,Medium:strong-red:4.0:6.9,High:strong-red:7.0:");
    }

    private void parseMustFail(String message, String rangeString) {
        boolean success;
        try {
            new CvssSeverityRanges(rangeString);
            success = true;
        } catch (IllegalArgumentException e) {
            success = false;
        }
        if (success) {
            fail(message);
        }
    }

    @Test
    public void parseRangesColorTest() {
        for (ColorScheme value : ColorScheme.values()) {
            try {
                new CvssSeverityRanges("Undefined:strong-gray:0.0:100.0");
            } catch (Exception e) {
                fail("Color " + value.getCssRootName() + " should have existed");
            }
        }
        for (ColorScheme value : ColorScheme.values()) {
            try {
                new CvssSeverityRanges("strong-gray:0.0:100.0");
                fail("Missing part with color " + value.getCssRootName() + " should not have been parsed");
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void scoreTest() {
        Assert.assertEquals("Undefined", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(-10.0).getName());
        Assert.assertEquals("Undefined", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(-0.1).getName());
        Assert.assertEquals("Low", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(0.0).getName());
        Assert.assertEquals("Low", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(3.9).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(4.0).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(5.0).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(6.9).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(7.0).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(8.2).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_2_SEVERITY_RANGES.getRange(10.0).getName());

        Assert.assertEquals("Undefined", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(-10.0).getName());
        Assert.assertEquals("Undefined", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(-0.1).getName());
        Assert.assertEquals("None", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(0.0).getName());
        Assert.assertEquals("Low", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(0.1).getName());
        Assert.assertEquals("Low", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(3.9).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(4.0).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(5.0).getName());
        Assert.assertEquals("Medium", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(6.9).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(7.0).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(8.2).getName());
        Assert.assertEquals("High", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(8.9).getName());
        Assert.assertEquals("Critical", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(9.0).getName());
        Assert.assertEquals("Critical", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(9.5).getName());
        Assert.assertEquals("Critical", CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.getRange(10.0).getName());
    }
}