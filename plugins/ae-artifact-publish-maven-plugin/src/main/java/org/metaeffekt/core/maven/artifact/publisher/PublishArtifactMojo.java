/**
 * Copyright 2009-2018 the original author or authors.
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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

/**
 * Create and promote API extensions to the maven repository.
 */
@Mojo(name="publish-artifact", defaultPhase=LifecyclePhase.PACKAGE, requiresProject=true, threadSafe=true)
public class PublishArtifactMojo extends PrepareArtifactMojo {

    /**
     * Directory containing the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;

    /**
     * Directory containing the classes and resource files that should be packaged into the JAR.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File classesDirectory;

    /**
     * The {@link MavenSession}.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Used for attaching the artifact in the project.
     */
    @Component
    private MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        // perform the preparation step
        super.execute();

        final File artifactFile = getArtifactFile(getClassifier());
        final File tempDir = getTempDir(artifactFile);

        try {
            archive.setForced(true);
            final MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);
            archiver.setOutputFile(artifactFile);
            archiver.getArchiver().setIncludeEmptyDirs(true);
            archiver.getArchiver().setCompress(true);
            archiver.getArchiver().addDirectory(tempDir);
            archiver.createArchive(session, getProject(), archive);

            // attach the artifact
            projectHelper.attachArtifact(getProject(), getType(), getClassifier(), artifactFile);
        } catch (Exception e) {
            getLog().info("Couldn't create artifact.", e);
        }
    }

}
