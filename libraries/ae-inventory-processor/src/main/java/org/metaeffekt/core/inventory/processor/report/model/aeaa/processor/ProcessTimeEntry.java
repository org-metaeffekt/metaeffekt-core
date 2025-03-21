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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class ProcessTimeEntry {

    private final String processId;

    private final ProcessTimestamp timestamp;

    private final Map<String, ProcessTimestamp> indexTimestamps = new HashMap<>();

    public ProcessTimeEntry(String processId, long timestamp) {
        this.processId = processId;
        this.timestamp = new ProcessTimestamp(timestamp);
    }

    private ProcessTimeEntry(String processId, ProcessTimestamp timestamp) {
        this.processId = processId;
        this.timestamp = timestamp;
    }

    public void addIndexTimestamp(String indexId, long lastChecked) {
        indexTimestamps.put(indexId, new ProcessTimestamp(lastChecked));
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("processId", processId);
        object.put("timestamp", timestamp.toJson());
        JSONObject indexes = new JSONObject();
        for (Map.Entry<String, ProcessTimestamp> entry : indexTimestamps.entrySet()) {
            indexes.put(entry.getKey(), entry.getValue().toJson());
        }
        object.put("indexTimestamps", indexes);
        return object;
    }

    public static ProcessTimeEntry fromJSON(JSONObject object) {
        ProcessTimeEntry tracker = new ProcessTimeEntry(object.getString("processId"), ProcessTimestamp.fromJSON(object.getJSONObject("timestamp")));
        if(object.getJSONObject("indexTimestamps") != null) {
            JSONObject indexTimestamps = object.getJSONObject("indexTimestamps");
            for(String index : indexTimestamps.keySet()) {
                tracker.indexTimestamps.put(index, ProcessTimestamp.fromJSON(indexTimestamps.getJSONObject(index)));
            }
        }
        return tracker;
    }

    public static ProcessTimeEntry merge(ProcessTimeEntry entry1, ProcessTimeEntry entry2) {
        ProcessTimeEntry merged = new ProcessTimeEntry(entry1.getProcessId(), ProcessTimestamp.merged(entry1.getTimestamp(), entry2.getTimestamp()));
        Map<String, ProcessTimestamp>  mergedTimestamps = new HashMap<>();
        Set<String> indices = new HashSet<>();
        Map<String, ProcessTimestamp> indexTimestamps1 = entry1.getIndexTimestamps();
        Map<String, ProcessTimestamp> indexTimestamps2 = entry2.getIndexTimestamps();
        indices.addAll(indexTimestamps1.keySet());
        indices.addAll(indexTimestamps2.keySet());
        for(String index : indices) {
            if(!indexTimestamps1.containsKey(index)) {
                mergedTimestamps.put(index, indexTimestamps2.get(index));
            } else if (!indexTimestamps2.containsKey(index)) {
                mergedTimestamps.put(index, indexTimestamps1.get(index));
            } else {
                mergedTimestamps.put(index, ProcessTimestamp.merged(indexTimestamps1.get(index), indexTimestamps2.get(index)));
            }
        }
        merged.indexTimestamps.putAll(mergedTimestamps);
        return merged;
    }
}
