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
package org.metaeffekt.core.inventory.processor.adapter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.adapter.npm.PackageLockParser;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;

import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.MARKER_OPTIONAL_DEPENDENCY;

@Slf4j
public abstract class AbstractLockAdapter {

    protected abstract String buildPurl(ResolvedModule module);

    /**
     * Collects all known direct DependencyTuples-
     *
     * @param packageLockParser The packageLockParser used.
     * @param processedModules The collected processed modules.
     * @param npmModuleArtifactMap Map of NpmModules to Artifacts.
     *
     * @return Stack or DependencyTuples for further processing.
     */
    protected Stack<DependencyTuple> collectDependencyTuples(PackageLockParser packageLockParser, List<ResolvedModule> processedModules, Map<ResolvedModule, Artifact> npmModuleArtifactMap) {
        final Stack<DependencyTuple> dependencyTuples = new Stack<>();
        for (ResolvedModule module : processedModules) {
            apply(module, module.getRuntimeDependencies(), npmModuleArtifactMap, MARKER_RUNTIME_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getDevDependencies(), npmModuleArtifactMap, MARKER_DEVELOPMENT_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getPeerDependencies(), npmModuleArtifactMap, MARKER_PEER_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getOptionalDependencies(), npmModuleArtifactMap, MARKER_OPTIONAL_DEPENDENCY, dependencyTuples, packageLockParser);
        }
        return dependencyTuples;
    }

    private void apply(ResolvedModule dependentModule, Map<String, UnresolvedModule> dependencyMap,
                       Map<ResolvedModule, Artifact> npmModuleArtifactMap, String dependencyType,
                       Stack<DependencyTuple> dependencyTuples, PackageLockParser packageLockParser) {

        if (dependencyMap == null) return;

        final Artifact dependentArtifact = npmModuleArtifactMap.get(dependentModule);

        if (dependentArtifact == null) throw new IllegalStateException("No artifact found for module: " + dependentModule);

        for  (Map.Entry<String, UnresolvedModule> entry : dependencyMap.entrySet()) {
            final ResolvedModule dependencyModule = packageLockParser.resolveNpmModule(dependentModule, entry.getKey(), entry.getValue().getVersionRange());
            if (dependencyModule == null) {
                reportMissingDependency(dependentModule, MARKER_PEER_DEPENDENCY.equalsIgnoreCase(dependencyType), entry, packageLockParser);
            } else {
                final Artifact dependencyArtifact = npmModuleArtifactMap.get(dependencyModule);
                if (dependencyArtifact != null) {
                    if (dependencyArtifact != dependentArtifact) {
                        dependencyArtifact.set("AID-" + dependentArtifact.getId(), dependencyType);
                        dependencyTuples.push(new DependencyTuple(dependentModule, dependencyType, dependencyModule));
                    }
                } else {
                    log.warn("Unable to find dependency for module [{}].", dependentModule.getName());
                }
            }
        }
    }

    protected void reportMissingDependency(ResolvedModule dependentResolvedModule, boolean peerDependency,
                                           Map.Entry<String, UnresolvedModule> entry, PackageLockParser packageLockParser) {

        final UnresolvedModule value = entry.getValue();
        if (value != null) {
            final String name = value.getName();
            if (!packageLockParser.getRuntimeEnvironmentModules().contains(name) && !peerDependency) {
                final String qualifier = name + "@" + value.getVersionRange();
                log.warn("Module [{}] with path [{}] not resolved in context [{}].",
                        qualifier, entry.getKey(), dependentResolvedModule.getPath());
                if (log.isDebugEnabled()) {
                    final List<String> options = packageLockParser.getPathModuleMap().keySet().stream()
                            .filter(n -> n.contains(name))
                            .collect(Collectors.toList());
                    if (!options.isEmpty()) {
                        log.debug("Options:");
                        options.forEach(log::debug);
                    }
                }
            }
        }
    }

