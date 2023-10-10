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
package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class InventoryExtractorUtil {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryExtractorUtil.class);

    public static final String SEPARATOR_LINK_MAP = " --> ";

    private InventoryExtractorUtil() {
    }

    /**
     * Filters the file list and outputs a list of files that is not covered by the packages.
     *
     * Symlinks are or relevance if the source of a symlinked file is covered by a package file or exclude
     * pattern. We try to consistency delete target and source of symlinks if either one is covered.
     *
     * This procedure is rather time-consuming depending on the container/virtual machine being analyzed. Changes must
     * only be applied with care and thorough testing.
     *
     * @param analysisDir The analysis base dir.
     * @param excludePatterns Additional exclude patterns.
     *
     * @return List of files not covered by package file lists or exclude patterns.
     *
     * @throws IOException May throw an {@link IOException}.
     */
    public static Set<String> filterFileList(File analysisDir, List<String> excludePatterns) throws IOException {
        final File filesFile = new File(analysisDir, "filesystem/files.txt");
        final File symbolicLinksFile = new File(analysisDir, "filesystem/symlinks.txt");
        final File foldersFile = new File(analysisDir, "filesystem/folders.txt");

        // parse and organize file list
        final List<String> fileList = FileUtils.readLines(filesFile, FileUtils.ENCODING_UTF_8);

        // collect package files
        final File packageFilesDir = new File(analysisDir, "package-files");
        final String[] packageFiles = FileUtils.scanForFiles(packageFilesDir, "**/*_files.txt", "--nothing--");

        // parse folder set
        final Set<String> folder = new HashSet<>(FileUtils.readLines(foldersFile, FileUtils.ENCODING_UTF_8));

        // optimize exclude patterns
        LOG.info("Optimizing exclude patterns...");
        final PatternSetMatcher excludePatternSetMatcher = new PatternSetMatcher(excludePatterns);

        LOG.info("Building symlink map...");
        // The symLinkMap maps symbolic links to there origin (target to source); in the original dataset the source
        // can be relative to the target (e.g. /var/spool/mail --> ../mail;
        // or /usr/share/zoneinfo/right/Pacific/Ponape --> Pohnpei) or absolut (e.g. /var/run --> /run).
        // In the map all values (symlink sources) are converted to canonicalized absolute paths (folder and files)
        final List<String> symbolicLinks = FileUtils.readLines(symbolicLinksFile, FileUtils.ENCODING_UTF_8);
        final Map<String, String> symLinkMap = buildSymLinkMap(symbolicLinks);

        LOG.info("Filtering [{}] files applying exclude patterns and file symlinks...", fileList.size());
        final Set<String> resultingFileSet = filterByExcludePatternsAndLinkedFiles(fileList, excludePatternSetMatcher, symLinkMap);

        LOG.info("Filtering [{}] files analyzing coverage by packages and symlinks...", resultingFileSet.size());
        filterByPackageFiles(packageFilesDir, resultingFileSet, folder, symLinkMap, packageFiles);

        LOG.info("Filtering [{}] files applying exclude patterns and folder symlinks...", resultingFileSet.size());

        // FIXME: temporarily deactivated; identified to remove too many files
        // filterByExcludePatternsAndLinkedFolders(resultingFileSet, excludePatternSetMatcher, symLinkMap);

        LOG.info("Filtering files completed resulting in [{}] files.", resultingFileSet.size());

        return resultingFileSet;
    }

    private static Set<String> filterByExcludePatternsAndLinkedFiles(List<String> fileList, PatternSetMatcher patternSetMatcher, Map<String, String> symLinkMap) {
        // build the result as set (performance improvement for later delete operations)
        final Set<String> resultingFileSet = new HashSet<>(fileList);

        // only preserve files not matching the exclude patterns
        for (final String file : fileList) {
            if (patternSetMatcher.matches(file)) {
                // remove file matching exclude patterns
                resultingFileSet.remove(file);
            }
        }

        for (final String file : symLinkMap.keySet()) {
            // in this case we may also remove symlinks pointing to this file (only if links are explicit of file level)
            final String linkedFile = symLinkMap.get(file);
            if (patternSetMatcher.matches(file)) {
                if (linkedFile != null) {
                    if (resultingFileSet.remove(linkedFile)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Removing " + file + " due to symlink " + linkedFile + "=" + file);
                        }
                    }
                }
            }
        }

        return resultingFileSet;
    }

    private static void filterByExcludePatternsAndLinkedFolders(Set<String> fileSet, PatternSetMatcher patternSetMatcher, Map<String, String> symLinkMap) {
        reduceSymlinks(fileSet, symLinkMap);

        for (final String file : symLinkMap.keySet()) {
            // in this case we may also remove symlinks pointing to this file (only if links are explicit of file level)
            if (patternSetMatcher.matches(file + "/")) {
                final String linkedFile = symLinkMap.get(file);
                if (linkedFile != null) {
                    final Set<String> toBeDeleted = new HashSet<>();
                    for (String filePath : fileSet) {
                        if (filePath.startsWith(linkedFile + "/")) {
                            toBeDeleted.add(filePath);
                        }
                    }
                    for (String filePath : toBeDeleted) {
                        if (fileSet.remove(filePath)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Removing " + file + " due to folder symlink " + linkedFile + "=" + file);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void filterByPackageFiles(File packageFilesDir, Set<String> resultingFileSet, Set<String> folders, Map<String, String> symLinkMap, String[] packageFiles) throws IOException {

        // FIXME: the inner loop iterating the symlinks is very expensive; a possible improvement would either be to
        //   reduce the symlinks to a relevant set or to better organize the symlinks (in buckets to not apply all to
        //   all)
        reduceSymlinks(resultingFileSet, symLinkMap);

        // iterate package files and eliminate the files covered
        for (final String singlePackageFile : packageFiles) {
            final List<String> packageFileList = FileUtils.readLines(new File(packageFilesDir, singlePackageFile), FileUtils.ENCODING_UTF_8);
            for (final String file : packageFileList) {
                // skip processing artifacts
                if (!StringUtils.isNotBlank(file)) continue;
                if (file.endsWith(" contains:")) continue;

                // skip folders (optimization)
                if (folders.contains(file)) continue;

                // convert to match requirements for matching in file list; compensate missing '/'
                final String convertedFile = file.startsWith("/") ? file : "/" + file;

                if (!resultingFileSet.remove(convertedFile)) {

                    // in case the file is not removed we need to check whether the file is covered as target of by a
                    // symbolic link
                    for (final Map.Entry<String, String> symLinkEntry : symLinkMap.entrySet()) {
                        if (convertedFile.startsWith(symLinkEntry.getKey())) {
                            final String effectivePath = replacePathPrefix(
                                    convertedFile, symLinkEntry.getKey(), symLinkEntry.getValue());
                            if (resultingFileSet.remove(effectivePath)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Removing " + effectivePath + " due to symlink " + symLinkEntry);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void reduceSymlinks(Set<String> resultingFileSet, Map<String, String> symLinkMap) {
        // collect individual folders in set
        final Set<String> parts = new HashSet<>();
        for (final String file : resultingFileSet) {
            final String[] split = file.split("/");
            Arrays.stream(split).forEach(s -> parts.add(s));
        }

        // remove all symlinks that have no parts with the remaining files in common
        for (final Map.Entry<String, String> linkEntry : new HashSet<>(symLinkMap.entrySet())) {
            final String linkSource = linkEntry.getValue();
            final String[] split = linkSource.split("/");
            for (final String s : split) {
                if (!parts.contains(s)) {
                    symLinkMap.remove(linkEntry.getKey());
                }
            }
        }
    }

    private static String replacePathPrefix(String file, String currentPrefix, String newPrefix) {
        return newPrefix + file.substring(currentPrefix.length());
    }

    private static Map<String, String> buildSymLinkMap(List<String> symbolicLinks) throws IOException {
        final Map<String, String> symLinkMap = new HashMap<>();
        for (String symLinkLine : symbolicLinks) {
            final int separatorIndex = symLinkLine.indexOf(SEPARATOR_LINK_MAP);

            if (separatorIndex == -1) {
                LOG.warn("Cannot parse symlink information: " + symLinkLine);
                continue;
            }

            final String linkFile = symLinkLine.substring(0, separatorIndex).trim();
            String linkTargetFile = symLinkLine.substring(separatorIndex + SEPARATOR_LINK_MAP.length()).trim();

            if (StringUtils.isNotBlank(linkFile) && StringUtils.isNotBlank(linkTargetFile)) {
                if (!linkTargetFile.startsWith("..")) {

                    // compensate missing slash
                    if (!linkTargetFile.startsWith("/")) {
                        linkTargetFile = "/" + linkTargetFile;
                    }

                    // links to nothing (empty result) or "/" are not mapped
                    if (linkTargetFile.length() >= 2) {
                        symLinkMap.put(linkFile, linkTargetFile);
                    }
                } else {
                    // the link target is relative; the resulting path must be composed and canonicalized
                    final String compositePath = new File(linkFile, linkTargetFile).getPath();

                    // the composite path may include ../ and ./ constructs and must be canonicalized
                    // however, File.getAbsolutePath() and File.getCanonicalPath() are painfully slow
                    // therefore, the FileUtils.canonicalizePath() method is applied
                    symLinkMap.put(linkFile, FileUtils.canonicalizeLinuxPath(compositePath));
                }
            }
        }

        return symLinkMap;
    }

}
