/**
 * Copyright 2009-2018 the original author or authors.
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
     * @parameter
     * @required
     */
    private String sourceInventoryPath;

    /**
     * @parameter expression="${project.build.directory}/inventory/${project.artifactId}-${project.version}-inventory.xls"
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
    private String referenceInventoryPath;

    /**
     * @parameter default-value="false"
     */
    private boolean enableDita;
    
    /**
     * @parameter expression="${basedir}/src/main/dita/gen"
     */
    private String targetDitaPath;

    /**
     * @parameter expression="${project.artifactId}-artifact-report.dita"
     */
    private String targetDitaArtifactReportPath;

    /**
     * @parameter expression="${project.artifactId}-package-report.dita"
     */
    private String targetDitaPackageReportPath;

    /**
     * @parameter expression="${project.artifactId}-component-report.dita"
     */
    private String targetDitaComponentReportPath;

    /**
     * @parameter expression="${project.artifactId}-diff.dita"
     */
    private String targetDitaDiffPath;

    /**
     * @parameter expression="${project.artifactId}-licenses.dita"
     */
    private String targetDitaLicenseReportPath;

    /**
     * @parameter expression="${project.artifactId}-notices.dita"
     */
    private String targetDitaNoticeReportPath;
    
    /**
     * @parameter
     */
    private String licenseTargetPath;

    /**
     * @parameter
     */
    private String licenseSourcePath;
    
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
    
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = new InventoryReport();
        configureInventoryReport(report);
        return report;
    }

    protected void configureInventoryReport(InventoryReport report) {
        report.setFailOnDevelopment(isFailOnDevelopment());
        report.setFailOnError(isFailOnError());
        report.setFailOnBanned(isFailOnBanned());
        report.setFailOnDowngrade(isFailOnDowngrade());
        report.setFailOnInternal(isFailOnInternal());
        report.setFailOnUnknown(isFailOnUnknown());
        report.setFailOnUnknownVersion(isFailOnUnknownVersion());
        report.setFailOnUpgrade(isFailOnUpgrade());
        report.setFailOnMissingLicense(isFailOnMissingLicense());
        report.setFailOnMissingLicenseFile(isFailOnMissingLicenseFile());
        report.setFailOnMissingNotice(isFailOnMissingNotice());
        report.setGlobalInventoryPath(getSourceInventoryPath());
        report.setProjectName(getProjectName());
        report.setTargetInventoryPath(getTargetInventoryPath());
        
        report.setReferenceInventoryPath(referenceInventoryPath);
        
        if (enableDita) {
            report.setTargetDitaDiffPath(createDitaPath(targetDitaDiffPath));
            report.setTargetDitaReportPath(createDitaPath(targetDitaArtifactReportPath));
            report.setTargetDitaPackageReportPath(createDitaPath(targetDitaPackageReportPath));
            report.setTargetDitaComponentReportPath(createDitaPath(targetDitaComponentReportPath));
            report.setTargetDitaLicenseReportPath(createDitaPath(targetDitaLicenseReportPath));
            report.setTargetDitaNoticeReportPath(createDitaPath(targetDitaNoticeReportPath));
        }
        
        report.setLicenseSourcePath(licenseSourcePath);
        report.setLicenseTargetPath(licenseTargetPath);
        report.setRelativeLicensePath(relativeLicensePath);
        
        report.setAddOnArtifacts(addOnArtifacts);

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

    private String createDitaPath(String relativePath) {
        return new File(targetDitaPath, relativePath).getPath();
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
            
            InventoryReport report = initializeInventoryReport();
            boolean success = false;
            try {
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
    
    public String getReferenceInventoryPath() {
        return referenceInventoryPath;
    }

    public void setReferenceInventoryPath(String referenceInventoryPath) {
        this.referenceInventoryPath = referenceInventoryPath;
    }

    public String getTargetDitaArtifactReportPath() {
        return targetDitaArtifactReportPath;
    }

    public void setTargetDitaArtifactReportPath(String targetDitaReportPath) {
        this.targetDitaArtifactReportPath = targetDitaReportPath;
    }

    public String getTargetDitaDiffPath() {
        return targetDitaDiffPath;
    }

    public void setTargetDitaDiffPath(String targetDitaDiffPath) {
        this.targetDitaDiffPath = targetDitaDiffPath;
    }

    
    public String getTargetDitaLicenseReportPath() {
        return targetDitaLicenseReportPath;
    }
    
    public void setTargetDitaLicenseReportPath(String targetDitaLicenseReportPath) {
        this.targetDitaLicenseReportPath = targetDitaLicenseReportPath;
    }
    
    public String getTargetDitaNoticeReportPath() {
        return targetDitaNoticeReportPath;
    }
    
    public void setTargetDitaNoticeReportPath(String targetDitaObligationReportPath) {
        this.targetDitaNoticeReportPath = targetDitaObligationReportPath;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public String getSourceInventoryPath() {
        return sourceInventoryPath;
    }

    public void setSourceInventoryPath(String sourceInventoryPath) {
        this.sourceInventoryPath = sourceInventoryPath;
    }

    public String getTargetInventoryPath() {
        return targetInventoryPath;
    }

    public void setTargetInventoryPath(String targetInventoryPath) {
        this.targetInventoryPath = targetInventoryPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isFailOnBanned() {
        return failOnBanned;
    }

    public void setFailOnBanned(boolean failOnBanned) {
        this.failOnBanned = failOnBanned;
    }

    public boolean isFailOnUnknown() {
        return failOnUnknown;
    }

    public void setFailOnUnknown(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }
    
    public boolean isFailOnUnknownVersion() {
        return failOnUnknownVersion;
    }

    public void setFailOnUnknownVersion(boolean failOnUnknownVersion) {
        this.failOnUnknownVersion = failOnUnknownVersion;
    }

    public boolean isFailOnDevelopment() {
        return failOnDevelopment;
    }

    public void setFailOnDevelopment(boolean failOnDevelopment) {
        this.failOnDevelopment = failOnDevelopment;
    }

    public boolean isFailOnDowngrade() {
        return failOnDowngrade;
    }

    public void setFailOnDowngrade(boolean failOnDowngrade) {
        this.failOnDowngrade = failOnDowngrade;
    }

    public boolean isFailOnInternal() {
        return failOnInternal;
    }

    public void setFailOnInternal(boolean failOnInternal) {
        this.failOnInternal = failOnInternal;
    }

    public boolean isFailOnUpgrade() {
        return failOnUpgrade;
    }

    public void setFailOnUpgrade(boolean failOnUpgrade) {
        this.failOnUpgrade = failOnUpgrade;
    }

    public boolean isFailOnMissingLicense() {
        return failOnMissingLicense;
    }

    public void setFailOnMissingLicense(boolean failOnMissingLicense) {
        this.failOnMissingLicense = failOnMissingLicense;
    }

    public String getArtifactExcludes() {
        return artifactExcludes;
    }

    public void setArtifactExcludes(String artifactFilterIncludes) {
        this.artifactExcludes = artifactFilterIncludes;
    }

    public String getArtifactIncludes() {
        return artifactIncludes;
    }

    public void setArtifactIncludes(String artifactFilterExcludes) {
        this.artifactIncludes = artifactFilterExcludes;
    }
    
    public boolean isFailOnMissingLicenseFile() {
        return failOnMissingLicenseFile;
    }
    
    public void setFailOnMissingLicenseFile(boolean failOnMissingLicenseFile) {
        this.failOnMissingLicenseFile = failOnMissingLicenseFile;
    }
    
    public boolean isFailOnMissingNotice() {
        return failOnMissingNotice;
    }

    public void setFailOnMissingNotice(boolean failOnMissingNotice) {
        this.failOnMissingNotice = failOnMissingNotice;
    }

}
