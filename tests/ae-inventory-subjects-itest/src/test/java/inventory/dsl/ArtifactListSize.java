package inventory.dsl;

import inventory.ArtifactList;
import inventory.dsl.predicates.NamedArtifactPredicate;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListSize extends ArtifactListFilter {

    default ArtifactList hasSizeGreaterThan(int boundary) {
        assertThat(getArtifactList()).as("Size of List where [" + getDescription() + "] should be greater than " + boundary).hasSizeGreaterThan(boundary);
        return (ArtifactList) this;
    }

    default void assertEmpty() {
        assertThat(getArtifactList()).as("List where [" + getDescription() + "] must be Emtpy").isEmpty();
    }

    default ArtifactList assertEmpty(NamedArtifactPredicate artifactPredicate) {
        filter(artifactPredicate).assertEmpty();
        return (ArtifactList) this;
    }


    default ArtifactList assertNotEmpty() {
        return hasSizeGreaterThan(0);
    }


    default ArtifactList assertNotEmpty(NamedArtifactPredicate artifactPredicate) {
        filter(artifactPredicate).hasSizeGreaterThan(0);
        return (ArtifactList) this;
    }


}
