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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.inventory.extractor.ExcludePatternsConfig;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsInventoryExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mojo(name = "extract-windows-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class WindowsInventoryExtractionMojo extends AbstractApplianceInventoryExtractionMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final File derivedAnalysisDir = deriveAnalysisFolder(inputArchiveFile, analysisDir);
            getLog().info("Found analysis directory: " + derivedAnalysisDir.getAbsolutePath());

            // derive exclude configs and set if activateFileLevelProcessing is true, otherwise set to null
            final ExcludePatternsConfig excludePatternsConfig = activateFileLevelProcessing ? InventoryExtractorUtil.loadExcludeConfigFromYamlFile(excludePatternsFile) : null;
            final List<String> fileExcludes = excludePatternsConfig != null ? new ArrayList<>(excludePatternsConfig.getExcludes()) : null;

            // fill content derived from preprocessed files
            final Inventory inventory = extractInventory(derivedAnalysisDir, fileExcludes);

            targetInventoryFile.getParentFile().mkdirs();

            try {
                new InventoryWriter().writeInventory(inventory, targetInventoryFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write Windows inventory to file: " + targetInventoryFile.getAbsolutePath(), e);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected boolean isAnalysisDirectory(Path dir) {
        return Files.exists(dir.resolve("FileSystemDirsList.txt"));
    }

    private Inventory extractInventory(File analysisDir, List<String> fileExcludes) throws IOException, MojoExecutionException {
        final WindowsInventoryExtractor extractor = new WindowsInventoryExtractor();
        if (!extractor.applies(analysisDir)) {
            throw new MojoExecutionException("The specified analysis directory does not contain any extracted Windows files. Valid files are:\n" + Arrays.stream(WindowsExtractorAnalysisFile.values())
                    .map(scanFile -> scanFile.getTypeName() + "." + scanFile.getFileType())
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .orElse(""));
        }

        // before extracting the content is validated using the extractor
        extractor.validate(analysisDir);

        // finally we run the extraction
        return extractor.extractInventory(analysisDir, artifactInventoryId, fileExcludes == null ? Collections.emptyList() : fileExcludes, activateFileLevelProcessing);
    }
}
