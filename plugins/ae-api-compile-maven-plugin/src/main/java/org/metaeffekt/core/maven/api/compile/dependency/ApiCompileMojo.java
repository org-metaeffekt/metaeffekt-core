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
package org.metaeffekt.core.maven.api.compile.dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.compiler.CompilerMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.maven.kernel.MavenProjectUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This goal extends the maven compile goal and ensures that source code compilation is performed
 * against API artifacts rather than runtime artifacts.
 */
@SuppressWarnings("rawtypes")
@Mojo( name = "compile", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true,
requiresDependencyResolution = ResolutionScope.COMPILE )
public class ApiCompileMojo extends CompilerMojo {

    /**
     * The ArtifactResolver to be used.
     */
    @Component
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * The local Maven repository where artifacts are cached during the build process.
     */
    @Parameter(defaultValue="${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * A list of remote Maven repositories to be used for the compile run.
     */
    @Parameter(defaultValue="${project.remoteArtifactRepositories}")
    private java.util.List remoteRepositories;

    /**
     * The directory for compiled classes.
     */
    @Parameter(defaultValue="${project.build.outputDirectory}", required=true)
    private File alternateOutputDirectory;
    
    /**
     * The Maven mavenProject this goal is called for.
     */
    @Parameter(defaultValue="${project}", required=true, readonly=true)
    private MavenProject mavenProject;
    
    /**
     * The defined API Violations.
     */
    @Parameter
    private APIViolation[] apiViolations;

    public void execute() throws MojoExecutionException, org.apache.maven.plugin.compiler.CompilationFailureException {
        // exit if the mavenProject is pom only
        if (MavenProjectUtil.isPomPackagingProject(mavenProject)) {
            return;
        }
        getLog().debug("############################################################");
        getLog().debug("#         Using Modified API compile Plugin                #");
        getLog().debug("############################################################");
        
        super.execute();
    }

    /**
     * Obtains the Classpath Elements for compilation.
     *
     * This methode is called by the super class and its content is overriden in
     * order to provide a check for API classified artifacts.
     * @return List with artifacts required for compilation
     */
    @SuppressWarnings("unchecked")
    protected List getClasspathElements() {
        getLog().info("Using modified getClasspathElements() method to replace classified artifacts");
        // First build a mapping between original artifact file paths and their alternative classified API artifact, when it exists
        List compileArtifacts = mavenProject.getCompileArtifacts();
        HashMap apiArtifacts = new HashMap();
        for (int i = 0; i < compileArtifacts.size(); i++) {
            Artifact a = (Artifact) compileArtifacts.get(i);
            // only check on non api artifacts for api existence
            if (!isConfiguredViolation(a)){
                if ("runtime".equalsIgnoreCase(a.getClassifier())) {
                    Artifact apiArtifact = getClassifiedArtifact(a, "api");
                    if (apiArtifact != null) {
                        apiArtifacts.put(a.getFile().getPath(), apiArtifact);
                    }
                }
            }
        }
        ArrayList processedClasspath = new ArrayList(super.getClasspathElements().size());
        for (int i = 0; i < super.getClasspathElements().size(); i++) {
            String path = super.getClasspathElements().get(i);
            if (apiArtifacts.containsKey(path)) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Classpath substitution from '" + path + "' to '" + ((Artifact) apiArtifacts.get(path)).getFile().getPath());
                }
                processedClasspath.add(((Artifact) apiArtifacts.get(path)).getFile().getPath());
            } else {
                processedClasspath.add(path);
            }
        }
        return processedClasspath;
    }

    /**
     * @return The classified artifact version corresponding to the provided classifier, null when it does not exist.
     */
    private Artifact getClassifiedArtifact(Artifact original, String classifier) {
        VersionRange versionRange = null;
        try {
            versionRange = VersionRange.createFromVersionSpec(original.getVersion());
        } catch (InvalidVersionSpecificationException ivse) {
            // invalid version? skip this artifact
            getLog().error("Artifact '" + original.getArtifactId() + "-"
                    + original.getVersion() + "' has a non-resolvable version: " + ivse.getMessage());
            return null;
        }
        ArtifactHandler handler = new DefaultArtifactHandler(original.getType());
        Artifact classifiedArtifact = new DefaultArtifact(
                original.getGroupId(),
                original.getArtifactId(),
                versionRange,
                original.getScope(),
                original.getType(),
                classifier,
                handler);
        try {
            resolver.resolve(classifiedArtifact, remoteRepositories, localRepository);
            getLog().debug("FOUND an API classified artifact for: "
                    + original.getArtifactId() + "-" + original.getVersion());
            return classifiedArtifact;
        } catch (ArtifactResolutionException are) {
            getLog().debug("Can not RESOLVE an API classified artifact for: "
                    + original.getArtifactId() + "-" + original.getVersion());
        } catch (ArtifactNotFoundException anfe) {
            getLog().debug("Can not FIND a API classified artifact for: "
                    + original.getArtifactId() + "-" + original.getVersion());
        }
        return null;
    }
    
    private boolean isConfiguredViolation(Artifact artifact) {
        if (artifact == null) {
            return false;
        }
        if (apiViolations == null) {
            return false;
        }
        
        for (APIViolation apiViolation : apiViolations) {
            if (apiViolation != null && apiViolation.matches(artifact)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the alternateOutputDirecotry property as {@link java.io.File} representing the output directory
     * configured for the compile run.
     * @return a {@link java.io.File} representing the output directory
     * configured for the compile run.
     */
    protected File getOutputDirectory() {
        return alternateOutputDirectory;
    }
}
