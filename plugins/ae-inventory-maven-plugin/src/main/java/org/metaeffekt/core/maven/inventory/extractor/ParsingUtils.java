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
package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Constants;

import java.util.List;

public class ParsingUtils {

    protected static int getIndex(List<String> lines, String key) {
        int index = -1;
        for (String line : lines) {
            index++;
            if (line.startsWith(key)) {
                return index;
            }
        }
        return -1;
    }

    protected static String getValue(List<String> lines, String key) {
        int index = getIndex(lines, key);
        if (index < 0) return null;
        if (lines.size() < index) return null;
        String line = lines.get(index);
        int colonIndex = line.indexOf(Constants.DELIMITER_COLON);
        if (colonIndex < 0) return null;
        StringBuilder sb = new StringBuilder(line.substring(colonIndex + 1).trim());
        int lineIndex = index + 1;
        while (lineIndex < lines.size() && !lines.get(lineIndex).contains("" + Constants.DELIMITER_COLON)) {
            if (StringUtils.isNotBlank(sb)) sb.append(Constants.DELIMITER_NEWLINE);
            line = lines.get(lineIndex).trim();

            // filter the debian specific lines with '.'
            if (!".".equalsIgnoreCase(line)) {
                sb.append(line);
            }
            lineIndex++;
        }
        return sb.toString().trim();

        // NOTE: potential flaw:
        //  - multiline values are read until the next line with a colon in detected.
    }

}
