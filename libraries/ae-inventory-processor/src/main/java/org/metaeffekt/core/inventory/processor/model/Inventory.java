/**
 * Copyright 2009-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Class representing an inventory of artifact and license meta data. The implementation
 * offers various methods to analyze, optimize and utilize the meta data.
 *
 * @author Karsten Klein
 */
public class Inventory {

    public static final String CLASSIFICATION_CURRENT = "current";

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);
    private static final String STRING_EMPTY = "";
    private static final String VERSION_UNSPECIFIED = "unspecified";
    private static final String ASTERISK = "*";
    private static final char DELIMITER_COLON = ':';

    private List<Artifact> artifacts = new ArrayList<Artifact>();

    private List<LicenseMetaData> licenseMetaData = new ArrayList<LicenseMetaData>();

    private Map<String, String> licenseNameMap = new HashMap<String, String>();

    private Map<String, String> componentNameMap = new HashMap<String, String>();

    private Map<String, Object> contextMap = new HashMap<>();

    public static void sortArtifacts(List<Artifact> artifacts) {
        Comparator<Artifact> comparator = new Comparator<Artifact>() {

            @Override
            public int compare(Artifact o1, Artifact o2) {
                return createRepresentation(o1).compareTo(createRepresentation(o2));
            }

            private String createRepresentation(Artifact o1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtils.hasText(o1.getGroupId()) ? o1.getGroupId() : STRING_EMPTY);
                sb.append(DELIMITER_COLON);
                sb.append(StringUtils.hasText(o1.getArtifactId()) ? o1.getArtifactId() : STRING_EMPTY);
                sb.append(DELIMITER_COLON);
                sb.append(StringUtils.hasText(o1.getVersion()) ? o1.getVersion() : STRING_EMPTY);
                return sb.toString();
            }
        };
        Collections.sort(artifacts, comparator);
    }

    /**
     * Merges this inventory with the specified inventory according to well-defined merge
     * rules. The merge assumes that the inventory specified as parameter contains more
     * recent information. The merge is component name-centric and focuses on the license and
     * usage meta data, only.
     * <ul>
     * <li>project list is merged</li>
     * <li>license is replaced</li>
     * <li>reported flag is replaced</li>
     * <li>versionCovered flag is recomputed</li>
     * <li>used flag is replaced</li>
     * <li>url is replaced</li>
     * </ul>
     *
     * @param mergeInventory The inventory to merge to this inventory.
     * @param propagateNotExisting Boolean indicating whether artifacts that do not yet exist in
     *            this inventory should be added.
     */
    public void mergeLicenseData(Inventory mergeInventory, boolean propagateNotExisting) {

        List<Artifact> derivedArtifacts = new ArrayList<Artifact>(mergeInventory.artifacts);

        Map<String, Set<Artifact>> mergeArtifactMap = mergeInventory.buildMapByName();

        for (Artifact localArtifact : this.artifacts) {
            String localName = localArtifact.getComponent();

            String mergeName = localName;

            boolean merged = false;
            if (StringUtils.hasText(localName)) {
                Set<Artifact> mergeSet = mergeArtifactMap.get(mergeName);
                if (mergeSet != null) {
                    for (Artifact mergeArtifact : mergeSet) {
                        boolean versionMatch = localArtifact.getVersion() != null &&
                                localArtifact.getVersion().equals(mergeArtifact.getVersion());
                        if (versionMatch) {
                            mergeArtifactLicenseData(mergeArtifact, localArtifact);
                            merged = true;
                            derivedArtifacts.remove(mergeArtifact);
                        }
                    }
                } else {
                    // no matching protex component
                    localArtifact.setVersionReported(false);
                    localArtifact.setReported(false);
                    merged = true;
                }
                if (!merged) {
                    // reasons: no matching version

                    LOG.debug("No matching version for component: {}", localName);

                    Artifact match = null;

                    // simply take the first artifact from the merge set
                    if (!mergeSet.isEmpty()) {
                        match = mergeSet.iterator().next();
                    }

                    if (match != null) {
                        // in case of unspecified version we need to check the
                        // for unique licenses. In case the license is ambiguous
                        // we do not merge.
                        LOG.debug("Checking for uniqueness of license for: {}", localName);
                        boolean unique = true;
                        String license = null;
                        boolean first = true;
                        for (Artifact mergeArtifact : mergeSet) {
                            if (first) {
                                license = mergeArtifact.getLicense();
                                first = false;
                            } else {
                                String candidateLicense = mergeArtifact.getLicense();
                                if ((license == null && candidateLicense == null) ||
                                        license != null && license.equals(candidateLicense)) {
                                    // preserve uniquess
                                } else {
                                    unique = false;
                                    LOG.debug("license [{}] != candidateLicense [{}]", license, candidateLicense);
                                }
                            }
                        }
                        if (unique) {
                            mergeArtifactLicenseData(match, localArtifact);
                            merged = true;
                            derivedArtifacts.remove(match);

                            localArtifact.setReported(true);
                        } else {
                            LOG.warn("No explicity version and no unique license reference found for: {}",
                                    localArtifact.createStringRepresentation());

                            // you need to introduce the specific version in the protex inventory to
                            // fix this. Otherwise the data in the resulting inventory will only
                            // be partially merged (conserved)
                        }
                    }
                }
                if (!merged) {
                    localArtifact.setVersionReported(false);
                    localArtifact.setReported(false);

                    // FIXME: activate with next inventory update
                    // localArtifact.setComment("" + localArtifact.getComment() +
                    // "[REVIEW REQUIRED]");
                    merged = true;
                }
            }

        }

        if (propagateNotExisting) {
            this.artifacts.addAll(derivedArtifacts);
        }
    }

    private void mergeArtifactLicenseData(Artifact mergeArtifact, Artifact localArtifact) {
        // the project list is extended
        localArtifact.addProjects(mergeArtifact.getProjects());

        // transport the reported id
        localArtifact.setReported(mergeArtifact.isReported());

        boolean versionCovered =
                localArtifact.getVersion().equalsIgnoreCase(mergeArtifact.getVersion());
        versionCovered |= localArtifact.isVersionReported();
        versionCovered &= !VERSION_UNSPECIFIED.equalsIgnoreCase(localArtifact.getVersion());
        localArtifact.setVersionReported(versionCovered);

        localArtifact.setUsed(mergeArtifact.isUsed());

        // protex ip url is master
        localArtifact.setUrl(mergeArtifact.getUrl());
    }

    private Map<String, Set<Artifact>> buildMapByName() {
        Map<String, Set<Artifact>> artifactMap = new HashMap<>();
        for (Artifact artifact : artifacts) {
            String key = artifact.getComponent();
            if (key != null && key.trim().length() > 0) {
                Set<Artifact> set = artifactMap.get(key);
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(artifact);
                artifactMap.put(key, set);
            }
        }
        return artifactMap;
    }

    public void expandArtifactsWithMultipleVersions() {

        List<Artifact> derivedArtifacts = new ArrayList<>();
        List<Artifact> removedArtifacts = new ArrayList<>();

        for (Artifact artifact : this.artifacts) {
            String version = artifact.getVersion();

            if (version != null) {
                String[] split = version.split(",");

                if (split.length > 1) {
                    for (String v : split) {
                        Artifact a = new DefaultArtifact(artifact);
                        a.setVersion(v.trim());
                        derivedArtifacts.add(a);
                    }
                    removedArtifacts.add(artifact);
                }
            }
        }

        this.artifacts.removeAll(removedArtifacts);
        this.artifacts.addAll(derivedArtifacts);

    }

    public void mergeDuplicates() {

        Map<String, Set<Artifact>> artifactMap = new HashMap<String, Set<Artifact>>();

        for (Artifact artifact : artifacts) {
            artifact.deriveArtifactId();

            String key = artifact.getId();
            if (!StringUtils.hasText(key)) {
                key = artifact.getComponent();
            }

            if (StringUtils.hasText(key)) {
                key += "^" + artifact.getVersion() + "^" + artifact.getGroupId();
                Set<Artifact> set = artifactMap.get(key);
                if (set == null) {
                    set = new HashSet<Artifact>();
                }
                set.add(artifact);
                artifactMap.put(key, set);
            }
        }

        for (Set<Artifact> set : artifactMap.values()) {
            if (set.size() > 1) {
                Iterator<Artifact> it = set.iterator();
                Artifact ref = it.next();
                while (it.hasNext()) {
                    Artifact a = it.next();
                    ref.merge(a);
                    artifacts.remove(a);
                }
            }
        }
    }

    public Artifact findArtifact(Artifact artifact) {
        return findArtifact(artifact, false);
    }

    public Artifact findArtifact(Artifact artifact, boolean fuzzy) {
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
        }
        for (Artifact candidate : getArtifacts()) {
            if (matchesOnMavenProperties(artifact, candidate)) {
                return candidate;
            }
        }

        // the pure match on id is required to support filesystem scans (no maven metadata available)
        if (fuzzy) {
            for (Artifact candidate : getArtifacts()) {
                if (matchesOnId(artifact.getId(), candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Artifact findArtifact(String id) {
        for (Artifact candidate : getArtifacts()) {
            if (matchesOnId(id, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean matchesOnMavenProperties(Artifact artifact, Artifact candidate) {
        if (!matchesOnId(artifact.getId(), candidate)) {
            return false;
        }
        if (!matchesOnType(artifact, candidate)) {
            return false;
        }
        if (candidate.getGroupId() == null)
            return false;
        if (candidate.getArtifactId() == null)
            return false;
        if (candidate.getVersion() == null)
            return false;
        if (artifact.getGroupId() == null)
            return false;
        if (artifact.getArtifactId() == null)
            return false;
        if (artifact.getVersion() == null)
            return false;
        if (!candidate.getGroupId().trim().equalsIgnoreCase(artifact.getGroupId().trim()))
            return false;
        if (!candidate.getArtifactId().trim().equalsIgnoreCase(artifact.getArtifactId().trim()))
            return false;
        if (!candidate.getVersion().trim().equalsIgnoreCase(artifact.getVersion().trim()))
            return false;
        return true;
    }

    protected boolean matchesOnId(String id, Artifact candidate) {
        final String candidateId = candidate.getId();

        if (id == null && candidateId == null) {
            return true;
        }

        if (candidateId == null) {
            return false;
        }

        // check the ids match (exact)
        if (id != null) {
            if (id.equals(candidateId)) {
                return true;
            }
        }

        // check the ids match (allow wildcard)
        if (ASTERISK.equals(candidate.getVersion())) {
            final int index = candidate.getId().indexOf(ASTERISK);
            if (index != -1) {
                String prefix = candidate.getId().substring(0, index - 1);
                String suffix = candidate.getId().substring(index + 1);
                if (id.startsWith(prefix) && id.endsWith(suffix)) {
                    return true;
                }
            }
        }

        // FIXME: id matching must be supported by checksum
        return false;
    }

    protected boolean matchesOnType(Artifact artifact, Artifact candidate) {
        final String artifactType = artifact.getType();
        final String candidateType = candidate.getType();
        if (artifactType == null && candidateType == null) {
            return true;
        }
        if (artifactType != null) {
            return artifactType.equals(candidateType);
        }
        return false;
    }

    public Artifact findArtifact(String groupId, String artifactId) {
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
            if (candidate.getArtifactId() == null)
                continue;
            if (candidate.getGroupId() == null)
                continue;
            if (!candidate.getArtifactId().trim().equals(artifactId.trim()))
                continue;
            if (!candidate.getGroupId().trim().equals(groupId.trim()))
                continue;
            return candidate;
        }
        return null;
    }

    public Set<Artifact> findArtifacts(String groupId, String artifactId) {
        Set<Artifact> matchingArtifacts = new HashSet<Artifact>();
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
            if (candidate.getArtifactId() == null)
                continue;
            if (candidate.getGroupId() == null)
                continue;
            if (!candidate.getArtifactId().trim().equals(artifactId.trim()))
                continue;
            if (!candidate.getGroupId().trim().equals(groupId.trim()))
                continue;

            matchingArtifacts.add(candidate);
        }
        return matchingArtifacts;
    }

    public void sortArtifacts() {
        sortArtifacts(artifacts);
    }

    public List<String> evaluateLicenses(boolean includeLicensesWithArtifactsOnly) {
        return evaluateLicenses(includeLicensesWithArtifactsOnly, false);
    }

    /**
     * Returns a sorted list of licenses that is covered by this inventory. Please note that this
     * method only produces the license names, which are assumed to be non-redundant and unique.
     *
     * @param includeLicensesWithArtifactsOnly
     * @param includeManagedArtifactsOnly
     * @return List of license names covered by this inventory.
     */
    public List<String> evaluateLicenses(boolean includeLicensesWithArtifactsOnly, boolean includeManagedArtifactsOnly) {
        Set<String> licenses = new HashSet<String>();
        for (Artifact artifact : getArtifacts()) {
            // not relevant artifact licenses must not be included
            if (!artifact.isRelevant()) {
                continue;
            }

            // skip license in case only managed artifacts are to be included
            if (includeManagedArtifactsOnly && !artifact.isManaged()) {
                continue;
            }

            String artifactLicense = artifact.getLicense();

            if (!StringUtils.hasText(artifact.getArtifactId())) {
                if (includeLicensesWithArtifactsOnly) {
                    continue;
                }
            }

            // check whether there is an effective license (set of licenses)

            final LicenseMetaData matchingLicenseMetaData = findMatchingLicenseMetaData(artifact.getComponent(), artifactLicense, artifact.getVersion());
            if (matchingLicenseMetaData != null) {
                artifactLicense = matchingLicenseMetaData.deriveLicenseInEffect();
            }

            if (artifactLicense != null) {
                String[] splitLicense = artifactLicense.split("\\|");
                for (int i = 0; i < splitLicense.length; i++) {
                    if (StringUtils.hasText(splitLicense[i])) {
                        licenses.add(splitLicense[i].trim());
                    }
                }
            }
        }

        List<String> sortedByLicense = new ArrayList<>(licenses);
        Collections.sort(sortedByLicense);
        return sortedByLicense;
    }

    /**
     * Returns all relevant notices for a given effective license.
     *
     * @param effectiveLicense The effective license.
     *
     * @return List of {@link ArtifactLicenseData} instances.
     */
    public List<ArtifactLicenseData> evaluateNotices(String effectiveLicense) {
        final Map<String, ArtifactLicenseData> map = new LinkedHashMap<>();
        for (final Artifact artifact : artifacts) {
            String artifactLicense = artifact.getLicense();
            if (artifactLicense != null) {
                artifactLicense = artifactLicense.trim();
                // find a matching LMD instance
                LicenseMetaData match = findMatchingLicenseMetaData(
                        artifact.getComponent(), artifactLicense, artifact.getVersion());
                if (match != null && matches(effectiveLicense, match)) {
                    String qualifier = new StringBuilder(artifactLicense).append("-").
                        append(artifact.getVersion()).append("-").append(artifact.getComponent()).toString();
                    ArtifactLicenseData artifactLicenseData = map.get(qualifier);
                    if (artifactLicenseData == null) {
                        artifactLicenseData = new ArtifactLicenseData(artifact.getComponent(), artifact.getVersion(), match);
                        map.put(qualifier, artifactLicenseData);
                    }
                    artifactLicenseData.add(artifact);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private boolean matches(String effectiveLicense, LicenseMetaData match) {
        List<String> licenses = Arrays.asList(match.deriveLicenseInEffect().split("\\|"));
        return licenses.contains(effectiveLicense);
    }

    /**
     * Iterates throw the license meta data to find a match for the given component and
     * license parameters.
     *
     * @param component The component.
     * @param license The license name.
     * @return A matching {@link LicenseMetaData} instance if available. In case multiple
     *         can be matched an {@link IllegalStateException} is thrown.
     */
    public LicenseMetaData findMatchingLicenseMetaData(String component, String license,
                                                       String version) {
        LicenseMetaData match = null;
        for (LicenseMetaData lmd : licenseMetaData) {
            if (lmd.getLicense().equals(license) &&
                    (lmd.getVersion().equals(version) || lmd.getVersion().equals(ASTERISK)) &&
                    (lmd.getComponent().equals(component) || lmd.getComponent().equals(ASTERISK))) {
                if (match != null) {
                    // in case match is not null we have found two matching license meta data elements
                    // this means that the license data is inconsistent and has overlaps. This must
                    // be resolved in the underlying meta data.
                    throw new IllegalStateException("Multiple matches for license " + license
                        + ". Meta data inconsistent. Please correct license meta data to resolve inconsistencies.");
                }
                match = lmd;
            }
        }
        return match;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public List<LicenseMetaData> getLicenseMetaData() {
        return licenseMetaData;
    }

    public void setLicenseMetaData(List<LicenseMetaData> licenses) {
        this.licenseMetaData = licenses;
    }

    public String getLicenseFolder(String license) {
        if (StringUtils.hasText(license)) {
            return LicenseMetaData.deriveLicenseFolderName(license.trim());
        } else {
            return null;
        }
    }

    public void dumpAsFile(File file) {
        file.delete();
        List<String> strings = new ArrayList<String>();
        try {
            for (Artifact artifact : artifacts) {
                artifact.deriveArtifactId();
                strings.add(artifact.createCompareStringRepresentation());
            }
            Collections.sort(strings);
            FileUtils.writeLines(file, strings, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String deriveLicenseId(String license) {
        if (license == null)
            return null;
        return removeSpecialCharacters(license);
    }

    private String removeSpecialCharacters(String license) {
        String licenseId = LicenseMetaData.deriveLicenseFolderName(license);
        licenseId = licenseId.replace("(", "_");
        licenseId = licenseId.replace(")", "_");
        licenseId = licenseId.replace(" ", "-");
        int length = -1;
        while (length != licenseId.length()) {
            length = licenseId.length();
            licenseId = licenseId.replace("__", "_");
            licenseId = licenseId.replace("--", "-");
        }
        return licenseId.toLowerCase();
    }

    public Artifact findMatchingId(Artifact artifact) {
        final String id = normalize(artifact.getId());
        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateId = normalize(candidate.getId());
                if (id.equals(candidateId)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Artifact findCurrent(Artifact artifact) {
        final String id = normalize(artifact.getId());
        final String artifactId = normalize(artifact.getArtifactId());
        final String groupId = normalize(artifact.getGroupId());
        final String classifier = normalize(artifact.getClassifier());
        final String type = normalize(artifact.getType());

        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateClassification = normalize(candidate.getClassification());
                if (candidateClassification.contains(CLASSIFICATION_CURRENT)) {
                    final String candidateGroupId = normalize(candidate.getGroupId());
                    final String candidateArtifactId = normalize(candidate.getArtifactId());
                    final String candidateClassifier = normalize(candidate.getClassifier());
                    final String candidateType = normalize(candidate.getType());
                    if (groupId.equals(candidateGroupId)) {
                        if (artifactId.equals(candidateArtifactId)) {
                            if (classifier.equals(candidateClassifier)) {
                                if (type.equals(candidateType)) {
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Artifact findArtifactClassificationAgnostic(Artifact artifact) {
        final String id = normalize(artifact.getId());
        final String artifactId = normalize(artifact.getArtifactId());
        final String groupId = normalize(artifact.getGroupId());
        final String classifier = normalize(artifact.getClassifier());
        final String type = normalize(artifact.getType());

        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateGroupId = normalize(candidate.getGroupId());
                final String candidateArtifactId = normalize(candidate.getArtifactId());
                final String candidateClassifier = normalize(candidate.getClassifier());
                final String candidateType = normalize(candidate.getType());
                if (groupId.equals(candidateGroupId)) {
                    if (artifactId.equals(candidateArtifactId)) {
                        if (classifier.equals(candidateClassifier)) {
                            if (type.equals(candidateType)) {
                                return candidate;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public String normalize(String s) {
        if (StringUtils.hasText(s))
            return s.trim();
        return STRING_EMPTY;
    }

    public List<Component> evaluateComponents() {
        Set<Component> componentNames = new HashSet<Component>();
        for (Artifact artifact : getArtifacts()) {
            if (StringUtils.hasText(artifact.getComponent())) {
                final Component component = createComponent(artifact);
                componentNames.add(component);
            }
        }

        List<Component> sortedByComponent = new ArrayList<Component>(componentNames);
        Collections.sort(sortedByComponent, new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });
        return sortedByComponent;
    }

    public List<Artifact> evaluateComponent(Component component,
                                            boolean includeLicensesWithArtifactsOnly) {
        List<Artifact> artifactsForComponent = new ArrayList<Artifact>();
        for (Artifact artifact : getArtifacts()) {
            if (component.group != null && component.group.equals(artifact.getComponent())) {
                if (component.license != null && component.license.equals(artifact.getLicense())) {

                    if (!StringUtils.hasText(artifact.getArtifactId())) {
                        if (includeLicensesWithArtifactsOnly) {
                            continue;
                        }
                    }

                    artifactsForComponent.add(artifact);
                }
            }
        }
        sortArtifacts(artifactsForComponent);
        return artifactsForComponent;
    }

    public List<Artifact> evaluateLicense(String licenseName,
                                          boolean includeLicensesWithArtifactsOnly) {
        List<Artifact> artifactsForComponent = new ArrayList<>();
        for (Artifact artifact : getArtifacts()) {
            if (licenseName.equals(artifact.getLicense())) {
                if (!StringUtils.hasText(artifact.getArtifactId())) {
                    if (includeLicensesWithArtifactsOnly) {
                        continue;
                    }
                }
                artifactsForComponent.add(artifact);
            }
        }
        sortArtifacts(artifactsForComponent);
        return artifactsForComponent;
    }

    public boolean removeInconsistencies() {
        Set<String> uniqueSet = new HashSet<String>();
        Set<String> qualifiedSet = new HashSet<String>();
        Map<String, Artifact> map = new HashMap<String, Artifact>();
        sortArtifacts();
        boolean b = true;
        int index = 1;

        List<Artifact> removeList = new ArrayList<Artifact>();

        for (Artifact a : getArtifacts()) {
            if (StringUtils.hasText(a.getComponent())) {
                String key = a.getComponent() + "/" + a.getVersion();
                String qualifier = key + "/" + a.getLicense() + "/" + a.getVersion();
                if (uniqueSet.contains(key) && !qualifiedSet.contains(qualifier)) {
                    Artifact duplicate = map.get(key);
                    LOG.warn("Detected inconsistency #{}: {} {}",
                            index++, a.createCompareStringRepresentation(),
                            duplicate.createCompareStringRepresentation());
                    b = false;

                    removeList.add(a);
                    removeList.add(duplicate);
                }
                uniqueSet.add(key);
                qualifiedSet.add(qualifier);
                map.put(key, a);
            }
        }

        getArtifacts().removeAll(removeList);
        return b;
    }

    public void mapComponentNames() {
        if (getArtifacts() != null) {
            for (Artifact a : getArtifacts()) {
                String mappedName = mapComponentName(a.getComponent());
                if (mappedName != null) {
                    a.setComponent(mappedName);
                }
            }
        }
        if (getLicenseMetaData() != null) {
            for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
                String mappedName = mapComponentName(licenseMetaData.getComponent());
                if (mappedName != null) {
                    licenseMetaData.setComponent(mappedName);
                }
            }
        }
    }

    private String mapComponentName(String componentName) {
        String mappedName = null;
        if (componentName != null) {
            componentName = componentName.trim();
            mappedName = componentNameMap.get(componentName);
        }
        return mappedName;
    }

    public void mapLicenseNames() {
        if (getArtifacts() != null) {
            for (Artifact a : getArtifacts()) {
                String mappedName = mapLicenseName(a.getLicense());
                if (mappedName != null) {
                    a.setLicense(mappedName);
                }
            }
        }
        if (getLicenseMetaData() != null) {
            for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
                String mappedName = mapLicenseName(licenseMetaData.getName());
                if (mappedName != null) {
                    licenseMetaData.setName(mappedName);
                }
            }
        }
    }

    private String mapLicenseName(String licenseName) {
        if (licenseName != null) {
            licenseName = licenseName.trim();
            return licenseNameMap.get(licenseName);
        }
        return null;
    }

    /**
     * Remove all artifacts that do not contain or valid data
     */
    public void cleanup() {
        Iterator<Artifact> iterator = getArtifacts().iterator();
        while (iterator.hasNext()) {
            final Artifact artifact = iterator.next();

            // an artifact requires at least an id or a component (name)
            if (!StringUtils.hasText(artifact.getArtifactId()) &&
                    !StringUtils.hasText(artifact.getComponent())) {
                iterator.remove();
            }
        }
    }

    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

    public Map<String, String> getLicenseNameMap() {
        return licenseNameMap;
    }

    public void setLicenseNameMap(Map<String, String> licenseNameMap) {
        this.licenseNameMap = licenseNameMap;
    }

    public Map<String, String> getComponentNameMap() {
        return componentNameMap;
    }

    public void setComponentNameMap(Map<String, String> componentNameMap) {
        this.componentNameMap = componentNameMap;
    }

    public boolean hasConcreteArtifacts(Artifact artifact) {
        if (artifact != null) {
            String id = artifact.getId();
            return StringUtils.hasText(id);
        }
        return false;
    }

    public boolean hasConcreteArtifacts(Collection<Artifact> artifacts) {
        if (artifacts != null && !artifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                if (hasConcreteArtifacts(artifact)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a shallow copy of the inventory. The artifact list is filtered such that only
     * artifacts evaluating isRelevant() to <code>true</code> are taken over into the new inventory.
     *
     * @return A shallow copy of the inventory containing only report-relevant artifacts.
     */
    public Inventory getFilteredInventory() {
        Inventory filteredInventory = new Inventory();
        for (Artifact artifact : getArtifacts()) {
            if (artifact.isRelevant()) {
                filteredInventory.getArtifacts().add(artifact);
            }
        }
        filteredInventory.setComponentNameMap(getComponentNameMap());
        filteredInventory.setContextMap(getContextMap());
        filteredInventory.setLicenseMetaData(getLicenseMetaData());
        filteredInventory.setLicenseNameMap(getLicenseNameMap());
        return filteredInventory;
    }

    public Component createComponent(Artifact artifact) {
        return new Component(artifact.getComponent(), artifact.getLicense());
    }

    public class Component {
        private String group;
        private String license;

        public Component(String group, String license) {
            this.group = group;
            this.license = license;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof Component) {
                return toString().equals(o.toString());
            }
            return false;
        }

        @Override
        public String toString() {
            return STRING_EMPTY + group + "/" + license;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }
    }

}
