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

package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.itest.common.fluent.base.*;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;

import java.util.List;
import java.util.stream.Collectors;

public class AssetList extends BaseList<AssetMetaData>
        implements BaseListAsserts<AssetMetaData>, BaseListLogger<AssetMetaData, AssetList>,
        BaseListFilter<AssetMetaData, AssetList>,
        BaseListSize<AssetMetaData, AssetList> {

    public AssetList(List<AssetMetaData> assetList, String description) {
        super(assetList, description);
    }

    public AssetList() {
        super();
    }

    @Override
    public AssetList filter(NamedBasePredicate<AssetMetaData> namedPredicate) {
        List<AssetMetaData> filteredItems = this.getItemList().stream()
                .filter(namedPredicate.getPredicate())
                .collect(Collectors.toList());

        this.description = this.getDescription() + ", filtered by: " + namedPredicate.getDescription();

        return this.createNewInstance(filteredItems);
    }


    @Override
    public AssetList createNewInstance(List<AssetMetaData> filteredList) {
        return new AssetList(filteredList, this.description);
    }

    @Override
    public AssetList logListWithAllAttributes() {
        LOG.info("LIST " + getDescription());
        getItemList().forEach(asset -> {
                    String[] attributes = asset.getAttributes().toArray(new String[0]);
                    LOG.info(withAttributes(asset, attributes));
                }
        );
        return this;
    }

    static String withAttributes(AssetMetaData asset, String[] additionalAttributes) {
        StringBuilder sb = new StringBuilder(asset.toString());
        for (String additionalAttribute : additionalAttributes) {
            sb.append(", ")
                    .append(additionalAttribute)
                    .append(": ")
                    .append(asset.get(additionalAttribute));
        }
        return sb.toString();
    }
}

