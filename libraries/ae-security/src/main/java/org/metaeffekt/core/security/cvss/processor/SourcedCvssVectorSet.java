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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.SourcedCvssVector;

import java.util.*;

/**
 * Represents a set of CVSS vectors associated with their respective sources.<br>
 * The {@link SourcedCvssVectorSet} class provides methods to manipulate and retrieve CVSS vectors.
 */
public class SourcedCvssVectorSet implements Cloneable {

    private final List<SourcedCvssVector<?>> cvssVectors = new ArrayList<>();

    public SourcedCvssVectorSet() {
    }

    /**
     * Creates a new instance of CvssVectorSet based on another CvssVectorSet.<br>
     * The {@link CvssVector} instances are cloned.
     *
     * @param other The CvssVectorSet to be cloned.
     */
    public SourcedCvssVectorSet(SourcedCvssVectorSet other) {
        this.addAllCvssVectors(other);
    }

    public SourcedCvssVectorSet(Collection<SourcedCvssVector<?>> other) {
        this.addAllCvssVectors(other);
    }

    public void removeForSourceAndCondition(CvssSource<?> source, JSONObject condition) {
        this.cvssVectors.removeIf(sourcedCvssVector -> Objects.equals(sourcedCvssVector.getCvssSource(), source) && Objects.equals(sourcedCvssVector.getApplicabilityCondition(), condition));
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link SourcedCvssVectorSet}.<br>
     * Will overwrite any existing {@link CvssVector} for an identical source.
     *
     * @param <T>    the type of the {@link CvssVector} being set.
     * @param source The source for which the vector is being set.
     * @param vector The {@link CvssVector} to be set.
     */
    public <T extends CvssVector> void addCvssVector(CvssSource<T> source, T vector) {
        SourcedCvssVector.assertNotNull(source, vector);
        this.addCvssVector(new SourcedCvssVector<>(source, vector));
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link SourcedCvssVectorSet}.<br>
     * Will overwrite any existing {@link CvssVector} for an identical source.<br>
     * This method will parse the vector string into a {@link CvssVector} instance using the
     * {@link CvssSource#parseVector(String)} method of the source.
     *
     * @param <T>    the type of the {@link CvssVector} being set
     * @param source The source for which the vector is being set.
     * @param vector The {@link String} representation of the {@link CvssVector} to be parsed and set.
     * @throws IllegalArgumentException if source or vector is null.
     * @throws IllegalStateException    if the vector string cannot be parsed into a {@link CvssVector} instance of type T.
     */
    public <T extends CvssVector> void addCvssVector(CvssSource<T> source, String vector) {
        SourcedCvssVector.assertNotNull(source, vector);
        this.addCvssVector(new SourcedCvssVector<>(source, vector));
    }

    public <T extends CvssVector> void addCvssVector(SourcedCvssVector<T> sourcedVector) {
        this.removeForSourceAndCondition(sourcedVector.getCvssSource(), sourcedVector.getApplicabilityCondition());
        this.cvssVectors.add(sourcedVector);
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link SourcedCvssVectorSet}.<br>
     * Will overwrite any existing {@link CvssVector} for an identical source.<br>
     * Allows setting a {@link CvssVector} without compile-time vector version type checking.
     *
     * @param source The source for which the vector is being set.
     * @param vector The {@link CvssVector} to be set.
     * @throws IllegalArgumentException if source or vector is null or if they are not compatible.
     */
    public void addCvssVectorUnchecked(CvssSource<?> source, CvssVector vector) {
        SourcedCvssVector.assertNotNull(source, vector);
        this.addCvssVector(SourcedCvssVector.fromUnknownType(source, vector));
    }

    public void addAllCvssVectors(SourcedCvssVectorSet other) {
        other.cvssVectors.forEach(this::addCvssVector);
    }

    public void addAllCvssVectors(Collection<SourcedCvssVector<?>> other) {
        other.forEach(this::addCvssVector);
    }

    public List<SourcedCvssVector<?>> getCvssVectors() {
        return cvssVectors;
    }

    public <T extends CvssVector> SourcedCvssVector<T> getCvssVectorBySource(CvssSource<T> source) {
        return (SourcedCvssVector<T>) this.cvssVectors.stream()
                .filter(sourcedCvssVector -> sourcedCvssVector.getCvssSource().equals(source))
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return cvssVectors.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public int size() {
        return cvssVectors.size();
    }

    public void clear() {
        cvssVectors.clear();
    }

    @Override
    public SourcedCvssVectorSet clone() {
        return new SourcedCvssVectorSet(this);
    }

    @Override
    public String toString() {
        return this.cvssVectors.toString();
    }

    public JSONArray toJson() {
        return SourcedCvssVector.toJson(this.cvssVectors);
    }

    public static SourcedCvssVectorSet fromJson(JSONArray json) {
        return new SourcedCvssVectorSet(SourcedCvssVector.fromJson(json));
    }
}
