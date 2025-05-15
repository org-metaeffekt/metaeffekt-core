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
import java.util.Map;

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
        if (indexTimestamps.containsKey(indexId)) {
            indexTimestamps.get(indexId).addTimestamp(lastChecked);
        } else {
            indexTimestamps.put(indexId, new ProcessTimestamp(lastChecked));
        }
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject()
                .put("processId", processId)
                .put("timestamp", timestamp.toJson());
        final JSONArray indexes = new JSONArray();
        for (Map.Entry<String, ProcessTimestamp> entry : indexTimestamps.entrySet()) {
            indexes.put(new JSONObject()
                    .put("indexId", entry.getKey())
                    .put("timestamp", entry.getValue().toJson()));
        }
        json.put("indexTimestamps", indexes);
        return json;
    }

    public static ProcessTimeEntry fromJson(JSONObject json) {
        final String processIdString = json.getString("processId");
        final ProcessId processId;
        try {
            processId = ProcessId.fromText(processIdString);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Process Id [%s] is not recognized", processIdString));
        }

        final ProcessTimeEntry tracker = new ProcessTimeEntry(processId, ProcessTimestamp.fromJSON(json.getJSONObject("timestamp")));
        if (json.getJSONArray("indexTimestamps") != null) {
            final JSONArray indexTimestamps = json.getJSONArray("indexTimestamps");
            for (int i = 0; i < indexTimestamps.length(); i++) {
                JSONObject index = indexTimestamps.getJSONObject(i);
                ProcessTimestamp timestamp = ProcessTimestamp.fromJSON(index.getJSONObject("timestamp"));
                tracker.indexTimestamps.put(index.getString("indexId"), timestamp);
            }
        }
        return tracker;
    }

    public void addAll(ProcessTimeEntry other) {
        this.setTimestamp(ProcessTimestamp.merged(this.getTimestamp(), other.getTimestamp()));

        final Map<String, ProcessTimestamp> mergedTimestamps = new HashMap<>(other.getIndexTimestamps());
        this.indexTimestamps.forEach((key, value) ->
                mergedTimestamps.merge(key, value, ProcessTimestamp::merged)
        );

        this.setIndexTimestamps(mergedTimestamps);
    }
}
