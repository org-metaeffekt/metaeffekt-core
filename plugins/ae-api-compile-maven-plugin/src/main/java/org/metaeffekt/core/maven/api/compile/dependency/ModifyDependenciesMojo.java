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
package org.metaeffekt.core.maven.api.compile.dependency;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This goal handles the resolution of custom ehf scopes to actual artifacts.
 */
@Mojo(name="expand-dependencies", defaultPhase=LifecyclePhase.INITIALIZE )
public class ModifyDependenciesMojo extends AbstractMojo {

    /**
     * The Maven project this goal is called for.
     */
    @Parameter(defaultValue="${project}", required=true, readonly=true)
    private MavenProject project;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException, CompilationFailureException {

        getLog().info("Analyzing depdendencies:");

        List<Dependency> list = project.getDependencies();
        
        List<Dependency> obsoleteDependencies = new ArrayList<Dependency>();
        List<Dependency> newDependencies = new ArrayList<Dependency>();
        
        for (Dependency dependency : list) {
            String scope = dependency.getScope();
            if (scope != null) {
                if (scope.startsWith("ehf")) {
                    getLog().info("- detected eHF scoped artifact:" + dependency);

                    // the original file is deleted from the dependencies 
                    obsoleteDependencies.add(dependency);

                    if (scope.contains("runtime")) {
                        newDependencies.add(createDerivedDependency(dependency, "runtime", "runtime"));
                    }
                    
                    if (scope.contains("config")) {
                        newDependencies.add(createDerivedDependency(dependency, "compile", "config"));
                    }

                    if (scope.contains("api")) {
                        newDependencies.add(createDerivedDependency(dependency, "compile", "api"));
                    }

                    if (scope.contains("test")) {
                        newDependencies.add(createDerivedDependency(dependency, "test", "tests"));
                    }

                    if (scope.contains("bootstrap")) {
                        newDependencies.add(createDerivedDependency(dependency, "runtime", "bootstrap"));
                    }

                    if (scope.contains("doc")) {
                        newDependencies.add(createDerivedDependency(dependency, "compile", "doc"));
                    }
                    
                    // FIXME: how to handle transitivity
                }
                
                // FIXME: here we could re-scope runtime artifacts making the compile plugin obsolete
            }
        }

        getLog().info("- removing orginal jars from the dependency list: " + obsoleteDependencies);
        list.removeAll(obsoleteDependencies);
        
        getLog().info("- adding dependencies: " + newDependencies);
        list.addAll(newDependencies);
    }

    private Dependency createDerivedDependency(Dependency dependency, String artifactScope, String artifactClassifier) {
        Dependency derivedDependency = new Dependency();
        derivedDependency.setGroupId(dependency.getGroupId());
        derivedDependency.setArtifactId(dependency.getArtifactId());
        derivedDependency.setExclusions(dependency.getExclusions());
        derivedDependency.setOptional(true);
        derivedDependency.setScope(artifactScope);
        derivedDependency.setType(dependency.getType());
        derivedDependency.setVersion(dependency.getVersion());
        derivedDependency.setClassifier(artifactClassifier);
        return derivedDependency;
    }

}
