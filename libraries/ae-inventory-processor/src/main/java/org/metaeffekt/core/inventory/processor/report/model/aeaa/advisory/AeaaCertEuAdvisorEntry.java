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

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AeaaCertEuAdvisorEntry extends AeaaAdvisoryEntry {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaCertEuAdvisorEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
    }};


    public AeaaCertEuAdvisorEntry() {
        super(AeaaAdvisoryTypeStore.CERT_EU);
    }

    public AeaaCertEuAdvisorEntry(String id) {
        super(AeaaAdvisoryTypeStore.CERT_EU, id);
    }

    @Override
    public String getUrl() {
        return "https://cert.europa.eu/publications/security-advisories/" + id.replace("CERT-EU-", "");
    }

    @Override
    public String getType() {
        return AdvisoryUtils.normalizeType("alert");
    }

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static AeaaCertEuAdvisorEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, AeaaCertEuAdvisorEntry::new);
    }

    public static AeaaCertEuAdvisorEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, AeaaCertEuAdvisorEntry::new);
    }

    public static AeaaCertEuAdvisorEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, AeaaCertEuAdvisorEntry::new);
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);
    }

    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);
    }
}
