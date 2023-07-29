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
import org.metaeffekt.core.maven.inventory.extractor.GenericAssetInventoryProcessor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Attaches generic asset metadata to an existing inventory.
 */
@Mojo(name = "attach-asset-metadata", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenericAssetExtractionMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(required = true)
    private File inventoryFile;

    @Parameter(required = false)
    private File targetInventoryFile;

    @Parameter(required = true)
    private Map<String, String> attributes;

    @Parameter(required = true)
    private String assetId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            GenericAssetInventoryProcessor processor = new GenericAssetInventoryProcessor()
                .supply(assetId)
                .withAttributes(attributes)
                .augmenting(inventoryFile)
                .writing(targetInventoryFile);
            processor.process();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
