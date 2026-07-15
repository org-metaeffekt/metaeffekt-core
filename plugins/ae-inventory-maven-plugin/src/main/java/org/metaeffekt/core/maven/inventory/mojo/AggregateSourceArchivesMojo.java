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
package org.metaeffekt.core.maven.inventory.mojo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.resolver.ArtifactPattern;
import org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository;
import org.metaeffekt.core.inventory.resolver.SourceArchiveResolverResult;
import org.metaeffekt.core.inventory.validation.ExecutionStatus;
import org.metaeffekt.core.inventory.validation.ExecutionStatusEntry;
import org.metaeffekt.core.maven.inventory.resolver.Mapping;
import org.metaeffekt.core.maven.inventory.resolver.SourceRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Mojo dedicated to automated aggregation of sources. For each artifact in the provided inventory the license metadata
 * is evaluated. Using the source category of the license metadata it is determined whether and where to download the
 * source artifacts.
 */
@Mojo(name = "aggregate-sources", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
@Slf4j
public class AggregateSourceArchivesMojo extends AbstractProjectAwareConfiguredMojo {

    public static final Artifact.Attribute SOURCE_AGGREGATION_MODE = Artifact.Attribute.SOURCE_AGGREGATION_MODE;
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * A list of remote Maven repositories to be used for the compile run.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteProjectRepositories;

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
    @Parameter(defaultValue = "true")
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

    /**
     * Optional YAML configuration file for source aggregation filtering and resolving.
     */
    @Parameter(property = "sourceAggregationConfig")
    private File sourceAggregationConfig;

    /**
     * Optional file path to write a protocol/log of the aggregation execution.
     */
    @Parameter(property = "aggregationProtocolFile")
    private File aggregationProtocolFile;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private org.eclipse.aether.spi.connector.transport.TransporterProvider transporterProvider;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // skip execution for POM packaged projects
        if (isPomPackagingProject()) {
            return;
        }

        if (skip) {
            log.info("Plugin execution skipped.");
            return;
        }

        // validations
        if (inventoryPath == null || !inventoryPath.exists() || !inventoryPath.isFile()) {
            throw new MojoExecutionException("Parameter 'inventoryPath' does not point to valid inventory file: " + inventoryPath);
        }

        // parse global yaml config
        SourceAggregationConfig config;
        try {
            config = SourceAggregationConfig.load(sourceAggregationConfig);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read source aggregation config: " + sourceAggregationConfig, e);
        }

        java.util.Map<String, Object> globalProps = new java.util.HashMap<>();
        if (config.getProperties() != null) {
            globalProps.putAll(config.getProperties());
        }

        // materialize configuration
        final List<ArtifactSourceRepository> delegateArtifactSourceRepositories = new ArrayList<>();
        for (SourceRepository sourceRepository : sourceRepositories) {
            if (sourceRepository.getProperties() == null) {
                sourceRepository.setProperties(new java.util.HashMap<>(globalProps));
            } else {
                sourceRepository.getProperties().putAll(globalProps);
            }

            if (sourceRepository.getFileServerMirror() != null) {
                if (sourceRepository.getFileServerMirror().getSourceUrls() == null || sourceRepository.getFileServerMirror().getSourceUrls().isEmpty()) {
                    if (config.getSourceUrls() != null && !config.getSourceUrls().isEmpty()) {
                        sourceRepository.getFileServerMirror().setSourceUrls(config.getSourceUrls());
                    }
                }
                if (sourceRepository.getFileServerMirror().getCredentials() == null || sourceRepository.getFileServerMirror().getCredentials().isEmpty()) {
                    if (config.getCredentials() != null && !config.getCredentials().isEmpty()) {
                        sourceRepository.getFileServerMirror().setCredentials(config.getCredentials());
                    }
                }
            }

            sourceRepository.dumpConfig(getLog(), "");
            delegateArtifactSourceRepositories.add(sourceRepository.constructDelegate(repositorySystem, repositorySystemSession, remoteProjectRepositories, transporterProvider));
        }

        final ExecutionStatus executionStatus = new ExecutionStatus();
        final AggregationProtocol protocol = new AggregationProtocol(aggregationProtocolFile);

        try {
            log.info("Loading inventory: {}", inventoryPath);

            final Inventory inventory = new InventoryReader().readInventory(inventoryPath);

            // iterate the license metadata and evaluate source category; we assume the license metadata was
            // filtered.
            for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : inventory.getArtifacts()) {

                ArtifactProtocolEntry protocolEntry = new ArtifactProtocolEntry();
                protocolEntry.setArtifactRepresentation(String.valueOf(createArtifactRepresentation(artifact, inventory)));
                protocol.addEntry(protocolEntry);

                if (!shouldIncludeArtifact(artifact, inventory, config, protocolEntry)) {
                    continue;
                }

                String aggregationMode = artifact.get(SOURCE_AGGREGATION_MODE);
                boolean isAcceptMissing = "accept missing".equalsIgnoreCase(aggregationMode);

                final LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);

                // differentiate source category RETAINED and ANNEX
                if (licenseMetaData != null && StringUtils.isNotBlank(licenseMetaData.getSourceCategory())) {
                    String sourceCategory = licenseMetaData.getSourceCategory().trim().toLowerCase();
                    switch (sourceCategory) {
                        case LicenseMetaData.SOURCE_CATEGORY_ADDITIONAL:
                        case LicenseMetaData.SOURCE_CATEGORY_RETAINED:
                            downloadArtifact(artifact, inventory, retainedSourcesSourcePath, delegateArtifactSourceRepositories, executionStatus, isAcceptMissing, protocolEntry, config);
                            break;
                        case LicenseMetaData.SOURCE_CATEGORY_EXTENDED:
                        case LicenseMetaData.SOURCE_CATEGORY_ANNEX:
                            downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories, executionStatus, isAcceptMissing, protocolEntry, config);
                            break;
                        default:
                            throw new MojoExecutionException(format("Source category of license meta data for %s unknown: '%s'",
                                    licenseMetaData.deriveQualifier(), licenseMetaData.getSourceCategory()));
                    }
                } else {
                    // if license metadata or no source category annotation is given, we evaluate includeAllSources
                    if (includeAllSources) {
                        downloadArtifact(artifact, inventory, softwareDistributionAnnexSourcePath, delegateArtifactSourceRepositories, executionStatus, isAcceptMissing, protocolEntry, config);
                    } else {
                        // in this case we skip the source download
                    }
                }
            }

            if (executionStatus.isError()) {
                log.error("Source aggregation incomplete:");
                for (ExecutionStatusEntry entry : executionStatus.getEntries()) {
                    if (entry.getSeverity() == ExecutionStatusEntry.SEVERITY.ERROR) {
                        log.error(entry.getMessage());
                    }
                }
                if (aggregationProtocolFile != null) {
                    protocol.writeToFile();
                }
                throw new MojoExecutionException("Aggregation of source artifacts failed. At least one source artifact was not retrieved.");
            }

            if (aggregationProtocolFile != null) {
                protocol.writeToFile();
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void downloadArtifact(Artifact artifact, Inventory inventory, File targetPath,
                                  List<ArtifactSourceRepository> sourceRepositories, ExecutionStatus executionStatus, boolean isAcceptMissing, ArtifactProtocolEntry protocolEntry, SourceAggregationConfig config) throws IOException {

        // otherwise apply matching repos
        final List<ArtifactSourceRepository> matchingSourceRepositories =
                findMatchingArtifactSourceRepositories(artifact, inventory, sourceRepositories);

        final Object artifactRepresentation = createArtifactRepresentation(artifact, inventory);

        if (matchingSourceRepositories != null && !matchingSourceRepositories.isEmpty()) {

            log.info("Resolving source artifacts for [{}].", artifactRepresentation);

            List<String> attemptedSourceLocations = new ArrayList<>();
            List<String> attemptedSourceRepositories = new ArrayList<>();

            for (ArtifactSourceRepository matchingSourceRepository : matchingSourceRepositories) {

                // early exit for explicitly ignored artifacts
                if (matchingSourceRepository.getSourceArchiveResolver() == null) {
                    if (!matchingSourceRepository.isIgnoreMatches()) {
                        log.info("Skipped resolving sources for artifact [{}] because it is configured as ignored.", artifactRepresentation);
                    }
                    if (isAcceptMissing) {
                        log.info("Skipped resolving sources for artifact [{}] because it is configured as acceptMissing.", artifactRepresentation);
                        return;
                    }
                    return;
                }

                // resolve
                SourceArchiveResolverResult result = matchingSourceRepository.resolveSourceArchive(artifact, targetPath);

                if (result.isEmpty()) {
                    if (!result.getAttemptedResourceLocations().isEmpty()) {
                        attemptedSourceLocations.addAll(result.getAttemptedResourceLocations());
                        for (String location : result.getAttemptedResourceLocations()) {
                            protocolEntry.addAttemptedLocation(location);
                        }
                    }
                    attemptedSourceRepositories.add(matchingSourceRepository.getId());
                } else {
                    if (!result.getAttemptedResourceLocations().isEmpty()) {
                        for (String location : result.getAttemptedResourceLocations()) {
                            protocolEntry.addAttemptedLocation(location);
                        }
                        protocolEntry.setDownloadedLocation(result.getAttemptedResourceLocations().get(result.getAttemptedResourceLocations().size() - 1));
                    }
                    protocolEntry.setDownloadStatus("SUCCESS");

                    String dynamicTargetFolder = "";
                    String successfulUrl = protocolEntry.getDownloadedLocation();

                    if (successfulUrl != null && config != null && config.getTargetFolderMappings() != null) {
                        for (SourceAggregationConfig.TargetFolderMapping mapping : config.getTargetFolderMappings()) {
                            if (successfulUrl.matches(mapping.getUrlPattern())) {
                                dynamicTargetFolder = mapping.getTargetFolder();
                                break;
                            }
                        }
                    }

                    for (File file : result.getFiles()) {
                        // copy file to target folder if necessary
                        File effectiveTargetDir = targetPath;
                        if (dynamicTargetFolder != null && !dynamicTargetFolder.isEmpty()) {
                            effectiveTargetDir = new File(targetPath, dynamicTargetFolder);
                            if (!effectiveTargetDir.exists()) {
                                effectiveTargetDir.mkdirs();
                            }
                        }
                        File destFile = new File(effectiveTargetDir, file.getName());
                        if (!destFile.exists()) {
                            FileUtils.copyFile(file, destFile);
                        }
                    }
                    return;
                }
            }

            logOrFailOn(format("Attempted resource locations:"), false, executionStatus, artifact, isAcceptMissing);
            if (attemptedSourceLocations.isEmpty()) {
                logOrFailOn("No resource location mapped.", false, executionStatus, artifact, isAcceptMissing);
            } else {
                for (String s : attemptedSourceLocations) {
                    logOrFailOn(s, false, executionStatus, artifact, isAcceptMissing);
                }
            }
            if (isAcceptMissing) {
                protocolEntry.setDownloadStatus("ACCEPTED MISSING");
            } else {
                protocolEntry.setDownloadStatus("FAILED");
            }
            logOrFailOn(format("No sources resolved for artifact [%s], while matching source repositories were: %s",
                    artifactRepresentation, attemptedSourceRepositories), true, executionStatus, artifact, isAcceptMissing);
        } else {
            if (isAcceptMissing) {
                protocolEntry.setDownloadStatus("ACCEPTED MISSING");
            } else {
                protocolEntry.setDownloadStatus("FAILED");
            }
            logOrFailOn(format("No sources resolved for artifact [%s], no matching source repository identified.",
                    artifactRepresentation), true, executionStatus, artifact, isAcceptMissing);
        }

    }

    private List<ArtifactSourceRepository> findMatchingArtifactSourceRepositories(Artifact artifact, Inventory inventory, List<ArtifactSourceRepository> sourceRepositories) {
        final List<ArtifactSourceRepository> matchedRepositories = new ArrayList<>();

        final LicenseMetaData lmd = inventory.findMatchingLicenseMetaData(artifact);
        final String component = artifact.getComponent();
        final String artifactVersion = artifact.getVersion();

        String effectiveLicense = artifact.getLicense();
        if (lmd != null && lmd.deriveLicenseInEffect() != null) {
            effectiveLicense = lmd.deriveLicenseInEffect();
        }

        for (ArtifactSourceRepository sourceRepository : sourceRepositories) {
            final ArtifactPattern artifactGroup = sourceRepository.findMatchingArtifactGroup(
                    artifact.getId(), component, artifactVersion, effectiveLicense);
            if (artifactGroup != null) {
                matchedRepositories.add(sourceRepository);
            }
        }
        return matchedRepositories;
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

    private void logOrFailOn(String message, boolean failInCase, ExecutionStatus executionStatus, Artifact artifact, boolean isAcceptMissing) {
        if (failOnMissingSources && !isAcceptMissing) {
            if (failInCase) {
                executionStatus.error(message, artifact);
            } else {
                log.warn(message);
            }
        } else {
            log.warn(message);
        }
    }

    boolean shouldIncludeArtifact(Artifact artifact, Inventory inventory, SourceAggregationConfig config, ArtifactProtocolEntry protocolEntry) throws MojoExecutionException {
        String aggregationMode = artifact.get(SOURCE_AGGREGATION_MODE);
        boolean isExclude = "exclude".equalsIgnoreCase(aggregationMode);
        boolean isInclude = "include".equalsIgnoreCase(aggregationMode);
        boolean isAcceptMissing = "accept missing".equalsIgnoreCase(aggregationMode);

        if (isExclude) {
            log.info("Skipping artifact [{}] due to explicit exclude mode.", createArtifactRepresentation(artifact, inventory));
            protocolEntry.setIncludeStatus("EXCLUDED");
            protocolEntry.setIncludeReason("Explicitly excluded via 'Source Aggregation Mode' = 'exclude'");
            return false;
        }

        if (isInclude) {
            protocolEntry.setIncludeStatus("INCLUDED");
            protocolEntry.setIncludeReason("Explicitly included via 'Source Aggregation Mode' = 'include'");
        } else if (isAcceptMissing) {
            protocolEntry.setIncludeStatus("INCLUDED");
            protocolEntry.setIncludeReason("Accept missing via 'Source Aggregation Mode' = 'accept missing'");
        } else {
            String includeReason = matchImplicitRulesReason(artifact, inventory, config.getInclude());
            String excludeReason = matchImplicitRulesReason(artifact, inventory, config.getExclude());

            if (includeReason != null && excludeReason != null) {
                throw new MojoExecutionException(format("Ambiguous implicit inclusion/exclusion for artifact [%s]. Both rules matched.", createArtifactRepresentation(artifact, inventory)));
            }

            if (excludeReason != null) {
                log.info("Skipping artifact [{}] due to implicit exclude rules.", createArtifactRepresentation(artifact, inventory));
                protocolEntry.setIncludeStatus("EXCLUDED");
                protocolEntry.setIncludeReason("Implicitly excluded (" + excludeReason + ")");
                return false;
            }

            if (includeReason == null) {
                if (!hasLicense(artifact, inventory)) {
                    if (config.isDefaultNoLicenseExclusion()) {
                        log.info("Skipping artifact [{}] because it has no license and excludeIfNoLicense is true.", createArtifactRepresentation(artifact, inventory));
                        protocolEntry.setIncludeStatus("EXCLUDED");
                        protocolEntry.setIncludeReason("Implicitly excluded (No license and excludeIfNoLicense is true)");
                        return false;
                    }
                } else if (!config.isDefaultImplicitInclusion()) {
                    log.info("Skipping artifact [{}] because defaultImplicitInclusion is false.", createArtifactRepresentation(artifact, inventory));
                    protocolEntry.setIncludeStatus("EXCLUDED");
                    protocolEntry.setIncludeReason("Implicitly excluded (defaultImplicitInclusion is false)");
                    return false;
                }
                protocolEntry.setIncludeStatus("INCLUDED");
                protocolEntry.setIncludeReason("Default implicit inclusion");
            } else {
                protocolEntry.setIncludeStatus("INCLUDED");
                protocolEntry.setIncludeReason("Implicitly included (" + includeReason + ")");
            }
        }
        return true;
    }

    boolean hasLicense(Artifact artifact, Inventory inventory) {
        return !getEffectiveLicenses(artifact, inventory).isEmpty();
    }

    List<String> getEffectiveLicenses(Artifact artifact, Inventory inventory) {
        String license = inventory != null ? inventory.getEffectiveLicense(artifact) : artifact.getLicense();
        if (license == null) {
            return new ArrayList<>();
        }
        return org.metaeffekt.core.inventory.InventoryUtils.tokenizeLicense(license, false, false);
    }

    String matchImplicitRulesReason(Artifact artifact, Inventory inventory, SourceAggregationConfig.ImplicitConfig config) {
        if (config == null) {
            return null;
        }

        // Check licenses
        List<String> licenses = getEffectiveLicenses(artifact, inventory);
        if (config.getLicenses() != null && !config.getLicenses().isEmpty() && licenses != null && !licenses.isEmpty()) {
            for (String license : licenses) {
                if (config.getLicenses().contains(license)) {
                    return "Matched license: " + license;
                }
            }
        }

        // Check patterns (groupId / namespace)
        String groupId = artifact.getGroupId();
        if (groupId == null) {
            groupId = artifact.get("Namespace");
        }
        if (groupId != null && config.getPatterns() != null && !config.getPatterns().isEmpty()) {
            for (String patternStr : config.getPatterns()) {
                String originalPattern = patternStr;
                patternStr = patternStr.trim();
                if (patternStr.startsWith("/") && patternStr.endsWith("/") && patternStr.length() > 2) {
                    // Regex pattern
                    String regex = patternStr.substring(1, patternStr.length() - 1);
                    if (Pattern.compile(regex).matcher(groupId).matches()) {
                        return "Matched pattern: " + originalPattern;
                    }
                } else {
                    // Literal pattern
                    if (groupId.equals(patternStr)) {
                        return "Matched pattern: " + originalPattern;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }
}
