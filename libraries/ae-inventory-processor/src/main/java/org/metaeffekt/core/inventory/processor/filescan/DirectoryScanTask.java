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

import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DirectoryScanTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryScanTask.class);

    private final File dir;

    public DirectoryScanTask(File dir) {
        this.dir = dir;
    }

    @Override
    public void process(FileSystemScan fileSystemScan) {
        LOG.info("Executing " + getClass().getName() + " on: " + dir);

        try {
            final File[] files = dir.listFiles();
            final ScanParam scanParam = fileSystemScan.getScanParam();

            for (File file : files) {
                final String path = FileUtils.normalizePathToLinux(file.getAbsolutePath());
                if (scanParam.collects(path)) {
                    if (file.isFile()) {
                        fileSystemScan.push(new FileScanTask(file));
                    } else {
                        fileSystemScan.push(new DirectoryScanTask(file));
                    }
                }
            }
        } catch (Exception e) {
            // TODO: mark errors
        }
    }

}
