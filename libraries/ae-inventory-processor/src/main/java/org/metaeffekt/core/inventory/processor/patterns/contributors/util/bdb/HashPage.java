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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.HashOffPageEntry.parseHashOffPageEntry;

public class HashPage {
    private static final Logger LOG = LoggerFactory.getLogger(HashPage.class);
    private byte[] LSN = new byte[8]; // 00-07: LSN.
    private long pageNo; // 08-11: Current page number.
    private long previousPageNo; // 12-15: Previous page number.
    private long nextPageNo; // 16-19: Next page number.
    private short numEntries; // 20-21: Number of items on the page.
    private short freeAreaOffset; // 22-23: High free byte page offset.
    private byte treeLevel; // 24: Btree tree level.
    private byte pageType; // 25: Page type.

    private static final int HASH_INDEX_ENTRY_SIZE = 2;
    private static final int PAGE_HEADER_SIZE = 26;
    private static final byte OVERFLOW_PAGE_TYPE = 7;
    private static final byte HASH_OFF_INDEX_PAGE_TYPE = 3;
    private static final byte HASH_OFF_PAGE_SIZE = 12;

    public static HashPage parseHashPage(byte[] data, boolean swapped) throws IOException {
        ByteOrder order = swapped ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(order);

        HashPage hashPage = new HashPage();
        buffer.get(hashPage.LSN);
        hashPage.pageNo = Integer.toUnsignedLong(buffer.getInt(8));
        hashPage.previousPageNo = Integer.toUnsignedLong(buffer.getInt(12));
        hashPage.nextPageNo = Integer.toUnsignedLong(buffer.getInt(16));
        hashPage.numEntries = buffer.getShort(20);
        hashPage.freeAreaOffset = buffer.getShort(22);
        hashPage.treeLevel = buffer.get(24);
        hashPage.pageType = buffer.get(25);

        return hashPage;
    }

    public static byte[] hashPageValueContent(RandomAccessFile file, byte[] pageData, short hashPageIndex, long pageSize, boolean swapped) throws IOException {
        byte valuePageType = pageData[hashPageIndex];

        if (valuePageType != HASH_OFF_INDEX_PAGE_TYPE) {
            throw new IOException("only HOFFPAGE types supported (" + valuePageType + ")");
        }

        byte[] hashOffPageEntryBuff = Arrays.copyOfRange(pageData, hashPageIndex, hashPageIndex + HASH_OFF_PAGE_SIZE);

        HashOffPageEntry entry = parseHashOffPageEntry(hashOffPageEntryBuff, swapped);

        List<byte[]> hashValueList = new ArrayList<>();
        long currentPageNo = entry.getPageNo();

        try {
            while (currentPageNo != 0) {
                long pageStart = pageSize * currentPageNo;
                file.seek(pageStart);

                byte[] currentPageBuff = slice(file, (int) pageSize);

                HashPage currentPage = parseHashPage(currentPageBuff, swapped);
                if (currentPage.pageType != OVERFLOW_PAGE_TYPE) {
                    continue;
                }

                byte[] hashValueBytes;
                if (currentPage.nextPageNo == 0) {
                    hashValueBytes = Arrays.copyOfRange(currentPageBuff, PAGE_HEADER_SIZE, PAGE_HEADER_SIZE + currentPage.freeAreaOffset);
                } else {
                    hashValueBytes = Arrays.copyOfRange(currentPageBuff, PAGE_HEADER_SIZE, currentPageBuff.length);
                }

                hashValueList.add(hashValueBytes);
                currentPageNo = currentPage.nextPageNo;
            }
        } catch (IOException e) {
            throw new IOException("error reading hash value content", e);
        }

        int totalLength = hashValueList.stream().mapToInt(b -> b.length).sum();
        byte[] hashValue = new byte[totalLength];
        int offset = 0;
        for (byte[] bytes : hashValueList) {
            System.arraycopy(bytes, 0, hashValue, offset, bytes.length);
            offset += bytes.length;
        }

        return hashValue;
    }

    public static List<Short> hashPageValueIndexes(byte[] data, int entries, boolean swapped) throws Exception {
        if (entries % 2 != 0) {
            throw new Exception("Invalid hash index: entries should only come in pairs (" + entries + ")");
        }

        ByteOrder order = swapped ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        List<Short> hashIndexValues = new ArrayList<>();

        // Every entry is a 2-byte offset that points somewhere in the current database page.
        int hashIndexSize = entries * HASH_INDEX_ENTRY_SIZE;
        byte[] hashIndexData = Arrays.copyOfRange(data, PAGE_HEADER_SIZE, PAGE_HEADER_SIZE + hashIndexSize);

        // Data is stored in key-value pairs, skip over keys and only keep values
        final int keyValuePairSize = 2 * HASH_INDEX_ENTRY_SIZE;

        for (int idx = 0; idx < hashIndexData.length; idx += HASH_INDEX_ENTRY_SIZE) {
            if ((idx - HASH_INDEX_ENTRY_SIZE) % keyValuePairSize == 0) {
                ByteBuffer buffer = ByteBuffer.wrap(hashIndexData, idx, HASH_INDEX_ENTRY_SIZE).order(order);
                short value = buffer.getShort();
                hashIndexValues.add(value);
            }
        }

        return hashIndexValues;
    }

    public static byte[] slice(RandomAccessFile reader, int n) throws IOException {
        byte[] newBuff = new byte[n];
        int numRead;
        try {
            numRead = reader.read(newBuff);
        } catch (IOException e) {
            throw new IOException("Error during file read: ", e);
        }

        if (numRead != n) {
            throw new IOException("Short page size: " + n + "!=" + numRead);
        }

        return newBuff;
    }

    public byte getPageType() {
        return pageType;
    }

    public short getNumEntries() {
        return numEntries;
    }
}

