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
package org.metaeffekt.core.inventory.processor.filescan.tasks;

import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;

import java.io.IOException;
import java.util.List;

/**
 * Abstract base class for scan tasks.
 */
public abstract class ScanTask {

    private final List<String> assetIdChain;

    protected ScanTask(List<String> assetIdChain) {
        this.assetIdChain = assetIdChain;
    }

    public List<String> getAssetIdChain() {
        return assetIdChain;
    }

    public abstract void process(FileSystemScanContext fileSystemScan) throws IOException;

}
