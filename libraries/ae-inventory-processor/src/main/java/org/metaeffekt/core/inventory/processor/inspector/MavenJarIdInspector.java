/*
 * Copyright 2022 the original author or authors.
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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MavenJarIdInspector extends AbstractJarInspector {

    private static final Logger LOG = LoggerFactory.getLogger(MavenJarIdInspector.class);


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

    protected boolean isPomMeta(String path) {
        String normalizedPath = FilenameUtils.normalize(path);

        if (normalizedPath == null) {
            // erroneous path.
            throw new IllegalArgumentException("Disallowed archive path. Might contain a path traversal attack?");
        }

        return isPomProperties(normalizedPath) || isPomXml(normalizedPath);
    }

    protected boolean importantNonNull(Artifact artifact) {
        return artifact.getArtifactId() != null &&
                artifact.getGroupId() != null &&
                artifact.getVersion() != null;
    }

    protected Artifact getArtifactFromPomProperties(Artifact artifact, InputStream inputStream) {
        Properties pomProperties = new Properties();
        try {
            pomProperties.load(inputStream);
        } catch (IOException e) {
            addError(artifact, "Error while loading 'pom.properties'.");
        }

        Artifact dummyArtifact = new Artifact();
        dummyArtifact.setRelevant(false);
        dummyArtifact.setArtifactId(pomProperties.getProperty("artifactId", null));
        dummyArtifact.setGroupId(pomProperties.getProperty("groupId", null));
        dummyArtifact.setVersion(pomProperties.getProperty("version", null));

        deriveId(dummyArtifact, pomProperties.getProperty("packaging", "jar"));

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    protected Artifact getArtifactFromPomXml(Artifact artifact, InputStream inputStream) {
        Artifact dummyArtifact = new Artifact();

        // parse pom
        try {
            Model model = new MavenXpp3Reader().read(inputStream);

            // grab artifactId, groupId and version from pom. get from parent section if not filled
            if (model.getArtifactId() != null) {
                dummyArtifact.setArtifactId(model.getArtifactId());
            } else {
                dummyArtifact.setArtifactId(model.getParent().getArtifactId());
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

            // NOTE: the current mode is identification. POM specified licenses are not subjec to identification
            // Furthermore, the leaf-pom may not include license information.

            deriveId(dummyArtifact, model.getPackaging() != null ? model.getPackaging() : "jar");

        } catch (IOException | XmlPullParserException e) {
            addError(artifact, "Exception while parsing a 'pom.xml'.");
        }

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    private void deriveId(Artifact dummyArtifact, String type) {
        if ("bundle".equalsIgnoreCase(type)) {
            type = "jar";
        }

        // embedded (shaded) artifacts have no id, we derive one for those without
        if (StringUtils.isEmpty(dummyArtifact.getId())) {
            final String version = dummyArtifact.getVersion();
            dummyArtifact.setId(dummyArtifact.getArtifactId() + "-" +
                    version + "." + type);
        }
    }

    protected Artifact dummyArtifactFromPom(Artifact artifact, ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            if (isPomProperties(pomEntry.getName())) {
                return getArtifactFromPomProperties(artifact, inputStream);
            } else if (isPomXml(pomEntry.getName())) {
                return getArtifactFromPomXml(artifact, inputStream);
            } else {
                addError(artifact, "Pom '" + pomEntry.getName() +  "' can't be parsed.");
            }
        } catch (IOException e) {
            addError(artifact, "IOException while reading pom.");
        }

        return null;
    }

    /**
     * Iternates throw the entries in the jar file and produced an artifact for every pom.properties or pom.xml file.
     *
     * @param jarFile The file being inspected.
     *
     * @return List of artifacts created from pom.properties or pom.xml entries in the jar file.
     */
    protected List<Artifact> getIds(Artifact artifact, File jarFile) {
        final List<Artifact> artifacts = new ArrayList<>();

        try(ZipFile zipFile = new ZipFile(jarFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                if (isPomMeta(entry.getName())) {
                    artifacts.add(dummyArtifactFromPom(artifact, zipFile, entry));
                }
            }
        } catch (IOException e) {
            // ignore
        }

        return artifacts;
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
        final Pattern pattern1 = Pattern.compile(Pattern.quote(dummyArtifact.getArtifactId() +
                "-" + dummyArtifact.getVersion()) +
                "-" + ".*" + ".jar");
        if (pattern1.matcher(fileName).matches()) {
            return true;
        }

        String match2 = dummyArtifact.getArtifactId() + "-" + dummyArtifact.getVersion() + ".jar";
        String match3 = dummyArtifact.getArtifactId() + ".jar";

        // otherwise throw the towel. no idea what this jar is supposed to be.
        return fileName.equals(match2) || fileName.equals(match3);
    }

    protected Set<Artifact> getConflictsWithOriginal(Artifact artifact, Collection<Artifact> toCheck) {
        Set<Artifact> conflictingArtifacts = new HashSet<>();

        for (Artifact checking : toCheck) {
            // since artifactid is wrong at this stage, ignore it for the original state check.

            if (StringUtils.isNotBlank(artifact.getGroupId())
                    && !artifact.getGroupId().equals(checking.getGroupId())) {

                conflictingArtifacts.add(checking);
                continue;
            }

            if (StringUtils.isNotBlank(artifact.getVersion())
                    && (!artifact.getVersion().equals(checking.getVersion()))) {

                conflictingArtifacts.add(checking);
            }
        }

        return conflictingArtifacts;
    }

    protected Set<Artifact> getConflictsWithEachOther(Collection<Artifact> toCheck) {
        Set<String> foundArtifactIds = new HashSet<>();
        Set<String> foundGroupIds = new HashSet<>();
        Set<String> foundVersions = new HashSet<>();

        Set<Artifact> dictatingState = new HashSet<>();

        for (Artifact checking : toCheck) {
            String currentArtifactId = checking.getArtifactId();
            String currentGroupId = checking.getGroupId();
            String currentVersion = checking.getVersion();

            if (!foundArtifactIds.contains(currentArtifactId)) {
                foundArtifactIds.add(currentArtifactId);
                dictatingState.add(checking);
            }

            if (!foundGroupIds.contains(currentGroupId)) {
                foundGroupIds.add(currentGroupId);
                dictatingState.add(checking);
            }

            if (!foundVersions.contains(currentVersion)) {
                foundVersions.add(currentVersion);
                dictatingState.add(checking);
            }
        }

        return dictatingState.size() > 1 ? dictatingState : new HashSet<>();
    }

    private List<Artifact> processArtifact(Artifact artifact, ProjectPathParam projectPathParam) {
        File jarFile = getJarFile(artifact, projectPathParam);

        if (jarFile == null) {
            // early exit: nothing to scan
            return null;
        }

        List<Artifact> dummyArtifacts = getIds(artifact, jarFile).stream()
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
        Set<Artifact> conflictWithOriginal = getConflictsWithOriginal(artifact, accepted);
        Set<Artifact> conflictWithEachOther = getConflictsWithEachOther(accepted);
        if (conflictWithOriginal.size() > 0) {
            addError(artifact, "Number of poms conflict with originally filled state (" + conflictWithOriginal.size() + ").");
        }
        if (conflictWithEachOther.size() > 0) {
            addError(artifact, "Number of poms conflict with each other's state (" + conflictWithEachOther.size() + ").");
        }

        if (accepted.size() > 0) {
            // on match (accepted and no conflicts): insert info into artifact
            if (conflictWithOriginal.size() == 0 && conflictWithEachOther.size() == 0) {
                for (Artifact newData : accepted) {

                    // copy all attributes
                    for (String attribute : newData.getAttributes()) {
                        // take over attributes from matched dummy without overwriting
                        if (StringUtils.isBlank(artifact.get(attribute))) {
                            artifact.set(attribute, newData.get(attribute));
                        }
                    }

                    // derive artifactid once groupId and version are set to produce an up-to-date output
                    artifact.setArtifactId(null);
                    artifact.deriveArtifactId();
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

        // set to record artifact ids in, used to only log errors once
        Set<String> alreadyReported = new HashSet<>();

        // we iterate a cloned list to avoid concurrent modification issues
        for (Artifact artifact : new ArrayList<>(inventory.getArtifacts())) {
            try {
                List<Artifact> notAccepted = processArtifact(artifact, projectPathParam);

                if (jarInspectionParam.isIncludeEmbedded()) {
                    includeEmbedded(inventory, artifact, notAccepted, alreadyReported);
                }
            } catch (Exception e) {
                // log error and carry on
                addError(artifact, "Error while running "
                        + this.getClass().getSimpleName());
                LOG.error("Error while running "
                        + this.getClass().getSimpleName()
                        + " on artifact '" + artifact + "':"
                        + e.getMessage());
            }
        }
    }

    // TODO: special steps to take for artifacts added in this fashion? maybe write a separate Inspector for this?
    private void includeEmbedded(Inventory inventory, Artifact artifact, List<Artifact> detectedArtifactsInFatJar,
                                 Set<String> alreadyReported) {
        if (detectedArtifactsInFatJar != null && !detectedArtifactsInFatJar.isEmpty()) {
            final String assetId = "AID-" + artifact.getId() + "-" + artifact.getChecksum();

            // construct asset metadata for shaded jars
            final AssetMetaData e = new AssetMetaData();
            e.set(AssetMetaData.Attribute.ASSET_ID, assetId);
            inventory.getAssetMetaData().add(e);

            for (Artifact detectedArtifact : detectedArtifactsInFatJar) {
                // filter artifacts that cannot be fully identified
                final String detectedArtifactId = detectedArtifact.getId();
                if (detectedArtifactId != null && detectedArtifactId.contains("${")) {
                    if (!alreadyReported.contains(detectedArtifactId)) {
                        LOG.warn("Skipping embedded artifact without fully qualified artifact id: {}", detectedArtifactId);
                        alreadyReported.add(detectedArtifactId);
                    }
                    continue;
                }

                final Artifact foundArtifact = inventory.findArtifact(detectedArtifact);
                if (foundArtifact != null) {
                    foundArtifact.set(assetId, "x");
                } else {
                    detectedArtifact.set(assetId, "x");
                    inventory.getArtifacts().add(detectedArtifact);
                }
            }
        }
    }

    private boolean isZipArchive(File jarFile) {
        boolean isZipArchive = false;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(jarFile, "r")) {
            long magic = randomAccessFile.readInt();
            if (magic == 0x504B0304) {
                isZipArchive = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isZipArchive;
    }

}
