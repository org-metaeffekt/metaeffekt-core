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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.inventory.extractor.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts a container inventory from pre-porcessed container information.
 */
@Mojo( name = "extract-container-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class ContainerInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    @Parameter(defaultValue="${project.build.directory}/analysis")
    protected File analysisDir;

    @Parameter(defaultValue = "false")
    protected boolean filterPackagesWithoutVersion = false;

    private InventoryExtractor[] inventoryExtractors = new InventoryExtractor[] {
        new DebianInventoryExtractor(),
        new CentOSInventoryExtractor(),
        new AlpineInventoryExtractor(),
        new ArchInventoryExtractor()
    };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // fill content derived from preprocessed files
            Inventory inventory = extractInventory(analysisDir);

            filterInventory(inventory);

            // write inventory
            targetInventoryFile.getParentFile().mkdirs();
            new InventoryWriter().writeInventory(inventory, targetInventoryFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Inventory extractInventory(File analysisDir) throws IOException {
        InventoryExtractor extractor = Arrays.stream(inventoryExtractors).filter(e -> e
            .applies(analysisDir))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No applicable inventory extractor found."));

        // before extracting the content is validated using the extractor
        extractor.validate(analysisDir);

        // finally we run the extraction
        return extractor.extractInventory(analysisDir, artifactInventoryId);
    }

    private void filterInventory(Inventory inventory) {
        List<Artifact> toBeDeleted = new ArrayList<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            if (filterPackagesWithoutVersion && StringUtils.isEmpty(artifact.getVersion())) {
                toBeDeleted.add(artifact);
            }
        }
        inventory.getArtifacts().removeAll(toBeDeleted);
    }

}
