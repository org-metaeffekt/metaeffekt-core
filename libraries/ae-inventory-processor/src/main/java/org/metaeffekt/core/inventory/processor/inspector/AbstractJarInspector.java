/*
 * Copyright 2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.inspector;

import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.Properties;

public abstract class AbstractJarInspector implements InspectorInterface {

    // TODO: should this support file types or check that it's really a jar? what could artifact.getProjects() contain?
    protected File getJarFile(Artifact artifact, ProjectPathParam param) {
        // add all existing regular files to file list for later processing.
        String jarPath = artifact.getProjects().stream().findFirst().orElse(null);

        File jarFile = null;
        if (jarPath != null) {
            jarFile = new File(param.getProjectPath(), jarPath);
        }

        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            return null;
        } else {
            return jarFile;
        }
    }

    protected void addError(Artifact artifact, String errorString) {
        artifact.append("Errors", errorString, ", ");
    }

    public abstract void run(Inventory inventory, Properties properties);
}
