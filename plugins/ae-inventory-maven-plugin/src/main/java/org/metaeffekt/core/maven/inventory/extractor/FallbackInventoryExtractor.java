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
package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FallbackInventoryExtractor extends AlpineInventoryExtractor {

    @Override
    public boolean applies(File analysisDir) {
        return true;
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
        // here the common aspects are validated
        validateFileExists(new File(analysisDir, "filesystem/files.txt"));
    }

    @Override
    public Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException {
        return super.extractInventory(analysisDir, inventoryId, excludePatterns);
    }

    @Override
    public void extendInventory(File analysisDir, Inventory inventory) throws IOException {
        // ignore
    }

}
