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
import org.metaeffekt.core.util.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Merges inventories. Reference inventories are usually the target inventory. Otherwise this Mojo is intended to
 * be applied to inventories from the extraction process, only.
 */
@Mojo(name = "merge-inventories", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class InventoryMergeMojo extends AbstractProjectAwareConfiguredMojo {

    /**
     * The source inventory.
     */
    @Parameter(required = false)
    protected File sourceInventory;

    /**
     * Alternatively to the sourceInventory parameter a directory can be specified. The inventory is scanned for files
     * using sourceInventoryIncludes and sourceInventoryExcludes.
     */
    @Parameter(required = false)
    protected File sourceInventoryBaseDir;

    /**
     * Includes based on sourceInventoryBaseDir.
     */
    @Parameter(defaultValue = "**/*.xls")
    protected String sourceInventoryInclude;

    /**
     * Excludes based on sourceInventoryBaseDir.
     */
    @Parameter
    protected String sourceInventoryExcludes;

    /**
     * Boolean indicating whether to include the default artifact excluded attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactExcludedAttributes = true;

    /**
     * The excluded attributes will be dropped when merging. The list may also be considered
     * as selection of attributes, which must not be differentiated by the merge process.
     */
    @Parameter(required = false)
    protected Set<String> artifactExcludedAttributes = new HashSet<>();

    /**
     * Boolean indicating whether to include the default artifact merge attributes.
     */
    @Parameter(defaultValue = "true")
    protected boolean addDefaultArtifactMergeAttributes = true;

    /**
     * The merge attributes will be merged on attribute level. Also these attributes are not
     * differentiated by the merge process.
     */
    @Parameter(required = false)
    protected Set<String> artifactMergeAttributes = new HashSet<>();

    /**
     * The target inventory. The parameter must be specified. However the target must not exist. If the target inventory
     * does not exist the complete source inventory is copied into the target location.
     */
    @Parameter(required = true)
    protected File targetInventory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            final List<File> sourceInventories = new ArrayList<>();

            if (sourceInventory != null) {
                if (sourceInventory.exists() && sourceInventory.isFile()) {
                    sourceInventories.add(sourceInventory);
                } else {
                    throw new MojoExecutionException("The parameter [sourceInventory] is set, but the file does not exist: " + sourceInventory.getAbsolutePath());
                }
            }

            if (sourceInventoryBaseDir != null) {
                if (sourceInventoryBaseDir.exists() && sourceInventoryBaseDir.isDirectory()) {
                    final String[] sourceFiles = FileUtils.scanForFiles(sourceInventoryBaseDir, sourceInventoryInclude, sourceInventoryExcludes);
                    Arrays.stream(sourceFiles).forEach(f -> sourceInventories.add(new File(sourceInventoryBaseDir, f)));
                } else {
                    throw new MojoExecutionException("The parameter [sourceInventoryDir] parameter is set, but the directory does not exist: " + sourceInventoryBaseDir.getAbsolutePath());
                }
            }

            // if the target does not exist we initiate a new inventory
            final Inventory targetInventory = this.targetInventory.exists() ?
                    new InventoryReader().readInventory(this.targetInventory) : new Inventory();

            // parse and merge the collected inventories
            for (File sourceInv : sourceInventories) {
                mergeSingleInventory(new InventoryReader().readInventory(sourceInv), targetInventory);
            }

            // NOTE:

            // - we merge artifacts
            // - we merge assets (currently only by adding; not merging; not cleaning duplicates)

            // TODO: revise the following once assets have been fully established
            // - we do not merge component patterns; the target component patterns are preserved (future: tbc)
            // - we do not merge license notices; merges are considered to use reference inventory as targets (future: merge and remove duplicates)
            // - we do not merge vulnerabilities; vulnerabilities are in the reference inventory (target) or
            //   processed later on (future: organize vulnerability data per asset, merge details on artifact level)
            // - we do not merge license data (reference is the target; future: merge; manage assets columns; remove duplicates)

            // write inventory to target location
            new InventoryWriter().writeInventory(targetInventory, this.targetInventory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void mergeSingleInventory(Inventory sourceInv, Inventory targetInv) {
        // complete artifacts in targetInv with checksums (with matching project location)
        // NOTE: this assumes that the source inventory container extensions to the target inventory
        for (final Artifact artifact : targetInv.getArtifacts()) {

            // existing checksums must not be overwritten
            if (StringUtils.hasText(artifact.getChecksum())) continue;

            final List<Artifact> candidates = sourceInv.findAllWithId(artifact.getId());

            // matches match on project location
            final List<Artifact> matches = new ArrayList<>();
            for (final Artifact candidate : candidates) {
                if (matchesProjects(artifact, candidate)) {
                    matches.add(candidate);
                }
            }
            for (final Artifact match : matches) {
                artifact.setChecksum(match.getChecksum());
            }
        }

        // add NOT COVERED artifacts from sourceInv in targetInv using id and checksum
        for (final Artifact artifact : sourceInv.getArtifacts()) {
            final Artifact candidate = targetInv.findArtifactByIdAndChecksum(artifact.getId(), artifact.getChecksum());
            if (candidate == null) {
                targetInv.getArtifacts().add(artifact);
            }
        }

        // simply add asset data
        targetInv.getAssetMetaData().addAll(sourceInv.getAssetMetaData());

        // remove duplicates (in the sense of exlude and merge attributes
        final Set<Artifact> toBeDeleted = new HashSet<>();
        final Map<String, Artifact> representationArtifactMap = new HashMap<>();
        final List<String> attributes = new ArrayList<>();
        final Set<String> excludedAttributes = new HashSet<>(artifactExcludedAttributes);

        if (addDefaultArtifactExcludedAttributes) {
            excludedAttributes.add("Verified");
            excludedAttributes.add("Archive Path");
            excludedAttributes.add("Latest Version");
            excludedAttributes.add("Security Relevance");
            excludedAttributes.add("Notice Parameter");
            excludedAttributes.add("License");
            excludedAttributes.add("Package Documentation Path");
            excludedAttributes.add("Package Group");
            excludedAttributes.add("Package License Path");
            excludedAttributes.add("Security Category");
            excludedAttributes.add("WILDCARD-MATCH");
            excludedAttributes.add("Issue");
        }

        // currently not configurable as these require
        final Set<String> mergeAttributes = new HashSet<>(artifactMergeAttributes);

        if (addDefaultArtifactMergeAttributes) {
            mergeAttributes.add("Projects");
            mergeAttributes.add("Source Project");
        }

        // compile attributes list covering all artifacts
        for (final Artifact artifact : targetInv.getArtifacts()) {

            // the excluded attributes are eliminated; merge attributes persist (as these are required)
            for (final String attribute : excludedAttributes) {
                artifact.set(attribute, null);
            }

            // add the (remaining) attributes to the overall list
            attributes.addAll(artifact.getAttributes());
        }

        // the merge attributes do not contribute to the artifacts representation
        attributes.removeAll(mergeAttributes);

        for (final Artifact artifact : targetInv.getArtifacts()) {
            final StringBuilder sb = new StringBuilder();
            for (String attribute : attributes) {
                if (sb.length() == 0) {
                    sb.append(";");
                }
                sb.append(attribute).append("=").append(artifact.get(attribute));
            }

            final String rep = sb.toString();
            if (representationArtifactMap.containsKey(rep)) {
                toBeDeleted.add(artifact);

                final Artifact retainedArtifact = representationArtifactMap.get(rep);
                for (final String key : mergeAttributes) {
                    retainedArtifact.append(key, artifact.get(key), ", ");
                }
            } else {
                representationArtifactMap.put(rep, artifact);
            }
        }

        targetInv.getArtifacts().removeAll(toBeDeleted);
    }

    private boolean matchesProjects(Artifact artifact, Artifact candidate) {
        for (String location : artifact.getProjects()) {
            for (String candidateLocation : candidate.getProjects()) {
                if (candidateLocation.contains(location)) {
                    return true;
                }
            }
        }
        return false;
    }

}
