package inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public class TrivialPredicates implements NamedArtifactPredicate{
    
    public static NamedArtifactPredicate trivialReturnAllElements = new TrivialPredicates(true);

    public static NamedArtifactPredicate trivialReturnNoElements = new TrivialPredicates(false);
    
    private boolean value;

    public TrivialPredicates(boolean value) {
        this.value = value;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> value;
    }

    @Override
    public String getDescription() {
        return value ? "All Elements":"No Elements";
    }
}
