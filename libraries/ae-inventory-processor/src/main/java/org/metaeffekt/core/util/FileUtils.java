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
package org.metaeffekt.core.util;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Checksum;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * FileUtils extension.
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String ENCODING_UTF_8 = "UTF-8";

    private static final String VAR_CHECKSUM = "checksum";

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static final String SEPARATOR_SLASH = "/";
    public static final String SEPARATOR_COMMA = ",";

    public static final char SEPARATOR_SLASH_CHAR = '/';

    private static Pattern NORMALIZE_PATH_PATTERN_001 = Pattern.compile("/./");
    private static Pattern NORMALIZE_PATH_PATTERN_002 = Pattern.compile("^\\./");
    private static Pattern NORMALIZE_PATH_PATTERN_003 = Pattern.compile("/[^/]*/\\.\\./");

    /**
     * Scans the given baseDir for files matching the includes and excludes.
     *
     * @param baseDir The directory to scan.
     * @param includes Include patterns separated by ','.
     * @param excludes Exclude patterns separated by ','.
     *
     * @return Array of file paths relative to baseDir.
     */
    public static String[] scanForFiles(File baseDir, String includes, String excludes) {
        if (baseDir.exists()) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(baseDir);
            if (includes != null) {
                scanner.setIncludes(includes.split(", ?"));
            }
            if (excludes != null) {
                scanner.setExcludes(excludes.split(", ?"));
            }
            scanner.setFollowSymlinks(false);
            scanner.scan();
            return scanner.getIncludedFiles();
        }
        return EMPTY_STRING_ARRAY;
    }

    public static String computeChecksum(File file) {
        return computeChecksum(file, "MD5");
    }

    public static String computeMD5Checksum(File file) {
        return computeChecksum(file, "MD5");
    }

    /**
     * The thread-local enables to reuse the Ant Checksum instance per thread.
     */
    private static ThreadLocal<Checksum> checksumThreadLocal = new ThreadLocal<>();

    public static String computeChecksum(File file, String algorithm) {
        try {
            final Checksum checksum = getChecksumInstance();
            // cannot reuse the project
            checksum.setProject(new Project());
            checksum.setFile(file);
            checksum.setAlgorithm(algorithm);
            checksum.execute();
            return checksum.getProject().getProperty(VAR_CHECKSUM);
        } catch (Exception e) {
            return null;
        }
    }

    private static Checksum getChecksumInstance() {
        Checksum checksum = checksumThreadLocal.get();
        if (checksum == null) {
            checksum = new Checksum();
            checksum.setProperty(VAR_CHECKSUM);
            checksumThreadLocal.set(checksum);
        }
        return checksum;
    }

    public static String computeSHA1Hash(File file) {

        return computeChecksum(file, "SHA-1");
    }

    public static String computeSHA256Hash(File file) {
        return computeChecksum(file, "SHA-256");
    }

    public static String computeSHA512Hash(File file) {
        return computeChecksum(file, "SHA-512");
    }

    public static String asRelativePath(String workingDirPath, String filePath) {
        try {
            File file = new File(filePath).getCanonicalFile();
            File workingDirFile = new File(workingDirPath).getCanonicalFile();
            return asRelativePath(workingDirFile, file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot compute relative path for " + filePath + ".", e);
        }
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
                path = commonBaseDir.getName() + SEPARATOR_SLASH + path;
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
            index++;
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


    public static boolean matches(final String normalizedPattern, final String normalizedPath) {
        if (normalizedPattern == null) return true;
        if (normalizedPath == null) return false;

        if (!normalizedPattern.contains(SEPARATOR_COMMA)) {
            final String trimmedPattern = normalizedPattern.trim();
            return internalMatching(normalizedPath, trimmedPattern);
        }

        final String[] patterns = normalizedPattern.split(SEPARATOR_COMMA);
        for (final String pattern : patterns) {
            final String trimmedPattern = pattern.trim();
            if (internalMatching(normalizedPath, trimmedPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The AntPathMatcher has the unexpected behavior to treat absolute paths differently. These would only match in
     * case the pattern is also absolute. This method adapts pattern and path to reach the anticipated results.
     *
     * @param normalizedPath Normalized path to match.
     * @param normalizedPattern Normalized pattern to match.
     *
     * @return <code>true</code> in case the pattern matches the path.
     */
    private static boolean xinternalMatching(String normalizedPath, String normalizedPattern) {
        if (normalizedPattern.startsWith(SEPARATOR_SLASH)) {
            // NOTE: many patterns are in the shape **/*.<suffix> and **/<sub-path>/**/*; these could be optimized
            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern.substring(1), normalizedPath);
            }
        } else {
            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath.substring(1));
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            }
        }
    }

    private static boolean internalMatching(final String normalizedPath, final String normalizedPattern) {

        // match on string equals level when no wildcard is contained
        if (!normalizedPattern.contains("*")) {
            return normalizedPath.equals(normalizedPattern);
        }

        if (normalizedPattern.startsWith(SEPARATOR_SLASH)) {
            // matching absolute path; check whether this is at all needed; i.e. by static defined component patterns

            if (!normalizedPath.contains(":")) {
                final Boolean matched = matchStandardPatternAnyFileInPath(normalizedPath, normalizedPattern);
                if (matched != null) return matched;
            }

            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern.substring(1), normalizedPath);
            }
        } else {

            if (!normalizedPath.contains(":")) {
                if (normalizedPattern.startsWith("**/")) {
                    final String subPattern = normalizedPattern.substring(2);
                    if (!subPattern.contains("*") && !subPattern.contains(":")) {
                        return normalizedPath.endsWith(subPattern);
                    }
                } else {
                    final Boolean matched = matchStandardPatternAnyFileInPath(normalizedPath, normalizedPattern);
                    if (matched != null) return matched;
                }
            }

            if (normalizedPath.startsWith(SEPARATOR_SLASH)) {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath.substring(1));
            } else {
                return ANT_PATH_MATCHER.match(normalizedPattern, normalizedPath);
            }
        }
    }

    private static Boolean matchStandardPatternAnyFileInPath(String normalizedPath, String normalizedPattern) {
        if (normalizedPattern.endsWith("/**/*")) {
            final String subPattern = normalizedPattern.substring(0, normalizedPattern.length() - 4);
            if (!subPattern.contains("*") && !subPattern.contains(":")) {
                return normalizedPath.startsWith(subPattern);
            }
        }

        // return null to indicate that match was not evaluated
        return null;
    }

    public static boolean matches(final Set<String> normalizedPatternSet, final String normalizedPath) {
        if (normalizedPath == null) return false;
        for (final String pattern : normalizedPatternSet) {
            final String trimmedPattern = pattern.trim();
            if (internalMatching(normalizedPath, trimmedPattern)) {
                return true;
            }
        }
        return false;
    }

    public static String[] normalizePatterns(String[] patterns) {
        if (patterns == null) return null;
        final String[] normalizedPatterns = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            normalizedPatterns[i] = normalizePathToLinux(patterns[i]);
        }
        return normalizedPatterns;
    }

    public static String normalizePathToLinux(String path) {
        if (path == null) return null;
        return path.replace('\\', SEPARATOR_SLASH_CHAR);
    }

    public static String normalizePathToLinux(File file) {
        if (file == null) return null;
        return normalizePathToLinux(file.getPath());
    }

    public static void waitForProcess(Process p) {
        try {
            while (p.isAlive()) {
                p.waitFor(1000, TimeUnit.MILLISECONDS);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    IOUtils.copy(p.getInputStream(), baos);
                } catch (IOException e) {
                    LOG.error("Unable to copy input stream into output stream.", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void deleteDirectoryQuietly(File directory) {
        try {
            forceDelete(directory);
        } catch (UncheckedIOException | IOException e) {
            // ignore
        }
    }

    public static String canonicalizeLinuxPath(String path) {
        path = NORMALIZE_PATH_PATTERN_001.matcher(path).replaceAll(SEPARATOR_SLASH);
        path = NORMALIZE_PATH_PATTERN_001.matcher(path).replaceAll(SEPARATOR_SLASH);
        path = NORMALIZE_PATH_PATTERN_002.matcher(path).replaceAll("");

        while (path.contains("/../")) {
            path = NORMALIZE_PATH_PATTERN_003.matcher(path).replaceAll(SEPARATOR_SLASH);
        }

        return path;
    }

    public static String[] scanDirectoryForFolders(File targetDir, String... includes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedDirectories();
    }

    public static File findSingleFile(File baseFile, String pattern) {
        final String[] files = FileUtils.scanDirectoryForFiles(baseFile, pattern);
        if (files.length == 1) {
            return new File(baseFile, files[0]);
        }
        return null;
    }

    public static String[] scanDirectoryForFiles(File targetDir, String... includes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static String[] scanDirectoryForFiles(File targetDir, String[] includes, String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static void createDirectoryContentChecksumFile(File baseDir, File targetContentChecksumFile) throws IOException {
        final StringBuilder checksumSequence = new StringBuilder();
        final String[] files = FileUtils.scanDirectoryForFiles(baseDir, new String[]{"**/*"}, new String[]{"**/.DS_Store*"});

        Arrays.stream(files).map(FileUtils::normalizePathToLinux).sorted(String::compareTo).forEach(fileName -> {
            final File file = new File(baseDir, fileName);
            try {
                final String fileChecksum = FileUtils.computeChecksum(file);
                if (checksumSequence.length() > 0) {
                    checksumSequence.append(0);
                }
                checksumSequence.append(fileChecksum);
            } catch (Exception e) {
                if (FileUtils.isSymlink(file)) {
                    LOG.warn("Cannot compute checksum for symbolic link file [{}].", file);
                } else {
                    LOG.warn("Cannot compute checksum for file [{}].", file);
                }
            }
        });
        FileUtils.writeStringToFile(targetContentChecksumFile, checksumSequence.toString(), FileUtils.ENCODING_UTF_8);
    }

    public static void validateExists(File file) {
        if (!file.exists()) {
            throw new IllegalStateException(String.format("File '%s' does not exist.", file.getAbsolutePath()));
        }
    }

    public static File combine(File baseDir, String relativePath) {
        if (".".equals(relativePath)) {
            return baseDir;
        } else {
            return new File(baseDir, relativePath);
        }
    }

    public static File initializeTmpFolder(File targetDir) {
        final File tmpFolder = new File(targetDir.getParentFile(), ".tmp");
        if (tmpFolder.exists()) {
            try {
                FileUtils.deleteDirectory(tmpFolder);
            } catch (IOException e) {
                LOG.error("Failed to delete tmp folder.", e);
            }
        }
        tmpFolder.mkdirs();
        return tmpFolder;
    }

}
