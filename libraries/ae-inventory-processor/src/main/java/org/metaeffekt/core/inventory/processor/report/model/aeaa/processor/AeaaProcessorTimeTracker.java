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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.processor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryInfo;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Getter
public class AeaaProcessorTimeTracker {

    public final static String TIME_TRACKING_INVENTORY_INFO_ROW_KEY = "processor-time-tracker";
    public final static String TIME_TRACKING_INVENTORY_INFO_COL_KEY = "Timestamps";

    private final Inventory inventory;
    private final InventoryInfo inventoryInfo;

    private final List<AeaaProcessTimeEntry> entries = new ArrayList<>();

    public AeaaProcessorTimeTracker(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryInfo = inventory.findOrCreateInventoryInfo(TIME_TRACKING_INVENTORY_INFO_ROW_KEY);

        parse();
        applyChanges();
    }

    public void addTimestamp(AeaaProcessTimeEntry entry) {
        entries.add(entry);
        applyChanges();
    }

    public AeaaProcessTimeEntry getTimestamp(String processId) {
        return entries.stream().filter(entry -> entry.getProcessId().equals(processId)).findFirst().orElse(null);
    }

    private void parse() {
        if (StringUtils.isBlank(inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY))) {
            return;
        }

        try {
            final JSONArray json = new JSONArray(inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY));

            for (int i = 0; i < json.length(); i++) {
                final JSONObject object = json.getJSONObject(i);
                entries.add(AeaaProcessTimeEntry.fromJSON(object));
            }

        } catch (Exception e) {
            log.error("Failed to parse correlation warnings.", e);
            throw new RuntimeException("Failed to parse correlation warnings from inventory: " + e.getMessage() + "\n" + inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY), e);
        }
    }


    public void applyChanges() {
        JSONArray jsonArray = new JSONArray();

        for (AeaaProcessTimeEntry entry : entries) {
            jsonArray.put(entry.toJSON());
        }

        inventoryInfo.set(TIME_TRACKING_INVENTORY_INFO_COL_KEY, jsonArray.toString());
    }

}
