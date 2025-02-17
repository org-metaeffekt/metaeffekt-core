/*
 * Copyright 2009-2024 the original author or authors.
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.JSONArray;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Abstract mojo. Base class for reporting mojos.
 */
public abstract class AbstractInventoryReportCreationMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * Local Maven repository where artifacts are cached during the build process.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The root inventory dir.
     *
     * @parameter
     * @required
     */
    private File sourceInventoryDir;

    /**
     * Includes of the source inventory; relative to the sourceInventoryDir.
     *
     * @parameter default-value="*.xls*"
     * @required
     */
    private String sourceInventoryIncludes;

    /**
     * Location of components relative to the sourceInventoryDir.
     *
     * @parameter default-value="components"
     * @required
     */
    private String sourceComponentPath;

    /**
     * Location of licenses relative to the sourceInventoryDir.
     *
     * @parameter default-value="licenses"
     * @required
     */
    private String sourceLicensePath;

    /**
     * @parameter expression="${project.build.directory}/inventory"
     */
    private File targetInventoryDir;

    /**
     * @parameter expression="${project.artifactId}-${project.version}-inventory.xls"
     */
    private String targetInventoryPath;

    /**
     * @parameter expression="${project.name}"
     */
    private String projectName;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnError;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnBanned;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnDowngrade;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnUnknown;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnUnknownVersion;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnDevelopment;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnInternal;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnUpgrade;

    /**
     * @parameter default-value="true"
     */
    private boolean failOnMissingLicense;

    /**
     * @parameter default-value="false"
     */
    private boolean failOnMissingLicenseFile;

    /**
     * @parameter default-value="false"
     */
    private boolean failOnMissingNotice;

    /**
     * @parameter default-value="false"
     */
    private boolean failOnMissingComponentFiles;

    /**
     * @parameter
     */
    private String artifactExcludes;

    /**
     * @parameter
     */
    private String artifactIncludes;

    /**
     * @parameter
     */
    private File diffInventoryFile;

    /**
     * @parameter default-value="false"
     */
    private boolean enableAssetReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enableAssessmentReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enableBomReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enablePomReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enableDiffReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enableVulnerabilityReport;

    /**
     * @parameter default-value="false"
     */
    private boolean enableVulnerabilityReportSummary;

    /**
     * @parameter default-value="false"
     */
    private boolean enableVulnerabilityStatisticsReport;

    /**
     * @parameter expression="${basedir}/src/main/dita/${project.artifactId}/gen"
     */
    private File targetReportDir;

    /**
     * @parameter
     */
    private File targetLicenseDir;

    /**
     * @parameter
     */
    private File targetComponentDir;

    /**
     * @parameter default-value="licenses"
     */
    private String relativeLicensePath;

    /**
     * @parameter
     */
    private List<Artifact> addOnArtifacts;

    /**
     * @parameter default-value="false"
     */
    private boolean skip;

    // vulnerability report parameters

    /**
     * @parameter
     */
    private CentralSecurityPolicyConfiguration securityPolicy;

    /**
     * If set, will overwrite the {@link #securityPolicy} with the contents of this file.<br>
     * If the {@link #securityPolicyOverwriteJson} is set, the properties of both will be merged.
     *
     * @parameter
     */
    private File securityPolicyFile;

    /**
     * If set, will overwrite the {@link #securityPolicy} with the contents of this JSON string.<br>
     * If the {@link #securityPolicyFile} is set, the properties of both will be merged.
     *
     * @parameter
     */
    private String securityPolicyOverwriteJson;

    /**
     * @parameter default-value="false"
     */
    private boolean filterVulnerabilitiesNotCoveredByArtifacts;

    /**
     * @parameter default-value="false"
     */
    private boolean filterAdvisorySummary;

    /**
     * Represents a {@link List}&lt;{@link Map}&lt;{@link String}, {@link String}&gt;&gt;.<br>
     * The key "name" is mandatory and can optionally be combined with an "implementation" value. If the implementation
     * is not specified, the name will be used as the implementation. Each list entry represents a single advisory type.
     * <p>
     * For every provider, an additional overview table will be generated
     * only evaluating the vulnerabilities containing the respecting provider.
     * If left empty, no additional table will be created.<br>
     * See {@link org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore}
     * or all available providers.
     * <p>
     * Example:
     * <pre>
     *     [{"name":"CERT_FR"},
     *      {"name":"CERT_SEI"},
     *      {"name":"RHSA","implementation":"CSAF"}]
     * </pre>
     *
     * @parameter
     */
    private final String generateOverviewTablesForAdvisories = "[]";

    /**
     * @parameter default-value="false"
     */
    private boolean hidePriorityScoreInformation = false;

    // other template parameters

    /**
     * @parameter default-value="ENGLISH"
     */
    private Locale templateLanguageSelector;

    /**
     * @parameter default-value="default"
     */
    private String reportContextId;

    /**
     * @parameter
     */
    private String reportContextTitle;

    /**
     * @parameter
     */
    private String reportContextGroup;

    /**
     * @parameter default-value="false"
     */
    private boolean includeInofficialOsiStatus;

    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = new InventoryReport(
                ReportConfigurationParameters.builder()
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
                        .inventoryVulnerabilityStatisticsReportEnabled(enableVulnerabilityStatisticsReport)
                        .build());
        configureInventoryReport(report);
        return report;
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
        try {
            if (securityPolicyOverwriteJson != null) {
                getLog().info("Reading security policy from securityPolicyOverwriteJson: " + securityPolicyOverwriteJson);
            }
            if (securityPolicyFile != null) {
                getLog().info("Reading security policy from securityPolicyFile: file://" + securityPolicyFile.getCanonicalPath());
            }
            securityPolicy = CentralSecurityPolicyConfiguration.fromConfiguration(securityPolicy, securityPolicyFile, securityPolicyOverwriteJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process security policy configuration: " + e.getMessage(), e);
        }

        // log the security policy only when it fits the context -->
        if (enableAssessmentReport || enableVulnerabilityReport || enableVulnerabilityReportSummary || enableVulnerabilityStatisticsReport) {
            getLog().info("");
            getLog().info("-------------------< Security Policy Configuration >--------------------");
            this.securityPolicy.logConfiguration();
            getLog().info("");
        }

        report.setSecurityPolicy(securityPolicy);

        try {
            report.addGenerateOverviewTablesForAdvisoriesByMap(new JSONArray(generateOverviewTablesForAdvisories));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse generateOverviewTablesForAdvisories, must be a valid content identifier JSONArray: " + generateOverviewTablesForAdvisories, e);
        }

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
        } finally {
            MavenLogAdapter.release();
        }
    }

}
