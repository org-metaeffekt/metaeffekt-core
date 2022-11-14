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
package org.metaeffekt.core.common.kernel.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * General utilities with regards to parameter conversion.
 *
 * @author Karsten Klein
 */
public class ParameterConversionUtil {

    /**
     * Converts a string separated by a delimiter regular expression into a list of strings. The
     * methods removed trailing whitespaces (which is often the case for multiline configuration
     * in xml) and does not include empty strings.
     *
     * @param string to separate. May be <code>null</code>.
     * @param delimiterRegExp that separates the string into several parts
     *
     * @return A list of trimmed strings. Potentially empty, but never <code>null</code>.
     */
    public static List<String> convertStringToStringList(String string, String delimiterRegExp) {
        List<String> list = new ArrayList<String>();
        if (string != null) {
            String[] splits = string.split(delimiterRegExp);
            for (int i = 0; i < splits.length; i++) {
                if (StringUtils.hasText(splits[i])) {
                    list.add(splits[i].trim());
                }
            }
        }
        return list;
    }

    /**
     * Converts a string separated by a delimiter regular expression into a string array. The
     * methods removed trailing whitespaces (which is often the case for multiline configuration
     * in xml) and does not include empty strings.
     *
     * @param string to separate. May be <code>null</code>.
     * @param delimiterRegExp that separates the string into several parts
     *
     * @return A list of trimmed strings. Potentially empty, but never <code>null</code>.
     */
    public static String[] convertStringToStringArray(String string, String delimiterRegExp) {
        List<String> list = convertStringToStringList(string, delimiterRegExp);
        return list.toArray(new String[list.size()]);
    }


}
