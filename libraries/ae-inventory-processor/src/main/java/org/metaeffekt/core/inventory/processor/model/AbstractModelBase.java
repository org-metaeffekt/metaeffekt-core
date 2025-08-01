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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Abstract model base class. Support associating key and values as generic concept. Keys can be bound to enums specified
 * by the subclass.
 */
public abstract class AbstractModelBase implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractModelBase.class);

    private static final Pattern PATH_DELIMITER_REGEXP = Pattern.compile("\\|\n");

    public static final String PATH_DELIMITER = "|\n";

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

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

    public final String get(final String key) {
        return get(key, null);
    }

    /**
     * Get the value associated with the given key. Use defaultValue, in case the key is not associated with any value.
     *
     * @param key          The key to get the value for.
     * @param defaultValue The default value to use, in case the key is not associated.
     *
     * @return The value associated with the key or defaultValue in case no value is associated. In case key is
     * <code>null</code> also <code>null</code> is returned.
     */
    public final String get(final String key, final String defaultValue) {
        if (key == null) return null;
        final String currentValue = attributeMap.get(key);
        return (currentValue != null) ? currentValue : defaultValue;
    }

    public final String get(final Attribute key) {
        return get(key.getKey(), null);
    }

    public final String get(final Attribute key, final String defaultValue) {
        return get(key.getKey(), defaultValue);
    }

    /**
     * Function to check whether a key exists.
     *
     * @param key The key to check for.
     *
     * @return <code>true</code> if the key exists, <code>false</code> if not.
     */
    public final boolean has(final String key) {
        return get(key) != null;
    }

    /**
     * Function to check for a boolean value.
     *
     * @param key The key to get the boolean value for.
     *
     * @return The boolean value associated with the key. Returns <code>false</code> in case the associated value is
     * not equivalent to <code>Boolean.TRUE</code> or in case key is <code>null</code>.
     */
    public final boolean is(final String key) {
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
    public final boolean is(final String key, final boolean defaultValue) {
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

    public void set(Attribute key, String value) {
        if (key == null) {
            throw new IllegalStateException("Attribute key must be defined.");
        }
        if (StringUtils.isBlank(value)) {
            attributeMap.remove(key.getKey());
        } else {
            attributeMap.put(key.getKey(), value);
        }
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
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #get(Attribute)} instead.
     */
    @Deprecated
    public String getComplete(Attribute key) {
        return get(key.getKey(), null);
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
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #get(Attribute, String)} instead.
     */
    @Deprecated
    public String getComplete(Attribute key, String defaultValue) {
        return get(key.getKey(), defaultValue);
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
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #get(String)} instead.
     */
    @Deprecated
    public String getComplete(String key) {
        return get(key, null);
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
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #get(String, String)} instead.
     */
    @Deprecated
    public String getComplete(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Safely set the value for a key.<br>
     * Excel limits the maximum cell length to <code>32767</code> characters.
     * This method bypasses this by creating multiple columns for the same value.<br>
     * See {@link #setComplete(String, String)} for an example.
     *
     * @param key   The key to set the value for.
     * @param value The value to set for the key.
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #set(Attribute, String)} instead.
     */
    @Deprecated
    public void setComplete(Attribute key, String value) {
        set(key.getKey(), value);
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
     * @deprecated No longer required. Limitation is handled by readers and writers. Use {@link #set(String, String)} instead.
     */
    @Deprecated
    public void setComplete(String key, String value) {
        set(key, value);
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

    public void logModelAttributesVertical() {
        logModelAttributesVertical(40, -1);
    }

    public void logModelAttributesVertical(int keyLength) {
        logModelAttributesVertical(keyLength, -1);
    }

    protected void logModelAttributesVertical(int keyIndent, int valueIndent) {
        for (String attribute : this.getAttributes()) {
            LOG.info("| {} | {}", StringUtils.rightPad(attribute, keyIndent), valueIndent == -1 ? get(attribute) : StringUtils.rightPad(get(attribute), valueIndent) + " |");
        }
    }

    public Set<String> getSet(Artifact.Attribute attribute) {
        return createSetForKey(get(attribute));
    }

    public Set<String> getSet(String key) {
        return createSetForKey(get(key));
    }

    private Set<String> createSetForKey(String pathsString) {
        if (StringUtils.isEmpty(pathsString)) {
            return Collections.emptySet();
        }
        return Arrays.stream(PATH_DELIMITER_REGEXP.split(pathsString)).
                map(String::trim).collect(Collectors.toSet());
    }

}
