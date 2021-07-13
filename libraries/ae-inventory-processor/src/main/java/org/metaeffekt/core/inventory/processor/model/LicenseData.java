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
package org.metaeffekt.core.inventory.processor.model;

import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 * Model class that supports to aggregate data around licenses. The information can be used in reports and documentation.
 */
public class LicenseData extends AbstractModelBase {

    public LicenseData(LicenseData ld) {
        super(ld);
    }

    public LicenseData() {
    }

    /**
     * Core attributes to support license data.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        CANONICAL_NAME("Canonical Name"),
        ID("Id"),
        SPDX_ID("SPDX Id"),
        OSI_APPROVED("OSI Approved"),
        COPYLEFT_TYPE("Copyleft Type"),
        COMMERCIAL("Commercial"),
        REPRESENTED_AS("RepresentedAs");

        private String key;
        Attribute(String key) {
            this.key = key;
        }
        public String getKey() {
            return key;
        }
    }

    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();
    static {
        // fix selection and order
        CORE_ATTRIBUTES.add(Attribute.CANONICAL_NAME.getKey());
        CORE_ATTRIBUTES.add(Attribute.ID.getKey());
        CORE_ATTRIBUTES.add(Attribute.SPDX_ID.getKey());
        CORE_ATTRIBUTES.add(Attribute.OSI_APPROVED.getKey());
        CORE_ATTRIBUTES.add(Attribute.COPYLEFT_TYPE.getKey());
        CORE_ATTRIBUTES.add(Attribute.COMMERCIAL.getKey());
        CORE_ATTRIBUTES.add(Attribute.REPRESENTED_AS.getKey());
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     */
    public boolean isValid() {
        if (StringUtils.isEmpty(get(Attribute.CANONICAL_NAME))) return false;
        return true;
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        final StringBuilder sb = new StringBuilder();
        sb.append(get(Attribute.CANONICAL_NAME)).append("-");
        sb.append(get(Attribute.ID));
        return sb.toString();
    }

    /**
     * The compare string representations is built from the core attributes.
     *
     * @return String to compare component patterns.
     */
    public String createCompareStringRepresentation() {
        return createCompareStringRepresentation(CORE_ATTRIBUTES);
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    public void set(Attribute attribute, Boolean value) {
        if (value == null) {
            set(attribute.getKey(), null);
        } else {
            set(attribute.getKey(), value.toString());
        }
    }

    public boolean is(Attribute attribute) {
        return Boolean.TRUE.equals(get(attribute.getKey(), "false"));
    }

}
