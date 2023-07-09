/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.inspector;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * General JAR-level {@link ArtifactInspector}. The inspector uses common resources (maven poms, osgi manifests,
 * java manifests) to parse information for identifying the artifact.
 */
public class JarInspector extends AbstractJarInspector {

    private static final Logger LOG = LoggerFactory.getLogger(JarInspector.class);

    protected boolean hasFilename(String normalizedPath, String fileName) {
        // zips must always use / as a path separator so this check should be correct to only use slash.
        return normalizedPath.equals(fileName) || normalizedPath.endsWith("/" + fileName);
    }

    protected boolean isPomProperties(String path) {
        return hasFilename(path, "pom.properties");
    }

    protected boolean isPomXml(String path) {
        return hasFilename(path, "pom.xml");
    }

    protected boolean isManifest(String path) {
        return hasFilename(path, "MANIFEST.MF");
    }

    protected String normalizePath(String path) {
        final String normalizedPath = FileUtils.normalizePathToLinux(FilenameUtils.normalize(path));
        if (normalizedPath == null) {
            // erroneous path.
            throw new IllegalArgumentException("No allowed archive path. Might contain a path traversal attack?");
        }
        return normalizedPath;
    }

    /**
     * For a valid identification we need at least basic attributes.
     *
     * @param artifact The artifact to check.
     * @return Whether the artifact (in parts) contains information for identification.
     */
    protected boolean importantNonNull(Artifact artifact) {
        return artifact.getGroupId() != null || artifact.getVersion() != null;
    }

    protected Artifact getArtifactFromPomProperties(Artifact artifact, InputStream inputStream, ZipArchiveEntry pomEntry) {
        final Properties pomProperties = new Properties();
        try {
            pomProperties.load(inputStream);
        } catch (IOException e) {
            addError(artifact, "Error while loading 'pom.properties'.");
        }

        final Artifact dummyArtifact = new Artifact();
        dummyArtifact.setGroupId(pomProperties.getProperty("groupId", null));
        dummyArtifact.setVersion(pomProperties.getProperty("version", null));

        dummyArtifact.set("ARTIFACT_ID", pomProperties.getProperty("artifactId", null));
        dummyArtifact.set("EMBEDDED_PATH", pomEntry.getName());

        deriveQualifiers(dummyArtifact);

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    protected Artifact getArtifactFromManifest(Artifact artifact, InputStream inputStream, ZipArchiveEntry manifestEntry) {
        Manifest manifest = null;
        try {
            manifest = new Manifest(inputStream);
        } catch (IOException e) {
            addError(artifact, "Error while loading 'pom.properties'.");
        }

        if (manifest != null) {
            final Attributes mainAttributes = manifest.getMainAttributes();

            final Artifact dummyArtifact = new Artifact();
            dummyArtifact.setId(artifact.getId());
            dummyArtifact.setVersion(mainAttributes.getValue("Implementation-Version"));

            dummyArtifact.set(Constants.KEY_ORGANIZATION, mainAttributes.getValue("Implementation-Vendor"));
            dummyArtifact.set("Organization Id", mainAttributes.getValue("Implementation-Vendor-Id"));

            if (StringUtils.isBlank(dummyArtifact.getVersion())) {
                dummyArtifact.setVersion(mainAttributes.getValue("Bundle-Version"));
            }

            dummyArtifact.set("EMBEDDED_PATH", manifestEntry.getName());

            String artifactId = artifact.getId();
            // cut off suffix
            final int suffixIndex = artifactId.lastIndexOf(".");
            if (suffixIndex > 0) {
                artifactId = artifactId.substring(0, suffixIndex);
            }
            final int versionIndex = artifactId.lastIndexOf("-" + dummyArtifact.getVersion());
            if (versionIndex > 0) {
                artifactId = artifactId.substring(0, versionIndex);
            }
            dummyArtifact.set("ARTIFACT_ID", artifactId);

            deriveQualifiers(dummyArtifact);

            return dummyArtifact;
        }
        return null;
    }

    protected Artifact getArtifactFromPomXml(Artifact artifact, InputStream inputStream, ZipArchiveEntry pomEntry) {
        Artifact dummyArtifact = new Artifact();

        // parse pom
        try {
            final Model model = new MavenXpp3Reader().read(inputStream);

            // grab artifactId, groupId and version from pom. get from parent section if not filled
            if (model.getArtifactId() != null) {
                dummyArtifact.set("ARTIFACT_ID", model.getArtifactId());
            }

            if (model.getGroupId() != null) {
                dummyArtifact.setGroupId(model.getGroupId());
            } else {
                dummyArtifact.setGroupId(model.getParent().getGroupId());
            }

            if (model.getVersion() != null) {
                dummyArtifact.setVersion(model.getVersion());
            } else {
                dummyArtifact.setVersion(model.getParent().getVersion());
            }

            // NOTE: the information may not be part of the pom, but provided in the parent pom. However, if the
            // information is available, it is included in the artifact
            if (model.getOrganization() != null) {
                dummyArtifact.set(Constants.KEY_ORGANIZATION, model.getOrganization().getName());
                dummyArtifact.set(Constants.KEY_ORGANIZATION_URL, model.getOrganization().getUrl());
            }

            dummyArtifact.set("EMBEDDED_PATH", pomEntry.getName());

            // NOTE: the current mode is identification. POM specified licenses are not subject to identification
            // Furthermore, the leaf-pom may not include license information.

            deriveQualifiers(dummyArtifact);
        } catch (IOException | XmlPullParserException e) {
            addError(artifact, "Exception while parsing a 'pom.xml'.");
        }

        return dummyArtifact;
    }

    private void deriveQualifiers(Artifact dummyArtifact) {
        final String version = dummyArtifact.getVersion();
        final String artifactId = dummyArtifact.get("ARTIFACT_ID");
        final String qualifierWithVersion = artifactId + "-" + version;
        dummyArtifact.set("QUALIFIER_WITH_VERSION", qualifierWithVersion);
        final String qualifierNoVersion = artifactId;
        dummyArtifact.set("QUALIFIER_NO_VERSION", qualifierNoVersion);
    }

    protected Artifact dummyArtifactFromPomProperties(Artifact artifact, ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            return getArtifactFromPomProperties(artifact, inputStream, pomEntry);
        } catch (IOException e) {
            addError(artifact, "IOException while reading [" + pomEntry.getName() + "].");
        }
        return null;
    }

