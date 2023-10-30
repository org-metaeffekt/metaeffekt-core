package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsInventoryExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mojo(name = "extract-windows-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class WindowsInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    @Parameter
    protected File analysisDir;

    @Parameter
    protected List<String> excludePatterns;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final WindowsInventoryExtractor extractor = new WindowsInventoryExtractor();

        if (!extractor.applies(this.analysisDir)) {
            throw new MojoExecutionException("The specified analysis directory does not contain any extracted Windows files. Valid files are:\n" + Arrays.stream(WindowsExtractorAnalysisFile.values())
                    .map(scanFile -> scanFile.getTypeName() + "." + scanFile.getFileType())
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .orElse(""));
        }

        final Inventory extractedInventory;
        try {
            extractedInventory = extractor.extractInventory(this.analysisDir, super.artifactInventoryId, this.excludePatterns == null ? Collections.emptyList() : this.excludePatterns);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to extract Windows inventory: " + e.getMessage(), e);
        }

        super.targetInventoryFile.getParentFile().mkdirs();

        try {
            new InventoryWriter().writeInventory(extractedInventory, super.targetInventoryFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write Windows inventory to file: " + super.targetInventoryFile.getAbsolutePath(), e);
        }
    }
}
