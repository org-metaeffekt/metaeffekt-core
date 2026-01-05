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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

public class IndexEntry {
    private EntryInfo info;
    private int length;
    private int rdlen;
    private byte[] data;

    public EntryInfo getInfo() {
        return info;
    }

    public void setInfo(EntryInfo info) {
        this.info = info;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getRdlen() {
        return rdlen;
    }

    public void setRdlen(int rdlen) {
        this.rdlen = rdlen;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "IndexEntry{" +
                "info=" + info +
                ", length=" + length +
                ", rdlen=" + rdlen +
                ", data length=" + (data != null ? data.length : 0) +
                '}';
    }
}

