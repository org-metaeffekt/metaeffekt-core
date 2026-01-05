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

package org.metaeffekt.core.itest.common.predicates;

public class TokenAtPosition<T, E extends Enum<E>> extends TokenStartsWith<T, E> {

    public TokenAtPosition(E attributeKey, String prefix, String[] separators, int position) {
        super(attributeKey, prefix, separators, position);
    }

    public static <T, E extends Enum<E>> NamedBasePredicate<T> tokenAtPosition(E attributeKey, String prefix, String[] separators, int position) {
        return new TokenAtPosition<>(attributeKey, prefix, separators, position);
    }
}
