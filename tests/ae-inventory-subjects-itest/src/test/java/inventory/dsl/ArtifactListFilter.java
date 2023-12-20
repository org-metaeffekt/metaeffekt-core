package inventory.dsl;

import inventory.ArtifactList;
import inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ArtifactListFilter extends ArtifactListDescriptor{


    default ArtifactList filter(Predicate<Artifact> predicate){
        return new ArtifactList(
                getArtifactList().
                        stream().
                        filter(predicate).
                        collect(Collectors.toList()),
                getDescription());
    }

    default ArtifactList filter(NamedArtifactPredicate namedPredicate){
        return new ArtifactList(
                getArtifactList().
                        stream().
                        filter(namedPredicate.getArtifactPredicate()).
                        collect(Collectors.toList()),
                namedPredicate.getDescription());
    }

}
