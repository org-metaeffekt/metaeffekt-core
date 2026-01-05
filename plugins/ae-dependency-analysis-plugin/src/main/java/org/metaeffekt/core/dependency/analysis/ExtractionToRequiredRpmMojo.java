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
package org.metaeffekt.core.dependency.analysis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.dependency.analysis.depres.RequirementsResult;
import org.metaeffekt.core.dependency.analysis.depres.ResolutionRun;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Mojo(name = "extract-required-packages-rpm", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ExtractionToRequiredRpmMojo extends AbstractMojo {
    /**
     * The "analysis" directory, as output by our extraction script.
     */
    @Parameter(required = true)
    public File extractionDir;

    /**
     * A list of packages that must be kept.<br>
     * Note that these must be package names as general requirements are not yet supported for this field.
     */
    @Parameter(required = true)
    public List<String> mustHaves;

    /**
     * An output directory where output (currently an xlsx file) will be stored.
     */
    @Parameter(required = true, defaultValue = "target")
    public File outputDirectory;

    // TODO: output data to a directory in some sort of format
    @Override
    public void execute() throws MojoExecutionException {
        ResolutionRun depRes = new ResolutionRun(extractionDir, mustHaves);
        RequirementsResult result = depRes.runResolution();

        try {
            result.exportToInventory(outputDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing output inventory:", e);
        }
    }
}
