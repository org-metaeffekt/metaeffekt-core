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

package org.metaeffekt.core.itest.common.fluent.base;

import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface BaseListFilter<T, SELF extends BaseListFilter<T, SELF>> extends BaseListDescriptor<T, SELF> {
    List<T> getItemList();

    SELF createNewInstance(List<T> filteredList);

    default SELF filter(Predicate<T> predicate) {
        List<T> filteredItems = getItemList().stream().filter(predicate).collect(Collectors.toList());
        return createNewInstance(filteredItems);
    }

    default SELF filter(NamedBasePredicate<T> namedPredicate) {
        return filter(namedPredicate.getPredicate());
    }

    default SELF with(NamedBasePredicate<T>... namedPredicates) {
        List<T> filteredList = getItemList();
        for (NamedBasePredicate<T> namedPredicate : namedPredicates) {
            filteredList = filteredList.stream().filter(namedPredicate.getPredicate()).collect(Collectors.toList());
        }
        return createNewInstance(filteredList);
    }
}
