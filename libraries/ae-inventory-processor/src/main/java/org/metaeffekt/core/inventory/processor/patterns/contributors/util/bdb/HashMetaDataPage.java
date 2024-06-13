package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;/*
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HashMetaDataPage extends HashMetadata {
    private boolean swapped;

    public static HashMetaDataPage parseHashMetadataPage(byte[] data) throws Exception {
        HashMetaDataPage pageMetadata = new HashMetaDataPage();

        pageMetadata.setSwapped(false);

        try {
            readHashMetadata(data, pageMetadata);
        } catch (IOException e) {
            throw new IOException("Failed to unpack HashMetadataPage", e);
        }

        pageMetadata.setSwapped(false);

        if (pageMetadata.getMagic() == GenericMetadataPage.HASH_MAGIC_NUMBER_BE) {
            pageMetadata.setSwapped(true);
            pageMetadata.parseGenericMetadataPage(data, ByteOrder.BIG_ENDIAN);
        }

        pageMetadata.validate();
        return pageMetadata;
    }

    private static void readHashMetadata(byte[] data, HashMetaDataPage page) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        page.parseGenericMetadataPage(data, ByteOrder.LITTLE_ENDIAN);
        page.setMaxBucket(Integer.toUnsignedLong(buffer.getInt(72)));
        page.setHighMask(Integer.toUnsignedLong(buffer.getInt(76)));
        page.setLowMask(Integer.toUnsignedLong(buffer.getInt(80)));
        page.setFillFactor(Integer.toUnsignedLong(buffer.getInt(84)));
        page.setNumKeys(Integer.toUnsignedLong(buffer.getInt(88)));
        page.setCharKeyHash(Integer.toUnsignedLong(buffer.getInt(92)));
    }

    public void validate() throws Exception {
        super.validate();
        if (super.getMagic() != GenericMetadataPage.HASH_MAGIC_NUMBER) {
            throw new Exception("unexpected DB magic number: " + getMagic());
        }

        if (super.getPageType() != GenericMetadataPage.HASH_METADATA_PAGE_TYPE) {
            throw new Exception("unexpected page type: " + getPageType());
        }
    }

    public boolean isSwapped() {
        return swapped;
    }

    public void setSwapped(boolean swapped) {
        this.swapped = swapped;
    }
}

