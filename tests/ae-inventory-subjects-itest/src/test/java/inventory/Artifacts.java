package inventory;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Artifacts {

    List<Artifact> artifacts;

    String description = "";
    public Artifacts(List<Artifact> artifacts, String description) {
        this.artifacts = artifacts;
        this.description = description;
    }

    public Artifacts hasNoErrors() {
        List<String> list = new ArrayList<>();
        artifacts.forEach(artifact -> {
                    if(artifact.get("Errors") != null){
                        list.add(artifact.getId()+ " has error: "+artifact.get("Errors"));
                    }
                }
        );
        assertThat(list).as(description+" should not have errors after parsing.").isEmpty();
        return this;
    }

    public Artifacts hasSizeGreaterThan(int minimum) {
        assertThat(artifacts).as("Minimum number of "+ description+" should be "+minimum).hasSizeGreaterThan(minimum);
        return this;
    }
}
