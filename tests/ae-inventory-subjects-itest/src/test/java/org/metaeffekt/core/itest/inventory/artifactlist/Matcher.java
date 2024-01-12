package org.metaeffekt.core.itest.inventory.artifactlist;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.itest.inventory.ArtifactList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class Matcher {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private Cardinality cardinality;
    private List<String> attributeList;

    private ArtifactList listOfMatching = new ArtifactList();

    private ArtifactList listOfMissing = new ArtifactList();

    private String primaryAttribute;

    public Matcher setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public Matcher setAttributes(String... attributeList) {
        this.attributeList = Arrays.asList(attributeList);
        return this;
    }

    public ArtifactList getListOfMatching() {
        return listOfMatching;
    }

    public ArtifactList getListOfMissing() {
        return listOfMissing;
    }

    public Matcher setPrimaryAttribute(String primaryAttribute) {
        this.primaryAttribute = primaryAttribute;
        return this;
    }

    public Matcher setPrimaryAttribute(Artifact.Attribute attribute) {
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
        for (Artifact template : templatelist.getArtifactList()) {
            Artifact toBeMatched = testobjectmap.get(template.get(primaryAttribute));
            if (!cardinality.equals(Cardinality.SUPERSET)) {
                assertThat(toBeMatched).as("Artifact not found during matching: " + template).isNotNull();
            }
            if (toBeMatched != null && matchAttributes(template, toBeMatched)) {
                listOfMatching.getArtifactList().add(template);
            } else {
                listOfMissing.getArtifactList().add(template);
            }
        }
        if(cardinality.equals(Cardinality.EQUAL)){
                assertThat(testobjectmap.size())
                        .as("Templatelist should be equal to Testlist")
                        .isEqualTo(templatelist.getArtifactList().size());
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
        for (Artifact artifact : first.getArtifactList()) {
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
