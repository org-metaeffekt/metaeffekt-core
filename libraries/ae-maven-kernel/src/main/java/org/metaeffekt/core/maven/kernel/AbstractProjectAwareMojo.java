/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.maven.kernel;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * Abstract mojo adding project awareness and some convenience methods. Unfortunately maven forces
 * us into a strange situation. The plugin compile mechanism requires the javadoc annotations.
 * Therefore the project member variable including all javadoc metadata need to be provided by
 * the implementor in the plugin project. Otherwise maven will not be able to detect the
 * configuration ultimately not injecting any such member.
 *
 * @author Karsten Klein
 */
public abstract class AbstractProjectAwareMojo extends AbstractMojo {

    /**
     * Get the current project in which context the mojo is executed.
     * Implementation should look like
     * <pre>
     * {@code
     * @Getter
     * @Parameter(defaultValue = "${project}", required = true, readonly = true)
     * private MavenProject project;
     * }
     * </pre>
     *
     * @return current maven project
     */
    public abstract MavenProject getProject();

    /**
     * Delegates to {@link MavenProjectUtil#isPomPackagingProject(MavenProject)}.
     * @return {@code} true if packaging is 'pom'.
     */
    protected boolean isPomPackagingProject() {
        return MavenProjectUtil.isPomPackagingProject(getProject());
    }

    /**
     * Delegates to {@link MavenProjectUtil#isJarPackagingProject(MavenProject)}.
     *
     * @return {@code} true if packaging is 'jar'.
     */
    protected boolean isJarPackagingProject() {
        return MavenProjectUtil.isJarPackagingProject(getProject());
    }

}
