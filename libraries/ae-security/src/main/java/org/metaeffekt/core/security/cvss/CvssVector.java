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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CvssVector {

    private final static Logger LOG = LoggerFactory.getLogger(CvssVector.class);

    public abstract String getName();

    public abstract String getWebEditorLink();

    public abstract int size();

    public abstract double getBaseScore();

    public abstract double getOverallScore();

    public abstract boolean isBaseFullyDefined();

    public abstract boolean isAnyBaseDefined();

    /**
     * Fills and removes attributes to produce a consistent vector for computations
     */
    protected abstract void completeVector();

    public abstract BakedCvssVectorScores<? extends CvssVector> bakeScores();

    protected abstract boolean applyVectorArgument(String identifier, String value);

    @Override
    public abstract CvssVector clone();

    public int applyVector(String vector) {
        if (vector == null) return 0;

        final String normalizedVector = normalizeVector(vector);
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

        return appliedCount;
    }

    <T extends CvssVector> int applyVectorPartsIf(String vector, Function<T, Double> scoreType, boolean lower) {
        if (vector == null) return 0;

        final String normalizedVector = normalizeVector(vector);
        if (normalizedVector.isEmpty()) return 0;

        final String[] arguments = normalizedVector.split("/");

        int appliedPartsCount = 0;

        for (String argument : arguments) {
            if (StringUtils.isEmpty(argument)) continue;
            final String[] parts = argument.split(":", 2);

            final CvssVector cloneBase = this.clone();
            final T clone = (T) cloneBase;

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

        return appliedPartsCount;
    }

    public int applyVector(CvssVector vector) {
        if (vector == null) return 0;
        return applyVector(vector.toString());
    }

    public CvssVector applyVectorChain(CvssVector vector) {
        if (vector == null) return this;
        applyVector(vector.toString());
        return this;
    }

    public <T extends CvssVector> int applyVectorPartsIfLower(T vector, Function<T, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector.toString(), scoreType, true);
    }

    public <T extends CvssVector> int applyVectorPartsIfHigher(T vector, Function<T, Double> scoreType) {
        if (vector == null) return 0;
        return applyVectorPartsIf(vector.toString(), scoreType, false);
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
        final int unknownAttributes = cvssVector.applyVector(vector);
        return unknownAttributes > 0 ? null : cvssVector;
    }

    public static String getVersionName(Class<? extends CvssVector> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown or unregistered CVSS version: null");
        } else if (clazz == Cvss2.class) {
            return Cvss2.getVersionName();
        } else if (clazz == Cvss3P1.class) {
            return Cvss3P1.getVersionName();
        } else if (clazz == Cvss4P0.class) {
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
        } else if (vector.startsWith("CVSS:3.1") || vector.startsWith("CVSS:3.0")) {
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
}
