package inventory.dsl;

import inventory.Artifacts;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Artifactlist {

    String getDescription();

    List<Artifact> getArtifactlist();

    default Artifacts filter(Predicate<Artifact> predicate){
        return new Artifacts(
                getArtifactlist().
                        stream().
                        filter(predicate).
                        collect(Collectors.toList()),
                getDescription())
                ;
    }

    Artifacts setDescription(String description);

    default Artifacts as (String description){
        return (Artifacts)this.setDescription(description);
    }

}
