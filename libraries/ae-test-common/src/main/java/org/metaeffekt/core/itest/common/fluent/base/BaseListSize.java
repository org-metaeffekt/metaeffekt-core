/*
 * Copyright 2009-2026 the original author or authors.
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

package org.metaeffekt.core.itest.common.fluent.base;

import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface BaseListSize<T, SELF extends BaseListSize<T, SELF>> extends BaseListFilter<T, SELF> {
    List<T> getItemList();
    String getDescription();

    default SELF hasSizeGreaterThan(int boundary) {
        assertThat(getItemList()).as("Size of List where [" + getDescription() + "] should be greater than " + boundary).hasSizeGreaterThan(boundary);
        return (SELF) this;
    }

    default SELF hasSizeOf(int boundary) {
        assertThat(getItemList())
                .as("Size of List where [" + getDescription() + "] should be equal to " + boundary)
                .hasSize(boundary);
        return (SELF) this;
    }

    default void assertEmpty() {
        assertThat(getItemList()).as("List where [" + getDescription() + "] must be empty").isEmpty();
    }

    default SELF assertEmpty(NamedBasePredicate<T> basePredicate) {
        SELF filteredList = filter(basePredicate.getPredicate());
        assertThat(filteredList.getItemList()).as("Filtered list should be empty").isEmpty();
        return filteredList;
    }

    default SELF assertNotEmpty() {
       return hasSizeGreaterThan(0);
    }


    default SELF assertNotEmpty(NamedBasePredicate<T> basePredicate) {
        filter(basePredicate).hasSizeGreaterThan(0);
        return (SELF) this;
    }
}
