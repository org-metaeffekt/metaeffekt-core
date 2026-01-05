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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.export;

import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.AeaaEolCycle;

/**
 * Enum representing the possible scenarios for a cycle's state.
 */
public enum AeaaCycleStateScenario {
    EXTENDED_SUPPORT_NOT_PRESENT("extendedSupportNotPresent"),
    EXTENDED_SUPPORT_INFORMATION_PRESENT("extendedSupportInformationPresent");

    private final String key;

    AeaaCycleStateScenario(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static AeaaCycleStateScenario from(AeaaEolCycle cycle) {
        if (cycle.isExtendedSupportGenerallyAvailable()) {
            return EXTENDED_SUPPORT_INFORMATION_PRESENT;
        } else {
            return EXTENDED_SUPPORT_NOT_PRESENT;
        }
    }

    public static AeaaCycleStateScenario fromKey(String key) {
        for (AeaaCycleStateScenario value : values()) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant for key: " + key);
    }
}
