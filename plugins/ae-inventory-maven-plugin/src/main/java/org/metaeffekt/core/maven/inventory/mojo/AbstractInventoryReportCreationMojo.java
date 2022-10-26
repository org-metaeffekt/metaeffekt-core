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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.VulnerabilityReportAdapter;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.util.List;

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

    /**
     * @parameter default-value="7.0"
     */
    private String vulnerabilityScoreThreshold;

    /**
     * Comma seperated list of advisory providers. All vulnerabilities not containing at least one of the providers
     * will be ignored when generating the report.<br>
     * If left empty, no filter will be applied.<br>
     * Available advisory providers:
     * <ul>
     *     <li><code>CERT-FR</code></li>
     *     <li><code>CERT-SEI</code></li>
     *     <li><code>MSRC</code></li>
     * </ul>
     * To address all providers, use <code>ALL</code>
     *
     * @parameter default-value=""
     */
    private String vulnerabilityAdvisoryFilter;

    /**
     * Comma seperated list of advisory providers. For every provider, an additional overview table will be generated
     * only evaluating the vulnerabilities containing the respecting provider.<br>
     * If left empty, no additional table will be created.<br>
     * Available advisory providers:
     * <ul>
     *     <li><code>CERT-FR</code></li>
     *     <li><code>CERT-SEI</code></li>
     *     <li><code>MSRC</code></li>
     * </ul>
     * To address all providers, use <code>ALL</code>
     *
     * @parameter default-value=""
     */
    private String generateOverviewTablesForAdvisories;

    /**
     * What mapper to use when generating the header row for the overview tables.<br>
     * Currently supported are:
     * <ul>
     *     <li><code>default</code> from {@link VulnerabilityReportAdapter.StatisticsOverviewTable#VULNERABILITY_STATUS_MAPPER_DEFAULT}</li>
     *     <li><code>abstracted</code> from {@link VulnerabilityReportAdapter.StatisticsOverviewTable#VULNERABILITY_STATUS_MAPPER_ABSTRACTED}</li>
     * </ul>
     *
     * @parameter
     */
    private String overviewTablesVulnerabilityStatusMappingFunction;

    /**
     * @parameter default-value="en"
     */
    private String templateLanguageSelector;

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

    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = new InventoryReport();
        configureInventoryReport(report);
        return report;
    }

    protected void configureInventoryReport(InventoryReport report) {
        report.setProjectName(projectName);

        // report control
        report.setFailOnDevelopment(failOnDevelopment);
        report.setFailOnError(failOnError);
        report.setFailOnBanned(failOnBanned);
        report.setFailOnDowngrade(failOnDowngrade);
        report.setFailOnInternal(failOnInternal);
        report.setFailOnUnknown(failOnUnknown);
        report.setFailOnUnknownVersion(failOnUnknownVersion);
        report.setFailOnUpgrade(failOnUpgrade);
        report.setFailOnMissingLicense(failOnMissingLicense);
        report.setFailOnMissingLicenseFile(failOnMissingLicenseFile);
        report.setFailOnMissingComponentFiles(failOnMissingComponentFiles);
        report.setFailOnMissingNotice(failOnMissingNotice);

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
        report.setVulnerabilityScoreThreshold(Float.parseFloat(vulnerabilityScoreThreshold));
        report.addVulnerabilityAdvisoryFilter(vulnerabilityAdvisoryFilter);
        report.addGenerateOverviewTablesForAdvisories(generateOverviewTablesForAdvisories);
        if (overviewTablesVulnerabilityStatusMappingFunction != null) {
            report.setOverviewTablesVulnerabilityStatusMappingFunction(overviewTablesVulnerabilityStatusMappingFunction);
        }

        // diff settings
        report.setDiffInventoryFile(diffInventoryFile);

        report.setAssetBomReportEnabled(enableAssetReport);
        report.setInventoryPomEnabled(enablePomReport);
        report.setInventoryDiffReportEnabled(enableDiffReport);
        report.setInventoryBomReportEnabled(enableBomReport);
        report.setInventoryVulnerabilityReportEnabled(enableVulnerabilityReport);
        report.setInventoryVulnerabilityStatisticsReportEnabled(enableVulnerabilityStatisticsReport);

        report.setTargetReportDir(targetReportDir);

        report.setRelativeLicensePath(relativeLicensePath);

        report.setAddOnArtifacts(addOnArtifacts);

        // enable to select language
        report.setTemplateLanguageSelector(templateLanguageSelector);

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
