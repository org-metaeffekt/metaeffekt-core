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
package org.metaeffekt.core.itest.common.asserts;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.predicates.AttributeExists;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.ERRORS;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.itest.common.predicates.AttributeExists.withAttribute;
import static org.metaeffekt.core.itest.common.predicates.Not.not;

public interface AnalysisAsserts {

    Analysis getAnalysis();

    default void assertInvariants() {
        assertAtLeastOneArtifact();
        // TODO Type detection not stable / available for all artifatcs
        //assertNoMissingTypes(analysis);
        assertNoErrors();
    }

    default void assertNoErrors() {
        getAnalysis().selectArtifacts(withAttribute(Artifact::get, ERRORS))
                .logList("Errors")
                .assertEmpty();
    }

    default void assertAtLeastOneArtifact() {
        getAnalysis().selectArtifacts()
                .hasSizeGreaterThan(0);
    }

    default void assertNoMissingTypes() {
        getAnalysis().selectArtifacts(not(AttributeExists.withAttribute(Artifact::get, TYPE)))
                .assertEmpty();
    }
}
