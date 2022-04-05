package org.metaeffekt.core.inventory.processor.probe;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MavenJarIdProbe {
    protected Artifact artifact;

    public MavenJarIdProbe(Artifact artifact) {
        this.artifact = artifact;
    }

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

    protected boolean isNovelArtifact(List<Artifact> artifacts, Artifact artifact) {
        for (Artifact existing : artifacts) {
            // if all are the same, this artifact already exists.
            if (existing.getArtifactId().equals(artifact.getArtifactId()) &&
                    existing.getGroupId().equals(artifact.getGroupId()) &&
                    existing.getVersion().equals(artifact.getVersion())) {
                return false;
            }
        }
        return true;
    }

    protected File getJarFile() {
        // add all existing regular files to file list for later processing.
        String jarPath = this.artifact.getProjects().stream().findFirst().orElse(null);

        File jarFile = null;
        if (jarPath != null) {
            jarFile = new File(jarPath);
        }

        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            addError(this.getClass().getName() + ": Could not read first jar from getProjects.");
            return null;
        } else {
            return jarFile;
        }
    }

    public boolean importantNonNull(Artifact artifact) {
        return artifact.getArtifactId() != null &&
                artifact.getGroupId() != null &&
                artifact.getVersion() != null;
    }

    protected Artifact getArtifactFromPomProperties(InputStream inputStream) {
        Properties pomProperties = new Properties();
        try {
            pomProperties.load(inputStream);
        } catch (IOException e) {
            addError("Error while loading a 'pom.properties'.");
        }

        Artifact dummyArtifact = new Artifact();
        dummyArtifact.setRelevant(false);
        dummyArtifact.setArtifactId(pomProperties.getProperty("artifactId", null));
        dummyArtifact.setGroupId(pomProperties.getProperty("groupId", null));
        dummyArtifact.setVersion(pomProperties.getProperty("version", null));

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    protected Artifact getArtifactFromPomXml(InputStream inputStream) {
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


        } catch (IOException | XmlPullParserException e) {
            addError("Exception while parsing a 'pom.xml'.");
        }

        return importantNonNull(dummyArtifact) ? dummyArtifact : null;
    }

    public Artifact dummyArtifactFromPom(ZipFile zipFile, ZipArchiveEntry pomEntry) {
        try (InputStream inputStream = zipFile.getInputStream(pomEntry)) {
            if (isPomProperties(pomEntry.getName())) {
                return getArtifactFromPomProperties(inputStream);
            } else if (isPomXml(pomEntry.getName())) {
                return getArtifactFromPomXml(inputStream);
            } else {
                addError("Pom '" + pomEntry.getName() +  "' can't be parsed.");
            }
        } catch (IOException e) {
            addError("IOException while reading pom.");
        }

        return null;
    }

    public List<Artifact> getIds(File jarFile) {
        List<Artifact> artifacts = new ArrayList<>();

        try(ZipFile zipFile = new ZipFile(jarFile)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                if (isPomMeta(entry.getName())) {
                    artifacts.add(dummyArtifactFromPom(zipFile, entry));
                }
            }
        } catch (IOException e) {
            addError("IOException while reading project jar.");
        }

        return artifacts;
    }


    protected boolean matchesFileName(String fileName, Artifact dummyArtifact) {
        // match against filename:
        //  strict matching. we find artifact ids and versions.
        //  we check and apply only when one of the following checks succeed:
        //  - either the filename matches "<artifactID>-<version>-<classifier(s)>.jar
        //  - or it matches               "<artifactID>-<version>.jar"
        //  - or it matches               "<artifactID>.jar"
        // keep these definitions strict to not produce "weird" data.

        if (fileName.matches(Pattern.quote(dummyArtifact.getArtifactId() +
                "-" + dummyArtifact.getVersion()) +
                "-" + ".*" + ".jar")) {
            return true;
        }

        String match2 = dummyArtifact.getArtifactId() + "-" + dummyArtifact.getVersion() + ".jar";
        String match3 = dummyArtifact.getArtifactId() + ".jar";

        // otherwise throw the towel. no idea what this jar is supposed to be.
        return fileName.equals(match2) || fileName.equals(match3);
    }

    public Set<Artifact> getConflictsWithOriginal(Collection<Artifact> toCheck) {
        Set<Artifact> conflictingArtifacts = new HashSet<>();

        for (Artifact checking : toCheck) {
            if (StringUtils.isNotBlank(artifact.getArtifactId())) {
                if (!artifact.getArtifactId().equals(checking.getArtifactId())) {
                    conflictingArtifacts.add(checking);
                    continue;
                }
            }

            if (StringUtils.isNotBlank(artifact.getGroupId())) {
                if (!artifact.getGroupId().equals(checking.getGroupId())) {
                    conflictingArtifacts.add(checking);
                    continue;
                }
            }

            if (StringUtils.isNotBlank(artifact.getVersion())) {
                if (!artifact.getVersion().equals(checking.getVersion())) {
                    conflictingArtifacts.add(checking);
                }
            }
        }

        return conflictingArtifacts;
    }

    public Set<Artifact> getConflictsWithEachOther(Collection<Artifact> toCheck) {
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

    private void addError(String errorString) {
        this.artifact.append("Errors", errorString, ", ");
    }

    public void runCompletion() {
        File jarFile = getJarFile();

        List<Artifact> dummyArtifacts;
        if (jarFile != null) {
            dummyArtifacts = getIds(jarFile).stream().filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            return;
        }

        // enforce all of artifactid, version and groupid being non-null for filling to kick in
        List<Artifact> accepted = new ArrayList<>();
        for (Artifact dummyArtifact : dummyArtifacts) {
            if (importantNonNull(dummyArtifact) && matchesFileName(jarFile.getName(), dummyArtifact)) {
                accepted.add(dummyArtifact);
            }
        }

        // process list of accepted dummies, detect disagreements (with original state and each other)
        Set<Artifact> conflictWithOriginal = getConflictsWithOriginal(accepted);
        Set<Artifact> conflictWithEachOther = getConflictsWithEachOther(accepted);
        if (conflictWithOriginal.size() > 1) {
            addError("Number of poms conflict with originally filled state (" + conflictWithOriginal.size() + ").");
        }
        if (conflictWithEachOther.size() > 1) {
            addError("Number of poms conflict with each other's state (" + conflictWithOriginal.size() + ").");
        }


        if (accepted.size() > 0) {
            // on match (accepted and no conflicts): insert info into artifact
            if (conflictWithOriginal.size() == 0 && conflictWithEachOther.size() == 0) {
                Artifact newData = accepted.stream().findAny().get();

                artifact.setArtifactId(newData.getArtifactId());
                artifact.setGroupId(newData.getGroupId());
                artifact.setVersion(newData.getVersion());
            }
        } else {
            // on mismatch: insert error into artifact.
            addError("No poms found by " + this.getClass().getName() + ".");
        }
    }
}
