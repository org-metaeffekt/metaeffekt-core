/*
 * Copyright 2009-2026 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.GUnzip;
import org.apache.tools.ant.taskdefs.Zip;
import org.metaeffekt.bundle.sevenzip.SevenZipExecutableUtils;
import org.metaeffekt.core.util.ExecUtils.ExecMonitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.metaeffekt.core.util.ExecUtils.executeAndThrowIOExceptionOnFailure;
import static org.metaeffekt.core.util.ExecUtils.executeCommand;

/**
 * ArchiveUtils for dealing with different archives on core-level.
 */
@Slf4j
public class ArchiveUtils {

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
        // in case no extension is available we still attempt to unzip
        zipExtensions.add("");

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

        // python eggs are also just zips
        zipExtensions.add("egg");

        // gzip: gzip compressed file, less commonly used extension than ".gz"
        gzipExtensions.add("gzip");
        // gz: gzip compressed file
        gzipExtensions.add("gz");

        // tar: various archive formats derived from an old "tape archive" utility
        tarExtensions.add("tar");
        // bz2: bzip2 format compressed files
        tarExtensions.add("bz2");
        // zstd: z compression standard https://github.com/facebook/zstd
        tarExtensions.add("zst");
        // xz: compression format xz (see also "lzma")
        tarExtensions.add("xz");
        // tgz: sometimes used as a shorthand for ".tar.gz"
        tarExtensions.add("tgz");
        // deb: debian package archive
        tarExtensions.add("deb");
        // apk: android package (for apps, special zip), alpine linux package (special tar file)
        tarExtensions.add("apk");
        tarExtensions.add("gem");
        // rpm: red hat package manager
        tarExtensions.add("rpm");
        tarExtensions.add("cpio");

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
     * @throws IOException If the file could not be untared
     */
    public static void untar(File file, File targetDir) throws IOException {
        final String fileName = file.getName().toLowerCase();

        if (!file.exists()) {
            log.warn("Requested to untar path [{}] but file doesn't even exist.", file.getAbsolutePath());
        }

        final List<File> intermediateFiles = new ArrayList<>();

        // preprocess tar wrappers and manage intermediate files
        try {
            if (fileName.endsWith(".gz")) {
                GUnzip gunzip = new GUnzip();
                gunzip.setProject(new Project());
                gunzip.setSrc(file);
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".gz", null));
                gunzip.setDest(target);
                intermediateFiles.add(target);
                gunzip.execute();
                file = target;
            }

            /*
            if (fileName.endsWith(".tgz")) {
                GUnzip gunzip = new GUnzip();
                gunzip.setProject(new Project());
                gunzip.setSrc(file);
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".tgz", ".tar"));
                gunzip.setDest(target);
                intermediateFiles.add(target);

                gunzip.execute();
                file = target;
            }
            */

