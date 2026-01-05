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

public class GenericMetadataPage {
    private byte[] LSN = new byte[8];         // 00-07: LSN.
    private long pageNo;                       // 08-11: Current page number.
    private long magic;                        // 12-15: Magic number.
    private long version;                      // 16-19: Version.
    private long pageSize;                     // 20-23: Pagesize.
    private byte encryptionAlg;               // 24: Encryption algorithm.
    private byte pageType;                    // 25: Page type.
    private byte metaFlags;                   // 26: Meta-only flags
    private byte unused1;                     // 27: Unused.
    private long free;                         // 28-31: Free list page number.
    private long lastPageNo;                   // 32-35: Page number of last page in db.
    private long nParts;                       // 36-39: Number of partitions.
    private long keyCount;                     // 40-43: Cached key count.
    private long recordCount;                  // 44-47: Cached record count.
    private long flags;                        // 48-51: Flags: unique to each AM.
    private byte[] uniqueFileID = new byte[19]; // 52-71: Unique file ID.

    private static final byte NoEncryptionAlgorithm = 0; // Define this as needed

    public static final int HASH_MAGIC_NUMBER = 0x00061561;
    public static final int HASH_MAGIC_NUMBER_BE = 0x61150600;
    public static final byte HASH_METADATA_PAGE_TYPE = 8;

    public GenericMetadataPage() {
    }

    public void parseGenericMetadataPage(byte[] data, ByteOrder order) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(order);

        buffer.get(this.LSN);
        this.pageNo = Integer.toUnsignedLong(buffer.getInt());
        this.magic = Integer.toUnsignedLong(buffer.getInt());
        this.version = Integer.toUnsignedLong(buffer.getInt());
        this.pageSize = Integer.toUnsignedLong(buffer.getInt());
        this.encryptionAlg = buffer.get();
        this.pageType = buffer.get();
        this.metaFlags = buffer.get();
        this.unused1 = buffer.get();
        this.free = Integer.toUnsignedLong(buffer.getInt());
        this.lastPageNo = Integer.toUnsignedLong(buffer.getInt());
        this.nParts = Integer.toUnsignedLong(buffer.getInt());
        this.keyCount = Integer.toUnsignedLong(buffer.getInt());
        this.recordCount = Integer.toUnsignedLong(buffer.getInt());
        this.flags = Integer.toUnsignedLong(buffer.getInt());
        buffer.get(this.uniqueFileID);

        this.validate();
    }

    public void validate() throws Exception {
        if (this.encryptionAlg != NoEncryptionAlgorithm) {
            throw new Exception("unexpected encryption algorithm: " + this.encryptionAlg);
        }
    }

    public long getMagic() {
        return magic;
    }

    public byte getPageType() {
        return pageType;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getLastPageNo() {
        return lastPageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public void setMagic(long magic) {
        this.magic = magic;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setLSN(byte[] LSN) {
        this.LSN = LSN;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public void setEncryptionAlg(byte encryptionAlg) {
        this.encryptionAlg = encryptionAlg;
    }

    public void setPageType(byte pageType) {
        this.pageType = pageType;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    public void setFree(long free) {
        this.free = free;
    }

    public void setKeyCount(long keyCount) {
        this.keyCount = keyCount;
    }

    public void setLastPageNo(long lastPageNo) {
        this.lastPageNo = lastPageNo;
    }

    public void setMetaFlags(byte metaFlags) {
        this.metaFlags = metaFlags;
    }

    public void setnParts(long nParts) {
        this.nParts = nParts;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }

    public void setUniqueFileID(byte[] uniqueFileID) {
        this.uniqueFileID = uniqueFileID;
    }

    public void setUnused1(byte unused1) {
        this.unused1 = unused1;
    }

    public byte[] getLSN() {
        return LSN;
    }
}

