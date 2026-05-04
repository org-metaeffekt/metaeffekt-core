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
package org.metaeffekt.core.maven.api.compile.dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.compiler.CompilerMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.metaeffekt.core.maven.kernel.MavenProjectUtil;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This goal extends the maven compile goal and ensures that source code compilation is performed
 * against API artifacts rather than runtime artifacts.
 */
@SuppressWarnings("rawtypes")
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class ApiCompileMojo extends CompilerMojo {

    /**
     * The ArtifactResolver to be used.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Inject
    private RepositorySystem repositorySystem;

    /**
     * A list of remote Maven repositories to be used for the compile run.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private java.util.List remoteRepositories;

    /**
     * The directory for compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File alternateOutputDirectory;

    /**
     * The Maven mavenProject this goal is called for.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
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
     * <p>
     * This methode is called by the super class and its content is overriden in
     * order to provide a check for API classified artifacts.
     *
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
            if (!isConfiguredViolation(a)) {
                if ("runtime".equalsIgnoreCase(a.getClassifier())) {
                    org.eclipse.aether.artifact.Artifact apiArtifact = getClassifiedArtifact(a, "api");
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
    private org.eclipse.aether.artifact.Artifact getClassifiedArtifact(Artifact original, String classifier) {
        VersionRange versionRange = null;
        try {
            versionRange = VersionRange.createFromVersionSpec(original.getVersion());
        } catch (InvalidVersionSpecificationException ivse) {
            // invalid version? skip this artifact
            getLog().error("Artifact '" + original.getArtifactId() + "-"
                    + original.getVersion() + "' has a non-resolvable version: " + ivse.getMessage());
            return null;
        }
        org.eclipse.aether.artifact.Artifact classifiedArtifact = new DefaultArtifact(
                original.getGroupId(),
                original.getArtifactId(),
                classifier,
                original.getType(),
                original.getVersion());
        try {
            final ArtifactResult artifactResult = repositorySystem.resolveArtifact(repoSession, new ArtifactRequest(classifiedArtifact, remoteRepositories, null));
            getLog().debug("FOUND an API classified artifact for: "
                    + original.getArtifactId() + "-" + original.getVersion());
            if (artifactResult.isMissing()) {
                getLog().debug("Can not FIND a API classified artifact for: "
                        + original.getArtifactId() + "-" + original.getVersion());
            } else if (artifactResult.isResolved()) {
                return artifactResult.getArtifact();
            }
        } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
            getLog().debug("Can not RESOLVE an API classified artifact for: "
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
     *
     * @return A {@link java.io.File} representing the output directory
     * configured for the compile run.
     */
    protected File getOutputDirectory() {
        return alternateOutputDirectory;
    }
}
