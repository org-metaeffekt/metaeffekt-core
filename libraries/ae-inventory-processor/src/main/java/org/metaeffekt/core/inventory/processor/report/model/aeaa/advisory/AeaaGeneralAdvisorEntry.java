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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaContentIdentifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AeaaGeneralAdvisorEntry extends AeaaAdvisoryEntry {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaGeneralAdvisorEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
    }};


    public AeaaGeneralAdvisorEntry() {
        super(AeaaContentIdentifiers.UNKNOWN_ADVISORY);
    }

    public AeaaGeneralAdvisorEntry(String id) {
        super(AeaaContentIdentifiers.UNKNOWN_ADVISORY, id);
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getType() {
        return AdvisoryUtils.normalizeType("alert");
    }

    /* TYPE CONVERSION METHODS */

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static AeaaGeneralAdvisorEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, AeaaGeneralAdvisorEntry::new);
    }

    public static AeaaGeneralAdvisorEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, AeaaGeneralAdvisorEntry::new);
    }

    public static AeaaGeneralAdvisorEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, AeaaGeneralAdvisorEntry::new);
    }

    protected static String stringOrNull(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    protected void inferSourceFromInput(String source) {
        final AeaaContentIdentifiers inferredSource = AeaaContentIdentifiers.extractAdvisorySourceFromSourceOrName(this.getId(), source);
        if (LOG.isDebugEnabled() && !inferredSource.name().equals(this.source)) {
            LOG.debug("Inferred source differs from originally assigned [{}] --> [{}]", this.source, inferredSource.name());
        }
        this.source = inferredSource.name();
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);
        this.inferSourceFromInput(amd.get(AdvisoryMetaData.Attribute.SOURCE));
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
    }

    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);
        this.inferSourceFromInput(stringOrNull(map.get(AdvisoryMetaData.Attribute.SOURCE.getKey())));
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);
    }

}
