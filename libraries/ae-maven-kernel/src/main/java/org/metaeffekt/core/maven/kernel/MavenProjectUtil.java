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

import org.apache.maven.project.MavenProject;

import static org.metaeffekt.core.maven.kernel.MavenConstants.MAVEN_PACKAGING_JAR;
import static org.metaeffekt.core.maven.kernel.MavenConstants.MAVEN_PACKAGING_POM;

/**
 * Utility for {@link MavenProject}.
 */
public final class MavenProjectUtil {

    private MavenProjectUtil() {
    }

    /**
     * Checks if the given project is packaging type 'pom'.
     * @param project project to check
     * @return {@code true} if type is 'pom'
     */
    public static boolean isPomPackagingProject(MavenProject project) {
        validateProject(project);
        return MAVEN_PACKAGING_POM.equalsIgnoreCase(project.getPackaging());
    }

    /**
     * Checks if the given project is packaging type 'jar'.
     * @param project project to check
     * @return {@code true} if type is 'jar'
     */
    public static boolean isJarPackagingProject(MavenProject project) {
        validateProject(project);
        return MAVEN_PACKAGING_JAR.equalsIgnoreCase(project.getPackaging());
    }

    private static void validateProject(MavenProject project) {
        if (project == null) {
            throw new IllegalStateException("Unexpected reference to a null maven project.");
        }
    }


}
