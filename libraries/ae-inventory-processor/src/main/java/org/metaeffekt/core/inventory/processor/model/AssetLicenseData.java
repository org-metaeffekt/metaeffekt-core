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
package org.metaeffekt.core.inventory.processor.model;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data container for assets and {@link LicenseMetaData}. Helps to collect license information around a given
 * asset.
 *
 * @author Karsten Klein
 */
public class AssetLicenseData {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    private final List<String> licenses;

    private final String assetId;
    private final String assetName;
    private final String assetVersion;

    private final String assetType;

    public AssetLicenseData(String assetId, String assetName, String assetVersion, String assetType, List<String> licenses) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.assetVersion = assetVersion;
        this.assetType = assetType;
        this.licenses = licenses;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getAssetVersion() {
        return assetVersion;
    }

    public String getAssetType() {
        return assetType;
    }

    public String deriveId() {
        return assetId;
    }

    @Override
    public String toString() {
        return "AssetLicenseData{" +
                "assetId='" + assetId + '\'' +
                ", assetName='" + assetName + '\'' +
                ", assetVersion='" + assetVersion + '\'' +
                '}';
    }

}
