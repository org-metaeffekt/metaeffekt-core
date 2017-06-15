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
package org.metaeffekt.core.inventory.processor.model;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultArtifact implements Artifact {

    private static final String DELIMITER_DASH = "-";

    private static final char DELIMITER_DOT = '.';

    private static final char DELIMITER_COLON = ':';

    // name of the artifact (uncontrolled)
    private String name;

    // id of the artifact (file name)
    private String id;

    private String version;

    private String license;

    private String classification;

    // url of the project home page
    private String url;

    // indicates whether the artifact is security relevant and needs to be
    // upgraded asap
    private boolean securityRelevant;

    // if the artifact is security relevant is is classified into a security category
    private String securityCategory;

    // indicated whether the artifact was reported by protex
    private boolean reported;

    // indicated whether the artifact was reported by protex under the given version
    private boolean versionReported;

    // indicates that the artifact is is active use
    private boolean used;

    // list of project the aritfacts is used by (source: protex)
    private Set<String> projects = new LinkedHashSet<String>();

    private Set<Artifact> dependencies = new LinkedHashSet<Artifact>();

    // comments
    private String comment;

    // artifact id in repository
    private String artifactId;

    // group id in repository
    private String groupId;

    // latest available version
    private String latestAvailableVersion;

    // field required for diff tooling
    private String previousVersion;

    /**
     * Relevant means "reportRelevant" meaning that the artifact and its metadata needs to ne
     * included in the build.
     */
    private transient boolean relevant = true;

    /**
     * Managed means that the artifact may fail the build, when the meta data reflects issues
     */
    private transient boolean managed = true;

    public DefaultArtifact() {
    }

    /**
     * Copy constructor.
     *
     * @param artifact The artifact to copy from.
     */
    public DefaultArtifact(Artifact artifact) {
        this.id = artifact.getId();
        this.name = artifact.getName();
        this.version = artifact.getVersion();
        this.license = artifact.getLicense();
        this.url = artifact.getUrl();
        this.securityRelevant = artifact.isSecurityRelevant();
        this.securityCategory = artifact.getSecurityCategory();
        this.reported = artifact.isReported();
        this.used = artifact.isUsed();
        this.versionReported = artifact.isVersionReported();
        this.classification = artifact.getClassification();

        this.projects = new LinkedHashSet<String>(artifact.getProjects());

        this.comment = artifact.getComment();
        this.groupId = artifact.getGroupId();
        this.artifactId = artifact.getArtifactId();
        this.latestAvailableVersion = artifact.getLatestAvailableVersion();

        this.relevant = artifact.isRelevant();
        this.managed = artifact.isManaged();

        // FIXME:
        // - why are the dependencies not copied?
    }

    @Override
    public Set<String> getProjects() {
        return projects;
    }

    @Override
    public void setProjects(Set<String> project) {
        this.projects = project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        if (StringUtils.hasText(groupId)) {
            this.groupId = groupId;
        } else {
            groupId = null;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getLicense() {
        return license;
    }

    @Override
    public void setLicense(String license) {
        this.license = license;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean isSecurityRelevant() {
        return securityRelevant;
    }

    @Override
    public void setSecurityRelevant(boolean securityRelevant) {
        this.securityRelevant = securityRelevant;
    }

    @Override
    public String getSecurityCategory() {
        return securityCategory;
    }

    @Override
    public void setSecurityCategory(String securityCategory) {
        this.securityCategory = securityCategory;
    }

    @Override
    public boolean isReported() {
        return reported;
    }

    @Override
    public void setReported(boolean reported) {
        this.reported = reported;
    }

    @Override
    public boolean isUsed() {
        return used;
    }

    @Override
    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public String getClassification() {
        return classification;
    }

    @Override
    public void setClassification(String classification) {
        this.classification = classification;
    }

    @Override
    public boolean isVersionReported() {
        return versionReported;
    }

    @Override
    public void setVersionReported(boolean versionReported) {
        this.versionReported = versionReported;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getLatestAvailableVersion() {
        return latestAvailableVersion;
    }

    @Override
    public void setLatestAvailableVersion(String latestAvailableVersion) {
        this.latestAvailableVersion = latestAvailableVersion;
    }

    @Override
    public String getPreviousVersion() {
        return previousVersion;
    }

    @Override
    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    @Override
    public String toString() {
        return "Artifact id: " + id + ", name: " + name + ", version: " + version
                + " securityCatergory: " + securityCategory;
    }

    @Override
    public void addProject(String project) {
        projects.add(project);
    }

    @Override
    public void addProjects(Collection<String> projects) {
        if (projects != null) {
            this.projects.addAll(projects);
        }
    }

    @Override
    public String extractVersionFromId() {
        String version = getId();
        if (version != null) {
            if (version.indexOf('.') > 0) {
                version = version.substring(0, version.lastIndexOf('.'));
            }

            if (version.endsWith("-tests") ||
                    version.endsWith("-api") ||
                    version.endsWith("-config") ||
                    version.endsWith("-sources") ||
                    version.endsWith("-sources") ||
                    version.endsWith("-bootstrap") ||
                    version.endsWith("-mock") ||
                    version.endsWith("-doc") ||
                    version.endsWith("-runtime")) {
                version = version.substring(0, version.lastIndexOf('-'));
            }

            if (version.endsWith("-api") ||
                    version.endsWith("-runtime")) {
                version = version.substring(0, version.lastIndexOf('-'));
            }

            while (version.length() > 0 && version.substring(0, 1).matches("[a-zA-Z]")) {
                int index = version.indexOf('-');
                if (index > -1) {
                    version = version.substring(index + 1);
                } else
                    break;
            }

            int index = version.indexOf('/');
            if (index > -1) {
                version = version.substring(0, index);
            }
        }
        return version;
    }

    @Override
    public void merge(Artifact a) {

        if (!StringUtils.hasText(this.id)) {
            this.id = a.getId();
        }

        if (!StringUtils.hasText(this.name)) {
            this.name = a.getName();
        }

        if (!StringUtils.hasText(this.version)) {
            this.version = a.getVersion();
        }

        if (!StringUtils.hasText(this.license)) {
            this.license = a.getLicense();
        }

        if (!StringUtils.hasText(this.url)) {
            this.url = a.getUrl();
        }

        if (!this.securityRelevant) {
            this.securityRelevant = a.isSecurityRelevant();
        }

        if (!StringUtils.hasText(this.securityCategory)) {
            this.securityCategory = a.getSecurityCategory();
        }

        this.reported |= a.isReported();

        this.used |= a.isUsed();

        this.versionReported |= a.isVersionReported();

        this.projects.addAll(a.getProjects());

        if (!StringUtils.hasText(this.comment)) {
            this.comment = a.getComment();
        }

        if (!this.versionReported) {
            this.versionReported = a.isVersionReported();
        }

        if (!StringUtils.hasText(this.classification)) {
            this.classification = a.getClassification();
        }

        this.projects.addAll(a.getProjects());

        if (!StringUtils.hasText(this.groupId)) {
            this.groupId = a.getGroupId();
        }

        if (!StringUtils.hasText(this.artifactId)) {
            this.groupId = a.getArtifactId();
        }

        if (!StringUtils.hasText(this.latestAvailableVersion)) {
            this.latestAvailableVersion = a.getLatestAvailableVersion();
        }

    }

    @Override
    public void deriveArtifactId() {
        if (artifactId == null) {
            String id = extractArtifactId(getId(), getVersion());
            if (id == null) {
                id = extractArtifactId(getId(), extractVersionFromId());
            }
            if (id == null) {
                id = getId();
            }
            this.setArtifactId(id);
        }
    }

    private String extractArtifactId(String id, String version) {
        if (StringUtils.hasText(id) && StringUtils.hasText(version)) {
            int index = id.lastIndexOf(version);
            if (index != -1) {
                id = id.substring(0, index);

                if (id.endsWith(DELIMITER_DASH)) {
                    id = id.substring(0, id.length() - 1);
                }

                if (StringUtils.hasText(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    @Override
    public String createStringRepresentation() {
        StringBuffer artifactRepresentation = new StringBuffer();
        if (groupId != null || artifactId != null) {
            artifactRepresentation.append(getGroupId());
            artifactRepresentation.append(DELIMITER_COLON);
            artifactRepresentation.append(getArtifactId());
            artifactRepresentation.append(DELIMITER_COLON);
            artifactRepresentation.append(getVersion());
            if (getClassification() != null) {
                artifactRepresentation.append(DELIMITER_COLON);
                artifactRepresentation.append(getClassification());
            }
            artifactRepresentation.append(DELIMITER_COLON);
            artifactRepresentation.append(getType());
        } else if (getName() != null) {
            artifactRepresentation.append(getName());
            artifactRepresentation.append(DELIMITER_COLON);
            artifactRepresentation.append(getVersion());
        } else if (getLicense() != null) {
            artifactRepresentation.append(getLicense());
        } else {
            artifactRepresentation.append(toString());
        }
        return artifactRepresentation.toString();
    }

    private String inferTypeFromId() {
        String type = null;
        if (id != null) {
            final int index = id.lastIndexOf(DELIMITER_DOT);
            if (index != -1) {
                type = id.substring(index + 1);
            }
        }
        return type;
    }

    private String inferClassifierFromId() {
        String classifier = null;
        if (id != null) {
            // get rid of trailing .{type}
            int index = id.lastIndexOf(DELIMITER_COLON);
            if (index != -1) {
                classifier = id.substring(0, index);
                index = classifier.lastIndexOf(getVersion() + DELIMITER_DASH);
                if (index != -1) {
                    System.out.println("Classifier: " + classifier);
                    return classifier.substring(index + 1);
                }
            }
        }

        return classifier;
    }

    public String createCompareStringRepresentation() {
        StringBuffer artifactRepresentation = new StringBuffer();
        artifactRepresentation.append(normalize(getGroupId()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getArtifactId()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getName()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getVersion()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getType()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getClassification()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getLicense()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getLatestAvailableVersion()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getComment()));
        return artifactRepresentation.toString();
    }

    public String normalize(String s) {
        if (StringUtils.hasText(s))
            return s.trim();
        return "";
    }

    public Set<Artifact> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<Artifact> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void addDependency(Artifact dependency) {
        this.dependencies.add(dependency);
    }

    @Override
    public void addDependencies(Collection<Artifact> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    @Override
    public void removeDependency(Artifact dependency) {
        this.dependencies.remove(dependency);
    }

    @Override
    public void removeDependencies(Collection<Artifact> dependencies) {
        this.dependencies.removeAll(dependencies);
    }

    public String getDerivedLicenseFolder() {
        return LicenseMetaData.deriveLicenseFolderName(getLicense());
    }

    @Override
    public String getType() {
        return inferTypeFromId();
    }

    @Override
    public String getClassifier() {
        return inferClassifierFromId();
    }

    public boolean isRelevant() {
        return relevant;
    }

    public void setRelevant(boolean relevant) {
        this.relevant = relevant;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

}
