/**
 * Copyright 2009-2017 the original author or authors.
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
package org.metaeffekt.core.maven.artifact.publisher;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;

/**
 * Prepares the artifact creation by copying selected resources to a dedicated
 * folder structure.
 * 
 * @goal publish-artifact-overwrite
 * @phase prepare-package
 */
public class PublishArtifactOverwriteMojo extends AbstractArtifactMojo {

    /**
     * The qualifier of the resource to be copied from.
     * @parameter
     */
    private String sourceQualifier = null;

    /**
     * The qualifier of the target resource.
     * @parameter
     */
    private String targetQualifier = null;

    /**
     * The classifier of the resource to be copied from.
     * @parameter
     */
    private String sourceClassifier = null;

    /**
     * The classifier of the target resource.
     * @parameter
     */
    private String targetClassifier = null;
    
    /**
     * Determines if the created artifact should be uploaded into the maven repository
     * @parameter
     */
    private boolean attachArtifact = false;
    
    /**
     * The groupId to be used for the created artifact.
     * @parameter
     */
    private String alternateGroupId = null;
    
    /**
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException 
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        File srcArtifactFile = getArtifactFile(getSourceClassifier(), getSourceQualifier());
        // verify that the artifact exisits
        if(!srcArtifactFile.exists()){
            getLog().info("No source artifact found for file: "+srcArtifactFile.getName());
            // do nothing, just skip it here.
            return;
        }
        File targetArtifactFile = getArtifactFile(getClassifier(), getQualifier());
        
        Project project = new Project();
        project.setBaseDir(new File(getProject().getBuild().getDirectory()));
        
        if (srcArtifactFile.exists()) {
            Copy copy = new Copy();
            copy.setProject(project);
            copy.setOverwrite(true);
            copy.setFile(srcArtifactFile);
            copy.setTofile(targetArtifactFile);
            copy.execute();
            
            // please note that we assume the artifact is already attached
        }
        
        // should it be uploaded into the maven repository?
        if(attachArtifact){
            attachArtifact(targetArtifactFile, alternateGroupId);
        }
        
    }

    @Override
    public String getClassifier() {
        if(getTargetClassifier() == null){
            return super.getClassifier(); 
        } else {
            return getTargetClassifier();
        }
    }

    @Override
    public String getQualifier() {
        if(getTargetQualifier() == null){
            return super.getQualifier();
        } else {
            return getTargetQualifier();
        }
        
    }

    public String getSourceQualifier() {
        return sourceQualifier;
    }

    public void setSourceQualifier(String sourceQualifier) {
        this.sourceQualifier = sourceQualifier;
    }

    public String getSourceClassifier() {
        return sourceClassifier;
    }

    public void setSourceClassifier(String sourceClassifier) {
        this.sourceClassifier = sourceClassifier;
    }
    
    public String getTargetClassifier() {
        return targetClassifier;
    }

    public void setTargetClassifier(String targetClassifier) {
        this.targetClassifier = targetClassifier;
    }

    public String getTargetQualifier() {
        return targetQualifier;
    }

    public void setTargetQualifier(String targetQualifier) {
        this.targetQualifier = targetQualifier;
    }
    
}
