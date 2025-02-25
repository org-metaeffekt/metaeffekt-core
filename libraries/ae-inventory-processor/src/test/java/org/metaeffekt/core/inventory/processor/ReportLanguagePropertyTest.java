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
package org.metaeffekt.core.inventory.processor;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.fail;

public class ReportLanguagePropertyTest {

    public static String[] propertyFiles = { "en.properties", "de.properties" };

    @Test
    public void testLangFilesHaveSameKeys() throws IOException {
        Map<String, Set<String>> fileKeys = new HashMap<>();
        Set<String> allKeys = new HashSet<>();

        for (String file : propertyFiles) {
            Properties props = loadProperties(file);
            Set<String> keys = props.stringPropertyNames();
            fileKeys.put(file, keys);
            allKeys.addAll(keys);
        }

        StringBuilder failureMessage = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : fileKeys.entrySet()) {
            Set<String> keys = entry.getValue();
            Set<String> missing = new HashSet<>(allKeys);
            missing.removeAll(keys);

            if (!missing.isEmpty()) {
                failureMessage.append("File '").append(entry.getKey())
                        .append("' is missing keys: ").append(missing).append("\n");
            }
        }

        if (failureMessage.length() > 0) {
            fail("Properties files do not have the same keys:\n" + failureMessage);
        }
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/templates/lang/" + resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            props.load(is);
        }
        return props;
    }
}
