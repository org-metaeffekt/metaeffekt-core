package org.metaeffekt.core.itest.inventory;

import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.List;

public class Analysis {
    private final Inventory inventory;

    public Analysis(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public ArtifactList selectArtifacts() {
        return new ArtifactList(inventory.getArtifacts(), "All artifacts");
    }

    public ArtifactList selectArtifacts(NamedArtifactPredicate artifactPredicate) {
        return selectArtifacts()
                .filter(artifactPredicate.getArtifactPredicate())
                .as(artifactPredicate.getDescription());
    }
}
