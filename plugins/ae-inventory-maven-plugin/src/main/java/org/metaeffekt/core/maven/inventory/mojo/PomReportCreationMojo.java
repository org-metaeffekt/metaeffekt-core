/**
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

/**
 * Creates a report for the dependencies listed in the pom.
 * 
 * @goal create-pom-report
 * @requiresDependencyResolution test
 */
public class PomReportCreationMojo extends AbstractInventoryReportCreationMojo {

    private static final String DEPENDENCY_TYPE_JAR = "jar";
    private static final String DEPENDENCY_TYPE_MAVEN_PLUGIN = "maven-plugin";

    private static final String SCOPE_PROVIDED = "provided";
    private static final String SCOPE_SYSTEM = "system";
    private static final String SCOPE_TEST = "test";
    
    /**
     * @parameter default-value="false"
     */
    private boolean includeScopeProvided;

    /**
     * @parameter default-value="false"
     */
    private boolean includeScopeSystem;

    /**
     * @parameter default-value="false"
     */
    private boolean includeScopeTest;
    
    /**
     * @parameter default-value="true"
     */
    private boolean includeOptional;

    /**
     * @parameter default-value="false"
     */
    private boolean includePlugins;

    /**
     * @parameter default-value="false"
     */
    private boolean manageScopeProvided;

    /**
     * @parameter default-value="false"
     */
    private boolean manageScopeSystem;

    /**
     * @parameter default-value="false"
     */
    private boolean manageScopeTest;
    
    /**
     * @parameter default-value="true"
     */
    private boolean manageOptional;
    
    /**
     * @parameter default-value="false"
     */
    private boolean managePlugins;

    /**
     * @parameter default-value="false"
     */
    private boolean skipPomPackagingProjectExecution;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
        if (skipExecution()) {
            return;
        }

        InventoryReport report = initializeInventoryReport();
        Inventory inventory = createInventoryFromPom();
        report.setInventory(inventory);

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

    private boolean skipExecution() {
        if (isPomPackagingProject()) {
            // NOTE: usually skipping the execute for a POM build is sensible. However
            //  we would already like to detect harmful artifacts in the scope
            //  a pom packaging project.
            if (skipPomPackagingProjectExecution) {
                return true;
            }
        }
        return false;
    }

    private Inventory createInventoryFromPom() {
        Inventory inventory = new Inventory();
        for (Object obj : getProject().getTestArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getDependencyArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getCompileArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getRuntimeArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        
        // plugins are treated a little differently (type is lost)
        for (Object obj : getProject().getPluginArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, includePlugins, managePlugins);
        }
        
        inventory.mergeDuplicates();
        
        return inventory;
    }

    protected void addArtifactIfNecessary(Inventory inventory,
                                          org.apache.maven.artifact.Artifact mavenArtifact, Boolean relevant, Boolean managed) {
        Artifact artifact = createInventoryArtifact(mavenArtifact);
        
        inventory.getArtifacts().add(artifact);

        // modulate relevant flag
        if (relevant != null) {
            artifact.setRelevant(relevant);
        } else {
            boolean localRelevant = true;
            if (SCOPE_PROVIDED.equals(mavenArtifact.getScope())) {
                if (!includeScopeProvided) {
                    localRelevant = false;
                }
            }
            if (SCOPE_SYSTEM.equals(mavenArtifact.getScope())) {
                if (!includeScopeSystem) {
                    localRelevant = false;
                }
            }
            if (SCOPE_TEST.equals(mavenArtifact.getScope())) {
                if (!includeScopeTest) {
                    localRelevant = false;
                }
            }
            if (mavenArtifact.isOptional()) {
                if (!includeOptional) {
                    localRelevant = false;
                }
            }
            artifact.setRelevant(localRelevant);
        }

        // modulate managed flag
        if (managed != null) {
            artifact.setManaged(managed);
        } else {
            boolean localManaged = true;
            if (SCOPE_PROVIDED.equals(mavenArtifact.getScope())) {
                if (!manageScopeProvided) {
                    localManaged = false;
                }
            }
            if (SCOPE_SYSTEM.equals(mavenArtifact.getScope())) {
                if (!manageScopeSystem) {
                    localManaged = false;
                }
            }
            if (SCOPE_TEST.equals(mavenArtifact.getScope())) {
                if (!manageScopeTest) {
                    localManaged = false;
                }
            }
            if (mavenArtifact.isOptional()) {
                if (!manageOptional) {
                    localManaged = false;
                }
            }
            artifact.setManaged(localManaged);
        }
    }
    
    protected Artifact createInventoryArtifact(org.apache.maven.artifact.Artifact mavenArtifact) {
        Artifact artifact = new Artifact();
        artifact.setArtifactId(mavenArtifact.getArtifactId());
        artifact.setGroupId(mavenArtifact.getGroupId());
        artifact.setVersion(mavenArtifact.getVersion());

        StringBuffer sb = new StringBuffer();
        sb.append(mavenArtifact.getArtifactId());
        sb.append('-');
        sb.append(mavenArtifact.getVersion());
        if (mavenArtifact.getClassifier() != null) {
            sb.append('-');
            sb.append(mavenArtifact.getClassifier());
        }
        sb.append('.');
        String type = mavenArtifact.getType();
        if (DEPENDENCY_TYPE_MAVEN_PLUGIN.equals(type)) {
            type = DEPENDENCY_TYPE_JAR;
        }
        sb.append(type);
        artifact.setId(sb.toString());
        return artifact;
    }

    public void setIncludeScopeProvided(boolean includeScopeProvided) {
        this.includeScopeProvided = includeScopeProvided;
    }

    public void setIncludeScopeSystem(boolean includeScopeSystem) {
        this.includeScopeSystem = includeScopeSystem;
    }

}
