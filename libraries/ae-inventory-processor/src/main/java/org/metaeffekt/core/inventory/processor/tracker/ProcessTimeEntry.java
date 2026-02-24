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
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ProcessTimeEntry {

    private final ProcessType processType;
    private String processName;

    private ProcessTimestamp timestamp;

    private Map<String, ProcessTimestamp> indexTimestamps = new HashMap<>();

    public ProcessTimeEntry(ProcessType processType, String processName, long timestamp) {
        this(processType, processName, new ProcessTimestamp(timestamp));
    }

    public ProcessTimeEntry(ProcessType processType, long timestamp) {
        this(processType, null, new ProcessTimestamp(timestamp));
    }

    private ProcessTimeEntry(ProcessType processType, String processName, ProcessTimestamp timestamp) {
        this.processType = processType;
        this.processName = processName;
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
                .put("processId", processType.get())
                .put("timestamp", timestamp.toJson());

        if (processName != null) {
            json.put("processName", processName);
        }

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
        final ProcessType processType;
        try {
            processType = ProcessType.fromText(processIdString);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Process Id [%s] is not recognized", processIdString));
        }

        final ProcessTimeEntry tracker = new ProcessTimeEntry(processType, json.optString("processName", null), ProcessTimestamp.fromJSON(json.getJSONObject("timestamp")));
        Object indexTimestampJsonProperty = json.opt("indexTimestamps");

        // legacy format
        if (indexTimestampJsonProperty instanceof JSONObject) {
            JSONObject indexTimestamps = (JSONObject) indexTimestampJsonProperty;
            for (String index : indexTimestamps.keySet()) {
                tracker.indexTimestamps.put(index, ProcessTimestamp.fromJSON(indexTimestamps.getJSONObject(index)));
            }
        } else if (indexTimestampJsonProperty instanceof JSONArray) {
            final JSONArray indexTimestamps = (JSONArray) indexTimestampJsonProperty;
            for (int i = 0; i < indexTimestamps.length(); i++) {
                JSONObject index = indexTimestamps.getJSONObject(i);
                ProcessTimestamp timestamp = ProcessTimestamp.fromJSON(index.getJSONObject("timestamp"));
                tracker.indexTimestamps.put(index.getString("indexId"), timestamp);
            }
        } else {
            throw new RuntimeException(String.format("Property: 'indexTimestamps' is not able to be parsed in [ {%s} ]", json));
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
