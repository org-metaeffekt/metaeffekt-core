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
package org.metaeffekt.core.security.cvss.processor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a list of CVSS vectors.<br>
 * The {@link CvssVectorSet} class provides methods to add, manipulate and retrieve CVSS vectors based on several
 * conditions.<br>
 * A method {@link CvssVectorSet#removeForSourceAndCondition(CvssSource, JSONObject)} exists that allows for removing
 * vectors that remove a vector based on the combination of a {@link CvssSource} vector applicability condition.
 */
public class CvssVectorSet implements Cloneable {

    private final List<CvssVector> cvssVectors = new ArrayList<>();

    public CvssVectorSet() {
    }

    /**
     * Creates a new instance of CvssVectorSet based on another CvssVectorSet.<br>
     * The {@link CvssVector} instances are cloned.
     *
     * @param other The CvssVectorSet to be cloned.
     */
    public CvssVectorSet(CvssVectorSet other) {
        this.addAllCvssVectors(other);
    }

    public CvssVectorSet(Collection<CvssVector> other) {
        this.addAllCvssVectors(other);
    }

    public void removeForSourceAndCondition(CvssSource source, JSONObject condition) {
        this.cvssVectors.removeIf(sourcedCvssVector -> Objects.equals(sourcedCvssVector.getCvssSource(), source) && Objects.equals(sourcedCvssVector.getApplicabilityCondition(), condition));
    }

    public void removeForNonMatchingConditionAttribute(String attribute, Object value) {
        this.cvssVectors.removeIf(sourcedCvssVector -> !Objects.equals(sourcedCvssVector.getApplicabilityCondition().opt(attribute), value));
    }

    public void addCvssVector(CvssSource source, String cvssVector) {
        final CvssVector vector = source.parseVector(cvssVector).deriveAddSource(source);
        this.addCvssVector(vector);
    }

    public void addCvssVector(CvssVector cvssVector) {
        this.cvssVectors.add(cvssVector);
    }

    public void addAllCvssVectors(Collection<CvssVector> cvssVectors) {
        this.cvssVectors.addAll(cvssVectors);
    }

    public void addAllCvssVectors(CvssVectorSet vectorSet) {
        this.cvssVectors.addAll(vectorSet.getCvssVectors().stream().map(CvssVector::clone).collect(Collectors.toList()));
    }

    public List<CvssVector> getCvssVectors() {
        return cvssVectors;
    }

    public CvssVector getCvssVectorBySource(CvssSource source) {
        return this.cvssVectors.stream()
                .filter(sourcedCvssVector -> sourcedCvssVector.getCvssSources().stream().anyMatch(s -> s.equals(source)))
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
    public CvssVectorSet clone() {
        return new CvssVectorSet(this);
    }

    @Override
    public String toString() {
        return this.cvssVectors.toString();
    }

    public JSONArray toJson() {
        return CvssVector.toJson(this.cvssVectors);
    }

    public static CvssVectorSet fromJson(JSONArray json) {
        return new CvssVectorSet(CvssVector.fromJson(json));
    }
}
