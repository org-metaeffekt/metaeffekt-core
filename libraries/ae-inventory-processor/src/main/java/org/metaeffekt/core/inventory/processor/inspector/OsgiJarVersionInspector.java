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


import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class OsgiJarVersionInspector extends AbstractJarInspector {
    protected String getVersion(File jarFile) {
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarFile.toPath()))) {
            Manifest manifest = jarInputStream.getManifest();

            if (manifest == null) {
                return null;
            }

            Attributes mainAttributes = manifest.getMainAttributes();

            if (mainAttributes == null) {
                // can't get main attributes
                return null;
            }

            String versionCandidate = mainAttributes.getValue("Bundle-Version");

            // return null if null or empty, otherwise return the stripped (better trim) version string.
            return StringUtils.stripToNull(versionCandidate);
        } catch (IOException e) {
            // ignore
        }

        // by default assume that nothing was found
        return null;
    }

    /**
     * Run completion step for the artifact of this inspector.<br>
     * Completion requires that at least the id is already filled to compare with the file name.
     */
    @Override
    public void run(Inventory inventory, Properties properties) {
        // process required parameters
        ProjectPathParam projectPathParam = new ProjectPathParam(properties);

        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
            // do not process if artifact already has a version
            if (StringUtils.isNotBlank(artifact.getVersion())) {
                return;
            }

            // grab version from manifest
            File jarFile = getJarFile(artifact, projectPathParam);

            if (jarFile == null) {
                // catch if jarfile is null and skip this artifact
                continue;
            }

            String versionCandidate = getVersion(jarFile);

            // check against filename (id) and fill result on match
            if (versionCandidate != null && artifact.getId().endsWith(versionCandidate + ".jar")) {
                artifact.setVersion(versionCandidate);
            }
        }
    }
}
