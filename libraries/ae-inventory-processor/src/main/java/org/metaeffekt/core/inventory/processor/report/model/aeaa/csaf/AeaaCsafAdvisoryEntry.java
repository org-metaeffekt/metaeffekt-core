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

package org.metaeffekt.core.inventory.processor.report.model.aeaa.csaf;

import lombok.Setter;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaReference;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;

import java.util.HashSet;
import java.util.Set;

public class AeaaCsafAdvisoryEntry extends AeaaAdvisoryEntry {

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
        add("vulnerabilityDescriptions");
        add("csafType");
    }};

    @Setter private String csafType;


    public AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeIdentifier<?> source) {
        super(source);
    }

    @Override
    public String getUrl() {
        AeaaReference reference = references.stream().filter(f -> f.getTags().contains("SELF") && f.getUrl().endsWith(".json")).findFirst().orElse(null);
        if (reference != null) {
            return reference.getUrl();
        }
        return "";
    }

    @Override
    public String getType() {
        return csafType.replace("csaf_", "");
    }

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
        amd.set("csafType", csafType);
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);
        this.setCsafType(amd.get("csafType"));
    }
}