            if (fileName.endsWith(".xz")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".xz", null));
                intermediateFiles.add(target);

                expandXZ(file, target);
                file = target;
            }

            if (fileName.endsWith(".bz2")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".bz2", null));
                intermediateFiles.add(target);

                expandBzip2(file, target);
                file = target;
            }

            if (fileName.endsWith(".zst")) {
                String targetName = file.getName();
                File target = new File(file.getParentFile(), intermediateUnpackFile(targetName, ".zst", null));
                intermediateFiles.add(target);

                expandZstd(file, target);
                file = target;
            }

        } catch (Exception e) {
            log.warn(e.getMessage());
            deleteIntermediateFiles(intermediateFiles);
        }

        // we may already have expanded something; the new file may have a different extension
        final String extension = FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.US);
        if (tarExtensions.contains(extension) || StringUtils.isEmpty(extension)) {
            try {
                if ("rpm".equals(extension)) {
                    unpackRpmInternal(file, targetDir);
                } else if ("cpio".equals(extension)) {
                    unpackCpioInternal(file, targetDir);
                } else {
                    // untar internal is the preferred approach
                    untarInternal(file, targetDir);
                }
            } catch (Exception e) {
                log.warn("Cannot unpack [{}]. Attempting 7zip to compensate [{}].", file.getAbsolutePath(), e.getMessage());
                unpackWithFallbacks(file, targetDir);
            } finally {
                deleteIntermediateFiles(intermediateFiles);
            }
        }
    }

    private static void unpackWithFallbacks(File file, File targetDir) {
        try {
            FileUtils.forceMkdir(targetDir);
            extractFileWithSevenZip(file, targetDir, true);
        } catch (Exception e) {
            log.warn("Cannot unpack [{}] with 7zip. Attempting native untar to compensate [{}].",
                    file.getAbsolutePath(), e.getMessage());
            try {
                nativeUntar(file, targetDir);
            } catch (Exception ex) {
                throw new IllegalStateException(format("Cannot unpack [%s] using native untar command.", file.getAbsolutePath()), ex);
            }
        }
    }

    private static void deleteIntermediateFiles(Collection<File> intermediateFiles) throws IOException {
        for (File intermediateFile : intermediateFiles) {
            log.trace("Deleting intermediate [{}]", intermediateFile.getAbsolutePath());
            FileUtils.forceDelete(intermediateFile);
        }
    }

    private static String intermediateUnpackFile(String targetName, String suffix, String newSuffix) {
        if (newSuffix != null) {
            return targetName.substring(0, targetName.toLowerCase().lastIndexOf(suffix)) + newSuffix;
        }
        return targetName.substring(0, targetName.toLowerCase().lastIndexOf(suffix));
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
        final BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in, true);

        unpackAndClose(bzIn, Files.newOutputStream(targetFile.toPath()));
    }

    private static void expandZstd(File file, File targetFile) throws IOException {
        final InputStream fin = Files.newInputStream(file.toPath());
        final BufferedInputStream in = new BufferedInputStream(fin);
        final ZstdCompressorInputStream zIn = new ZstdCompressorInputStream(in);

        unpackAndClose(zIn, Files.newOutputStream(targetFile.toPath()));
    }

    /**
     * This untar method supports the desired handling of symbolic links. This method should be preferred.
     *
     * @param file      The file to untar.
     * @param targetDir The target directory to untar to.
     * @throws IOException Throws {@link IOException}s in case of an issue.
     */
    private static void untarInternal(File file, File targetDir) throws IOException {
        try {
            final InputStream fin = Files.newInputStream(file.toPath());
            final BufferedInputStream in = new BufferedInputStream(fin);
            final TarArchiveInputStream xzIn = new TarArchiveInputStream(in);
            if (!targetDir.exists()) {
                FileUtils.forceMkdir(targetDir);
            }
            unpackAndClose(xzIn, targetDir);
        } catch (Exception e) {
            throw new IOException("Could not untar file [" + file.getAbsolutePath() + "]", e);
        }
    }

    private static void unpackRpmInternal(File file, File targetDir) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            FileUtils.forceMkdir(targetDir);
            skipToPayload(in); // skip Lead + Signature Header + Header

            final File cpioFile = new File(targetDir, FilenameUtils.getBaseName(file.getName()) + ".cpio");
            try (InputStream payload = new CompressorStreamFactory().createCompressorInputStream(in)) {
                Files.copy(payload, cpioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IOException("Could not unpack rpm file [" + file.getAbsolutePath() + "]", e);
        }
    }

    private static void unpackCpioInternal(File file, File targetDir) throws IOException {
        try {
            final InputStream fin = Files.newInputStream(file.toPath());
            final BufferedInputStream in = new BufferedInputStream(fin);
            final CpioArchiveInputStream cpioIn = new CpioArchiveInputStream(in);
            if (!targetDir.exists()) {
                FileUtils.forceMkdir(targetDir);
            }
            unpackAndClose(cpioIn, targetDir);
        } catch (Exception e) {
            throw new IOException("Could not unpack cpio file [" + file.getAbsolutePath() + "]", e);
        }
    }

    private static void unpackAndClose(InputStream in, OutputStream out) throws IOException {
        try (in; out) {
            final byte[] buffer = new byte[1024];
            int n;
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static void unpackAndClose(TarArchiveInputStream in, File targetDir) throws IOException {
        try (in) {
            // we need to check the os we are running on
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

            TarArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                final File targetFile = new File(targetDir, entry.getName());

                if (!isWindows) {
                    try {
                        int uid = (Integer) Files.getAttribute(targetDir.toPath(), "unix:uid");
                        int gid = (Integer) Files.getAttribute(targetDir.toPath(), "unix:gid");
                        entry.setUserId(uid);
                        entry.setGroupId(gid);
                    } catch (UnsupportedOperationException e) {
                        log.warn("Unix file attributes not supported on this platform.");
                    }
                }

                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(targetFile);
                } else {
                    if (targetFile.exists() || Files.isSymbolicLink(targetFile.toPath())) {
                        FileUtils.forceDelete(targetFile);
                    }
                    if (entry.isSymbolicLink()) {
                        final String linkName = entry.getLinkName();
                        createSymlink(targetFile, targetDir, linkName);
                    } else {
                        writeFile(in, targetFile);
                    }
                }
            }
        }
    }

    /**
     * Overloaded method for unpacking cpio archives.
     *
     * @param in        the cpio archive input stream
     * @param targetDir the target directory
     * @throws IOException error if an I/O exception occurs
     */
    private static void unpackAndClose(CpioArchiveInputStream in, File targetDir) throws IOException {
        try (in) {
            // hardlinks without data: the content is in the last entry of the inode
            final Map<String, List<File>> pendingLinks = new HashMap<>();

            CpioArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                final File targetFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(targetFile);
                    continue;
                }

                if (targetFile.exists() || Files.isSymbolicLink(targetFile.toPath())) {
                    FileUtils.forceDelete(targetFile);
                }
                FileUtils.forceMkdir(targetFile.getParentFile());

                if (entry.isSymbolicLink()) {
                    // In cpio the link name is the content of the entry. To get it, read the entry's content (entry size bytes)
                    final String linkName = new String(IOUtils.toByteArray(in, (int) entry.getSize()), StandardCharsets.UTF_8);
                    createSymlink(targetFile, targetDir, linkName);
                    continue;
                }

                // A file can exist in the file system under several names (hardlinks in cpio).
                // e.g. /usr/bin/foo and /usr/bin/bar are the same file. They share the same inode (the unique number of the file on a device).
                // CPIO stores a separate entry for EVERY name of the file, all with the same inode.
                // The data, however, is stored only ONCE in the archive, in the LAST entry of that inode. The earlier entries have getSize() == 0
                // even though the file is not actually empty.

                // Key that uniquely identifies a file: device (major/minor) + inode.
                // All names of the same file produce the same key. The device numbers are included because inode numbers are only unique within one device.
                final String key = entry.getDeviceMaj() + ":" + entry.getDeviceMin() + ":" + entry.getInode();

                // If the file has several names (NumberOfLinks > 1) & this entry carries no data (Size == 0), this is one of the early entries
                // whose content comes later in the archive.
                if (entry.getNumberOfLinks() > 1 && entry.getSize() == 0) {
                    // no data yet, create the file once the last entry of this inode arrives
                    pendingLinks.computeIfAbsent(key, k -> new ArrayList<>()).add(targetFile);
                    continue;
                }

                // write file
                try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                    IOUtils.copy(in, out);
                }

                // Were there names with the same inode waiting (identified by key), then this is the last entry of that inode (the one with the data).
                final List<File> waitingLinkFiles = pendingLinks.remove(key);
                if (waitingLinkFiles != null) {
                    for (File linkFile : waitingLinkFiles) {
                        // The parent directory of the waiting name may not exist yet.
                        FileUtils.forceMkdir(linkFile.getParentFile());
                        // Put the file that was just written under the waiting link file name as a copy.
                        // A copy instead of a real hardlink (Files.createLink), so it also works on systems without hardlink support (e.g. some Windows setups).
                        Files.copy(targetFile.toPath(), linkFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // Names that never received data (last entry did not have data and size was 0) are empty files and will be written as empty files.
            for (List<File> rest : pendingLinks.values()) {
                for (File f : rest) {
                    FileUtils.forceMkdir(f.getParentFile());
                    Files.write(f.toPath(), new byte[0]);
                }
            }
        }
    }

    private static void createSymlink(File targetFile, File targetDir, String linkName) {
        if (linkName != null) {
            final Path linkTarget;
            if (linkName.startsWith("/")) {
                // handle absolute paths
                linkTarget = targetDir.toPath().resolve(linkName.substring(1));
            } else {
                // handle relative paths
                linkTarget = targetFile.toPath().getParent().resolve(linkName).normalize();
            }

            try {
                Files.createSymbolicLink(targetFile.toPath(), linkTarget);
            } catch (UnsupportedOperationException | IOException e) {
                log.warn("Symbolic links not supported or insufficient permissions. Skipping symbolic link creation.");
            }
        }
    }

    private static void writeFile(InputStream in, File targetFile) throws IOException {
        final File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            FileUtils.forceMkdir(parentFile);
        }
        try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
            IOUtils.copy(in, out);
        }
    }

    private static void skipToPayload(InputStream in) throws IOException {
        final DataInputStream din = new DataInputStream(in);

        // lead: 96 Bytes
        din.skipNBytes(96);

        // signature header (8 Byte aligned), following main header (not aligned)
        skipHeader(din, true);
        skipHeader(din, false);
    }

    private static void skipHeader(DataInputStream din, boolean align8) throws IOException {
        // Magic (3 Bytes: 8E AD E8) + Version (1) + Reserved (4)
        final byte[] magic = new byte[8];
        din.readFully(magic);
        if ((magic[0] & 0xFF) != 0x8E || (magic[1] & 0xFF) != 0xAD || (magic[2] & 0xFF) != 0xE8) {
            throw new IOException("Invalid rpm header");
        }

        final int indexCount = din.readInt();   // number of index entries
        final int dataSize = din.readInt();   // size of data section

        final long toSkip = 16L * indexCount + dataSize;   // every index entry = 16 Bytes
        din.skipNBytes(toSkip);

        if (align8) {
            final long pad = (8 - (toSkip % 8)) % 8;       // signature header is 8 byte aligned
            din.skipNBytes(pad);
        }
    }

    /**
     * Recursively unpacks all archives in the baseDir to the given analysis directory.
     *
     * @param baseDir     The baseDir to start in.
     * @param analysisDir The analysisDir to unpack to.
     * @param issues      The list of issues to add to.
     * @throws IOException If the file could not be unpacked.
     */
    public static void recursiveUnpack(File baseDir, File analysisDir, List<String> issues) throws IOException {
        final String[] files = FileUtils.scanDirectoryForFiles(baseDir, "**/*.tar", "**/*.tgz", "**/*.bz2",
                "**/*.gz", "**/*.xz", "**/*.tar", "**/*.deb", "**/*.rpm", "**/*.apk", "**/*.zip", "**/*.war", "**/*.cpio",
                "**/*.jar", "**/*.mbizip", "**/*.nupkg", "**/*.nupack", "**/*.aar", "**/*.dll", "**/*.pyd", "**/*.exe",
                "**/*.jmod", "**/modules, **/*.ez");
        for (String file : files) {
            File archiveFile = new File(baseDir, file);
            if (!analysisDir.exists()) {
                FileUtils.forceMkDirQuietly(analysisDir);
                if (unpackIfPossible(archiveFile, analysisDir, new ArrayList<>())) {
                    // recurse into just unpacked folder
                    recursiveUnpack(analysisDir, issues);
                } else {
                    FileUtils.deleteDir(analysisDir);
                    issues.add("Cannot unpack " + archiveFile);
                }
            }
        }
    }

    /**
     * Recursively unpacks all archives in the given directory.
     *
     * @param baseDir The baseDir to start in.
     * @param issues  The list of issues to add to.
     * @throws IOException If the file could not be unpacked.
     */
    public static void recursiveUnpack(File baseDir, List<String> issues) throws IOException {
        final String[] files = FileUtils.scanDirectoryForFiles(baseDir, "**/*.tar", "**/*.tgz", "**/*.bz2",
                "**/*.gz", "**/*.xz", "**/*.tar", "**/*.deb", "**/*.rpm", "**/*.apk", "**/*.zip", "**/*.war", "**/*.cpio",
                "**/*.jar", "**/*.mbizip", "**/*.nupkg", "**/*.nupack", "**/*.aar", "**/*.dll", "**/*.pyd", "**/*.exe",
                "**/*.jmod", "**/modules, **/*.ez");
        for (String file : files) {
            File archiveFile = new File(baseDir, file);
            File targetPath = new File(archiveFile.getParentFile(), "[" + archiveFile.getName() + "]");
            if (!targetPath.exists()) {
                FileUtils.forceMkDirQuietly(targetPath);
                if (unpackIfPossible(archiveFile, targetPath, new ArrayList<>())) {
                    // delete the file after successful expansion
                    FileUtils.forceDelete(archiveFile);

                    // recurse into just unpacked folder
                    recursiveUnpack(targetPath, issues);
                } else {
                    FileUtils.deleteDir(targetPath);
                    issues.add("Cannot unpack " + archiveFile);
                }
            }
        }
    }

    public static boolean unpackIfPossible(File archiveFile, File targetDir, List<String> issues) {
        if (!archiveFile.exists() || !archiveFile.getParentFile().exists()) {
            log.warn("Trying to unpack a file, which does not exists (anymore): {}", archiveFile);
            return false;
        }

        final Project project = new Project();
        project.setBaseDir(archiveFile.getParentFile());

        log.info("Attempting unpacking: " + archiveFile.getAbsolutePath());

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
            // only report an issue, in case the extension was non-blank
            if (!StringUtils.isBlank(extension)) {
                issues.add("Cannot unzip " + archiveFile.getAbsolutePath());
            }
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
            log.error(e.getMessage(), e);
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
                extractFileWithSevenZip(archiveFile, targetDir, true);
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
            final String[] commandParts = new String[]{jmodExecutable.getAbsolutePath(), "extract", file.getAbsolutePath()};
            executeExtraction(commandParts, file, targetFile, true, false);
        } else {
            log.error("Cannot unpack jmod executable: " + jmodExecutable +
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
            final String[] commandParts = new String[]{jImageExecutable.getAbsolutePath(), "extract", file.getAbsolutePath()};
            executeExtraction(commandParts, file, targetFile, true, false);
        } else {
            log.error("Cannot unpack jimage executable: " + jImageExecutable +
                    ". Ensure property jdk.path is set and points to a JDK with version > 11.0.");
        }
    }

    private static ExecMonitor extractFileWithSevenZip(File file, File targetFile, boolean throwExceptionOnError) throws IOException {
        // this requires 7zip to perform the extraction
        final File sevenZipBinaryFile = SevenZipExecutableUtils.getBinaryFile();
        if (sevenZipBinaryFile.exists()) {
            final String[] commandParts = {sevenZipBinaryFile.getAbsolutePath(), "x",
                    file.getAbsolutePath(), "-aoa", "-o" + targetFile.getAbsolutePath()};
            return executeExtraction(commandParts, file, targetFile, throwExceptionOnError, true);
        } else {
            log.error("Cannot unpack file: " + file.getAbsolutePath() + " with 7zip. Ensure 7zip is installed at [" + sevenZipBinaryFile.getAbsolutePath() + "].");
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
        //  this last-ditch effort to give good support.
        String[] commandParts = new String[]{
                "tar", "-x", "-f", file.getAbsolutePath(),
                "--no-same-permissions", "-C", targetFile.getAbsolutePath()};
        executeExtraction(commandParts, file, targetFile, true, false);
    }

    private static ExecMonitor executeExtraction(String[] commandParts, File file, File targetFile, boolean throwExceptionOnError, boolean sevenZipContext) throws IOException {

        final ExecUtils.ExecParam execParam = new ExecUtils.ExecParam(commandParts);

        // apply standard configuration
        execParam.destroyOnTimeout(true);
        execParam.retainErrorOutputs();
        execParam.setWorkingDir(targetFile);
        execParam.timeoutAfter(EXTRACT_DURATION, EXTRACT_DURATION_TIMEOUT_UNIT);

        ExecMonitor execMonitor = throwExceptionOnError ? executeAndThrowIOExceptionOnFailure(execParam) : executeCommand(execParam);
        if (sevenZipContext) {
            attemptUnpackingIntermediateArchive(targetFile);
        }
        return execMonitor;
    }

    private static void attemptUnpackingIntermediateArchive(File targetFile) {
        try {
            final String[] files = FileUtils.scanDirectoryForFiles(targetFile, "*");
            if (files.length == 1) {
                for (String unpackedFileName : files) {
                    final File unpackedFile = new File(targetFile, unpackedFileName);
                    if (!unpackedFile.isDirectory()) {
                        if (unpackedFileName.endsWith("~")) {
                            nativeUntar(unpackedFile, targetFile);
                            FileUtils.forceDelete(unpackedFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot unpack intermediate archives in: " + targetFile, e);
        }
    }

    public static boolean isArchiveByName(String pathOrName) {
        if (pathOrName == null) return false;
        final String extension = FilenameUtils.getExtension(pathOrName.toLowerCase(Locale.US));
        return allExtensions.contains(extension);
    }

}