    protected Artifact dummyArtifactFromPomXml(Artifact artifact, ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            return getArtifactFromPomXml(artifact, inputStream, pomEntry);
        } catch (IOException e) {
            addError(artifact, "IOException while reading [" + pomEntry.getName() + "].");
        }
        return null;
    }

    protected Artifact dummyArtifactFromManifest(Artifact artifact, ZipFile zipFile, ZipArchiveEntry manifestEntry) {
        try (InputStream inputStream = zipFile.getInputStream(manifestEntry)) {
            return getArtifactFromManifest(artifact, inputStream, manifestEntry);
        } catch (IOException e) {
            addError(artifact, "IOException while reading [" + manifestEntry.getName() + "].");
        }
        return null;
    }

    /**
     * Iterates throw the entries in the jar file and produced an artifact for every pom.properties or pom.xml file.
     *
     * @param artifact The artifact carrying data.
     * @param jarFile The file being inspected.
     *
     * @return List of artifacts created from pom.properties or pom.xml entries in the jar file.
     */
    protected List<Artifact> collectArtifactCandidates(Artifact artifact, File jarFile) {
        final List<Artifact> artifacts = new ArrayList<>();

        // manifest derived information is only used as fallback
        final List<Artifact> manifestArtifacts = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(jarFile)) {
            final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                final ZipArchiveEntry entry = entries.nextElement();
                final String path = normalizePath(entry.getName());
                if (isPomProperties(path)) {
                    artifacts.add(dummyArtifactFromPomProperties(artifact, zipFile, entry));
                } else if (isPomXml(path)) {
                    artifacts.add(dummyArtifactFromPomXml(artifact, zipFile, entry));
                } else if (isManifest(path)) {
                    manifestArtifacts.add(dummyArtifactFromManifest(artifact, zipFile, entry));
                }
            }
        } catch (IOException e) {
            // ignore
        }

        // FIXME: option to filter/merge duplicates sharing the same id/version combination
        //  Specifically, artifacts from pom.xml and pom.properties should be combined to the xml variant

        // strategy: combine artifacts originating from the same id
        final Map<String, Artifact> qualifierArtifactMap = new HashMap<>();

        for (Artifact candidate : artifacts) {
            String qualifier = candidate.get("QUALIFIER_WITH_VERSION");
            if (qualifier != null) {
                String qualifierNoVersion = candidate.get("QUALIFIER_NO_VERSION");
                Artifact collector = qualifierArtifactMap.get(qualifier);

                if (collector == null) {
                    qualifierArtifactMap.put(qualifier, candidate);
                    qualifierArtifactMap.put(qualifierNoVersion, candidate);
                } else {
                    final File embeddedFileCandidate = getEmbeddedPath(candidate);
                    final File embeddedFileCollector = getEmbeddedPath(collector);

                    collector.merge(candidate);

                    // pom.xml wins over all
                    if (embeddedFileCandidate.getName().endsWith("pom.xml")) {
                        collector.set("EMBEDDED_PATH", embeddedFileCandidate.getPath());
                    }

                    // pom.properties wins over MANIFEST.MF
                    if (embeddedFileCandidate.getName().endsWith("pom.properties")) {
                        if (embeddedFileCollector.getName().endsWith("MANIFEST.MF")) {
                            collector.set("EMBEDDED_PATH", embeddedFileCandidate.getPath());
                        }
                    }

                }
            }
        }

        for (Artifact candidate : manifestArtifacts) {
            if (candidate.getId() != null && candidate.getVersion() != null) {
                String qualifier = candidate.get("QUALIFIER_WITH_VERSION");
                String qualifierNoVersion = candidate.get("QUALIFIER_NO_VERSION");
                Artifact collectorWithVersion = qualifierArtifactMap.get(qualifier);
                Artifact collectorNoVersion = qualifierArtifactMap.get(qualifierNoVersion);

                if (collectorWithVersion != null || collectorNoVersion != null) {
                    // already covered; do nothing
                } else {
                     qualifierArtifactMap.put(qualifier, candidate);
                    qualifierArtifactMap.put(qualifierNoVersion, candidate);
                }
            }
        }

        return new ArrayList<>(qualifierArtifactMap.values());
    }

    private static File getEmbeddedPath(Artifact a) {
        String embeddedPath = a.get("EMBEDDED_PATH");
        File embeddedFile = new File(embeddedPath);
        return embeddedFile;
    }

    /**
     * Check if the information in the dummy artifact matches the given fileName.<br>
     * @param fileName The file's name to check against.
     * @param dummyArtifact Dummy artifact containing the id and version that will be checked.
     * @return Returns whether the dummy artifact's data directly relates to the filename.
     */
    protected boolean matchesFileName(String fileName, Artifact dummyArtifact) {
        // match against filename:
        //  strict matching. we find artifact ids and versions.
        //  we check and apply only when one of the following checks succeed:
        //  - either the filename matches "<artifactID>-<version>-<classifier(s)>.jar
        //  - or it matches               "<artifactID>-<version>.jar"
        //  - or it matches               "<artifactID>.jar"
        // keep these definitions strict to not produce "weird" data.

        if (dummyArtifact.getId() != null && dummyArtifact.get("ARTIFACT_ID") == null) {
            if (dummyArtifact.getId().equals(fileName)) {
                return true;
            }
        }

        final Pattern pattern1 = Pattern.compile(Pattern.quote(dummyArtifact.get("ARTIFACT_ID") +
                "-" + dummyArtifact.getVersion()) +
                "[-\\.].*");
        if (pattern1.matcher(fileName).matches()) {
            return true;
        }

        final String match2 = dummyArtifact.get("ARTIFACT_ID") + "-" + dummyArtifact.getVersion() + ".";
        final String match3 = dummyArtifact.get("ARTIFACT_ID") + ".";

        // otherwise throw the towel. no idea what this jar is supposed to be.
        return fileName.startsWith(match2) || fileName.startsWith(match3);
    }

    protected boolean conflictsWithOriginal(Artifact artifact, Collection<Artifact> toCheck) {
        for (final Artifact checking : toCheck) {
            final String groupId = artifact.getGroupId();
            if (StringUtils.isNotBlank(checking.getGroupId())) {
                if (StringUtils.isNotBlank(groupId) && !groupId.equals(checking.getGroupId())) {
                    return true;
                }
            }
            final String version = artifact.getVersion();
            if (StringUtils.isNotBlank(checking.getVersion())) {
                if (StringUtils.isNotBlank(version) && (!version.equals(checking.getVersion()))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean conflictsWithEachOther(Collection<Artifact> toCheck) {
        Set<String> foundArtifactIds = new HashSet<>();
        Set<String> foundGroupIds = new HashSet<>();
        Set<String> foundVersions = new HashSet<>();

        for (Artifact checking : toCheck) {
            String currentArtifactId = checking.get("ARTIFACT_ID");
            String currentGroupId = checking.getGroupId();
            String currentVersion = checking.getVersion();

            if (StringUtils.isNotBlank(currentArtifactId)) {
                foundArtifactIds.add(currentArtifactId);
            }
            if (StringUtils.isNotBlank(currentGroupId)) {
                foundGroupIds.add(currentGroupId);
            }
            if (StringUtils.isNotBlank(currentVersion)) {
                foundVersions.add(currentVersion);
            }
        }

        return foundArtifactIds.size() > 1 || foundGroupIds.size() > 1 || foundVersions.size() > 1;
    }

    private List<Artifact> processArtifact(Artifact artifact, ProjectPathParam projectPathParam) {
        final File jarFile = getJarFile(artifact, projectPathParam);

        if (jarFile == null) {
            // early exit: nothing to scan
            return null;
        }

        List<Artifact> dummyArtifacts = collectArtifactCandidates(artifact, jarFile).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // enforce all of artifactid, version and groupid being non-null for filling to kick in
        final List<Artifact> accepted = new ArrayList<>();
        final List<Artifact> notAccepted = new ArrayList<>();
        for (Artifact dummyArtifact : dummyArtifacts) {
            if (importantNonNull(dummyArtifact)) {
                if (matchesFileName(jarFile.getName(), dummyArtifact)) {
                    accepted.add(dummyArtifact);
                } else {
                    notAccepted.add(dummyArtifact);
                }
            }
        }

        // process list of accepted dummies, detect disagreements (with original state and other dummies)
        boolean conflictsWithOriginal = conflictsWithOriginal(artifact, accepted);
        boolean conflictsWithEachOther = conflictsWithEachOther(accepted);
        if (conflictsWithOriginal) {
            addError(artifact, "Detected information shows conflicts with original artifact.");
        }
        if (conflictsWithEachOther) {
            addError(artifact, "Detected information conflicts with each other.");
        }

        if (accepted.size() > 0) {
            // on match (accepted and no conflicts): insert info into artifact
            if (!conflictsWithOriginal && !conflictsWithEachOther) {
                for (final Artifact newData : accepted) {

                    // copy all attributes
                    for (String attribute : newData.getAttributes()) {
                        // take over attributes from matched dummy without overwriting
                        if (StringUtils.isBlank(artifact.get(attribute))) {
                            artifact.set(attribute, newData.get(attribute));
                        }
                    }
                }
            }
        }
        // otherwise: no pom found; ignore

        return notAccepted;
    }

    @Override
    public void run(Inventory inventory, Properties properties) {
        // get params
        final ProjectPathParam projectPathParam = new ProjectPathParam(properties);
        final JarInspectionParam jarInspectionParam = new JarInspectionParam(properties);

        // execute

        // set to record artifact ids; used to only log errors once
        Set<String> alreadyReported = new HashSet<>();

        // we iterate a cloned list to avoid concurrent modification issues
        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
            try {
                final List<Artifact> notAccepted = processArtifact(artifact, projectPathParam);

                if (jarInspectionParam.isIncludeEmbedded()) {
                    includeEmbedded(inventory, artifact, notAccepted, alreadyReported);
                }

                // mainly to satisfy the tests
                artifact.setArtifactId(null);
                artifact.deriveArtifactId();
            } catch (Exception e) {
                // log error and carry on
                addError(artifact, "Error while running " + this.getClass().getSimpleName());
                LOG.error("Error while running "
                        + this.getClass().getSimpleName()
                        + " on artifact '" + artifact + "':"
                        + e.getMessage());
            }
        }
    }

    // TODO: special steps to take for artifacts added in this fashion? maybe write a separate Inspector for this?
    private void includeEmbedded(Inventory inventory, Artifact containingArtifact,
             List<Artifact> embeddedArtifacts, Set<String> alreadyReported) {

        if (embeddedArtifacts != null && !embeddedArtifacts.isEmpty()) {
            final String assetId = "AID-" + containingArtifact.getId() + "-" + containingArtifact.getChecksum();

            // manage assetIdChain and artifact path
            String parentAssetIdChain = containingArtifact.get("ASSET_ID_CHAIN");
            String parentArtifactPath = containingArtifact.get("ARTIFACT PATH");
            final String foundAssetIdChain = deriveAssetIdChain(parentAssetIdChain, parentArtifactPath);

            // construct asset metadata for containing artifacts (e.g. shaded jars)
            final AssetMetaData assetMetaData = new AssetMetaData();
            assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, assetId);
            assetMetaData.set("File Path", parentArtifactPath);
            assetMetaData.set("Checksum", containingArtifact.getChecksum());
            assetMetaData.set("ARTIFACT_PATH", parentArtifactPath);
            assetMetaData.set("Source", JarInspector.class.getName());
            inventory.getAssetMetaData().add(assetMetaData);

            for (final Artifact embeddedArtifact : embeddedArtifacts) {
                // supplement default id; FIXME: move to identification code; here only consumer
                if (StringUtils.isBlank(embeddedArtifact.getId())) {
                    embeddedArtifact.setId(embeddedArtifact.get("ARTIFACT_ID") + "-" +
                            embeddedArtifact.getVersion() + "." + "jar");
                }

                // remove artifacts that cannot be fully identified
                final String embeddedArtifactId = embeddedArtifact.getId();

                // check for artifactId and placeholders
                if (embeddedArtifactId != null && embeddedArtifactId.contains("${")) {
                    if (!alreadyReported.contains(embeddedArtifactId)) {
                        LOG.warn("Skipping embedded artifact without fully qualified artifact id: {}", embeddedArtifactId);
                        alreadyReported.add(embeddedArtifactId);
                    }
                    continue;
                }

                // FIXME: this should not be required here
                embeddedArtifact.set(assetId, "x");

                if (StringUtils.isNotBlank(foundAssetIdChain)) {
                    embeddedArtifact.set("ASSET_ID_CHAIN", foundAssetIdChain);
                }

                // NOTE: we add whether there is already an artifact existing. Later a merge can take
                //  care of multiple attributes.
                inventory.getArtifacts().add(embeddedArtifact);
            }
        }
    }

    private static String deriveAssetIdChain(String parentAssetIdChain, String parentArtifactPath) {
        String foundAssetIdChain = "";
        if (StringUtils.isNotBlank(parentAssetIdChain)) {
            foundAssetIdChain = parentAssetIdChain;
        }
        if (StringUtils.isNotBlank(parentArtifactPath)) {
            if (StringUtils.isNotBlank(foundAssetIdChain)) {
                foundAssetIdChain += "|\n";
            }
            foundAssetIdChain += parentArtifactPath;
        }
        return foundAssetIdChain;
    }

}
