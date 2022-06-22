/*
 * Copyright 2009-2021 the original author or authors.
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
import org.metaeffekt.core.maven.inventory.extractor.ContainerAssetInventoryProcessor;

import java.io.File;
import java.io.IOException;

/**
 * Extracts a container inventory from pre-preprocessed container information.
 */
@Mojo(name = "extract-container-asset", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ContainerAssetExtractionMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(required = true, defaultValue = "${ae.extractor.analysis.dir}")
    protected File analysisDir;

    @Parameter(required = true, defaultValue = "${ae.extractor.analysis.dir}/image-inspect.yaml")
    private File containerInspectionFile;

    @Parameter(required = true)
    private File inventoryFile;

    @Parameter(required = true)
    private String repo;

    @Parameter(required = true)
    private String tag;

    @Parameter(required = true)
    private String assetQualifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ContainerAssetInventoryProcessor processor = new ContainerAssetInventoryProcessor()
                .basedOn(containerInspectionFile)
                .fromRepo(repo).withTag(tag).qualifiedBy(assetQualifier)
                .fromAnalysisDir(analysisDir)
                .augmenting(inventoryFile);
            processor.process();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
