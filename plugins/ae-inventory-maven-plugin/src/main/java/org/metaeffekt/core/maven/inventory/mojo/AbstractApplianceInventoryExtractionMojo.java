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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.util.ArchiveUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class AbstractApplianceInventoryExtractionMojo extends AbstractInventoryExtractionMojo {
    // the mojo ingest an archive as primary input
    @Parameter
    protected File inputArchiveFile;

    @Parameter(required = true)
    protected File analysisDir;

    @Parameter(required = true)
    protected File excludePatternsFile;

    @Parameter(defaultValue = "true")
    protected boolean activateFileLevelProcessing = true;

    /**
     * Derives the specific analysis directory from unpacking the input archive file.
     *
     * @param archiveFile the input archive file.
     * @return the analysis directory with the unpacked folders to analyze
     * @throws IOException            if no analysis directory was found
     * @throws MojoExecutionException if an error occurs during the execution of the mojo
     */
    protected File deriveAnalysisFolder(File archiveFile, File analysisDir) throws IOException, MojoExecutionException {
        if (!archiveFile.isFile()) {
            throw new MojoExecutionException("Archive file is not a file: " + archiveFile + ".");
        }

        // unpacking data into specified analysis output directory
        ArchiveUtils.recursiveUnpack(archiveFile.getParentFile(), analysisDir, new ArrayList<>());

        // determine the analysis directory
        return findAnalysisDirectory(analysisDir);
    }

    /**
     * Finds the analysis directory recursively starting from a (extracted) directory .
     *
     * @param extractedDir the starting directory to search for the analysis directory
     * @return the found analysis directory
     * @throws IOException if no analysis directory was found
     */
    protected File findAnalysisDirectory(File extractedDir) throws IOException {
        try (Stream<Path> paths = Files.walk(extractedDir.toPath())) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(this::isAnalysisDirectory)
                    .map(Path::toFile)
                    .findFirst()
                    .orElseThrow(() -> new IOException(String.format("No analysis files found in directory [%s]", extractedDir)));
        }
    }

    protected abstract boolean isAnalysisDirectory(Path path);
}
