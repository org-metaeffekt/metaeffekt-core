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

/**
 * Merges inventories. Reference inventories are usually the target inventory. Otherwise this Mojo is intended to
 * be applied to inventories from the extraction process, only.
 */
@Mojo( name = "merge-inventories", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class InventoryMergeMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * The source inventory. Required and referenced file must exist.
     */
    @Parameter(required = true)
    protected File sourceInventory;

    /**
     * The target inventory. The parameter must be specified. However the target must not exist. If the target inventory
     * does not exist the complete source inventory is copied into the target location.
     */
    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // source inventory must exist
        validateInventoryFileExists(sourceInventory, "sourceInventory");

        try {
            final Inventory sourceInv = new InventoryReader().readInventory(sourceInventory);

            // convenience; if the target does not exist the source inventory is saved to the target location
            if (!targetInventory.exists()) {
                new InventoryWriter().writeInventory(sourceInv, targetInventory);
            }

            // if the target does not exist we initiate a new inventory
            final Inventory targetInv = new InventoryReader().readInventory(targetInventory);

            // complete artifacts in targetInv with checksums
            for (Artifact artifact : targetInv.getArtifacts()) {
                final List<Artifact> candidates = sourceInv.findAllWithId(artifact.getId());

                // matches match on project location
                final List<Artifact> matches = new ArrayList<>();
                for (Artifact candidate : candidates) {
                    if (matches(artifact, candidate)) {
                        matches.add(candidate);
                    }
                }
                for (Artifact match : matches) {
                    artifact.setChecksum(match.getChecksum());
                }
            }

            // add not covered artifacts from sourceInv in targetInv using id and checksum
            for (Artifact artifact : sourceInv.getArtifacts()) {
                final Artifact candidate = targetInv.findArtifactByIdAndChecksum(artifact.getId(), artifact.getChecksum());
                if (candidate == null) {
                    targetInv.getArtifacts().add(artifact);
                }
            }

            // NOTE:
            // - we do not merge component patterns; the target component patterns are preserved
            // - we do not merge license notices; merges are considered to use reference inventory as targets
            // - we do not merge vulnerabilities; vulnerabilities are in the reference inventory (target) or
            //   processed lateron
            // - we do not merge license data (reference is the target)

            // write inventory to target location
            new InventoryWriter().writeInventory(targetInv, targetInventory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
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
