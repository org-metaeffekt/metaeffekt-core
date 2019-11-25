/**
 * Copyright 2009-2019 the original author or authors.
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
package org.metaeffekt.core.maven.version.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.metaeffekt.core.common.kernel.util.ParameterConversionUtil;
import org.metaeffekt.core.maven.kernel.log.MavenLogAdapter;

import java.io.File;
import java.util.Map;

/**
 * Updates the versions in a maven project and all included POMs.
 * 
 * @goal update
 */
public class UpdateVersionMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * @parameter default-value="${project.basedir}"
     * @required
     */
    private File projectPath;

    /**
     * @parameter default-value="${ae.version.update.includes}"
     * @required
     */
    private String includes;

    /**
     * @parameter default-value="${ae.version.update.excludes}"
     */
    private String excludes;

    /**
     * @parameter default-value="${ae.version.update.project.version}"
     * @required
     */
    private String projectVersion;

    /**
     * @parameter
     * @required
     */
    private Map<String, String> propertyVersionMap;

    /**
     * @parameter
     * @required
     */
    private Map<String, String> groupIdVersionMap;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // adapt maven logging to underlying logging facade
        MavenLogAdapter.initialize(getLog());
        try {
            UpdateVersionTask task = new UpdateVersionTask();
            
            task.setProjectPath(projectPath);
            task.setIncludes(ParameterConversionUtil.convertStringToStringArray(includes, ","));
            task.setExcludes(ParameterConversionUtil.convertStringToStringArray(excludes, ","));

            task.setProjectVersion(projectVersion);
            task.setGroupIdVersionMap(groupIdVersionMap);
            task.setPropertyVersionMap(propertyVersionMap);

            task.updateVersions();
        } finally {
            MavenLogAdapter.release();
        }
    }

}
