/*
 * Copyright 2009-2021 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;

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

    public static String asRelativePath(String workingDirPath, String filePath) throws IOException {
        File file = new File(filePath).getCanonicalFile();
        File workingDirFile = new File(workingDirPath).getCanonicalFile();
        return asRelativePath(workingDirFile, file);
    }

    public static String asRelativePath(File workingDirFile, File file) {
        final LinkedHashSet<File> set = new LinkedHashSet<>();
        File commonBaseDir = workingDirFile;
        set.add(commonBaseDir);

        // decompose the working dir in separate files
        while (commonBaseDir.getParentFile() != null) {
            set.add(commonBaseDir.getParentFile());
            commonBaseDir = commonBaseDir.getParentFile();
        }

        // walk down the file path until common base dir is found
        commonBaseDir = file;
        String path = "";
        while (commonBaseDir != null && !set.contains(commonBaseDir)) {
            if (path.length() > 0) {
                path = commonBaseDir.getName() + "/" + path;
            } else {
                path = commonBaseDir.getName();
            }
            commonBaseDir = commonBaseDir.getParentFile();
        }

        // see on which index the common base path lies
        int index = 0;
        for (File pos : set) {
            if (pos.equals(commonBaseDir)) {
                break;
            }
            index ++;
        }

        // move up the path until common base path is reached
        String relativePath = "";
        for (int i = 0; i < index; i++) {
            relativePath = "../" + relativePath;
        }

        // and append the path composed earlier
        relativePath += path;
        if (relativePath.trim().isEmpty()) {
           return Constants.DOT;
        }
        return relativePath;
    }

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    public static boolean matches(final String normalizedPattern, final String normalizedPath) {
        if (normalizedPattern == null) return true;
        if (normalizedPath == null) return false;

        if (!normalizedPattern.contains(",")) {
            return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
        }
        final String[] patterns = normalizedPattern.split(",");
        for (final String pattern : patterns) {
            if (ANT_PATH_MATCHER.match(pattern.trim(), normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizePathToLinux(String path) {
        if (path == null) return null;
        return path.replace("\\", "/");
    }

}
