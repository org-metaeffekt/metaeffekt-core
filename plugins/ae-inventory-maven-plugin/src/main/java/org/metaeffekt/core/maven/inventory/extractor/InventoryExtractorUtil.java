package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class InventoryExtractorUtil {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryExtractorUtil.class);

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    public static final String SEPARATOR_LINK_MAP = " --> ";

    private InventoryExtractorUtil() {
    }

    ;

    /**
     * Filters the file list and outputs a list of files that is not covered by the packages.
     *
     * @param analysisDir The analysis base dir.
     * @param excludePatterns Additional exclude patterns.
     *
     * @return List of files not covered by package file lists or exclude patterns.
     *
     * @throws IOException May throw an {@link IOException}.
     */
    public static List<String> filterFileList(File analysisDir, List<String> excludePatterns) throws IOException {
        final File filesFile = new File(analysisDir, "filesystem/files.txt");
        final File symbolicLinksFile = new File(analysisDir, "filesystem/symlinks.txt");
        final File foldersFile = new File(analysisDir, "filesystem/folders.txt");

        final File packageFilesDir = new File(analysisDir, "package-files");
        final String[] packageFiles = FileUtils.scanForFiles(packageFilesDir, "**/*_files.txt", "--nothing--");
        final List<String> fileList = FileUtils.readLines(filesFile, FileUtils.ENCODING_UTF_8);
        final Set<String> folder = new HashSet<>(FileUtils.readLines(foldersFile, FileUtils.ENCODING_UTF_8));
        final List<String> symbolicLinks = FileUtils.readLines(symbolicLinksFile, FileUtils.ENCODING_UTF_8);

        final Map<String, String> symLinkMap = buildSymLinkMap(symbolicLinks);

        filterByExcludePatterns(excludePatterns, fileList, symLinkMap);

        filterByPackageFiles(packageFilesDir, fileList, folder, symLinkMap, packageFiles);

        return fileList;
    }

    private static void filterByPackageFiles(File packageFilesDir, List<String> fileList, Set<String> folders, Map<String, String> symLinkMap, String[] packageFiles) throws IOException {
        // iterate package files and eliminate the files covered
        for (String singlePackageFile : packageFiles) {
            List<String> packageFileList = FileUtils.readLines(new File(packageFilesDir, singlePackageFile), FileUtils.ENCODING_UTF_8);
            for (String file : packageFileList) {
                // skip processing artifacts
                if (StringUtils.isEmpty(file)) continue;
                if (file.endsWith(" contains:")) continue;

                // skip folders (optimization)
                if (folders.contains(file)) {
                    continue;
                }

                // convert to match requirements for matching in file list
                String convertedFile = file;
                if (!convertedFile.startsWith("/")) {
                    convertedFile = "/" + convertedFile;
                }
                if (!fileList.remove(convertedFile)) {
                    // in case the file is not removed we need to check whether the file is covered by a symbolic link
                    for (Map.Entry<String, String> symLinkEntry : symLinkMap.entrySet()) {
                        if (convertedFile.startsWith(symLinkEntry.getKey())) {
                            if (fileList.remove(replacePathPrefix(convertedFile, symLinkEntry.getKey(), symLinkEntry.getValue()))) {
                                LOG.debug("Removing " + convertedFile + " due to symlink" + symLinkEntry);
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void filterByExcludePatterns(List<String> excludePatterns, List<String> fileList, Map<String, String> symLinkMap) {
        // filter those matching the exclude patterns
        for (final String file : new ArrayList<>(fileList)) {
            if (removeMatching(excludePatterns, fileList, file, file)) {
                LOG.debug("Removing " + file + " due to pattern");
                continue;
            }
        }

        // check whether the patterns covered by a symbolic link
        for (Map.Entry<String, String> symLinkEntry : symLinkMap.entrySet()) {
            final String modulatedFile = symLinkEntry.getValue();
            if (removeMatching(excludePatterns, fileList, modulatedFile, symLinkEntry.getKey())) {
                LOG.debug("Removing " + symLinkEntry.getKey() + " due to symlink " + symLinkEntry);
                continue;
            }
        }

        // NOTE: please note, that reverse matching the symlinks removes too many files that are not intended to be
        // covered by the exclude patterns.
    }

    private static String replacePathPrefix(String file, String currentPrefix, String newPrefix) {
        return newPrefix + file.substring(currentPrefix.length());
    }

    private static Map<String, String> buildSymLinkMap(List<String> symbolicLinks) throws IOException {
        final Map<String, String> symLinkMap = new HashMap<>();
        for (String symLinkLine : symbolicLinks) {
            final String linkFile = symLinkLine.substring(0, symLinkLine.indexOf(SEPARATOR_LINK_MAP)).trim();
            String linkTargetFile = symLinkLine.substring(symLinkLine.indexOf(SEPARATOR_LINK_MAP) + SEPARATOR_LINK_MAP.length()).trim();
            if (StringUtils.hasText(linkFile) && StringUtils.hasText(linkTargetFile)) {
                if (!linkTargetFile.startsWith("..")) {
                    if (!linkTargetFile.startsWith("/")) {
                        linkTargetFile = "/" + linkTargetFile;
                    }
                    if (linkTargetFile.length() >= 2) {
                        symLinkMap.put(linkFile, linkTargetFile);
                    }
                } else {
                    symLinkMap.put(linkFile, new File(linkFile, linkTargetFile).getCanonicalPath());
                }
            }
        }
        return symLinkMap;
    }

    private static boolean removeMatching(List<String> excludePatterns, List<String> fileList, String matchableFile, String originalFile) {
        for (String pattern : excludePatterns) {
            if (ANT_PATH_MATCHER.match(pattern, matchableFile)) {
                return fileList.remove(originalFile);
            }
        }
        return false;
    }

}
