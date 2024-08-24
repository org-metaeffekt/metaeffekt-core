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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.export;

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.AeaaEolCycle;

public enum AeaaCycleStateExtendedSupportInformationNotPresent {
    SUPPORT_VALID("supportValid", CycleScenarioRating.RATING_1),
    SUPPORT_ENDING_SOON("supportEndingSoon", CycleScenarioRating.RATING_4),
    SUPPORT_EXPIRED("supportExpired", CycleScenarioRating.RATING_5);

    private final String key;
    private final CycleScenarioRating rating;

    AeaaCycleStateExtendedSupportInformationNotPresent(String key, CycleScenarioRating rating) {
        this.key = key;
        this.rating = rating;
    }

    public String getKey() {
        return key;
    }

    public CycleScenarioRating getRating() {
        return rating;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("key", key)
                .put("rating", getRating().getRating());
    }

    public static AeaaCycleStateExtendedSupportInformationNotPresent from(AeaaEolCycle cycle, long millisUntilEol) {
        final long timeUntilSupportEnd = cycle.getTimeUntilSupportEnd();

        if (timeUntilSupportEnd <= 0) {
            return SUPPORT_EXPIRED;
        } else if (timeUntilSupportEnd < millisUntilEol) {
            return SUPPORT_ENDING_SOON;
        } else {
            return SUPPORT_VALID;
        }
    }

    public static AeaaCycleStateExtendedSupportInformationNotPresent fromJson(JSONObject jsonObject) {
        final String key = jsonObject.getString("key");
        for (AeaaCycleStateExtendedSupportInformationNotPresent value : values()) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant for key: " + key);
    }
}
