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
package org.metaeffekt.core.security.cvss;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;
import org.metaeffekt.core.security.cvss.processor.UniversalCvssCalculatorLinkGenerator;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P0;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for modeling CVSS Vectors.
 * <p>
 * The list of sources represents the following logic:
 * <ul>
 *     <li>
 *         If a no source is listed, the source of the vector is either unknown, irrelevant for the current task and therefore left away, not required or was forgotten to be added.
 *     </li>
 *     <li>
 *         If a single source is listed, the vector is directly sourced from the provided source and was not modified.
 *     </li>
 *     <li>
 *         If more than a single source is listed, other sourced vectors have been applied onto this vector in the given order. The only process provided by this library at this time that does this is the {@link org.metaeffekt.core.security.cvss.processor.CvssSelector#selectVector(Collection)}.
 *     </li>
 * </ul>
 * <p>
 * It also provides the option to cache the calculated CVSS scores in a {@link BakedCvssVectorScores} instance to reduce the amount of times a score has to be recalculated if it is known that the scores will have to be calculated multiple times.<br>
 * Note that you have to recalculate these values yourself if you changed the CVSS vector components via the individual setters. Using the {@link CvssVector#applyVector(String)} (and related) methods will clear the cache automatically.
 */
public abstract class CvssVector {

    private final static Logger LOG = LoggerFactory.getLogger(CvssVector.class);

    protected final List<CvssSource> sources = new ArrayList<>();
    protected final JSONObject applicabilityCondition;

    protected BakedCvssVectorScores bakedScores;

    public CvssVector() {
        this.applicabilityCondition = new JSONObject();
    }

    public CvssVector(CvssSource source) {
        this.addSource(source);
        this.applicabilityCondition = new JSONObject();
    }

    public CvssVector(CvssSource source, JSONObject applicabilityCondition) {
        this.addSource(source);
        this.applicabilityCondition = applicabilityCondition;
    }

    public CvssVector(Collection<CvssSource> sources, JSONObject applicabilityCondition) {
        this.addSources(sources);
        this.applicabilityCondition = applicabilityCondition;
    }

    public abstract String getName();

    public String getWebEditorLink() {
        return getAeUniversalWebEditorLink().generateOptimizedLink();
    }

    public abstract String getNistFirstWebEditorLink();

    public UniversalCvssCalculatorLinkGenerator getAeUniversalWebEditorLink() {
        final UniversalCvssCalculatorLinkGenerator generator = new UniversalCvssCalculatorLinkGenerator();
        generator.addVectorNullThrowing(this);
        generator.setSelectedVectorNullThrowing(this);
        return generator;
    }

    public abstract int size();

    public abstract double getBaseScore();

    public abstract double getOverallScore();

    public abstract boolean isBaseFullyDefined();

    public abstract boolean isAnyBaseDefined();

    /**
     * Fills and removes attributes to produce a consistent vector for computations
     */
    protected abstract void completeVector();

    protected abstract boolean applyVectorArgument(String identifier, String value);

    public abstract CvssVectorAttribute getVectorArgument(String identifier);

    @Override
    public abstract CvssVector clone();

    protected abstract BakedCvssVectorScores bakeScores();

    public abstract Map<String, CvssVectorAttribute[]> getAttributes();

    public BakedCvssVectorScores getBakedScores() {
        if (bakedScores == null) {
            bakedScores = bakeScores();
        }
        return bakedScores;
    }

    public void clearBakedScores() {
        bakedScores = null;
    }

    public JSONObject getApplicabilityCondition() {
        return applicabilityCondition;
    }

    public void putAllApplicabilityCondition(Map<String, Object> condition) {
        for (String key : condition.keySet()) {
            applicabilityCondition.put(key, condition.get(key));
        }
    }

    public void putAllApplicabilityCondition(JSONObject condition) {
        for (String key : condition.keySet()) {
            applicabilityCondition.put(key, condition.get(key));
        }
    }

    /* SOURCES */

    public CvssVector deriveAddSource(CvssSource source) {
        final CvssVector clone = this.clone();
        clone.addSource(source);
        return clone;
    }

    public CvssVector deriveAddSources(Collection<CvssSource> sources) {
        final CvssVector clone = this.clone();
        clone.addSources(sources);
        return clone;
    }

    protected void addSource(CvssSource source) {
        Objects.requireNonNull(source, "Vector source must not be null");
        this.sources.add(source);
    }

    protected void addSources(Collection<CvssSource> sources) {
        Objects.requireNonNull(sources, "Vector sources collection must not be null when adding multiple sources");
        sources.forEach(source -> Objects.requireNonNull(source, "Vector source must not be null when adding multiple sources " + sources));
        this.sources.addAll(sources);
    }

    public List<CvssSource> getCvssSources() {
        return sources;
    }

    public CvssSource getLatestSource() {
        if (!sources.isEmpty()) {
            return sources.get(sources.size() - 1);
        }
        return null;
    }

    public CvssSource getInitialSource() {
        if (!sources.isEmpty()) {
            return sources.get(0);
        }
        return null;
    }

    public CvssSource getCvssSource() {
        return getInitialSource();
    }

    public String getCombinedCvssSource(boolean includeVersion) {
        return CvssSource.toCombinedColumnHeaderString(sources, includeVersion);
    }

    public boolean containsSource(CvssSource source) {
        return sources.contains(source);
    }

    /* APPLYING VECTORS */

    public int applyVector(String vector) {
        if (vector == null) return 0;
        final String normalizedVector = normalizeVector(vector);
        return applyNormalizedVector(normalizedVector);
    }

    protected int applyNormalizedVector(String normalizedVector) {
        if (normalizedVector == null) return 0;

        if (normalizedVector.isEmpty()) return 0;

        final String[] arguments = normalizedVector.split("/");

        int appliedCount = 0;
        for (String argument : arguments) {
            if (StringUtils.isEmpty(argument)) continue;
            final String[] parts = argument.split(":", 2);

            if (parts.length == 2) {
                if (applyVectorArgument(parts[0], parts[1])) {
                    appliedCount++;
                }
            } else {
                LOG.debug("Unknown vector argument: [{}]", argument);
            }
        }

        completeVector();

        bakedScores = null;
        return appliedCount;
    }

    int applyVectorPartsIf(String vector, Function<CvssVector, Double> scoreType, boolean lower) {
        if (vector == null) return 0;

        final String normalizedVector = normalizeVector(vector);
        if (normalizedVector.isEmpty()) return 0;

        final String[] arguments = normalizedVector.split("/");

        int appliedPartsCount = 0;

        for (String argument : arguments) {
            if (StringUtils.isEmpty(argument)) continue;
            final String[] parts = argument.split(":", 2);

            final CvssVector clone = this.clone();

            final double currentScore = scoreType.apply(clone);

            if (parts.length == 2) {
                clone.applyVectorArgument(parts[0], parts[1]);
                clone.completeVector();
                final double newScore = scoreType.apply(clone);

                if (lower) {
                    if (newScore <= currentScore) {
                        appliedPartsCount += this.applyVectorArgument(parts[0], parts[1]) ? 1 : 0;
                        this.completeVector();
                    }
                } else {
                    if (newScore >= currentScore) {
                        appliedPartsCount += this.applyVectorArgument(parts[0], parts[1]) ? 1 : 0;
                        this.completeVector();
                    }
                }
            } else {
                LOG.debug("Unknown vector argument: [{}]", argument);
            }
        }

        bakedScores = null;
        return appliedPartsCount;
    }

    public int applyVector(CvssVector vector) {
        if (vector == null) return 0;
        return applyVector(vector.toString());
    }

    public CvssVector applyVectorAndReturn(CvssVector vector) {
        if (vector == null) return this;
        applyVector(vector.toString());
        return this;
    }

    public <T extends CvssVector> int applyVectorPartsIfLower(T vector, Function<T, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector.toString(), (Function<CvssVector, Double>) scoreType, true);
    }

    public int applyVectorPartsIfLower(String vector, Function<CvssVector, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector, scoreType, true);
    }

    public <T extends CvssVector> int applyVectorPartsIfHigher(T vector, Function<T, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector.toString(), (Function<CvssVector, Double>) scoreType, false);
    }

    public int applyVectorPartsIfHigher(String vector, Function<CvssVector, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector, scoreType, false);
    }

    protected static String normalizeVector(String vector) {
        return vector.toUpperCase()
                .replace("(", "")
                .replace(")", "")
                .replaceAll("CVSS:\\d+\\.?\\d?", "")
                .replaceAll("^/", "")
                .trim();
    }

    public static <T extends CvssVector> T parseVectorOnlyIfKnownAttributes(String vector, Supplier<T> constructor) {
        final T cvssVector = constructor.get();
        final String normalizedVector = normalizeVector(vector);
        final int knownAttributes = cvssVector.applyNormalizedVector(normalizedVector);
        final int allAttributes = normalizedVector.split("/").length;

        return allAttributes - knownAttributes > 0 ? null : cvssVector;
    }

    public static <T extends CvssVector> String getVersionName(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown or unregistered CVSS version: null");
        } else if (Cvss2.class.isAssignableFrom(clazz)) {
            return Cvss2.getVersionName();
        } else if (Cvss3P1.class.isAssignableFrom(clazz)) {
            return Cvss3P1.getVersionName();
        } else if (Cvss4P0.class.isAssignableFrom(clazz)) {
            return Cvss4P0.getVersionName();
        } else {
            throw new IllegalArgumentException("Unknown or unregistered CVSS version: " + clazz.getSimpleName());
        }
    }

    public static Class<? extends CvssVector> classFromVersionName(String versionName) {
        if (versionName == null) {
            throw new IllegalArgumentException("Unknown or unregistered CVSS version: null");
        } else if (versionName.equals(Cvss2.getVersionName())) {
            return Cvss2.class;
        } else if (versionName.equals(Cvss3P1.getVersionName())) {
            return Cvss3P1.class;
        } else if (versionName.equals(Cvss4P0.getVersionName())) {
            return Cvss4P0.class;
        } else {
            throw new IllegalArgumentException("Unknown or unregistered CVSS version: " + versionName);
        }
    }

    /**
     * Attempts to parse the given vector. This is split into two steps:
     * <ol>
     *     <li>Try to discover the vector version using the prefix (e.g. <code>CVSS:3.1</code>)</li>
     *     <li>Attempt to find a vector implementation that can parse all attributes on the vector, uses the {@link #parseVectorOnlyIfKnownAttributes(String, Supplier)} method</li>
     * </ol>
     *
     * @param vector the vector to parse
     * @return the parsed vector or <code>null</code> if the vector could not be parsed
     */
    public static CvssVector parseVector(String vector) {
        if (vector == null || StringUtils.isEmpty(CvssVector.normalizeVector(vector))) {
            return null;
        }

        if (vector.startsWith("CVSS:2.0")) {
            return new Cvss2(vector);
        } else if (vector.startsWith("CVSS:3.0")) {
            return new Cvss3P0(vector);
        } else if (vector.startsWith("CVSS:3.1")) {
            return new Cvss3P1(vector);
        } else if (vector.startsWith("CVSS:4.0")) {
            return new Cvss4P0(vector);

        } else {
            final Cvss2 potentialCvss2Vector = CvssVector.parseVectorOnlyIfKnownAttributes(vector, Cvss2::new);
            if (potentialCvss2Vector != null) {
                return potentialCvss2Vector;
            }

            final Cvss3P1 potentialCvss3Vector = CvssVector.parseVectorOnlyIfKnownAttributes(vector, Cvss3P1::new);
            if (potentialCvss3Vector != null) {
                return potentialCvss3Vector;
            }

            final Cvss4P0 potentialCvss4P0Vector = CvssVector.parseVectorOnlyIfKnownAttributes(vector, Cvss4P0::new);
            if (potentialCvss4P0Vector != null) {
                return potentialCvss4P0Vector;
            }

            LOG.warn("Cannot fully determine CVSS version in vector [{}]", vector);
            return null;
        }
    }

    /* SERIALIZATION */

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        json.put("sources", new JSONArray(sources.stream().map(CvssSource::toColumnHeaderString).toArray()));
        json.put("vector", toString());
        if (applicabilityCondition != null) {
            json.put("condition", applicabilityCondition);
        }
        return json;
    }

    public static CvssVector fromJson(JSONObject json) {
        final List<CvssSource> sources;
        if (json.has("source")) {
            final CvssSource source = CvssSource.fromColumnHeaderString(json.getString("source"));
            sources = Collections.singletonList(source);
        } else if (json.has("sources")) {
            final JSONArray sourcesJson = json.getJSONArray("sources");
            sources = new ArrayList<>();
            for (int i = 0; i < sourcesJson.length(); i++) {
                sources.add(CvssSource.fromColumnHeaderString(sourcesJson.getString(i)));
            }
            if (sources.isEmpty()) {
                throw new IllegalArgumentException("No sources found in json [" + json + "]");
            }
        } else {
            sources = Collections.emptyList();
        }

        final CvssVector vector;
        if (!sources.isEmpty()) {
            vector = sources.get(0).parseVector(json.getString("vector"));
        } else {
            vector = parseVector(json.getString("vector"));
        }

        if (json.has("condition")) {
            final JSONObject condition = json.getJSONObject("condition");
            for (String key : condition.keySet()) {
                vector.getApplicabilityCondition().put(key, condition.get(key));
            }
            vector.addSources(sources);
        } else {
            vector.addSources(sources);
        }
        return vector;
    }

    public static JSONArray toJson(List<CvssVector> vectorsList) {
        final JSONArray json = new JSONArray();
        vectorsList.forEach(sourcedCvssVector -> json.put(sourcedCvssVector.toJson()));
        return json;
    }

    public static List<CvssVector> fromJson(JSONArray json) {
        final List<CvssVector> vectorsList = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            vectorsList.add(CvssVector.fromJson(json.getJSONObject(i)));
        }
        return vectorsList;
    }

    public interface CvssVectorAttribute {
        String getIdentifier();

        String getShortIdentifier();
    }
}