    /**
     * The look file may be inaccurate in the sense that some dependencies are treated as direct dependencies, while
     * these are not listed in the package.json. Instead of artificially modifying the dependency tree for such cases
     * this method cures the result and managed incorrectly derived direct dependencies to indirect dependencies.
     *
     * @param webModule The web module.
     * @param rootResolvedModule The root NpmModule.
     * @param npmModuleArtifactMap The NpmModule to Artifact map.
     * @param packageLockParser The PackageLockParser holding parsing information.
     */
    protected void reviseDirectDependencyMarker(WebModule webModule, ResolvedModule rootResolvedModule, Map<ResolvedModule, Artifact> npmModuleArtifactMap, PackageLockParser packageLockParser) {
        // revise derived markers and correct false-direct dependencies to indirect dependencies
        if (rootResolvedModule != null) {
            final List<WebModuleDependency> directDependencies = webModule.getDirectDependencies();

            if (directDependencies != null && !directDependencies.isEmpty()) {
                final Map<String, WebModuleDependency> qualifierDependencyMap = new HashMap<>();
                for (WebModuleDependency wmd : directDependencies) {
                    ResolvedModule resolvedModule = packageLockParser.resolveNpmModule(packageLockParser.getRootModule(), wmd.getName(), wmd.getVersionRange());
                    if (resolvedModule != null) {
                        qualifierDependencyMap.put(resolvedModule.deriveQualifier(), wmd);
                    }
                }

                final String rootAssetId = "AID-" + rootResolvedModule.getName() + "-" + rootResolvedModule.getVersion();

                for (Map.Entry<ResolvedModule, Artifact> entry : npmModuleArtifactMap.entrySet()) {
                    final Artifact artifact = entry.getValue();
                    final String currentMark = artifact.get(rootAssetId);
                    if (currentMark != null && !currentMark.startsWith("(")) {

                        final ResolvedModule resolvedModule = entry.getKey();
                        final WebModuleDependency webModuleDependency = qualifierDependencyMap.get(resolvedModule.deriveQualifier());

                        if (webModuleDependency == null) {
                            String newMark = "(" + currentMark + ")";
                            if (log.isDebugEnabled()) {
                                log.debug("Managing indirect dependency of [{}] from [{}] to [{}].", resolvedModule.deriveQualifier(), currentMark, newMark);
                            }
                            artifact.set(rootAssetId, newMark);
                        }
                    }
                }
            }
        }
    }

    protected List<ResolvedModule> collectModules(WebModule webModule, PackageLockParser packageLockParser, Stack<ResolvedModule> stack,
                                                  Map<ResolvedModule, Artifact> npmModuleArtifactMap, Map<String, Artifact> qualifierArtifactMap, Inventory inventory) {
        final List<ResolvedModule> processedModules = new ArrayList<>();
        while (!stack.isEmpty()) {
            final ResolvedModule module = stack.pop();
            processSingleModule(webModule, packageLockParser, stack, npmModuleArtifactMap, qualifierArtifactMap, inventory, module, processedModules);


        }
        return processedModules;
    }

    protected Artifact processSingleModule(WebModule webModule, PackageLockParser packageLockParser, Stack<ResolvedModule> stack,
                                           Map<ResolvedModule, Artifact> npmModuleArtifactMap, Map<String, Artifact> qualifierArtifactMap, Inventory inventory, ResolvedModule module, List<ResolvedModule> processedModules) {
        // only process a module once
        if (npmModuleArtifactMap.containsKey(module)) return npmModuleArtifactMap.get(module);

        // remember that we processed this module
        processedModules.add(module);

        final String qualifier = module.deriveQualifier();
        Artifact artifact = qualifierArtifactMap.get(qualifier);

        if (artifact == null) {
            artifact = new Artifact();
            artifact.setId(qualifier);

            // determine component name; currently rather atomic
            String componentName = module.getName();
            artifact.setComponent(componentName);

            // contribute other attributes
            artifact.setVersion(module.getVersion());
            artifact.set(KEY_TYPE, ARTIFACT_TYPE_WEB_MODULE);

            artifact.set(KEY_COMPONENT_SOURCE_TYPE, getComponentSourceType());
            artifact.set("Source Archive - URL", module.getSourceArchiveUrl());

            final String purl = buildPurl(module);
            artifact.set(Artifact.Attribute.PURL, purl);
            // NOTE: do not populate ARTIFACT_ROOT_PATHS; a root path is not known in this case

            inventory.getArtifacts().add(artifact);

            qualifierArtifactMap.put(qualifier, artifact);
        }

        // extend the artifact with the additional path information
        final Set<String> set = artifact.getSet(KEY_PATH_IN_ASSET);
        final String rootPath = webModule.getPath() + "[" + module.getName() + "@" + module.getVersion() + "]";
        if (!set.contains(rootPath)) {
            artifact.append(KEY_PATH_IN_ASSET, rootPath, Artifact.PATH_DELIMITER);
        }

        // one artifact may be shared by several modules; (unresolved) modules may have a content
        npmModuleArtifactMap.put(module, artifact);

        pushDependenciesToStack(module, module.getRuntimeDependencies(), stack, false, packageLockParser);
        pushDependenciesToStack(module, module.getDevDependencies(), stack, false, packageLockParser);
        pushDependenciesToStack(module, module.getOptionalDependencies(), stack, false, packageLockParser);
        pushDependenciesToStack(module, module.getPeerDependencies(), stack, true, packageLockParser);

        return artifact;
    }

    protected abstract String getComponentSourceType();

