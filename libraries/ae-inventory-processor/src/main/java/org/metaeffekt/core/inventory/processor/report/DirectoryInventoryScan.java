/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.FileSet;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.util.FileUtils.*;

public class DirectoryInventoryScan {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryInventoryScan.class);

    public static final String HINT_SCAN = "scan";

    public static final String DOUBLE_ASTERISK = Constants.ASTERISK + Constants.ASTERISK;

    private Inventory referenceInventory;
    private String[] scanIncludes;
    private String[] scanExcludes;
    private File inputDirectory;
    private File scanDirectory;

    private boolean enableImplicitUnpack = true;

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory, String[] scanIncludes, String[] scanExcludes, Inventory referenceInventory) {
        this.inputDirectory = inputDirectory;

        this.scanDirectory = scanDirectory;
        this.scanIncludes = scanIncludes;
        this.scanExcludes = scanExcludes;

        this.referenceInventory = referenceInventory;
    }

    public Inventory createScanInventory() {
        final Project project = new Project();

        // delete scan directory (content is progressively unpacked)
        Delete delete = new Delete();
        delete.setProject(project);
        delete.setDir(scanDirectory);
        delete.execute();

        // ensure scan directory root folder is recreated
        scanDirectory.mkdirs();

        // copy files to folder that are of interest
        final Copy copy = new Copy();
        copy.setProject(project);
        FileSet set = new FileSet();
        set.setDir(inputDirectory);
        set.setIncludes(combinePatterns(scanIncludes, "**/*"));
        set.setExcludes(combinePatterns(scanExcludes, "--nothing--"));
        copy.addFileset(set);
        copy.setTodir(scanDirectory);
        copy.execute();

        return performScan();
    }

    private String combinePatterns(String[] patterns, String defaultPattern) {
        if (patterns == null) return defaultPattern;
        return Arrays.stream(patterns).collect(Collectors.joining(","));
    }

    public Inventory performScan() {
        // initialize inventories
        Inventory scanInventory = new Inventory();

        // process scanning
        scanDirectory(scanDirectory, scanDirectory, scanIncludes, scanExcludes, referenceInventory, scanInventory);

        scanInventory.mergeDuplicates();

        return scanInventory;
    }

    private static class MatchResult {
        ComponentPatternData componentPatternData;
        File anchorFile;
        MatchResult(ComponentPatternData componentPatternData, File anchorFile) {
            this.componentPatternData = componentPatternData;
            this.anchorFile = anchorFile;
        }
    }

    /**
     * Scans the directory recursively.
     *
     * All paths are relative to the scanBaseDir.
     *
     * @param scanBaseDir The scan base dir. Use always the same root folder. Also for the recursion.
     * @param scanDir The scan dir. Changes with the recursion.
     * @param scanIncludes
     * @param scanExcludes
     * @param referenceInventory
     * @param scanInventory
     */
    private void scanDirectory(File scanBaseDir, File scanDir, final String[] scanIncludes, final String[] scanExcludes, Inventory referenceInventory, Inventory scanInventory) {
        // scan the directory using includes and excludes; scans the full tree (maybe not yet unwrapped)
        final String[] filesArray = scanDirectory(scanDir, scanIncludes, scanExcludes);

        // collect all files as list; the list is explicitly created as we later modify the content
        final Set<File> files = new HashSet<>();
        Arrays.stream(filesArray).map(f -> new File(scanDir, f)).forEach(files::add);

        final List<MatchResult> matchedComponentPatterns = matchComponentPatterns(files, scanBaseDir, scanDir, referenceInventory, scanInventory);

        filterFilesMatchedByComponentPatterns(files, scanBaseDir, matchedComponentPatterns);

        populateInventoryWithScannedFiles(scanBaseDir, scanIncludes, scanExcludes, referenceInventory, scanInventory, files);
    }

    private List<MatchResult> matchComponentPatterns(Set<File> files, File scanBaseDir, File scanDir, Inventory referenceInventory, Inventory scanInventory) {
        // match component patterns using version anchor version anchors; results in matchedComponentPatterns
        final List<MatchResult> matchedComponentPatterns = new ArrayList<>();

        for (final ComponentPatternData cpd : referenceInventory.getComponentPatternData()) {
            LOG.debug("Checking component pattern: {}", cpd.createCompareStringRepresentation());

            final String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            final String versionAnchor = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            final String normalizedVersionAnchor = normalizePathToLinux(versionAnchor);

            if (versionAnchor == null) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (versionAnchor.contains(DOUBLE_ASTERISK)) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must not contain **. Use * only.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (anchorChecksum == null) {
                throw new IllegalStateException(String.format("The version anchor checksum of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }

            // memorize whether the path fragment of version anchor contains wildcards
            final boolean isVersionAnchorPattern = versionAnchor.contains(Constants.ASTERISK);

            // memorize whether version anchor checksum is specific (and not just *)
            final boolean isVersionAnchorChecksumSpecific = !anchorChecksum.equalsIgnoreCase(Constants.ASTERISK);

            if (versionAnchor.equalsIgnoreCase(Constants.ASTERISK) || versionAnchor.equalsIgnoreCase(Constants.DOT)) {

                if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK)) {
                    throw new IllegalStateException(String.format(
                        "The version anchor checksum of component pattern [%s] with version anchor [%s] must be '*'.",
                            cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN), versionAnchor));
                }

                final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, Constants.ASTERISK);
                matchedComponentPatterns.add(new MatchResult(copyCpd, scanDir));

                // derive artifact from matched component
                scanInventory.getArtifacts().add(deriveArtifact(scanBaseDir, scanDir, copyCpd));

                // continue with next component pattern (otherwise this would produce a hugh amount of matched patterns)
                continue;
            }

            // check whether the version anchor path fragment matches one of the file paths
            for (final File file : files) {
                // generate normalized path relative to scanBaseDir (important; not to scanDir, which may vary as we
                // descend into the hierarchy on recursion)
                final String normalizedPath = normalizePathToLinux(asRelativePath(scanBaseDir, file));

                if (versionAnchorMatches(normalizedVersionAnchor, normalizedPath, isVersionAnchorPattern)) {

                    // on match infer the checksum of the file
                    final String fileChecksumOrAsterisk = isVersionAnchorChecksumSpecific ? computeChecksum(file) : Constants.ASTERISK;

                    if (!anchorChecksum.equalsIgnoreCase(fileChecksumOrAsterisk)) {
                        LOG.debug("Anchor fileChecksumOrAsterisk mismatch: " + file.getPath());
                        LOG.debug("Expected fileChecksumOrAsterisk :{}; actual file fileChecksumOrAsterisk: {}", anchorChecksum, fileChecksumOrAsterisk);
                    }  else {
                        final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                        copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, fileChecksumOrAsterisk);
                        matchedComponentPatterns.add(new MatchResult(copyCpd, file));

                        // derive artifact from matched component
                        scanInventory.getArtifacts().add(deriveArtifact(
                                scanBaseDir, computeComponentBaseDir(scanBaseDir, file, normalizedVersionAnchor), copyCpd));
                    }
                }
            }
        }
        return matchedComponentPatterns;
    }

    private Artifact deriveArtifact(File scanBaseDir, File scanDir, ComponentPatternData componentPatternData) {
        final Artifact derivedArtifact = new Artifact();
        derivedArtifact.setId(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_PART));
        derivedArtifact.setComponent(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_NAME));
        derivedArtifact.setVersion(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
        derivedArtifact.addProject(asRelativePath(scanBaseDir, scanDir));
        return derivedArtifact;
    }

    private void populateInventoryWithScannedFiles(File scanBaseDir, String[] scanIncludes, String[] scanExcludes, Inventory referenceInventory, Inventory scanInventory, Set<File> files) {
        for (final File file : files) {
            final String id = file.getName();
            final String checksum = computeChecksum(file);
            final String idFullPath = file.getPath();
            Artifact artifact = referenceInventory.findArtifactByIdAndChecksum(id, checksum);

            if (artifact == null) {
                artifact = referenceInventory.findArtifactByIdAndChecksum(idFullPath, checksum);
            }

            // match on file name
            if (artifact == null) {
                artifact = referenceInventory.findArtifact(id, true);
                if (!matchesChecksumIfAvailable(artifact, checksum)) {
                    artifact = null;
                }
            }

            // match on file path
            if (artifact == null) {
                artifact = referenceInventory.findArtifact(idFullPath, true);
                if (!matchesChecksumIfAvailable(artifact, checksum)) {
                    artifact = null;
                }
            }

            if (artifact == null) {
                boolean unpacked = false;

                if (enableImplicitUnpack) {
                    // unknown or requires expansion
                    File unpackedFile = unpackIfPossible(file, false);
                    if (unpackedFile != null) {
                        scanDirectory(scanBaseDir, unpackedFile, scanIncludes, scanExcludes, referenceInventory, scanInventory);
                        unpacked = true;
                    }
                }

                if (!unpacked) {
                    // add new unknown artifact
                    Artifact newArtifact = new Artifact();
                    newArtifact.setId(id);
                    newArtifact.setChecksum(checksum);
                    newArtifact.addProject(asRelativePath(scanBaseDir, file));
                    scanInventory.getArtifacts().add(newArtifact);
                } else {
                    // NOTE: we should add the unpacked artifact level anyway; need to understand implications
                }
            } else {
                artifact.addProject(asRelativePath(scanBaseDir, file));

                // we use the plain id to continue. The rest is sorted out by the report.
                Artifact copy = new Artifact();
                copy.setId(id);
                copy.setChecksum(checksum);
                copy.addProject(asRelativePath(scanBaseDir, file));
                scanInventory.getArtifacts().add(copy);

                // in case the artifact contains the scan classification we try to unpack and scan in depth
                if (StringUtils.hasText(artifact.getClassification()) && artifact.getClassification().contains(HINT_SCAN)) {
                    File unpackedFile = unpackIfPossible(file, true);
                    if (unpackedFile != null) {
                        scanDirectory(scanBaseDir, unpackedFile, scanIncludes, scanExcludes, referenceInventory, scanInventory);
                    } else {
                        throw new IllegalStateException("The artifact with id " + artifact.getId() + " was classified to be scanned in-depth, but cannot be unpacked");
                    }
                }
            }
        }
    }

    private void filterFilesMatchedByComponentPatterns(Set<File> files, File scanBaseDir, List<MatchResult> matchedComponentDataOnAnchor) {
        // remove the matched file covered by the matched components
        for (MatchResult matchResult : matchedComponentDataOnAnchor) {
            final ComponentPatternData cpd = matchResult.componentPatternData;
            final File anchorFile = matchResult.anchorFile;

            final String versionAnchor = normalizePathToLinux(cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR));

            final File baseDir = computeComponentBaseDir(scanBaseDir, anchorFile, versionAnchor);

            // build patters to match (using scanBaseDir relative paths)
            final String baseDirPath = normalizePathToLinux(asRelativePath(scanBaseDir, baseDir));
            final String normalizedIncludePattern = extendIncludePattern(cpd, baseDirPath);
            final String normalizedExcludePattern = normalizePathToLinux(cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN));

            final List<File> matchedFiles = new ArrayList<>();
            for (final File file : files) {
                final String normalizedPath = normalizePathToLinux(asRelativePath(scanBaseDir, file));
                if (matches(normalizedIncludePattern, normalizedPath)) {
                    if (StringUtils.isEmpty(normalizedExcludePattern) || !matches(normalizedExcludePattern, normalizedPath)) {
                        LOG.debug("Filtered component file: {} for component pattern {}", file, cpd.deriveQualifier());
                        matchedFiles.add(file);
                    }
                }
            }
            files.removeAll(matchedFiles);
        }
    }

    private File computeComponentBaseDir(File scanBaseDir, File anchorFile, String versionAnchor) {
        if (Constants.ASTERISK.equalsIgnoreCase(versionAnchor)) return scanBaseDir;
        if (Constants.DOT.equalsIgnoreCase(versionAnchor)) return scanBaseDir;

        final int versionAnchorFolderDepth = StringUtils.countOccurrencesOf(versionAnchor, "/") + 1;

        File baseDir = anchorFile;
        for (int i = 0; i < versionAnchorFolderDepth; i++) {
            baseDir = baseDir.getParentFile();

            // handle special case the the parent dir does not exist (for whatever reason)
            if (baseDir == null) {
                baseDir = scanBaseDir;
                break;
            }
        }
        return baseDir;
    }

    private String extendIncludePattern(ComponentPatternData cpd, String baseDirPath) {
        String p = cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
        if (p == null) return p;

        if (StringUtils.isEmpty(baseDirPath)) return p;
        if (Constants.DOT.equals(baseDirPath)) return p;
        if (Constants.DOT_SLASH.equals(baseDirPath)) return p;

        String[] patterns = p.split(",");
        return normalizePathToLinux(Arrays.stream(patterns)
                .map(String::trim)
                .map(s -> baseDirPath + File.separatorChar + s)
                .collect(Collectors.joining(",")));
    }

    private boolean versionAnchorMatches(String normalizedVersionAnchor, String normalizedPath, boolean isVersionAnchorPattern) {
        return (!isVersionAnchorPattern && normalizedPath.endsWith(normalizedVersionAnchor)) ||
                (isVersionAnchorPattern && matches("**/" + normalizedVersionAnchor, normalizedPath));
    }

    private boolean matchesChecksumIfAvailable(Artifact artifact, String checksum) {
        if (artifact == null) return false;
        final String artifactChecksum = artifact.getChecksum();
        if (!StringUtils.hasText(artifactChecksum)) return true; // no checksum available
        return artifactChecksum.equals(checksum);
    }

    private File unpackIfPossible(File archive, boolean includeJarExtension) {
        LOG.debug("Expanding {}", archive.getAbsolutePath());

        final Project project = new Project();
        project.setBaseDir(archive.getParentFile());

        Set<String> zipExtensions = new HashSet<String>();
        zipExtensions.add("war");
        zipExtensions.add("zip");
        zipExtensions.add("ear");
        zipExtensions.add("sar");
        zipExtensions.add("rar");

        if (includeJarExtension) {
            zipExtensions.add("jar");
        }

        Set<String> gzipExtensions = new HashSet<String>();
        gzipExtensions.add("gzip");
        gzipExtensions.add("gz");
        gzipExtensions.add("tgz");

        Set<String> tarExtensions = new HashSet<String>();
        tarExtensions.add("tar");

        final String extension = FilenameUtils.getExtension(archive.getName()).toLowerCase();

        // try unzip
        try {
            if (zipExtensions.contains(extension)) {
                Expand expandTask = new Expand();
                expandTask.setProject(project);
                File targetFolder = new File(archive.getParentFile(), "[" + archive.getName() + "]");
                targetFolder.mkdirs();
                expandTask.setDest(targetFolder);
                expandTask.setSrc(archive);
                expandTask.execute();
                deleteArchive(archive);
                return targetFolder;
            }
        } catch (Exception e) {
            LOG.error("Cannot unzip " + archive.getAbsolutePath());
        }

        // try gunzip
        try {
            if (gzipExtensions.contains(extension)) {
                GUnzip expandTask = new GUnzip();
                expandTask.setProject(project);
                File targetFolder = new File(archive.getParentFile(), "[" + archive.getName() + "]");
                targetFolder.mkdirs();
                expandTask.setDest(targetFolder);
                expandTask.setSrc(archive);
                expandTask.execute();
                deleteArchive(archive);
                return targetFolder;
            }
        } catch (Exception e) {
            LOG.error("Cannot gunzip " + archive.getAbsolutePath());
        }

        // try untar
        try  {
            if (tarExtensions.contains(extension)) {
                Untar expandTask = new Untar();
                expandTask.setProject(project);
                File targetFolder = new File(archive.getParentFile(), "[" + archive.getName() + "]");
                targetFolder.mkdirs();
                expandTask.setDest(targetFolder);
                expandTask.setSrc(archive);
                expandTask.execute();
                deleteArchive(archive);
                return targetFolder;
            }
        } catch (Exception e) {
            LOG.error("Cannot untar " + archive.getAbsolutePath());
        }

        return null;
    }

    public void deleteArchive(File archive) {
        try {
            archive.delete();
        } catch (Exception e) {
            if (archive.exists()) {
                archive.deleteOnExit();
            }
        }
    }

    // FIXME: DirectoryScanner is not performing very well
    protected String[] scanDirectory(final File directoryToScan, final String[] scanIncludes, final String[] scanExcludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directoryToScan);
        scanner.setIncludes(scanIncludes);
        scanner.setExcludes(scanExcludes);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public void setEnableImplicitUnpack(boolean enableImplicitUnpack) {
        this.enableImplicitUnpack = enableImplicitUnpack;
    }

}
