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

import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Database;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Entry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.HashPage.hashPageValueIndexes;
import static org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.HashPage.slice;

public class BerkeleyDB implements Database {
    private final RandomAccessFile file;
    private final HashMetaDataPage hashMetaData;

    private static final Set<Integer> validPageSizes = new HashSet<>(Arrays.asList(512, 1024, 2048, 4096, 8192, 16384, 32768));
    public static final byte HASH_UNSORTED_PAGE_TYPE = 2;
    public static final byte HASH_PAGE_TYPE = 13; // sorted hash page
    private static final byte HASH_OFF_INDEX_PAGE_TYPE = 3;


    public BerkeleyDB(RandomAccessFile file, HashMetaDataPage hashMetaData) {
        this.file = file;
        this.hashMetaData = hashMetaData;
    }

    @Override
    public Database open(String path) {
        try {
            RandomAccessFile file = new RandomAccessFile(path, "r");

            // Read file to byte array of length 512
            ByteBuffer buffer = ByteBuffer.allocate(512);
            byte[] metadataBuff = buffer.array();
            file.read(metadataBuff);
            file.seek(0);

            HashMetaDataPage hashMetadata;
            try {
                hashMetadata = HashMetaDataPage.parseHashMetadataPage(metadataBuff);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!validPageSizes.contains((int) hashMetadata.getPageSize())) {
                throw new IOException("Unexpected page size: " + hashMetadata.getPageSize());
            }

            return new BerkeleyDB(file, hashMetadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws IOException {
        file.close();
    }

    public BlockingQueue<Entry> read() {
        BlockingQueue<Entry> entries = new LinkedBlockingQueue<>();

        new Thread(() -> {
            try {
                for (long pageNum = 0; pageNum <= hashMetaData.getLastPageNo(); pageNum++) {
                    byte[] pageData = slice(file, (int) hashMetaData.getPageSize());

                    // Keep track of the start of the next page for the next iteration
                    long endOfPageOffset = file.getFilePointer();

                    HashPage hashPageHeader = HashPage.parseHashPage(pageData, hashMetaData.isSwapped());

                    if (hashPageHeader.getPageType() != HASH_UNSORTED_PAGE_TYPE &&
                            hashPageHeader.getPageType() != HASH_PAGE_TYPE) {
                        // Skip over pages that do not have hash values
                        continue;
                    }

                    for (short hashPageIndex : hashPageValueIndexes(pageData, hashPageHeader.getNumEntries(), hashMetaData.isSwapped())) {
                        // The first byte is the page type, so we can peek at it first before parsing further
                        byte valuePageType = pageData[hashPageIndex];

                        // Only Overflow pages contain package data, skip anything else
                        if (valuePageType != HASH_OFF_INDEX_PAGE_TYPE) {
                            continue;
                        }

                        // Traverse the page to concatenate the data that may span multiple pages
                        byte[] valueContent = HashPage.hashPageValueContent(
                                file, pageData, hashPageIndex, hashMetaData.getPageSize(), hashMetaData.isSwapped());

                        entries.put(new Entry(valueContent));
                    }

                    // Go back to the start of the next page for reading
                    file.seek(endOfPageOffset);
                }
            } catch (Exception e) {
                try {
                    entries.put(new Entry(e));
                } catch (Exception ex) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                // signal that processing is complete
                try {
                    entries.put(new Entry()); // sentinel value indicating completion
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
        return entries;
    }
}
