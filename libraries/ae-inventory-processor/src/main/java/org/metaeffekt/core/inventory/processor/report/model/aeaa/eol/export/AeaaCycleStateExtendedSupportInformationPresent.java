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

import lombok.Getter;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.AeaaEolCycle;

@Getter
public enum AeaaCycleStateExtendedSupportInformationPresent {
    SUPPORT_VALID("supportValid", CycleScenarioRating.RATING_1),
    SUPPORT_ENDING_SOON("supportEndingSoon", CycleScenarioRating.RATING_2),
    EXTENDED_SUPPORT_VALID("extendedSupportValid", CycleScenarioRating.RATING_3),
    EXTENDED_SUPPORT_ENDING_SOON("extendedSupportEnding", CycleScenarioRating.RATING_4),
    EXTENDED_SUPPORT_EXPIRED("extendedSupportExpired", CycleScenarioRating.RATING_5);

    private final String key;
    private final CycleScenarioRating rating;

    AeaaCycleStateExtendedSupportInformationPresent(String key, CycleScenarioRating rating) {
        this.key = key;
        this.rating = rating;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("key", key)
                .put("rating", getRating().getRating());
    }

    public static AeaaCycleStateExtendedSupportInformationPresent from(AeaaEolCycle cycle, long millisUntilEol, long millisUntilExtendedSupportEndWarning) {
        final long timeUntilSupportEnd = cycle.getTimeUntilSupportEnd();
        final long timeUntilEol = cycle.getTimeUntilEol();
        final long timeUntilExtendedSupportEnd = cycle.getTimeUntilExtendedSupportEnd();

        final long effectiveTimeUntilExtendedSupportEnd = Math.max(timeUntilEol, timeUntilExtendedSupportEnd);

        // check if EOL has been reached
        if (effectiveTimeUntilExtendedSupportEnd <= 0) {
            return EXTENDED_SUPPORT_EXPIRED;
        }

        // check if regular support is available
        if (timeUntilSupportEnd > 0) {
            if (timeUntilSupportEnd < millisUntilEol) {
                return SUPPORT_ENDING_SOON;
            } else {
                return SUPPORT_VALID;
            }
        }

        // check if extended support is available
        if (effectiveTimeUntilExtendedSupportEnd < millisUntilExtendedSupportEndWarning) {
            return EXTENDED_SUPPORT_ENDING_SOON;
        } else {
            return EXTENDED_SUPPORT_VALID;
        }

        // extended support has expired, but that has already been checked for above
    }

    public static AeaaCycleStateExtendedSupportInformationPresent fromJson(JSONObject jsonObject) {
        final String key = jsonObject.getString("key");
        for (AeaaCycleStateExtendedSupportInformationPresent value : values()) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant for key: " + key);
    }
}
