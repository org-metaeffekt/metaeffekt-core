/**
 * Copyright 2009-2020 the original author or authors.
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.resolver.ArtifactPattern;
import org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository;
import org.metaeffekt.core.inventory.resolver.SourceArchiveResolverResult;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mojo dedicated to automated aggregation of sources. For each artifact in the provided inventory the license metadata
 * is evaluated. Using the source category of the license meta data it is determined whether and whereto download the
 * source artifacts.
 */
@Mojo( name = "aggregate-sources", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class AggregateSourceArchivesMojo extends AbstractProjectAwareMojo {

    public static final String DELIMITER_NEWLINE = String.format("%n");

    /**
     * The ArtifactResolver to be used.
     */
    @Component
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * A list of repositories, where the source archives for the included artifacts can be loaded from.
     */
    @Parameter(required = true)
    private List<SourceRepository> sourceRepositories;

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
     * Boolean indicating whether all sources need to be included (except retained).
     */
    @Parameter(defaultValue = "false")
    private boolean includeAllSources;

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

    /**
     * Indicates whether the process should fail on missing sources.
     */
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

            // materialize configuration
            final List<ArtifactSourceRepository> delegateArtifactSourceRepositories = new ArrayList<>();
            for (SourceRepository sourceRepository : sourceRepositories) {
                sourceRepository.dumpConfig(getLog(), "");
                delegateArtifactSourceRepositories.add(sourceRepository.constructDelegate());
            }

            final ExecutionStatus executionStatus = new ExecutionStatus();

            try {
                getLog().info("Loading inventory: " + inventoryPath);

                final Inventory inventory = new InventoryReader().readInventory(inventoryPath);

                // iterate the license metadata and evaluate source category; we assume the license metadata was
                // filtered.
                for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {
                    final LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);

                    // differentiate source category RETAINED and ANNEX
                    if (licenseMetaData != null && StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                        String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                        switch (sourceCategory) {
                            case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                            case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                                downloadArtifact(artifact, inventory, retainedSourcesSourcePath, delegateArtifactSourceRepositories, executionStatus);
                                break;
                            case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                            case LicenseMetaData.SOURCE_CATEGORY_ANNEX:
                                downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories, executionStatus);
                                break;
                            default:
                                throw new MojoExecutionException(
                                        String.format("Source category of license meta data for %s unknown: '%s'",
                                                licenseMetaData.deriveQualifier(), licenseMetaData.getSourceCategory()));
                        }
                    } else {
                        // if license metadata or no source category annotation is given, we evaluate includeAllSources
                        if (includeAllSources) {
                            downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories, executionStatus);
                        } else {
                            // in this case we skip the source download
                        }
                    }
                }

                if (executionStatus.isError()) {
                    getLog().error("Source aggregation incomplete:");
                    for (ExecutionStatusEntry entry : executionStatus.getEntries()) {
                        if (entry.getSeverity() == ExecutionStatusEntry.SEVERITY.ERROR) {
                            getLog().error(entry.getMessage());
                        }
                    }
                    throw new MojoExecutionException("Aggregation of source artifacts failed. At least one source artifact was not retrieved.");
                }

            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } finally {
            MavenLogAdapter.release();
        }
    }

    private void downloadArtifact(Artifact artifact, Inventory inventory, File targetPath,
          List<ArtifactSourceRepository> sourceRepositories, ExecutionStatus executionStatus) throws IOException, MojoFailureException {

        final ArtifactSourceRepository matchingSourceRepository =
                findMatchingArtifactSourceRepository(artifact, inventory, sourceRepositories);

        final Object artifactRepresentation = createArtifactRepresentation(artifact, inventory);

        if (matchingSourceRepository != null) {
            // early exit for explicitly ignored artifacts
            if (matchingSourceRepository.getSourceArchiveResolver() == null) {
                if (!matchingSourceRepository.isIgnoreMatches()) {
                    getLog().info(String.format("Skipped resolving sources for artifact '%s'.", artifactRepresentation));
                }
                return;
            }

            // resolve
            getLog().info(String.format("Resolving source artifacts for '%s'", artifactRepresentation));
            SourceArchiveResolverResult result = matchingSourceRepository.resolveSourceArchive(artifact, targetPath);

            if (result.isEmpty()) {
                if (!result.getAttemptedResourceLocations().isEmpty()) {
                    logOrFailOn(String.format("Attempted resource locations:"), false, executionStatus);
                    for (String s : result.getAttemptedResourceLocations()) {
                        logOrFailOn(s, false, executionStatus);
                    }
                } else {
                    logOrFailOn("No resource location mapped.", false, executionStatus);
                }
                logOrFailOn(String.format("No sources resolved for artifact '%s', while matching source repository was: '%s'",
                        artifactRepresentation, matchingSourceRepository.getId()), true, executionStatus);
                return;
            } else {
                for (File file : result.getFiles()) {
                    // copy file to target folder if necessary
                    File destFile = new File(targetPath, matchingSourceRepository.getTargetFolder() + "/" + file.getName());
                    if (!destFile.exists()) {
                        FileUtils.copyFile(file, destFile);
                    }
                }
                return;
            }
        } else {
            logOrFailOn(String.format("No sources resolved for artifact '%s', no matching source repository identified.",
                    artifactRepresentation), true, executionStatus);
            return;
        }

    }

    private ArtifactSourceRepository findMatchingArtifactSourceRepository(Artifact artifact, Inventory inventory, List<ArtifactSourceRepository> sourceRepositories) {
        final LicenseMetaData lmd = inventory.findMatchingLicenseMetaData(artifact);
        final String component = artifact.getComponent();
        final String artifactVersion = artifact.getVersion();

        String effectiveLicense = artifact.getLicense();
        if (lmd != null && lmd.deriveLicenseInEffect() != null) {
            effectiveLicense = lmd.deriveLicenseInEffect();
        }

        ArtifactSourceRepository matchingSourceRepository = null;
        for (ArtifactSourceRepository sourceRepository : sourceRepositories) {
            ArtifactPattern artifactGroup = sourceRepository.findMatchingArtifactGroup(artifact.getId(), component, artifactVersion, effectiveLicense);
            if (artifactGroup != null) {
                matchingSourceRepository = sourceRepository;
                break;
            }
        }
        return matchingSourceRepository;
    }

    private Object createArtifactRepresentation(Artifact artifact, Inventory inventory) {
        StringBuilder sb = new StringBuilder();
        if (artifact.getId() != null) {
            sb.append(artifact.getId());
        }
        sb.append(':');
        if (artifact.getComponent() != null) {
            sb.append(artifact.getComponent());
        }
        sb.append(':');
        if (artifact.getVersion() != null) {
            sb.append(artifact.getVersion());
        }
        sb.append(':');
        if (inventory != null) {
            LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
            if (licenseMetaData != null) {
                if (licenseMetaData.deriveLicenseInEffect() != null) {
                    sb.append(licenseMetaData.deriveLicenseInEffect());
                }
            } else {
                if (artifact.getLicense() != null) {
                    sb.append(artifact.getLicense());
                }
            }
        }
        return sb;
    }

    private void logOrFailOn(String message, boolean failInCase, ExecutionStatus executionStatus) throws MojoFailureException {
        if (failOnMissingSources) {
            if (failInCase) {
                executionStatus.error(message);
            } else {
                getLog().warn(message);
            }
        } else {
            getLog().warn(message);
        }
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
