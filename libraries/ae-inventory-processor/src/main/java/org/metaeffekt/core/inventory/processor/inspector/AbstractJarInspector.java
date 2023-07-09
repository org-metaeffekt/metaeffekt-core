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
package org.metaeffekt.core.inventory.processor.inspector;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Properties;

/**
 * Abstract {@link ArtifactInspector} with basis support for JARs (Java Archives).
 */
public abstract class AbstractJarInspector implements ArtifactInspector {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJarInspector.class);

    protected File getJarFile(Artifact artifact, ProjectPathParam param) {
        // add all existing regular files to file list for later processing.
        String jarPath = artifact.get(ArtifactUnwrapTask.ATTRIBUTE_KEY_ARTIFACT_PATH);

        if (StringUtils.isEmpty(jarPath)) {
            jarPath = artifact.getProjects().stream().findFirst().orElse(null);
        }

        if (jarPath != null) {
            final File jarFile = new File(param.getProjectPath(), jarPath);
            if (jarFile.exists() && jarFile.isFile() && isZipArchive(jarFile)) {
                return jarFile;
            }
        }

        return null;
    }

    private boolean isZipArchive(final File file) {
        if (file.isDirectory()) return false;
        if (!file.exists()) return false;
        if (file.length() < 4) return false;

        boolean isZipArchive = false;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long magic = randomAccessFile.readInt();
            if (magic == 0x504B0304) {
                isZipArchive = true;
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("IOException while inspecting file [{}] for zip characteristics.", file);
            }
            return false;
        }
        return isZipArchive;
    }

    protected void addError(Artifact artifact, String errorString) {
        artifact.append("Errors", errorString, ", ");
    }

    public abstract void run(Inventory inventory, Properties properties);
}
