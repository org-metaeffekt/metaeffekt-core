/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.security.cvss;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource.CvssEntity;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CvssSelector {

    private final static Logger LOG = LoggerFactory.getLogger(CvssSelector.class);

    private final List<CvssRule> rules;

    public CvssSelector(List<CvssRule> rules) {
        this.rules = rules;
    }

    public <T extends CvssVector> T calculateEffective(Collection<SourcedCvssVector<T>> vectors) {
        T effective = null;

        for (CvssRule rule : rules) {
            final SourcedCvssVector<T> chosenVector = rule.getSourceSelector().selectVector(vectors);
            if (chosenVector != null) {
                if (effective == null) {
                    effective = (T) chosenVector.getCvssVector().clone();
                } else {
                    effective = rule.getMergingMethod().mergeVectors(effective, chosenVector.getCvssVector());
                }
            }
        }

        return effective;
    }

    public JSONArray toJson() {
        final JSONArray rules = new JSONArray();
        for (CvssRule rule : this.rules) {
            rules.put(rule.toJson());
        }
        return rules;
    }

    public static CvssSelector fromJson(JSONArray json) {
        final List<CvssRule> rules = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            rules.add(CvssRule.fromJson(json.getJSONObject(i)));
        }
        return new CvssSelector(rules);
    }

    public List<CvssRule> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static class CvssRule {
        private final SourceSelector sourceSelector;
        private final MergingMethod mergingMethod;

        public CvssRule(MergingMethod mergingMethod, SourceSelector sourceSelector) {
            this.mergingMethod = mergingMethod;
            this.sourceSelector = sourceSelector;
        }

        public CvssRule(MergingMethod mergingMethod, SourceSelectorEntry... preferredSources) {
            this.mergingMethod = mergingMethod;
            this.sourceSelector = new SourceSelector(preferredSources);
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("selector", sourceSelector.toJson())
                    .put("method", mergingMethod.name());
        }

        public static CvssRule fromJson(JSONObject json) {
            return new CvssRule(
                    MergingMethod.valueOf(json.getString("method")),
                    SourceSelector.fromJson(json.getJSONArray("selector"))
            );
        }

        public SourceSelector getSourceSelector() {
            return sourceSelector;
        }

        public MergingMethod getMergingMethod() {
            return mergingMethod;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }
    }

    public static class SourceSelector {
        private final List<SourceSelectorEntry> preferredSources;

        public SourceSelector(List<SourceSelectorEntry> preferredSources) {
            this.preferredSources = preferredSources;
        }

        public SourceSelector(SourceSelectorEntry... preferredSources) {
            this.preferredSources = Arrays.asList(preferredSources);
        }

        public <T extends CvssVector> SourcedCvssVector<T> selectVector(Collection<SourcedCvssVector<T>> vectors) {
            for (SourceSelectorEntry source : preferredSources) {
                for (SourcedCvssVector<T> vector : vectors) {
                    if (source.matches(vector.getCvssSource())) {
                        return vector;
                    }
                }
            }
            return null;
        }

        public JSONArray toJson() {
            final JSONArray preferredSources = new JSONArray();
            for (SourceSelectorEntry entry : this.preferredSources) {
                preferredSources.put(entry.toJson());
            }
            return preferredSources;
        }

        public static SourceSelector fromJson(JSONArray json) {
            final List<SourceSelectorEntry> entries = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                entries.add(SourceSelectorEntry.fromJson(json.getJSONObject(i)));
            }
            return new SourceSelector(entries);
        }

        public List<SourceSelectorEntry> getPreferredSources() {
            return preferredSources;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }
    }

    public static class SourceSelectorEntry {
        private final List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities;
        private final List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities;
        private final List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles;

        public SourceSelectorEntry(List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities, List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles, List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities) {
            this.hostingEntities = hostingEntities;
            this.issuingEntityRoles = issuingEntityRoles;
            this.issuingEntities = issuingEntities;
        }

        public SourceSelectorEntry(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity,
                                   boolean invertedHostingEntity, boolean invertedIssuingEntity, boolean invertedIssuingEntityRole) {
            this.hostingEntities = hostingEntity == null && !invertedHostingEntity ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(hostingEntity, invertedHostingEntity));
            this.issuingEntityRoles = issuingEntityRole == null && !invertedIssuingEntity ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(issuingEntityRole, invertedIssuingEntityRole));
            this.issuingEntities = issuingEntity == null && !invertedIssuingEntityRole ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(issuingEntity, invertedIssuingEntity));
        }

        public SourceSelectorEntry(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity) {
            this(hostingEntity, issuingEntityRole, issuingEntity, false, false, false);
        }

        public boolean matches(CvssSource<?> source) {
            if (source == null) return false;

            final boolean matchesHost = matchListWithSource(hostingEntities, source.getHostingEntity());
            final boolean matchesIssuer = matchListWithSource(issuingEntities, source.getIssuingEntity());
            final boolean matchesRole = matchListWithSource(issuingEntityRoles, source.getIssuingEntityRole());

            return matchesHost && matchesIssuer && matchesRole;
        }

        private <T extends CvssSource.NameProvider> boolean matchListWithSource(List<SourceSelectorEntryEntry<T>> list, T sourceAttribute) {
            // all of these operators have been chosen carefully, please only change them if you know what you are doing
            if (sourceAttribute == null && list.isEmpty()) {
                return true;
            } else {
                return !list.isEmpty() && list.stream().allMatch(entry -> entry.matches(sourceAttribute));
            }
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("host", new JSONArray(this.hostingEntities.stream().map(String::valueOf).collect(Collectors.toList())))
                    .put("issuerRole", new JSONArray(this.issuingEntityRoles.stream().map(String::valueOf).collect(Collectors.toList())))
                    .put("issuer", new JSONArray(this.issuingEntities.stream().map(String::valueOf).collect(Collectors.toList())));
        }

        public static SourceSelectorEntry fromJson(JSONObject json) {
            final List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities = new ArrayList<>();
            final List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles = new ArrayList<>();
            final List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities = new ArrayList<>();

            final JSONArray host = json.getJSONArray("host");
            for (int i = 0; i < host.length(); i++) {
                hostingEntities.add(SourceSelectorEntryEntry.fromString(host.getString(i), CvssEntity::new));
            }
            final JSONArray issuerRole = json.getJSONArray("issuerRole");
            for (int i = 0; i < issuerRole.length(); i++) {
                issuingEntityRoles.add(SourceSelectorEntryEntry.fromString(issuerRole.getString(i), CvssIssuingEntityRole::new));
            }
            final JSONArray issuer = json.getJSONArray("issuer");
            for (int i = 0; i < issuer.length(); i++) {
                issuingEntities.add(SourceSelectorEntryEntry.fromString(issuer.getString(i), CvssEntity::new));
            }

            return new SourceSelectorEntry(hostingEntities, issuingEntityRoles, issuingEntities);
        }

        private static String getPotentiallyInvertedOrNullName(CvssSource.NameProvider nameProvider, boolean inverted) {
            if (nameProvider == null) return null;
            if (inverted) {
                return "not:" + nameProvider.getName();
            } else {
                return nameProvider.getName();
            }
        }

        private static <T> Pair<T, Boolean> extractPotentiallyInverted(String value, Function<String, T> extractor) {
            if (value == null) return Pair.of(null, false);
            if (value.startsWith("not:")) {
                return Pair.of(extractor.apply(value.substring(4)), true);
            } else {
                return Pair.of(extractor.apply(value), false);
            }
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public final static CvssEntity ANY_ENTITY = new CvssEntity("*");
        public final static CvssIssuingEntityRole ANY_ROLE = new CvssIssuingEntityRole("*");

        public final static CvssEntity EMPTY_ENTITY = null;
        public final static CvssIssuingEntityRole EMPTY_ROLE = null;
    }

    public static class SourceSelectorEntryEntry<T extends CvssSource.NameProvider> {
        private final T value;
        private final boolean inverted;

        public SourceSelectorEntryEntry(T value, boolean inverted) {
            this.value = value;
            this.inverted = inverted;
        }

        public SourceSelectorEntryEntry(T value) {
            this(value, false);
        }

        protected boolean potentiallyInvert(boolean value) {
            return inverted != value;
        }

        /**
         * Follows these rules. Horizontal is this instance, vertical is the parameter.
         * <pre>
         * |       | ANY | value | null |
         * |-------|-----|-------|------|
         * | ANY   | Y   | Y     | Y    |
         * | value | Y   | ==    | N    |
         * | null  | N   | N     | Y    |
         * </pre>
         * <p>
         * Inverted if {@link #inverted} is true.
         *
         * @param checkValue The value to compare this instances value to.
         * @return true if the values match, false otherwise.
         */
        public boolean matches(CvssSource.NameProvider checkValue) {
            final String checkName = checkValue == null ? null : checkValue.getName();
            final String thisName = value == null ? null : value.getName();

            if (checkName == null && thisName == null) return potentiallyInvert(true);

            if (SourceSelectorEntry.ANY_ENTITY.getName().equals(thisName)) {
                return potentiallyInvert(true);
            }

            if (checkName == null || thisName == null) return potentiallyInvert(false);

            if (SourceSelectorEntry.ANY_ENTITY.getName().equals(checkName)) {
                return potentiallyInvert(true);
            }

            if (checkName.equals(thisName)) return potentiallyInvert(true);

            return potentiallyInvert(false);
        }

        public T getValue() {
            return value;
        }

        public boolean isInverted() {
            return inverted;
        }

        @Override
        public String toString() {
            return (inverted ? "not:" : "") + (value == null ? "" : value.getName());
        }

        public static <T extends CvssSource.NameProvider> SourceSelectorEntryEntry<T> fromString(String value, Function<String, T> extractor) {
            if (value == null) return null;
            if (value.startsWith("not:")) {
                final String param = value.substring(4);
                return new SourceSelectorEntryEntry<>(extractor.apply(param.isEmpty() ? null : param), true);
            } else {
                return new SourceSelectorEntryEntry<>(extractor.apply(value.isEmpty() ? null : value), false);
            }
        }
    }

    public enum MergingMethod {
        ALL((base, newVector) -> base.clone().applyVector(newVector)),
        LOWER((base, newVector) -> base.clone().applyVectorPartsIfLower(newVector, CvssVector::getOverallScore)),
        HIGHER((base, newVector) -> base.clone().applyVectorPartsIfHigher(newVector, CvssVector::getOverallScore)),
        OVERWRITE((base, newVector) -> newVector.clone());

        private final BiFunction<CvssVector, CvssVector, CvssVector> mergingFunction;

        MergingMethod(BiFunction<CvssVector, CvssVector, CvssVector> mergingFunction) {
            this.mergingFunction = mergingFunction;
        }

        public <T extends CvssVector> T mergeVectors(T base, T newVector) {
            return (T) mergingFunction.apply(base, newVector);
        }
    }
}
