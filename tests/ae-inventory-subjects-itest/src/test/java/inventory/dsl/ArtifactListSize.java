package inventory.dsl;

import inventory.Artifacts;
import inventory.dsl.Artifactlist;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListSize extends Artifactlist {

    default Artifacts hasSizeGreaterThan(int boundary) {
        assertThat(getArtifactlist()).as("Minimum number of "+ getDescription() +" should be "+boundary).hasSizeGreaterThan(boundary);
        return (Artifacts)this;
    }


}
