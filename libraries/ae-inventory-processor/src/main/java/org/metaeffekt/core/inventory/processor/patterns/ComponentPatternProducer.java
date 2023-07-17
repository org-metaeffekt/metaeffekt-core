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
package org.metaeffekt.core.inventory.processor.patterns;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.filescan.MatchResult;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.*;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.util.FileUtils.*;

public class ComponentPatternProducer {

    public static final String DOUBLE_ASTERISK = Constants.ASTERISK + Constants.ASTERISK;

    public static final String[] INCLUDE_PATTERNS_ORDERED_BY_PRIORITY = new String[]{
            // web modules
            // prioritize bower components; in general, better metadata first
            "bower_components/**/.bower.json",
            "bower_components/**/bower.json",
            "node_modules/**/package-lock.json",
            "node_modules/**/package.json",
            "package.json",
            ".bower.json",
            "bower.json",
            "composer.json",

            // container marker
            "json",

            // eclipse bundles
            "about.html",
            "about.ini",
            "about.properties",
            "about.mappings",

            // jars
            "META-INF/maven/**/pom.xml",

            // python modules
            "*.dist-info/METADATA",
            "*.dist-info/RECORD",
            "*.dist-info/WHEEL",
            "__init__.py",
            "__about__.py",
    };
    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternProducer.class);

    public void extractComponentPatterns(File baseDir, Inventory targetInventory) {

        final Set<String> uniqueFolder = new HashSet<>();
        final Map<String, String> uniqueFile = new HashMap<>();

        // collect all folders
        final String[] folders = FileUtils.scanDirectoryForFolders(baseDir, "**/*");

        final Set<String> fileSet = new HashSet<>(Arrays.asList(folders));
        final List<String> foldersByLength = new ArrayList<>(fileSet);
        foldersByLength.sort(String.CASE_INSENSITIVE_ORDER);
        foldersByLength.sort(Comparator.comparingInt(String::length));

        // configure contributors
        final List<ComponentPatternContributor> componentPatternContributors = new ArrayList<>();
        componentPatternContributors.add(new ContainerComponentPatternContributor());
        componentPatternContributors.add(new WebModuleComponentPatternContributor());
        componentPatternContributors.add(new UnwrappedEclipseBundleContributor());
        componentPatternContributors.add(new PythonModuleComponentPatternContributor());
        componentPatternContributors.add(new JarModuleComponentPatternContributor());
        componentPatternContributors.add(new DefaultComponentPatternContributor());

        Set<String> qualifierSet = new HashSet<>();

        // for each include pattern (by priority) try to identify a component pattern
        for (String includePattern : INCLUDE_PATTERNS_ORDERED_BY_PRIORITY) {

            // process folders (ordered by path length)
            for (String folder : foldersByLength) {

                File contextBaseDir = new File(baseDir, folder);

                // FIXME: shouldn't this be an exact match rather that a subtree match; is this an optimization?
                // skip subtrees already covered
                boolean skip = false;
                for (String subtree : uniqueFolder) {
                    if (folder.startsWith(subtree + "/")) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }

                // scan inside folder using the current include pattern
                String[] files = FileUtils.scanForFiles(contextBaseDir, includePattern, "--none--");

                for (String file : files) {
                    final File anchorFile = new File(contextBaseDir, file);
                    final File anchorParentDir = anchorFile.getParentFile();

                    // skip if there is already a component pattern available
                    if (uniqueFolder.contains(anchorParentDir.getAbsolutePath())) {
                        continue;
                    }

                    final String checksum = FileUtils.computeChecksum(anchorFile);
                    uniqueFile.put(checksum, new File(file).getAbsolutePath());
                    uniqueFolder.add(anchorParentDir.getAbsolutePath());

                    final Artifact artifact = new Artifact();
                    artifact.setId(anchorParentDir.getName());
                    artifact.setChecksum(checksum);

                    // construct component pattern
                    final ComponentPatternData componentPatternData = new ComponentPatternData();
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                            FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);

                    // apply contributors
                    for (ComponentPatternContributor cpc : componentPatternContributors) {
                        if (cpc.applies(contextBaseDir, file, artifact)) {
                            cpc.contribute(contextBaseDir, file, artifact, componentPatternData);

                            // the first contributor wins
                            break;
                        }
                    }

                    LOG.info("Identified component pattern: " + componentPatternData.createCompareStringRepresentation());

                    // FIXME: defer to 2nd pass
                    final String version = componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION);
                    if ("unspecific".equalsIgnoreCase(version)) {
                        continue;
                    }

                    final String qualifier = componentPatternData.deriveQualifier();
                    if (!qualifierSet.contains(qualifier)) {
                        targetInventory.getComponentPatternData().add(componentPatternData);
                        qualifierSet.add(qualifier);
                    }
                }
            }
        }
    }

    public void detectAndApplyComponentPatterns(Inventory implicitReferenceInventory, FileSystemScanContext fileSystemScanContext) {
        // NOTE: until here only the static, pre-defined component patterns in the reference inventory have been
        //  anticipated. These prevented to further unwrap parts which are already considered part of a greater
        //  component (otherwise the component pattern would not be precise).
        //  Here, we also have unwrapped the full subtree (except things already covered) and can now derive default
        //  component patterns applying the ComponentPatternProducer.
        extractComponentPatterns(fileSystemScanContext.getBaseDir().getFile(), implicitReferenceInventory);
        matchAndApplyComponentPatterns(implicitReferenceInventory, fileSystemScanContext);
    }

    public void matchAndApplyComponentPatterns(final Inventory componentPatternSourceInventory, FileSystemScanContext fileSystemScanContext) {
        final List<MatchResult> matchedComponentPatterns =
                matchComponentPatterns(fileSystemScanContext.getInventory(), componentPatternSourceInventory, fileSystemScanContext);
        LOG.info("Matching component patterns resulted in {} anchor matches.", matchedComponentPatterns.size());

        final ArrayList<MatchResult> matchResultsWithoutFileMatches = new ArrayList<>();
        markFilesCoveredByComponentPatterns(matchedComponentPatterns, matchResultsWithoutFileMatches, fileSystemScanContext);

        // we need to remove those match results, which did not match any file. Such match results may be caused by
        // generic anchor matches and wildcard anchor checksums.
        if (!matchResultsWithoutFileMatches.isEmpty()) {
            matchedComponentPatterns.removeAll(matchResultsWithoutFileMatches);
        }

        // add artifacts representing the component patterns
        deriveAddonArtifactsFromMatchResult(matchedComponentPatterns, fileSystemScanContext);

        // transfer matched component patterns to the artifact inventory
        for (MatchResult matchResult : matchedComponentPatterns) {
            fileSystemScanContext.getInventory().getComponentPatternData().add(matchResult.componentPatternData);
        }

        // delete artifacts marked with delete directive
        final List<Artifact> toBeDeleted = new ArrayList<>();
        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            final String scanDirectiveDelete = artifact.get(FileSystemScanConstants.ATTRIBUTE_KEY_SCAN_DIRECTIVE);
            if (!StringUtils.isEmpty(scanDirectiveDelete) && scanDirectiveDelete.contains(FileSystemScanConstants.SCAN_DIRECTIVE_DELETE)) {
                toBeDeleted.add(artifact);
            }
        }
        fileSystemScanContext.removeAll(toBeDeleted);
    }

    private void deriveAddonArtifactsFromMatchResult(List<MatchResult> componentPatterns, FileSystemScanContext fileSystemScanContext) {
        for (MatchResult matchResult : componentPatterns) {
            final Artifact derivedArtifact = matchResult.deriveArtifact(fileSystemScanContext.getBaseDir());
            fileSystemScanContext.contribute(derivedArtifact);
        }
    }

    private void markFilesCoveredByComponentPatterns(List<MatchResult> matchedComponentDataOnAnchor, List<MatchResult> matchResultsWithoutFileMatches, FileSystemScanContext fileSystemScanContext) {

        // remove the matched files covered by the matched components
        for (MatchResult matchResult : matchedComponentDataOnAnchor) {
            final ComponentPatternData cpd = matchResult.componentPatternData;

            final String baseDir = normalizePathToLinux(matchResult.baseDir.getAbsolutePath());
            final String relativePathToComponentBaseDir = asRelativePath(fileSystemScanContext.getBaseDir().getPath(), baseDir);

            // build patterns to match (using scanBaseDir relative paths); this should be done during parsing?!
            final String normalizedIncludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN));
            final String normalizedExcludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN));

            final Inventory inventory = fileSystemScanContext.getInventory();
            synchronized (inventory) {
                boolean matched = false;
                for (final Artifact artifact : inventory.getArtifacts()) {
                    String relativePathFromBaseDir = getRelativePathFromBaseDir(artifact);

                    // FIXME: revise path matching; consider abstraction
                    if (pathBeginsWith(relativePathFromBaseDir, relativePathToComponentBaseDir)) {
                        if (!relativePathToComponentBaseDir.equalsIgnoreCase(".")) {
                            relativePathFromBaseDir = relativePathFromBaseDir.substring(relativePathToComponentBaseDir.length() + 1);
                        }
                        if (matches(normalizedIncludePattern, relativePathFromBaseDir)) {
                            if (StringUtils.isEmpty(normalizedExcludePattern) || !matches(normalizedExcludePattern, relativePathFromBaseDir)) {
                                LOG.info("Component anchor {} (checksum: {}): removed artifact covered by pattern: {} ",
                                        cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR),
                                        cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM),
                                        relativePathFromBaseDir);
                                artifact.set(FileSystemScanConstants.ATTRIBUTE_KEY_SCAN_DIRECTIVE, FileSystemScanConstants.SCAN_DIRECTIVE_DELETE);
                                artifact.set(FileSystemScanConstants.ATTRIBUTE_KEY_ASSET_ID_CHAIN, matchResult.assetIdChain);
                                matched = true;
                            }
                        }
                    }
                }

                if (!matched) {
                    LOG.info("No files matched for component pattern {}.", matchResult.componentPatternData.createCompareStringRepresentation());
                    matchResultsWithoutFileMatches.add(matchResult);
                }
            }
        }
    }

    private static String getRelativePathFromBaseDir(Artifact artifact) {
        String relativePathFromBaseDir = artifact.get(FileSystemScanConstants.ATTRIBUTE_KEY_ARTIFACT_PATH);
        if (StringUtils.isEmpty(relativePathFromBaseDir)) {
            // FIXME: fallback to old style
            if (artifact.getProjects().size() > 0) {
                relativePathFromBaseDir = artifact.getProjects().iterator().next();
            }
        }
        return relativePathFromBaseDir;
    }

    private boolean pathBeginsWith(String relativePathFromBaseDir, String relativePathToComponentBaseDir) {
        if (relativePathToComponentBaseDir.equalsIgnoreCase(".")) {
            return true;
        } else {
            return relativePathFromBaseDir.startsWith(relativePathToComponentBaseDir + "/");
        }
    }

    private String normalizePattern(String pattern) {
        if (pattern == null) return null;

        String[] patterns = pattern.split(",");
        return Arrays.stream(patterns)
                .map(String::trim)
                .map(FileUtils::normalizePathToLinux)
                .collect(Collectors.joining(","));
    }

    /**
     * Matches the component patterns. The inventory remains unmodified.
     *
     * @param inputInventory The inventory carrying the current scan result that is examined for component patterns.
     * @param componentPatternSourceInventory The inventory to take component patterns from.
     *
     * @return List of matched / potential component patterns.
     */
    public List<MatchResult> matchComponentPatterns(final Inventory inputInventory, final Inventory componentPatternSourceInventory, FileSystemScanContext fileSystemScanContext) {
        // match component patterns using version anchor; results in matchedComponentPatterns
        final List<MatchResult> matchedComponentPatterns = new ArrayList<>();

        for (final ComponentPatternData cpd : componentPatternSourceInventory.getComponentPatternData()) {
            LOG.debug("Checking component pattern: {}", cpd.createCompareStringRepresentation());

            final String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            final String versionAnchor = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            final String normalizedVersionAnchor = normalizePathToLinux(versionAnchor);

            if (versionAnchor == null) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (versionAnchor.contains(DOUBLE_ASTERISK)) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must not contain **. Use * only.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (anchorChecksum == null) {
                throw new IllegalStateException(String.format("The version anchor checksum of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }

            // memorize whether the path fragment of version anchor contains wildcards
            final boolean isVersionAnchorPattern = versionAnchor.contains(Constants.ASTERISK);

            // memorize whether version anchor checksum is specific (and not just *)
            final boolean isVersionAnchorChecksumSpecific = !anchorChecksum.equalsIgnoreCase(Constants.ASTERISK);

            final File rootDir = fileSystemScanContext.getBaseDir().getFile();

            if (versionAnchor.equalsIgnoreCase(Constants.ASTERISK) || versionAnchor.equalsIgnoreCase(Constants.DOT)) {

                if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK)) {
                    throw new IllegalStateException(String.format(
                            "The version anchor checksum of component pattern [%s] with version anchor [%s] must be '*'.",
                            cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN), versionAnchor));
                }

                // FIXME: why do we clone and adjust the anchor checksum here? May be instantiated multiple times?
                final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, Constants.ASTERISK);
                matchedComponentPatterns.add(new MatchResult(copyCpd, rootDir, rootDir, null));

                // continue with next component pattern (otherwise this would produce a hugh amount of matched patterns)
                continue;
            }

            // extract the longest char sequence (omitting *) from the anchor
            final String[] split = normalizedVersionAnchor.split("\\*");
            final List<String> quickCheck = Arrays.stream(split).sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
            final String longestCharSequenceInVersionAnchor = quickCheck.get(quickCheck.size() - 1);

            // check whether the version anchor path fragment matches one of the file paths
            final String path = fileSystemScanContext.getBaseDir().getPath();
            for (final Artifact artifact : inputInventory.getArtifacts()) {
                // generate normalized path relative to scanBaseDir (important; not to scanDir, which may vary as we
                // descend into the hierarchy on recursion)

                final String relativePathFromBaseDir = getRelativePathFromBaseDir(artifact);

                if (StringUtils.isEmpty(relativePathFromBaseDir)) continue;

                final String normalizedPath = path + "/" + relativePathFromBaseDir;

                if (normalizedPath.contains(longestCharSequenceInVersionAnchor)) {
                    if (versionAnchorMatches(normalizedVersionAnchor, normalizedPath, isVersionAnchorPattern)) {

                        // on match infer the checksum of the file
                        final String fileChecksumOrAsterisk = isVersionAnchorChecksumSpecific ? artifact.getChecksum() : Constants.ASTERISK;

                        if (!anchorChecksum.equalsIgnoreCase(fileChecksumOrAsterisk)) {
                            LOG.debug("Anchor fileChecksumOrAsterisk mismatch: " + normalizedPath);
                            LOG.debug("Expected fileChecksumOrAsterisk :{}; actual file fileChecksumOrAsterisk: {}", anchorChecksum, fileChecksumOrAsterisk);
                        } else {
                            final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                            copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, fileChecksumOrAsterisk);

                            final File file = new File(normalizedPath);
                            matchedComponentPatterns.add(new MatchResult(copyCpd, file,
                                computeComponentBaseDir(rootDir, file, normalizedVersionAnchor), artifact.get("ASSET_ID_CHAIN")));
                        }
                    }
                }
            }
        }

        return matchedComponentPatterns;
    }

    private static boolean versionAnchorMatches(String normalizedVersionAnchor, String normalizedPath, boolean isVersionAnchorPattern) {
        return (!isVersionAnchorPattern && normalizedPath.endsWith(normalizedVersionAnchor)) ||
                (isVersionAnchorPattern && matches("**/" + normalizedVersionAnchor, normalizedPath));
    }

    private static File computeComponentBaseDir(File scanBaseDir, File anchorFile, String versionAnchor) {
        if (Constants.ASTERISK.equalsIgnoreCase(versionAnchor)) return scanBaseDir;
        if (Constants.DOT.equalsIgnoreCase(versionAnchor)) return scanBaseDir;

        final int versionAnchorFolderDepth = org.springframework.util.StringUtils.countOccurrencesOf(versionAnchor, "/") + 1;

        File baseDir = anchorFile;
        for (int i = 0; i < versionAnchorFolderDepth; i++) {
            baseDir = baseDir.getParentFile();

            // handle special case the parent dir does not exist (for whatever reason)
            if (baseDir == null) {
                baseDir = scanBaseDir;
                break;
            }
        }
        return baseDir;
    }


}
