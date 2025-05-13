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

import org.metaeffekt.core.inventory.processor.filescan.FileRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.metaeffekt.core.util.FileUtils.normalizePathToLinux;

public class FileSystemMap {

    private FileRef baseDirRef;

    private Map<String, FolderContent> map = new HashMap<>();

    public static FileSystemMap create(final File baseDir) throws IOException {
        final FileSystemMap fileSystemMap = new FileSystemMap();
        fileSystemMap.initialize(baseDir, fileSystemMap);
        return fileSystemMap;
    }

    private void initialize(File baseDir, FileSystemMap fileSystemMap) throws IOException {
        baseDirRef = new FileRef(baseDir.getAbsoluteFile());

        Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                return getFileVisitResult(path, attrs);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                return getFileVisitResult(path, attrs);
            }

            private FileVisitResult getFileVisitResult(Path path, BasicFileAttributes attrs) {
                final File parentFile = path.getParent().toFile();
                final File file = path.toFile();

                final String normalizedParentPath = normalizePathToLinux(parentFile.getAbsolutePath());
                final String normalizedPath = normalizePathToLinux(file);
                final String relativePath = normalizePathToLinux(FileUtils.asRelativePath(baseDirRef.getPath(), normalizedPath));

                final FolderContent folderContent = fileSystemMap.map.computeIfAbsent(normalizedParentPath, a -> new FolderContent());
                final FileRef fileRef = new FileRef(relativePath);

                if (!".".equals(fileRef.getPath())) {
                    if (attrs.isDirectory()) {
                        folderContent.addFolder(fileRef);
                    } else {
                        if (attrs.isSymbolicLink()) {
                            // symlinks are included, but not followed
                            folderContent.addFile(fileRef);
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            folderContent.addFile(fileRef);
                        }
                    }
                }

                if (attrs.isSymbolicLink()) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public String[] scanDirectoryForFiles(File scanBaseDir, Set<String> includes, Set<String> excludes) {
        final String basePath = normalizePathToLinux(scanBaseDir.getAbsolutePath());

        final FolderContent baseFolderContent = map.get(basePath);
        if (baseFolderContent == null) return new String[0];
        if (includes == null || includes.isEmpty()) return new String[0];

        // walk map and collect all files and folders
        List<FileRef> candidatePaths = getFiles(baseFolderContent);

        // apply excludes
        if (excludes != null && !excludes.isEmpty()) {
            candidatePaths.removeIf(pathRef -> FileUtils.matches(excludes, pathRef.getPath()));
        }

        PatternSetMatcher includesMatcher = new PatternSetMatcher(includes);

        // apply includes
        List<String> matchedFiles = new ArrayList<>();
        String relativePath = FileUtils.asRelativePath(baseDirRef.getFile(), scanBaseDir);
        for (FileRef fileRef : candidatePaths) {
            String path = fileRef.getPath();
            if (path.startsWith(relativePath + "/")) {
                path = path.substring(relativePath.length() + 1);
            }
            if (includesMatcher.matches(path)) {
                matchedFiles.add(path);
            }
        }

        /*
        // DEBUG SUPPORT / COMPARSSION
        matchedFiles.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println("Collected " + new LinkedHashSet<>(matchedFiles));
        final String[] files = FileUtils.scanDirectoryForFiles(scanBaseDir, toArray(includes), toArray(excludes));
        final List<String> list = Arrays.asList(files);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        System.out.println("Scanned   " + list);
         */

        return matchedFiles.toArray(new String[matchedFiles.size()]);
    }

    private List<FileRef> getFiles(FolderContent folderContent) {
        final List<FileRef> candidatePaths = new ArrayList<>();
        final Set<String> processedPaths = new HashSet<>();

        final Stack<FolderContent> unprocessedFolderContentStack = new Stack<>();
        unprocessedFolderContentStack.push(folderContent);

        // iterate, push and pop on stack until no further item is available
        while (!unprocessedFolderContentStack.isEmpty()) {
            final FolderContent currentFolderContent = unprocessedFolderContentStack.pop();

            candidatePaths.addAll(currentFolderContent.getFiles());

            final Collection<FileRef> folders = currentFolderContent.getFolders();
            candidatePaths.addAll(folders);

            // push folders onto the stack for further processing
            for (FileRef folderRef : folders) {
                final String absoluteFolderPath = normalizePathToLinux(new File(this.baseDirRef.getFile(), folderRef.getPath()).getAbsoluteFile());
                if (!processedPaths.contains(absoluteFolderPath)) {
                    processedPaths.add(absoluteFolderPath);
                    final FolderContent content = map.get(absoluteFolderPath);
                    if (content != null) {
                        unprocessedFolderContentStack.push(content);
                    }
                }
            }
        }

        return candidatePaths;
    }

    private String[] toArray(Set<String> patternSet) {
        if (patternSet.isEmpty()) return null;
        return patternSet.toArray(new String[0]);
    }

    private static class FolderContent {
        List<FileRef> files = null;
        List<FileRef> folders = null;

        public void addFile(FileRef path) {
            if (files == null) {
                files = new ArrayList<>();
            }
            files.add(path);
        }

        public void addFolder(FileRef path) {
            if (folders == null) {
                folders = new ArrayList<>();
            }
            folders.add(path);
        }

        public Collection<FileRef> getFiles() {
            if (files == null) return Collections.emptyList();
            return files;
        }

        public Collection<FileRef> getFolders() {
            if (folders == null) return Collections.emptyList();
            return folders;
        }
    }

}
