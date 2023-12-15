package inventory.dsl;

import inventory.Artifacts;
import inventory.dsl.Artifactlist;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListSize extends Artifactlist {

    default Artifacts hasSizeGreaterThan(int boundary) {
        assertThat(getArtifactlist()).as(getDescription() +" size should be greater than "+boundary).hasSizeGreaterThan(boundary);
        return (Artifacts)this;
    }

    default Artifacts mustBeEmpty() {
        assertThat(getArtifactlist()).as(getDescription() +" must be Emtpy").isEmpty();
        return (Artifacts)this;
    }
}
