package inventory;

import inventory.dsl.ArtifactListSize;
import inventory.dsl.ArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.List;

public class InventoryScanner {
    private final Inventory inventory;

    public InventoryScanner(Inventory inventory) {
        this.inventory = inventory;
    }


    public List<String> getErrorlist() {
        List<String> errorlist = new ArrayList<>();
        inventory.getArtifacts().forEach(artifact -> {
                    if(artifact.get("Errors") != null){
                        errorlist.add(artifact.getId()+ " has error: "+artifact.get("Errors"));
                    }
                }
        );
        return errorlist;
    }

    public List<String> getMissingTypelist() {
        List<String> typelist = new ArrayList<>();
        inventory.getArtifacts().forEach(artifact -> {
                    if(artifact.get("Type") == null){
                        typelist.add(artifact.getId()+ " has no type assigned!");
                    }
                }
        );
        return typelist;
    }


    public List<Artifact> getArtifacts() {
        return inventory.getArtifacts();
    }

    public Artifacts selectAllArtifacts() {
        return new Artifacts(inventory.getArtifacts(), "All artifacts");
    }

    public ArtifactListSize select(ArtifactPredicate artifactPredicate) {
        return selectAllArtifacts()
                .filter(artifactPredicate.getArtifactPredicate())
                .as(artifactPredicate.getDescription());
    }
}
