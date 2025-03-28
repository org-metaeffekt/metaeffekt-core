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
package org.metaeffekt.core.itest.common.predicates;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdMismatchesVersion implements NamedBasePredicate<Artifact> {

    private static final Logger LOG = LoggerFactory.getLogger(IdMismatchesVersion.class);

    public static final NamedBasePredicate<Artifact> ID_MISMATCHING_VERSION = new IdMismatchesVersion();

    private static final Pattern PATTERN = Pattern.compile("(?!\\.)(\\d+(\\.\\d+)+)(?:[-.][A-Z]+)?(?![\\d.])");

    public static boolean evaluate(Artifact artifact) {
        final String version = artifact.get(Artifact.Attribute.VERSION);
        final String id = artifact.get(Artifact.Attribute.ID);

        // the check here is quite simple; generous
        return !id.contains(version);
    }

    @Override
    public Predicate<Artifact> getPredicate() {
        return IdMismatchesVersion::evaluate;
    }

    @Override
    public String getDescription() {
        return "Artifact Version mismatch";
    }

    public static NamedBasePredicate<Artifact> idMismatchesVersion() {
        return ID_MISMATCHING_VERSION;
    }

}
