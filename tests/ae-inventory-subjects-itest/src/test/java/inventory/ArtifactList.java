package inventory;

import inventory.dsl.ArtifactListLogger;
import inventory.dsl.ArtifactListSize;
import inventory.dsl.ArtifactListFilter;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.List;

public class ArtifactList implements
        ArtifactListFilter,
        ArtifactListSize,
        ArtifactListLogger {

    private final List<Artifact> artifactlist;

    private String description;

    public ArtifactList(List<Artifact> artifacts, String description) {
        this.artifactlist = artifacts;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<Artifact> getArtifactList() {
        return artifactlist;
    }

    @Override
    public ArtifactList setDescription(String description) {
        this.description = description;
        return this;
    }
}
