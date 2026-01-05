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
package org.metaeffekt.reader.inspect.image.model;

import com.fasterxml.jackson.annotation.*;
import org.metaeffekt.reader.inspect.image.model.subtypes.Config;
import org.metaeffekt.reader.inspect.image.model.subtypes.ContainerConfig;
import org.metaeffekt.reader.inspect.image.model.subtypes.GraphDriver;
import org.metaeffekt.reader.inspect.image.model.subtypes.RootFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Id",
    "RepoTags",
    "RepoDigests",
    "Parent",
    "Comment",
    "Created",
    "Container",
    "ContainerConfig",
    "DockerVersion",
    "Author",
    "Config",
    "Architecture",
    "Os",
    "Size",
    "VirtualSize",
    "GraphDriver",
    "RootFS",
    "Metadata"
})
public class ImageInspectElement {

    @JsonProperty("Id")
    private String id;
    @JsonProperty("RepoTags")
    private List<String> repoTags = new ArrayList<String>();
    @JsonProperty("RepoDigests")
    private List<String> repoDigests = new ArrayList<String>();
    @JsonProperty("Parent")
    private String parent;
    @JsonProperty("Comment")
    private String comment;
    @JsonProperty("Created")
    private String created;
    @JsonProperty("Container")
    private String container;
    @JsonProperty("ContainerConfig")
    private ContainerConfig containerConfig;
    @JsonProperty("DockerVersion")
    private String dockerVersion;
    @JsonProperty("Author")
    private String author;
    @JsonProperty("Config")
    private Config config;
    @JsonProperty("Architecture")
    private String architecture;
    @JsonProperty("Os")
    private String os;
    @JsonProperty("Size")
    private Long size;
    @JsonProperty("VirtualSize")
    private Long virtualSize;
    @JsonProperty("GraphDriver")
    private GraphDriver graphDriver;
    @JsonProperty("RootFS")
    private RootFS rootFS;
    @JsonProperty("Metadata")
    private Map<String, String> metadata;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public List<String> getRepoTags() {
        return repoTags;
    }

    public List<String> getRepoDigests() {
        return repoDigests;
    }

    public String getParent() {
        return parent;
    }

    public String getComment() {
        return comment;
    }

    public String getCreated() {
        return created;
    }

    public String getContainer() {
        return container;
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }

    public String getAuthor() {
        return author;
    }

