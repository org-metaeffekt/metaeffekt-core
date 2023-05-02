/*
 * Copyright 2009-2022 the original author or authors.
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

import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;

public class InventoryInfo extends AbstractModelBase {

    public InventoryInfo(InventoryInfo info) {
        super(info);
    }

    public InventoryInfo() {
    }

    /**
     * Core attributes to support cert advisories.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        ID("Id");

        private String key;

        Attribute(String key) {
            this.key = key;
        }

        public static Attribute match(String key) {
            for (Attribute a : values()) {
                if (a.getKey().equalsIgnoreCase(key)) {
                    return a;
                }
            }
            return null;
        }

        public String getKey() {
            return key;
        }
    }

    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();

    static {
        // fix selection and order
        CORE_ATTRIBUTES.add(Attribute.ID.getKey());
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     *
     * @return Boolean indicating whether the instance is valid.
     */
    public boolean isValid() {
        if (ObjectUtils.isEmpty(get(Attribute.ID))) return false;
        return true;
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(get(Attribute.ID));
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
            sb.append(get(attributeKey));
        }
        return sb.toString();
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public float getFloat(Attribute attribute, float defaultValue) {
        return getFloat(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    public void append(Attribute attribute, String value, String delimiter) {
        append(attribute.getKey(), value, delimiter);
    }

    public String getId() {
        return get(Attribute.ID);
    }

    public static InventoryInfo getById(Collection<InventoryInfo> inventoryInfos, String id) {
        return inventoryInfos.stream()
                .filter(e -> id.equals(e.get(Attribute.ID)))
                .findFirst()
                .orElse(null);
    }
}
