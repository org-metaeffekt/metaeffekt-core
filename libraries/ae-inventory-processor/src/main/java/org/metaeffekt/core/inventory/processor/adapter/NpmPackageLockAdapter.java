/*
 * Copyright 2009-2024 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.adapter.npm.NpmLockParserFactory;
import org.metaeffekt.core.inventory.processor.adapter.npm.PackageLockParser;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
@Slf4j
public class NpmPackageLockAdapter {

    /**
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param relPath The relative path to the file from the relevant basedir.
     * @param webModule The webModule for which to extract data.
     *
     * @return An inventory populated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, String relPath, WebModule webModule) throws IOException {

        final PackageLockParser packageLockParser = NpmLockParserFactory.createPackageLockParser(packageLockJsonFile);

        // parse dependency tree from lock; may use the webModule name for filtering (project in workspace)
        packageLockParser.parseModules(webModule);

        // populate data into inventory
        return createInventory(webModule, packageLockParser);
    }

    private Inventory createInventory(WebModule webModule, PackageLockParser packageLockParser) {
        final Inventory inventory = new Inventory();

        final Map<NpmModule, Artifact> npmModuleArtifactMap = new HashMap<>();
        final Stack<NpmModule> stack = new Stack<>();

        final Map<String, Artifact> qualifierArtifactMap = new HashMap<>();
        final List<NpmModule> processedModules = new ArrayList<>();

        // push self onto stack first
        NpmModule rootModule = packageLockParser.getRootModule();

        if (rootModule != null) {
            stack.push(rootModule);

            final AssetMetaData assetMetaData = new AssetMetaData();
            assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, "AID-" + rootModule.deriveQualifier());
            assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH, webModule.getPath());
            assetMetaData.set(AssetMetaData.Attribute.NAME, rootModule.getName());
            assetMetaData.set(AssetMetaData.Attribute.VERSION, rootModule.getVersion());
            assetMetaData.set(KEY_PRIMARY, MARKER_CROSS);

            inventory.getAssetMetaData().add(assetMetaData);
        } else {
            packageLockParser.getPathModuleMap().values().forEach(stack::push);
        }

        while (!stack.isEmpty()) {
            final NpmModule module = stack.pop();

            // only process a module once
            if (npmModuleArtifactMap.containsKey(module)) continue;

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
                artifact.set(KEY_COMPONENT_SOURCE_TYPE, "npm-module");
                artifact.set("Source Archive - URL", module.getUrl());

                final String purl = buildPurl(module.getName(), module.getVersion());
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
        }

        // fill in transitive dependency information and collect DependencyTuples
        // step 1:
        final Stack<DependencyTuple> dependencyTuples = new Stack<>();
        for (NpmModule module : processedModules) {
            apply(module, module.getRuntimeDependencies(), npmModuleArtifactMap, MARKER_RUNTIME_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getDevDependencies(), npmModuleArtifactMap, MARKER_DEVELOPMENT_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getPeerDependencies(), npmModuleArtifactMap, MARKER_PEER_DEPENDENCY, dependencyTuples, packageLockParser);
            apply(module, module.getOptionalDependencies(), npmModuleArtifactMap, MARKER_OPTIONAL_DEPENDENCY, dependencyTuples, packageLockParser);
        }

        // step 2: extend the DependencyTuples to all possible DependencyTriples and conclude dependency types
        final Set<String> processedTuples = new HashSet<>();
        while (!dependencyTuples.isEmpty()) {
            final DependencyTuple dependencyTuple = dependencyTuples.pop();

            final NpmModule middleModule = dependencyTuple.getDependentModule();
            final NpmModule bottomModule = dependencyTuple.getDependencyModule();
            final String middleToBottomDependencyType = dependencyTuple.getDependencyType();

            if (log.isDebugEnabled()) {
                log.debug("Dependency Tuple: {} depends ({}) on {}.",
                        middleModule.getName() + ":" + middleModule.getVersion(),
                        middleToBottomDependencyType,
                        bottomModule.getName() + ":" + middleModule.getVersion());
            }

            for (NpmModule topModule : middleModule.getDependentModules()) {
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

        reviseDirectDependencyMarker(webModule, rootModule, npmModuleArtifactMap, packageLockParser);

        return inventory;
    }

    /**
     * The look file may be inaccurate in the sense that some dependencies are treated as direct dependencies, while
     * these are not listed in the package.json. Instead of artificially modifying the dependency tree for such cases
     * this method cures the result and managed incorrectly derived direct dependencies to indirect dependencies.
     *
     * @param webModule The web module.
     * @param rootNpmModule The root NpmModule.
     * @param npmModuleArtifactMap The NpmModule to Artifact map.
     * @param packageLockParser The PackageLockParser holding parsing information.
     */
    private void reviseDirectDependencyMarker(WebModule webModule, NpmModule rootNpmModule, Map<NpmModule, Artifact> npmModuleArtifactMap, PackageLockParser packageLockParser) {
        // revise derived markers and correct false-direct dependencies to indirect dependencies
        if (rootNpmModule != null) {
            final List<WebModuleDependency> directDependencies = webModule.getDirectDependencies();

            if (directDependencies != null && !directDependencies.isEmpty()) {
                final Map<String, WebModuleDependency> qualifierDependencyMap = new HashMap<>();
                for (WebModuleDependency wmd : directDependencies) {
                    NpmModule npmModule = packageLockParser.getPathModuleMap().get(wmd.getName());
                    if (npmModule != null) {
                        qualifierDependencyMap.put(npmModule.deriveQualifier(), wmd);
                    }
                }

                final String rootAssetId = "AID-" + rootNpmModule.getName() + "-" + rootNpmModule.getVersion();

                for (Map.Entry<NpmModule, Artifact> entry : npmModuleArtifactMap.entrySet()) {
                    final Artifact artifact = entry.getValue();
                    final String currentMark = artifact.get(rootAssetId);
                    if (currentMark != null && !currentMark.startsWith("(")) {

                        final NpmModule npmModule = entry.getKey();
                        final WebModuleDependency webModuleDependency = qualifierDependencyMap.get(npmModule.deriveQualifier());

                        if (webModuleDependency == null) {
                            String newMark = "(" + currentMark + ")";
                            if (log.isDebugEnabled()) {
                                log.debug("Managing indirect dependency of [{}] from [{}] to [{}].", npmModule.deriveQualifier(), currentMark, newMark);
                            }
                            artifact.set(rootAssetId, newMark);
                        }
                    }
                }
            }
        }
    }

    @Getter
    private static class DependencyTuple {
        private final NpmModule dependentModule;
        private final NpmModule dependencyModule;
        private final String dependencyType;

        public DependencyTuple(NpmModule dependentModule, String dependencyType, NpmModule dependencyModule) {
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
    private static class DependencyTriple {
        private final NpmModule topModule;
        private final NpmModule middleModule;
        private final NpmModule bottomModule;

        private final String topToMiddleDependencyType;
        private final String middleToBottomDependencyType;

        public DependencyTriple(NpmModule topModule, String topToMiddleDependencyType, NpmModule middleModule, String middleToBottomDependencyType, NpmModule bottomModule) {
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

    private void pushDependenciesToStack(NpmModule dependentModule, Map<String, ModuleData> dependencyNameVersionRangeMap,
         Stack<NpmModule> stack, boolean peerDependency, PackageLockParser packageLockParser) {

        if (dependencyNameVersionRangeMap == null) return;

        for  (Map.Entry<String, ModuleData> entry : dependencyNameVersionRangeMap.entrySet()) {
            final String dependencyName = entry.getKey();
            final String dependencyVersionRange = entry.getValue().getVersionRange();
            final NpmModule dependencyModule = packageLockParser.resolveNpmModule(dependentModule, dependencyName, dependencyVersionRange);
            if (dependencyModule == null) {
                reportMissingDependency(dependentModule, peerDependency, entry, packageLockParser);
            } else {
                // manage dependent modules; add dependentModule to dependentModules list of dependency
                final List<NpmModule> dependentModules = dependencyModule.getDependentModules();
                if (!dependentModules.contains(dependentModule)) {
                    dependentModules.add(dependentModule);
                }
                // push onto stack for further downstream processing
                stack.push(dependencyModule);
            }
        }
    }

    private void reportMissingDependency(NpmModule dependentNpmModule, boolean peerDependency, Map.Entry<String, ModuleData> entry, PackageLockParser packageLockParser) {
        if (!peerDependency) {
            String qualifier = entry.getKey() + "@" + entry.getValue();
            log.warn("Module [{}] not resolved using path [{}]. Potentially an optional dependency of [{}].",
                    qualifier, entry.getKey(), dependentNpmModule.getPath());

            List<String> options = packageLockParser.getPathModuleMap().keySet().stream().filter(a -> a.contains(entry.getKey())).collect(Collectors.toList());
            if (!options.isEmpty()) {
                log.warn("Options:");
                options.forEach(log::warn);
            }
        }
    }

    private void apply(NpmModule dependentModule, Map<String, ModuleData> dependencyMap,
              Map<NpmModule, Artifact> npmModuleArtifactMap, String dependencyType,
                       Stack<DependencyTuple> dependencyTuples, PackageLockParser packageLockParser) {

        if (dependencyMap == null) return;

        final Artifact dependentArtifact = npmModuleArtifactMap.get(dependentModule);
        if (dependentArtifact == null) throw new IllegalStateException("No artifact found for module: " + dependentModule);

        for  (Map.Entry<String, ModuleData> entry : dependencyMap.entrySet()) {
            final NpmModule dependencyModule = packageLockParser.resolveNpmModule(dependentModule, entry.getKey(), entry.getValue().getVersionRange());
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

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }

}
