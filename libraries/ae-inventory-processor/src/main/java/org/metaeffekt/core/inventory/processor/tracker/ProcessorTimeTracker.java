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
package org.metaeffekt.core.inventory.processor.tracker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Getter
public class ProcessorTimeTracker {

    public final static String TIME_TRACKING_INVENTORY_INFO_ROW_KEY = "processor-time-tracker";
    public final static String TIME_TRACKING_INVENTORY_INFO_COL_KEY = "Timestamps";

    private final Inventory inventory;
    private final InventoryInfo inventoryInfo;

    @Getter
    private final List<ProcessTimeEntry> entries = new ArrayList<>();

    private ProcessorTimeTracker(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryInfo = inventory.findOrCreateInventoryInfo(TIME_TRACKING_INVENTORY_INFO_ROW_KEY);

        this.parse();
    }

    public static ProcessorTimeTracker fromInventory(Inventory inventory) {
        return new ProcessorTimeTracker(inventory);
    }

    public ProcessTimeEntry addTimestamp(ProcessTimeEntry newEntry) {
        for (ProcessTimeEntry entry : entries) {
            if (Objects.equals(entry.getProcessType(), newEntry.getProcessType())
                    && Objects.equals(entry.getProcessName(), newEntry.getProcessName())) {
                entry.addAll(newEntry);
                this.writeBack();
                return entry;
            }
        }
        entries.add(newEntry);
        this.writeBack();
        return newEntry;
    }

    public ProcessTimeEntry getTimestamp(ProcessType processType) {
        return getTimestamp(processType, null);
    }

    public ProcessTimeEntry getTimestamp(ProcessType processType, String processName) {
        return entries.stream().filter(entry -> Objects.equals(entry.getProcessType(), processType) && Objects.equals(entry.getProcessName(), processName)).findFirst().orElse(null);
    }

    public ProcessTimeEntry getOrCreateTimestamp(ProcessType processType, long creationTimestamp) {
        return entries.stream()
                .filter(entry -> entry.getProcessType().equals(processType))
                .findFirst()
                .orElseGet(() -> addTimestamp(new ProcessTimeEntry(processType, creationTimestamp)));
    }

    public ProcessTimeEntry getOrCreateTimestamp(ProcessType processType, String processName, long creationTimestamp) {
        if (processName == null) {
            return getOrCreateTimestamp(processType, creationTimestamp);
        }
        return entries.stream()
                .filter(entry -> entry.getProcessType().equals(processType) && Objects.equals(entry.getProcessName(), processName))
                .findFirst()
                .orElseGet(() -> addTimestamp(new ProcessTimeEntry(processType, processName, creationTimestamp)));
    }

    private boolean parse() {
        if (StringUtils.isBlank(inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY))) {
            return true;
        }

        try {
            final JSONArray json = new JSONArray(inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY));

            for (int i = 0; i < json.length(); i++) {
                final JSONObject object = json.getJSONObject(i);
                entries.add(ProcessTimeEntry.fromJson(object));
            }
        } catch (Exception e) {
            log.warn("Failed to parse process timestamps: {}", e.getMessage(), e);
            return false;
        }

        return true;
    }

    public void writeBack() {
        try {
            inventoryInfo.set(TIME_TRACKING_INVENTORY_INFO_COL_KEY, toJson().toString());
        } catch (Exception e) {
            log.warn("Failed to add timestamp to time tracker");
        }
    }

    public JSONArray toJson() {
        return new JSONArray(entries.stream().map(ProcessTimeEntry::toJson).collect(Collectors.toList()));
    }

    public static ProcessorTimeTracker merge(Inventory inventory, ProcessorTimeTracker tracker1, ProcessorTimeTracker tracker2) {
        final ProcessorTimeTracker merged = new ProcessorTimeTracker(inventory);

        tracker1.getEntries().forEach(merged::addTimestamp);
        tracker2.getEntries().forEach(merged::addTimestamp);

        return merged;
    }
}
