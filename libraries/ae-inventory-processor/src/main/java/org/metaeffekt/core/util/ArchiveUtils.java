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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.GUnzip;
import org.apache.tools.ant.taskdefs.Zip;
import org.metaeffekt.bundle.sevenzip.SevenZipExecutableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * ArchiveUtils for dealing with different archives on core-level.
 */
public class ArchiveUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveUtils.class);

    private static final long EXTRACT_DURATION = 1;
    private static final TimeUnit EXTRACT_DURATION_TIMEOUT_UNIT = TimeUnit.HOURS;

    private static final Set<String> zipExtensions = new HashSet<>();
    private static final Set<String> gzipExtensions = new HashSet<>();
    private static final Set<String> tarExtensions = new HashSet<>();

    private static final Set<String> jmodExtensions = new HashSet<>();
    private static final Set<String> jimageExtensions = new HashSet<>();

    private static final Set<String> jimageFilenames = new HashSet<>();

    private static final Set<String> windowsExtensions = new HashSet<>();

    /**
     * In allExtensions we collect all suffixes
     */
    private static final Set<String> allExtensions = new HashSet<>();

    static {
        zipExtensions.add("war");
        // zip: regular zip archives
        zipExtensions.add("zip");
        zipExtensions.add("nar");
        // jar: java archives
        zipExtensions.add("jar");
        zipExtensions.add("xar");
        zipExtensions.add("webjar");
        zipExtensions.add("ear");
        zipExtensions.add("aar");
        zipExtensions.add("sar");
        // nupkg: nuget package (special zip)
        zipExtensions.add("nupkg");
        // whl: python / pip wheel files (used for distribution binary dependencies like libraries)
        zipExtensions.add("whl");

        // gzip: gzip compressed file, less commonly used extention than ".gz"
        gzipExtensions.add("gzip");
        // gz: gzip compressed file
        gzipExtensions.add("gz");

        // tar: various archive formats derived from an old "tape archive" utility
        tarExtensions.add("tar");
        // bz2: bzip2 format compressed files
        tarExtensions.add("bz2");
        // xz: compression format xz(see also "lzma")
        tarExtensions.add("xz");
        // tgz: sometimes used as a shorthand for ".tar.gz"
        tarExtensions.add("tgz");
        // deb: debian package archive
        tarExtensions.add("deb");
        // apk: android package (for apps, special zip), alpine linux package (special tar file)
        tarExtensions.add("apk");
        tarExtensions.add("gem");

        // cab: windows cabinet file
        windowsExtensions.add("cab");
        // exe: windows executable (sometimes self-extracting archives)
        windowsExtensions.add("exe");
        // msi: windows installer package
        windowsExtensions.add("msi");

        jmodExtensions.add("jmod");

        // keep for behavior consistency reasons
        jimageExtensions.add("modules");

        jimageFilenames.add("modules");

        allExtensions.addAll(zipExtensions);
        allExtensions.addAll(gzipExtensions);
        allExtensions.addAll(tarExtensions);
        allExtensions.addAll(windowsExtensions);
        allExtensions.addAll(jmodExtensions);
        allExtensions.addAll(jimageExtensions);
    }

    public static void registerZipExtension(String suffix) {
        zipExtensions.add(suffix);
        allExtensions.add(suffix);
    }

    public static void registerGzipExtension(String suffix) {
        gzipExtensions.add(suffix);
        allExtensions.add(suffix);
    }

    public static void registerTarExtension(String suffix) {
        tarExtensions.add(suffix);
        allExtensions.add(suffix);
    }

    public static void registerJmodExtension(String suffix) {
        jmodExtensions.add(suffix);
        allExtensions.add(suffix);
    }

    public static void registerJimageExtension(String suffix) {
        jimageExtensions.add(suffix);
        allExtensions.add(suffix);
    }

    /**
     * Tar files may be wrapped. This method extracts the tar-construct until it reaches the content and keeps book
     * on the intermediate files created.
     *
     * @param file      The file to untar.
     * @param targetDir The directory to untar the file into.
     *
     * @throws IOException If the file could not be untared
     */
    public static void untar(File file, File targetDir) throws IOException {
        final String fileName = file.getName().toLowerCase();

        if (!file.exists()) {
            LOG.warn("Requested to untar path [{}] but file doesn't even exist.", file.getAbsolutePath());
        }

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
                    LOG.trace("Deleting intermediate [{}]", intermediateFile.getAbsolutePath());
                    FileUtils.forceDelete(intermediateFile);
                }
            }
        }

        try {
            untarInternal(file, targetDir);
        } catch (Exception e) {
            LOG.warn("Cannot untar [{}]. Attempting 7zip untar to compensate [{}].", file.getAbsolutePath(), e.getMessage());
            try {
                FileUtils.forceMkdir(targetDir);
                extractFileWithSevenZip(file, targetDir);
            } catch (Exception ex) {
                LOG.warn("Cannot untar [{}]. Attempting native untar. to compensate [{}].", file.getAbsolutePath(), ex.getMessage());
                try {
                    nativeUntar(file, targetDir);
                } catch(Exception exc) {
                    throw new IllegalStateException(format("Cannot untar [%s] using native untar command.", file.getAbsolutePath()), exc);
                }
            }
        } finally {
            for (File intermediateFile : intermediateFiles) {
                LOG.trace("Deleting intermediate [{}]", intermediateFile.getAbsolutePath());
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
            final InputStream fin = Files.newInputStream(file.toPath());
            final BufferedInputStream in = new BufferedInputStream(fin);
            final TarArchiveInputStream xzIn = new TarArchiveInputStream(in);
            if (!targetFile.exists()) {
                FileUtils.forceMkdir(targetFile);
            }
            unpackAndClose(xzIn, targetFile);
        } catch (Exception e) {
            throw new IOException("Could not untar file [" + file.getAbsolutePath() + "]", e);
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

    private static void unpackAndClose(TarArchiveInputStream in, File targetDir) throws IOException {
        try {
            TarArchiveEntry entry;

            // we need to check the os we are running on
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

            while ((entry = in.getNextEntry()) != null) {
                final File targetFile = new File(targetDir, entry.getName());

                if (!isWindows) {
                    try {
                        int uid = (Integer) Files.getAttribute(targetDir.toPath(), "unix:uid");
                        int gid = (Integer) Files.getAttribute(targetDir.toPath(), "unix:gid");
                        entry.setUserId(uid);
                        entry.setGroupId(gid);
                    } catch (UnsupportedOperationException e) {
                        LOG.warn("Unix file attributes not supported on this platform.");
                    }
                }

                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(targetFile);
                } else {
                    if (targetFile.exists() || Files.isSymbolicLink(targetFile.toPath())) {
                        FileUtils.forceDelete(targetFile);
                    }
                    if (entry.isSymbolicLink()) {
                        Path linkTarget;
                        if (entry.getLinkName().startsWith("/")) {
                            // handle absolute paths
                            linkTarget = targetDir.toPath().resolve(entry.getLinkName().substring(1));
                        } else {
                            // handle relative paths
                            linkTarget = targetFile.toPath().getParent().resolve(entry.getLinkName()).normalize();
                        }

                        try {
                            Files.createSymbolicLink(targetFile.toPath(), linkTarget);
                        } catch (UnsupportedOperationException | IOException e) {
                            LOG.warn("Symbolic links not supported or insufficient permissions. Skipping symbolic link creation.");
                        }
                    } else {
                        try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    public static boolean unpackIfPossible(File archiveFile, File targetDir, List<String> issues) {
        if (!archiveFile.exists() || !archiveFile.getParentFile().exists()) {
            LOG.warn("Trying to unpack a file, which does not exists (anymore): {}", archiveFile);
            return false;
        }

        final Project project = new Project();
        project.setBaseDir(archiveFile.getParentFile());

        final String archiveFileName = archiveFile.getName().toLowerCase();
        final String extension = FilenameUtils.getExtension(archiveFileName);

        boolean mkdir = !targetDir.exists();

        // FIXME-KKL: discuss before enabling
        // skip unwrap if target already exists (we already have extracted the directory)
        // if (!mkdir) return true;

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
            LOG.error(e.getMessage(), e);
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

        // try windows
        try {
            if (windowsExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                extractFileWithSevenZip(archiveFile, targetDir);
                return true;
            }
        } catch (Exception e) {
            if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);
            issues.add("Cannot extract Windows file: " + archiveFile.getAbsolutePath());
            return false;
        }

        // in case the targetDir was actively created, it is actively removed.
        if (mkdir) FileUtils.deleteDirectoryQuietly(targetDir);

        return false;
    }

    private static void extractJMod(File file, File targetFile) throws IOException {
        // this requires a jdk to perform the extraction
        final String jdkPath = getJdkPath();

        final File jmodExecutable = new File(jdkPath, "bin/jmod");
        if (jmodExecutable.exists()) {
            final String[] commandParts = new String[] { jmodExecutable.getAbsolutePath(), "extract", file.getAbsolutePath() };
            executeExtraction(commandParts, file, targetFile);
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

        final File jImageExecutable = new File(jdkPath, "bin/jimage");
        if (jImageExecutable.exists()) {
            final String[] commandParts = new String[] { jImageExecutable.getAbsolutePath(), "extract", file.getAbsolutePath() };
            executeExtraction(commandParts, file, targetFile);
        } else {
            LOG.error("Cannot unpack jimage executable: " + jImageExecutable +
                    ". Ensure property jdk.path is set and points to a JDK with version > 11.0.");
        }
    }

    private static void extractFileWithSevenZip(File file, File targetFile) throws IOException {
        // this requires 7zip to perform the extraction
        final File sevenZipBinaryFile = SevenZipExecutableUtils.getBinaryFile();
        if (sevenZipBinaryFile.exists()) {
            final String[] commandParts = {sevenZipBinaryFile.getAbsolutePath(), "x",
                    file.getAbsolutePath(), "-aoa", "-o" + targetFile.getAbsolutePath()};
            executeExtraction(commandParts, file, targetFile);
        } else {
            LOG.error("Cannot unpack file: " + file.getAbsolutePath() + " with 7zip. Ensure 7zip is installed at [" + sevenZipBinaryFile.getAbsolutePath() + "].");
            throw new IOException("Could not execute command due to missing binary.");
        }
    }

    /**
     * Uses the native zip command to zip the file. Uses the -X attribute to create files with deterministic checksum.
     *
     * @param sourceDir     The directory to zip (recursively).
     * @param targetZipFile The target zip file name.
     */
    public static void zipAnt(File sourceDir, File targetZipFile) {
        Zip zip = new Zip();
        Project project = new Project();
        project.setBaseDir(sourceDir);
        zip.setProject(project);
        zip.setBasedir(sourceDir);
        zip.setCompress(true);
        zip.setDestFile(targetZipFile);
        zip.setFollowSymlinks(false);
        zip.execute();
    }

    public static void nativeUntar(File file, File targetFile) throws IOException {
        // FIXME: this fallback doesn't adjust file permissions, leading to "Permission denied" while scanning.
        //  this also means that prepareScanDirectory may fail on rescan.
        //  we should probably just make sure that the java-native unwrao doesn't fail instead of relying on
        //  this last-ditch efford to give good support.
        String[] commandParts = new String[] {
                "tar", "-x", "-f", file.getAbsolutePath(),
                "--no-same-permissions", "-C", targetFile.getAbsolutePath() };
        executeExtraction(commandParts, file, targetFile);
    }

    private static void executeExtraction(String[] commandParts, File file, File targetFile) throws IOException {

        final ExecUtils.ExecParam execParam = new ExecUtils.ExecParam(commandParts);

        // apply standard configuration
        execParam.destroyOnTimeout(true);
        execParam.retainErrorOutputs();
        execParam.setWorkingDir(targetFile);
        execParam.timeoutAfter(EXTRACT_DURATION, EXTRACT_DURATION_TIMEOUT_UNIT);

        ExecUtils.executeAndThrowIOExceptionOnFailure(execParam);
    }

    public static boolean isArchiveByName(String pathOrName) {
        if (pathOrName == null) return false;
        final String extension = FilenameUtils.getExtension(pathOrName.toLowerCase(Locale.US));
        return allExtensions.contains(extension);
    }

}
