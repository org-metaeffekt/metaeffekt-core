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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.util.*;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM;

public class FileSystemScanParam {

    private String[] collectIncludes = new String[] { "**/*" };

    private String[] collectExcludes = new String[0];

    private String[] unwrapIncludes = new String[] { "**/*" };

    private String[] unwrapExcludes = new String[0];

    private boolean implicitUnwrap = true;

    private boolean includeEmbedded = true;

    private boolean detectComponentPatterns = true;

    private Inventory referenceInventory;

    private transient Map<String, Set<ComponentPatternData>> checksumComponentPatternDataMap;

    public FileSystemScanParam() {
    }

    public FileSystemScanParam collectAllMatching(String[] includes, String[] excludes) {
        this.collectIncludes = FileUtils.normalizePatterns(includes);
        this.collectExcludes = FileUtils.normalizePatterns(excludes);
        return this;
    }

    public FileSystemScanParam unwrapAllMatching(String[] includes, String[] excludes) {
        this.unwrapIncludes = FileUtils.normalizePatterns(includes);
        this.unwrapExcludes = FileUtils.normalizePatterns(excludes);
        return this;
    }

    public boolean collects(String path) {
        return matches(path, collectIncludes, collectExcludes);
    }

    public boolean unwraps(String path) {
        return matches(path, unwrapIncludes, unwrapExcludes);
    }

    public FileSystemScanParam includeEmbedded(boolean includeEmbedded) {
        this.includeEmbedded = includeEmbedded;
        return this;
    }

    public boolean isIncludeEmbedded() {
        return includeEmbedded;
    }

    public FileSystemScanParam withReferenceInventory(Inventory referenceInventory) {
        this.referenceInventory = referenceInventory;

        // cache component patterns
        cacheComponentPatterns(referenceInventory);

        return this;
    }

    private void cacheComponentPatterns(Inventory inventory) {
        synchronized (inventory) {
            this.checksumComponentPatternDataMap = new HashMap<>();
            final List<ComponentPatternData> componentPatternDataList = inventory.getComponentPatternData();
            if (componentPatternDataList != null) {
                for (ComponentPatternData componentPatternData : componentPatternDataList) {
                    // the checksum may be a md5 or '*'
                    final String checksumOrWildcard = componentPatternData.get(VERSION_ANCHOR_CHECKSUM);
                    Set<ComponentPatternData> cpdSet = checksumComponentPatternDataMap.computeIfAbsent(checksumOrWildcard, m -> new HashSet<>());
                    cpdSet.add(componentPatternData);
                }
            }
        }
    }

    private boolean matches(String path, String[] includePatterns, String[] excludePatterns) {
        if (excludePatterns != null) {
            for (final String exclude : excludePatterns) {
                if (FileUtils.matches(exclude, path)) {
                    return false;
                }
            }
        }
        if (includePatterns != null) {
            for (final String include : includePatterns) {
                if (FileUtils.matches(include, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isImplicitUnwrap() {
        return implicitUnwrap;
    }

    public FileSystemScanParam implicitUnwrap(boolean implicitUnwrap) {
        this.implicitUnwrap = implicitUnwrap;
        return this;
    }

    public Inventory getReferenceInventory() {
        return referenceInventory;
    }

    public Set<ComponentPatternData> getComponentPatternsByChecksum(String md5Checksum) {
        return checksumComponentPatternDataMap.getOrDefault(md5Checksum, Collections.emptySet());
    }

    public FileSystemScanParam detectComponentPatterns(boolean detectComponentPatterns) {
        this.detectComponentPatterns = detectComponentPatterns;
        return this;
    }

    public boolean isDetectComponentPatterns() {
        return detectComponentPatterns;
    }
}
