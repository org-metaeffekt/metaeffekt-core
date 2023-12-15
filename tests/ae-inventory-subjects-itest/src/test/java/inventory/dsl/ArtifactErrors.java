package inventory.dsl;

import inventory.Artifacts;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactErrors extends Artifactlist{

    default  Artifacts hasNoErrors() {
        List<String> list = new ArrayList<>();
        getArtifactlist().forEach(artifact -> {
                    if(artifact.get("Errors") != null){
                        list.add(artifact.getId()+ " has error: "+artifact.get("Errors"));
                    }
                }
        );
        assertThat(list).as(getDescription() + " should not have errors after parsing.").isEmpty();
        return (Artifacts)this;
    }
}
