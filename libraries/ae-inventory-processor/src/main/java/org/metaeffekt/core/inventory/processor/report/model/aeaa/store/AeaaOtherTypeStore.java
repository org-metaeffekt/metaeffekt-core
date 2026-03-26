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

import org.json.JSONArray;

import java.util.*;
import java.util.regex.Pattern;

public class AeaaOtherTypeStore extends AeaaContentIdentifierStore<AeaaOtherTypeIdentifier> {

    @Deprecated
    public final static AeaaOtherTypeIdentifier CWE = new AeaaOtherTypeIdentifier("CWE", "CWE", "",
            Pattern.compile("(CWE-\\d+)", Pattern.CASE_INSENSITIVE));
    @Deprecated
    public final static AeaaOtherTypeIdentifier CAPEC = new AeaaOtherTypeIdentifier("CAPEC", "CAPEC", "",
            Pattern.compile("(CAPEC-\\d+)", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier CPE = new AeaaOtherTypeIdentifier("CPE", "CPE", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier NVD = new AeaaOtherTypeIdentifier("NVD", "NVD", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier ASSESSMENT_STATUS = new AeaaOtherTypeIdentifier("ASSESSMENT_STATUS", "Assessment Status", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier EOL = new AeaaOtherTypeIdentifier("EOL", "endoflife.date", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier EPSS = new AeaaOtherTypeIdentifier("EPSS", "EPSS", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier KEV = new AeaaOtherTypeIdentifier("KEV", "KEV", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));

    private final static AeaaOtherTypeStore INSTANCE = new AeaaOtherTypeStore();

    public static AeaaOtherTypeStore get() {
        return INSTANCE;
    }

    protected AeaaOtherTypeStore() {
        super(AeaaOtherTypeIdentifier.class);
    }

    // CHECK FOR CWE AND CAPEC
    public Map<AeaaContentIdentifierStore.AeaaContentIdentifier, Set<String>> fromJsonMultipleReferencedIdsConvertDeprecated(JSONArray json) {
        Map<AeaaOtherTypeIdentifier, Set<String>> otherTypeIdentifierSetMap = super.fromJsonMultipleReferencedIds(json);

        return extractAndConvertDeprecated(otherTypeIdentifierSetMap);
    }

    public Map<AeaaContentIdentifierStore.AeaaContentIdentifier, Set<String>> fromListMultipleReferencedIdsConvertDeprecated(List<Map<String, Object>> map) {
        final Map<AeaaOtherTypeIdentifier, Set<String>> referencedIds = new HashMap<>();

        for (Map<String, Object> entry : map) {
            final AeaaSingleContentIdentifierParseResult<AeaaOtherTypeIdentifier> pair = fromMap(entry);
            final AeaaOtherTypeIdentifier identifier = pair.getIdentifier();
            final String id = pair.getId();

            referencedIds.computeIfAbsent(identifier, k -> new HashSet<>()).add(id);
        }

        return extractAndConvertDeprecated(referencedIds);
    }

    private Map<AeaaContentIdentifier, Set<String>> extractAndConvertDeprecated(Map<AeaaOtherTypeIdentifier, Set<String>> otherTypeIdentifierSetMap) {
        Map<AeaaContentIdentifier, Set<String>> otherAndThreatTypeIdentifierSetMap = new HashMap<>(otherTypeIdentifierSetMap);

        Set<String> referencedCwes = otherAndThreatTypeIdentifierSetMap.get(CWE);
        if (referencedCwes != null) {
            otherAndThreatTypeIdentifierSetMap.remove(CWE);
            otherAndThreatTypeIdentifierSetMap.put(AeaaThreatTypeStore.CWE, referencedCwes);
        }

        Set<String> referencedCapecs = otherAndThreatTypeIdentifierSetMap.get(CAPEC);
        if (referencedCapecs != null) {
            otherAndThreatTypeIdentifierSetMap.remove(CAPEC);
            otherAndThreatTypeIdentifierSetMap.put(AeaaThreatTypeStore.CAPEC, referencedCapecs);
        }

        return otherAndThreatTypeIdentifierSetMap;
    }

    @Override
    protected AeaaOtherTypeIdentifier createIdentifier(String name, String implementation) {
        return new AeaaOtherTypeIdentifier(name, AeaaContentIdentifierStore.AeaaContentIdentifier.deriveWellFormedName(name), implementation, Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE));
    }

}
