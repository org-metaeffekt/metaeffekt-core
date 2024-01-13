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
import org.metaeffekt.core.inventory.InventoryUtils;
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

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.AssetMetaData.Attribute.ASSET_ID;
import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.ATTRIBUTE_KEY_ASSET_PATH;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_CHECKSUM;

/**
 * General JAR-level {@link ArtifactInspector}. The inspector uses common resources (maven poms, osgi manifests,
 * java manifests) to parse information for identifying the artifact.
 */
public class JarInspector extends AbstractJarInspector {

    private static final Logger LOG = LoggerFactory.getLogger(JarInspector.class);

    public static final String ATTRIBUTE_KEY_ARTIFACT_ID = "ARTIFACT_ID";
    public static final String ATTRIBUTE_KEY_EMBEDDED_PATH = Constants.KEY_PATH_IN_ASSET;
    public static final String ATTRIBUTE_KEY_QUALIFIER_WITH_VERSION = "QUALIFIER_WITH_VERSION";
    public static final String ATTRIBUTE_KEY_QUALIFIER_NO_VERSION = "QUALIFIER_NO_VERSION";

    protected boolean hasFilename(String normalizedPath, String fileName) {
        // zips must always use / as a path separator so this check should be correct to only use slash.
        return normalizedPath.equals(fileName) ||
                normalizedPath.endsWith("/" + fileName);
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

    public Artifact getArtifactFromPomProperties(Artifact artifact, InputStream inputStream, String embeddedPath) {
        final Properties pomProperties = new Properties();
        try {
            pomProperties.load(inputStream);
        } catch (IOException e) {
            addError(artifact, "Error while loading 'pom.properties'.");
        }

        final Artifact dummyArtifact = new Artifact();
        dummyArtifact.setGroupId(pomProperties.getProperty("groupId", null));
        dummyArtifact.setVersion(pomProperties.getProperty("version", null));

        dummyArtifact.set(ATTRIBUTE_KEY_ARTIFACT_ID, pomProperties.getProperty("artifactId", null));
        dummyArtifact.set(ATTRIBUTE_KEY_EMBEDDED_PATH, deriveEmbeddedPath(artifact, embeddedPath));

        deriveQualifiers(dummyArtifact);

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    protected Artifact getArtifactFromManifest(Artifact artifact, InputStream inputStream, String embeddedPath) {
        Manifest manifest = null;
        try {
            manifest = new Manifest(inputStream);
        } catch (IOException e) {
            addError(artifact, "Error while parsing [" + embeddedPath + "].");
        }

        if (manifest != null) {
            final Attributes mainAttributes = manifest.getMainAttributes();

            final Artifact dummyArtifact = new Artifact();
            dummyArtifact.setId(artifact.getId());
            String version = mainAttributes.getValue("Implementation-Version");

            // lucene manifest contains "8.11.2 17dee71932c683e345508113523e764c3e4c80f ..."
            if (version != null) {
                version = version.trim();
                int whiteSpaceIndex = version.indexOf(" ");
                if (whiteSpaceIndex > 0) {
                    version = version.substring(0, whiteSpaceIndex);
                }
            }

            dummyArtifact.setVersion(version);

            dummyArtifact.set(Constants.KEY_ORGANIZATION, mainAttributes.getValue("Implementation-Vendor"));
            dummyArtifact.set("Organization Id", mainAttributes.getValue("Implementation-Vendor-Id"));

            if (StringUtils.isBlank(dummyArtifact.getVersion())) {
                dummyArtifact.setVersion(mainAttributes.getValue("Bundle-Version"));
            }

            dummyArtifact.set(ATTRIBUTE_KEY_EMBEDDED_PATH, deriveEmbeddedPath(artifact, embeddedPath));

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
            dummyArtifact.set(ATTRIBUTE_KEY_ARTIFACT_ID, artifactId);

            deriveQualifiers(dummyArtifact);

            return dummyArtifact;
        }
        return null;
    }

    private static String deriveEmbeddedPath(Artifact artifact, String pathInsideArtifact) {
        String path = artifact.get(ATTRIBUTE_KEY_ARTIFACT_PATH);
        if (path != null) {
            File file = new File(path);
            file = new File(file.getParentFile(), "[" + artifact.getId() + "]");
            return new File(file, pathInsideArtifact).getPath();
        }
        return null;
    }

    public Artifact getArtifactFromPomXml(Artifact artifact, InputStream inputStream, String embeddedPath) {
        Artifact dummyArtifact = new Artifact();

        // parse pom
        try {
            final Model model = new MavenXpp3Reader().read(inputStream);

            // grab artifactId, groupId and version from pom. get from parent section if not filled
            if (model.getArtifactId() != null) {
                dummyArtifact.set(ATTRIBUTE_KEY_ARTIFACT_ID, model.getArtifactId());
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

            dummyArtifact.set("Packaging", packagingToSuffix(model));

            // NOTE: the information may not be part of the pom, but provided in the parent pom. However, if the
            // information is available, it is included in the artifact
            if (model.getOrganization() != null) {
                dummyArtifact.set(Constants.KEY_ORGANIZATION, model.getOrganization().getName());
                dummyArtifact.set(Constants.KEY_ORGANIZATION_URL, model.getOrganization().getUrl());
            }

            dummyArtifact.set(ATTRIBUTE_KEY_EMBEDDED_PATH, deriveEmbeddedPath(artifact, embeddedPath));

            // NOTE: the current mode is identification. POM specified licenses are not subject to identification
            // Furthermore, the leaf-pom may not include license information.

            deriveQualifiers(dummyArtifact);
        } catch (IOException | XmlPullParserException e) {
            addError(artifact, "Exception while parsing a 'pom.xml'.");
        }

        return dummyArtifact;
    }

    private String packagingToSuffix(Model model) {
        if (model.getPackaging() == null) return "jar";
        switch (model.getPackaging()) {
            case "pom":
                return "pom";
            default:
                return "jar";
        }
    }

    private void deriveQualifiers(Artifact dummyArtifact) {
        final String version = dummyArtifact.getVersion();
        final String artifactId = dummyArtifact.get(ATTRIBUTE_KEY_ARTIFACT_ID);
        final String qualifierWithVersion = artifactId + "-" + version;
        dummyArtifact.set(ATTRIBUTE_KEY_QUALIFIER_WITH_VERSION, qualifierWithVersion);
        dummyArtifact.set(ATTRIBUTE_KEY_QUALIFIER_NO_VERSION, artifactId);
    }

    protected Artifact dummyArtifactFromPomProperties(Artifact artifact, ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            return getArtifactFromPomProperties(artifact, inputStream, pomEntry.getName());
        } catch (IOException e) {
            addError(artifact, "IOException while reading [" + pomEntry.getName() + "].");
        }
        return null;
    }

    protected Artifact dummyArtifactFromPomXml(Artifact artifact, ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            return getArtifactFromPomXml(artifact, inputStream, pomEntry.getName());
        } catch (IOException e) {
            addError(artifact, "IOException while reading [" + pomEntry.getName() + "].");
        }
        return null;
    }

    protected Artifact dummyArtifactFromManifest(Artifact artifact, ZipFile zipFile, ZipArchiveEntry manifestEntry) {
        try (InputStream inputStream = zipFile.getInputStream(manifestEntry)) {
            return getArtifactFromManifest(artifact, inputStream, manifestEntry.getName());
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
            String qualifier = candidate.get(ATTRIBUTE_KEY_QUALIFIER_WITH_VERSION);
            if (qualifier != null) {
                String qualifierNoVersion = candidate.get(ATTRIBUTE_KEY_QUALIFIER_NO_VERSION);
                Artifact collector = qualifierArtifactMap.get(qualifier);

                if (collector == null) {
                    qualifierArtifactMap.put(qualifier, candidate);
                    qualifierArtifactMap.put(qualifierNoVersion, candidate);
                } else {
                    final File embeddedFileCandidate = getEmbeddedPath(candidate);
                    final File embeddedFileCollector = getEmbeddedPath(collector);

                    collector.merge(candidate);

                    // this condition is specific to the environment; the embedded path may not be known to the artifact
                    if (embeddedFileCandidate == null || embeddedFileCollector == null) continue;

                    // pom.xml wins over all
                    if (embeddedFileCandidate.getName().endsWith("pom.xml")) {
                        collector.set(ATTRIBUTE_KEY_EMBEDDED_PATH, embeddedFileCandidate.getPath());
                    }

                    // pom.properties wins over MANIFEST.MF
                    if (embeddedFileCandidate.getName().endsWith("pom.properties")) {
                        if (embeddedFileCollector.getName().endsWith("MANIFEST.MF")) {
                            collector.set(ATTRIBUTE_KEY_EMBEDDED_PATH, embeddedFileCandidate.getPath());
                        }
                    }

                }
            }
        }

        for (Artifact candidate : manifestArtifacts) {
            if (candidate.getId() != null && candidate.getVersion() != null) {
                String qualifier = candidate.get(ATTRIBUTE_KEY_QUALIFIER_WITH_VERSION);
                String qualifierNoVersion = candidate.get(ATTRIBUTE_KEY_QUALIFIER_NO_VERSION);
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
        final String embeddedPath = a.get(ATTRIBUTE_KEY_EMBEDDED_PATH);
        if (StringUtils.isNotBlank(embeddedPath)) {
            return new File(embeddedPath);
        }
        return null;
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
        //  - either the filename matches "<artifactId>-<version>-<classifier(s)>.jar
        //  - or it matches               "<artifactId>-<version>.jar"
        //  - or it matches               "<artifactId>.jar"
        //  - or it matches               "<groupId>.<artifactId>-<version>.jar"
        // keep these definitions strict to not produce "weird" data.

        // check id versus filename
        if (dummyArtifact.getId() != null && dummyArtifact.get("ARTIFACT_ID") == null) {
            if (dummyArtifact.getId().equals(fileName)) {
                return true;
            }
        }

        final Pattern pattern1 = Pattern.compile(
                ".*[\\.]{0,1}" +    // filter groupid or any other prefix; <artifactId>-<version> is enought evidence
                Pattern.quote(dummyArtifact.get(ATTRIBUTE_KEY_ARTIFACT_ID) + "-" + dummyArtifact.getVersion()) +
                "[-\\._].*");
        if (pattern1.matcher(fileName).matches()) {
            return true;
        }

        final String match2 = dummyArtifact.get(ATTRIBUTE_KEY_ARTIFACT_ID) + "-" + dummyArtifact.getVersion() + ".";
        final String match3 = dummyArtifact.get(ATTRIBUTE_KEY_ARTIFACT_ID) + ".";

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
            String currentArtifactId = checking.get(ATTRIBUTE_KEY_ARTIFACT_ID);
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

        return foundGroupIds.size() > 1 || foundVersions.size() > 1;
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

        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ARTIFACT_ID, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_QUALIFIER_NO_VERSION, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_QUALIFIER_WITH_VERSION, inventory);

        InventoryUtils.removeAssetAttribute(ATTRIBUTE_KEY_ARTIFACT_PATH, inventory);
        InventoryUtils.removeAssetAttribute(ATTRIBUTE_KEY_INSPECTION_SOURCE, inventory);
    }

    // TODO: special steps to take for artifacts added in this fashion? maybe write a separate Inspector for this?
    private void includeEmbedded(Inventory inventory, Artifact containingArtifact,
             List<Artifact> embeddedArtifacts, Set<String> alreadyReported) {

        if (embeddedArtifacts != null && !embeddedArtifacts.isEmpty()) {
            final String assetId = "AID-" + containingArtifact.getId() + "-" + containingArtifact.getChecksum();

            // manage assetIdChain and artifact path
            String parentAssetIdChain = containingArtifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);
            String parentArtifactPath = containingArtifact.get(ATTRIBUTE_KEY_ARTIFACT_PATH);

            final String foundAssetIdChain = deriveAssetIdChain(parentAssetIdChain, parentArtifactPath);

            // construct asset metadata for containing artifacts (e.g. shaded jars)
            final AssetMetaData assetMetaData = new AssetMetaData();
            assetMetaData.set(ASSET_ID, assetId);
            assetMetaData.set(ATTRIBUTE_KEY_ASSET_PATH, parentArtifactPath);
            assetMetaData.set(KEY_CHECKSUM, containingArtifact.getChecksum());
            assetMetaData.set(ATTRIBUTE_KEY_ARTIFACT_PATH, parentArtifactPath);
            assetMetaData.set(ATTRIBUTE_KEY_INSPECTION_SOURCE, JarInspector.class.getName());
            inventory.getAssetMetaData().add(assetMetaData);

            for (final Artifact embeddedArtifact : embeddedArtifacts) {
                // supplement default id; FIXME: move to identification code; here only consumer
                if (StringUtils.isBlank(embeddedArtifact.getId())) {
                    embeddedArtifact.setId(embeddedArtifact.get(ATTRIBUTE_KEY_ARTIFACT_ID) + "-" +
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
                embeddedArtifact.set(assetId, Constants.MARKER_CONTAINS);

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
