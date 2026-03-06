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
import org.metaeffekt.core.inventory.processor.model.WeaknessMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaMatchableDetailsAmbDataClass;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaThreatTypeStore;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaThreatReference;

import java.util.*;
import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
public abstract class AeaaWeakness extends AeaaMatchableDetailsAmbDataClass<WeaknessMetaData, AeaaWeakness> implements AeaaThreatReference {

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_AMB) {{
        addAll(Arrays.asList(
                WeaknessMetaData.Attribute.ID.getKey(),
                WeaknessMetaData.Attribute.CONTENT.getKey()
        ));
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaMatchableDetailsAmbDataClass.CONVERSION_KEYS_MAP) {{
        addAll(Arrays.asList(
                "id", "content"
        ));
    }};

    @Override
    public WeaknessMetaData constructBaseModel() {
        return new WeaknessMetaData();
    }

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static List<AeaaWeakness> fromJson(JSONArray jsonArray) {
        List<AeaaWeakness> weaknesses = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (AeaaThreatTypeStore.CWE.patternMatchesId(o.optString("id"))) {
                weaknesses.add(AeaaCweEntry.fromJson(o));
            }
        }
        return weaknesses;
    }

    private static <T extends AeaaWeakness> T fromWeaknessMetaData(WeaknessMetaData wmd, Supplier<T> constructor) {
        if (wmd == null) return null;
        return constructor.get()
                .performAction(v -> v.appendFromBaseModel(wmd));
    }

    public static AeaaWeakness fromWeaknessMetaData(WeaknessMetaData wmd) {
        final AeaaThreatTypeIdentifier<? extends AeaaWeakness> foundType = AeaaThreatTypeStore.get().fromWeaknessMetaData(wmd).getIdentifier();
        return fromWeaknessMetaData(wmd, foundType.getThreatReferenceFactory());
    }

    public static List<AeaaWeakness> fromWeaknessMetaData(List<WeaknessMetaData> wmds) {
        final List<AeaaWeakness> weaknesses = new ArrayList<>();
        for (WeaknessMetaData wmd : wmds) {
            weaknesses.add(AeaaWeakness.fromWeaknessMetaData(wmd));
        }
        return weaknesses;
    }

    protected abstract JSONObject toReferencedWeaknesses();

    public static JSONArray toReferencedWeaknesses(List<AeaaWeakness> weaknesses) {
        return weaknesses.stream().map(w -> w.toReferencedWeaknesses()).collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
    }

}
