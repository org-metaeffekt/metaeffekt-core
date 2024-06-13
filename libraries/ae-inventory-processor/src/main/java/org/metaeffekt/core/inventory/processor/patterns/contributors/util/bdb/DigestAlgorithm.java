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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

public enum DigestAlgorithm {
    MD5(1),
    SHA1(2),
    SHA256(8);

    private final int value;

    DigestAlgorithm(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DigestAlgorithm fromValue(int value) {
        switch (value) {
            case 1:
                return MD5;
            case 2:
                return SHA1;
            case 8:
                return SHA256;
            default:
                throw new IllegalArgumentException("Unknown DigestAlgorithm value: " + value);
        }
    }
}
