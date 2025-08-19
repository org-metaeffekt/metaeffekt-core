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
package org.metaeffekt.core.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public abstract class PropertiesUtils {

    public static Properties loadPropertiesFile(File propertiesFile) {
        return loadPropertiesFile(propertiesFile, false);
    }

    public static Properties loadPropertiesFile(File file, boolean escalateException) {
        final Properties p = new Properties();
        try (final FileReader reader = new FileReader(file)) {
            p.load(reader);
        } catch (IOException e) {
            if (escalateException) {
                throw new IllegalStateException(e);
            } else {
                log.warn("Cannot load properties file [{}].", file.getAbsolutePath());
            }
        }
        return p;
    }

}
