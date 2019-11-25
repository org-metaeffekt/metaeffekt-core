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
package org.metaeffekt.core.maven.jira.mojo;


import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;

/**
 * Abstract JIRA access mojo providing some necessary data like the JIRA URL, user and password.
 */
public abstract class AbstractJiraMojo extends AbstractProjectAwareMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue="${project}", required=true, readonly=true)
    private MavenProject mavenProject;

    /**
     * The base URL of the Jira instance, the issues are retrieved from.
     */
    @Parameter(required=true)
    protected String serverUrl;

    /**
     * User for authentication with Jira.
     */
    @Parameter(required=true)
    protected String userName;

    /**
     * Password for authentication with Jira.
     */
    @Parameter(required=true)
    protected String userPassword;

    @Override
    public MavenProject getProject() {
        return this.mavenProject;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

}
