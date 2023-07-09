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
package org.metaeffekt.core.inventory.processor.reader;

import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.writer.XlsInventoryWriter;
import org.metaeffekt.core.inventory.processor.writer.XlsxInventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InventoryReader {

    public Inventory readInventory(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File [" + file.getAbsolutePath()  + "] does not exist.");
        }
        if (file.getName().toLowerCase().endsWith(".xls")) {
            return new XlsInventoryReader().readInventory(file);
        } else {
            return new XlsxInventoryReader().readInventory(file);
        }
    }

    public Inventory readInventoryAsClasspathResource(File file) throws IOException {
        final Resource inventoryResource = new ClassPathResource(file.getPath());
        try (InputStream in = inventoryResource.getInputStream()) {
            if (file.getName().toLowerCase().endsWith(".xls")) {
                return new XlsInventoryReader().readInventory(in);
            } else {
                return new XlsxInventoryReader().readInventory(in);
            }
        }
    }

}
