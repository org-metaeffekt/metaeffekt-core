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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.writer.AbstractXlsxInventoryWriter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract model base class. Support associating key and values as generic concept. Keys can be bound to enums specified
 * by the subclass.
 */
public abstract class AbstractModelBase implements Serializable {

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
     * @param key          The key to get the value for.
     * @param defaultValue The default value to use, in case the key is not associated.
     * @return The value associated with the key or defaultValue in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public String get(String key, String defaultValue) {
        if (key == null) return null;
        final String currentValue = attributeMap.get(key);
        return (currentValue != null) ? currentValue : defaultValue;
    }

    /**
     * Get the value associated with the given key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.<br>
     * This method reconstructs the string set by the {@link #setComplete(String, String)} or
     * {@link #setComplete(Attribute, String)} method.
     *
     * @param key The key to get the value for.
     * @return The value associated with the key or <code>null</code> in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public String getComplete(Attribute key) {
        return getComplete(key.getKey(), null);
    }

    /**
     * Get the value associated with the given key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.<br>
     * This method reconstructs the string set by the {@link #setComplete(String, String)} or
     * {@link #setComplete(Attribute, String)} method.
     *
     * @param key          The key to get the value for.
     * @param defaultValue The default value to use, in case the key is not associated.
     * @return The value associated with the key or defaultValue in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public String getComplete(Attribute key, String defaultValue) {
        return getComplete(key.getKey(), defaultValue);
    }

    /**
     * Get the value associated with the given key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.<br>
     * This method reconstructs the string set by the {@link #setComplete(String, String)} or
     * {@link #setComplete(Attribute, String)} method.
     *
     * @param key The key to get the value for.
     * @return The value associated with the key or <code>null</code> in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public String getComplete(String key) {
        return getComplete(key, null);
    }

    /**
     * Get the value associated with the given key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.<br>
     * This method reconstructs the string set by the {@link #setComplete(String, String)} or
     * {@link #setComplete(Attribute, String)} method.
     *
     * @param key          The key to get the value for.
     * @param defaultValue The default value to use, in case the key is not associated.
     * @return The value associated with the key or defaultValue in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    // FIXME: limitation of excel
    public String getComplete(String key, String defaultValue) {
        if (key == null) return null;
        if (get(key) == null) return defaultValue;
        StringBuilder sb = new StringBuilder(get(key));
        int index = 1;
        while (get(key + " (split-" + index + ")") != null) {
            String s = get(key + " (split-" + index + ")");
            sb.append(s);
            index++;
        }
        return sb.toString();
    }

    /**
     * Function to check whether a key exists.
     *
     * @param key The key to check for.
     * @return <code>true</code> if the key exists, <code>false</code> if not.
     */
    public boolean has(String key) {
        return get(key) != null;
    }

    /**
     * Function to check for a boolean value.
     *
     * @param key The key to get the boolean value for.
     * @return The boolean value associated with the key. Returns <code>false</code> in case the associated value is
     * not equivalent to <code>Boolean.TRUE</code> or in case key is <code>null</code>.
     */
    public boolean is(String key) {
        return is(key, false);
    }

    /**
     * Function to check for a boolean value.
     *
     * @param key          The key to get the boolean value for.
     * @param defaultValue he default value to use, in case the key is not associated.
     * @return The boolean value associated with the key. Returns defaultValue in case the associated value is
     * not equivalent to <code>Boolean.TRUE</code> and <code>false</code> in case key is <code>null</code>.
     */
    public boolean is(String key, boolean defaultValue) {
        if (key == null) return defaultValue;
        final String currentValue = attributeMap.get(key);
        return (currentValue != null) ? Boolean.TRUE.toString().equalsIgnoreCase(currentValue) : defaultValue;
    }

    /**
     * Set the value for a key.
     *
     * @param key   The key to set the value for.
     * @param value The value to set for the key.
     */
    public void set(String key, String value) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalStateException("Attribute key must be defined.");
        }
        if (StringUtils.isBlank(value)) {
            attributeMap.remove(key);
        } else {
            attributeMap.put(key, value);
        }
    }

    /**
     * Safely set the value for a key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.
     * This method bypasses this by creating multiple columns for the same value.<br>
     * See {@link #setComplete(String, String)} for an example.
     *
     * @param key   The key to set the value for.
     * @param value The value to set for the key.
     */
    public void setComplete(Attribute key, String value) {
        setComplete(key.getKey(), value);
    }

    /**
     * Safely set the value for a key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.
     * This method bypasses this by creating multiple columns for the same value.<br>
     * Example: A string with the length of 100000 characters should be saved to <code>Vulnerability</code>.<br>
     * <table>
     *     <caption>Splitting attributes over several columns</caption>
     *     <tr>
     *         <td><code>Vulnerability</code></td>
     *         <td><code>Vulnerability (split-1)</code></td>
     *         <td><code>Vulnerability (split-2)</code></td>
     *         <td><code>Vulnerability (split-3)</code></td>
     *     </tr>
     *     <tr>
     *         <td><code>32760 characters</code></td>
     *         <td><code>32760 characters</code></td>
     *         <td><code>32760 characters</code></td>
     *         <td><code>1720 characters</code></td>
     *     </tr>
     * </table>
     *
     * @param key   The key to set the value for.
     * @param value The value to set for the key.
     */
    // FIXME: limitation of excel, move into writers, letting them handle the issue when reading and writing
    public void setComplete(String key, String value) {
        // clear the current value before writing the new value
        int index = 1;
        while (get(key + " (split-" + index + ")") != null) {
            set(key + " (split-" + index + ")", null);
            index++;
        }

        // if the new value is null, no need to split the value
        if (value == null) {
            set(key, null);
            return;
        }

        // if the content is longer than the maximum cell length, it has to be split up into multiple cells
        if (value.length() <= AbstractXlsxInventoryWriter.MAX_CELL_LENGTH) {
            set(key, value);
        } else {
            index = 0;
            while (index * AbstractXlsxInventoryWriter.MAX_CELL_LENGTH < value.length()) {
                String part = value.substring(index * AbstractXlsxInventoryWriter.MAX_CELL_LENGTH, Math.min(value.length(), (index + 1) * AbstractXlsxInventoryWriter.MAX_CELL_LENGTH));
                if (index == 0) set(key, part);
                else set(key + " (split-" + index + ")", part);
                index++;
            }
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
            if (StringUtils.isBlank(value)) {
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
        final String stringValue = get(key);
        if (stringValue == null || stringValue.trim().isEmpty()) return defaultValue;
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

    public String getAlternatives(String... keys) {
        for (final String key : keys) {
            final String value = get(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
