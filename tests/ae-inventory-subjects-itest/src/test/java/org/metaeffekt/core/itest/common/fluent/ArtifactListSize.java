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
package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.itest.common.predicates.NamedArtifactPredicate;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListSize extends ArtifactListFilter {

    default ArtifactList hasSizeGreaterThan(int boundary) {
        assertThat(getArtifactList()).as("Size of List where [" + getDescription() + "] should be greater than " + boundary).hasSizeGreaterThan(boundary);
        return (ArtifactList) this;
    }

    default ArtifactList hasSizeOf(int boundary) {
        assertThat(getArtifactList())
                .as("Size of List where [" + getDescription() + "] should be equal to " + boundary)
                .hasSize(boundary);
        return (ArtifactList) this;
    }

    default void assertEmpty() {
        assertThat(getArtifactList()).as("List where [" + getDescription() + "] must be Emtpy").isEmpty();
    }

    default ArtifactList assertEmpty(NamedArtifactPredicate artifactPredicate) {
        filter(artifactPredicate).assertEmpty();
        return (ArtifactList) this;
    }


    default ArtifactList assertNotEmpty() {
        return hasSizeGreaterThan(0);
    }


    default ArtifactList assertNotEmpty(NamedArtifactPredicate artifactPredicate) {
        filter(artifactPredicate).hasSizeGreaterThan(0);
        return (ArtifactList) this;
    }


}
