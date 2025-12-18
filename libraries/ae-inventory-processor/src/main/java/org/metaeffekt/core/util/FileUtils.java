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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Checksum;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileUtils extension.
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String ENCODING_UTF_8 = "UTF-8";

    private static final String VAR_CHECKSUM = "checksum";

    public static final String SEPARATOR_SLASH = "/";
    public static final String SEPARATOR_COMMA = ",";

    public static final char SEPARATOR_SLASH_CHAR = '/';

    private static final Pattern SLASH_DOT_SLASH_PATTERN = Pattern.compile("/\\./");
    private static final Pattern DOT_SLASH_PREFIX_PATTERN = Pattern.compile("^\\./");
    private static final Pattern FOLDER_SLASH_DOTDOT_SLASH_PATTERN = Pattern.compile("([^/]*)/\\.\\./");
    private static final Pattern FOLDER_SLASH_DOTDOT_SUFFIX_PATTERN = Pattern.compile("([^/]*)/\\.\\.$");
    private static final Pattern SLASH_SLASH_PATTERN = Pattern.compile("//");

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
    private static final ThreadLocal<Checksum> checksumThreadLocal = new ThreadLocal<>();

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
        File file = new File(FileUtils.canonicalizeLinuxPath(FileUtils.normalizePathToLinux(filePath)));
        File workingDirFile = new File(FileUtils.canonicalizeLinuxPath(FileUtils.normalizePathToLinux(workingDirPath)));
        return asRelativePath(workingDirFile, file);
    }

    public static String asRelativePath(FileRef workingDirPath, FileRef filePath) {
        File file = new File(FileUtils.canonicalizeLinuxPath(filePath.getPath()));
        File workingDirFile = new File(FileUtils.canonicalizeLinuxPath(workingDirPath.getPath()));
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

    // FIXME: further harmonize with PatternSetMatcher; move method; rename to differentiate from PSM.matches()
    public static boolean matches(final String normalizedPattern, final String normalizedPath) {
        if (normalizedPattern == null) return true;
        if (normalizedPath == null) return false;

        if (!normalizedPattern.contains(SEPARATOR_COMMA)) {
            final String trimmedPattern = normalizedPattern.trim();
            return PatternSetMatcher.internalMatching(normalizedPath, trimmedPattern);
        }

        final String[] patterns = normalizedPattern.split(SEPARATOR_COMMA);
        for (final String pattern : patterns) {
            final String trimmedPattern = pattern.trim();
            if (PatternSetMatcher.internalMatching(normalizedPath, trimmedPattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(final Set<String> normalizedPatternSet, final String normalizedPath) {
        if (normalizedPath == null) return false;
        for (final String pattern : normalizedPatternSet) {
            final String trimmedPattern = pattern.trim();
            if (PatternSetMatcher.internalMatching(normalizedPath, trimmedPattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesWithContext(final Set<String> normalizedPatternSet, String normalizedPath, final String relativeBasePath) {
        if (normalizedPath == null) return false;
        if (normalizedPath.startsWith(relativeBasePath)) {
            normalizedPath = normalizedPath.substring(relativeBasePath.length() + 1);
            for (final String pattern : normalizedPatternSet) {
                final String trimmedPattern = pattern.trim();
                if (PatternSetMatcher.internalMatching(normalizedPath, trimmedPattern)) {
                    return true;
                }
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
        path = path.replace('\\', SEPARATOR_SLASH_CHAR);
        if (path.length() > 2 && path.endsWith("/.")) {
            path = path.substring(0, path.length() - 2);
        }
        if (path.equals("./")) {
            path = ".";
        }
        return path;
    }

    public static String normalizePathToLinux(File file) {
        if (file == null) return null;
        return normalizePathToLinux(file.getPath());
    }

    public static void deleteDirectoryQuietly(File directory) {
        try {
            forceDelete(directory);
        } catch (UncheckedIOException | IOException e) {
            // ignore
        }
    }

    public static String normalizeToLinuxPathAndCanonicalizePath(String path) {
        return canonicalizeLinuxPath(normalizePathToLinux(path));
    }

    public static String canonicalizeLinuxPath(String path) {
        final String originalPath = path;

        // replace /./ by /
        path = RegExUtils.replaceAll(path, SLASH_DOT_SLASH_PATTERN, SEPARATOR_SLASH);

        // replace // by /
        path = RegExUtils.replaceAll(path, SLASH_SLASH_PATTERN, SEPARATOR_SLASH);
        validatePath(path, originalPath);

        // the set is meant to detect, when no change is applied
        final Set<String> previousVersions = new HashSet<>();

        // replace <folder>/../ constructs
        path = replaceFolderDotDotConstruct(path, previousVersions, FOLDER_SLASH_DOTDOT_SLASH_PATTERN, originalPath);
        validatePath(path, originalPath);

        // replace <folder>/..$ constructs
        path = replaceFolderDotDotConstruct(path, previousVersions, FOLDER_SLASH_DOTDOT_SUFFIX_PATTERN, originalPath);
        validatePath(path, originalPath);

        // eliminate prefixed ./
        path = RegExUtils.replaceAll(path, DOT_SLASH_PREFIX_PATTERN, "");

        // remove trailing / an any case
        if (path.length() > 1 && path.endsWith(SEPARATOR_SLASH)) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    private static void validatePath(String path, String originalPath) {
        if (path.startsWith("/..")) throw new IllegalStateException("Illegal path detected: " + originalPath);
    }

    private static String replaceFolderDotDotConstruct(String path, Set<String> previousVersions, Pattern pattern, String originalPath) {
        do {
            previousVersions.add(path);
            final Matcher matcher = pattern.matcher(path);
            while (matcher.find()) {
                final String group = matcher.group(1);
                // skip occurrences where the parent is not real '../..' or './..'
                if (!group.equals(".") && !group.equals("..")) {
                    path = path.substring(0, matcher.start()) + path.substring(matcher.end());
                    // since we modified the path, we have to rematch; path should already be different
                    break;
                }
            }
            validatePath(path, originalPath);
        } while (!previousVersions.contains(path));
        return path;
    }

    @Deprecated // use PatternSetMatcher instead
    public static String[] scanDirectoryForFolders(File targetDir, String... includes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedDirectories();
    }

    public static File findSingleFile(File baseFile, String... patterns) {
        final String[] files = FileUtils.scanDirectoryForFiles(baseFile, patterns);
        if (files.length == 1) {
            return new File(baseFile, files[0]);
        }
        return null;
    }

    public static String[] scanDirectoryForFiles(File targetDir, String... includes) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static String[] scanDirectoryForFiles(File targetDir, String[] includes, String[] excludes) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDir);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static void createDirectoryContentChecksumFile(File baseDir, File targetContentChecksumFile) throws IOException {
        final StringBuilder checksumSequence = new StringBuilder();

        // NOTE: could be moved to FileSystemMap; current impl may be more efficient
        final String[] files = FileUtils.scanDirectoryForFiles(baseDir, new String[]{"**/*"}, new String[]{"**/.DS_Store*"});
        // FIXME: we can save the normalizePathToLinux operation when the FileSystemMap could produce FileRef; revise
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

    /**
     * Ensures that the given folder specified by dir is empty and recreated
     *
     * @param dir Specifies the folder.
     *
     * @return Returns dir.
     */
    public static File ensureEmptyFolder(File dir) {
        // automated file indexing may cause issues while deleting; compensation via iteration
        int maxIteration = 1000;
        Throwable t = null;
        while (dir.exists() && maxIteration-- > 0) {
            try {
                System.out.println("Deleting " + dir);
                FileUtils.forceDelete(dir);
            } catch (IOException e) {
                t = e;
            }
        }
        if (dir.exists()) {
            LOG.error("Failed to delete tmp folder: {}", dir.getAbsolutePath(), t);
        }

        // recreate empty folder
        dir.mkdirs();
        return dir;
    }

    /**
     * Creates a {@link FileRef} instance for the given filePath. If the filePath is an absolute path the methods uses
     * the normalized and canonicalized version of the filePath. In case filePath is a relative path the baseDir path
     * is used to compose a resulting path (which is not necessarily absolute).
     *
     * @param filePath The file path.
     * @param baseDir The base dir to construct a composite path from, in case filePath is not absolute.
     *
     * @return The constructed PathRef instance.
     */
    public static FileRef toAbsoluteOrReferencePath(String filePath, File baseDir) {
        final boolean isAbsoluteWindowsPath = isAbsoluteWindowsPath(filePath);
        final String normalizePathToLinux = normalizePathToLinux(filePath);
        if (isAbsoluteWindowsPath) {
            return new FileRef(canonicalizeLinuxPath(normalizePathToLinux));
        }
        final boolean isAbsoluteLinuxPath = normalizePathToLinux.startsWith(SEPARATOR_SLASH);
        if (isAbsoluteLinuxPath) {
            return new FileRef(canonicalizeLinuxPath(normalizePathToLinux));
        }

        // filePath is a relative path
        final String baseDirRelativePath = normalizePathToLinux(baseDir) + "/" + normalizePathToLinux;
        return new FileRef(canonicalizeLinuxPath(baseDirRelativePath));
    }

    /**
     * Determines whether a given path string is an absolute windows path.
     *
     * @param path The path to evaluate.
     *
     * @return Returns <code>true</code> when the path is evaluated as absolute windows path.
     */
    public static boolean isAbsoluteWindowsPath(String path) {
        // check pattern '<single-drive-letter>:\<path-string>' anticipate that '\' was already replaced by '/'
        return path.length() > 3 && path.charAt(0) != '\\' && path.charAt(1) == ':' &&
                (path.charAt(2) == '/' || path.charAt(2) == '\\');
    }

    /**
     * Creates a {@link FileRef} instance for the given filePath. If the filePath is an absolute path the methods uses
     * the normalized and canonicalized version of the filePath. In case filePath is a relative path the baseDir path
     * is used to compose a resulting path (which is not necessarily absolute).
     *
     * @param filePath The file path.
     * @param baseDirPath The base dir path to construct a composite path from, in case filePath is not absolute.
     *
     * @return The constructed PathRef instance.
     */
    public static FileRef toAbsoluteOrReferencePath(String filePath, String baseDirPath) {
        return toAbsoluteOrReferencePath(filePath, new File(baseDirPath));
    }

}
