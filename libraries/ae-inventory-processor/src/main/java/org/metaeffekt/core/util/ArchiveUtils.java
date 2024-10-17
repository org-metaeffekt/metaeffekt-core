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

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * ArchiveUtils for dealing with different archives on core-level.
 */
public class ArchiveUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveUtils.class);

    private static final long UNTAR_TIMEOUT_NUMBER = 1;
    private static final TimeUnit UNTAR_TIMEOUT_UNIT = TimeUnit.HOURS;

    private static final Set<String> zipExtensions = new HashSet<>();
    private static final Set<String> gzipExtensions = new HashSet<>();
    private static final Set<String> tarExtensions = new HashSet<>();

    private static final Set<String> jmodExtensions = new HashSet<>();
    private static final Set<String> jimageExtensions = new HashSet<>();

    private static final Set<String> jimageFilenames = new HashSet<>();

    private static final Set<String> windowsExtensions = new HashSet<>();

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
                extractAndCopyWithSymlinks(file, targetDir, ArchiveFormat.GZIP);
            }

            if (fileName.endsWith(".tgz")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".tgz"));
                intermediateFiles.add(target);

                expandTGZ(file, target);
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

    private static void expandTGZ(File file, File targetFile) throws IOException {
        final InputStream fin = Files.newInputStream(file.toPath());
        final BufferedInputStream in = new BufferedInputStream(fin);
        final GzipCompressorInputStream tgzIn = new GzipCompressorInputStream(in);

        unpackAndClose(tgzIn, Files.newOutputStream(targetFile.toPath()));
    }

    private static void expandBzip2(File file, File targetFile) throws IOException {
        final InputStream fin = Files.newInputStream(file.toPath());
        final BufferedInputStream in = new BufferedInputStream(fin);
        final BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);

        unpackAndClose(bzIn, Files.newOutputStream(targetFile.toPath()));
    }

    private static void untarInternal(File file, File targetFile) throws IOException {
        try {
            extractAndCopyWithSymlinks(file, targetFile, ArchiveFormat.TAR);
        } catch (Exception exception) {
            LOG.debug(
                    "Cannot untar file [{}] due to [{}]. Attempting untar via command line.",
                    file.getAbsolutePath(),
                    exception.getMessage()
            );

            // FIXME: this fallback doesn't adjust file permissions, leading to "Permission denied" while scanning.
            //  this also means that prepareScanDirectory may fail on rescan.
            //  we should probably just make sure that the java-native unwrao doesn't fail instead of relying on
            //  this last-ditch efford to give good support.
            // fallback to native support on command line

            Process tarExtract = new ProcessBuilder().command(
                            "tar",
                            "-x",
                            "-f", file.getAbsolutePath(),
                            "--no-same-permissions",
                            "-C", targetFile.getAbsolutePath())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            // wait for untar. if our untar takes more than one hour, we can be pretty sure that something is broken
            try {
                if (!tarExtract.waitFor(UNTAR_TIMEOUT_NUMBER, UNTAR_TIMEOUT_UNIT)) {
                    // timeout
                    LOG.error("Failed to untar [{}].", file.getAbsolutePath());

                    LOG.error("Killing untar process...");
                    tarExtract.destroyForcibly();
                    if (!tarExtract.waitFor(1, TimeUnit.MINUTES)) {
                        // once we are in Java 9 or newer, we should output PID in this case
                        LOG.error("Failed to kill untar! This will leave a tar process with unknown state!");
                    }

                    throw new IOException("Untar failed: timed out.");
                }
            } catch (InterruptedException e) {
                throw new IOException("Untar thread was interrupted.", e);
            }

            if (tarExtract.exitValue() != 0) {
                LOG.error(
                        "Untar of [{}] failed with exit value [{}].",
                        file.getAbsolutePath(),
                        tarExtract.exitValue()
                );
                throw new IOException("Failed to untar requested file.");
            }

            // NOTE: further exceptions are handled upstream
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


    public static void extractAndCopyWithSymlinks(File fileToUnzip, File targetDir, ArchiveFormat format) throws IOException {
        IInArchive archive = SevenZip.openInArchive(format, new RandomAccessFileInStream(new RandomAccessFile(fileToUnzip, "r")));

        // use IntStream to generate the array of item indices
        int[] items = IntStream.range(0, archive.getNumberOfItems()).toArray();

        archive.extract(items, false, // Non-test mode
                new MyExtractCallback(archive, targetDir));
        archive.close();
    }

    public static boolean unpackIfPossible(File archiveFile, File targetDir, List<String> issues) {
        final String archiveFileName = archiveFile.getName().toLowerCase();
        final String extension = FilenameUtils.getExtension(archiveFileName);

        boolean mkdir = !targetDir.exists();

        // try unzip
        try {
            if (zipExtensions.contains(extension)) {
                FileUtils.forceMkdir(targetDir);
                extractAndCopyWithSymlinks(archiveFile, targetDir, ArchiveFormat.ZIP);
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
                extractAndCopyWithSymlinks(archiveFile, targetDir, ArchiveFormat.GZIP);
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
                extractWindowsFile(archiveFile, targetDir);
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

    private static void extractWindowsFile(File file, File targetFile) {
        // this requires 7zip to perform the extraction
        try {
            Process exec = Runtime.getRuntime().exec("7z x " + file.getAbsolutePath() + " -o" + targetFile.getAbsolutePath());
            FileUtils.waitForProcess(exec);
        } catch (IOException e) {
            LOG.error("Cannot unpack windows file: " + file.getAbsolutePath() + ". Ensure 7zip is installed.");
        }
    }

    static class MyExtractCallback implements IArchiveExtractCallback {
        private final IInArchive inArchive;
        private final File targetDir;

        public MyExtractCallback(IInArchive inArchive, File targetDir) {
            this.inArchive = inArchive;
            this.targetDir = targetDir;
        }

        public ISequentialOutStream getStream(int index,
                                              ExtractAskMode extractAskMode) throws SevenZipException {
            if (extractAskMode != ExtractAskMode.EXTRACT) {
                return null;
            }

            String path = (String) inArchive.getProperty(index, PropID.PATH);
            File outputFile = new File(targetDir, path);
            boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            String symLink = inArchive.getStringProperty(index, PropID.SYM_LINK);

            // create parent directories if necessary
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                try {
                    FileUtils.forceMkdir(outputFile.getParentFile());
                } catch (IOException e) {
                    throw new SevenZipException("Error creating parent directory: " + outputFile.getParentFile(), e);
                }
            }

            // handle directories
            if (isFolder) {
                if (!outputFile.exists()) {
                    try {
                        FileUtils.forceMkdir(outputFile);
                    } catch (IOException e) {
                       throw new SevenZipException("Error creating directory: " + outputFile, e);
                    }
                }
                return null; // directories don't need a stream
            }

            // handle symbolic links
            if (!StringUtils.isBlank(symLink)) {
                // Resolve the symbolic link target based on its relative path
                Path symlinkTargetPath;
                if (symLink.startsWith("../")) {
                    // handle relative symbolic link
                    symlinkTargetPath = outputFile.getParentFile().toPath().resolve(symLink).normalize();
                } else if (!symLink.contains("/")) {
                    // handle symlinks within the same directory (e.g., "alternatives")
                    symlinkTargetPath = outputFile.getParentFile().toPath().resolve(symLink).normalize();
                } else {
                    // handle absolute or direct symbolic link within the target directory
                    symlinkTargetPath = new File(targetDir, symLink).toPath();
                }

                // create the symbolic link
                try {
                    Files.createSymbolicLink(outputFile.toPath(), symlinkTargetPath);
                    LOG.info("Created symbolic link [{}] -> [{}].", outputFile, symlinkTargetPath);
                } catch (IOException e) {
                    throw new SevenZipException("Error creating symbolic link: " + outputFile + " -> " + symlinkTargetPath, e);
                }
                return null;
            }

            // handle file extraction
            try {
                final OutputStream out = Files.newOutputStream(outputFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return data -> {
                    try {
                        out.write(data);
                        out.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return data.length;
                };
            } catch (IOException e) {
                throw new SevenZipException("Error writing to file: " + outputFile, e);
            }
        }

        public void prepareOperation(ExtractAskMode extractAskMode) {
        }

        public void setOperationResult(ExtractOperationResult
                                               extractOperationResult) {
            if (extractOperationResult != ExtractOperationResult.OK) {
                LOG.error("Extract operation result is not OK: {}", extractOperationResult);
            }
        }

        public void setCompleted(long completeValue) {
        }

        public void setTotal(long total) {
        }

    }

}
