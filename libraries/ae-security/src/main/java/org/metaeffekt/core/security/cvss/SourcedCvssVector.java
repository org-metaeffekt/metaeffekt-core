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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;

import java.util.*;

public class SourcedCvssVector<T extends CvssVector> implements Cloneable {

    private final T cvssVector;
    private final List<CvssSource<T>> cvssSources;
    private final JSONObject applicabilityCondition;

    public SourcedCvssVector(CvssSource<T> source, T vector) {
        this(source, vector, null);
    }

    public SourcedCvssVector(CvssSource<T> source, String vector) {
        this(source, vector, null);
    }

    public SourcedCvssVector(CvssSource<T> source, T vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSources = Collections.singletonList(source);
        this.cvssVector = vector;
        this.applicabilityCondition = applicabilityCondition;
    }

    public SourcedCvssVector(CvssSource<T> source, String vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSources = Collections.singletonList(source);
        this.cvssVector = this.getCvssSource().parseVector(vector);
        this.applicabilityCondition = applicabilityCondition;
    }

    public SourcedCvssVector(Collection<CvssSource<T>> source, T vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSources = new ArrayList<>(source);
        this.cvssVector = vector;
        this.applicabilityCondition = applicabilityCondition;
    }

    public SourcedCvssVector(Collection<CvssSource<T>> source, String vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSources = new ArrayList<>(source);
        this.cvssVector = this.getCvssSource().parseVector(vector);
        this.applicabilityCondition = applicabilityCondition;
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromUnknownType(CvssSource<T> source, CvssVector vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        if (source.getVectorClass() != vector.getClass()) {
            throw new IllegalArgumentException("Source [" + source + "] is not compatible with vector [" + vector + "]");
        }
        return new SourcedCvssVector<>(source, (T) vector, applicabilityCondition);
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromUnknownType(CvssSource<T> source, CvssVector vector) {
        return fromUnknownType(source, vector, null);
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromUnknownType(Collection<CvssSource<T>> source, CvssVector vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        if (source.stream().noneMatch(s -> s.getVectorClass() == vector.getClass())) {
            throw new IllegalArgumentException("Source [" + source + "] is not compatible with vector [" + vector + "]");
        }
        return new SourcedCvssVector<>(source, (T) vector, applicabilityCondition);
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromUnknownType(Collection<CvssSource<T>> source, CvssVector vector) {
        return fromUnknownType(source, vector, null);
    }

    public JSONObject getApplicabilityCondition() {
        return applicabilityCondition;
    }

    public T getCvssVector() {
        return cvssVector;
    }

    public CvssSource<T> getCvssSource() {
        return cvssSources.get(0);
    }

    public List<CvssSource<T>> getCvssSources() {
        return cvssSources;
    }

    @Override
    public SourcedCvssVector<T> clone() {
        return fromUnknownType(this.cvssSources, this.cvssVector.clone(), this.applicabilityCondition == null ? null : new JSONObject(this.applicabilityCondition.toString()));
    }

    @Override
    public String toString() {
        return "{" + cvssSources + ": " + cvssVector + (applicabilityCondition != null ? " " + applicabilityCondition : "") + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourcedCvssVector<?> that = (SourcedCvssVector<?>) o;
        return Objects.equals(cvssVector, that.cvssVector) && Objects.equals(cvssSources, that.cvssSources) && Objects.equals(applicabilityCondition, that.applicabilityCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cvssVector, cvssSources, applicabilityCondition);
    }

    public BakedCvssVectorScores<T> bakeScores() {
        return BakedCvssVectorScores.fromNullableCvss(this);
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        json.put("sources", new JSONArray(cvssSources.stream().map(CvssSource::toColumnHeaderString).toArray()));
        json.put("vector", cvssVector.toString());
        if (applicabilityCondition != null) {
            json.put("condition", applicabilityCondition);
        }
        return json;
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromJson(JSONObject json) {
        final List<CvssSource<T>> sources;
        if (json.has("source")) {
            final CvssSource<T> source = (CvssSource<T>) CvssSource.fromColumnHeaderString(json.getString("source"));
            sources = Collections.singletonList(source);
        } else if (json.has("sources")) {
            final JSONArray sourcesJson = json.getJSONArray("sources");
            sources = new ArrayList<>();
            for (int i = 0; i < sourcesJson.length(); i++) {
                sources.add((CvssSource<T>) CvssSource.fromColumnHeaderString(sourcesJson.getString(i)));
            }
            if (sources.isEmpty()) {
                throw new IllegalArgumentException("No sources found in json [" + json + "]");
            }
        } else {
            throw new IllegalArgumentException("No source or sources found in json [" + json + "]");
        }

        final T vector = sources.get(0).parseVector(json.getString("vector"));
        if (json.has("condition")) {
            return new SourcedCvssVector<>(sources, vector, json.getJSONObject("condition"));
        } else {
            return new SourcedCvssVector<>(sources, vector, null);
        }
    }

    public static JSONArray toJson(List<SourcedCvssVector<?>> vectorsList) {
        final JSONArray json = new JSONArray();
        vectorsList.forEach(sourcedCvssVector -> json.put(sourcedCvssVector.toJson()));
        return json;
    }

    public static List<SourcedCvssVector<?>> fromJson(JSONArray json) {
        final List<SourcedCvssVector<?>> vectorsList = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            vectorsList.add(SourcedCvssVector.fromJson(json.getJSONObject(i)));
        }
        return vectorsList;
    }

    public SourcedCvssVector<T> deriveVector(JSONObject applicabilityCondition) {
        final SourcedCvssVector<T> cloneBase = this.clone();
        return new SourcedCvssVector<>(cloneBase.getCvssSource(), cloneBase.getCvssVector(), applicabilityCondition);
    }

    public SourcedCvssVector<T> deriveVector(T vector) {
        final SourcedCvssVector<T> cloneBase = this.clone();
        return new SourcedCvssVector<>(cloneBase.getCvssSource(), vector, cloneBase.getApplicabilityCondition());
    }

    public SourcedCvssVector<T> deriveAppendSourceSetVector(CvssSource<T> source, T vector) {
        final SourcedCvssVector<T> cloneBase = this.clone();
        final ArrayList<CvssSource<T>> modifiableSourcesList = new ArrayList<>(cloneBase.getCvssSources());
        modifiableSourcesList.add(source);
        return new SourcedCvssVector<>(modifiableSourcesList, vector, cloneBase.getApplicabilityCondition());
    }

    public static void assertNotNull(CvssSource<?> source, Object vector) {
        if (source == null || vector == null) {
            throw new IllegalArgumentException("Source [" + source + "] and vector [" + vector + "] must not be null");
        }
    }

    private static <T extends CvssVector> void assertNotNull(Collection<CvssSource<T>> sources, Object vector) {
        if (sources == null || vector == null) {
            throw new IllegalArgumentException("Sources [" + sources + "] and vector [" + vector + "] must not be null");
        }
        sources.forEach(source -> assertNotNull(source, vector));
    }
}