    protected void propagateDependencyDetails(Stack<DependencyTuple> dependencyTuples, Map<ResolvedModule, Artifact> npmModuleArtifactMap) {
        final Set<String> processedTuples = new HashSet<>();
        while (!dependencyTuples.isEmpty()) {
            final DependencyTuple dependencyTuple = dependencyTuples.pop();

            final ResolvedModule middleModule = dependencyTuple.getDependentModule();
            final ResolvedModule bottomModule = dependencyTuple.getDependencyModule();
            final String middleToBottomDependencyType = dependencyTuple.getDependencyType();

            if (log.isDebugEnabled()) {
                log.debug("Dependency Tuple: {} depends ({}) on {}.",
                        middleModule.getName() + ":" + middleModule.getVersion(),
                        middleToBottomDependencyType,
                        bottomModule.getName() + ":" + middleModule.getVersion());
            }

            for (ResolvedModule topModule : middleModule.getDependentModules()) {
                final Artifact middleArtifact = npmModuleArtifactMap.get(middleModule);
                final Artifact bottomArtifact = npmModuleArtifactMap.get(bottomModule);

                final String topAssetId = "AID-" + topModule.getName() + "-" + topModule.getVersion();
                final String topToMiddleDependencyType = middleArtifact.get(topAssetId);
                final String topToBottomDependencyType = bottomArtifact.get(topAssetId);

                final DependencyTriple dependencyTriple = new DependencyTriple(
                        topModule, topToMiddleDependencyType, middleModule, middleToBottomDependencyType, bottomModule);
                String depType = topToBottomDependencyType;
                if (topToBottomDependencyType == null) {
                    depType = dependencyTriple.getTopToBottomDependencyType();
                    bottomArtifact.set(topAssetId, depType);
                }

                final DependencyTuple derivedTuple = new DependencyTuple(
                        dependencyTriple.getTopModule(), depType, dependencyTriple.getBottomModule());
                final String tupleQualifier = derivedTuple.deriveQualifier();

                // only add tuple in case it was not processed already
                if (!processedTuples.contains(tupleQualifier)) {
                    processedTuples.add(tupleQualifier);
                    dependencyTuples.push(derivedTuple);
                }
            }
        }
    }

    private void pushDependenciesToStack(ResolvedModule dependentModule, Map<String, UnresolvedModule> dependencyNameVersionRangeMap,
                                         Stack<ResolvedModule> stack, boolean peerDependency, PackageLockParser packageLockParser) {

        if (dependencyNameVersionRangeMap == null) return;

        for  (Map.Entry<String, UnresolvedModule> entry : dependencyNameVersionRangeMap.entrySet()) {
            final String dependencyName = entry.getKey();
            final String dependencyVersionRange = entry.getValue().getVersionRange();
            final ResolvedModule dependencyModule = packageLockParser.resolveNpmModule(dependentModule, dependencyName, dependencyVersionRange);
            if (dependencyModule == null) {
                reportMissingDependency(dependentModule, peerDependency, entry, packageLockParser);
            } else {
                // manage dependent modules; add dependentModule to dependentModules list of dependency
                final List<ResolvedModule> dependentModules = dependencyModule.getDependentModules();
                if (!dependentModules.contains(dependentModule)) {
                    dependentModules.add(dependentModule);
                }
                // push onto stack for further downstream processing
                stack.push(dependencyModule);
            }
        }
    }

    @Getter
    protected static class DependencyTuple {
        private final ResolvedModule dependentModule;
        private final ResolvedModule dependencyModule;
        private final String dependencyType;

        public DependencyTuple(ResolvedModule dependentModule, String dependencyType, ResolvedModule dependencyModule) {
            this.dependentModule = dependentModule;
            this.dependencyModule = dependencyModule;
            this.dependencyType = dependencyType;
        }

        public String deriveQualifier() {
            String qualifier = dependentModule.getName() + dependentModule.getVersion();
            qualifier += "=" + dependencyType;
            qualifier += "=" + dependencyModule.getName() + dependencyModule.getVersion();
            return qualifier;
        }
    }

    @Getter
    protected static class DependencyTriple {
        private final ResolvedModule topModule;
        private final ResolvedModule middleModule;
        private final ResolvedModule bottomModule;

        private final String topToMiddleDependencyType;
        private final String middleToBottomDependencyType;

        public DependencyTriple(ResolvedModule topModule, String topToMiddleDependencyType, ResolvedModule middleModule, String middleToBottomDependencyType, ResolvedModule bottomModule) {
            this.topModule = topModule;
            this.middleModule = middleModule;
            this.bottomModule = bottomModule;
            this.topToMiddleDependencyType = topToMiddleDependencyType;
            this.middleToBottomDependencyType = middleToBottomDependencyType;
        }

