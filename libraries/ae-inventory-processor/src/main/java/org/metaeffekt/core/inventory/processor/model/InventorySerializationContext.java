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

import java.util.HashMap;
import java.util.Map;
public class InventorySerializationContext {

    public static final String CONTEXT_KEY_ASSET_DATA = "asset";
    public static final String CONTEXT_KEY_ARTIFACT_DATA = "artifact";
    public static final String CONTEXT_KEY_LICENSE_DATA = "license";
    public static final String CONTEXT_KEY_REPORT_DATA = "report";
    public static final String CONTEXT_KEY_COMPONENT_PATTERN_DATA = "report";
    public static final String CONTEXT_KEY_VULNERABILITY_DATA = "vulnerability";
    public static final String CONTEXT_KEY_ADVISORY_DATA = "advisory";
    public static final String CONTEXT_KEY_LICENSE_NOTICE_DATA = "notices";

    /**
     * Key for context map. Stores Artifact column names in the order that is used when writing the
     * inventory.
     */
    public static final String CONTEXT_ARTIFACT_COLUMN_LIST = "artifact.columnlist";

    /**
     * Key for context map. Stores the LicenseData column names in the order that is used when writing the
     * inventory.
     */
    public static final String CONTEXT_LICENSEDATA_COLUMN_LIST = "licensedata.columnlist";

    /**
     * The context map stores metadata on excel file level, including
     * <ul>
     *     <li>order of columns in a sheet</li>
     *     <li>column width</li>
     * </ul>
     */
    private Map<String, Object> contextMap = new HashMap<>();

    /**
     * Put a key-value-pair into the context map.
     *
     * @param key The key.
     * @param value The value.
     *
     * @return The previous value.
     */
    public Object put(String key, Object value) {
        return contextMap.put(key, value);
    }

    /**
     * Get the value of a given key.
     *
     * @param key The key.
     *
     * @return The value currently stored for key in the context map.
     */
    public <T> T get(String key) {
        return (T) contextMap.get(key);
    }

    /**
     * Get the value for a given key supporting a default value in case the current value in <code>null</code>.
     *
     * @param key The key.
     * @param defaultValue The default value.
     *
     * @return The value in the map or the default value.
     */
    public <T> T getOrDefaultTo(String key, T defaultValue) {
        final T value = (T) contextMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Only kept for legacy purposes. Should disappear from external interface.
     *
     * @return The current context map.
     */
    @Deprecated
    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    /**
     * Only kept for legacy purposes. Should disappear from external interface.
     *
     * @param contextMap The contextMap to set.
     */
    @Deprecated
    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

}
