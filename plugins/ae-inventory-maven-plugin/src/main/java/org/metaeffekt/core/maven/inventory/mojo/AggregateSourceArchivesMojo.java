/**
 * Copyright 2009-2019 the original author or authors.
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
 * Mojo dedicated to automated aggregation of sources. For each artifact in the provided inventory the license meta data
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
            List<ArtifactSourceRepository> delegateArtifactSourceRepositories = new ArrayList<>();
            for (SourceRepository sourceRepository : sourceRepositories) {
                sourceRepository.dumpConfig(getLog(), "");
                delegateArtifactSourceRepositories.add(sourceRepository.constructDelegate());
            }

            try {
                getLog().info("Loading inventory: " + inventoryPath);
                Inventory inventory = new InventoryReader().readInventory(inventoryPath);

                // iterate the license meta data and evaluate source category; we assume the license meta data was
                // filtered.
                for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData != null && StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                        String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                        switch (sourceCategory) {
                            case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                            case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                                downloadArtifact(artifact, inventory, retainedSourcesSourcePath, delegateArtifactSourceRepositories);
                                break;
                            case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                            case LicenseMetaData.SOURCE_CATEGORY_ANNEX:
                                downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories);
                                break;
                            default:
                                throw new MojoExecutionException(
                                        String.format("Source category of license meta data for %s unknown: '%s'",
                                                licenseMetaData.deriveQualifier(), licenseMetaData.getSourceCategory()));
                        }
                    } else {
                        // if license meta data or no source category annotation is given, we evaluate includeAllSources
                        if (includeAllSources) {
                            downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories);
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

    private void downloadArtifact(Artifact artifact, Inventory inventory, File targetPath, List<ArtifactSourceRepository> sourceRepositories) throws IOException, MojoFailureException {
        String component = artifact.getComponent();
        String artifactVersion = artifact.getVersion();

        LicenseMetaData lmd = inventory.findMatchingLicenseMetaData(artifact);

        String associatedLicense = artifact.getLicense();
        String effectiveLicense = associatedLicense;
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


        final Object artifactRepresentation = createArtifactRepresentation(artifact, inventory);

        if (matchingSourceRepository != null) {
            // early exit for explicitly ignored artifacts
            if (matchingSourceRepository.getSourceArchiveResolver() == null) {
                if (!matchingSourceRepository.isIgnoreMatches()) {
                    getLog().warn(String.format("Skipped resolving sources for artifact '%s'.", artifactRepresentation));
                }
                return;
            }

            // resolve
            getLog().info(String.format("Resolving source artifacts for '%s'", artifactRepresentation));
            SourceArchiveResolverResult result = matchingSourceRepository.resolveSourceArchive(artifact, targetPath);

            if (result.isEmpty()) {
                if (!result.getAttemptedResourceLocations().isEmpty()) {
                    logOrFailOn(String.format("Attempted resource locations:"), false);
                    for (String s : result.getAttemptedResourceLocations()) {
                        logOrFailOn(s, false);
                    }
                } else {
                    logOrFailOn("No resource location mapped.", false);
                }
                logOrFailOn(String.format("No sources resolved for artifact '%s', while matching source repository was: '%s'",
                        artifactRepresentation, matchingSourceRepository.getId()), true);
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
                    artifactRepresentation), true);
            return;
        }

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

    private void logOrFailOn(String message, boolean failInCase) throws MojoFailureException {
        if (failOnMissingSources) {
            getLog().error(message);
            if (failInCase) {
                throw new MojoFailureException(message);
            }
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

            DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
            DefaultArtifact sourceArtifact = new DefaultArtifact(groupId, artifactId,
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
                return null;
            }
        } catch (InvalidVersionSpecificationException e) {
            return null;
        }
    }

    private void logMissingSource(String message, Exception e) {
        if (failOnMissingSources) {
            if (e != null) {
                getLog().error(message, e);
            } else {
                getLog().error(message);
            }
        } else {
            getLog().error(message);
        }
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
