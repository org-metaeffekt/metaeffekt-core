package inventory;

import inventory.dsl.ArtifactErrors;
import inventory.dsl.ArtifactListSize;
import inventory.dsl.Artifactlist;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.List;

public class Artifacts implements
        Artifactlist,
        ArtifactErrors,
        ArtifactListSize
{

    private final List<Artifact> artifactlist;

    private String description;

    public Artifacts(List<Artifact> artifacts, String description) {
        this.artifactlist =artifacts;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<Artifact> getArtifactlist() {
        return artifactlist;
    }

    @Override
    public Artifacts setDescription(String description) {
        this.description = description;
        return this;
    }
}
