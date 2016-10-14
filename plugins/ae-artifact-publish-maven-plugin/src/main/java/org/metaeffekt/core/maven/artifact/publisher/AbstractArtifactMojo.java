/**
 * Copyright 2009-2016 the original author or authors.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;

import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;
import org.metaeffekt.core.maven.kernel.MavenConstants;

/**
 * Abstract mojo common to all artifact mojos.
 */
public abstract class AbstractArtifactMojo extends AbstractProjectAwareMojo {

    /**
     * Used for attaching the artifact in the project.
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;
    
    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The qualifier to use.
     * @parameter
     */
    private String qualifier = null;

    /**
     * The artifact type.
     * @parameter
     * @default jar
     */
    private String type = MavenConstants.MAVEN_PACKAGING_JAR;

    /**
     * The classifier to use.
     * @parameter
     */
    private String classifier = null;

    /**
     * Manages the created artifacts.
     * 
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;
    
    private String computeArtifactName(String classifier, String qualifier) {
        StringBuffer artifactName = new StringBuffer();
        artifactName.append(project.getArtifactId());
        artifactName.append('-');
        if (classifier != null) {
            artifactName.append(classifier);
            artifactName.append('-');
        }
        artifactName.append(project.getVersion());
        if (qualifier != null) {
            artifactName.append('-');
            artifactName.append(qualifier);
        }

        artifactName.append('.');
        artifactName.append(getType());

        return artifactName.toString();
    }

    protected File getArtifactFile(String classifier, String qualifier) {
        String artifactName = computeArtifactName(classifier, qualifier);

        File artifactFile = new File(getProject().getBuild().getDirectory(), artifactName.toString());
        return artifactFile;
    }

    protected File getTempDir(File artifactFile) {
        File tempDir = new File(getProject().getBuild().getDirectory(), "publish-artifact-"
                + artifactFile.getName());
        return tempDir;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
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

    protected Artifact attachArtifact(MavenProject project, String artifactType, final String artifactClassifier,
            final String artifactQualifier, final String artifactGroupId, File artifactFile) {
        String type = artifactType;
        ArtifactHandler handler = null;
        if (type != null) {
            handler = artifactHandlerManager.getArtifactHandler(artifactType);
        }
        if (handler == null) {
            handler = artifactHandlerManager.getArtifactHandler("jar");
        }

        Artifact projectArtifact = project.getArtifact();
        
        String derivedGroupId = artifactGroupId;
        if (derivedGroupId == null) {
            derivedGroupId = projectArtifact.getGroupId();
        }
        
        StringBuffer derivedArtifactId = new StringBuffer();
        derivedArtifactId.append(projectArtifact.getArtifactId());

        if (artifactClassifier != null) {
            derivedArtifactId.append("-");
            derivedArtifactId.append(artifactClassifier);
        }
        
        Artifact artifact = new DefaultArtifact(
              derivedGroupId,
              derivedArtifactId.toString(),
              projectArtifact.getVersionRange(), 
              projectArtifact.getScope(), 
              projectArtifact.getType(), 
              getQualifier(),
              handler);

            artifact.setFile(artifactFile);
            artifact.setResolved(true);
            project.addAttachedArtifact(artifact);
            
        return artifact;
    }

    protected void attachArtifact(File artifactFile, String alternateGroupId) {
        if (getClassifier() != null || alternateGroupId != null) {
            Artifact artifact = attachArtifact(getProject(), getType(), getClassifier(), getQualifier(), alternateGroupId, artifactFile);
            
            if (alternateGroupId != null) {
                // here is the possibility to upload artifacts with an alternative group id to a local legacy repository
                
                getLog().info("Installing artifact to local maven one repository...");
                
                String userHome = System.getProperty("user.home");
                File file = new File(userHome, "build.properties");
                
                try {
                    if (file.exists()) {
                        getLog().info("  Loading properties file: " + file);
                        
                        // load the build.properties file
                        Properties properties = new Properties();
                        FileInputStream fileInputStream = new FileInputStream(file);
                        try {
                            properties.load(fileInputStream);
                        } finally {
                            fileInputStream.close();
                        }

                        // derive maven.repo.local from properties
                        String mavenRepoLocal = properties.getProperty("maven.repo.local");
                        if (mavenRepoLocal == null) {
                            String mavenHomeLocal = properties.getProperty("maven.home.local");
                            if (mavenHomeLocal != null) {
                                mavenRepoLocal = mavenHomeLocal + File.separatorChar + "repository";
                            }
                        }
                        
                        // copy artifacts to appropriate location (m1)
                        if (mavenRepoLocal != null) {
                            getLog().info("  Detected maven.repo.local: " + mavenRepoLocal);
                            
                            File targetLocation = new File(mavenRepoLocal, alternateGroupId + File.separatorChar + getType() + "s");
                            targetLocation.mkdirs();
                            
                            String artifactFilename = computeArtifactName(artifact);
                            
                            File targetFile = new File(targetLocation, artifactFilename);
                            getLog().info("  Installing " + artifactFile + " to " + targetFile);
                            
                            Project project = new Project();
                            project.setBaseDir(new File(userHome));
                            Copy copy = new Copy();
                            copy.setProject(project);
                            copy.setFile(artifactFile);
                            copy.setTofile(targetFile);
                            copy.setOverwrite(true);
                            copy.execute();
                        } else {
                            getLog().info("  Maven repo location not available. Skipping upload.");
                        }
                    } else {
                        getLog().info("  Properties file " + file + " does not exist. Skipping upload.");
                    }
                } catch (IOException e) {
                    getLog().info("Install to local m1 repository failed.", e);
                }
            }
        } else {
            projectHelper.attachArtifact(getProject(), getType(), getQualifier(), artifactFile);
        }
    }

    private String computeArtifactName(Artifact artifact) {
        StringBuffer name = new StringBuffer(artifact.getArtifactId());
        if (artifact.getClassifier() != null) {
            name.append('-');
            name.append(artifact.getClassifier());
        }
        name.append('-');
        name.append(artifact.getVersion());
        if (getQualifier() != null) {
            name.append('-');
            name.append(getClassifier());
        }
        name.append(".");
        name.append(artifact.getType());
        return name.toString();
    }
    
}
