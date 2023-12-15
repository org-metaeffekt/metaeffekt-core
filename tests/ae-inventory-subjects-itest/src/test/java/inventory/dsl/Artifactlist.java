package inventory.dsl;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.List;

public interface Artifactlist {

    String getDescription();

    List<Artifact> getArtifactlist();
}
