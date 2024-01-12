package org.metaeffekt.core.itest.inventory;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;

import java.util.List;

public class Analysis {
    private final Inventory inventory;
    private String description;

    public Analysis(Inventory inventory) {
        this(inventory, "All artifacts");
    }

    public Analysis(Inventory inventory, String description) {
        this.inventory = inventory;
        this.description = description;
    }

    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public ArtifactList selectArtifacts() {
        return new ArtifactList(inventory.getArtifacts(), description);
    }

    public ArtifactList selectArtifacts(NamedArtifactPredicate artifactPredicate) {
        return selectArtifacts()
                .filter(artifactPredicate.getArtifactPredicate())
                .as(artifactPredicate.getDescription());
    }
}