        public String getTopToBottomDependencyType() {
            switch (deriveCombinedType()) {
                case "-r": return "(r)";
                case "-d": return "(d)";
                case "-o": return "(o)";
                case "-p": return "(p)";

                case "d-r" :
                case "d-d" :
                case "d-p" : return "(d)";
                case "d-o" : return "(o)";

                case "r-r" : return "(r)";
                case "r-d" : return "(d)";
                case "r-p" : return "(r)";
                case "r-o" : return "(o)";

                case "p-r" : return "(r)";
                case "p-d" : return "(d)";
                case "p-p" : return "(p)";
                case "p-o" : return "(o)";

                case "o-r" :
                case "o-d" :
                case "o-p" :
                case "o-o" : return "(o)";

                default:
                    // return 'u' for unknown
                    return "u";
            }
        }

        private String deriveCombinedType() {
            String topToMiddleType = topToMiddleDependencyType;
            String middleToBottomType = middleToBottomDependencyType;

            if (topToMiddleType == null) {
                topToMiddleType = "";
            } else if (topToMiddleType.startsWith("(")) {
                topToMiddleType = topToMiddleType.substring(1, 2);
            }

            if (middleToBottomType == null) {
                middleToBottomType = "";
            } else if (middleToBottomType.startsWith("(")) {
                middleToBottomType = middleToBottomType.substring(1, 2);
            }

            return topToMiddleType + "-" + middleToBottomType;
        }
    }

    protected Inventory createInventory(WebModule webModule, PackageLockParser packageLockParser) {
        final Inventory inventory = new Inventory();

        final Map<ResolvedModule, Artifact> npmModuleArtifactMap = new HashMap<>();
        final Stack<ResolvedModule> moduleStack = new Stack<>();

        final Map<String, Artifact> qualifierArtifactMap = new HashMap<>();

        // make initial pushed onto module stack

        final ResolvedModule rootModule = packageLockParser.getRootModule();
        if (rootModule != null) {
            moduleStack.push(rootModule);

            final AssetMetaData assetMetaData = new AssetMetaData();
            assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, "AID-" + rootModule.deriveQualifier());
            assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH, webModule.getPath());
            assetMetaData.set(AssetMetaData.Attribute.NAME, rootModule.getName());
            assetMetaData.set(AssetMetaData.Attribute.VERSION, rootModule.getVersion());
            assetMetaData.set(KEY_PRIMARY, MARKER_CROSS);

            inventory.getAssetMetaData().add(assetMetaData);
        } else {
            packageLockParser.getPathModuleMap().values().forEach(moduleStack::push);
        }

        // step 1: collect all relevant dependencies as modules
        final List<ResolvedModule> processedModules = collectModules(webModule, packageLockParser, moduleStack, npmModuleArtifactMap, qualifierArtifactMap, inventory);
        final Set<ResolvedModule> unprocessedModules = new HashSet<>(packageLockParser.getPathModuleMap().values());
        processedModules.forEach(unprocessedModules::remove);

        // step 2: handle unprocessed modules; these are modules that are not part of the dependency tree
        if (!unprocessedModules.isEmpty()) {
            for (ResolvedModule resolvedModule : unprocessedModules) {
                Artifact artifact = processSingleModule(webModule, packageLockParser, moduleStack, npmModuleArtifactMap, qualifierArtifactMap, inventory, resolvedModule, processedModules);

                // FIXME-KKL: solve differently; the modules have no explicit dependency to the root and will be
                //  filtered when condensed. We need to hook these unreferenced modules into the dependency tree for
                //  proper processing; yet as transitive dependency.
                if (artifact != null && rootModule != null) {
                    String qualifier = rootModule.deriveQualifier();
                    if (resolvedModule.isDevDependency()) {
                        artifact.set("AID-" + qualifier, "(d)");
                    }
                    if (resolvedModule.isRuntimeDependency()) {
                        artifact.set("AID-" + qualifier, "(r)");
                    }
                    if (resolvedModule.isPeerDependency()) {
                        artifact.set("AID-" + qualifier, "(p)");
                    }
                    if (resolvedModule.isOptionalDependency()) {
                        artifact.set("AID-" + qualifier, "(o)");
                    }
                }
            }
        }

        // step 3: fill in known direct dependencies; collect as DependencyTuples
        final Stack<DependencyTuple> dependencyTuples = collectDependencyTuples(packageLockParser, processedModules, npmModuleArtifactMap);

        // step 4: extend the DependencyTuples to all possible DependencyTriples and conclude dependency types on artifact-level
        propagateDependencyDetails(dependencyTuples, npmModuleArtifactMap);

        // step 5: revise direct dependency from root module
        reviseDirectDependencyMarker(webModule, rootModule, npmModuleArtifactMap, packageLockParser);

        return inventory;
    }

}
