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
package org.metaeffekt.core.inventory.processor.model;

import org.springframework.util.StringUtils;

import java.util.ArrayList;

/**
 * Class to capture component patterns for matching artifacts during scans.
 */
public class ComponentPatternData extends AbstractModelBase {

    public ComponentPatternData(ComponentPatternData cpd) {
        super(cpd);
    }

    public ComponentPatternData() {
    }

    /**
     * Core attributes to support component patterns.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        INCLUDE_PATTERN("Include Pattern"),
        EXCLUDE_PATTERN("Exclude Pattern"),
        COMPONENT_NAME("Component Name"),
        COMPONENT_PART("Component Part"),
        COMPONENT_VERSION("Component Version"),

        // version anchors must not be null; these are primarily for matching
        VERSION_ANCHOR("Version Anchor"),
        VERSION_ANCHOR_CHECKSUM("Version Anchor Checksum");

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
        CORE_ATTRIBUTES.add(Attribute.INCLUDE_PATTERN.getKey());
        CORE_ATTRIBUTES.add(Attribute.EXCLUDE_PATTERN.getKey());
        CORE_ATTRIBUTES.add(Attribute.COMPONENT_NAME.getKey());
        CORE_ATTRIBUTES.add(Attribute.COMPONENT_PART.getKey());
        CORE_ATTRIBUTES.add(Attribute.COMPONENT_VERSION.getKey());
        CORE_ATTRIBUTES.add(Attribute.VERSION_ANCHOR.getKey());
        CORE_ATTRIBUTES.add(Attribute.VERSION_ANCHOR_CHECKSUM.getKey());
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     */
    public boolean isValid() {
        if (StringUtils.isEmpty(get(Attribute.INCLUDE_PATTERN))) return false;
        return true;
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(get(Attribute.INCLUDE_PATTERN)).append("-");
        sb.append(get(Attribute.VERSION_ANCHOR)).append("-");
        sb.append(get(Attribute.VERSION_ANCHOR_CHECKSUM));
        return sb.toString();
    }

    /**
     * The compare string representations is built from the core attributes.
     *
     * @return String to compare component patterns.
     */
    public String createCompareStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (String attributeKey : CORE_ATTRIBUTES) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            String value = get(attributeKey);
            sb.append(value == null ? "" : value);
        }
        return sb.toString();
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

}
