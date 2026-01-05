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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

public class HashMetadata extends GenericMetadataPage {
    private long maxBucket; // 72-75: ID of maximum bucket in use.
    private long highMask; // 76-79: modulo mask into table
    private long lowMask; // 80-83: module mask into table lower half
    private long fillFactor; // 85-87: fill factor
    private long numKeys; // 88-91: number of keys in hash table
    private long charKeyHash; // 92-95: value of hash(CHARKEY)

    // getters and setters for each field

    public long getMaxBucket() {
        return maxBucket;
    }

    public void setMaxBucket(long maxBucket) {
        this.maxBucket = maxBucket;
    }

    public long getHighMask() {
        return highMask;
    }

    public void setHighMask(long highMask) {
        this.highMask = highMask;
    }

    public long getLowMask() {
        return lowMask;
    }

    public void setLowMask(long lowMask) {
        this.lowMask = lowMask;
    }

    public long getFillFactor() {
        return fillFactor;
    }

    public void setFillFactor(long fillFactor) {
        this.fillFactor = fillFactor;
    }

    public long getNumKeys() {
        return numKeys;
    }

    public void setNumKeys(long numKeys) {
        this.numKeys = numKeys;
    }

    public long getCharKeyHash() {
        return charKeyHash;
    }

    public void setCharKeyHash(long charKeyHash) {
        this.charKeyHash = charKeyHash;
    }
}
