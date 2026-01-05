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
package org.metaeffekt.reader.inspect.image.model.subtypes;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "MergedDir",
    "UpperDir",
    "WorkDir",
    "LowerDir"
})
public class Data {

    @JsonProperty("MergedDir")
    private String mergedDir;
    @JsonProperty("UpperDir")
    private String upperDir;
    @JsonProperty("WorkDir")
    private String workDir;
    @JsonProperty("LowerDir")
    private String lowerDir;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("MergedDir")
    public void setMergedDir(String mergedDir) {
        this.mergedDir = mergedDir;
    }

    @JsonProperty("UpperDir")
    public void setUpperDir(String upperDir) {
        this.upperDir = upperDir;
    }

    @JsonProperty("WorkDir")
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    @JsonProperty("LowerDir")
    public void setLowerDir(String lowerDir) {
        this.lowerDir = lowerDir;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public String getMergedDir() {
        return mergedDir;
    }

    public String getUpperDir() {
        return upperDir;
    }

    public String getWorkDir() {
        return workDir;
    }

    public String getLowerDir() {
        return lowerDir;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Data.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("mergedDir");
        sb.append('=');
        sb.append(((this.mergedDir == null)?"<null>":this.mergedDir));
        sb.append(',');
        sb.append("upperDir");
        sb.append('=');
        sb.append(((this.upperDir == null)?"<null>":this.upperDir));
        sb.append(',');
        sb.append("workDir");
        sb.append('=');
        sb.append(((this.workDir == null)?"<null>":this.workDir));
        sb.append(',');
        sb.append("lowerDir");
        sb.append('=');
        sb.append(((this.lowerDir == null)?"<null>":this.lowerDir));
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
        result = ((result* 31)+((this.mergedDir == null)? 0 :this.mergedDir.hashCode()));
        result = ((result* 31)+((this.workDir == null)? 0 :this.workDir.hashCode()));
        result = ((result* 31)+((this.lowerDir == null)? 0 :this.lowerDir.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.upperDir == null)? 0 :this.upperDir.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Data) == false) {
            return false;
        }
        Data rhs = ((Data) other);
        return ((((((this.mergedDir == rhs.mergedDir)||((this.mergedDir!= null)&&this.mergedDir.equals(rhs.mergedDir)))&&((this.workDir == rhs.workDir)||((this.workDir!= null)&&this.workDir.equals(rhs.workDir))))&&((this.lowerDir == rhs.lowerDir)||((this.lowerDir!= null)&&this.lowerDir.equals(rhs.lowerDir))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.upperDir == rhs.upperDir)||((this.upperDir!= null)&&this.upperDir.equals(rhs.upperDir))));
    }

}
