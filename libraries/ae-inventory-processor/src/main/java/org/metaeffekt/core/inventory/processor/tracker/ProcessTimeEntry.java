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
package org.metaeffekt.core.inventory.processor.tracker;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ProcessTimeEntry {

    private String processId;

    private ProcessTimestamp timestamp;

    private Map<String, ProcessTimestamp> indexTimestamps = new HashMap<>();

    public ProcessTimeEntry(ProcessId processId, long timestamp) {
        this.processId = processId.get();
        this.timestamp = new ProcessTimestamp(timestamp);
    }

    private ProcessTimeEntry(ProcessId processId, ProcessTimestamp timestamp) {
        this.processId = processId.get();
        this.timestamp = timestamp;
    }

    public void addIndexTimestamp(String indexId, long lastChecked) {
        if(indexTimestamps.containsKey(indexId)){
            ProcessTimestamp indexStamp = indexTimestamps.get(indexId);
            indexStamp.addTimestamp(lastChecked);
        } else {
            indexTimestamps.put(indexId, new ProcessTimestamp(lastChecked));
        }
    }

     JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("processId", processId);
        object.put("timestamp", timestamp.toJson());
        JSONArray indexes = new JSONArray();
        for (Map.Entry<String, ProcessTimestamp> entry : indexTimestamps.entrySet()) {
            JSONObject index = new JSONObject();
            index.put("indexId", entry.getKey());
            index.put("timestamp", entry.getValue().toJson());
            indexes.put(index);
        }
        object.put("indexTimestamps", indexes);
        return object;
    }

    static ProcessTimeEntry fromJSON(JSONObject object) {
        String processIdString = object.getString("processId");
        ProcessId processId;
        try{
            processId = ProcessId.fromText(processIdString);
        } catch (IllegalArgumentException e){
            throw new RuntimeException(String.format("Process Id [%s] is not recognized", processIdString));
        }

        ProcessTimeEntry tracker = new ProcessTimeEntry(processId, ProcessTimestamp.fromJSON(object.getJSONObject("timestamp")));
        if(object.getJSONArray("indexTimestamps") != null) {
            JSONArray indexTimestamps = object.getJSONArray("indexTimestamps");
            for (int i = 0; i < indexTimestamps.length(); i++) {
                JSONObject index = indexTimestamps.getJSONObject(i);
                ProcessTimestamp timestamp = ProcessTimestamp.fromJSON(index.getJSONObject("timestamp"));
                tracker.indexTimestamps.put(index.getString("indexId"), timestamp);
            }
        }
        return tracker;
    }

    void merge(ProcessTimeEntry other) {
        this.setTimestamp(ProcessTimestamp.merged(this.getTimestamp(), other.getTimestamp()));

        Map<String, ProcessTimestamp>  mergedTimestamps = new HashMap<>();
        Set<String> indices = new HashSet<>();
        Map<String, ProcessTimestamp> indexTimestamps1 = this.getIndexTimestamps();
        Map<String, ProcessTimestamp> indexTimestamps2 = other.getIndexTimestamps();
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

        this.setIndexTimestamps(mergedTimestamps);
    }
}
