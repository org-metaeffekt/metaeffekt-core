package inventory.dsl;

import inventory.Artifacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ArtifactListLogger extends Artifactlist {

    static final Logger LOG = LoggerFactory.getLogger(ArtifactListLogger.class);


    default Artifacts logArtifactList(){
        getArtifactlist().forEach(artifact -> LOG.info(artifact.toString()));
        return (Artifacts)this;
    }
}