    public Config getConfig() {
        return config;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getOs() {
        return os;
    }

    public Long getSize() {
        return size;
    }

    public Long getVirtualSize() {
        return virtualSize;
    }

    public GraphDriver getGraphDriver() {
        return graphDriver;
    }

    public RootFS getRootFS() {
        return rootFS;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonProperty("Id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("RepoTags")
    public void setRepoTags(List<String> repoTags) {
        this.repoTags = repoTags;
    }

    @JsonProperty("RepoDigests")
    public void setRepoDigests(List<String> repoDigests) {
        this.repoDigests = repoDigests;
    }

    @JsonProperty("Parent")
    public void setParent(String parent) {
        this.parent = parent;
    }

    @JsonProperty("Comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

    @JsonProperty("Created")
    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty("Container")
    public void setContainer(String container) {
        this.container = container;
    }

    @JsonProperty("ContainerConfig")
    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

    @JsonProperty("DockerVersion")
    public void setDockerVersion(String dockerVersion) {
        this.dockerVersion = dockerVersion;
    }

    @JsonProperty("Author")
    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonProperty("Config")
    public void setConfig(Config config) {
        this.config = config;
    }

    @JsonProperty("Architecture")
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @JsonProperty("Os")
    public void setOs(String os) {
        this.os = os;
    }

    @JsonProperty("Size")
    public void setSize(Long size) {
        this.size = size;
    }

    @JsonProperty("VirtualSize")
    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    @JsonProperty("GraphDriver")
    public void setGraphDriver(GraphDriver graphDriver) {
        this.graphDriver = graphDriver;
    }

    @JsonProperty("RootFS")
    public void setRootFS(RootFS rootFS) {
        this.rootFS = rootFS;
    }

    @JsonProperty("Metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ImageInspectElement.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("repoTags");
        sb.append('=');
        sb.append(((this.repoTags == null)?"<null>":this.repoTags));
        sb.append(',');
        sb.append("repoDigests");
        sb.append('=');
        sb.append(((this.repoDigests == null)?"<null>":this.repoDigests));
        sb.append(',');
        sb.append("parent");
        sb.append('=');
        sb.append(((this.parent == null)?"<null>":this.parent));
        sb.append(',');
        sb.append("comment");
        sb.append('=');
        sb.append(((this.comment == null)?"<null>":this.comment));
        sb.append(',');
        sb.append("created");
        sb.append('=');
        sb.append(((this.created == null)?"<null>":this.created));
        sb.append(',');
        sb.append("container");
        sb.append('=');
        sb.append(((this.container == null)?"<null>":this.container));
        sb.append(',');
        sb.append("containerConfig");
        sb.append('=');
        sb.append(((this.containerConfig == null)?"<null>":this.containerConfig));
        sb.append(',');
        sb.append("dockerVersion");
        sb.append('=');
        sb.append(((this.dockerVersion == null)?"<null>":this.dockerVersion));
        sb.append(',');
        sb.append("author");
        sb.append('=');
        sb.append(((this.author == null)?"<null>":this.author));
        sb.append(',');
        sb.append("config");
        sb.append('=');
        sb.append(((this.config == null)?"<null>":this.config));
        sb.append(',');
        sb.append("architecture");
        sb.append('=');
        sb.append(((this.architecture == null)?"<null>":this.architecture));
        sb.append(',');
        sb.append("os");
        sb.append('=');
        sb.append(((this.os == null)?"<null>":this.os));
        sb.append(',');
        sb.append("size");
        sb.append('=');
        sb.append(((this.size == null)?"<null>":this.size));
        sb.append(',');
        sb.append("virtualSize");
        sb.append('=');
        sb.append(((this.virtualSize == null)?"<null>":this.virtualSize));
        sb.append(',');
        sb.append("graphDriver");
        sb.append('=');
        sb.append(((this.graphDriver == null)?"<null>":this.graphDriver));
        sb.append(',');
        sb.append("rootFS");
        sb.append('=');
        sb.append(((this.rootFS == null)?"<null>":this.rootFS));
        sb.append(',');
        sb.append("metadata");
        sb.append('=');
        sb.append(((this.metadata == null)?"<null>":this.metadata));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.container == null)? 0 :this.container.hashCode()));
        result = ((result* 31)+((this.parent == null)? 0 :this.parent.hashCode()));
        result = ((result* 31)+((this.metadata == null)? 0 :this.metadata.hashCode()));
        result = ((result* 31)+((this.containerConfig == null)? 0 :this.containerConfig.hashCode()));
        result = ((result* 31)+((this.os == null)? 0 :this.os.hashCode()));
        result = ((result* 31)+((this.graphDriver == null)? 0 :this.graphDriver.hashCode()));
        result = ((result* 31)+((this.created == null)? 0 :this.created.hashCode()));
        result = ((result* 31)+((this.author == null)? 0 :this.author.hashCode()));
        result = ((result* 31)+((this.repoDigests == null)? 0 :this.repoDigests.hashCode()));
        result = ((result* 31)+((this.virtualSize == null)? 0 :this.virtualSize.hashCode()));
        result = ((result* 31)+((this.dockerVersion == null)? 0 :this.dockerVersion.hashCode()));
        result = ((result* 31)+((this.size == null)? 0 :this.size.hashCode()));
        result = ((result* 31)+((this.repoTags == null)? 0 :this.repoTags.hashCode()));
        result = ((result* 31)+((this.rootFS == null)? 0 :this.rootFS.hashCode()));
        result = ((result* 31)+((this.comment == null)? 0 :this.comment.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.config == null)? 0 :this.config.hashCode()));
        result = ((result* 31)+((this.architecture == null)? 0 :this.architecture.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ImageInspectElement) == false) {
            return false;
        }
        ImageInspectElement rhs = ((ImageInspectElement) other);
        return ((((((((((((((((((((this.container == rhs.container)||((this.container!= null)&&this.container.equals(rhs.container)))&&((this.parent == rhs.parent)||((this.parent!= null)&&this.parent.equals(rhs.parent))))&&((this.metadata == rhs.metadata)||((this.metadata!= null)&&this.metadata.equals(rhs.metadata))))&&((this.containerConfig == rhs.containerConfig)||((this.containerConfig!= null)&&this.containerConfig.equals(rhs.containerConfig))))&&((this.os == rhs.os)||((this.os!= null)&&this.os.equals(rhs.os))))&&((this.graphDriver == rhs.graphDriver)||((this.graphDriver!= null)&&this.graphDriver.equals(rhs.graphDriver))))&&((this.created == rhs.created)||((this.created!= null)&&this.created.equals(rhs.created))))&&((this.author == rhs.author)||((this.author!= null)&&this.author.equals(rhs.author))))&&((this.repoDigests == rhs.repoDigests)||((this.repoDigests!= null)&&this.repoDigests.equals(rhs.repoDigests))))&&((this.virtualSize == rhs.virtualSize)||((this.virtualSize!= null)&&this.virtualSize.equals(rhs.virtualSize))))&&((this.dockerVersion == rhs.dockerVersion)||((this.dockerVersion!= null)&&this.dockerVersion.equals(rhs.dockerVersion))))&&((this.size == rhs.size)||((this.size!= null)&&this.size.equals(rhs.size))))&&((this.repoTags == rhs.repoTags)||((this.repoTags!= null)&&this.repoTags.equals(rhs.repoTags))))&&((this.rootFS == rhs.rootFS)||((this.rootFS!= null)&&this.rootFS.equals(rhs.rootFS))))&&((this.comment == rhs.comment)||((this.comment!= null)&&this.comment.equals(rhs.comment))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.config == rhs.config)||((this.config!= null)&&this.config.equals(rhs.config))))&&((this.architecture == rhs.architecture)||((this.architecture!= null)&&this.architecture.equals(rhs.architecture))));
    }

}
