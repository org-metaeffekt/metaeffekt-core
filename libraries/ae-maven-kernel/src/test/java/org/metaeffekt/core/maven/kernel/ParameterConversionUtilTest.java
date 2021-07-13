/*
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
package org.metaeffekt.core.maven.kernel;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;

import java.util.List;


public class ParameterConversionUtilTest {

    private static final String STR_HELLO = "Hello";
    private static final String STR_BEAUTIFUL = "beautiful";
    private static final String STR_WORLD = "World";
    private static final String STR_DELIMITER = ",";
    private static final String SEP_LINE = System.getProperty("line.separator");

    @Test
    public void testEmtpyStringsNotIncluded() {
        String string = STR_HELLO + STR_DELIMITER + STR_DELIMITER + STR_WORLD;
        List<String> splitList = ParameterConversionUtil.convertStringToStringList(string, ",");
        Assert.assertEquals(2, splitList.size());
    }

    @Test
    public void testCanHandleNull() {
        List<String> splitList = ParameterConversionUtil.convertStringToStringList(null, ",");
        Assert.assertEquals(0, splitList.size());
    }

    @Test
    public void testMultilineTime() {
        String string = STR_HELLO + STR_DELIMITER + SEP_LINE + STR_BEAUTIFUL + STR_DELIMITER + SEP_LINE + STR_WORLD;
        String[] splitArray = ParameterConversionUtil.convertStringToStringArray(string, ",");
        Assert.assertEquals(3, splitArray.length);
        Assert.assertEquals(STR_HELLO, splitArray[0]);
        Assert.assertEquals(STR_BEAUTIFUL, splitArray[1]);
        Assert.assertEquals(STR_WORLD, splitArray[2]);
    }

}
