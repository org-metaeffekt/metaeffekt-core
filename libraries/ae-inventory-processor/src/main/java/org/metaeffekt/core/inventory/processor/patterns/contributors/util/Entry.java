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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util;

public class Entry {
    private byte[] value;
    private Exception error;

    public Entry(byte[] value) {
        this.value = value;
    }

    public Entry(Exception error) {
        this.error = error;
    }

    public Entry() {
        this.value = null;
        this.error = null;
    }

    public Entry(byte[] value, Exception error) {
        this.value = value;
        this.error = error;
    }

    public byte[] getValue() {
        return value;
    }

    public Exception getError() {
        return error;
    }
}
