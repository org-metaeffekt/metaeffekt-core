package org.metaeffekt.core.itest.inventory.dsl;

import org.metaeffekt.core.itest.inventory.ArtifactList;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.List;

public interface ArtifactListDescriptor {

    List<Artifact> getArtifactList();

    String getDescription();

    ArtifactList setDescription(String description);

    default ArtifactList as(String description) {
        return this.setDescription(description);
    }

}
