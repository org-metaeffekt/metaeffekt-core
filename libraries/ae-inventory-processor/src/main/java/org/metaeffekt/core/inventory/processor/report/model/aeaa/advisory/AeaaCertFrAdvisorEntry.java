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
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.advisory.CertFrAdvisorEntry</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaCertFrAdvisorEntry extends AeaaAdvisoryEntry {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaCertFrAdvisorEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
    }};


    protected final static String CERT_FR_BASE_URL = "https://www.cert.ssi.gouv.fr/";

    public AeaaCertFrAdvisorEntry() {
        super(AeaaAdvisoryTypeStore.CERT_FR);
    }

    public AeaaCertFrAdvisorEntry(String id) {
        super(AeaaAdvisoryTypeStore.CERT_FR, id);
    }

    protected String getBaseType() {
        if (id == null) return null;

        final String[] parts = id.split("-");
        if (parts.length < 4) return null;

        switch (parts[2]) {
            case "AVI":
                return "avis";
            case "ALE":
                return "alerte";
            case "IOC":
                return "ioc";
            case "DUR":
                return "dur";
            case "ACT":
                return "actualite";
            case "CTI":
                return "cti";
            case "REC":
            case "INF":
                return "information";
            default:
                return null;
        }
    }

    public String getType() {
        return AdvisoryUtils.normalizeType(getBaseType());
    }

    @Override
    public String getUrl() {
        if (id == null) return CERT_FR_BASE_URL;

        final String[] parts = id.split("-");
        if (parts.length < 4) return CERT_FR_BASE_URL;

        final StringJoiner onlineCertFr = new StringJoiner("-");
        for (int i = 0; i < parts.length; i++) {
            if (i < 4) {
                if (i == 3 && parts[i].matches("\\d+") && parts[i].length() == 2) {
                    parts[i] = "0" + parts[i];
                }
                onlineCertFr.add(parts[i]);
            }
        }

        final String type = getBaseType();
        if (type == null) return CERT_FR_BASE_URL;
        return CERT_FR_BASE_URL + type + "/" + onlineCertFr;
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

    public static AeaaCertFrAdvisorEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, AeaaCertFrAdvisorEntry::new);
    }

    public static AeaaCertFrAdvisorEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, AeaaCertFrAdvisorEntry::new);
    }

    public static AeaaCertFrAdvisorEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, AeaaCertFrAdvisorEntry::new);
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
