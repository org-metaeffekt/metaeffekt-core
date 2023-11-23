/*
 * Copyright 2009-2022 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.inventory.extractor.*;
import org.metaeffekt.core.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Extracts a container inventory from pre-preprocessed container information.
 */
@Mojo(name = "extract-container-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ContainerInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    @Parameter(required = true, defaultValue = "${ae.extractor.analysis.dir}")
    protected File analysisDir;

    @Parameter(defaultValue = "false")
    protected boolean filterPackagesWithoutVersion = false;

    @Parameter
    protected String[] excludes;

    private InventoryExtractor[] inventoryExtractors = new InventoryExtractor[]{
            new DebianInventoryExtractor(),
            new CentOSInventoryExtractor(),
            new AlpineInventoryExtractor(),
            new ArchInventoryExtractor(),
            new FallbackInventoryExtractor()
    };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // fill content derived from preprocessed files
            final Inventory inventory = extractInventory(analysisDir);

            filterInventory(inventory);

            // write inventory
            targetInventoryFile.getParentFile().mkdirs();

            // write not covered file list
            final StringBuilder sb = new StringBuilder();
            for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
                if (ARTIFACT_TYPE_FILE.equalsIgnoreCase(artifact.get(KEY_TYPE))) {
                    Set<String> projects = artifact.getProjects();
                    if (projects != null) {
                        for (String project : projects) {
                            if (sb.length() > 0) {
                                sb.append(DELIMITER_NEWLINE);
                            }
                            if (!project.startsWith("/")) {
                                sb.append("/");
                            }
                            // we always normalize to linux paths
                            sb.append(FileUtils.normalizePathToLinux(project));
                        }
                    }

                    // remove not covered file from inventory; inventory was just a vehicle
                    inventory.getArtifacts().remove(artifact);
                }
            }

            // write list of filtered files
            FileUtils.write(new File(analysisDir, "filtered-files.txt"), sb.toString(), FileUtils.ENCODING_UTF_8);

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

    private Inventory extractInventory(File analysisDir) throws IOException {
        InventoryExtractor extractor = Arrays.stream(inventoryExtractors).filter(e -> e
                .applies(analysisDir))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No applicable inventory extractor found."));

        getLog().info("Using extractor " + extractor.getClass() + ".");

        // before extracting the content is validated using the extractor
        extractor.validate(analysisDir);

        // finally we run the extraction
        return extractor.extractInventory(analysisDir,
                artifactInventoryId, excludes == null ? Collections.emptyList() : Arrays.asList(excludes));
    }

    private void filterInventory(Inventory inventory) {
        List<Artifact> toBeDeleted = new ArrayList<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            if (filterPackagesWithoutVersion &&
                    Constants.ARTIFACT_TYPE_PACKAGE.equalsIgnoreCase(artifact.get(Constants.KEY_TYPE)) &&
                    StringUtils.isEmpty(artifact.getVersion())) {
                toBeDeleted.add(artifact);
            }
        }
        inventory.getArtifacts().removeAll(toBeDeleted);
    }

}
