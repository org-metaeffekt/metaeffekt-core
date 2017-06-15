/**
 * Copyright 2009-2017 the original author or authors.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Create and promote API extensions to the maven repository.
 * 
 * @goal publish-artifact
 * @phase package
 */
public class PublishArtifactMojo extends PrepareArtifactMojo {

    /**
     * The groupId to be used for the created artifact.
     * @parameter
     */
    private String alternateGroupId = null;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        // perform the preparation step
        super.execute();

        File artifactFile = getArtifactFile(getClassifier(), getQualifier());
        File tempDir = getTempDir(artifactFile);

        if (tempDir.exists()) {
            Project project = new Project();
            project.setBaseDir(new File(getProject().getBuild().getDirectory()));

            try {
                JarArchiver archiver = new JarArchiver();
                archiver.setDestFile(artifactFile);
                archiver.setIncludeEmptyDirs(true);
                archiver.setCompress(true);
                archiver.addDirectory(tempDir);
                
                // check whether the aggregated artifact includes any file
                ResourceIterator resourceIterator = archiver.getResources();
                boolean hasFile = false;
                while (!hasFile && resourceIterator.hasNext()) {
                    ArchiveEntry entry = resourceIterator.next();
                    if (ArchiveEntry.FILE == entry.getType()) {
                        hasFile = true;
                    }
                }
                
                if (hasFile) {
                    archiver.createArchive();
                    attachArtifact(artifactFile, getAlternateGroupId());
                    getLog().info("Artifact marked for export: " + artifactFile.getAbsolutePath());
                } else {
                    getLog().info("Skipping creation of empty archive.");
                }
            } catch (Exception e) {
                getLog().info("Couldn't create artifact.", e);
            }
        }
    }

    public String getAlternateGroupId() {
        return alternateGroupId;
    }

    public void setAlternateGroupId(String alternateGroupId) {
        this.alternateGroupId = alternateGroupId;
    }
    
}
