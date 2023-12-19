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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.ATTRIBUTE_KEY_ASSET_ID_CHAIN;
import static org.metaeffekt.core.util.FileUtils.*;

public class ComponentPatternProducer {

    public static final String DOUBLE_ASTERISK = Constants.ASTERISK + Constants.ASTERISK;

    public static final String[] FILE_SUFFIX_LIST = new String[]{

            // NOTE: the anchor patterns must allow for context. Always try to take the parent directory into
            // context.

            // ruby; order matters; start from the ones you regard more appropriate
            ".gemspec",

            ".xed",

            // web modules
            // prioritize bower components; in general, better metadata first
            ".bower.json",
            "/bower.json",
            "/package-lock.json",
            "/package.json",
            "/composer.json",

            // nextcloud appinfo
            "/appinfo/info.xml",

            // container marker
            "/json",

            // eclipse bundles
            "/about.html",
            "/about.ini",
            "/about.properties",
            "/about.mappings",

            // jars
            "/pom.xml",

            // python modules
            "/METADATA",
            "/RECORD",
            "/WHEEL",
            "/__init__.py",
            "/__about__.py",

            // debian status files
            "/status",

            // node runtime
            "/node/node_version.h",

            // nordeck license summary
            "app/lib/licenses.json",

            // java openjdk/openjre dervied
            "/release",

            // jetty
            "/jetty/version.txt",

            // web applications
            "/web-inf/web.xml"
    };

    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternProducer.class);

    public void extractComponentPatterns(FileSystemScanContext fileSystemScanContext, Inventory targetInventory) {

        final File baseDir = fileSystemScanContext.getBaseDir().getFile();
        final Map<String, Artifact> pathToArtifactMap = new HashMap<>();

        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            // ASSUMPTION: archives are never anchors
            if (artifact.getChecksum() != null) {
                String path = artifact.get(Constants.KEY_PATH_IN_ASSET);
                pathToArtifactMap.put(path, artifact);
            }
        }


        final List<String> filesByPathLength = new ArrayList<>(pathToArtifactMap.keySet());
        filesByPathLength.sort(String.CASE_INSENSITIVE_ORDER);
        filesByPathLength.sort(Comparator.comparingInt(String::length));

        // configure contributors; please note that currently the contributors consume anchors (no anchor can be used twice)
        final List<ComponentPatternContributor> componentPatternContributors = new ArrayList<>();
        componentPatternContributors.add(new DpkgPackageContributor());
        componentPatternContributors.add(new GemSpecContributor());
        componentPatternContributors.add(new ContainerComponentPatternContributor());
        componentPatternContributors.add(new WebModuleComponentPatternContributor());
        componentPatternContributors.add(new UnwrappedEclipseBundleContributor());
        componentPatternContributors.add(new PythonModuleComponentPatternContributor());
        componentPatternContributors.add(new JarModuleComponentPatternContributor());
        componentPatternContributors.add(new NextcloudAppInfoContributor());
        componentPatternContributors.add(new ComposerLockContributor());
        componentPatternContributors.add(new XWikiExtensionComponentPatternContributor());
        componentPatternContributors.add(new NodeRuntimeComponentPatternContributor());
        componentPatternContributors.add(new NordeckAppComponentPatternContributor());
        componentPatternContributors.add(new JavaRuntimeComponentPatternContributor());
        componentPatternContributors.add(new JettyComponentPatternContributor());
        componentPatternContributors.add(new WebApplicationComponentPatternContributor());

        // record component pattern qualifiers for deduplication purposes
        final Set<String> deduplicationQualifierSet = new HashSet<>();

        // for each include pattern (by priority) try to identify a component pattern
        for (String fileSuffix : FILE_SUFFIX_LIST) {

            for (String pathInContext : filesByPathLength) {
                // FIXME: toLowerCase for paths is error-prone and (lack of specified Locale) platform-dependent
                if (!pathInContext.toLowerCase().endsWith(fileSuffix)) continue;

                final Artifact artifact = pathToArtifactMap.get(pathInContext);
                final String checksum = artifact.getChecksum();

                // apply contributors
                for (ComponentPatternContributor cpc : componentPatternContributors) {
                    if (cpc.applies(pathInContext)) {

                        final List<ComponentPatternData> componentPatternDataList =
                                cpc.contribute(baseDir, pathInContext, checksum);

                        if (!componentPatternDataList.isEmpty()) {
                            for (ComponentPatternData cpd : componentPatternDataList) {
                                LOG.info("Identified component pattern: " + cpd.createCompareStringRepresentation());

                                // FIXME: defer to 2nd pass
                                final String version = cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION);
                                if ("unspecific".equalsIgnoreCase(version)) {
                                    continue;
                                }

                                final String qualifier = cpd.deriveQualifier();
                                if (!deduplicationQualifierSet.contains(qualifier)) {
                                    targetInventory.getComponentPatternData().add(cpd);
                                    deduplicationQualifierSet.add(qualifier);
                                }

                                cpd.validate(cpc.getClass().getName());
                            }
                        }

                        // the first contributor wins
                        break;
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
        extractComponentPatterns(fileSystemScanContext, implicitReferenceInventory);

        matchAndApplyComponentPatterns(implicitReferenceInventory, fileSystemScanContext, false);
    }

    public void matchAndApplyComponentPatterns(final Inventory componentPatternSourceInventory,
                                               FileSystemScanContext fileSystemScanContext, boolean applyDeferred) {

        final List<MatchResult> matchedComponentPatterns = matchComponentPatterns(
                fileSystemScanContext.getInventory(), componentPatternSourceInventory, fileSystemScanContext, applyDeferred);

        if (!matchedComponentPatterns.isEmpty()) {
            LOG.info("Matching component patterns resulted in {} anchor matches.", matchedComponentPatterns.size());
        }

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

            final Supplier<Inventory> expansionInventorySupplier = matchResult.componentPatternData.getExpansionInventorySupplier();
            if (expansionInventorySupplier != null) {

                for (Artifact artifact : expansionInventorySupplier.get().getArtifacts()) {
                    artifact.set(ATTRIBUTE_KEY_ASSET_ID_CHAIN, matchResult.assetIdChain);
                    fileSystemScanContext.contribute(artifact);
                }
            }
        }
    }

    private void markFilesCoveredByComponentPatterns(List<MatchResult> matchedComponentDataOnAnchor,
                                                     List<MatchResult> matchResultsWithoutFileMatches, FileSystemScanContext fileSystemScanContext) {

        // remove the matched files covered by the matched component patterns
        for (MatchResult matchResult : matchedComponentDataOnAnchor) {
            final ComponentPatternData cpd = matchResult.componentPatternData;

            final String baseDir = normalizePathToLinux(matchResult.baseDir.getAbsolutePath());
            final String relativePathToComponentBaseDir = asRelativePath(fileSystemScanContext.getBaseDir().getPath(), baseDir);

            // build pattern sets to match (using scanBaseDir relative paths)
            final NormalizedPatternSet normalizedIncludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN));
            final NormalizedPatternSet normalizedExcludePattern = normalizePattern(cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN));

            final Inventory inventory = fileSystemScanContext.getInventory();
            synchronized (inventory) {
                boolean matched = false;
                for (final Artifact artifact : inventory.getArtifacts()) {
                    final String relativePathFromBaseDir = getRelativePathFromBaseDir(artifact);

                    final String absolutePathFromBaseDir = "/" + relativePathFromBaseDir;

                    // match absolute first (anchor matched, so we have to check anyway)
                    if (matches(normalizedExcludePattern.absolutePatterns, absolutePathFromBaseDir)) {
                        // continue without adding
                        continue;
                    }

                    // match absolute include patterns
                    if (matches(normalizedIncludePattern.absolutePatterns, absolutePathFromBaseDir)) {
                        // marked matched
                        artifact.set(FileSystemScanConstants.ATTRIBUTE_KEY_SCAN_DIRECTIVE, FileSystemScanConstants.SCAN_DIRECTIVE_DELETE);
                        artifact.set(ATTRIBUTE_KEY_ASSET_ID_CHAIN, matchResult.assetIdChain);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Component anchor {} (checksum: {}): removed artifact covered by pattern: {} ",
                                    cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR),
                                    cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM),
                                    relativePathFromBaseDir);
                        }

                        // continue adding
                        matched = true;
                        continue;
                    }

                    // NOTE: absolute matching did not conclude anything yet; match relative paths

                    // check whether the artifact is at all in the subtree of the match (relative to component base dir)
                    if (pathBeginsWith(relativePathFromBaseDir, relativePathToComponentBaseDir)) {

                        // NOTE: within this block, we know that we are within the subtree of the anchor (path)

                        // compute the relative path within subtree
                        String relativePathFromComponentBaseDir = relativePathFromBaseDir;
                        if (!relativePathToComponentBaseDir.equalsIgnoreCase(".")) {
                            // cut off path from component base dir
                            relativePathFromComponentBaseDir = relativePathFromComponentBaseDir.
                                    substring(relativePathToComponentBaseDir.length() + 1);
                        }

                        // match patterns (relative only)
                        if (!matches(normalizedExcludePattern.relativePatterns, relativePathFromComponentBaseDir)) {
                            if (matches(normalizedIncludePattern.relativePatterns, relativePathFromComponentBaseDir)) {
                                artifact.set(FileSystemScanConstants.ATTRIBUTE_KEY_SCAN_DIRECTIVE, FileSystemScanConstants.SCAN_DIRECTIVE_DELETE);
                                artifact.set(ATTRIBUTE_KEY_ASSET_ID_CHAIN, matchResult.assetIdChain);

                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Component anchor {} (checksum: {}): removed artifact covered by pattern: {} ",
                                            cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR),
                                            cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM),
                                            relativePathFromBaseDir);
                                }

                                matched = true;
                            }
                        }
                    }
                }

                if (!matched) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No files matched for component pattern {}.", matchResult.componentPatternData.createCompareStringRepresentation());
                    }
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

    public static class NormalizedPatternSet {
        public Set<String> relativePatterns = new LinkedHashSet<>();
        public Set<String> absolutePatterns = new LinkedHashSet<>();
    }

    /**
     * Splits the comma-separated patterns into individual patterns and sorts the patterns into relative and absolute
     * pattern. The {@link NormalizedPatternSet} uses LinkedHashSets to unify the patterns, while preserving the order.
     *
     * @param commaSeparatedPatterns The comma-separated patterns to normalize.
     * @return The normalized pattern set according to the rules above.
     */
    public static NormalizedPatternSet normalizePattern(String commaSeparatedPatterns) {
        final NormalizedPatternSet normalizedPatternSet = new NormalizedPatternSet();
        if (commaSeparatedPatterns == null) return normalizedPatternSet;
        final String[] patterns = commaSeparatedPatterns.split(",");

        for (String pattern : patterns) {
            pattern = pattern.trim();
            pattern = FileUtils.normalizePathToLinux(pattern);

            if (pattern.startsWith("/")) {
                normalizedPatternSet.absolutePatterns.add(pattern);
            } else {
                normalizedPatternSet.relativePatterns.add(pattern);
            }
        }

        return normalizedPatternSet;
    }

    /**
     * Matches the component patterns. The inventory remains unmodified.
     *
     * @param inputInventory                  The inventory carrying the current scan result that is examined for component patterns.
     * @param componentPatternSourceInventory The inventory to take component patterns from.
     * @param fileSystemScanContext           The file system scan context to use.
     * @param applyDeferred                   Whether to apply deferred component patterns.
     * @return List of matched / potential component patterns.
     */
    public List<MatchResult> matchComponentPatterns(final Inventory inputInventory, final Inventory componentPatternSourceInventory,
                                                    FileSystemScanContext fileSystemScanContext, boolean applyDeferred) {

        // match component patterns using version anchor; results in matchedComponentPatterns
        final List<MatchResult> matchedComponentPatterns = new ArrayList<>();

        for (final ComponentPatternData cpd : componentPatternSourceInventory.getComponentPatternData()) {
            LOG.debug("Checking component pattern: {}", cpd.createCompareStringRepresentation());

            // evaluate component pattern mode and skip if mode doesn't match
            final String mode = cpd.get("Mode", "immediate").trim();
            final boolean isDeferred = "deferred".equalsIgnoreCase(mode);
            if (applyDeferred != isDeferred) continue;

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
                            final String assetIdChain = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);
                            matchedComponentPatterns.add(new MatchResult(copyCpd, file,
                                    computeComponentBaseDir(rootDir, file, normalizedVersionAnchor), assetIdChain));
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
