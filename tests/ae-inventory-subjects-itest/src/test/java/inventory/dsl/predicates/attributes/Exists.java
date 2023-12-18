package inventory.dsl.predicates.attributes;

import inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public class Exists implements NamedArtifactPredicate {

    private final String attribute;

    public Exists(String attribute) {
        this.attribute = attribute;
    }

    public static NamedArtifactPredicate exists(Artifact.Attribute attribute){
        return new Exists(attribute.getKey());
    }

    public static NamedArtifactPredicate exists(String attribute){
        return new Exists(attribute);
    }
    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> artifact.get(attribute) != null;
    }


    @Override
    public String getDescription() {
        return "'"+attribute+"' exists";
    }
}
