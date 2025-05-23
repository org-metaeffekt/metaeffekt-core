/*
 * Copyright 2009-2024 the original author or authors.
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
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;

import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_SOURCE_PROJECT;

/**
 * Extracts a container inventory from pre-processed container information.
 */
@Mojo(name = "extract-scan-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class DirectoryScanInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    /**
     * The source inventory dir.
     */
    @Parameter(required = true)
    private File sourceInventoryDir;

    /**
     * Includes of the source inventory; relative to the sourceInventoryDir.
     */
    @Parameter(defaultValue = "*.xls*")
    private String sourceInventoryIncludes;

    @Parameter(required = true)
    private File inputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/scan")
    private File scanDirectory;

    @Parameter(defaultValue = "\"**/*\"")
    private String[] scanIncludes = new String[]{"**/*"};

    @Parameter
    private String[] scanExcludes;

    @Parameter(defaultValue = "\"**/*\"")
    private String[] unwrapIncludes = new String[]{"**/*"};

    @Parameter
    private String[] unwrapExcludes;

    /**
     * Whether certain artifacts should be inspected for nested information.
     * For backward compatibility reasons the default value is false. The feature requires explicit activation.
     */
    @Parameter(defaultValue = "false")
    private boolean includeEmbedded = false;

    /**
     * When true, enables that implicitly general archive types are unpacked.
     */
    @Parameter(defaultValue = "true")
    private boolean enableImplicitUnpack = true;

    /**
     * When true, enables that implicitly component patterns are detected and applied.
     * Defaults to false for compatibility reasons.
     */
    @Parameter(defaultValue = "false")
    private boolean enableDetectComponentPatterns = false;

    @Parameter
    private String[] postScanExcludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // read reference inventory
            final Inventory sourceInventory = InventoryUtils.readInventory(sourceInventoryDir, sourceInventoryIncludes);

            // initialize DirectoryInventoryScan instance to perfom scan
            final DirectoryInventoryScan scan = new DirectoryInventoryScan(
                    inputDirectory, scanDirectory,
                    scanIncludes, scanExcludes,
                    unwrapIncludes, unwrapExcludes,
                    postScanExcludes, sourceInventory);

            // transfer configuration
            scan.setIncludeEmbedded(includeEmbedded);
            scan.setEnableImplicitUnpack(enableImplicitUnpack);
            scan.setEnableDetectComponentPatterns(enableDetectComponentPatterns);

            // run scan
            final Inventory inventory = scan.createScanInventory();

            // FIXME: this may be legacy, when asset information is provided.
            // mark artifacts with inventory id
            if (StringUtils.isNotBlank(artifactInventoryId)) {
                for (final Artifact artifact : inventory.getArtifacts()) {
                    artifact.set(KEY_SOURCE_PROJECT, artifactInventoryId);
                }
            }

            // ensure file to write inventory exists
            targetInventoryFile.getParentFile().mkdirs();

            // write inventory
            new InventoryWriter().writeInventory(inventory, targetInventoryFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
