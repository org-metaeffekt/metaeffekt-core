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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileScanTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanTask.class);


    private File file;

    public FileScanTask(File file) {
        this.file = file;
    }

    @Override
    public void process(FileSystemScan fileSystemScan) {
        LOG.info("Executing " + getClass().getName() + " on: " + file);

        final List<String> errors = new ArrayList<>();

        // check whether we want to unwrap (implicit unwrap, scan directive)
        boolean unpacked = false;
        if (fileSystemScan.getScanParam().isImplicitUnwrap()) {

            boolean unwraps = fileSystemScan.getScanParam().unwraps(file.getAbsolutePath());
            if (unwraps) {
                // unknown or requires expansion
                final File targetFolder = new File(file.getParentFile(), "[" + file.getName() + "]");
                if (unpackIfPossible(file, targetFolder, false, errors)) {
                    unpacked = true;
                    fileSystemScan.push(new DirectoryScanTask(targetFolder));
                }
            }
        }

        if (!unpacked) {
            // check whether the file should be added to the inventory
            final Artifact artifact = new Artifact();
            artifact.setId(file.getName());
            artifact.addProject(file.getPath());
            artifact.setChecksum(FileUtils.computeChecksum(file));
            fileSystemScan.getInventory().getArtifacts().add(artifact);
        }
    }

    private boolean unpackIfPossible(File file, File targetDir, boolean includeModules, List<String> issues) {
        if (!includeModules) {
            if (file == null || file.getName().toLowerCase().endsWith(".jar")) {
                return false;
            }
        }
        return ArchiveUtils.unpackIfPossible(file, targetDir, issues);
    }

}
