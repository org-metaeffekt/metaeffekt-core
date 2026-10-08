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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.filepatterns.FileComponentPatternProcessor;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.inventory.extractor.*;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Extracts a container inventory from pre-preprocessed container information.
 */
@Mojo(name = "extract-container-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ContainerInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    // the mojo ingest an archive as primary input
    @Parameter
    protected File inputArchiveFile;

    // use legacy property to configure inputDir
    @Parameter(required = true, defaultValue = "${ae.extractor.analysis.dir}")
    protected File analysisDir;

    @Parameter(defaultValue = "false")
    protected boolean filterPackagesWithoutVersion = false;

    @Parameter(defaultValue = "false")
    protected boolean filterArtifactsWithoutVersion = false;

    @Parameter(defaultValue = "true")
    protected boolean activateFileLevelProcessing = true;

    @Parameter(required = true)
    protected File excludePatternsFile;

    private final InventoryExtractor[] inventoryExtractors = new InventoryExtractor[]{
            new DebianInventoryExtractor(), // -> AptBasedInventoryExtractor
            new CentOSInventoryExtractor(), // -> RpmBasedInventoryExtractor
            new AlpineInventoryExtractor(), // -> ApkBasedInventoryExtractor
            new ArchInventoryExtractor(), // -> PacmanBasedInventoryExtractor
            new FallbackInventoryExtractor()
    };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final File analysisDir = deriveAnalysisFolder(inputArchiveFile);
            getLog().info("Found analysis directory: " + analysisDir.getAbsolutePath());

            // derive exclude configs and set if activateFileLevelProcessing is true, otherwise set to null
            final ExcludePatternsConfig excludePatternsConfig = activateFileLevelProcessing ? InventoryExtractorUtil.loadExcludeConfigFromYamlFile(excludePatternsFile) : null;
            final List<String> fileExcludes = excludePatternsConfig != null ? new ArrayList<>(excludePatternsConfig.getExcludes()) : null;

            // fill content derived from preprocessed files
            final Inventory inventory = extractInventory(analysisDir, fileExcludes);

            filterInventory(inventory);

            // write inventory
            targetInventoryFile.getParentFile().mkdirs();

            // check whether file level processing is active and handle files to be added/excluded from inventory
            if (activateFileLevelProcessing && excludePatternsConfig != null) {
                processFiles(excludePatternsConfig, inventory);
            }

            // try saving the excel file; may be too big
            try {
                new InventoryWriter().writeInventory(inventory, targetInventoryFile);
            } catch (Exception e) {
                getLog().warn("Cannot save file inventory.", e);
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void processFiles(ExcludePatternsConfig excludePatternsConfig, Inventory inventory) throws IOException {
        final Set<String> unknownFilePatterns = excludePatternsConfig.getUnknownFilePatterns();
        final Map<String, String> idToVersionMap = excludePatternsConfig.getArtifactIdToVersionMap();

        final String filteredFilesData = writeNotCoveredFileList(inventory);

        // write list of filtered files to dedicated file
        final File filteredFile = new File(targetInventoryFile.getParentFile(), "filtered-files.txt");
        FileUtils.write(filteredFile, filteredFilesData, FileUtils.ENCODING_UTF_8);

        // convert files from filtered-files.txt to artifacts and add to inventory
        addFileArtifactsToInventory(inventory, filteredFile);

        // read files from filtered-files.txt and enrich artifacts
        applyFileComponentPatterns(inventory, idToVersionMap, unknownFilePatterns);
    }

    /**
     * Derives the specific analysis directory from the input directory which either can have an archive as a direct child or can can have the unpacked analysis directory as any of its children.
     *
     * @param archiveFile the input directory containing an archive as direct child or the analysis folder as any sub-child
     * @return the analysis directory to analyze
     * @throws IOException            if no analysis directory was found
     * @throws MojoExecutionException if an error occurs during the execution of the mojo
     */
    private File deriveAnalysisFolder(File archiveFile) throws IOException, MojoExecutionException {
        if (!archiveFile.isFile()) {
            throw new MojoExecutionException("Archive file is not a file: " + archiveFile + ".");
        }

        ArchiveUtils.unpackIfPossible(archiveFile, analysisDir, new ArrayList<>());

        // determine the analysis directory
        return findAnalysisDirectory(analysisDir);
    }

    /**
     * Finds the analysis directory recursively starting from a (extracted) directory .
     *
     * @param extractedDir the starting directory to search for the analysis directory
     *
     * @return the found analysis directory
     *
     * @throws IOException if no analysis directory was found
     */
    private File findAnalysisDirectory(File extractedDir) throws IOException {
        try (Stream<Path> paths = Files.walk(extractedDir.toPath())) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(this::isAnalysisDirectory)
                    .map(Path::toFile)
                    .findFirst()
                    .orElseThrow(() -> new IOException(String.format("No analysis files found in directory [%s]", extractedDir)));
        }
    }

    private boolean isAnalysisDirectory(Path dir) {
        return Files.exists(dir.resolve("issue.txt"))
                && Files.exists(dir.resolve("release.txt"));
    }

    private Inventory extractInventory(File analysisDir, List<String> fileExcludes) throws IOException {
        InventoryExtractor extractor = Arrays.stream(inventoryExtractors).filter(e -> e
                        .applies(analysisDir))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No applicable inventory extractor found."));

        getLog().info("Using extractor " + extractor.getClass() + ".");

        // before extracting the content is validated using the extractor
        extractor.validate(analysisDir);

        // finally we run the extraction
        return extractor.extractInventory(analysisDir, artifactInventoryId, fileExcludes == null ? Collections.emptyList() : fileExcludes, activateFileLevelProcessing);
    }

    private void filterInventory(Inventory inventory) {
        final List<Artifact> toBeDeleted = new ArrayList<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            if (StringUtils.isEmpty(artifact.getVersion())) {
                if (filterArtifactsWithoutVersion) {
                    toBeDeleted.add(artifact);
                } else if (filterPackagesWithoutVersion &&
                        ARTIFACT_TYPE_PACKAGE.equalsIgnoreCase(artifact.get(KEY_TYPE))) {
                    toBeDeleted.add(artifact);
                }
            }
        }
        inventory.getArtifacts().removeAll(toBeDeleted);
    }

    private String writeNotCoveredFileList(Inventory inventory) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
            if (ARTIFACT_TYPE_FILE.equalsIgnoreCase(artifact.get(KEY_TYPE))) {
                final Set<String> rootPaths = artifact.getRootPaths();
                if (rootPaths != null) {
                    for (String project : rootPaths) {
                        // we always normalize to Linux paths
                        final String line = project.startsWith("/") ? FileUtils.normalizePathToLinux(project) : FileUtils.normalizePathToLinux(FileUtils.SEPARATOR_SLASH + project);
                        if (!sb.isEmpty()) {
                            sb.append(DELIMITER_NEWLINE);
                        }
                        sb.append(line);
                    }
                }

                // remove not covered file from inventory; inventory was just a vehicle
                inventory.getArtifacts().remove(artifact);
            }
        }
        return sb.toString();
    }

    private void addFileArtifactsToInventory(Inventory inventory, File filesFile) throws IOException {
        final List<String> filePaths = FileUtils.readLines(filesFile, FileUtils.ENCODING_UTF_8);
        for (final String filePath : filePaths) {
            final File file = new File(filePath);
            final Artifact fileArtifact = new Artifact();
            fileArtifact.setId(file.getName());
            fileArtifact.addRootPath(filePath);

            inventory.getArtifacts().add(fileArtifact);
        }
    }

    private void applyFileComponentPatterns(Inventory inventory, Map<String, String> idToVersionMap, Set<String> unknownFilePatterns) throws IOException {
        FileComponentPatternProcessor fileComponentPatternProcessor = new FileComponentPatternProcessor(idToVersionMap, unknownFilePatterns);
        fileComponentPatternProcessor.applyFileComponentPatterns(inventory);
    }
}
