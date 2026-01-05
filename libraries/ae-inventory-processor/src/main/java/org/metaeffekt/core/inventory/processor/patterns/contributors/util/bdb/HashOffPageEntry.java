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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HashOffPageEntry {
    private byte pageType;      // 0: Page type.
    private byte[] unused = new byte[3];  // 1-3: Padding, unused.
    private long pageNo;         // 4-7: Offpage page number.
    private long length;         // 8-11: Total length of item.

    public static HashOffPageEntry parseHashOffPageEntry(byte[] data, boolean swapped) {
        ByteOrder order = swapped ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(order);

        HashOffPageEntry entry = new HashOffPageEntry();
        entry.pageType = buffer.get(0);
        // see https://morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/
        ((java.nio.Buffer) buffer).position(1);
        buffer.get(entry.unused);
        entry.pageNo = buffer.getInt(4);
        entry.length = buffer.getInt(8);

        return entry;
    }

    // Getters and setters as needed
    public byte getPageType() {
        return pageType;
    }

    public void setPageType(byte pageType) {
        this.pageType = pageType;
    }

    public byte[] getUnused() {
        return unused;
    }

    public void setUnused(byte[] unused) {
        this.unused = unused;
    }

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}

