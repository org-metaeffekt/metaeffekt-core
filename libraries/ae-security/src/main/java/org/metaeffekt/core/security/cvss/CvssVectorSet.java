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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a set of CVSS vectors associated with their respective sources.<br>
 * The {@link CvssVectorSet} class provides methods to manipulate and retrieve CVSS vectors.
 */
public class CvssVectorSet implements Cloneable {

    private final Map<CvssSource<?>, CvssVector> cvssVectors = new LinkedHashMap<>();

    public CvssVectorSet() {
    }

    /**
     * Creates a new instance of CvssVectorSet based on another CvssVectorSet.<br>
     * The {@link CvssVector} instances are cloned.
     *
     * @param other The CvssVectorSet to be cloned.
     */
    public CvssVectorSet(CvssVectorSet other) {
        this.copyAllCvssVectors(other);
    }

    public CvssVectorSet(CvssSource<?> source, CvssVector vector) {
        cvssVectors.put(source, vector);
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link CvssVectorSet}.<br>
     * Will overwrite any existing {@link CvssVector} for an identical source.
     *
     * @param <T>    the type of the {@link CvssVector} being set.
     * @param source The source for which the vector is being set.
     * @param vector The {@link CvssVector} to be set.
     */
    public <T extends CvssVector> void setCvssVector(CvssSource<T> source, T vector) {
        this.cvssVectors.put(source, vector);
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link CvssVectorSet}.<br>
     * Will overwrite any existing {@link CvssVector} for an identical source.<br>
     * Allows setting a {@link CvssVector} without compile-time vector version type checking.
     *
     * @param source The source for which the vector is being set.
     * @param vector The {@link CvssVector} to be set.
     * @throws IllegalArgumentException if source or vector is null or if they are not compatible.
     */
    public void setCvssVectorUnchecked(CvssSource<?> source, CvssVector vector) {
        if (source == null || vector == null) {
            throw new IllegalArgumentException("Source [" + source + "] and vector [" + vector + "] must not be null");
        }
        if (source.getVectorClass() != vector.getClass()) {
            throw new IllegalArgumentException("Source [" + source + "] is not compatible with vector [" + vector + "]");
        }
        this.cvssVectors.put(source, vector);
    }

    /**
     * Sets the {@link CvssVector} for a specific source in the {@link CvssVectorSet}.<br>
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
    public <T extends CvssVector> void setCvssVector(CvssSource<T> source, String vector) {
        if (source == null || vector == null) {
            throw new IllegalArgumentException("Source [" + source + "] and vector [" + vector + "] must not be null");
        }
        final T cvssVector = source.parseVector(vector);
        this.setCvssVector(source, cvssVector);
    }

    public void copyAllCvssVectors(CvssVectorSet other) {
        for (Map.Entry<CvssSource<?>, CvssVector> entry : other.cvssVectors.entrySet()) {
            this.setCvssVectorUnchecked(entry.getKey(), entry.getValue().clone());
        }
    }

    public Map<CvssSource<?>, CvssVector> getCvssVectors() {
        return cvssVectors;
    }

    public boolean isEmpty() {
        return cvssVectors.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public JSONArray toJson() {
        return CvssSource.toJson(this.cvssVectors);
    }

    @Override
    public CvssVectorSet clone() {
        return new CvssVectorSet(this);
    }
}
