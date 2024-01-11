package org.metaeffekt.core.itest.inventory.dsl;

import org.metaeffekt.core.itest.inventory.ArtifactList;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface ArtifactListLogger extends ArtifactListDescriptor {

    static final Logger LOG = LoggerFactory.getLogger(ArtifactListLogger.class);

    default ArtifactList logArtifactList(){
        getArtifactList().forEach(artifact -> LOG.info(artifact.toString()));
        return (ArtifactList)this;
    }

    default ArtifactList logArtifactList(String ... additionalAttributes){
        getArtifactList().forEach(artifact ->
            LOG.info(artifactWithAttributes(artifact, additionalAttributes)));
        return (ArtifactList)this;
    }

    default ArtifactList logArtifactListWithAllAtributes(){
        getArtifactList().forEach(artifact -> {
                    String[] attributes = artifact.getAttributes().toArray(new String[0]);
                    LOG.info(artifactWithAttributes(artifact, attributes));
                }
                );
        return (ArtifactList)this;
    }

    static String artifactWithAttributes(Artifact artifact, String[] additionalAttributes){
        StringBuilder sb = new StringBuilder(artifact.toString());
        for (String additionalAttribute : additionalAttributes) {
            sb.append(", ")
                    .append(additionalAttribute)
                    .append(": ")
                    .append(artifact.get(additionalAttribute));
        }
        return sb.toString();
    }

    /**
     * Show some informational logs into the test logging stdout.
     * @param attributes Select the displayed additional attributes.
     */
    default ArtifactList logArtifactList(Artifact.Attribute ... attributes){
        List<String> list = Arrays.stream(attributes).map(Artifact.Attribute::getKey).collect(Collectors.toList());
        logArtifactList(list.toArray(new String[0]));
        return (ArtifactList) this;
    }

    /**
     * Show some informational logs into the test logging stdout.
     * @param info The info text to be logged during test execution.
     */
    default ArtifactList logInfo(String ... info){
        LOG.info(info.length > 0 ? info[0] : "- - - - - ");
        return (ArtifactList) this;
    }

}
