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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract model base class. Support associating key and values as generic concept. Keys can be bound to enums specified
 * by the subclass.
 */
public abstract class AbstractModelBase {

    public interface Attribute {
        String getKey();
    }

    // map to store key values pairs.
    private Map<String, String> attributeMap = new HashMap<>();

    public AbstractModelBase(AbstractModelBase baseModelInstance) {
        this.attributeMap = new HashMap<>(baseModelInstance.attributeMap);
    }

    public AbstractModelBase() {
    }

    public String get(String key) {
        return get(key, null);
    }

    /**
     * Get the value associated with the given key. Use defaultValue, in case the key is not associated with any value.
     *
     * @param key The key to get the value for.
     * @param defaultValue The default value to use, in case the key is not associated.
     *
     * @return The value associated with the key or defaultValue in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public String get(String key, String defaultValue) {
        if (key == null) return null;
        final String currentValue = attributeMap.get(key);
        return (currentValue != null) ? currentValue : defaultValue;
    }

    /**
     * Function to check for a boolean value.
     *
     * @param key The key to get the boolean value for.
     *
     * @return The boolean value associated with the key. Returns <code>false</code> in case the associated value is
     * not equivalent to <code>Boolean.TRUE</code> or in case key is <code>null</code>.
     */
    public boolean is(String key) {
        return is(key, false);
    }

    /**
     * Function to check for a boolean value.
     *
     * @param key The key to get the boolean value for.
     * @param defaultValue he default value to use, in case the key is not associated.
     *
     * @return The boolean value associated with the key. Returns defaultValue in case the associated value is
     * not equivalent to <code>Boolean.TRUE</code> and <code>false</code> in case key is <code>null</code>.
     */
    public boolean is(String key, boolean defaultValue) {
        if (key == null) return defaultValue;
        final String currentValue = attributeMap.get(key);
        return (currentValue != null) ? Boolean.TRUE.toString().equalsIgnoreCase(currentValue) : defaultValue;
    }

    public void set(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            attributeMap.remove(key);
        } else {
            attributeMap.put(key, value);
        }
    }

    public void append(String key, String value, String delimiter) {
        String currentValue = get(key);
        if (currentValue == null) {
            set(key, value);
        } else {
            set(key, currentValue + delimiter + value);
        }
    }

    protected void merge(AbstractModelBase a) {
        for (String key : a.getAttributes()) {
            String value = get(key);
            if (!StringUtils.hasText(value)) {
                set(key, a.get(key));
            }
        }
    }

    public Set<String> getAttributes() {
        return attributeMap.keySet();
    }

    public int numAttributes() {
        return attributeMap.size();
    }

    public float getFloat(String key, float defaultValue) {
        String stringValue = get(key);
        if (stringValue == null) return defaultValue;
        try {
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected String createCompareStringRepresentation(List<String> attributeKeys) {
        final StringBuilder sb = new StringBuilder();
        for (final String attributeKey : attributeKeys) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            final String value = get(attributeKey);
            sb.append(value == null ? "" : value);
        }
        return sb.toString();
    }


}
