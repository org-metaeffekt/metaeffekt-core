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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.ndb;

import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Database;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Entry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.metaeffekt.core.inventory.processor.patterns.contributors.util.ndb.NDB.NDBBlobHeader.NDB_BLOB_HEADER_SIZE;

public class NDB implements Database {

    private static final int NDB_SlotEntriesPerPage = 4096 / 16; // 16 == size of NDBSlotEntry
    private static final int NDB_HeaderMagic = 'R' | 'p' << 8 | 'm' << 16 | 'P' << 24;
    private static final int NDB_DBVersion = 0;
    private static final int NDB_SlotMagic = 'S' | 'l' << 8 | 'o' << 16 | 't' << 24;
    private static final int NDB_BlobMagic = 'B' | 'l' << 8 | 'b' << 16 | 'S' << 24;

    private RandomAccessFile file;
    private NDBSlotEntry[] slots;

    public NDB(RandomAccessFile file, NDBSlotEntry[] slots) {
        this.file = file;
        this.slots = slots;
    }


    @Override
    public Database open(String path) {
        try {
            file = new RandomAccessFile(path, "r");
            FileChannel channel = file.getChannel();
            FileLock lock = channel.lock(0, Long.MAX_VALUE, true);
            // Read NDB header
            ByteBuffer headerBuffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(headerBuffer);
            headerBuffer.flip();

            NDBHeader header = new NDBHeader(headerBuffer);
            if (header.headerMagic != NDB_HeaderMagic || header.slotNPages == 0 || header.ndbVersion != NDB_DBVersion) {
                throw new IOException("Invalid or unsupported NDB format");
            }

            // Sanity check against excessive memory usage
            if (header.slotNPages > 2048) {
                throw new IOException("Slot page limit exceeded: " + header.slotNPages);
            }

            // Read slots
            slots = new NDBSlotEntry[(int) (header.slotNPages * NDB_SlotEntriesPerPage - 2)];
            ByteBuffer slotsBuffer = ByteBuffer.allocate(slots.length * 16).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(slotsBuffer);
            slotsBuffer.flip();

            for (int i = 0; i < slots.length; i++) {
                slots[i] = new NDBSlotEntry(slotsBuffer);
            }

            lock.release();
            return new NDB(file, slots);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws IOException {
        file.close();
    }

    @Override
    public BlockingQueue<Entry> read() {
        BlockingQueue<Entry> entries = new ArrayBlockingQueue<>(slots.length);

        new Thread(() -> {
            try {
                for (NDBSlotEntry slot : slots) {
                    if (slot.slotMagic != NDB_SlotMagic) {
                        entries.put(new Entry(null, new IOException("Bad slot magic: " + slot.slotMagic)));
                        return;
                    }

                    if (slot.pkgIndex == 0) {
                        continue; // Empty slot
                    }

                    // Seek to Blob
                    file.seek(slot.blkOffset * NDB_BLOB_HEADER_SIZE);

                    // Read Blob Header
                    ByteBuffer blobHeaderBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
                    file.getChannel().read(blobHeaderBuffer);
                    blobHeaderBuffer.flip();

                    NDBBlobHeader blobHeader = new NDBBlobHeader(blobHeaderBuffer);

                    if (blobHeader.blobMagic != NDB_BlobMagic) {
                        entries.put(new Entry(null, new IOException("Unexpected NDB blob magic for pkg " + slot.pkgIndex + ": " + blobHeader.blobMagic)));
                        return;
                    }

                    if (blobHeader.pkgIndex != slot.pkgIndex) {
                        entries.put(new Entry(null, new IOException("Failed to find NDB blob for pkg " + slot.pkgIndex)));
                        return;
                    }

                    // Read Blob Content
                    byte[] blobEntry = new byte[(int) blobHeader.blobLen];
                    file.readFully(blobEntry);

                    entries.put(new Entry(blobEntry, null));
                }
            } catch (IOException | InterruptedException e) {
                try {
                    entries.put(new Entry(null, e));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                try {
                    entries.put(new Entry(null, null)); // Indicate the end of processing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        return entries;
    }

    // necessary data structures
    static class NDBHeader {
        long headerMagic;
        long ndbVersion;
        long ndbGeneration;
        long slotNPages;

        NDBHeader(ByteBuffer buffer) {
            headerMagic = Integer.toUnsignedLong(buffer.getInt());
            ndbVersion = Integer.toUnsignedLong(buffer.getInt());
            ndbGeneration = Integer.toUnsignedLong(buffer.getInt());
            slotNPages = Integer.toUnsignedLong(buffer.getInt());
            buffer.getInt(); // Skip reserved space
            buffer.getInt();
            buffer.getInt();
            buffer.getInt();
        }
    }

    static class NDBSlotEntry {
        long slotMagic;
        long pkgIndex;
        long blkOffset;
        long blkCount;

        NDBSlotEntry(ByteBuffer buffer) {
            slotMagic = Integer.toUnsignedLong(buffer.getInt());
            pkgIndex = Integer.toUnsignedLong(buffer.getInt());
            blkOffset = Integer.toUnsignedLong(buffer.getInt());
            blkCount = Integer.toUnsignedLong(buffer.getInt());
        }
    }

    static class NDBBlobHeader {
        long blobMagic;
        long pkgIndex;
        long blobCkSum;
        long blobLen;

        public static final int NDB_BLOB_HEADER_SIZE = Integer.BYTES * 4;

        NDBBlobHeader(ByteBuffer buffer) {
            blobMagic = Integer.toUnsignedLong(buffer.getInt());
            pkgIndex = Integer.toUnsignedLong(buffer.getInt());
            blobCkSum = Integer.toUnsignedLong(buffer.getInt());
            blobLen = Integer.toUnsignedLong(buffer.getInt());
        }
    }
}

