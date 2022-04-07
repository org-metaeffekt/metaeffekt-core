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
package org.metaeffekt.reader.inspect.image.model.subtypes;

import com.fasterxml.jackson.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Hostname",
    "Domainname",
    "User",
    "AttachStdin",
    "AttachStdout",
    "AttachStderr",
    "Tty",
    "OpenStdin",
    "StdinOnce",
    "Env",
    "Cmd",
    "Image",
    "Volumes",
    "WorkingDir",
    "Entrypoint",
    "OnBuild",
    "Labels",
    "ExposedPorts"
})
public class ContainerConfig {

    @JsonProperty("Hostname")
    private String hostname;
    @JsonProperty("Domainname")
    private String domainname;
    @JsonProperty("User")
    private String user;
    @JsonProperty("AttachStdin")
    private Boolean attachStdin;
    @JsonProperty("AttachStdout")
    private Boolean attachStdout;
    @JsonProperty("AttachStderr")
    private Boolean attachStderr;
    @JsonProperty("Tty")
    private Boolean tty;
    @JsonProperty("OpenStdin")
    private Boolean openStdin;
    @JsonProperty("StdinOnce")
    private Boolean stdinOnce;
    @JsonProperty("Env")
    private List<String> env = new ArrayList<String>();
    @JsonProperty("Cmd")
    private List<String> cmd = new ArrayList<String>();
    @JsonProperty("Image")
    private String image;
    @JsonProperty("Volumes")
    private Object volumes;
    @JsonProperty("WorkingDir")
    private String workingDir;
    @JsonProperty("Entrypoint")
    private List<String> entrypoint = new ArrayList<String>();
    @JsonProperty("OnBuild")
    private Object onBuild;
    @JsonProperty("Labels")
    private Map<String, String> labels;
    @JsonProperty("ExposedPorts")
    private Map<String, Map<String, String>> exposedPorts;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("Hostname")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @JsonProperty("Domainname")
    public void setDomainname(String domainname) {
        this.domainname = domainname;
    }

    @JsonProperty("User")
    public void setUser(String user) {
        this.user = user;
    }

    @JsonProperty("AttachStdin")
    public void setAttachStdin(Boolean attachStdin) {
        this.attachStdin = attachStdin;
    }

    @JsonProperty("AttachStdout")
    public void setAttachStdout(Boolean attachStdout) {
        this.attachStdout = attachStdout;
    }

    @JsonProperty("AttachStderr")
    public void setAttachStderr(Boolean attachStderr) {
        this.attachStderr = attachStderr;
    }

    @JsonProperty("Tty")
    public void setTty(Boolean tty) {
        this.tty = tty;
    }

    @JsonProperty("OpenStdin")
    public void setOpenStdin(Boolean openStdin) {
        this.openStdin = openStdin;
    }

    @JsonProperty("StdinOnce")
    public void setStdinOnce(Boolean stdinOnce) {
        this.stdinOnce = stdinOnce;
    }

    @JsonProperty("Env")
    public void setEnv(List<String> env) {
        this.env = env;
    }

