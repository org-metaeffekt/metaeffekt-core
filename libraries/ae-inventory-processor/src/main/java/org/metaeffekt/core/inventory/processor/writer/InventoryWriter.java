/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.writer;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.excel.XlsInventoryWriter;
import org.metaeffekt.core.inventory.processor.writer.excel.XlsxInventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class InventoryWriter extends AbstractInventoryWriter {

    private final static Logger LOG = LoggerFactory.getLogger(InventoryWriter.class);

    public static final String VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX = "Assessment-";

    public static final String SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET = "Vulnerabilities";

    private final static Map<String, Supplier<AbstractInventoryWriter>> EXTENSIONS_TO_WRITERS = new LinkedHashMap<String, Supplier<AbstractInventoryWriter>>() {{
        put(".xls", XlsInventoryWriter::new);
        put(".xlsx", XlsxInventoryWriter::new);
        put(".ser", SerializedInventoryWriter::new);
    }};

    @Override
    public void writeInventory(Inventory inventory, File file) throws IOException {
        if (file == null) {
            throw new FileNotFoundException("File is null.");
        }

        final String extension = getFileExtension(file.getName());
        getWriterForExtension(extension).writeInventory(inventory, file);
    }

    private AbstractInventoryWriter getWriterForExtension(String extension) throws IOException {
        final Supplier<AbstractInventoryWriter> writerSupplier = EXTENSIONS_TO_WRITERS.get(extension);
        if (writerSupplier == null) {
            throw new IOException("Unsupported file type [" + extension + "]. Available types are: " + EXTENSIONS_TO_WRITERS.keySet());
        }
        return writerSupplier.get();
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.toLowerCase().lastIndexOf(".");
        if (lastIndexOf == -1) return "";
        return filename.substring(lastIndexOf);
    }
}
