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
package org.metaeffekt.core.maven.artifact.publisher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Prepares the artifact creation by copying selected resources to a dedicated
 * folder structure.
 */
@Mojo(name = "prepare-artifact", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class PrepareArtifactMojo extends AbstractArtifactMojo {

    /**
     * The list of fileSets to include in the API jar.
     */
    @Parameter
    private List<Fileset> filesets;

    /**
     * The content of this properties file will be used to do the replacements.
     */
    @Parameter(defaultValue = "${propertiesFile}")
    private File propertiesFile;

    /**
     * The exclude pattern for token replacement.
     */
    @Parameter(defaultValue = "${tokenReplaceExcludePattern}")
    private String tokenReplaceExcludePattern;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        File artifactFile = getArtifactFile(getClassifier());

        File tempDir = getTempDir(artifactFile);

        Project project = new Project();
        project.setBaseDir(new File(getProject().getBuild().getDirectory()));

        // ensure the temp structure is clean
        Delete delete = new Delete();
        delete.setProject(project);
        delete.setDir(tempDir);
        delete.setIncludes("**/*");
        delete.execute();

        if (!filesets.isEmpty()) {
            Iterator<Fileset> iterFiles = filesets.iterator();
            while (iterFiles.hasNext()) {
                Fileset fs = iterFiles.next();

                org.apache.tools.ant.types.FileSet antFileSet = new org.apache.tools.ant.types.FileSet();
                antFileSet.setProject(project);
                antFileSet.setDir(new File(fs.getDirectory()));
                antFileSet.appendIncludes(convert(fs.getIncludes()));
                antFileSet.appendExcludes(convert(fs.getExcludes()));

                if (antFileSet.getDir().exists()) {
                    Copy copyTask = new Copy();
                    copyTask.setProject(project);
                    copyTask.addFileset(antFileSet);

                    if (fs.getOutputDirectory() != null) {
                        copyTask.setTodir(new File(tempDir, fs.getOutputDirectory()));
                    } else {
                        copyTask.setTodir(tempDir);
                    }

                    copyTask.execute();
                }
            }
        }
    }

    private String[] convert(List<String> strings) {
        if (strings == null) {
            return null;
        }
        return strings.toArray(new String[strings.size()]);
    }
}
