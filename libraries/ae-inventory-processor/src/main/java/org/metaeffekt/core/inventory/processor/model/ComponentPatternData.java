/*
 * Copyright 2009-2024 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Class to capture component patterns for matching artifacts during scans.
 */
public class ComponentPatternData extends AbstractModelBase {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    private transient Supplier<Inventory> expansionInventorySupplier;

    /**
     * Notate which component pattern contributor was used to generate this component pattern. Useful for debugging.
     */
    private transient String context = "UNKNOWN";

    public ComponentPatternData(ComponentPatternData cpd) {
        super(cpd);
        this.expansionInventorySupplier = cpd.expansionInventorySupplier;
        this.context = cpd.context;
    }

    public ComponentPatternData() {}

    public void validate(String context) {
        // validate minimal attributes

        validateNotEmptyOrNull(context, Attribute.INCLUDE_PATTERN.getKey(), get(Attribute.INCLUDE_PATTERN));
        validateNotEmptyOrNull(context, Attribute.VERSION_ANCHOR.getKey(), get(Attribute.VERSION_ANCHOR));
        validateNotEmptyOrNull(context, Attribute.VERSION_ANCHOR_CHECKSUM.getKey(), get(Attribute.VERSION_ANCHOR_CHECKSUM));
        validateNotEmptyOrNull(context, Attribute.COMPONENT_PART.getKey(), get(Attribute.COMPONENT_PART));
    }

    public void validate() {
        validate(context);
    }

    private void validateNotEmptyOrNull(String context, String key, String s) {
        if (StringUtils.isBlank(s)) {
            throw new IllegalStateException(context + ": ComponentPatternData [" + key + "] must not be empty.");
        }
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
        VERSION_ANCHOR_CHECKSUM("Version Anchor Checksum"),
        TYPE("Type"),
        COMPONENT_SOURCE_TYPE("Component Source Type"),

        // files covered by a component pattern may be shared with other component patterns; this attribute allows
        // to specify files which are known to be shared, but should remain included, when the files are collected
        SHARED_INCLUDE_PATTERN("Shared Include Pattern"),

        // files covered by a component pattern may be shared with other component patterns; this attribute allows
        // to specify files which are known to be shared, and should not be collected for the component
        SHARED_EXCLUDE_PATTERN("Shared Exclude Pattern");

        private final String key;

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
     *
     * @return Boolean whether the {@link ComponentPatternData} instance is valid.
     */
    public boolean isValid() {
        return !StringUtils.isEmpty(get(Attribute.INCLUDE_PATTERN));
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        String sb = get(Attribute.INCLUDE_PATTERN) + "-" +
                get(Attribute.VERSION_ANCHOR) + "-" +
                get(Attribute.VERSION_ANCHOR_CHECKSUM);
        return sb;
    }

    /**
     * @return The derived simple qualifier for this instance.
     */
    public String deriveSimpleQualifier() {
        String sb = get(Attribute.VERSION_ANCHOR) + "-" +
                get(Attribute.VERSION_ANCHOR_CHECKSUM);
        return sb;
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

    public String createToStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (String attributeKey : CORE_ATTRIBUTES) {
            // attribute keys to omit
            if (attributeKey.equals(Attribute.INCLUDE_PATTERN.getKey())) continue;
            if (attributeKey.equals(Attribute.EXCLUDE_PATTERN.getKey())) continue;

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

    public void setExpansionInventorySupplier(Supplier<Inventory> expansionInventorySupplier) {
        this.expansionInventorySupplier = expansionInventorySupplier;
    }

    public Supplier<Inventory> getExpansionInventorySupplier() {
        return expansionInventorySupplier;
    }

    @Override
    public String toString() {
        return "ComponentPatternData: " + createToStringRepresentation();
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
