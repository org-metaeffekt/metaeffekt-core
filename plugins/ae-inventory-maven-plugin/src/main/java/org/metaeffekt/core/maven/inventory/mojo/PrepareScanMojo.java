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
import org.metaeffekt.core.inventory.processor.command.PrepareScanDirectoryCommand;

import java.io.File;

/**
 * Mojo to prepare a folder for scanning. Procedure has been extracted from {@link DirectoryScanReportCreationMojo} to
 * isolate expensive file-system-level operations (delete current, copy).
 */
@Mojo(name = "prepare-scan-directory", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PrepareScanMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(required = true)
    private File inputDirectory;

    @Parameter(required = true, defaultValue = "${project.build.directory}/scan")
    private File scanDirectory;

    @Parameter(required = true)
    private String[] scanIncludes = new String[] {"**/*"};

    @Parameter
    private String[] scanExcludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isPomPackagingProject()) {
            try {
                final PrepareScanDirectoryCommand prepareScanDirectoryCommand = new PrepareScanDirectoryCommand();
                prepareScanDirectoryCommand.prepareScanDirectory(inputDirectory, scanDirectory, scanIncludes, scanExcludes);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

}
