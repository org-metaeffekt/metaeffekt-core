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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;

/**
 * Creates a report for the local test repository.
 * 
 * @goal create-repository-report
 */
public class RepositoryReportCreationMojo extends AbstractInventoryReportCreationMojo {

    /**
     * @parameter
     */
    private String repositoryIncludes = "**/*";

    /**
     * @parameter
     */
    private String repositoryExcludes = 
        "**/*.pom, **/*.sha1, **/*.xml, " +
        "**/*.repositories, **/*.jar-not-available, **/*.md5";

    /**
     * @parameter
     * @required
     */
    private String repositoryPath;

    @Override
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {
        InventoryReport report = super.initializeInventoryReport();
        report.setRepositoryIncludes(
            ParameterConversionUtil.convertStringToStringArray(repositoryIncludes, ","));
        report.setRepositoryExcludes(
            ParameterConversionUtil.convertStringToStringArray(repositoryExcludes, ","));

        // supply the repository path as source for the artifacts
        report.setRepositoryPath(repositoryPath);

        return report;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getRepositoryIncludes() {
        return repositoryIncludes;
    }

    public void setRepositoryIncludes(String localRepositoryIncludes) {
        this.repositoryIncludes = localRepositoryIncludes;
    }

    public String getRepositoryExcludes() {
        return repositoryExcludes;
    }

    public void setRepositoryExcludes(String localRepositoryExcludes) {
        this.repositoryExcludes = localRepositoryExcludes;
    }

}
