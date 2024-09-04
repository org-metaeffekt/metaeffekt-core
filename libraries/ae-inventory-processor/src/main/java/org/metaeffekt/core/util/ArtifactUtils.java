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
package org.metaeffekt.core.util;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Buffer class for methods to be consolidated with Artifact class in core project.
 */
public abstract class ArtifactUtils {

    public static final String CLASSIFICATION_SCAN = "scan";

    public static boolean hasScanClassification(Artifact artifact) {
        return hasClassification(artifact, CLASSIFICATION_SCAN);
    }

    public static boolean hasClassification(final Artifact artifact, final String queryClassification) {
        final String classification = artifact.getClassification();
        if (!StringUtils.isBlank(classification) && classification.contains(queryClassification)) {
            return splitToSet(classification, ",").contains(queryClassification);
        }
        return false;
    }

    public static Set<String> splitToSet(String string, String delimiter) {
        return splitToSet(string, delimiter, true);
    }

    public static Set<String> splitToSet(String string, String delimiter, boolean trim) {
        if (delimiter == null) {
            throw new IllegalArgumentException("Delimiter must not be null.");
        }
        if (!StringUtils.isBlank(string)) {
            final String[] split = string.split(delimiter);
            final Set<String> set = new LinkedHashSet<>();
            for (String item : split) {
                if (trim) {
                    set.add(item.trim());
                } else {
                    set.add(item);
                }
            }
            return Collections.unmodifiableSet(set);
        }
        return Collections.emptySet();
    }

}
