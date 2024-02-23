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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactListMatcher {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private Cardinality cardinality;

    private List<String> attributeList;

    private ArtifactList listOfMatching = new ArtifactList();

    private ArtifactList listOfMissing = new ArtifactList();

    private String primaryAttribute;

    public ArtifactListMatcher setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public ArtifactListMatcher setAttributes(String... attributeList) {
        this.attributeList = Arrays.asList(attributeList);
        return this;
    }

    public ArtifactList getListOfMatching() {
        return listOfMatching;
    }

    public ArtifactList getListOfMissing() {
        return listOfMissing;
    }

    public ArtifactListMatcher setPrimaryAttribute(String primaryAttribute) {
        this.primaryAttribute = primaryAttribute;
        return this;
    }

    public ArtifactListMatcher setPrimaryAttribute(Artifact.Attribute attribute) {
        return this.setPrimaryAttribute(attribute.getKey());
    }

    public void match(ArtifactList template, ArtifactList testobject) {
        Map<String, Artifact> testobjectmap = populateMap(testobject);
        matchArtifacts(template, testobjectmap);
    }

    public void match(Analysis template, Analysis testobject) {
        match(template.selectArtifacts(), testobject.selectArtifacts());
    }

    private void matchArtifacts(ArtifactList templatelist, Map<String, Artifact> testobjectmap) {
        listOfMatching = new ArtifactList().as("matching "+ templatelist.getDescription());
        listOfMissing = new ArtifactList().as("missing "+ templatelist.getDescription());
        for (Artifact template : templatelist.getItemList()) {
            Artifact toBeMatched = testobjectmap.get(template.get(primaryAttribute));
            if (!cardinality.equals(Cardinality.SUPERSET)) {
                assertThat(toBeMatched).as("Artifact not found during matching: " + template).isNotNull();
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

    private boolean matchAttributes(Artifact template, Artifact toBeMatched) {
        for(String attribute : attributeList){
            assertThat(toBeMatched.get(attribute))
                    .as(attribute + " missmatch in "+toBeMatched)
                    .isEqualTo(template.get(attribute));
        }
        return true;
    }

    private Map<String, Artifact> populateMap(ArtifactList first) {
        Map<String, Artifact> artifactmap = new HashMap<>();
        for (Artifact artifact : first.getItemList()) {
            assertThat(artifact.get(primaryAttribute)).as(primaryAttribute + " of is null for " + artifact).isNotNull();
            Artifact val = artifactmap.put(artifact.get(primaryAttribute), artifact);
            assertThat(val).as("Collision during matching: " + val + " has the same " + primaryAttribute + " as " + artifact).isNull();
        }
        return artifactmap;
    }

    public enum Cardinality {
        SUBSET, EQUAL, SUPERSET
    }
}
