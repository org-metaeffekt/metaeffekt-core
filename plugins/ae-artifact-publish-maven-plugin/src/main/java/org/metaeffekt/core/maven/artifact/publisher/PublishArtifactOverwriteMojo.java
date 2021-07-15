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
package org.metaeffekt.core.maven.artifact.publisher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;

import java.io.File;

/**
 * Prepares the artifact creation by copying selected resources to a dedicated
 * folder structure.
 */
@Mojo(name = "publish-artifact-overwrite", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class PublishArtifactOverwriteMojo extends AbstractArtifactMojo {

    /**
     * The qualifier of the resource to be copied from.
     */
    @Parameter
    private String sourceQualifier = null;

    /**
     * The classifier of the resource to be copied from.
     */
    @Parameter
    private String sourceClassifier = null;

    /**
     * The classifier of the target resource.
     */
    @Parameter
    private String targetClassifier = null;

    /**
     * Determines if the created artifact should be uploaded into the maven repository
     */
    @Parameter
    private boolean attachArtifact = false;

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        File srcArtifactFile = getArtifactFile(getSourceClassifier());

        // verify that the artifact exists
        if (!srcArtifactFile.exists()) {
            getLog().info("No source artifact found for file: " + srcArtifactFile.getName());
            // do nothing, just skip it here.
            return;
        }
        File targetArtifactFile = getArtifactFile(getClassifier());

        Project project = new Project();
        project.setBaseDir(new File(getProject().getBuild().getDirectory()));

        if (srcArtifactFile.exists()) {
            Copy copy = new Copy();
            copy.setProject(project);
            copy.setOverwrite(true);
            copy.setFile(srcArtifactFile);
            copy.setTofile(targetArtifactFile);
            copy.execute();

            // we assume the orginal artifact is already attached
        }

        // should it be uploaded into the maven repository?
        if (attachArtifact) {
            attachArtifact(targetArtifactFile);
        }

    }

    @Override
    public String getClassifier() {
        if (getTargetClassifier() == null) {
            return super.getClassifier();
        } else {
            return getTargetClassifier();
        }
    }

    public String getSourceClassifier() {
        return sourceClassifier;
    }

    public void setSourceClassifier(String sourceClassifier) {
        this.sourceClassifier = sourceClassifier;
    }

    public String getTargetClassifier() {
        return targetClassifier;
    }

    public void setTargetClassifier(String targetClassifier) {
        this.targetClassifier = targetClassifier;
    }

}
