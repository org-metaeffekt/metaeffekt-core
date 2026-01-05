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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
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

    private String url;
    private String type = "alert";


    public AeaaGeneralAdvisorEntry() {
        super(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("UNKNOWN", "UNKNOWN"));
    }

    public AeaaGeneralAdvisorEntry(String id) {
        super(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("UNKNOWN", "UNKNOWN"), id);
    }

    public AeaaGeneralAdvisorEntry(AeaaAdvisoryTypeIdentifier<?> source) {
        super(source);
    }

    public AeaaGeneralAdvisorEntry(AeaaAdvisoryTypeIdentifier<?> source, String id) {
        super(source, id);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getType() {
        return AdvisoryUtils.normalizeType(type);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);

        this.url = stringOrNull(amd.get(AdvisoryMetaData.Attribute.URL));

        if (StringUtils.isNotEmpty(amd.get(AdvisoryMetaData.Attribute.TYPE))) {
            this.type = String.valueOf(amd.get(AdvisoryMetaData.Attribute.TYPE));
        }
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
    }

    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);

        this.url = stringOrNull(map.get("url"));

        if (StringUtils.isNotEmpty(String.valueOf(map.get("type")))) {
            this.type = String.valueOf(map.get("type"));
        }
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);
    }
}
