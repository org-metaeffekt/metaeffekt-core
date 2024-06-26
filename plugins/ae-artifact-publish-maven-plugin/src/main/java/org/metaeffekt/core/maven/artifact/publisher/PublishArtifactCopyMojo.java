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
package org.metaeffekt.core.maven.artifact.publisher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Prepares the artifact creation by copying selected resources to a dedicated
 * folder structure.
 */
@Mojo(name = "publish-artifact-copy", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class PublishArtifactCopyMojo extends AbstractArtifactMojo {

    /**
     * The classifier of the resource to be copied from.
     */
    @Parameter
    private String sourceClassifier = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        File srcArtifactFile = getArtifactFile(getSourceClassifier());

        if (srcArtifactFile.exists()) {
            attachArtifact(srcArtifactFile);
        }
    }

    public String getSourceClassifier() {
        return sourceClassifier;
    }

    public void setSourceClassifier(String sourceClassifier) {
        this.sourceClassifier = sourceClassifier;
    }

}
