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
package org.metaeffekt.core.util;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.GUnzip;
import org.apache.tools.ant.taskdefs.Untar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ArchiveUtils for dealing with different archives on core-level.
 */
public class ArchiveUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveUtils.class);

    private static final Set<String> zipExtensions = new HashSet<>();
    private static final Set<String> gzipExtensions = new HashSet<>();
    private static final Set<String> tarExtensions = new HashSet<>();
    
    private static final Set<String> jmodExtensions = new HashSet<>();
    private static final Set<String> jimageExtensions = new HashSet<>();

    private static final Set<String> jimageFilenames = new HashSet<>();

    static {
        zipExtensions.add("war");
        zipExtensions.add("zip");
        zipExtensions.add("nar");
        zipExtensions.add("jar");
        zipExtensions.add("xar");
        zipExtensions.add("webjar");
        zipExtensions.add("ear");
        zipExtensions.add("aar");
        zipExtensions.add("sar");
        zipExtensions.add("nupkg");
        // whl: python / pip wheel files (used for distribution binary dependencies like libraries)
        zipExtensions.add("whl");

        gzipExtensions.add("gzip");
        gzipExtensions.add("gz");

        tarExtensions.add("tar");
        tarExtensions.add("rpm"); // RPMs contain further metadata; how to handle them to not get lost? Generate summary file?
        tarExtensions.add("bz2");
        tarExtensions.add("xz");
        tarExtensions.add("tgz");
        tarExtensions.add("deb");
        tarExtensions.add("apk");

        jmodExtensions.add("jmod");

        // keep for behavior consistency reasons
        jimageExtensions.add("modules");

        jimageFilenames.add("modules");
    }

    public static void registerZipExtension(String suffix) {
        zipExtensions.add(suffix);
    }

    public static void registerGzipExtension(String suffix) {
        gzipExtensions.add(suffix);
    }

    public static void registerTarExtension(String suffix) {
        tarExtensions.add(suffix);
    }

    public static void registerJmodExtension(String suffix) {
        jmodExtensions.add(suffix);
    }

    public static void registerJimageExtension(String suffix) {
        jimageExtensions.add(suffix);
    }

    /**
     * Tar files may be wrapped. This method extracts the tar-construct until it reaches the content and keeps book
     * on the intermediate files created.
     *
     * @param file      The file to untar
     * @param targetDir The directory to untar the file into
     * @throws IOException If the file could not be untared
     */
    public static void untar(File file, File targetDir) throws IOException {
        final String fileName = file.getName().toLowerCase();

        final List<File> intermediateFiles = new ArrayList<>();

        // preprocess tar wrappers and manage intermediate files
        try {
            if (fileName.endsWith(".gz")) {
                GUnzip gunzip = new GUnzip();
                gunzip.setProject(new Project());
                gunzip.setSrc(file);
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".gz"));
                gunzip.setDest(target);
                intermediateFiles.add(target);
                gunzip.execute();
                file = target;
            }

            if (fileName.endsWith(".tgz")) {
                GUnzip gunzip = new GUnzip();
                gunzip.setProject(new Project());
                gunzip.setSrc(file);
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".tgz"));
                gunzip.setDest(target);
                intermediateFiles.add(target);

                gunzip.execute();
                file = target;
            }

            if (fileName.endsWith(".xz")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".xz"));
                intermediateFiles.add(target);

                expandXZ(file, target);
                file = target;
            }

            if (fileName.endsWith(".bz2")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".bz2"));
                intermediateFiles.add(target);

                expandBzip2(file, target);
                file = target;
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            for (File intermediateFile : intermediateFiles) {
                if (intermediateFile.exists()) {
                    FileUtils.forceDelete(intermediateFile);
                }
            }
        }

        try {
            untarInternal(file, targetDir);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot untar " + file, e);
        } finally {
            for (File intermediateFile : intermediateFiles) {
                FileUtils.forceDelete(intermediateFile);
            }
        }
    }

    private static String intermediateUnpackFile(String targetName, String suffix) {
        // prefix with .unpack_to avoid name collisions
        return ".unpack_" + targetName.substring(0, targetName.toLowerCase().lastIndexOf(suffix));
    }

    private static void expandXZ(File file, File targetFile) throws IOException {
        final InputStream fin = Files.newInputStream(file.toPath());
        final BufferedInputStream in = new BufferedInputStream(fin);
        final XZCompressorInputStream xzIn = new XZCompressorInputStream(in);

        unpackAndClose(xzIn, Files.newOutputStream(targetFile.toPath()));
    }

    private static void expandBzip2(File file, File targetFile) throws IOException {
        final InputStream fin = Files.newInputStream(file.toPath());
        final BufferedInputStream in = new BufferedInputStream(fin);
        final BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);

        unpackAndClose(bzIn, Files.newOutputStream(targetFile.toPath()));
    }

    private static void untarInternal(File file, File targetFile) throws IOException {
        try {
            // attempt ant untar (anticipating broad compatibility)
            final Project project = new Project();
            Untar expandTask = new Untar();
            expandTask.setProject(project);
            expandTask.setDest(targetFile);
            expandTask.setSrc(file);
            expandTask.setAllowFilesToEscapeDest(false);
            expandTask.execute();
        } catch (Exception exception) {
            try {
                // fallback to commons-compress (local code with support for specific cases (i.e., .deb)
                final InputStream fin = Files.newInputStream(file.toPath());
                final BufferedInputStream in = new BufferedInputStream(fin);
                final ArArchiveInputStream xzIn = new ArArchiveInputStream(in);
                unpackAndClose(xzIn, targetFile);
            } catch (Exception ex) {
                // report commons compress exception only and indicate fallback
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cannot untar file [{}]. {}. Attempting native untar.", file, ex.getMessage());
                }
                // fallback to native support on command line
                Process exec = Runtime.getRuntime().exec("tar -xf " + file.getAbsolutePath() + " -C " + targetFile.getAbsolutePath());
                FileUtils.waitForProcess(exec);

                // NOTE: further exceptions are handled upstream
            }
        }
    }

    private static void unpackAndClose(InputStream in, OutputStream out) throws IOException {
        try {
            final byte[] buffer = new byte[1024];
            int n;
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
            }
        } finally {
            out.close();
            in.close();
        }
    }

    private static void unpackAndClose(ArArchiveInputStream in, File targetDir) throws IOException {
        try {
            ArArchiveEntry entry;
            while ((entry = in.getNextArEntry()) != null) {
                final File targetFile = new File(targetDir, entry.getName());
                try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                    IOUtils.copy(in, out);
                }
            }
        } finally {
            in.close();
        }
    }

    public static boolean unpackIfPossible(File archiveFile, File targetDir, List<String> issues) {
        final Project project = new Project();
        project.setBaseDir(archiveFile.getParentFile());

        final String archiveFileName = archiveFile.getName().toLowerCase();
        final String extension = FilenameUtils.getExtension(archiveFileName);

        boolean mkdir = !targetDir.exists();

        // try unzip
        try {
            if (zipExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                Expand expandTask = new Expand();
                expandTask.setProject(project);
                expandTask.setDest(targetDir);
                expandTask.setSrc(archiveFile);
                expandTask.execute();
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot unzip " + archiveFile.getAbsolutePath());
            return false;
        }

        // try gunzip
        try {
            if (gzipExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                GUnzip expandTask = new GUnzip();
                expandTask.setProject(project);
                expandTask.setDest(targetDir);
                expandTask.setSrc(archiveFile);
                expandTask.execute();
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot gunzip " + archiveFile.getAbsolutePath());
            return false;
        }

        // NOTE: currently PE files are not supported on core-level. These require further
        //   dependencies. PE files are not regarded as relevant for the software component identification use case.

        // try untar
        try {
            if (tarExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                untar(archiveFile, targetDir);
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot untar: " + archiveFile.getAbsolutePath());
            return false;
        }

        // native support

        // try jmod
        try {
            if (jmodExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                extractJMod(archiveFile, targetDir);
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot extract JMod: " + archiveFile.getAbsolutePath());
            return false;
        }

        // try jimage
        try {
            if (jimageExtensions.contains(extension) || jimageFilenames.contains(archiveFileName)) {
                FileUtils.forceMkdir(targetDir);
                extractJImage(archiveFile, targetDir);
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot extract JImage: " + archiveFile.getAbsolutePath());
            return false;
        }

        if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
        return false;
    }

    private static void extractJMod(File file, File targetFile) throws IOException {
        // this requires a jdk to perform the extraction
        final String jdkPath = getJdkPath();

        final File jmodExecutable = new File(jdkPath, "bin/jmod");
        if (jmodExecutable.exists()) {
            Process exec = Runtime.getRuntime().exec(jmodExecutable.getAbsolutePath() + " extract " + file.getAbsolutePath(), null, targetFile);
            FileUtils.waitForProcess(exec);
        } else {
            LOG.error("Cannot unpack jmod executable: " + jmodExecutable +
                    ". Ensure property jdk.path is set and points to a JDK with version > 11.0.");
        }
    }

    private static String getJdkPath() {
        String jdkPath = System.getProperty("jdk.path");
        if (!StringUtils.isNotBlank(jdkPath)) {
            throw new IllegalStateException("No jdk.path for extracting jmod files available.");
        }
        return jdkPath;
    }

    private static void extractJImage(File file, File targetFile) throws IOException {
        // this requires a jdk to perform the extraction
        final String jdkPath = getJdkPath();

        final File jmodExecutable = new File(jdkPath, "bin/jimage");
        if (jmodExecutable.exists()) {
            Process exec = Runtime.getRuntime().exec(jmodExecutable.getAbsolutePath() + " extract " + file.getAbsolutePath(), null, targetFile);
            FileUtils.waitForProcess(exec);
        } else {
            LOG.error("Cannot unpack jimage executable: " + jmodExecutable +
                    ". Ensure property jdk.path is set and points to a JDK with version > 11.0.");
        }
    }

}
