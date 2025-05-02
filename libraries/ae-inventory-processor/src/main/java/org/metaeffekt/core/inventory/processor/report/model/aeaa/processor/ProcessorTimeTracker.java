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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public ProcessorTimeTracker(Inventory inventory) {
        this.inventory = inventory;
        this.inventoryInfo = inventory.findOrCreateInventoryInfo(TIME_TRACKING_INVENTORY_INFO_ROW_KEY);

        parse();
        applyChanges();
    }

    public void addTimestamp(ProcessTimeEntry entry) {
        entries.add(entry);
        applyChanges();
    }

    public ProcessTimeEntry getTimestamp(String processId) {
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
                entries.add(ProcessTimeEntry.fromJSON(object));
            }

        } catch (Exception e) {
            log.error("Failed to parse correlation warnings.", e);
            throw new RuntimeException("Failed to parse correlation warnings from inventory: " + e.getMessage() + "\n" + inventoryInfo.get(TIME_TRACKING_INVENTORY_INFO_COL_KEY), e);
        }
    }


    public void applyChanges() {
        JSONArray jsonArray = new JSONArray();

        for (ProcessTimeEntry entry : entries) {
            jsonArray.put(entry.toJSON());
        }

        inventoryInfo.set(TIME_TRACKING_INVENTORY_INFO_COL_KEY, jsonArray.toString());
    }

    public static ProcessorTimeTracker merge(Inventory inventory, ProcessorTimeTracker processorTimeTracker1, ProcessorTimeTracker processorTimeTracker2) {
        ProcessorTimeTracker merged = new ProcessorTimeTracker(inventory);
        List<ProcessTimeEntry> mergedEntries = new ArrayList<>();


        Set<String> processes = new HashSet<>();
        processes.addAll(processorTimeTracker1.getEntries().stream().map(ProcessTimeEntry::getProcessId).collect(Collectors.toSet()));
        processes.addAll(processorTimeTracker2.getEntries().stream().map(ProcessTimeEntry::getProcessId).collect(Collectors.toSet()));

        for (String processId : processes) {
            ProcessTimeEntry entry1 = processorTimeTracker1.getTimestamp(processId);
            ProcessTimeEntry entry2 = processorTimeTracker2.getTimestamp(processId);
            if (entry1 == null){
                mergedEntries.add(entry2);
            } else if (entry2 == null) {
                mergedEntries.add(entry1);
            } else {
                mergedEntries.add(ProcessTimeEntry.merge(entry1, entry2));
            }
        }
        merged.getEntries().addAll(mergedEntries);
        return merged;
    }

}
