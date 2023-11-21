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
package org.metaeffekt.core.security.cvss.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssSource.CvssEntity;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.SourcedCvssVector;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class CvssSelector implements Cloneable {

    private final static Logger LOG = LoggerFactory.getLogger(CvssSelector.class);

    private final List<CvssRule> rules;
    private final List<SelectorStatsEvaluator> statsEvaluatorActions;
    private final List<SelectorVectorEvaluator> selectorVectorEvaluators;

    public CvssSelector(List<CvssRule> rules, List<SelectorStatsEvaluator> statsEvaluatorActions, List<SelectorVectorEvaluator> selectorVectorEvaluators) {
        if (rules == null) throw new IllegalArgumentException("rules must not be null");
        if (statsEvaluatorActions == null) throw new IllegalArgumentException("statsEvaluatorActions must not be null");
        if (selectorVectorEvaluators == null)
            throw new IllegalArgumentException("selectorVectorEvaluators must not be null");
        this.rules = rules;
        this.statsEvaluatorActions = statsEvaluatorActions;
        this.selectorVectorEvaluators = selectorVectorEvaluators;
    }

    public CvssSelector(List<CvssRule> rules) {
        this(rules, Collections.emptyList(), Collections.emptyList());
    }

    public <T extends CvssVector> SourcedCvssVector<T> selectVector(Collection<SourcedCvssVector<T>> vectors) {
        SourcedCvssVector<T> effective = null;
        final Map<String, Integer> stats = new HashMap<>();

        for (CvssRule rule : rules) {
            final SourcedCvssVector<T> chosenVector = rule.getSourceSelector().selectVector(vectors);

            final List<SelectorVectorEvaluator> vectorEvaluators = rule.getVectorEvaluators();

            boolean skip = false;
            for (SelectorVectorEvaluator vectorEvaluator : vectorEvaluators) {
                if (vectorEvaluator.evaluate(chosenVector == null ? null : chosenVector.getCvssVector())) {
                    final EvaluatorAction action = vectorEvaluator.getAction();
                    if (action == EvaluatorAction.RETURN_NULL) {
                        return null;
                    } else if (action == EvaluatorAction.FAIL) {
                        throw new IllegalStateException("Evaluator action failed: " + vectorEvaluator + " on " + this);
                    } else if (action == EvaluatorAction.SKIP) {
                        skip = true;
                        break;
                    } else if (action == EvaluatorAction.RETURN_PREVIOUS) {
                        return effective;
                    }
                }
            }
            if ((chosenVector == null || chosenVector.getCvssVector() == null) & vectorEvaluators.isEmpty()) {
                for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                    collector.apply(stats, 0, 1, () -> 0);
                }
                skip = true;
            }
            if (skip) continue;

            if (chosenVector != null) {
                if (effective == null) {
                    effective = chosenVector.clone();

                    for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                        collector.apply(stats, 1, 0, effective.getCvssVector()::size);
                    }
                } else {
                    final Pair<T, Integer> result = rule.getMergingMethod().mergeVectors(effective.getCvssVector(), chosenVector.getCvssVector());
                    effective = effective.deriveAppendSourceSetVector(chosenVector.getCvssSource(), result.getLeft());

                    for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                        collector.apply(stats, 1, 0, result::getRight);
                    }
                }

            } else {
                for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                    collector.apply(stats, 0, 1, () -> 0);
                }
            }
        }

        for (SelectorStatsEvaluator statsEvaluatorAction : statsEvaluatorActions) {
            final Integer value = stats.getOrDefault(statsEvaluatorAction.getAttributeName(), 0);
            if (statsEvaluatorAction.getComparator().test(value, statsEvaluatorAction.getComparisonValue())) {
                final EvaluatorAction action = statsEvaluatorAction.getAction();
                if (action == EvaluatorAction.RETURN_NULL) {
                    return null;
                } else if (action == EvaluatorAction.FAIL) {
                    throw new IllegalStateException("Evaluator action failed: " + statsEvaluatorAction + " on " + this);
                }
            }
        }

        for (SelectorVectorEvaluator vectorEvaluator : selectorVectorEvaluators) {
            if (vectorEvaluator.evaluate(effective == null ? null : effective.getCvssVector())) {
                final EvaluatorAction action = vectorEvaluator.getAction();
                if (action == EvaluatorAction.RETURN_NULL) {
                    return null;
                } else if (action == EvaluatorAction.FAIL) {
                    throw new IllegalStateException("Evaluator action failed: " + vectorEvaluator + " on " + this);
                }
            }
        }

        return effective;
    }

    public <T extends CvssVector> SourcedCvssVector<T> selectVector(Collection<SourcedCvssVector<?>> vectors, Class<T> vectorClass) {
        final List<SourcedCvssVector<T>> checkVectors = vectors.stream()
                .filter(v -> vectorClass.isAssignableFrom(v.getCvssVector().getClass()))
                .map(v -> (SourcedCvssVector<T>) v)
                .collect(Collectors.toList());

        return selectVector(checkVectors);
    }

    public <T extends CvssVector> SourcedCvssVector<T> selectVector(SourcedCvssVectorSet vectors, Class<T> vectorClass) {
        return selectVector(vectors.getCvssVectors(), vectorClass);
    }

    public JSONObject toJson() {
        final JSONArray rules = new JSONArray();
        for (CvssRule rule : this.rules) {
            rules.put(rule.toJson());
        }

        final JSONArray stats = new JSONArray();
        for (SelectorStatsEvaluator statsEvaluatorAction : statsEvaluatorActions) {
            stats.put(statsEvaluatorAction.toJson());
        }

        final JSONArray vectorEvaluators = new JSONArray(selectorVectorEvaluators.stream().map(SelectorVectorEvaluator::toJson).collect(Collectors.toList()));

        return new JSONObject()
                .put("rules", rules)
                .put("stats", stats)
                .put("vectorEval", vectorEvaluators);
    }

    public static CvssSelector fromJson(JSONObject json) {
        final JSONArray jsonRules = json.optJSONArray("rules");
        final List<CvssRule> rules;
        if (jsonRules != null) {
            rules = new ArrayList<>();
            for (int i = 0; i < jsonRules.length(); i++) {
                rules.add(CvssRule.fromJson(jsonRules.getJSONObject(i)));
            }
        } else {
            rules = Collections.emptyList();
        }

        final JSONArray jsonStats = json.optJSONArray("stats");
        final List<SelectorStatsEvaluator> statsEvaluatorActions;
        if (jsonStats != null) {
            statsEvaluatorActions = new ArrayList<>();
            for (int i = 0; i < jsonStats.length(); i++) {
                statsEvaluatorActions.add(SelectorStatsEvaluator.fromJson(jsonStats.getJSONObject(i)));
            }
        } else {
            statsEvaluatorActions = Collections.emptyList();
        }

        return new CvssSelector(rules, statsEvaluatorActions, SelectorVectorEvaluator.fromParentJson(json));
    }

    public List<CvssRule> getRules() {
        return rules;
    }

    public List<SelectorStatsEvaluator> getStatsEvaluatorActions() {
        return statsEvaluatorActions;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public CvssSelector clone() {
        return new CvssSelector(this.rules.stream().map(CvssRule::clone).collect(Collectors.toList()));
    }

    public static class CvssRule implements Cloneable {
        private final SourceSelector sourceSelector;
        private final MergingMethod mergingMethod;
        private final List<SelectorStatsCollector> statsCollectors;
        private final List<SelectorVectorEvaluator> vectorEvaluators;

        public CvssRule(MergingMethod mergingMethod, List<SelectorStatsCollector> statsCollectors, List<SelectorVectorEvaluator> vectorEvaluators, SourceSelector sourceSelector) {
            this.mergingMethod = mergingMethod;
            this.statsCollectors = statsCollectors;
            this.vectorEvaluators = vectorEvaluators;
            this.sourceSelector = sourceSelector;
        }

        public CvssRule(MergingMethod mergingMethod, List<SelectorStatsCollector> statsCollectors, List<SelectorVectorEvaluator> vectorEvaluators, SourceSelectorEntry... preferredSources) {
            this.mergingMethod = mergingMethod;
            this.statsCollectors = statsCollectors;
            this.vectorEvaluators = vectorEvaluators;
            this.sourceSelector = new SourceSelector(preferredSources);
        }

        public CvssRule(MergingMethod mergingMethod, SourceSelector sourceSelector) {
            this(mergingMethod, Collections.emptyList(), Collections.emptyList(), sourceSelector);
        }

        public CvssRule(MergingMethod mergingMethod, SourceSelectorEntry... preferredSources) {
            this(mergingMethod, Collections.emptyList(), Collections.emptyList(), preferredSources);
        }

        public JSONObject toJson() {
            final JSONArray vectorEvaluators = new JSONArray(this.vectorEvaluators.stream().map(SelectorVectorEvaluator::toJson).collect(Collectors.toList()));
            return new JSONObject()
                    .put("vectorEval", vectorEvaluators)
                    .put("selector", sourceSelector.toJson())
                    .put("stats", statsCollectors.stream().map(SelectorStatsCollector::toJson).collect(Collectors.toList()))
                    .put("method", mergingMethod.name());
        }

        public static CvssRule fromJson(JSONObject json) {
            final JSONArray stats = json.optJSONArray("stats");
            final List<SelectorStatsCollector> statsCollectors;
            if (stats != null) {
                statsCollectors = new ArrayList<>();
                for (int i = 0; i < stats.length(); i++) {
                    statsCollectors.add(SelectorStatsCollector.fromJson(stats.getJSONObject(i)));
                }
            } else {
                statsCollectors = Collections.emptyList();
            }

            return new CvssRule(
                    MergingMethod.valueOf(json.getString("method")),
                    statsCollectors,
                    SelectorVectorEvaluator.fromParentJson(json),
                    SourceSelector.fromJson(json.getJSONArray("selector"))
            );
        }

        public SourceSelector getSourceSelector() {
            return sourceSelector;
        }

        public MergingMethod getMergingMethod() {
            return mergingMethod;
        }

        public List<SelectorStatsCollector> getStatsCollectors() {
            return statsCollectors;
        }

        public List<SelectorVectorEvaluator> getVectorEvaluators() {
            return vectorEvaluators;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public CvssRule clone() {
            return new CvssRule(
                    this.mergingMethod,
                    this.getStatsCollectors().stream().map(SelectorStatsCollector::clone).collect(Collectors.toList()),
                    this.getVectorEvaluators().stream().map(SelectorVectorEvaluator::clone).collect(Collectors.toList()),
                    this.sourceSelector.clone()
            );
        }
    }

    public static class SourceSelector implements Cloneable {
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

        public SourcedCvssVector<? extends CvssVector> selectVectorUnchecked(Collection<SourcedCvssVector<? extends CvssVector>> vectors) {
            for (SourceSelectorEntry source : preferredSources) {
                for (SourcedCvssVector<? extends CvssVector> vector : vectors) {
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

        @Override
        public SourceSelector clone() {
            return new SourceSelector(this.preferredSources.stream().map(SourceSelectorEntry::clone).collect(Collectors.toList()));
        }
    }

    public static class SourceSelectorEntry implements Cloneable {
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

        private <T extends CvssSource.EntityNameProvider> boolean matchListWithSource(List<SourceSelectorEntryEntry<T>> list, T sourceAttribute) {
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

        private static String getPotentiallyInvertedOrNullName(CvssSource.EntityNameProvider nameProvider, boolean inverted) {
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

        @Override
        public SourceSelectorEntry clone() {
            return new SourceSelectorEntry(this.hostingEntities.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()),
                    this.issuingEntityRoles.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()),
                    this.issuingEntities.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()));
        }

        public final static CvssEntity ANY_ENTITY = new CvssEntity("*");
        public final static CvssIssuingEntityRole ANY_ROLE = new CvssIssuingEntityRole("*");

        public final static CvssEntity EMPTY_ENTITY = null;
        public final static CvssIssuingEntityRole EMPTY_ROLE = null;
    }

    public static class SourceSelectorEntryEntry<T extends CvssSource.EntityNameProvider> implements Cloneable {
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
        public boolean matches(CvssSource.EntityNameProvider checkValue) {
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

        public static <T extends CvssSource.EntityNameProvider> SourceSelectorEntryEntry<T> fromString(String value, Function<String, T> extractor) {
            if (value == null) return null;
            if (value.startsWith("not:")) {
                final String param = value.substring(4);
                return new SourceSelectorEntryEntry<>(extractor.apply(param.isEmpty() ? null : param), true);
            } else {
                return new SourceSelectorEntryEntry<>(extractor.apply(value.isEmpty() ? null : value), false);
            }
        }

        @Override
        public SourceSelectorEntryEntry<T> clone() {
            return new SourceSelectorEntryEntry<>(this.value, this.inverted);
        }
    }

    public static class SelectorStatsCollector implements Cloneable {
        private final String attributeName;
        private final StatsCollectorProvider provider;
        private final StatsCollectorSetType setType;

        public SelectorStatsCollector(String attributeName, StatsCollectorProvider provider, StatsCollectorSetType setType) {
            this.attributeName = attributeName;
            this.provider = provider;
            this.setType = setType;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public StatsCollectorProvider getProvider() {
            return provider;
        }

        public StatsCollectorSetType getSetType() {
            return setType;
        }

        public void apply(Map<String, Integer> stats, int presence, int absence, Supplier<Integer> sizeSupplier) {
            final int value;

            if (provider == StatsCollectorProvider.PRESENCE) {
                value = presence;
            } else if (provider == StatsCollectorProvider.ABSENCE) {
                value = absence;
            } else if (provider == StatsCollectorProvider.APPLIED_PARTS_COUNT) {
                value = sizeSupplier.get();
            } else {
                throw new IllegalStateException("Unknown provider: " + provider);
            }

            if (setType == StatsCollectorSetType.ADD) {
                stats.merge(attributeName, value, Integer::sum);
            } else if (setType == StatsCollectorSetType.SUBTRACT) {
                stats.merge(attributeName, value, (a, b) -> a - b);
            } else if (setType == StatsCollectorSetType.SET) {
                stats.put(attributeName, value);
            } else if (setType == StatsCollectorSetType.MAX) {
                stats.merge(attributeName, value, Integer::max);
            } else if (setType == StatsCollectorSetType.MIN) {
                stats.merge(attributeName, value, Integer::min);
            } else {
                throw new IllegalStateException("Unknown set type: " + setType);
            }
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("attribute", attributeName)
                    .put("provider", provider.name())
                    .put("setType", setType.name());
        }

        public static SelectorStatsCollector fromJson(JSONObject json) {
            return new SelectorStatsCollector(
                    json.getString("attribute"),
                    StatsCollectorProvider.valueOf(json.getString("provider")),
                    StatsCollectorSetType.valueOf(json.getString("setType"))
            );
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorStatsCollector clone() {
            return new SelectorStatsCollector(this.attributeName, this.provider, this.setType);
        }
    }

    public static class SelectorVectorEvaluator implements Cloneable {
        private final Map<VectorEvaluatorOperation, Boolean> operations; // AND; can be inverted
        private final EvaluatorAction action;

        public SelectorVectorEvaluator(Map<VectorEvaluatorOperation, Boolean> operations, EvaluatorAction action) {
            this.operations = operations;
            this.action = action;
        }

        public SelectorVectorEvaluator(VectorEvaluatorOperation operation, boolean inverted, EvaluatorAction action) {
            this.operations = Collections.singletonMap(operation, inverted);
            this.action = action;
        }

        public SelectorVectorEvaluator(VectorEvaluatorOperation operation, EvaluatorAction action) {
            this.operations = Collections.singletonMap(operation, false);
            this.action = action;
        }

        public boolean evaluate(CvssVector vector) {
            for (Map.Entry<VectorEvaluatorOperation, Boolean> entry : operations.entrySet()) {
                final VectorEvaluatorOperation operation = entry.getKey();
                final Boolean isInverted = entry.getValue();
                if (vector == null) {
                    if (operation == VectorEvaluatorOperation.IS_NULL) {
                        return !isInverted;
                    } else {
                        return isInverted;
                    }
                }
                final boolean result = operation.test(vector);
                final boolean effective = isInverted != result;
                if (!effective) return false;
            }
            return true;
        }

        public EvaluatorAction getAction() {
            return action;
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("action", action.name())
                    .put("and", new JSONArray(operations.entrySet().stream().map(e -> e.getKey().toPotentiallyInvertedString(e.getValue())).collect(Collectors.toList())));
        }

        public static SelectorVectorEvaluator fromJson(JSONObject json) {
            final Map<VectorEvaluatorOperation, Boolean> operations = new HashMap<>();
            final JSONArray jsonOperations = json.getJSONArray("and");
            for (int i = 0; i < jsonOperations.length(); i++) {
                final String operation = jsonOperations.getString(i);
                final Pair<VectorEvaluatorOperation, Boolean> pair = VectorEvaluatorOperation.extractPotentiallyInverted(operation);
                operations.put(pair.getLeft(), pair.getRight());
            }
            return new SelectorVectorEvaluator(operations, EvaluatorAction.valueOf(json.getString("action")));
        }

        private static List<SelectorVectorEvaluator> fromParentJson(JSONObject json) {
            final JSONArray vectorEvaluators = json.optJSONArray("vectorEval");
            if (vectorEvaluators != null) {
                final List<SelectorVectorEvaluator> selectorVectorEvaluators = new ArrayList<>();
                for (int i = 0; i < vectorEvaluators.length(); i++) {
                    selectorVectorEvaluators.add(SelectorVectorEvaluator.fromJson(vectorEvaluators.getJSONObject(i)));
                }
                return selectorVectorEvaluators;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorVectorEvaluator clone() {
            return new SelectorVectorEvaluator(new HashMap<>(this.operations), action);
        }
    }

    public static class SelectorStatsEvaluator implements Cloneable {
        private final String attributeName;
        private final StatsEvaluatorOperation comparator;
        private final EvaluatorAction action;
        private final Integer comparisonValue;

        public SelectorStatsEvaluator(String attributeName, StatsEvaluatorOperation comparator, EvaluatorAction action, Integer comparisonValue) {
            this.attributeName = attributeName;
            this.comparator = comparator;
            this.action = action;
            this.comparisonValue = comparisonValue;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public StatsEvaluatorOperation getComparator() {
            return comparator;
        }

        public EvaluatorAction getAction() {
            return action;
        }

        public Integer getComparisonValue() {
            return comparisonValue;
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("attribute", attributeName)
                    .put("comparator", comparator.name())
                    .put("action", action.name())
                    .put("value", comparisonValue);
        }

        public static SelectorStatsEvaluator fromJson(JSONObject json) {
            return new SelectorStatsEvaluator(
                    json.getString("attribute"),
                    StatsEvaluatorOperation.valueOf(json.getString("comparator")),
                    EvaluatorAction.valueOf(json.getString("action")),
                    json.getInt("value")
            );
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorStatsEvaluator clone() {
            return new SelectorStatsEvaluator(this.attributeName, this.comparator, this.action, comparisonValue);
        }
    }

    public enum MergingMethod {
        /**
         * Represents a merging method that applies all parts of the newCvssVector to the base CvssVector.
         */
        ALL((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVector(newVector);
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newCvssVector to the base CvssVector if it results in a lower/equal overall score.
         */
        // LOWER((base, newVector) -> base.clone().applyVectorPartsIfLower(newVector, CvssVector::getOverallScore)),
        LOWER((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfLower(newVector, CvssVector::getOverallScore);
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newCvssVector to the base CvssVector if it results in a higher/equal overall score.
         */
        // HIGHER((base, newVector) -> base.clone().applyVectorPartsIfHigher(newVector, CvssVector::getOverallScore)),
        HIGHER((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfHigher(newVector, CvssVector::getOverallScore);
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that overwrites the full base CvssVector with the newCvssVector.
         */
        OVERWRITE((base, newVector) -> Pair.of(newVector.clone(), 0));

        private final BiFunction<CvssVector, CvssVector, Pair<CvssVector, Integer>> mergingFunction;

        MergingMethod(BiFunction<CvssVector, CvssVector, Pair<CvssVector, Integer>> mergingFunction) {
            this.mergingFunction = mergingFunction;
        }

        public <T extends CvssVector> Pair<T, Integer> mergeVectors(T base, T newVector) {
            return (Pair<T, Integer>) mergingFunction.apply(base, newVector);
        }
    }

    public enum StatsCollectorProvider {
        PRESENCE,
        ABSENCE,
        APPLIED_PARTS_COUNT
    }

    public enum StatsCollectorSetType {
        ADD,
        SUBTRACT,
        SET,
        MAX,
        MIN
    }

    public enum StatsEvaluatorOperation {
        EQUAL(Objects::equals),
        SMALLER((left, right) -> left < right),
        SMALLER_OR_EQUAL((left, right) -> left <= right),
        GREATER((left, right) -> left > right),
        GREATER_OR_EQUAL((left, right) -> left >= right);

        private final BiPredicate<Integer, Integer> predicate;

        StatsEvaluatorOperation(BiPredicate<Integer, Integer> predicate) {
            this.predicate = predicate;
        }

        public boolean test(int left, int right) {
            return predicate.test(left, right);
        }
    }

    public enum VectorEvaluatorOperation {
        IS_NULL(Objects::isNull),
        IS_BASE_FULLY_DEFINED(CvssVector::isBaseFullyDefined),
        IS_BASE_PARTIALLY_DEFINED(CvssVector::isAnyBaseDefined),
        IS_ENVIRONMENTAL_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? (((MultiScoreCvssVector) vector).isAnyEnvironmentalDefined()) : (vector instanceof Cvss4P0 ? ((Cvss4P0) vector).isAnyEnvironmentalDefined() : false)),
        IS_TEMPORAL_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? (((MultiScoreCvssVector) vector).isAnyTemporalDefined()) : false),
        IS_THREAT_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? false : (vector instanceof Cvss4P0 ? ((Cvss4P0) vector).isAnyThreatDefined() : false));

        private final Predicate<CvssVector> predicate;

        VectorEvaluatorOperation(Predicate<CvssVector> predicate) {
            this.predicate = predicate;
        }

        public boolean test(CvssVector vector) {
            return predicate.test(vector);
        }

        public String toPotentiallyInvertedString(boolean inverted) {
            return (inverted ? "not:" : "") + name();
        }

        public static Pair<VectorEvaluatorOperation, Boolean> extractPotentiallyInverted(String value) {
            if (value == null) return Pair.of(null, false);
            if (value.startsWith("not:")) {
                return Pair.of(VectorEvaluatorOperation.valueOf(value.substring(4)), true);
            } else {
                return Pair.of(VectorEvaluatorOperation.valueOf(value), false);
            }
        }
    }

    public enum EvaluatorAction {
        FAIL,
        RETURN_NULL,
        SKIP,
        RETURN_PREVIOUS
    }
}
