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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.CspLoader;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;
import java.util.List;

/**
 * Abstract mojo. Base class for reporting mojos.
 */
public abstract class AbstractInventoryReportCreationMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * The root inventory dir.
     */
    @Parameter
    private File sourceInventoryDir;

    /**
     * Includes of the source inventory; relative to the sourceInventoryDir.
     */
    @Parameter(defaultValue = "*.xls*")
    private String sourceInventoryIncludes;

    /**
     * Location of components relative to the sourceInventoryDir.
     */
    @Parameter(required = true, defaultValue = "components")
    private String sourceComponentPath;

    /**
     * Location of licenses relative to the sourceInventoryDir.
     */
    @Parameter(required = true, defaultValue = "licenses")
    private String sourceLicensePath;

    @Parameter(defaultValue = "${project.build.directory}/inventory")
    private File targetInventoryDir;

    @Parameter(defaultValue = "${project.artifactId}-${project.version}-inventory.xls")
    private String targetInventoryPath;

    @Parameter(property = "project.name")
    private String projectName;

    @Parameter(defaultValue = "true")
    private boolean failOnError;

    @Parameter(defaultValue = "true")
    private boolean failOnBanned;

    @Parameter(defaultValue = "true")
    private boolean failOnDowngrade;

    @Parameter(defaultValue = "true")
    private boolean failOnUnknown;

    @Parameter(defaultValue = "true")
    private boolean failOnUnknownVersion;

    @Parameter(defaultValue = "true")
    private boolean failOnDevelopment;

    @Parameter(defaultValue = "true")
    private boolean failOnInternal;

    @Parameter(defaultValue = "true")
    private boolean failOnUpgrade;

    @Parameter(defaultValue = "true")
    private boolean failOnMissingLicense;

    @Parameter(defaultValue = "false")
    private boolean failOnMissingLicenseFile;

    @Parameter(defaultValue = "false")
    private boolean failOnMissingNotice;

    @Parameter(defaultValue = "false")
    private boolean failOnMissingComponentFiles;

    @Parameter
    private String artifactExcludes;

    @Parameter
    private String artifactIncludes;

    @Parameter
    private File diffInventoryFile;

    @Parameter(defaultValue = "false")
    private boolean enableAssetReport;

    @Parameter(defaultValue = "false")
    private boolean enableAssessmentReport;

    @Parameter(defaultValue = "false")
    private boolean enableBomReport;

    @Parameter(defaultValue = "false")
    private boolean enablePomReport;

    @Parameter(defaultValue = "false")
    private boolean enableDiffReport;

    @Parameter(defaultValue = "false")
    private boolean enableVulnerabilityReport;

    @Parameter(defaultValue = "false")
    private boolean enableVulnerabilityReportSummary;

    @Parameter(defaultValue = "false")
    private boolean enableVulnerabilityStatisticsReport;

    @Parameter(defaultValue = "${basedir}/src/main/dita/${project.artifactId}/gen")
    private File targetReportDir;

    @Parameter
    private File targetLicenseDir;

    @Parameter
    private File targetComponentDir;

    @Parameter(defaultValue = "licenses")
    private String relativeLicensePath;

    @Parameter
    private List<Artifact> addOnArtifacts;

    @Parameter(defaultValue = "false")
    private boolean skip;

    // vulnerability report parameters

    /**
     * The CspLoader instance that will provide the {@link CentralSecurityPolicyConfiguration} instance to use for report generation.
     *
     */
    @Parameter
    private CspLoader securityPolicy = new CspLoader();

    @Parameter(defaultValue = "false")
    private boolean filterVulnerabilitiesNotCoveredByArtifacts;

    @Parameter(defaultValue = "false")
    private boolean filterAdvisorySummary;

    @Parameter(defaultValue = "false")
    private boolean hidePriorityScoreInformation = false;

    // other template parameters

    @Parameter(defaultValue = "en")
    private String templateLanguageSelector;

    @Parameter(defaultValue = "default")
    private String reportContextId;

    @Parameter
    private String reportContextTitle;

    @Parameter
    private String reportContextGroup;

    @Parameter(defaultValue = "false")
    private boolean includeInofficialOsiStatus;

    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = new InventoryReport(configureParameters().build());
        configureInventoryReport(report);
        return report;
    }

    protected ReportConfigurationParameters.ReportConfigurationParametersBuilder configureParameters() {
        return ReportConfigurationParameters.builder()
                .hidePriorityInformation(hidePriorityScoreInformation)
                .reportLanguage(templateLanguageSelector)
                .includeInofficialOsiStatus(includeInofficialOsiStatus)
                .filterVulnerabilitiesNotCoveredByArtifacts(filterVulnerabilitiesNotCoveredByArtifacts)
                .filterAdvisorySummary(filterAdvisorySummary)
                .failOnDevelopment(failOnDevelopment)
                .failOnDevelopment(failOnDevelopment)
                .failOnError(failOnError)
                .failOnBanned(failOnBanned)
                .failOnDowngrade(failOnDowngrade)
                .failOnInternal(failOnInternal)
                .failOnUnknown(failOnUnknown)
                .failOnUnknownVersion(failOnUnknownVersion)
                .failOnUpgrade(failOnUpgrade)
                .failOnMissingLicense(failOnMissingLicense)
                .failOnMissingLicenseFile(failOnMissingLicenseFile)
                .failOnMissingComponentFiles(failOnMissingComponentFiles)
                .failOnMissingNotice(failOnMissingNotice)
                .assetBomReportEnabled(enableAssetReport)
                .assessmentReportEnabled(enableAssessmentReport)
                .inventoryPomEnabled(enablePomReport)
                .inventoryDiffReportEnabled(enableDiffReport)
                .inventoryBomReportEnabled(enableBomReport)
                .inventoryVulnerabilityReportEnabled(enableVulnerabilityReport)
                .inventoryVulnerabilityReportSummaryEnabled(enableVulnerabilityReportSummary)
                .inventoryVulnerabilityStatisticsReportEnabled(enableVulnerabilityStatisticsReport);
    }

    protected void configureInventoryReport(InventoryReport report) {
        report.setProjectName(projectName);

        // source inventory settings
        report.setReferenceInventoryDir(sourceInventoryDir);
        report.setReferenceInventoryIncludes(sourceInventoryIncludes);
        report.setReferenceLicensePath(sourceLicensePath);
        report.setReferenceComponentPath(sourceComponentPath);

        // target inventory settings
        report.setTargetInventoryDir(targetInventoryDir);
        report.setTargetInventoryPath(targetInventoryPath);
        report.setTargetLicenseDir(targetLicenseDir);
        report.setTargetComponentDir(targetComponentDir);

        // vulnerability settings
        final CentralSecurityPolicyConfiguration activeSecurityPolicy = this.securityPolicy.loadConfiguration();

        // log the security policy only when it fits the context -->
        if (enableAssessmentReport || enableVulnerabilityReport || enableVulnerabilityReportSummary || enableVulnerabilityStatisticsReport) {
            getLog().debug("");
            getLog().debug("-------------------< Security Policy Configuration >--------------------");
            activeSecurityPolicy.debugLogConfiguration();
            getLog().debug("");
        }

        report.setSecurityPolicy(activeSecurityPolicy);

        // diff settings
        report.setDiffInventoryFile(diffInventoryFile);

        report.setTargetReportDir(targetReportDir);

        report.setRelativeLicensePath(relativeLicensePath);

        report.setAddOnArtifacts(addOnArtifacts);

        // configure report context
        report.setReportContext(new ReportContext(reportContextId, reportContextTitle, reportContextGroup));

        if (artifactExcludes != null) {
            PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
            artifactFilter.addIncludePatterns(
                    ParameterConversionUtil.convertStringToStringArray(artifactExcludes, ","));
            if (artifactIncludes != null) {
                artifactFilter.addExcludePatterns(
                        ParameterConversionUtil.convertStringToStringArray(artifactIncludes, ","));
            }
            report.setArtifactFilter(artifactFilter);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // skip execution for POM packaged projects
        if (isPomPackagingProject()) {
            return;
        }

        if (skip) {
            getLog().info("Plugin execution skipped.");
            return;
        }
        boolean success;
        try {
            InventoryReport report = initializeInventoryReport();
            success = report.createReport();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (!success) {
            throw new MojoFailureException("Failing build due to findings in report.");
        }
    }

}
