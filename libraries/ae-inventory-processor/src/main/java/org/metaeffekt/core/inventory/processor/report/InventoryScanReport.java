/**
 * Copyright 2009-2018 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class InventoryScanReport extends InventoryReport {

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

    protected void scanDirectory(File scanDir, Inventory globalInventory,
                                 Inventory scanInventory) {
        final String[] files = scanDirectory(scanDir);
        for (int i = 0; i < files.length; i++) {
            final String id = new File(files[i]).getName();
            final String idFullPath = new File(scanDir, files[i]).getPath();
            Artifact artifact = globalInventory.findArtifact(id);
            if (artifact == null) {
                artifact = globalInventory.findArtifact(idFullPath);
            }
            if (artifact == null) {
                // unknown or requires expansion
                File unpackedFile = unpackIfPossible(scanDir, files[i], false);
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
                artifact.addProject(files[i]);

                // we use the plain id to continue. The rest is sorted out by the report.
                Artifact copy = new Artifact();
                copy.setId(id);
                scanInventory.getArtifacts().add(copy);
                copy.addProject(idFullPath);

                // in case the artifact contain the scan classification we try to unpack and scan in depth
                if (artifact.getClassification().contains("scan")) {
                    File unpackedFile = unpackIfPossible(scanDir, files[i], true);
                    if (unpackedFile != null) {
                        scanDirectory(unpackedFile, globalInventory, scanInventory);
                    } else {
                        throw new IllegalStateException("The artifact with id " + artifact.getId() + " was classified to be scanned in-depth, but cannot be unpacked");
                    }
                }
            }
        }
    }

    private File unpackIfPossible(File baseDir, String filepath, boolean includeJarExtension) {
        File archive = new File(baseDir, filepath);

        final Project project = new Project();
        project.setBaseDir(baseDir);

        Set<String> zipExtensions = new HashSet<String>();
        zipExtensions.add("war");
        zipExtensions.add("zip");
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

        // try gunzip
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

        // try untar
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

    protected String[] scanDirectory(final File directoryToScan) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directoryToScan);
        scanner.setIncludes(scanIncludes);
        scanner.setExcludes(scanExcludes);
        scanner.scan();
        final String[] files = scanner.getIncludedFiles();
        return files;
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
