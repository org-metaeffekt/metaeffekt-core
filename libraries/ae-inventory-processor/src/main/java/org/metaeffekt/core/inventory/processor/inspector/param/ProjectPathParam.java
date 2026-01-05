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
package org.metaeffekt.core.inventory.processor.inspector.param;

import java.io.File;
import java.util.Properties;

public class ProjectPathParam {
    // where the jar files are found. this is called the scanDirectory in other classes
    public static final String KEY_PROJECT_PATH = "project.path";

    private final File projectPath;

    public ProjectPathParam(Properties properties) {
        projectPath = new File(properties.getProperty(KEY_PROJECT_PATH, "./"));
    }

    public File getProjectPath() {
        return projectPath;
    }

}
