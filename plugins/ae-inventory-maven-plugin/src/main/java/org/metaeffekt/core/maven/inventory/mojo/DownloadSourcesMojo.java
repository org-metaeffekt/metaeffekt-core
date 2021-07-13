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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.io.IOException;

/**
 * Mojo dedicated to automated downloading sources. For each artifact in the provided inventory the license meta data
 * is evaluated. Using the source category of the license meta data it is determined whether and whereto download the
 * source artifacts.
 */
@Mojo( name = "download-sources", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class DownloadSourcesMojo extends AbstractProjectAwareMojo {

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
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Boolean enabling to skip the execution of the mojo.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * The inventory path points to the filtered artifact inventory. Usually this is the output from a previous scan
     * process.
     */
    @Parameter(defaultValue = "${project.build.directory}/inventory/${project.artifactId}-${project.version}-inventory.xls")
    private File inventoryPath;

    /**
     * Sources for selected artifacts are downloaded to this folder as part of the distribution annex.
     */
    @Parameter(defaultValue = "${project.build.directory}/annex/sources")
    private File softwareDistributionAnnexSourcePath;

    /**
     * Sources for selected artifacts are downloaded to this folder as part of the retained sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/retained-sources/sources")
    private File retainedSourcesSourcePath;

    /**
     * Some source files may not be located at the expected preset location. This mapping allows to map a file to an
     * alternative location. Key for the mapping is the file name (later with checksum). Values are new maven
     * source artifact coordinates in the shape groupId:artifactId:version:classifier:type.
     */
    @Parameter
    private Mapping alternativeArtifactSourceMapping;


    @Parameter(defaultValue = "true")
    private boolean failOnMissingSources;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {

            // skip execution for POM packaged projects
            if (isPomPackagingProject()) {
                return;
            }

            if (skip) {
                getLog().info("Plugin execution skipped.");
                return;
            }

            // validations
            if (inventoryPath == null || !inventoryPath.exists() || !inventoryPath.isFile()) {
                throw new MojoExecutionException("Parameter 'inventoryPath' does not point to valid inventory file: " + inventoryPath);
            }

            try {
                Inventory inventory = new InventoryReader().readInventory(inventoryPath);

                // iterate the license metadata and evaluate source category; we assume the license metadata was
                // filtered.
                for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData != null) {
                        if (StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                            String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                            switch (sourceCategory) {
                                case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                                case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                                    downloadArtifact(artifact, retainedSourcesSourcePath);
                                    break;
                                case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                                case LicenseMetaData.SOURCE_CATEGORY_ANNEX:                              
                                    downloadArtifact(artifact, softwareDistributionAnnexSourcePath);
                                    break;
                                default:
                                    throw new MojoExecutionException(
                                            String.format("Source category of license meta data for %s unknown: '%s'",
                                                    licenseMetaData.deriveQualifier(), licenseMetaData.getSourceCategory()));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } finally {
            MavenLogAdapter.release();
        }
    }

    private void downloadArtifact(org.metaeffekt.core.inventory.processor.model.Artifact artifact, File targetPath) throws IOException, MojoFailureException {
        org.apache.maven.artifact.Artifact sourceArtifact = resolveSourceArtifact(artifact);
        if (sourceArtifact != null) {
            FileUtils.copyFile(sourceArtifact.getFile(), new File(targetPath, sourceArtifact.getFile().getName()));
        }
    }

    private void logOrFailOn(String message) throws MojoFailureException {
        if (failOnMissingSources) {
            throw new MojoFailureException(message);
        } else {
            getLog().warn(message);
        }
    }

    private org.apache.maven.artifact.Artifact resolveSourceArtifact(org.metaeffekt.core.inventory.processor.model.Artifact artifact) throws MojoFailureException {
        try {
            artifact.deriveArtifactId();

            // preset for default source resolving
            String groupId = artifact.getGroupId();
            String artifactId = artifact.getArtifactId();
            String version = artifact.getVersion();
            String classifier = "sources";
            String type = artifact.getType();

            if (isEmpty(groupId)) {
                logOrFailOn("Cannot process " + artifact.getId() + " without group id.");
                return null;
            }
            if (isEmpty(artifactId)) {
                logOrFailOn("Cannot process " + artifact.getId() + " without artifact id.");
                return null;
            }
            if (isEmpty(version)) {
                logOrFailOn("Cannot process " + artifact.getId() + " without version.");
                return null;
            }
            if (isEmpty(type)) {
                logOrFailOn("Cannot process " + artifact.getId() + " without type.");
                return null;
            }

            // enable mapping (by filename)
            if (alternativeArtifactSourceMapping != null &&
                    alternativeArtifactSourceMapping.getMap().containsKey(artifact.getId())) {
                String mappedSource = alternativeArtifactSourceMapping.getMap().get(artifact.getId());
                // FIXME: for uniqueness a checksum may be required here

                String[] mappedSourceParts = mappedSource.split(":");
                groupId = extractPart(mappedSourceParts, 0, groupId);
                artifactId = extractPart(mappedSourceParts, 1, artifactId);
                version = extractPart(mappedSourceParts, 2, version);
                classifier = extractPart(mappedSourceParts, 3, classifier);
                type = extractPart(mappedSourceParts, 4, type);
            }

            StringBuilder sourceCoordinates = new StringBuilder();
            appendPart(sourceCoordinates, groupId);
            appendPart(sourceCoordinates, artifactId);
            appendPart(sourceCoordinates, version);
            appendPart(sourceCoordinates, classifier);
            appendPart(sourceCoordinates, type);

            final DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
            final DefaultArtifact sourceArtifact = new DefaultArtifact(groupId, artifactId,
                    VersionRange.createFromVersionSpec(version), "runtime", type, classifier, handler);

            getLog().info("Resolving " + sourceArtifact);

            final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(sourceArtifact);
            request.setLocalRepository(localRepository);
            request.setRemoteRepositories(remoteRepositories);

            final ArtifactResolutionResult result = resolver.resolve(request);

            if (result != null && result.isSuccess()) {
                return result.getArtifacts().iterator().next();
            } else {
                logOrFailOn(String.format("Cannot resolve sources for [%s] with source parameters [%s].",
                        artifact.createStringRepresentation(), sourceCoordinates.toString()));
                return null;

            }
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private boolean isEmpty(String string) {
        if (string == null) return true;
        if (string.isEmpty()) return true;
        if (string.trim().isEmpty()) return true;
        return false;
    }

    private void appendPart(StringBuilder sourceCoordinates, String part) {
        sourceCoordinates.append(part == null ? "" : part).append(":");
    }

    private String extractPart(String[] mappedSourceParts, int index, String defaultValue) {
        if (mappedSourceParts.length > index && StringUtils.isNotBlank(mappedSourceParts[index])) {
            final String value = mappedSourceParts[index].trim();
            if (value.equals("-")) {
                // value '-' indicates that the value is removed
                return null;
            }
            return value;
        }
        return defaultValue;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
