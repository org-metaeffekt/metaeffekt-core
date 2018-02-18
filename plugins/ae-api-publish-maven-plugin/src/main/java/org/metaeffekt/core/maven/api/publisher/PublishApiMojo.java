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
package org.metaeffekt.core.maven.api.publisher;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.apache.maven.archiver.MavenArchiveConfiguration;

import org.metaeffekt.core.common.kernel.annotation.PublicAnnotationAnalyser;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;

/**
 * Create and promote API extensions to the maven repository.
 */
@Mojo( name = "publishapi", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class PublishApiMojo extends AbstractProjectAwareMojo {

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

    /**
     * The Maven project.
     */
    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    /**
     * Used for attaching the artifact in the project.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The list of fileSets to include in the API jar.
     */
    @Parameter
    private List<FileSet> filesets;

    /**
     * The root directory from which to start scanning for artefact to include in the API jar.
     */
    @Parameter
    private String scanRootDir;

    /**
     * The class name of the annotation used to mark classes and interfaces
     * as part of the public API.
     */
    @Parameter(required = true)
    private String annotationClass;

    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    /**
     * Executes this Mojo.
     * 
     * @throws MojoExecutionException
     *             if an unexpected problem occurs.
     * @throws MojoFailureException
     *             if an expected problem (such as a compilation failure)
     *             occurs.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArtifactHandler artifactHandler = getProject().getArtifact().getArtifactHandler();
        if (!"java".equals(artifactHandler.getLanguage())) {
            this.getLog().debug("Not executing >>publishapi<< as the project is not a Java classpath-capable package");
            return;
        }

        getLog().debug("############################################################");
        getLog().debug("#         Using Publish API Plugin                         #");
        getLog().debug("############################################################");
        File apiJar = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-"
            + project.getVersion() + "-api.jar");

        extendFileset();

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(apiJar);
        archive.setForced(true);
        try {
            Iterator<FileSet> iterFiles = filesets.iterator();
            while (iterFiles.hasNext()) {
                FileSet fs = iterFiles.next();
                final String[] includes = convert(fs.getIncludes());
                final String[] excludes = convert(fs.getExcludes());
                archiver.getArchiver().addDirectory(new File(fs.getDirectory()), includes, excludes);
            }
            archiver.createArchive(session, project, archive);
            projectHelper.attachArtifact(project, "jar", "api", apiJar);
            getLog().info("API jar marked for export: " + apiJar.getAbsolutePath());
        } catch (Exception e) {
            getLog().warn("Couldn't create API jar.", e);
        }
    }

    private String[] convert(List<String> strings) {
        if (strings == null) {
            return null;
        }
        return strings.toArray(new String[strings.size()]);
    }
    
    private void extendFileset() throws MojoExecutionException {
        if (filesets == null) {
            filesets = new ArrayList<FileSet>();
        }

        if (scanRootDir != null) {

            try {
                // the runtime class path doesn't contain "provided" scoped
                // dependencies so, use the compile dependencies scope instead.
                List<?> cpel = project.getCompileClasspathElements();

                URL[] urls = new URL[cpel.size()];
                for (int i = 0; i < urls.length; i++) {
                    urls[i] = new File(String.valueOf(cpel.get(i))).toURI().toURL();
                }
                URLClassLoader runtimeClassLoader = new URLClassLoader(urls, Thread.currentThread()
                        .getContextClassLoader());
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> annotationClazz = 
                    (Class<? extends Annotation>) runtimeClassLoader
                        .loadClass(annotationClass);

                this.getLog().debug("Scanning: " + scanRootDir);
                List<String> includeFiles = new ArrayList<String>();
                PublicAnnotationAnalyser analyzer = 
                    new PublicAnnotationAnalyser(runtimeClassLoader, annotationClazz);
                analyzer.collectPublicTypes(new File(scanRootDir), includeFiles);

                this.getLog().debug("Found public java types: " + includeFiles);

                if (includeFiles.size() > 0) {
                    FileSet fileset = new FileSet();
                    fileset.setDirectory(scanRootDir);
                    fileset.setIncludes(includeFiles);
                    filesets.add(fileset);
                }

            } catch (Exception e) {
                getLog().warn("Cannot complete public class scan.", e);
            }

        }
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
