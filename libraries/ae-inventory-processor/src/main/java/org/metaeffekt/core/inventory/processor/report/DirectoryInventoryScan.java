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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;

public class DirectoryInventoryScan {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryInventoryScan.class);

    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static final String HINT_SCAN = "scan";

    private Inventory globalInventory;
    private String[] scanIncludes;
    private String[] scanExcludes;
    private File inputDirectory;
    private File scanDirectory;

    private boolean enableImplicitUnpack = true;

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory, String[] scanIncludes, String[] scanExcludes, Inventory globalInventory) {
        this.inputDirectory = inputDirectory;

        this.scanDirectory = scanDirectory;
        this.scanIncludes = scanIncludes;
        this.scanExcludes = scanExcludes;

        this.globalInventory = globalInventory;
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
        Copy copy = new Copy();
        copy.setProject(project);
        FileSet set = new FileSet();
        set.setDir(inputDirectory);
        set.setIncludes("**/*");
        copy.addFileset(set);
        copy.setTodir(scanDirectory);
        copy.execute();

        // initialize inventories
        Inventory scanInventory = new Inventory();

        // process scanning
        scanDirectory(scanDirectory, scanDirectory, scanIncludes, scanExcludes, globalInventory, scanInventory);

        scanInventory.mergeDuplicates();

        return scanInventory;
    }

    private void scanDirectory(File scanBaseDir, File scanDir, final String[] scanIncludes, final String[] scanExcludes, Inventory globalInventory, Inventory scanInventory) {
        final String[] filesArray = scanDirectory(scanDir, scanIncludes, scanExcludes);

        List<File> files = new ArrayList<>();
        Arrays.stream(filesArray).map(f -> new File(scanDir, f)).forEach(f -> files.add(f));

        List<ComponentPatternData> matchedComponentDataOnAnchor = new ArrayList<>();
        for (ComponentPatternData cpd : globalInventory.getComponentPatternData()) {
            LOG.info("Checking component pattern: {}", cpd.createCompareStringRepresentation());
            String versionAnchor = File.separatorChar + cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            for (File file : files) {
                if (file.getPath().endsWith(versionAnchor)) {
                    String checksum = FileUtils.computeChecksum(file);
                    ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                    if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK) &&
                            !anchorChecksum.equals(checksum)) {
                        LOG.warn("Anchor checksum mismatch: " + file.getPath());
                        LOG.warn("Expected checksum :{}; actual file checksum: {}", anchorChecksum, checksum);
                        // since the checksum does not match we modify the CPD with a missing version.
                        // this will cause a validation message later, but will give the user enough information
                        // to cure the inventory.
                        copyCpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, "unknown");
                    }

                    copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
                    matchedComponentDataOnAnchor.add(copyCpd);
                    scanInventory.getComponentPatternData().add(copyCpd);

                    // derive artifact from matched component
                    Artifact derivedArtifact = new Artifact();
                    derivedArtifact.setId(copyCpd.get(ComponentPatternData.Attribute.COMPONENT_PART));
                    derivedArtifact.setComponent(copyCpd.get(ComponentPatternData.Attribute.COMPONENT_NAME));
                    derivedArtifact.setVersion(copyCpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
                    derivedArtifact.addProject(FileUtils.asRelativePath(scanBaseDir, file));
                    scanInventory.getArtifacts().add(derivedArtifact);
                }
            }
        }

        // remove the matched file covered by the matched components
        for (ComponentPatternData cpd : matchedComponentDataOnAnchor) {
            String includePattern = File.separatorChar + cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
            String excludePattern = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
            String modifiedExcludePattern = File.separatorChar + excludePattern;

            List<File> matchedFiles = new ArrayList<>();
            for (File file : files) {
                String path = file.getPath();
                if (ANT_PATH_MATCHER.match(includePattern, path)) {
                    if (StringUtils.isEmpty(excludePattern) ||
                            !ANT_PATH_MATCHER.match(modifiedExcludePattern, path)) {
                        LOG.debug("Filtered component file: {}", file);
                        matchedFiles.add(file);
                    }
                }
            }
            files.removeAll(matchedFiles);
        }

        for (File file : files) {
            final String id = file.getName();
            final String checksum = FileUtils.computeChecksum(file);
            final String idFullPath = file.getPath();
            Artifact artifact = globalInventory.findArtifact(id, checksum);

            if (artifact == null) {
                artifact = globalInventory.findArtifact(idFullPath, checksum);
            }

            // match on file name
            if (artifact == null) {
                artifact = globalInventory.findArtifact(id, true);
            }

            // match on file path
            if (artifact == null) {
                artifact = globalInventory.findArtifact(idFullPath, true);
            }

            if (artifact == null) {

                boolean unpacked = false;

                if (enableImplicitUnpack) {
                    // unknown or requires expansion
                    File unpackedFile = unpackIfPossible(file, false);
                    if (unpackedFile != null) {
                        scanDirectory(scanBaseDir, unpackedFile, scanIncludes, scanExcludes, globalInventory, scanInventory);
                        unpacked = true;
                    }
                }

                if (!unpacked) {
                    // add new unknown artifact
                    Artifact newArtifact = new Artifact();
                    newArtifact.setId(id);
                    newArtifact.setChecksum(checksum);
                    newArtifact.addProject(FileUtils.asRelativePath(scanDir, file));
                    scanInventory.getArtifacts().add(newArtifact);
                } else {
                    // NOTE: we should add the unpacked artifact level anyway; need to understand implications
                }
            } else {
                artifact.addProject(FileUtils.asRelativePath(scanBaseDir, file));

                // we use the plain id to continue. The rest is sorted out by the report.
                Artifact copy = new Artifact();
                copy.setId(id);
                copy.setChecksum(checksum);
                copy.addProject(FileUtils.asRelativePath(scanBaseDir, file));
                scanInventory.getArtifacts().add(copy);

                // in case the artifact contains the scan classification we try to unpack and scan in depth
                if (StringUtils.hasText(artifact.getClassification()) && artifact.getClassification().contains(HINT_SCAN)) {
                    File unpackedFile = unpackIfPossible(file, true);
                    if (unpackedFile != null) {
                        scanDirectory(scanBaseDir, unpackedFile, scanIncludes, scanExcludes, globalInventory, scanInventory);
                    } else {
                        throw new IllegalStateException("The artifact with id " + artifact.getId() + " was classified to be scanned in-depth, but cannot be unpacked");
                    }
                }
            }
        }
    }

    private File unpackIfPossible(File archive, boolean includeJarExtension) {
        LOG.info("Expanding {}", archive.getAbsolutePath());

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
