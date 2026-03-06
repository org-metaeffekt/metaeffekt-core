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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre;

import lombok.EqualsAndHashCode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AttackPatternMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaMatchableDetailsAmbDataClass;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeStore;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaThreatReference;

import java.util.*;
import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
public abstract class AeaaAttackPattern extends AeaaMatchableDetailsAmbDataClass<AttackPatternMetaData, AeaaAttackPattern> implements AeaaThreatReference {

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_AMB) {{
        addAll(Arrays.asList(
                AttackPatternMetaData.Attribute.ID.getKey(),
                AttackPatternMetaData.Attribute.CONTENT.getKey()
        ));
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_MAP) {{
        addAll(Arrays.asList(
                "id", "content"
        ));
    }};

    @Override
    public AttackPatternMetaData constructBaseModel() {
        return new AttackPatternMetaData();
    }

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static List<AeaaAttackPattern> fromJson(JSONArray jsonArray) {
        List<AeaaAttackPattern> attackPattern = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (AeaaThreatTypeStore.CAPEC.patternMatchesId(o.optString("id"))) {
                attackPattern.add(AeaaCapecEntry.fromJson(o));
            }
        }
        return attackPattern;
    }

    private static <T extends AeaaAttackPattern> T fromAttackPatternMetaData(AttackPatternMetaData atmd, Supplier<T> constructor) {
        if (atmd == null) return null;
        return constructor.get()
                .performAction(v -> v.appendFromBaseModel(atmd));
    }

    public static AeaaAttackPattern fromAttackPatternMetaData(AttackPatternMetaData atmd) {
        final AeaaThreatTypeIdentifier<? extends AeaaAttackPattern> foundType = AeaaThreatTypeStore.get().fromAttackPatternMetaData(atmd).getIdentifier();
        return fromAttackPatternMetaData(atmd, foundType.getThreatReferenceFactory());
    }

    public static List<AeaaAttackPattern> fromAttackPatternMetaData(List<AttackPatternMetaData> atmds) {
        final List<AeaaAttackPattern> attackPatterns = new ArrayList<>();
        for (AttackPatternMetaData atmd : atmds) {
            attackPatterns.add(AeaaAttackPattern.fromAttackPatternMetaData(atmd));
        }
        return attackPatterns;
    }

    protected abstract JSONObject toReferencedAttackPatterns();

    public static JSONArray toReferencedAttackPatterns(List<AeaaAttackPattern> attackPatterns) {
        return attackPatterns.stream().map(w -> w.toReferencedAttackPatterns()).collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
    }

    @Override
    public String toString() {
        return id;
    }
}
