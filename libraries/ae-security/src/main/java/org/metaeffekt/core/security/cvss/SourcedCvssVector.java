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

import java.util.*;

public class SourcedCvssVector<T extends CvssVector> implements Cloneable {

    private final T cvssVector;
    private final CvssSource<T> cvssSource;
    private final JSONObject applicabilityCondition;

    public SourcedCvssVector(CvssSource<T> source, T vector) {
        this(source, vector, null);
    }

    public SourcedCvssVector(CvssSource<T> source, String vector) {
        this(source, vector, null);
    }

    public SourcedCvssVector(CvssSource<T> source, T vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSource = source;
        this.cvssVector = vector;
        this.applicabilityCondition = applicabilityCondition;
    }

    public SourcedCvssVector(CvssSource<T> source, String vector, JSONObject applicabilityCondition) {
        assertNotNull(source, vector);
        this.cvssSource = source;
        this.cvssVector = this.cvssSource.parseVector(vector);
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

    public JSONObject getApplicabilityCondition() {
        return applicabilityCondition;
    }

    public T getCvssVector() {
        return cvssVector;
    }

    public CvssSource<T> getCvssSource() {
        return cvssSource;
    }

    @Override
    public SourcedCvssVector<T> clone() {
        return fromUnknownType(this.cvssSource, this.cvssVector.clone(), this.applicabilityCondition == null ? null : new JSONObject(this.applicabilityCondition.toString()));
    }

    @Override
    public String toString() {
        return "{" + cvssSource + ": " + cvssVector + (applicabilityCondition != null ? " " + applicabilityCondition : "") + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourcedCvssVector<?> that = (SourcedCvssVector<?>) o;
        return Objects.equals(cvssVector, that.cvssVector) && Objects.equals(cvssSource, that.cvssSource) && Objects.equals(applicabilityCondition, that.applicabilityCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cvssVector, cvssSource, applicabilityCondition);
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        json.put("source", cvssSource.toColumnHeaderString());
        json.put("vector", cvssVector.toString());
        if (applicabilityCondition != null) {
            json.put("condition", applicabilityCondition);
        }
        return json;
    }

    public static <T extends CvssVector> SourcedCvssVector<T> fromJson(JSONObject json) {
        final CvssSource<T> source = (CvssSource<T>) CvssSource.fromColumnHeaderString(json.getString("source"));
        final T vector = source.parseVector(json.getString("vector"));
        if (json.has("condition")) {
            return new SourcedCvssVector<>(source, vector, json.getJSONObject("condition"));
        } else {
            return new SourcedCvssVector<>(source, vector);
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

    public static void assertNotNull(CvssSource<?> source, Object vector) {
        if (source == null || vector == null) {
            throw new IllegalArgumentException("Source [" + source + "] and vector [" + vector + "] must not be null");
        }
    }
}
