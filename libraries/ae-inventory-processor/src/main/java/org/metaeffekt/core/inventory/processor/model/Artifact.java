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
package org.metaeffekt.core.inventory.processor.model;

import java.util.Collection;
import java.util.Set;

// FIXME: clarify usage. If not used elsewhere let us get rid of the interface
public interface Artifact {

    Set<String> getProjects();

    void setProjects(Set<String> projects);

    Set<Artifact> getDependencies();

    void setDependencies(Set<Artifact> dependencies);

    void addDependency(Artifact dependency);

    void addDependencies(Collection<Artifact> dependencies);

    void removeDependency(Artifact dependency);

    void removeDependencies(Collection<Artifact> dependencies);

    String getName();

    void setName(String name);

    String getGroupId();

    void setGroupId(String grpoupId);

    String getId();

    void setId(String id);

    String getVersion();

    void setVersion(String version);

    String getLicense();

    void setLicense(String license);

    String getUrl();

    void setUrl(String url);

    boolean isSecurityRelevant();

    void setSecurityRelevant(boolean securityRelevant);

    String getSecurityCategory();

    void setSecurityCategory(String securityCategory);

    boolean isReported();

    void setReported(boolean reported);

    boolean isUsed();

    void setUsed(boolean used);

    String getClassification();

    void setClassification(String classification);

    boolean isVersionReported();

    void setVersionReported(boolean versionReported);

    String getArtifactId();

    void setArtifactId(String repositoryArtifactId);

    void addProject(String project);

    void addProjects(Collection<String> projects);

    String extractVersionFromId();

    void merge(Artifact a);

    void deriveArtifactId();

    String getComment();

    void setComment(String comment);

    String getLatestAvailableVersion();

    void setLatestAvailableVersion(String latestAvailableVersion);

    String getPreviousVersion();

    void setPreviousVersion(String previousVersion);

    String createStringRepresentation();

    String createCompareStringRepresentation();

    String getType();

    String getClassifier();

    /**
     * Provides information whether this artifact is relevant for reporting.
     *
     * @return
     */
    boolean isRelevant();

    /**
     * Provides information whether this artifact is managed (ie. fails the build if meta data und 
     * usage context conflict)
     *
     * @return
     */
    boolean isManaged();

}