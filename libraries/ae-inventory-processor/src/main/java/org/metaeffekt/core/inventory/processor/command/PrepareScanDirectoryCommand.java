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
package org.metaeffekt.core.inventory.processor.command;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;

public class PrepareScanDirectoryCommand {

    /**
     * Ensures scanDir contains the relevant content copied from sourceDir. The directory scanDir will be completely
     * rebuild. The copy process is filtered by the provided includes and excludes.
     *
     * @param sourceDir The sourceDir containing the content to be scanned.
     * @param scanDir The scanDir is the directory being prepared for scanning.
     * @param includes The includes lists ant-style patterns for copying form sourceDir.
     * @param excludes The includes lists ant-style patterns for copying form sourceDir.
     */
    public void prepareScanDirectory(File sourceDir, File scanDir, String[] includes, String[] excludes) {
        if (scanDir.equals(sourceDir)) {
            return;
        }

        final Project project = new Project();

        // delete scan directory (content is progressively unpacked)
        if (scanDir.exists()) {
            FileUtils.deleteDirectoryQuietly(scanDir);
        }

        // ensure scan directory root folder is recreated
        scanDir.mkdirs();

        // copy files to folder that are of interest
        final Copy copy = new Copy();
        copy.setProject(project);
        FileSet fileSet = new FileSet();
        fileSet.setDir(sourceDir);
        fileSet.setIncludes(combinePatterns(includes, "**/*"));
        fileSet.setExcludes(combinePatterns(excludes, "--nothing--"));
        copy.addFileset(fileSet);
        copy.setTodir(scanDir);
        copy.execute();
    }

    private String combinePatterns(String[] patterns, String defaultPattern) {
        if (patterns == null) return defaultPattern;
        return String.join(",", patterns);
    }

}
