package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InventoryMergeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryMergeUtils.class);

    protected boolean addDefaultArtifactExcludedAttributes = true;

    protected Set<String> artifactExcludedAttributes = new HashSet<>();

    protected boolean addDefaultArtifactMergeAttributes = true;

    protected Set<String> artifactMergeAttributes = new HashSet<>();

    // NOTE: we merge
    // - artifacts
    // - assets (currently only by adding; not merging; not cleaning duplicates)
    // - license data (resolving duplicates; merge on attribute level; input inventory should be consistent/up-to-date)
    // - license data (applying consecutive inherits)

    // TODO: revise the following once assets have been fully established
    // - we do not merge component patterns; the target component patterns are preserved (future: tbc; currently not required)
    // - we do not merge vulnerabilities; for inventories with vulnerabilities dedicated procedures exist.
    public void merge(List<File> sourceInventories, Inventory targetInventory) throws IOException {
        // parse and merge the collected inventories
        for (File sourceInventoryFile : sourceInventories) {
            final Inventory sourceInventory = new InventoryReader().readInventory(sourceInventoryFile);

            // merge and manage artifacts
            mergeArtifacts(sourceInventory, targetInventory);

            // simply add asset data (anticipating there are no duplicates)
            mergeAssetMetaData(sourceInventory, targetInventory);

            // merge license data
            mergeLicenseData(sourceInventory, targetInventory);

            // also merge license notices
            mergeLicenseMetaData(sourceInventory, targetInventory);
        }
    }

    private void mergeLicenseMetaData(Inventory sourceInventory, Inventory targetInventory) {
        if (sourceInventory.getLicenseMetaData() != null && !sourceInventory.getLicenseMetaData().isEmpty()) {
            targetInventory.inheritLicenseMetaData(sourceInventory, false);
        }
    }

    private void mergeArtifacts(Inventory sourceInv, Inventory targetInv) {
        // complete artifacts in targetInv with checksums (with matching project location)
        // NOTE: this assumes that the source inventory container extensions to the target inventory
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {

            // existing checksums must not be overwritten
            if (StringUtils.isNotBlank(artifact.getChecksum())) continue;

            final List<org.metaeffekt.core.inventory.processor.model.Artifact> candidates = sourceInv.findAllWithId(artifact.getId());

            // matches match on project location
            final List<org.metaeffekt.core.inventory.processor.model.Artifact> matches = new ArrayList<>();
            for (final org.metaeffekt.core.inventory.processor.model.Artifact candidate : candidates) {
                if (matchesProjects(artifact, candidate)) {
                    matches.add(candidate);
                }
            }
            for (final org.metaeffekt.core.inventory.processor.model.Artifact match : matches) {
                artifact.setChecksum(match.getChecksum());
            }
        }

        // add NOT COVERED artifacts from sourceInv in targetInv using id and checksum
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : sourceInv.getArtifacts()) {
            final org.metaeffekt.core.inventory.processor.model.Artifact candidate = targetInv.findArtifactByIdAndChecksum(artifact.getId(), artifact.getChecksum());
            if (candidate == null) {
                targetInv.getArtifacts().add(artifact);
            }
        }

        // remove duplicates (in the sense of exclude and merge attributes
        final Set<org.metaeffekt.core.inventory.processor.model.Artifact> toBeDeleted = new HashSet<>();
        final Map<String, org.metaeffekt.core.inventory.processor.model.Artifact> representationArtifactMap = new HashMap<>();
        final Set<String> attributes = new HashSet<>();
        final Set<String> excludedAttributes = new HashSet<>(artifactExcludedAttributes);

        if (addDefaultArtifactExcludedAttributes) {
            excludedAttributes.add("Verified");
            excludedAttributes.add("Archive Path");
            excludedAttributes.add("Latest Version");
            excludedAttributes.add("Security Relevance");
            excludedAttributes.add("Security Category");
            excludedAttributes.add("WILDCARD-MATCH");
        }

        // currently not configurable as these require specialized implementations
        final Set<String> mergeAttributes = new HashSet<>(artifactMergeAttributes);

        if (addDefaultArtifactMergeAttributes) {
            mergeAttributes.add("Projects");
            mergeAttributes.add("Source Project");
        }

        // compile attributes list covering all artifacts / clear excluded attributes
        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {

            // the excluded attributes are eliminated; merge attributes persist (as these are required)
            for (final String attribute : excludedAttributes) {
                artifact.set(attribute, null);
            }

            // add the (remaining) attributes to the overall list
            attributes.addAll(artifact.getAttributes());
        }

        // the merge attributes do not contribute to the artifacts representation
        attributes.removeAll(mergeAttributes);

        // produce an ordered list of the attributes
        final List<String> attributesOrdered = new ArrayList<>(attributes);
        attributesOrdered.sort(String::compareToIgnoreCase);

        for (final org.metaeffekt.core.inventory.processor.model.Artifact artifact : targetInv.getArtifacts()) {
            final StringBuilder stringRepresentation = new StringBuilder();
            for (String attribute : attributes) {
                if (stringRepresentation.length() == 0) {
                    stringRepresentation.append(";");
                }
                stringRepresentation.append(attribute).append("=").append(artifact.get(attribute));
            }

            final String rep = stringRepresentation.toString();
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

    private static void mergeAssetMetaData(Inventory sourceInv, Inventory targetInv) {
        targetInv.getAssetMetaData().addAll(sourceInv.getAssetMetaData());
    }

    private void mergeLicenseData(Inventory sourceInv, Inventory targetInv) {
        // build map
        final Map<String, LicenseData> canonicalNameLicenseDataMap = new HashMap<>();
        for (LicenseData licenseData : targetInv.getLicenseData()) {
            canonicalNameLicenseDataMap.put(licenseData.get(LicenseData.Attribute.CANONICAL_NAME), licenseData);
        }

        // compare, merge or insert
        for (LicenseData sourceLicenseData : sourceInv.getLicenseData()) {
            final String canonicalName = sourceLicenseData.get(LicenseData.Attribute.CANONICAL_NAME);
            final LicenseData targetLicenseData = canonicalNameLicenseDataMap.get(canonicalName);
            if (targetLicenseData != null) {
                targetLicenseData.merge(sourceLicenseData);
            } else {
                // does not exist in targetInv yet; add
                targetInv.getLicenseData().add(sourceLicenseData);

                // manage map
                canonicalNameLicenseDataMap.put(canonicalName, sourceLicenseData);
            }
        }
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
