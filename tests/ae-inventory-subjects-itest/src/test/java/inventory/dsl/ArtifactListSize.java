package inventory.dsl;

import inventory.Artifacts;
import inventory.dsl.Artifactlist;
import inventory.dsl.predicates.NamedArtifactPredicate;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListSize extends Artifactlist {

    default Artifacts hasSizeGreaterThan(int boundary) {
        assertThat(getArtifactlist()).as("Size of List where ["+getDescription() +"] should be greater than "+boundary).hasSizeGreaterThan(boundary);
        return (Artifacts)this;
    }

    default void mustBeEmpty() {
        assertThat(getArtifactlist()).as("List where ["+ getDescription() +"] must be Emtpy").isEmpty();
    }


}
