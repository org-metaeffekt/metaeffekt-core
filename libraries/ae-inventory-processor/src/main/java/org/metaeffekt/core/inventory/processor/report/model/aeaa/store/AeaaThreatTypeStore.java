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

import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.AttackPatternMetaData;
import org.metaeffekt.core.inventory.processor.model.WeaknessMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaAttackPattern;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaCapecEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaCweEntry;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre.AeaaWeakness;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaGeneralThreatReference;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaThreatReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class AeaaThreatTypeStore extends AeaaContentIdentifierStore<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>> {

    public final static AeaaThreatTypeIdentifier<AeaaCweEntry> CWE = new AeaaThreatTypeIdentifier<>("CWE",
            "Common Weakness Enumeration", ThreatCategory.WEAKNESS_IMPLEMENTATION.getKey(),
            Pattern.compile("(CWE-\\d+)", Pattern.CASE_INSENSITIVE),
            AeaaCweEntry.class, AeaaCweEntry::new);

    public final static AeaaThreatTypeIdentifier<AeaaCapecEntry> CAPEC = new AeaaThreatTypeIdentifier<>("CAPEC",
            "Common Attack Pattern Enumeration and Classification", ThreatCategory.ATTACK_PATTERN_IMPLEMENTATION.getKey(),
            Pattern.compile("(CAPEC-\\d+)", Pattern.CASE_INSENSITIVE),
            AeaaCapecEntry.class, AeaaCapecEntry::new);


    private final static AeaaThreatTypeStore INSTANCE = new AeaaThreatTypeStore();

    protected AeaaThreatTypeStore() {
        super((Class<AeaaThreatTypeIdentifier<?>>) (Class<?>) AeaaThreatTypeIdentifier.class);
    }

    public static AeaaThreatTypeStore get() {
        return INSTANCE;
    }

    @Override
    protected AeaaThreatTypeIdentifier<? extends AeaaThreatReference> createIdentifier(@NonNull String name, @NonNull String implementation) {
        ThreatCategory category;
        try {
            category = ThreatCategory.valueOf(implementation);
        } catch (Exception e) {
            category = ThreatCategory.THREAT_IMPLEMENTATION;
        }

        return new AeaaThreatTypeIdentifier<>(name, AeaaContentIdentifierStore.AeaaContentIdentifier.deriveWellFormedName(name),
                category.threatCategory,
                Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
                AeaaGeneralThreatReference.class, AeaaGeneralThreatReference::new);
    }

    public AeaaSingleContentIdentifierParseResult<AeaaThreatTypeIdentifier<? extends AeaaWeakness>> fromWeaknessMetaData(WeaknessMetaData wmd) {
        final String entryId = ObjectUtils.firstNonNull(wmd.get(WeaknessMetaData.Attribute.ID));
        final String type = ObjectUtils.firstNonNull(wmd.get(AdvisoryMetaData.Attribute.SOURCE.getKey()));

        final AeaaThreatTypeIdentifier<? extends AeaaWeakness> weaknessTypeIdentifier = (AeaaThreatTypeIdentifier<? extends AeaaWeakness>) this.fromNameAndImplementation(type, ThreatCategory.WEAKNESS_IMPLEMENTATION.getKey());
        return new AeaaSingleContentIdentifierParseResult<>(weaknessTypeIdentifier, entryId);
    }

    public AeaaSingleContentIdentifierParseResult<AeaaThreatTypeIdentifier<? extends AeaaAttackPattern>> fromAttackPatternMetaData(AttackPatternMetaData atmd) {
        final String entryId = ObjectUtils.firstNonNull(atmd.get(AttackPatternMetaData.Attribute.ID));
        final String type = ObjectUtils.firstNonNull(atmd.get(AdvisoryMetaData.Attribute.SOURCE.getKey()));

        final AeaaThreatTypeIdentifier<? extends AeaaAttackPattern> attackPatternTypeIdentifier = (AeaaThreatTypeIdentifier<? extends AeaaAttackPattern>) this.fromNameAndImplementation(type, ThreatCategory.ATTACK_PATTERN_IMPLEMENTATION.getKey());
        return new AeaaSingleContentIdentifierParseResult<>(attackPatternTypeIdentifier, entryId);
    }

    /**
     * Defaults to creating a type of implementation 'threat'
     *
     * @param name of the to be determined Threat Content Type
     * @return the found/created ThreatTypeIdentifier
     */
    public AeaaThreatTypeIdentifier<? extends AeaaThreatReference> fromName(String name) {
        AeaaThreatTypeIdentifier<? extends AeaaThreatReference> result = fromNameWithoutCreation(name);
        if (result == null) {
            return super.registerIdentifier(createIdentifier(name, ""));
        }
        return result;
    }

    @Override
    public Map<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>, Set<String>> fromJsonMultipleReferencedIds(JSONArray json) {
        final Map<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>, Set<String>> referencedIds = new HashMap<>();

        for (int i = 0; i < json.length(); i++) {
            final JSONObject jsonObject = json.getJSONObject(i);
            final AeaaSingleContentIdentifierParseResult<AeaaThreatTypeIdentifier<? extends AeaaThreatReference>> pair = fromJsonNameAndImplementation(jsonObject);
            final String id = pair.getId();

            referencedIds.computeIfAbsent(pair.getIdentifier(), k -> new HashSet<>()).add(id);
        }

        return referencedIds;
    }

    @Override
    public AeaaSingleContentIdentifierParseResult<AeaaThreatTypeIdentifier<?>> fromJsonNameAndImplementation(JSONObject json) {
        final AeaaSingleContentIdentifierParseResult<?> superResult = super.fromJsonNameAndImplementation(json);

        if (superResult.getIdentifier() instanceof AeaaThreatTypeIdentifier) {
            return (AeaaSingleContentIdentifierParseResult<AeaaThreatTypeIdentifier<?>>) superResult;
        } else {
            throw new IllegalArgumentException("The provided JSON object does not represent a threat type identifier, which is an impossible scenario since this class can by definition only support advisory type identifiers.");
        }
    }

}
