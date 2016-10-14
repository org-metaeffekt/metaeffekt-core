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
package org.metaeffekt.core.maven.api.publisher;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import org.metaeffekt.core.common.kernel.annotation.PublicAnnotationAnalyser;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;

/**
 * Create and promote API extensions to the maven repository.
 * 
 * @goal publishapi
 * @phase package
 * @requiresDependencyResolution compile
 */
public class PublishApiMojo extends AbstractProjectAwareMojo {

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Used for attaching the artifact in the project.
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The list of fileSets to include in the API jar.
     * 
     * @parameter
     */
    private List<FileSet> filesets;

    /**
     * The root directory from which to start scanning for artefact to include in the API jar.
     * @parameter
     */
    private String scanRootDir;

    /**
     * The class name of the annotation used to mark classes and interfaces
     * as part of the public API.
     * @parameter
     * @required
     */
    private String annotationClass;

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
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if (!"java".equals(artifactHandler.getLanguage())) {
            this.getLog().debug("Not executing >>publishapi<< as the project is not a Java classpath-capable package");
            return;
        }

        getLog().debug("############################################################");
        getLog().debug("#         Using Publish API Plugin                         #");
        getLog().debug("############################################################");
        File apiJar = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-"
            + project.getVersion() + "-api.jar");

        try {
            extendFileset();

            if (!filesets.isEmpty()) {
                JarArchiver archiver = new JarArchiver();
                archiver.setDestFile(apiJar);
                archiver.setCompress(true);
                archiver.setIncludeEmptyDirs(true);
                // suppress logging of the archiver (logs massively on info)
                archiver.enableLogging(new ConsoleLogger(
                    org.codehaus.plexus.logging.Logger.LEVEL_WARN, "Warn Level Uppwards"));
                getLog().debug("Processing " + filesets.size() + " FileSets with configuration data");

                Iterator<FileSet> iterFiles = filesets.iterator();
                while (iterFiles.hasNext()) {
                    FileSet fs = iterFiles.next();
                    final String[] includes = convert(fs.getIncludes());
                    final String[] excludes = convert(fs.getExcludes());
                    archiver.addDirectory(new File(fs.getDirectory()), includes, excludes);
                }
                archiver.createArchive();
            }

            projectHelper.attachArtifact(project, "jar", "api", apiJar);
            getLog().info("API jar marked for export: " + apiJar.getAbsolutePath());

        } catch (ArchiverException e) {
            // in case the archiver cannot create an artifact we take care of. Even if the
            // resulting artifact is empty. This is required for symmetry reasons.
            
            getLog().info("Creating empty API artifact: " + apiJar.getAbsolutePath());

            Project antProject = new Project();
            antProject.setBaseDir(new File(project.getBuild().getDirectory()));

            Jar jar = new Jar();
            jar.setProject(antProject);
            jar.setBasedir(new File(project.getBuild().getDirectory()));
            jar.setExcludes("**/*");
            jar.setDestFile(apiJar);
            jar.execute();
            
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
