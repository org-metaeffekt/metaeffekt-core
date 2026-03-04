/*
 * Copyright 2009-2026 the original author or authors.
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

import java.util.*;

public class InventorySerializationContext {

    public static final String CONTEXT_KEY_ARTIFACT_DATA = "artifact";
    public static final String CONTEXT_KEY_ASSET_DATA = "asset";
    public static final String CONTEXT_KEY_COMPONENT_PATTERN_DATA = "report";
    public static final String CONTEXT_KEY_LICENSE_DATA = "license";
    public static final String CONTEXT_KEY_LICENSE_NOTICE_DATA = "notices";
    public static final String CONTEXT_KEY_VULNERABILITY_DATA = "vulnerability";
    public static final String CONTEXT_KEY_ADVISORY_DATA = "advisory";
    public static final String CONTEXT_KEY_THREAT_DATA = "threat";
    public static final String CONTEXT_KEY_WEAKNESS_DATA = "weakness";
    public static final String CONTEXT_KEY_ATTACK_PATTERN_DATA = "attack_pattern";
    public static final String CONTEXT_KEY_INFO_DATA = "info";
    public static final String CONTEXT_KEY_REPORT_DATA = "report";

    /**
     * Stores Artifact column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_ARTIFACT_DATA_COLUMN_LIST = CONTEXT_KEY_ARTIFACT_DATA + ".columnlist";

    /**
     * Stores LicenseMetaData column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_LICENSE_NOTICE_DATA_COLUMN_LIST = CONTEXT_KEY_LICENSE_NOTICE_DATA + ".columnlist";

    /**
     * Stores LicenseData column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_LICENSE_DATA_COLUMN_LIST = CONTEXT_KEY_LICENSE_DATA + ".columnlist";

    /**
     * Stores Vulnerability column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_VULNERABILITY_DATA_COLUMN_LIST = CONTEXT_KEY_VULNERABILITY_DATA + ".columnlist";

    /**
     * Stores Advisory column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_ADVISORY_DATA_COLUMN_LIST = CONTEXT_KEY_ADVISORY_DATA + ".columnlist";

    /**
     * Stores Asset column names in the order that is used when writing the inventory.
     */
    public static final String CONTEXT_ASSET_DATA_COLUMN_LIST = CONTEXT_KEY_ASSET_DATA + ".columnlist";


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
     * @param <T> Coerced type.
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
     * @param <T> Coerced type.
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

    private static List<String> initializeSerializationContext(Inventory inventory, List<? extends AbstractModelBase> objects, String serializationContextKey, List<String> defaultAttributes) {
        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();

        for (final AbstractModelBase vmd : objects) {
            attributes.addAll(vmd.getAttributes());
        }

        final InventorySerializationContext serializationContext = inventory.getSerializationContext();
        final List<String> contextColumnList = serializationContext.get(serializationContextKey);
        if (contextColumnList != null) {
            attributes.addAll(contextColumnList);
        }

        // add minimum columns
        attributes.addAll(defaultAttributes);

        // impose context or default order
        List<String> orderedAttributes = new ArrayList<>(attributes);
        Collections.sort(orderedAttributes);
        int insertIndex = 0;
        if (contextColumnList != null) {
            for (String key : contextColumnList) {
                insertIndex = reinsert(insertIndex, key, orderedAttributes, attributes);
            }
        } else {
            for (String key : defaultAttributes) {
                insertIndex = reinsert(insertIndex, key, orderedAttributes, attributes);
            }
        }

        serializationContext.put(serializationContextKey, orderedAttributes);
        return orderedAttributes;
    }

    private static int reinsert(int insertIndex, String key, List<String> orderedAttributesList, Set<String> attributesSet) {
        if (attributesSet.contains(key)) {
            orderedAttributesList.remove(key);
            orderedAttributesList.add(Math.min(insertIndex, orderedAttributesList.size()), key);
            insertIndex++;
        }
        return insertIndex;
    }

    public static void initializeSerializationContext(Inventory inventory) {
        initializeSerializationContext(inventory, inventory.getArtifacts(), CONTEXT_ARTIFACT_DATA_COLUMN_LIST, Artifact.MIN_ATTRIBUTES);
        initializeSerializationContext(inventory, inventory.getAdvisoryMetaData(), CONTEXT_ADVISORY_DATA_COLUMN_LIST, AdvisoryMetaData.MIN_ATTRIBUTES);
        initializeSerializationContext(inventory, Collections.emptyList(), CONTEXT_VULNERABILITY_DATA_COLUMN_LIST, VulnerabilityMetaData.MIN_ATTRIBUTES);
        initializeSerializationContext(inventory, inventory.getLicenseData(), CONTEXT_LICENSE_DATA_COLUMN_LIST, LicenseData.MIN_ATTRIBUTES);
        initializeSerializationContext(inventory, inventory.getLicenseMetaData(), CONTEXT_LICENSE_NOTICE_DATA_COLUMN_LIST, LicenseMetaData.MIN_ATTRIBUTES);
    }

    public SheetSerializationContext createArtifactSerializationContext(Inventory inventory) {
        return new SheetSerializationContext(inventory, CONTEXT_KEY_ARTIFACT_DATA, inventory::getArtifacts);
    }

    public SheetSerializationContext createAssetSerializationContext(Inventory inventory) {
        return new SheetSerializationContext(inventory, CONTEXT_KEY_ASSET_DATA, inventory::getAssetMetaData);
    }

    public SheetSerializationContext createAdvisorySerializationContext(Inventory inventory) {
        return new SheetSerializationContext(inventory, CONTEXT_KEY_ADVISORY_DATA, inventory::getAdvisoryMetaData);
    }

    public SheetSerializationContext createVulnerabilitySerializationContext(Inventory inventory, String assessmentContext) {
        return new SheetSerializationContext(inventory, CONTEXT_KEY_VULNERABILITY_DATA, () -> inventory.getVulnerabilityMetaData(assessmentContext));
    }

    public void clear() {
        contextMap.clear();
    }

}
