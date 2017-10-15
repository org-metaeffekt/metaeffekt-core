package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.inventory.processor.model.*;
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
     * Sources for selected artifacts are downloaded to this folder as part of the extended distribution.
     *
     * @parameter expression="${project.build.directory}/extended-distribution/sources"
     */
    @Parameter(defaultValue = "${project.build.directory}/extended-distribution/sources")
    private File extendedDistributionSourcePath;

    /**
     * Sources for selected artifacts are downloaded to this folder as part of the additional sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/additional-sources/sources")
    private File additionalSourcesSourcePath;

    /**
     * The mojo supports to specify a mapping von artifact id to group id to provide an alternative to download the
     * sources from. This indirection can be used, when originally no source is provided and needs to be augmented.
     */
    @Parameter
    private Mapping alternativeArtifactIdToGroupIdMapping;

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

                // iterate the license meta data and evaluate source category; we assume the license meta data was
                // filtered.
                for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData != null) {
                        if (StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                            String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                            switch (sourceCategory) {
                                case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                                    downloadArtifact(artifact, additionalSourcesSourcePath);
                                    break;
                                case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                                    downloadArtifact(artifact, extendedDistributionSourcePath);
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
        Artifact sourceArtifact = resolveSourceArtifact(artifact);
        if (sourceArtifact != null) {
            FileUtils.copyFile(sourceArtifact.getFile(), new File(targetPath, sourceArtifact.getFile().getName()));
        } else {
            logOrFailOn(String.format("Cannot resolve sources for %s.", artifact.createStringRepresentation()));
        }
    }

    private void logOrFailOn(String message) throws MojoFailureException {
        if (failOnMissingSources) {
            throw new MojoFailureException(message);
        } else {
            getLog().warn(message);
        }
    }

    private Artifact resolveSourceArtifact(org.metaeffekt.core.inventory.processor.model.Artifact artifact) throws MojoFailureException {
        ArtifactHandler handler = new DefaultArtifactHandler(artifact.getType());
        try {
            artifact.deriveArtifactId();

            // allow to derive a groupId from the artifact id
            String groupId = artifact.getGroupId();
            final String artifactId = artifact.getArtifactId();
            if (alternativeArtifactIdToGroupIdMapping != null && alternativeArtifactIdToGroupIdMapping.getMap().containsKey(artifactId)) {
                groupId = alternativeArtifactIdToGroupIdMapping.getMap().get(artifactId);
            }

            Artifact sourceArtifact = new DefaultArtifact(
                    groupId, artifactId,
                    VersionRange.createFromVersionSpec(artifact.getVersion()),
                    "runtime", artifact.getType(), "sources", handler);

            getLog().info("Resolving " + sourceArtifact);
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(sourceArtifact);
            request.setLocalRepository(localRepository);
            request.setRemoteRepositories(remoteRepositories);
            ArtifactResolutionResult result = resolver.resolve(request);
            if (result != null && result.isSuccess()) {
                return result.getArtifacts().iterator().next();
            } else {
                logOrFailOn(String.format("Cannot resolve sources for %s.", artifact.createStringRepresentation()));
            }
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
