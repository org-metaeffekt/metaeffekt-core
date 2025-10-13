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
package org.metaeffekt.core.inventory.processor.reader;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class InventoryReader extends AbstractInventoryReader {

    private final static Map<String, Supplier<AbstractInventoryReader>> EXTENSIONS_TO_READERS = new LinkedHashMap<String, Supplier<AbstractInventoryReader>>() {{
        put(".xls", XlsInventoryReader::new);
        put(".xlsx", XlsxInventoryReader::new);
        put(".ser", SerializedInventoryReader::new);
    }};

    @Override
    public Inventory readInventory(File file) throws IOException {
        if (file == null) {
            throw new FileNotFoundException("File is null.");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File [" + file.getAbsolutePath() + "] does not exist.");
        }

        final String extension = getFileExtension(file.getName());
        return getReaderForExtension(extension).readInventory(file);
    }

    public Inventory readInventoryAsClasspathResource(File file) throws IOException {
        if (file == null) {
            throw new FileNotFoundException("File is null.");
        }

        final Resource inventoryResource = new ClassPathResource(file.getPath());
        try (InputStream in = inventoryResource.getInputStream()) {
            final String extension = getFileExtension(file.getName());
            return getReaderForExtension(extension).readInventory(in);
        }
    }

    @Override
    public Inventory readInventory(InputStream in) throws IOException {
        throw new UnsupportedOperationException("Reading from input stream is not supported. Use readInventory(InputStream in, String extension) or the specific implementations instead.");
    }

    public Inventory readInventory(InputStream in, String extension) throws IOException {
        return getReaderForExtension(extension).readInventory(in);
    }

    private AbstractInventoryReader getReaderForExtension(String extension) throws IOException {
        final Supplier<AbstractInventoryReader> readerSupplier = EXTENSIONS_TO_READERS.get(extension);
        if (readerSupplier == null) {
            throw new IOException("Unsupported file type [" + extension + "]. Available types are: " + EXTENSIONS_TO_READERS.keySet());
        }
        return readerSupplier.get();
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.toLowerCase().lastIndexOf(".");
        if (lastIndexOf == -1) return "";
        return filename.substring(lastIndexOf);
    }
}
