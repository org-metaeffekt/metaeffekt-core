/**
 * Copyright 2009-2019 the original author or authors.
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

public class InventoryScanReport extends InventoryReport {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryScanReport.class);

    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private File inputDirectory;

    private File scanDirectory;

    private String[] scanIncludes = new String[]{"**/*"};

    private String[] scanExcludes;

    @Override
    public boolean createReport() throws Exception {
        final Project project = new Project();

        // delete scan directory
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
        Inventory globalInventory = readGlobalInventory();
        Inventory scanInventory = new Inventory();

        // process scanning
        scanDirectory(scanDirectory, globalInventory, scanInventory);

        scanInventory.mergeDuplicates();

        // insert (potentially incomplete) artifacts for reporting
        // this supports adding licenses and obligations into the
        // report, which are not covered by the repository.
        if (getAddOnArtifacts() != null) {
            scanInventory.getArtifacts().addAll(getAddOnArtifacts());
        }

        // produce reports and write files to disk
        return createReport(globalInventory, scanInventory);
    }

    protected void scanDirectory(File scanDir, Inventory globalInventory, Inventory scanInventory) {
        final String[] filesArray = scanDirectory(scanDir);

        List<File> files = new ArrayList<>();
        Arrays.stream(filesArray).map(f -> new File(scanDir, f)).forEach(f -> files.add(f));

        List<ComponentPatternData> matchedComponentDataOnAnchor = new ArrayList<>();
        for (ComponentPatternData cpd : globalInventory.getComponentPatternData()) {
            LOG.debug("Checking CPD: {}", cpd.createCompareStringRepresentation());
            String versionAnchor = "/" + cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            for (File file : files) {
                if (file.getPath().endsWith(versionAnchor)) {
                    LOG.debug("Found anchor: {}", file.getPath());
                    String checksum = FileUtils.computeChecksum(file);
                    ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                    if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK) &&
                            !anchorChecksum.equals(checksum)) {
                        LOG.warn("Anchor checksum mismatch: " + file.getPath());
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
                    scanInventory.getArtifacts().add(derivedArtifact);
                }
            }
        }

        // remove the matched file covered by the matched components
        for (ComponentPatternData cpd : matchedComponentDataOnAnchor) {
            String includePattern = "/" + cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
            String excludePattern = cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN);
            String modifiedExcludePattern = "/" + excludePattern;

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
            final String idFullPath = file.getPath();
            Artifact artifact = globalInventory.findArtifact(id, true);
            if (artifact == null) {
                artifact = globalInventory.findArtifact(idFullPath, true);
            }
            if (artifact == null) {
                // unknown or requires expansion
                File unpackedFile = unpackIfPossible(file, false);
                if (unpackedFile == null) {
                    // add new unknown artifact
                    Artifact newArtifact = new Artifact();
                    newArtifact.setId(id);
                    newArtifact.addProject(idFullPath);
                    scanInventory.getArtifacts().add(newArtifact);
                } else {
                    scanDirectory(unpackedFile, globalInventory, scanInventory);
                }
            } else {
                artifact.addProject(file.getPath());

                // we use the plain id to continue. The rest is sorted out by the report.
                Artifact copy = new Artifact();
                copy.setId(id);
                scanInventory.getArtifacts().add(copy);
                copy.addProject(idFullPath);

                // in case the artifact contains the scan classification we try to unpack and scan in depth
                if (artifact.getClassification().contains("scan")) {
                    File unpackedFile = unpackIfPossible(file, true);
                    if (unpackedFile != null) {
                        scanDirectory(unpackedFile, globalInventory, scanInventory);
                    } else {
                        throw new IllegalStateException("The artifact with id " + artifact.getId() + " was classified to be scanned in-depth, but cannot be unpacked");
                    }
                }
            }
        }
    }

    private File unpackIfPossible(File archive, boolean includeJarExtension) {
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
    protected String[] scanDirectory(final File directoryToScan) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directoryToScan);
        scanner.setIncludes(scanIncludes);
        scanner.setExcludes(scanExcludes);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public File getScanDirectory() {
        return scanDirectory;
    }

    public void setScanDirectory(File scanDirectory) {
        this.scanDirectory = scanDirectory;
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public void setInputDirectory(File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    public String[] getScanIncludes() {
        return scanIncludes;
    }

    public void setScanIncludes(String[] scanIncludes) {
        this.scanIncludes = scanIncludes;
    }

    public String[] getScanExcludes() {
        return scanExcludes;
    }

    public void setScanExcludes(String[] scanExcludes) {
        this.scanExcludes = scanExcludes;
    }

}
