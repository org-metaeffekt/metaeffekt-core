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
package org.metaeffekt.core.security.cvss.v3;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;

import java.util.Collection;
import java.util.Optional;

public final class Cvss3P0 extends Cvss3 {

    public Cvss3P0() {
        super();
    }

    public Cvss3P0(String vector) {
        super(vector);
    }

    public Cvss3P0(String vector, CvssSource source) {
        super(vector, source);
    }

    public Cvss3P0(String vector, CvssSource source, JSONObject applicabilityCondition) {
        super(vector, source, applicabilityCondition);
    }

    public Cvss3P0(String vector, Collection<CvssSource> sources, JSONObject applicabilityCondition) {
        super(vector, sources, applicabilityCondition);
        super.applyVector(vector);
    }

    @Override
    protected double calculateAdjustedImpact() {
        double miss = calculateMISS();
        if (isModifiedScope())
            return Scope.SCOPE_UNCHANGED_FACTOR * miss;
        else return Scope.SCOPE_CHANGED_FACTOR * (miss - 0.029) - 3.25 * Math.pow(miss - 0.02, 15);
    }

    @Override
    public double roundUp(double value) {
        return Math.ceil(value * 10) / 10;
    }

    @Override
    public Cvss3P0 clone() {
        return super.cloneInternal(new Cvss3P0());
    }

    @Override
    public Optional<Cvss3P0> optionalParse(String vector) {
        if (vector == null || StringUtils.isEmpty(MultiScoreCvssVector.normalizeVector(vector))) {
            return Optional.empty();
        }
        return Optional.of(new Cvss3P0(vector));
    }
}
