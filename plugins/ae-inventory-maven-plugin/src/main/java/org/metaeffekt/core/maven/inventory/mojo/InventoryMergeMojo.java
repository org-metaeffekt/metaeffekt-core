package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mojo( name = "merge-inventories", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class InventoryMergeMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(required = true)
    protected File sourceInventory;

    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        validateInventoryFileExists(sourceInventory, "sourceInventory");
        validateInventoryFileExists(targetInventory, "targetInventory");

        try {
            Inventory sourceInv = new InventoryReader().readInventory(sourceInventory);
            Inventory targetInv = new InventoryReader().readInventory(targetInventory);

            // complete artifacts in targetInv with checksums
            for (Artifact artifact : targetInv.getArtifacts()) {
                List<Artifact> candidates = sourceInv.findAllWithId(artifact.getId());

                // matches match on id and project location
                List<Artifact> matches = new ArrayList<>();
                for (Artifact candidate : candidates) {
                    if (matches(artifact, candidate)) {
                        matches.add(candidate);
                    }
                }
                for (Artifact match : matches) {
                    artifact.setChecksum(match.getChecksum());
                }
            }

            // add not covered artifacts from sourceInv in targetInv
            for (Artifact artifact : sourceInv.getArtifacts()) {
                Artifact candidate = targetInv.findArtifactByIdAndChecksum(artifact.getId(), artifact.getChecksum());
                if (candidate == null) {
                    targetInv.getArtifacts().add(artifact);
                }
            }

            new InventoryWriter().writeInventory(targetInv, targetInventory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean matches(Artifact artifact, Artifact candidate) {
        for (String location : artifact.getProjects()) {
            for (String candidateLocation : candidate.getProjects()) {
                if (candidateLocation.contains(location)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateInventoryFileExists(File inventory, String attributeKey) throws MojoFailureException {
        if (inventory == null || !inventory.exists() || inventory.isDirectory()) {
            throw new MojoFailureException("File specified by " + attributeKey + " must exist.");
        }
    }
}
