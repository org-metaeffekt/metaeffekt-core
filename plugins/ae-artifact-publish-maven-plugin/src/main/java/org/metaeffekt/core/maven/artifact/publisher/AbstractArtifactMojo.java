/**
 * Copyright 2009-2020 the original author or authors.
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

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;
import org.metaeffekt.core.maven.kernel.MavenConstants;

import java.io.File;

/**
 * Abstract mojo common to all artifact mojos.
 */
public abstract class AbstractArtifactMojo extends AbstractProjectAwareMojo {

    /**
     * Used for attaching the artifact in the project.
     */
    @Component
    private MavenProjectHelper projectHelper;
    
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The artifact type.
     */
    @Parameter(defaultValue = "jar")
    private String type = MavenConstants.MAVEN_PACKAGING_JAR;

    /**
     * The classifier to use.
     */
    @Parameter
    private String classifier = null;

    /**
     * Manages the created artifacts.
     */
    @Component
    private ArtifactHandlerManager artifactHandlerManager;
    
    private String computeArtifactName(String classifier) {
        final StringBuffer artifactName = new StringBuffer();
        artifactName.append(project.getArtifactId());
        artifactName.append('-');
        artifactName.append(project.getVersion());
        if (classifier != null) {
            artifactName.append('-');
            artifactName.append(classifier);
        }
        artifactName.append('.');
        artifactName.append(getType());
        return artifactName.toString();
    }

    protected File getArtifactFile(String classifier) {
        final String artifactName = computeArtifactName(classifier);
        return new File(getProject().getBuild().getDirectory(), artifactName.toString());
    }

    protected File getTempDir(File artifactFile) {
        File tempDir = new File(getProject().getBuild().getDirectory(), "publish-artifact-"
                + artifactFile.getName());
        return tempDir;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public MavenProject getProject() {
        return project;
    }

    protected void attachArtifact(File artifactFile) {
        if (getClassifier() != null) {
            projectHelper.attachArtifact(getProject(), getType(), getClassifier(), artifactFile);
        }
    }

}
