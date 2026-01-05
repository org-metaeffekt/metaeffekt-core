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
package org.metaeffekt.core.itest.common.matchers;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ComponentPatternList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentPatternListMatcher {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private Cardinality cardinality;

    private List<String> attributeList;

    private ComponentPatternList listOfMatching = new ComponentPatternList();

    private ComponentPatternList listOfMissing = new ComponentPatternList();

    private String primaryAttribute;

    public ComponentPatternListMatcher setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public ComponentPatternListMatcher setAttributes(String... attributeList) {
        this.attributeList = Arrays.asList(attributeList);
        return this;
    }

    public ComponentPatternList getListOfMatching() {
        return listOfMatching;
    }

    public ComponentPatternList getListOfMissing() {
        return listOfMissing;
    }

    public ComponentPatternListMatcher setPrimaryAttribute(String primaryAttribute) {
        this.primaryAttribute = primaryAttribute;
        return this;
    }

    public ComponentPatternListMatcher setPrimaryAttribute(ComponentPatternData.Attribute attribute) {
        return this.setPrimaryAttribute(attribute.getKey());
    }

    public void match(ComponentPatternList template, ComponentPatternList testobject) {
        Map<String, ComponentPatternData> testobjectmap = populateMap(testobject);
        matchComponentPatterns(template, testobjectmap);
    }

    public void match(Analysis template, Analysis testobject) {
        match(template.selectComponentPatterns(), testobject.selectComponentPatterns());
    }

    private void matchComponentPatterns(ComponentPatternList templatelist, Map<String, ComponentPatternData> testobjectmap) {
        listOfMatching = new ComponentPatternList().as("matching "+ templatelist.getDescription());
        listOfMissing = new ComponentPatternList().as("missing "+ templatelist.getDescription());
        for (ComponentPatternData template : templatelist.getItemList()) {
            ComponentPatternData toBeMatched = testobjectmap.get(template.get(primaryAttribute));
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

    private boolean matchAttributes(ComponentPatternData template, ComponentPatternData toBeMatched) {
        for(String attribute : attributeList){
            assertThat(toBeMatched.get(attribute))
                    .as(attribute + " missmatch in "+toBeMatched)
                    .isEqualTo(template.get(attribute));
        }
        return true;
    }

    private Map<String, ComponentPatternData> populateMap(ComponentPatternList first) {
        Map<String, ComponentPatternData> assetmap = new HashMap<>();
        for (ComponentPatternData component : first.getItemList()) {
            assertThat(component.get(primaryAttribute)).as(primaryAttribute + " of is null for " + component).isNotNull();
            ComponentPatternData val = assetmap.put(component.get(primaryAttribute), component);
            assertThat(val).as("Collision during matching: " + val + " has the same " + primaryAttribute + " as " + component).isNull();
        }
        return assetmap;
    }

    public enum Cardinality {
        SUBSET, EQUAL, SUPERSET
    }
}
