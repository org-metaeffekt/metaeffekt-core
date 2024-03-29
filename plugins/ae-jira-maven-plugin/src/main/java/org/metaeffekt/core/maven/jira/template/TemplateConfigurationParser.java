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
package org.metaeffekt.core.maven.jira.template;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateConfigurationParser {

    private static final Pattern PATTERN_DEFINITION =
            Pattern.compile(".*##\\s+([A-Za-z][A-Za-z0-9_]+):\\s*(.*)");

    public static Map<String, String> parse(File file) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String line : FileUtils.readLines(file, "UTF-8")) {
            Matcher matcher = PATTERN_DEFINITION.matcher(line);
            if (matcher.find()) {
                result.put(matcher.group(1), matcher.group(2).trim());
            }
        }
        return result;
    }
}
