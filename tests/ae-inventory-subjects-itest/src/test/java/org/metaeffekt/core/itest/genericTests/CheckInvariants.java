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
package org.metaeffekt.core.itest.genericTests;

import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.itest.inventory.dsl.predicates.Exists;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.Exists.withAttribute;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.Not.not;

public class CheckInvariants {

    public static void assertInvariants(Analysis analysis){
        assertAtLeastOneArtifact(analysis);
        // TODO Type detection not stable / available for all artifatcs
        //assertNoMissingTypes(analysis);
        assertNoErrors(analysis);
    }

    public static void assertNoErrors(Analysis analysis) {
        analysis.selectArtifacts(withAttribute("Errors"))
                .logArtifactList("Errors")
                .assertEmpty();
    }

    public static void assertAtLeastOneArtifact(Analysis analysis) {
        analysis.selectArtifacts()
                .hasSizeGreaterThan(0);
    }

    public static void assertNoMissingTypes(Analysis analysis) {
        analysis.selectArtifacts(not(Exists.withAttribute(TYPE)))
                .assertEmpty();
    }
}
