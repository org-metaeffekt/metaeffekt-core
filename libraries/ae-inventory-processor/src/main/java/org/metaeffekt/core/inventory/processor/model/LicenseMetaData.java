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

    private String license;

    private String licenseInEffect;

    private String notice;

    private String comment;

    // FIXME: we should also copy the folder with the license in effect
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

    @Deprecated
    public String getName() {
        return license;
    }

    @Deprecated
    public void setName(String license) {
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
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

    public String getLicenseInEffect() {
        return licenseInEffect;
    }

    public void setLicenseInEffect(String licenseInEffect) {
        this.licenseInEffect = licenseInEffect;
    }

    public String deriveQualifier() {
        return new StringBuilder(getComponent()).append(getName()).append(getVersion()).toString();
    }

    public String deriveLicenseInEffect() {
        String licenseInEffect = getLicenseInEffect();
        if (StringUtils.isEmpty(licenseInEffect)) {
            licenseInEffect = getLicense();
        }
        return licenseInEffect;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(license);
        sb.append('/').append(component);
        sb.append('/').append(version);
        sb.append('/').append(licenseInEffect);
        sb.append('/').append(notice);
        if (StringUtils.hasText(comment)) {
            sb.append('/').append(comment);
        }
        return sb.toString();
    }

    public String createCompareStringRepresentation() {
        return toString();
    }

    public boolean isValid() {
        if (StringUtils.isEmpty(getComponent())) return false;
        if (StringUtils.isEmpty(getVersion())) return false;
        return true;
    }
}
