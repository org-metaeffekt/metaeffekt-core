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
package org.metaeffekt.core.itest.common.matchers;

import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.AssetList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetListMatcher {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private Cardinality cardinality;

    private List<String> attributeList;

    private AssetList listOfMatching = new AssetList();

    private AssetList listOfMissing = new AssetList();

    private String primaryAttribute;

    public AssetListMatcher setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public AssetListMatcher setAttributes(String... attributeList) {
        this.attributeList = Arrays.asList(attributeList);
        return this;
    }

    public AssetList getListOfMatching() {
        return listOfMatching;
    }

    public AssetList getListOfMissing() {
        return listOfMissing;
    }

    public AssetListMatcher setPrimaryAttribute(String primaryAttribute) {
        this.primaryAttribute = primaryAttribute;
        return this;
    }

    public AssetListMatcher setPrimaryAttribute(AssetMetaData.Attribute attribute) {
        return this.setPrimaryAttribute(attribute.getKey());
    }

    public void match(AssetList template, AssetList testobject) {
        Map<String, AssetMetaData> testobjectmap = populateMap(testobject);
        matchAssets(template, testobjectmap);
    }

    public void match(Analysis template, Analysis testobject) {
        match(template.selectAssets(), testobject.selectAssets());
    }

    private void matchAssets(AssetList templatelist, Map<String, AssetMetaData> testobjectmap) {
        listOfMatching = new AssetList().as("matching "+ templatelist.getDescription());
        listOfMissing = new AssetList().as("missing "+ templatelist.getDescription());
        for (AssetMetaData template : templatelist.getItemList()) {
            AssetMetaData toBeMatched = testobjectmap.get(template.get(primaryAttribute));
            if (!cardinality.equals(Cardinality.SUPERSET)) {
                assertThat(toBeMatched).as("Asset not found during matching: " + template).isNotNull();
            }
            if (toBeMatched != null && matchAttributes(template, toBeMatched)) {
                listOfMatching.getItemList().add(template);
            } else {
                listOfMissing.getItemList().add(template);
            }
        }
        if(cardinality.equals(Cardinality.EQUAL)){
                assertThat(testobjectmap.size())
                        .as("Templatelist should be equal to Testlist")
                        .isEqualTo(templatelist.getItemList().size());
        }
    }

    private boolean matchAttributes(AssetMetaData template, AssetMetaData toBeMatched) {
        for(String attribute : attributeList){
            assertThat(toBeMatched.get(attribute))
                    .as(attribute + " missmatch in "+toBeMatched)
                    .isEqualTo(template.get(attribute));
        }
        return true;
    }

    private Map<String, AssetMetaData> populateMap(AssetList first) {
        Map<String, AssetMetaData> assetmap = new HashMap<>();
        for (AssetMetaData asset : first.getItemList()) {
            assertThat(asset.get(primaryAttribute)).as(primaryAttribute + " of is null for " + asset).isNotNull();
            AssetMetaData val = assetmap.put(asset.get(primaryAttribute), asset);
            assertThat(val).as("Collision during matching: " + val + " has the same " + primaryAttribute + " as " + asset).isNull();
        }
        return assetmap;
    }

    public enum Cardinality {
        SUBSET, EQUAL, SUPERSET
    }
}
