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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ThreatCategory {
    WEAKNESS_IMPLEMENTATION("WEAKNESS"),
    THREAT_IMPLEMENTATION("THREAT"),
    ATTACK_PATTERN_IMPLEMENTATION("ATTACK_PATTERN");

    public final String threatCategory;

    public String getKey() {
        return this.threatCategory;
    }
}
