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

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum ProcessId {
    SPDX_IMPORTER("spdx-importer"),
    CYCLONEDX_IMPORTER("cyclonedx-importer"),
    SBOM_CREATION("sbom-creation"),
    INVENTORY_ENRICHMENT("inventory-enrichment"),
    ADVISOR_PERIODIC_ENRICHMENT("advisor-periodic-enrichment"),
    INVENTORY_MERGER("inventory-merger"),
    REPORT_GENERATION("report-generation"),
    ;

    final String id;

    public String get() {
        return id;
    }

    public static ProcessId fromText(String text) {
        return Arrays.stream(ProcessId.values())
                .filter(v -> v.id.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + text));
    }
}
