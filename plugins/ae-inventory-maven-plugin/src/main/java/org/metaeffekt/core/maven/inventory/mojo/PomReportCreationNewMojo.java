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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.io.IOException;

/**
 * Creates a report for the dependencies listed in the pom.
 *
 */
@Mojo(name = "create-pom-inventory", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class PomReportCreationNewMojo extends AbstractProjectAwareConfiguredMojo {

    private static final String DEPENDENCY_TYPE_JAR = "jar";
    private static final String DEPENDENCY_TYPE_MAVEN_PLUGIN = "maven-plugin";

    private static final String SCOPE_PROVIDED = "provided";
    private static final String SCOPE_SYSTEM = "system";
    private static final String SCOPE_TEST = "test";

    @Parameter(property = "ae.create.pom.inventory.includeScopeProvided", defaultValue = "false")
    private boolean includeScopeProvided;

    @Parameter(property = "ae.create.pom.inventory.includeScopeSystem", defaultValue = "true")
    private boolean includeScopeSystem;

    @Parameter(property = "ae.create.pom.inventory.includeScopeTest", defaultValue = "false")
    private boolean includeScopeTest;

    @Parameter(property = "ae.create.pom.inventory.includeOptional", defaultValue = "false")
    private boolean includeOptional;

    @Parameter(property = "ae.create.pom.inventory.includePlugins", defaultValue = "false")
    private boolean includePlugins;

    @Parameter(property = "ae.create.pom.inventory.targetInventory", defaultValue = "false")
    private File targetInventoryFile;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
            Inventory inventory = createInventoryFromPom();

            new InventoryWriter().writeInventory(inventory, targetInventoryFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write inventory.", e);
        } finally {
            MavenLogAdapter.release();
        }
    }

    private Inventory createInventoryFromPom() {
        Inventory inventory = new Inventory();
        for (Object obj : getProject().getTestArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null);
        }
        for (Object obj : getProject().getDependencyArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null);
        }
        for (Object obj : getProject().getCompileArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null);
        }
        for (Object obj : getProject().getRuntimeArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null);
        }
        for (Object obj : getProject().getArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, null);
        }

        // plugins are treated a little differently (type is lost)
        for (Object obj : getProject().getPluginArtifacts()) {
            org.apache.maven.artifact.Artifact mavenArtifact = (org.apache.maven.artifact.Artifact) obj;
            addArtifactIfNecessary(inventory, mavenArtifact, includePlugins);
        }

        inventory.mergeDuplicates();

        return inventory;
    }

    protected void addArtifactIfNecessary(Inventory inventory,
                                          org.apache.maven.artifact.Artifact mavenArtifact, Boolean relevant) {
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

}
