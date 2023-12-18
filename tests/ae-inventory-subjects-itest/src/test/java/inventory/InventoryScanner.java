package inventory;

import inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.List;

public class InventoryScanner {
    private final Inventory inventory;

    public InventoryScanner(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public Artifacts select() {
        return new Artifacts(inventory.getArtifacts(), "All artifacts");
    }

    public Artifacts select(NamedArtifactPredicate artifactPredicate) {
        return select()
                .filter(artifactPredicate.getArtifactPredicate())
                .as(artifactPredicate.getDescription());
    }
}
