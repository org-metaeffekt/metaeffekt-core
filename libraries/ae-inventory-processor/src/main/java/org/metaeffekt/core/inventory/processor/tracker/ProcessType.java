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
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public enum ProcessType {
    SPDX_IMPORTER("spdx-importer", Collections.emptyList()),
    CYCLONEDX_IMPORTER("cyclonedx-importer", Collections.emptyList()),
    SBOM_CREATION("sbom-creation", Arrays.asList("spdx-bom", "cyclonedx-bom")),
    INVENTORY_ENRICHMENT("inventory-enrichment", Collections.emptyList()),
    ADVISOR_PERIODIC_ENRICHMENT("advisor-periodic-enrichment", Collections.emptyList()),
    INVENTORY_MERGER("inventory-merger", Collections.emptyList()),
    REPORT_GENERATION("report-generation", Collections.emptyList()),
    ;

    final String id;
    final List<String> deprecatedNames;

    public String get() {
        return id;
    }

    public static ProcessType fromText(String text) {
        return Arrays.stream(ProcessType.values())
                .filter(v -> v.id.equals(text) || v.deprecatedNames.contains(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + text));
    }
}