    @JsonProperty("Cmd")
    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }

    @JsonProperty("Image")
    public void setImage(String image) {
        this.image = image;
    }

    @JsonProperty("Volumes")
    public void setVolumes(Object volumes) {
        this.volumes = volumes;
    }

    @JsonProperty("WorkingDir")
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @JsonProperty("Entrypoint")
    public void setEntrypoint(List<String> entrypoint) {
        this.entrypoint = entrypoint;
    }

    @JsonProperty("OnBuild")
    public void setOnBuild(Object onBuild) {
        this.onBuild = onBuild;
    }

    @JsonProperty("Labels")
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @JsonProperty("ExposedPorts")
    public void setExposedPorts(Map<String, Map<String, String>> exposedPorts) {
        this.exposedPorts = exposedPorts;
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
        sb.append(ContainerConfig.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("hostname");
        sb.append('=');
        sb.append(((this.hostname == null)?"<null>":this.hostname));
        sb.append(',');
        sb.append("domainname");
        sb.append('=');
        sb.append(((this.domainname == null)?"<null>":this.domainname));
        sb.append(',');
        sb.append("user");
        sb.append('=');
        sb.append(((this.user == null)?"<null>":this.user));
        sb.append(',');
        sb.append("attachStdin");
        sb.append('=');
        sb.append(((this.attachStdin == null)?"<null>":this.attachStdin));
        sb.append(',');
        sb.append("attachStdout");
        sb.append('=');
        sb.append(((this.attachStdout == null)?"<null>":this.attachStdout));
        sb.append(',');
        sb.append("attachStderr");
        sb.append('=');
        sb.append(((this.attachStderr == null)?"<null>":this.attachStderr));
        sb.append(',');
        sb.append("tty");
        sb.append('=');
        sb.append(((this.tty == null)?"<null>":this.tty));
        sb.append(',');
        sb.append("openStdin");
        sb.append('=');
        sb.append(((this.openStdin == null)?"<null>":this.openStdin));
        sb.append(',');
        sb.append("stdinOnce");
        sb.append('=');
        sb.append(((this.stdinOnce == null)?"<null>":this.stdinOnce));
        sb.append(',');
        sb.append("env");
        sb.append('=');
        sb.append(((this.env == null)?"<null>":this.env));
        sb.append(',');
        sb.append("cmd");
        sb.append('=');
        sb.append(((this.cmd == null)?"<null>":this.cmd));
        sb.append(',');
        sb.append("image");
        sb.append('=');
        sb.append(((this.image == null)?"<null>":this.image));
        sb.append(',');
        sb.append("volumes");
        sb.append('=');
        sb.append(((this.volumes == null)?"<null>":this.volumes));
        sb.append(',');
        sb.append("workingDir");
        sb.append('=');
        sb.append(((this.workingDir == null)?"<null>":this.workingDir));
        sb.append(',');
        sb.append("entrypoint");
        sb.append('=');
        sb.append(((this.entrypoint == null)?"<null>":this.entrypoint));
        sb.append(',');
        sb.append("onBuild");
        sb.append('=');
        sb.append(((this.onBuild == null)?"<null>":this.onBuild));
        sb.append(',');
        sb.append("labels");
        sb.append('=');
        sb.append(((this.labels == null)?"<null>":this.labels));
        sb.append(',');
        sb.append("exposedPorts");
        sb.append('=');
        sb.append(((this.exposedPorts == null)?"<null>":this.exposedPorts));
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
        result = ((result* 31)+((this.image == null)? 0 :this.image.hashCode()));
        result = ((result* 31)+((this.attachStdout == null)? 0 :this.attachStdout.hashCode()));
        result = ((result* 31)+((this.exposedPorts == null)? 0 :this.exposedPorts.hashCode()));
        result = ((result* 31)+((this.workingDir == null)? 0 :this.workingDir.hashCode()));
        result = ((result* 31)+((this.domainname == null)? 0 :this.domainname.hashCode()));
        result = ((result* 31)+((this.volumes == null)? 0 :this.volumes.hashCode()));
        result = ((result* 31)+((this.env == null)? 0 :this.env.hashCode()));
        result = ((result* 31)+((this.onBuild == null)? 0 :this.onBuild.hashCode()));
        result = ((result* 31)+((this.attachStdin == null)? 0 :this.attachStdin.hashCode()));
        result = ((result* 31)+((this.labels == null)? 0 :this.labels.hashCode()));
        result = ((result* 31)+((this.attachStderr == null)? 0 :this.attachStderr.hashCode()));
        result = ((result* 31)+((this.hostname == null)? 0 :this.hostname.hashCode()));
        result = ((result* 31)+((this.entrypoint == null)? 0 :this.entrypoint.hashCode()));
        result = ((result* 31)+((this.tty == null)? 0 :this.tty.hashCode()));
        result = ((result* 31)+((this.cmd == null)? 0 :this.cmd.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.openStdin == null)? 0 :this.openStdin.hashCode()));
        result = ((result* 31)+((this.stdinOnce == null)? 0 :this.stdinOnce.hashCode()));
        result = ((result* 31)+((this.user == null)? 0 :this.user.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ContainerConfig) == false) {
            return false;
        }
        ContainerConfig rhs = ((ContainerConfig) other);
        return ((((((((((((((((((((this.image == rhs.image)||((this.image!= null)&&this.image.equals(rhs.image)))&&((this.attachStdout == rhs.attachStdout)||((this.attachStdout!= null)&&this.attachStdout.equals(rhs.attachStdout))))&&((this.exposedPorts == rhs.exposedPorts)||((this.exposedPorts!= null)&&this.exposedPorts.equals(rhs.exposedPorts))))&&((this.workingDir == rhs.workingDir)||((this.workingDir!= null)&&this.workingDir.equals(rhs.workingDir))))&&((this.domainname == rhs.domainname)||((this.domainname!= null)&&this.domainname.equals(rhs.domainname))))&&((this.volumes == rhs.volumes)||((this.volumes!= null)&&this.volumes.equals(rhs.volumes))))&&((this.env == rhs.env)||((this.env!= null)&&this.env.equals(rhs.env))))&&((this.onBuild == rhs.onBuild)||((this.onBuild!= null)&&this.onBuild.equals(rhs.onBuild))))&&((this.attachStdin == rhs.attachStdin)||((this.attachStdin!= null)&&this.attachStdin.equals(rhs.attachStdin))))&&((this.labels == rhs.labels)||((this.labels!= null)&&this.labels.equals(rhs.labels))))&&((this.attachStderr == rhs.attachStderr)||((this.attachStderr!= null)&&this.attachStderr.equals(rhs.attachStderr))))&&((this.hostname == rhs.hostname)||((this.hostname!= null)&&this.hostname.equals(rhs.hostname))))&&((this.entrypoint == rhs.entrypoint)||((this.entrypoint!= null)&&this.entrypoint.equals(rhs.entrypoint))))&&((this.tty == rhs.tty)||((this.tty!= null)&&this.tty.equals(rhs.tty))))&&((this.cmd == rhs.cmd)||((this.cmd!= null)&&this.cmd.equals(rhs.cmd))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.openStdin == rhs.openStdin)||((this.openStdin!= null)&&this.openStdin.equals(rhs.openStdin))))&&((this.stdinOnce == rhs.stdinOnce)||((this.stdinOnce!= null)&&this.stdinOnce.equals(rhs.stdinOnce))))&&((this.user == rhs.user)||((this.user!= null)&&this.user.equals(rhs.user))));
    }

    public String getHostname() {
        return hostname;
    }

    public String getDomainname() {
        return domainname;
    }

    public String getUser() {
        return user;
    }

    public Boolean getAttachStdin() {
        return attachStdin;
    }

    public Boolean getAttachStdout() {
        return attachStdout;
    }

    public Boolean getAttachStderr() {
        return attachStderr;
    }

    public Boolean getTty() {
        return tty;
    }

    public Boolean getOpenStdin() {
        return openStdin;
    }

    public Boolean getStdinOnce() {
        return stdinOnce;
    }

    public List<String> getEnv() {
        return env;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public String getImage() {
        return image;
    }

    public Object getVolumes() {
        return volumes;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public List<String> getEntrypoint() {
        return entrypoint;
    }

    public Object getOnBuild() {
        return onBuild;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Map<String, String>> getExposedPorts() {
        return exposedPorts;
    }
}
