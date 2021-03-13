/**
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
package org.metaeffekt.core.maven.api.compile.dependency;

import org.apache.maven.artifact.Artifact;

/**
 * POJO to specify APIViolations for the API Compile Mojo.
 * @author i001098
 */
public class APIViolation {
    
    private String groupId;
    private String artifactId;
    
    public APIViolation() {
        super();
    };

    public APIViolation(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * @param artifact to match against
     * @return true if groupId and artifactId of the given artifact are equal to groupId and artifactId of the APIViolation. 
     */
    public boolean matches(Artifact artifact) {
        return (artifact != null && 
                artifact.getGroupId() != null && 
                artifact.getArtifactId() != null && 
                groupId != null && 
                artifactId != null && 
                groupId.equals(artifact.getGroupId()) &&
                artifactId.equals(artifact.getArtifactId()));
    }
}
