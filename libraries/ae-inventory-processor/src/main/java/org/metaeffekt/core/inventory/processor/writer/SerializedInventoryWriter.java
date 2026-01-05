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
package org.metaeffekt.core.inventory.processor.writer;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

public class SerializedInventoryWriter extends AbstractInventoryWriter {

    @Override
    public void writeInventory(Inventory inventory, File file) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(file);
             final GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut);
             final ObjectOutputStream out = new ObjectOutputStream(gzipOut)) {
            out.writeObject(inventory);
        }
    }
}
