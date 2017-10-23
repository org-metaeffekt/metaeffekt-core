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
        try {
            artifact.deriveArtifactId();

            // preset for default source resolving
            String groupId = artifact.getGroupId();
            String artifactId = artifact.getArtifactId();
            String version = artifact.getVersion();
            String classifier = "sources";
            String type = artifact.getType();

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
            sourceCoordinates.append(groupId).append(":");
            sourceCoordinates.append(artifactId).append(":");
            sourceCoordinates.append(version).append(":");
            sourceCoordinates.append(classifier).append(":");
            sourceCoordinates.append(type).append(":");

            ArtifactHandler handler = new DefaultArtifactHandler(type);
            Artifact sourceArtifact = new DefaultArtifact(groupId, artifactId,
                    VersionRange.createFromVersionSpec(version), "runtime", type, classifier, handler);
            getLog().info("Resolving " + sourceArtifact);

            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(sourceArtifact);
            request.setLocalRepository(localRepository);
            request.setRemoteRepositories(remoteRepositories);
            ArtifactResolutionResult result = resolver.resolve(request);
            if (result != null && result.isSuccess()) {
                return result.getArtifacts().iterator().next();
            } else {
                logOrFailOn(String.format("Cannot resolve sources for %s with source parameters %s.",
                        artifact.createStringRepresentation(), sourceCoordinates.toString()));

            }
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return null;
    }

    private String extractPart(String[] mappedSourceParts, int index, String defaultValue) {
        if (mappedSourceParts.length > index && StringUtils.isNotBlank(mappedSourceParts[index])) {
            return mappedSourceParts[index].trim();
        }
        return defaultValue;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
