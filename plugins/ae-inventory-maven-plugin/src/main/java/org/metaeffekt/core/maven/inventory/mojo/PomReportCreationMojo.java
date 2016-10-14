/**
 * Copyright 2009-2016 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

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
    
    private static final Set<String> processedPoms = new HashSet<String>();
    
    private static File multiProjectInventoryFile;
    
    private static Inventory multiProjectInventory;
    
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
    
    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession mavenSession;
    
    // NOTE: we use a shutdown hook to write the last version of the multi project inventory to
    // disc. This is necessary to prevent that the reactor build cleans the root target
    // folder after we have last stored the file. This approach is obviously not very nice. But
    // for the time being a better approach was not obvious (maven was not playing nice, too).
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() { 
            public void run() { 
                try {
                    PomReportCreationMojo.writeMultiProjectInventory();
                } catch (IOException e) {
                    // noting we can do about it. Just provide the output without relying
                    // that resources like a log are still available.
                    e.printStackTrace();
                }
            } 
        });
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
        if (skipExecution()) {
            return;
        }
        initializeMultiProjectInventoryFileIfNecessary();
        
        InventoryReport report = initializeInventoryReport();
        Inventory inventory = createInventoryFromPom();
        report.setRepositoryInventory(inventory);

        boolean success = false;
        try {
            success = report.createReport();
            extendMultiProjectReport(report);
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

    private void extendMultiProjectReport(InventoryReport report) throws IOException {
        Inventory localReportInventory = report.getLastProjectInventory();
        for (org.metaeffekt.core.inventory.processor.model.Artifact artifact : localReportInventory.getArtifacts()) {
            artifact.getProjects().clear();
            artifact.addProject(getProjectName());
        }

        if (multiProjectInventoryFile.exists()) {
            // read inventory (may be dirty from other process)
            try {
                multiProjectInventory = new GlobalInventoryReader().readInventory(multiProjectInventoryFile);
            } catch (IOException e) {
                getLog().warn("Cannot read global inventory from " + multiProjectInventoryFile + ".");
            }
        } else{
            if (multiProjectInventory == null) {
                multiProjectInventory = new Inventory();
            } else {
                // we continue to use the static instance
            }
        }
        
        // merge content
        multiProjectInventory.getArtifacts().addAll(localReportInventory.getArtifacts());
        multiProjectInventory.mergeDuplicates();
        
        // we write the file every time; this way the content is complete in the sense
        // of the build
        writeMultiProjectInventory();
    }

    protected static void writeMultiProjectInventory() throws IOException {
        if (multiProjectInventory != null && multiProjectInventoryFile != null) {
            // ensure that the target folder exists (this may not be the case due to the build 
            // sequence and goals invoked)
            if (multiProjectInventoryFile.getParentFile() != null) {
                multiProjectInventoryFile.getParentFile().mkdirs();
            }
            new InventoryWriter().writeInventory(multiProjectInventory, multiProjectInventoryFile);
            
            // check whether reading is possible
            try {
                new GlobalInventoryReader().readInventory(multiProjectInventoryFile);
            } catch (IOException e) {
                System.err.println("Inventory file corrupted after save. Please analyze and report this"
                    + " error.");
                multiProjectInventoryFile.deleteOnExit();
            }
        }
    }
    
    private void initializeMultiProjectInventoryFileIfNecessary() {
        if (multiProjectInventoryFile == null && mavenSession != null) {
            // find execution project (independent from parent hierarchy)
            List<MavenProject> projects = mavenSession.getSortedProjects();
            MavenProject executionRootMavenProject = mavenSession.getCurrentProject();
            for (MavenProject mavenProject : projects) {
                if (mavenProject.isExecutionRoot()) {
                    executionRootMavenProject = mavenProject;
                    break;
                }
            }
            
            // infer relative filename from targetInventoryPath (as this may be plugin configured)
            File file = new File(getTargetInventoryPath());
            File targetFolder = new File(getProject().getBuild().getDirectory());
            String path = file.getAbsolutePath();
            path = path.replace(targetFolder.getAbsolutePath(), "");
            
            // construct file path for complete inventory
            File projectRootBuildDir = new File(executionRootMavenProject.getBuild().getDirectory());
            File completeInventoryFolder = new File(projectRootBuildDir, path).getParentFile();
            multiProjectInventoryFile = new File(completeInventoryFolder, 
                executionRootMavenProject.getArtifactId() + "-" + 
                executionRootMavenProject.getVersion() + "-" + 
                "complete-inventory.xls");
            
            getLog().info("Global inventory report will be written to " + multiProjectInventoryFile);
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
        
        String key = getProject().getModel().getId();
        if (processedPoms.contains(key)) {
            return true;
        }
        processedPoms.add(key);
        return false;
    }

    private Inventory createInventoryFromPom() {
        Inventory inventory = new Inventory();
        for (Object obj : getProject().getTestArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getDependencyArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getCompileArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getRuntimeArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        for (Object obj : getProject().getArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null, null);
        }
        
        // plugins are treated a little differently (type is lost)
        for (Object obj : getProject().getPluginArtifacts()) {
            Artifact mavenArtifact = (Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, includePlugins, managePlugins);
        }
        
        inventory.mergeDuplicates();
        
        return inventory;
    }

    protected void addArtifactIfNecessary(Inventory inventory, Artifact mavenArtifact, Boolean relevant, Boolean managed) {
        DefaultArtifact artifact = createInventoryArtifact(mavenArtifact);
        
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
    
    protected DefaultArtifact createInventoryArtifact(Artifact mavenArtifact) {
        DefaultArtifact artifact = new DefaultArtifact();
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
