/**
 * Copyright 2009-2019 the original author or authors.
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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Checksum;

import java.io.File;

/**
 * FileUtils extension.
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String ENCODING_UTF_8 = "UTF-8";
    public static final String VAR_CHECKSUM = "checksum";

    /**
     * Scans the given baseDir for files matching the includes and excludes.
     *
     * @param baseDir The directory to scan.
     * @param includes The include patterns separated by ','.
     * @param excludes The exclude patterns separated by ','.
     * @return Array of file paths relative to baseDir.
     */
    public static String[] scanForFiles(File baseDir, String includes, String excludes) {
        if (baseDir.exists()) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(baseDir);
            scanner.setIncludes(includes.split(","));
            scanner.setExcludes(excludes.split(","));
            scanner.setFollowSymlinks(false);
            scanner.scan();
            return scanner.getIncludedFiles();
        }
        return EMPTY_STRING_ARRAY;
    }

    public static String computeChecksum(File file) {
        // FIXME: currently we use the Ant checksum means (may be not very efficient)
        Checksum checksum = new Checksum();
        checksum.setProject(new Project());
        checksum.setFile(file);
        checksum.setProperty(VAR_CHECKSUM);
        checksum.execute();
        return checksum.getProject().getProperty(VAR_CHECKSUM);
    }
}
