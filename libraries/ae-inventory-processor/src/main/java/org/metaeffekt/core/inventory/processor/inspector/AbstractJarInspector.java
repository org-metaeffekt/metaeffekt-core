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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Properties;

public abstract class AbstractJarInspector implements InspectorInterface {

    protected File getJarFile(Artifact artifact, ProjectPathParam param) {
        // add all existing regular files to file list for later processing.
        final String jarPath = artifact.getProjects().stream().findFirst().orElse(null);

        if (jarPath != null) {
            final File jarFile = new File(param.getProjectPath(), jarPath);
            if (jarFile.exists() && jarFile.isFile() && isZipArchive(jarFile)) {
                return jarFile;
            }
        }

        return null;
    }

    private boolean isZipArchive(File jarFile) {
        if (jarFile.isDirectory()) return false;
        if (!jarFile.exists()) return false;
        if (jarFile.length() < 4) return false;

        boolean isZipArchive = false;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(jarFile, "r")) {
            long magic = randomAccessFile.readInt();
            if (magic == 0x504B0304) {
                isZipArchive = true;
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isZipArchive;
    }

    protected void addError(Artifact artifact, String errorString) {
        artifact.append("Errors", errorString, ", ");
    }

    public abstract void run(Inventory inventory, Properties properties);
}
