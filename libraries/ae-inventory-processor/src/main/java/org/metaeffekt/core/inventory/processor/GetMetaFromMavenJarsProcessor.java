package org.metaeffekt.core.inventory.processor;


import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.probe.MavenJarIdProbe;

public class GetMetaFromMavenJarsProcessor extends AbstractInventoryProcessor {

    @Override
    public void process(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
            probe.runCompletion();
        }
    }
}
