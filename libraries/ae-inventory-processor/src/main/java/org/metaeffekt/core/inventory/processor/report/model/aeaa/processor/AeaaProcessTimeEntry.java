/*
 * Copyright 2021-2024 the original author or authors.
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
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@Getter
public class AeaaProcessTimeEntry {

    private final String processId;

    private final long timestamp;

    private final Map<String, Long> indexTimestamps = new HashMap<>();

    public AeaaProcessTimeEntry(String processId, long timestamp) {
        this.processId = processId;
        this.timestamp = timestamp;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("processId", processId);
        object.put("timestamp", timestamp);
        object.put("indexTimestamps", indexTimestamps);
        return object;
    }

    public static AeaaProcessTimeEntry fromJSON(JSONObject object) {
        AeaaProcessTimeEntry tracker = new AeaaProcessTimeEntry(object.getString("processId"), object.getLong("timestamp"));
        if(object.getJSONObject("indexTimestamps") != null) {
            JSONObject indexTimestamps = object.getJSONObject("indexTimestamps");
            for(String index : indexTimestamps.keySet()) {
                tracker.indexTimestamps.put(index, indexTimestamps.getLong(index));
            }
        }
        return tracker;
    }




}
