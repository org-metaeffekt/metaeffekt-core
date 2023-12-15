package inventory.dsl;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public interface NamedArtifactPredicate {

    Predicate<Artifact> getArtifactPredicate();

    String getDescription();
}
