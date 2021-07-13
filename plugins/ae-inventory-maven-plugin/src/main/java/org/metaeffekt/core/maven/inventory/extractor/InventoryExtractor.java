/*
 * Copyright 2009-2021 the original author or authors.
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

/**
 * Interface common to all {@link InventoryExtractor}s.
 */
public interface InventoryExtractor {

    /**
     * Checks whether the extractor is applicable to the content in analysisDir.
     *
     * @param analysisDir The analysisDir.
     * @return
     */
    boolean applies(File analysisDir);

    /**
     * Validates that the content in analysisDir is as anticipated.
     *
     * @param analysisDir The analysisDir.
     * @return
     *
     * @Throws IllegalStateException
     */
    void validate(File analysisDir) throws IllegalStateException;

    /**
     * Extract an inventory from the information aggregated in analysisDir.
     *
     * @param analysisDir The analysisDir.
     * @param inventoryId The identifier or discriminator for the inventory.
     * @param excludePatterns List of exclude patterns (ant style).
     * @return The extracted inventory.
     * @throws IOException
     */
    Inventory extractInventory(File analysisDir, String inventoryId, List<String> excludePatterns) throws IOException;

}
