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
package org.metaeffekt.core.inventory.processor;

import org.apache.poi.util.TempFile;
import org.apache.poi.util.TempFileCreationStrategy;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.poi.util.TempFile.JAVA_IO_TMPDIR;

/**
 * Custom implementation of the {@link TempFileCreationStrategy} used by {@link TempFile}:
 * Files are collected into one directory.
 * Files are deleted when removeCreatedTempFiles() is called.
 *
 * The temporary directory is not deleted after the JVM exits.
 * Files that are created in the poifiles directory outside
 * the control of DefaultTempFileCreationStrategy are not deleted.
 * See {@link TempFileCreationStrategy} for better strategies for long-running
 * processes or limited temporary storage.
 *
 * Original Work:
 * Apache POI
 * Copyright 2003-2025 The Apache Software Foundation (<a href="https://www.apache.org/">...</a>).
 *
 * Modifications:
 * Jonas Fuegen, {metaeffekt} GmbH (<a href="https://metaeffekt.com">...</a>).
 * <ul>
 *     <li>Used {@link org.apache.poi.util.DefaultTempFileCreationStrategy as a template for this class}.</li>
 *     <li>Modified createTempFile() and createTempDirectory() methods.</li>
 *     <li>Modified comments and JavaDoc.</li>
 * </ul>
 */
public class CustomTempFileCreationStrategy implements TempFileCreationStrategy {

    /** Name of POI files directory in temporary directory. */
    public static final String POIFILES = "poifiles";

    /** The directory where the temporary files will be created (<code>null</code> to use the default directory). */
    private volatile File dir;

    /** The directory that was passed to the constructor (<code>null</code> to use the default directory). */
    private final File initDir;

    /** The lock to make dir initialized only once. */
    private final Lock dirLock = new ReentrantLock();

    private final ThreadLocal<Set<File>> tempFiles = new ThreadLocal<>();

    public CustomTempFileCreationStrategy() {
        this(null);
    }

    public CustomTempFileCreationStrategy(File dir) {
        this.initDir = dir;
        this.dir = dir;
        if (dir != null && dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException("The given directory does not exist or is not a directory: " + dir);
        }
    }

    public void removeCreatedTempFiles() {
        Set<File> files = tempFiles.get();
        if (files != null) {
            files.forEach(FileUtils::deleteQuietly);
            tempFiles.set(null);
        }
    }

    @Override
    public File createTempFile(String prefix, String suffix) throws IOException {
        createPOIFilesDirectoryIfNecessary();
        File newFile = Files.createTempFile(dir.toPath(), prefix, suffix).toFile();

        Set<File> files = tempFiles.get();
        if (files == null) {
            files = new HashSet<>();
            tempFiles.set(files);
        }

        files.add(newFile);

        return newFile;
    }

    @Override
    public File createTempDirectory(String prefix) throws IOException {
        createPOIFilesDirectoryIfNecessary();
        return Files.createTempDirectory(dir.toPath(), prefix).toFile();
    }

    protected String getJavaIoTmpDir() throws IOException {
        final String tmpDir = System.getProperty(JAVA_IO_TMPDIR);
        if (tmpDir == null) {
            throw new IOException("System's temporary directory not defined - set the -D" + JAVA_IO_TMPDIR + " jvm property!");
        }
        return tmpDir;
    }

    protected Path getPOIFilesDirectoryPath() throws IOException {
        if (initDir == null) {
            return Paths.get(getJavaIoTmpDir(), POIFILES);
        } else {
            return initDir.toPath();
        }
    }

    /**
     * Create our temp dir only once by double-checked locking. The directory is not deleted,
     * even if it was created by this TempFileCreationStrategy.
     *
     * @throws IOException thrown if the directory does not exist or can not be created due to lack of permissions.
     */
    private void createPOIFilesDirectoryIfNecessary() throws IOException {
        if (dir != null && !dir.exists()) {
            dir = null;
        }
        if (dir == null) {
            dirLock.lock();
            try {
                if (dir == null) {
                    final Path dirPath = getPOIFilesDirectoryPath();
                    File fileDir = dirPath.toFile();
                    if (fileDir.exists()) {
                        if (!fileDir.isDirectory()) {
                            throw new IOException("Could not create temporary directory. '" + fileDir + "' exists but is not a directory.");
                        }
                        dir = fileDir;
                    } else {
                        dir = Files.createDirectories(dirPath).toFile();
                    }
                }
            } finally {
                dirLock.unlock();
            }
        }
    }
}
