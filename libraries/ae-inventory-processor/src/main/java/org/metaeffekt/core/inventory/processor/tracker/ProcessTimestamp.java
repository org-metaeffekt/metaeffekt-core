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
import org.json.JSONObject;

@Getter
public class ProcessTimestamp{

    private long first;
    private long last;

    protected ProcessTimestamp (long first, long last) {
        this.last = last;
        addTimestamp(first);
    }

    protected ProcessTimestamp(long last) {
        this.last = last;
    }

    protected ProcessTimestamp() {}

    protected void addTimestamp(long timestamp) {
        if(timestamp >= last) {
            if(first == 0){
                first = last;
            }
            last = timestamp;
        } else if((timestamp < first || first == 0) && timestamp != 0) {
            first = timestamp;
        }
    }

    protected static ProcessTimestamp  fromJSON(JSONObject json) {
        ProcessTimestamp tracker = new ProcessTimestamp();
        tracker.first = json.getLong("first");
        tracker.last = json.getLong("last");
        return tracker;
    }

    protected JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("first", first);
        object.put("last", last);
        return object;
    }

    protected static ProcessTimestamp merged(ProcessTimestamp timestamp1, ProcessTimestamp timestamp2) {
        ProcessTimestamp copy = copy(timestamp1);

        copy.addTimestamp(timestamp2.first);
        copy.addTimestamp(timestamp2.last);
        return copy;
    }

    private static ProcessTimestamp copy(ProcessTimestamp timestamp) {
        return new ProcessTimestamp(timestamp.first, timestamp.last);
    }
}
