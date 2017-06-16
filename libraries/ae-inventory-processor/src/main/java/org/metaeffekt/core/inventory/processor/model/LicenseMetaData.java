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

/**
 * {@link LicenseMetaData} contains relevant meta data to evaluate notices with regards to the
 * used licenses in a project. 
 *
 * @author Karsten Klein
 */
public class LicenseMetaData {

    private String component;

    private String version;

    private String name;

    private String obligationText;

    private String comment;

    public static String deriveLicenseFolderName(String license) {
        if (license == null) return null;
        return normalizeId(license);
    }

    public static String normalizeId(String string) {
        String result = string.replace(" ", "-");
        result = result.replace("+", "");
        result = result.replace("!", "");
        result = result.replace(",", "-");
        result = result.replace(":", "_");
        result = result.replace(";", "_");
        result = result.replace("/", "_");
        result = result.replace("\\", "_");

        int length = -1;
        while (length != result.length()) {
            length = result.length();
            result = result.replace("__", "_");
            result = result.replace("--", "-");
            result = result.replace("-_", "-");
            result = result.replace("_-", "-");
            result = result.replace("_(", "(");
            result = result.replace("-)", ")");
        }
        return result;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObligationText() {
        return obligationText;
    }

    public void setObligationText(String obligationText) {
        this.obligationText = obligationText;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String deriveQualifier() {
        return new StringBuilder(getComponent()).append(getName()).append(getVersion()).toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append('/').append(component);
        sb.append('/').append(version);
        sb.append('/').append(obligationText);
        if (StringUtils.hasText(comment)) {
            sb.append('/').append(comment);
        }
        return sb.toString();
    }

    public String createCompareStringRepresentation() {
        return toString();
    }
}
