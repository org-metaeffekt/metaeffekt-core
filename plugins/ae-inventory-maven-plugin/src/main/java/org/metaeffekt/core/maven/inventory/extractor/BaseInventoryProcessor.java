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
package org.metaeffekt.core.maven.inventory.extractor;

import java.io.File;

public class BaseInventoryProcessor {

    private File inventoryFile;

    private File targetInventoryFile;

    public BaseInventoryProcessor augmenting(File inventoryFile) {
        this.inventoryFile = inventoryFile;
        return this;
    }

    public File getInventoryFile() {
        return inventoryFile;
    }

    public BaseInventoryProcessor writing(File targetInventoryFile) {
        this.targetInventoryFile = targetInventoryFile;
        return this;
    }

    public File getTargetInventoryFile() {
        return targetInventoryFile;
    }

}
