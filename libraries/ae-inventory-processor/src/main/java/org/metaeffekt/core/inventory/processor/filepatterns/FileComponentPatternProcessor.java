/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.filepatterns;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.PatternSetMatcher;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FileComponentPatternProcessor {

    private final Map<String, FileMetaData> pathDataMap;
    private final List<CompiledFileComponentPattern> patterns;
    private final Map<String, String> idToVersionMap;
    private final List<String> unknownFilePatterns;
    private final static Artifact NULL_ARTIFACT = new Artifact();

    private static class CompiledFileComponentPattern {
        public FileComponentPattern fileComponentPattern;
        public Pattern pattern;
    }

    public FileComponentPatternProcessor(Map<String, String> idToVersionMap, Set<String> unknownFilePatterns) {
        this.pathDataMap = new HashMap<>();
        this.patterns = compile(DefaultFileComponentPatterns.PATTERNS);
        this.idToVersionMap = idToVersionMap;
        this.unknownFilePatterns = new ArrayList<>(unknownFilePatterns);
    }

    public FileComponentPatternProcessor() {
        this.pathDataMap = new HashMap<>();
        this.patterns = compile(DefaultFileComponentPatterns.PATTERNS);
        this.idToVersionMap = new HashMap<>();
        this.unknownFilePatterns = new ArrayList<>();
    }

    private List<CompiledFileComponentPattern> compile(List<FileComponentPattern> fileComponentPatternList) {
        // compile patterns
        final List<CompiledFileComponentPattern> compiledPatterns = new ArrayList<>();
        for (FileComponentPattern fcp : fileComponentPatternList) {
            CompiledFileComponentPattern cfcp = new CompiledFileComponentPattern();
            cfcp.fileComponentPattern = fcp;
            cfcp.pattern = Pattern.compile(fcp.patternString);
            compiledPatterns.add(cfcp);
        }
        return compiledPatterns;
    }

    public void applyFileComponentPatterns(Inventory inventory) {
        final Map<String, Artifact> idToRepresentativeArtifactMap = new HashMap<>();

        final Set<Artifact> removableArtifacts = new HashSet<>();
        final List<Artifact> toBeAddedArtifacts = new ArrayList<>();

        final Set<String> removableMatches = new HashSet<>();

        boolean fail = false;

        for (Artifact artifact : inventory.getArtifacts()) {
            final String type = artifact.get(Constants.KEY_TYPE);

            // anticipate other artifacts in inventory; apply file patterns only to files; needs to be revised
            if (type == null || Constants.ARTIFACT_TYPE_FILE.equals(type)) {

                // check idToVersion map first; consume id if present and move to next artifact
                final String artifactId = artifact.getId();
                final String mappedVersion = idToVersionMap != null ? idToVersionMap.get(artifactId) : null;
                if (mappedVersion != null) {
                    artifact.setVersion(mappedVersion);
                    continue;
                }

                final Artifact representativeArtifact = idToRepresentativeArtifactMap.get(artifactId);
                if (representativeArtifact != null) {
                    // found an already matched artifact with the same id
                    processRepresentativeArtifact(artifact, representativeArtifact, removableArtifacts);
                    continue;
                }

                // we have no representative yet...
                final Set<String> projects = artifact.getRootPaths();
                for (String path : projects) {

                    // skip files that we have anyway no pattern for; add to configuration; these files remain unprocessed in this inventory
                    final PatternSetMatcher unknownFilePatternSetMatcher = new PatternSetMatcher(unknownFilePatterns);
                    if (unknownFilePatternSetMatcher.matches(path)) {
                        continue;
                    }

                    boolean detected = false;
                    for (CompiledFileComponentPattern cfcp : patterns) {
                        final Matcher matcher = cfcp.pattern.matcher(path);
                        if (matcher.matches()) {
                            idToRepresentativeArtifactMap.put(artifactId, artifact);

                            final FileComponentPattern fcp = cfcp.fileComponentPattern;
                            final String name = matcher.replaceAll(fcp.replacementForName);
                            final String version = matcher.replaceAll(fcp.replacementForVersion);
                            final String qualifier = matcher.replaceAll(fcp.replacementForQualifier);
                            final String removableSubPath = matcher.replaceAll(fcp.replacementForSubpath);

                            if (qualifier.startsWith("-")) {
                                throw new IllegalStateException(String.format("Invalid file component pattern: %s", cfcp.fileComponentPattern.patternString));
                            }

                            final Artifact derivedArtifact = new Artifact();
                            derivedArtifact.setId(qualifier);
                            derivedArtifact.setComponent(name);
                            derivedArtifact.setVersion(version);
                            derivedArtifact.set(Constants.KEY_TYPE, fcp.type);
                            derivedArtifact.addRootPath(path);

                            removableMatches.add(removableSubPath);
                            toBeAddedArtifacts.add(derivedArtifact);

                            detected = true;
                        }
                    }

                    if (!detected) {
                        // complain
                        if (path.endsWith(".jar")) {
                            if (idToVersionMap != null && idToVersionMap.get(new File(path).getName()) == null) {
                                System.out.println("idToVersionMap.put(\"" + new File(path).getName() + "\", null); // " + path);
                                fail = true;
                            }
                        }

                        // automatically fill
                        idToRepresentativeArtifactMap.put(artifactId, NULL_ARTIFACT);
                    }
                }
            }
        }

        if (fail) {
            throw new IllegalStateException("Failed to process artifact.");
        }

        for (Artifact artifact : inventory.getArtifacts()) {
            final String type = artifact.get(Constants.KEY_TYPE);

            // anticipate other artifacts in inventory; apply file patterns only to files; needs to be revised
            if (type == null || Constants.ARTIFACT_TYPE_FILE.equals(type)) {
                final Set<String> projects = artifact.getRootPaths();
                for (String path : projects) {
                    for (String removableSubPaths : removableMatches) {
                        if (path.contains(removableSubPaths)) {
                            artifact.getRootPaths().remove(path);
                        }
                    }
                }
                if (artifact.getRootPaths().isEmpty()) {
                    removableArtifacts.add(artifact);
                }
            }
        }

        // remove all files covered by new artifact
        inventory.getArtifacts().removeAll(removableArtifacts);

        // add new derived artifacts
        inventory.getArtifacts().addAll(toBeAddedArtifacts);
    }

    private static void processRepresentativeArtifact(Artifact artifact, Artifact representativeArtifact, Set<Artifact> removableArtifacts) {
        // if we were not successful before, we expect not to be successful now
        if (representativeArtifact == NULL_ARTIFACT) {
            return;
        }

        // merge rootPaths with representative
        final Set<String> rootPaths = new HashSet<>(representativeArtifact.getRootPaths());
        rootPaths.addAll(artifact.getRootPaths());
        representativeArtifact.setRootPaths(rootPaths);

        // remove the artifact
        removableArtifacts.add(artifact);
    }

    public FileMetaData deriveFileMetaData(String path) {
        // check map; early exit
        final FileMetaData mappedData = pathDataMap.get(path);
        if (mappedData != null) {
            return mappedData;
        }

        // skip files that we have anyway no pattern for
        if (path.endsWith(".dat")) return null;
        if (path.endsWith(".py")) return null;
        if (path.endsWith(".pyc")) return null;
        if (path.endsWith(".c")) return null;
        if (path.endsWith(".cpp")) return null;
        if (path.endsWith(".h")) return null;
        if (path.endsWith(".notzip")) return null;
        if (path.endsWith(".ko")) return null;
        if (path.endsWith(".go")) return null;
        if (path.endsWith(".vmanifest")) return null;
        if (path.endsWith("/Makefile")) return null;
        if (path.endsWith("/makefile")) return null;

        // apply file component patterns
        for (CompiledFileComponentPattern cfcp : patterns) {
            final Matcher matcher = cfcp.pattern.matcher(path);
            if (matcher.matches()) {
                final FileComponentPattern fcp = cfcp.fileComponentPattern;
                final String name = matcher.replaceAll(fcp.replacementForName);
                final String version = matcher.replaceAll(fcp.replacementForVersion);
                final String qualifier = matcher.replaceAll(fcp.replacementForQualifier);
                final String removableSubPath = matcher.replaceAll(fcp.replacementForSubpath);

                FileMetaData fmd = new FileMetaData();
                fmd.setPath(path);
                fmd.setName(name);
                fmd.setVersion(version);
                fmd.setQualifier(qualifier);
                fmd.setType(fcp.getType());
                fmd.setSpecificType(fcp.getSpecificType());

                return fmd;
            }
        }

        // complain
        if (path.endsWith(".jar")) {
            log.warn("Could not identify matching pattern to artifact in path: " + path);
        }

        return null;
    }

}
