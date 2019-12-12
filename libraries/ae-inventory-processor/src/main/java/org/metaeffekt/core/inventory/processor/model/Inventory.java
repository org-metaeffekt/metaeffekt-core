/**
 * Copyright 2009-2019 the original author or authors.
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
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Class representing an inventory of artifact and license meta data. The implementation
 * offers various methods to analyze, optimize and utilize the meta data.
 *
 * @author Karsten Klein
 */
public class Inventory {

    public static final String CLASSIFICATION_CURRENT = "current";

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    private List<Artifact> artifacts = new ArrayList<>();

    private List<LicenseMetaData> licenseMetaData = new ArrayList<>();

    private List<ComponentPatternData> componentPatternData = new ArrayList<>();

    private List<VulnerabilityMetaData> vulnerabilityMetaData = new ArrayList<>();

    private Map<String, String> licenseNameMap = new HashMap<>();

    private Map<String, String> componentNameMap = new HashMap<>();

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

    public Artifact findArtifactByIdAndChecksum(String id, String checksum) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(checksum)) {
            return null;
        }
        String trimmedId = id.trim();
        String trimmedChecksum = checksum.trim();
        for (Artifact candidate : getArtifacts()) {
            if (candidate.getId() == null || candidate.getChecksum() == null) {
                continue;
            }
            if (trimmedId.equalsIgnoreCase(candidate.getId().trim())) {
                if (trimmedChecksum.equalsIgnoreCase(candidate.getChecksum().trim())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Artifact findArtifact(String id) {
        return findArtifact(id, false);
    }

    public Artifact findArtifact(String id, boolean matchWildcards) {
        if (id == null) return null;

        // 1st pass: check for exact match
        for (Artifact candidate : getArtifacts()) {
            if (id.equals(candidate.getId())) {
                return candidate;
            }
        }

        // 2nd pass: allow wildcard matches
        if (matchWildcards) {
            for (Artifact candidate : getArtifacts()) {
                if (matchesOnId(id, candidate)) {
                    return candidate;
                }
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
            // check the wildcard is really used in the candidate id
            final int index = candidateId.indexOf(ASTERISK);
            if (index != -1) {
                String prefix = candidateId.substring(0, index);
                String suffix = candidateId.substring(index + 1);
                if (id.startsWith(prefix) && id.endsWith(suffix)) {
                    return true;
                }
            }
        }

        // NOTE: in this case, no VERSION_PLACEHOLDER agnostic match is appropriate

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

            final LicenseMetaData matchingLicenseMetaData = findMatchingLicenseMetaData(artifact);
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

    public List<ArtifactLicenseData> evaluateComponents(String effectiveLicense) {
        final Map<String, ArtifactLicenseData> map = new LinkedHashMap<>();
        for (final Artifact artifact : artifacts) {
            String artifactLicense = artifact.getLicense();
            if (artifactLicense != null) {
                artifactLicense = artifactLicense.trim();
                // find a matching LMD instance
                LicenseMetaData match = findMatchingLicenseMetaData(
                        artifact.getComponent(), artifactLicense, artifact.getVersion());

                if (match == null) {
                    match = new LicenseMetaData();
                    match.setLicense(artifactLicense);
                }

                if (match != null && matches(effectiveLicense, match)) {
                    // only version and name must be used here
                    // there may be multiple entries (if validation for the component is disabled), but that
                    // is not of interest here (we need just representatives for documentation).
                    String qualifier = new StringBuilder(artifact.getVersion()).append("-").append(artifact.getComponent()).toString();
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
     * Iterates through the license metadata to find a match for the given component and license parameters.
     *
     * @param component The component.
     * @param license The license name.
     * @return A matching {@link LicenseMetaData} instance if available. In case multiple
     *         can be matched an {@link IllegalStateException} is thrown.
     */
    public LicenseMetaData findMatchingLicenseMetaData(String component, String license, String version) {
        LicenseMetaData match = null;
        for (LicenseMetaData lmd : licenseMetaData) {

            // NOTE: for artifacts with VERSION_PLACEHOLDER we expect that the license metadata is provided with ASTERISK.

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

    /**
     * Tries to find the matching {@link LicenseMetaData} details for the specified artifact.
     *
     * @param artifact The artifact to look for {@link LicenseMetaData}.
     * @return The found {@link LicenseMetaData} or <code>null</code> when no matching {@link LicenseMetaData} could be
     *      found.
     */
    public LicenseMetaData findMatchingLicenseMetaData(Artifact artifact) {
        return findMatchingLicenseMetaData(artifact.getComponent(), artifact.getLicense(), artifact.getVersion());
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
        }
        return null;
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
        licenseId = licenseId.replace("\"", "");
        licenseId = licenseId.replace("'", "");
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

        List<Component> sortedByComponent = new ArrayList<>(componentNames);
        Collections.sort(sortedByComponent, new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });
        return sortedByComponent;
    }

    public List<List<Artifact>> evaluateComponent(Component component, boolean includeLicensesWithArtifactsOnly) {
        List<Artifact> artifactsForComponent = new ArrayList<>();
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

        // rearrange components into groups of max 60
        List<List<Artifact>> groups = new ArrayList<>();
        List<Artifact> currentGroup = new ArrayList<>();
        groups.add(currentGroup);
        for (Artifact artifact : artifactsForComponent) {
            currentGroup.add(artifact);

            if (currentGroup.size() >= 60) {
                currentGroup = new ArrayList<>();
                groups.add(currentGroup);
            }
        }

        return groups;
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
        filteredInventory.setVulnerabilityMetaData(getVulnerabilityMetaData());
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

    /**
     * Takes over missing license metadata from the provided inputInventory. If the local inventory already has matching
     * license metadata the local data has priority.
     *
     * @param inputInventory The input inventory. From this inventory license metadata will be taken over.
     */
    public void inheritLicenseMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        // Iterate through all license meta data. Generate qualifier based on component name, version and license.
        // Test whether the qualifier is present in current. If yes skip; otherwise add.
        final Map<String, LicenseMetaData> currentLicenseMetaDataMap = new HashMap<>();
        for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            currentLicenseMetaDataMap.put(qualifier, licenseMetaData);
        }

        for (LicenseMetaData licenseMetaData : inputInventory.getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            if (currentLicenseMetaDataMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                if (infoOnOverwrite) {
                    LicenseMetaData currentLicenseMetaData = currentLicenseMetaDataMap.get(qualifier);
                    if (currentLicenseMetaData.createCompareStringRepresentation().equals(
                            licenseMetaData.createCompareStringRepresentation())) {
                        LOG.info("License meta data {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info("License meta data {} overwritten.", qualifier);
                    }
                }
            } else {
                // add the license meta data
                getLicenseMetaData().add(licenseMetaData);
            }
        }
    }

    public void filterLicenseMetaData() {
        Set<LicenseMetaData> filteredSet = new HashSet<>();
        for (Artifact artifact : getArtifacts()) {
            LicenseMetaData lmd = findMatchingLicenseMetaData(artifact);
            if (lmd != null) {
                filteredSet.add(lmd);
            }
        }
        getLicenseMetaData().retainAll(filteredSet);
    }



    /**
     * Inherits the artifacts from the specified inputInventory. Local artifacts with the same qualifier have
     * priority.
     *
     * @param inputInventory Input inventory with artifact information.
     * @param infoOnOverwrite Logs information on overwrites when active.
     */
    public void inheritArtifacts(Inventory inputInventory, boolean infoOnOverwrite) {
        // Iterate through all artifacts in the input repository. If the artifact is present in the current repository
        // then skip (but log some information); otherwise add the artifact to current.
        final Map<String, Artifact> currentArtifactMap = new HashMap<>();
        for (Artifact artifact : getArtifacts()) {
            artifact.deriveArtifactId();
            String artifactQualifier = artifact.deriveQualifier();
            currentArtifactMap.put(artifactQualifier, artifact);
        }
        for (Artifact artifact : inputInventory.getArtifacts()) {
            artifact.deriveArtifactId();
            String qualifier = artifact.deriveQualifier();
            if (currentArtifactMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                if (infoOnOverwrite) {
                    Artifact currentArtifact = currentArtifactMap.get(qualifier);
                    if (artifact.createCompareStringRepresentation().equals(
                            currentArtifact.createCompareStringRepresentation())) {
                        LOG.info("Artifact {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Artifact %s overwritten. %n  %s%n  %s", qualifier,
                            artifact.createCompareStringRepresentation(),
                            currentArtifact.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getArtifacts().add(artifact);
            }
        }
    }

    public void inheritComponentPatterns(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, ComponentPatternData> localCpds = new HashMap<>();
        for (ComponentPatternData cpd : getComponentPatternData()) {
            String artifactQualifier = cpd.deriveQualifier();
            localCpds.put(artifactQualifier, cpd);
        }
        for (ComponentPatternData cpd : inputInventory.getComponentPatternData()) {
            String qualifier = cpd.deriveQualifier();
            if (localCpds.containsKey(qualifier)) {
                // overwrite; the localCpds inventory contains the artifact.
                if (infoOnOverwrite) {
                    ComponentPatternData localCpd = localCpds.get(qualifier);
                    if (cpd.createCompareStringRepresentation().equals(
                            localCpd.createCompareStringRepresentation())) {
                        LOG.info("Component pattern {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Component pattern %s overwritten. %n  %s%n  %s", qualifier,
                                cpd.createCompareStringRepresentation(),
                                localCpd.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getComponentPatternData().add(cpd);
            }
        }
    }

    public void inheritVulnerabilityMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, VulnerabilityMetaData> localVmds = new HashMap<>();
        for (VulnerabilityMetaData vmd : getVulnerabilityMetaData()) {
            String artifactQualifier = vmd.deriveQualifier();
            localVmds.put(artifactQualifier, vmd);
        }
        for (VulnerabilityMetaData vmd : inputInventory.getVulnerabilityMetaData()) {
            String qualifier = vmd.deriveQualifier();
            if (localVmds.containsKey(qualifier)) {
                // overwrite; the localVmds inventory contains the artifact.
                if (infoOnOverwrite) {
                    VulnerabilityMetaData localVmd = localVmds.get(qualifier);
                    if (vmd.createCompareStringRepresentation().equals(
                            localVmd.createCompareStringRepresentation())) {
                        LOG.info("Vulnerability metadata {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Vulnerability metadata %s overwritten. %n  %s%n  %s", qualifier,
                                vmd.createCompareStringRepresentation(),
                                localVmd.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getVulnerabilityMetaData().add(vmd);
            }
        }
    }

    public void filterVulnerabilityMetaData() {
        Set<String> coveredVulnerabilityIds = new HashSet<>();
        for (Artifact artifact : artifacts) {
            String v = artifact.getVulnerability();
            splitCommaSeparated(v).stream().
                    map(this::toPlainCVE).
                    filter(Objects::nonNull).
                    forEach(cve -> coveredVulnerabilityIds.add(cve));
        }
        LOG.debug("Covered vulnerabilities: {}", coveredVulnerabilityIds);
        List<VulnerabilityMetaData> forDeletion = new ArrayList<>();
        for (VulnerabilityMetaData vmd : getVulnerabilityMetaData()) {
            if (!coveredVulnerabilityIds.contains(vmd.get(VulnerabilityMetaData.Attribute.NAME))) {
                forDeletion.add(vmd);
            }
        }
        LOG.debug("Removing vulnerability metadata for: {}",
            forDeletion.stream().map(v -> v.get(VulnerabilityMetaData.Attribute.NAME)).collect(Collectors.joining(", ")));
        getVulnerabilityMetaData().removeAll(forDeletion);
    }

    public void setComponentPatternData(List<ComponentPatternData> componentPatternData) {
        this.componentPatternData = componentPatternData;
    }

    public List<ComponentPatternData> getComponentPatternData() {
        return this.componentPatternData;
    }

    public void setVulnerabilityMetaData(List<VulnerabilityMetaData> vulnerabilityMetaData) {
        this.vulnerabilityMetaData = vulnerabilityMetaData;
    }

    public List<VulnerabilityMetaData> getVulnerabilityMetaData() {
        return vulnerabilityMetaData;
    }

    public List<VulnerabilityMetaData> getApplicableVulnerabilityMetaData(float threshold) {
        return VulnerabilityMetaData.filterApplicableVulnerabilities(getVulnerabilityMetaData(), threshold);
    }

    public List<VulnerabilityMetaData> getNotApplicableVulnerabilityMetaData(float threshold) {
        return VulnerabilityMetaData.filterNotApplicableVulnerabilities(getVulnerabilityMetaData(), threshold);
    }

    public List<VulnerabilityMetaData> getInsignificantVulnerabilities(float threshold) {
        return VulnerabilityMetaData.filterInsignificantVulnerabilities(getVulnerabilityMetaData(), threshold);
    }



    // helpers

    private Set<String> splitCommaSeparated(String string) {
        if (string == null) return Collections.EMPTY_SET;
        return Arrays.stream(string.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private String toPlainCVE(String cve) {
        if (StringUtils.isEmpty(cve)) return null;
        int index = cve.indexOf(" ");
        return index == -1 ? cve : cve.substring(0, index);
    }

}
