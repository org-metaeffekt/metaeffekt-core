package inventory.dsl;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public enum ArtifactPredicate implements NamedArtifactPredicate{

    MissingType ("Artifactlist with missing type",
            artifact -> artifact.get("Type") == null),

    ErrorExist ("Artificatlist with errors",
        artifact -> artifact.get("Errors") != null),

    IdMissmatchesVersion ("Artifactlist where ID and Version missmatches",
        artifact -> {
            System.out.println(artifact.get(Artifact.Attribute.VERSION));
            System.out.println(artifact.get(Artifact.Attribute.ID));
        return artifact.get(Artifact.Attribute.VERSION) == "";
        });


    private Predicate<Artifact> predicate;
    private String description;

    private ArtifactPredicate (String description, Predicate<Artifact> predicate){
        this.predicate = predicate;
        this.description = description;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return predicate;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
